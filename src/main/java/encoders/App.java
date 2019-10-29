package encoders;
import encoders.lzw.*;
import encoders.lzw.dict.*;
import encoders.util.*;
import encoders.util.unchecked.URunnable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;;


public final class App {
  private CliParser cli;
  private int inSz;
  private int byteC;
  private int geParam;
  private GolombRice ge;
  private Stream<Byte> cIn;
  private OutputStream cOut;
  private static byte[] lineSepr = System.getProperty("line.separator").getBytes();

  public App(String[] args) {
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

  private void initCin() {
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

      try {
        InputStream in = new BufferedInputStream(
            Files.newInputStream(p, READ));

        if(cli.decode()) {
          geParam = in.read();
          ge = new GolombRice(geParam);
        } else {
          geParam = (int)Math.ceil(Math.log(Files.size(p)));
        }

        cIn = Streams.readFileBytes(in);
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
      }

    } else {
      byte[] tmp = String.join(" ", fArgs.toArray(String[]::new)).getBytes();
      Stream.Builder<Byte> acc = Stream.builder();
      for(int i = 0; i < tmp.length; i++) {
        acc.accept(tmp[i]);
      }
      cIn = acc.build();
    }
  }

  private void initCout() {
    if(!cli.write()) {
      cOut = System.out;
      return;
    }

    List<String> wArgs = cli.getWriteArgs();
    if(wArgs.isEmpty()) {
      System.err.println("err: expects output file path");
      System.exit(1);
    }

    Path p = pathFromList(wArgs);
    try {
      cOut = new BufferedOutputStream(Files.newOutputStream(p, CREATE, WRITE, TRUNCATE_EXISTING));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private <A> void decode(
      BidiDict<Integer, Pair<A, Integer>> dict
    , Function<Stream<A>, Stream<Byte>> normalizer) {

    Stream<A> tmp = Lzw.decode(dict
            , ge.decode(cIn.flatMap(Streams::streamToBin)))
          .flatMap(x -> x.stream());

    normalizer.apply(tmp)
        .forEach(this::writeOut);
    writeOut(lineSepr);
  }

  private void writeOut(Byte b) {
    try {
      cOut.write(b);
    } catch(IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void writeOut(byte[] b) {
    try {
      cOut.write(b);
    } catch(IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private <A> void encode(
      BidiDict<Pair<A, Integer>, Integer> dict
    , Stream<A> cIn
    , Function<Stream<A>, Stream<Byte>> normalizer) {

    if(cli.verify()) {
      Stream<A> tmp = Lzw.decode(dict.flip() // proof of correctness
          , ge.decode(Streams.mapToByte(Lzw.encode(dict, cIn)
              .flatMap(ge::encode))
            .flatMap(Streams::streamToBin)))
        .flatMap(x -> x.stream());

      normalizer.apply(tmp)
        .forEach(this::writeOut);
      writeOut(lineSepr);

    } else if(cli.raw()) {
      Lzw.encode(dict, cIn)
        .map(x -> x.toString() + ",")
        .map(x -> x.getBytes())
        .forEach(this::writeOut);
      writeOut(lineSepr);

    } else {
      writeOut((byte)geParam);
      Streams.mapToByte(Lzw.encode(dict, cIn)
          .flatMap(ge::encode))
          .forEach(this::writeOut);
      writeOut(lineSepr);
    }
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
    App app = new App(args);
    app.run();
  }
}
