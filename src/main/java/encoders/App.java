package encoders;
import encoders.lzw.*;
import encoders.lzw.dict.*;
import encoders.util.*;
import encoders.util.unchecked.UConsumer;
import encoders.util.unchecked.UFunction;
import encoders.util.unchecked.URunnable;
import encoders.util.unchecked.USupplier;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;;


public final class App {
  private CliParser cli;
  private long inSz;
  private long outSz;
  private long byteC;
  private long statusMod;
  private int geParam;
  private GolombRice ge;
  private Stream<Byte> cIn;
  private OutputStream cOut;
  private static byte[] lineSepr = System.getProperty("line.separator").getBytes();

  public App(String[] args) throws IOException {
    cli = new CliParser(args);
    initCin();
    initCout();

    if(ge == null) {
      if(cli.tunable()) {
        geParam = Integer.valueOf(cli.getTunableArg());
      }
      ge = new GolombRice(geParam);
    }
  }

  public void run() {
    if(cli.binary()) {
      Function<Stream<Boolean>, Stream<Byte>> normalizer = Streams::mapToByte;

      if(cli.decode()) {
        decode(Lzw.getDict2().flip(), normalizer);
      } else {
        encode(Lzw.getDict2()
          , cIn.flatMap(Streams::streamToBin)
          , normalizer);
      }
    } else {
      Function<Stream<Byte>, Stream<Byte>> normalizer = x -> x;

      if(cli.decode()) {
        decode(Lzw.getDict8().flip(), normalizer);
      } else {
        encode(Lzw.getDict8()
          , cIn
          , normalizer);
      }
    }

    cIn.close();
    URunnable.unchecked(cOut::close).run();
  }

  private void initCin() throws IOException {
    List<String> fArgs = cli.getFileArgs();
    if(fArgs.isEmpty()) {
      System.err.println("err: expects input or file path");
      System.exit(1);
    }

    if(cli.file()) {

      Path p = pathFromList(fArgs);
      if(!Files.isRegularFile(p)) {
        System.err.println("err: " + p + " is not a file or cannot be read");
        System.exit(1);
      }

      inSz = Files.size(p);
      InputStream in = new BufferedInputStream(
          Files.newInputStream(p, READ));

      if(cli.decode()) {
        geParam = in.read();
        ge = new GolombRice(geParam);
      } else {
        geParam = (int)Math.ceil(Math.log(Files.size(p)));
      }

      cIn = Streams.readFileBytes(in);

    } else {
      byte[] tmp = String.join(" ", fArgs.toArray(String[]::new)).getBytes();
      inSz = tmp.length;
      Stream.Builder<Byte> acc = Stream.builder();
      for(int i = 0; i < tmp.length; i++) {
        acc.accept(tmp[i]);
      }
      cIn = acc.build();
    }
  }

  private void initCout() throws IOException {
    if(cli.silent()) { // null output stream. Useful with verbose flag
      cOut = new OutputStream(){
        @Override
        public void write(int arg0) throws IOException {
          return;
        }
      };
    } else if(cli.write()) {

      List<String> wArgs = cli.getWriteArgs();
      if(wArgs.isEmpty()) {
        System.err.println("err: expects output file path");
        System.exit(1);
      }

      Path p = pathFromList(wArgs);
      cOut = new BufferedOutputStream(Files.newOutputStream(p, CREATE, WRITE, TRUNCATE_EXISTING));
    } else {
      cOut = System.out;
    }

  }

  private <A> void decode(
      BidiDict<Integer, Pair<A, Integer>> dict
    , Function<Stream<A>, Stream<Byte>> normalizer) {

    Stream<A> tmp = Lzw.decode(dict
            , ge.decode(cIn.flatMap(Streams::streamToBin)))
          .flatMap(x -> x.stream());

    normalizer.apply(tmp)
        .forEach(UConsumer.unchecked(cOut::write));
  }

  private <A> void encode(
      BidiDict<Pair<A, Integer>, Integer> dict
    , Stream<A> cIn
    , Function<Stream<A>, Stream<Byte>> normalizer) {

    Optional<Runnable> counter = null;
    if(cli.verbose()) {
      counter = Optional.of(() -> byteC++);
    } else {
      counter = Optional.empty();
    }

    if(cli.identity()) {
      Stream<A> tmp = Lzw.decode(dict.flip() // proof of correctness
          , ge.decode(Streams.mapToByte(Lzw.encode(dict, cIn, counter)
              .flatMap(ge::encode))
            .flatMap(Streams::streamToBin)))
        .flatMap(x -> x.stream());

      normalizer.apply(tmp)
        .forEach(UConsumer.unchecked(cOut::write));
      URunnable.unchecked(() -> cOut.write(lineSepr)).run();

    } else if(cli.raw()) {
      Lzw.encode(dict, cIn, counter)
        .map(x -> x.toString() + ",")
        .map(x -> x.getBytes())
        .forEach(UConsumer.unchecked(cOut::write));
      URunnable.unchecked(() -> cOut.write(lineSepr)).run();

    } else {
      URunnable.unchecked(() -> cOut.write((byte)geParam)).run();
      Stream<Byte> tmp = Streams.mapToByte(Lzw.encode(dict, cIn, counter)
          .flatMap(ge::encode));

      if(cli.verbose()) {
        statusMod = inSz / 20;
        tmp.forEach(UConsumer.unchecked(this::writeStatus));
      } else {
        tmp.forEach(UConsumer.unchecked(cOut::write));
      }
      URunnable.unchecked(() -> cOut.write(lineSepr)).run();
    }
  }

  private void writeStatus(Byte b) throws IOException { // this is only an estimate there may be trailing bytes and there is the golomb prefix to consider
    cOut.write(b);

    if(byteC > inSz) {
      return;
    }
    if(outSz == 0) {
      System.out.print("<");
    }

    if(++outSz % statusMod == 0) {
      System.out.print("=");
    }

    if(byteC == inSz) {
      float cr = outSz / (float)inSz * 100;

      Pair<Float, String> n = formatSz(outSz);
      Pair<Float, String> d = formatSz(inSz);

      System.out.printf(">  CR: (%.2f %s/%.2f %s) -> %.3f%c%n", n.fst, n.snd, d.fst, d.snd, cr, '%');
      byteC++;
    }
  }

  private static Pair<Float, String> formatSz(long sz) {
    float pow = (float)Math.pow(2, 10);
    float curr = pow;
    float prev = 1;

    Iterator<String> si = Stream.of("B", "KiB", "MiB", "GiB").iterator();
    int i = 1;
    String unit = null;
    while(si.hasNext()) {
      unit = si.next();
      if(sz < curr) {
        return new Pair<>(sz / prev, unit);
      } else {
        prev = curr;
        curr = (float)Math.pow(pow, ++i);
      }
    }
    return new Pair<>(sz / prev, unit);
  }


  private static Path pathFromList(List<String> l) {
    String root = l.remove(0);
    Path p = null;
    if(!l.isEmpty()) {
      p = Paths.get(root, l.toArray(String[]::new));
    } else {
      p = Paths.get(root);
    }

    return p;
  }

  public static void main(String[] args) {
      App app = USupplier.unchecked(() -> new App(args)).get();
      app.run();
  }
}
