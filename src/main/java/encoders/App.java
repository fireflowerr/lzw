package encoders;
import encoders.lzw.*;
import encoders.lzw.dict.*;
import encoders.util.*;
import encoders.util.unchecked.URunnable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;;


public final class App {
  private CliParser cli;
  private GolombRice ge;
  private Stream<Byte> cIn;
  private OutputStream cOut;
  private static byte[] lineSepr = System.getProperty("line.separator").getBytes();

  public App(String[] args, int golombRiceK) {
    cli = new CliParser(args);
    ge = new GolombRice(golombRiceK);
    cIn = initCin();
    cOut = initCout();
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

  private Stream<Byte> initCin() {
    List<String> fArgs = cli.getFileArgs();
    if(fArgs.isEmpty()) {
      System.err.println("err: expects input or file path");
      System.exit(1);
    }

    Stream<Byte> cIn = null;
    if(cli.file()) {

      Path p = pathFromList(fArgs);
      try {
        InputStream in = new BufferedInputStream(
            Files.newInputStream(p, READ));

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
    return cIn;
  }

  private OutputStream initCout() {
    if(!cli.write()) {
      cOut = System.out;
      return cOut;
    }

    List<String> wArgs = cli.getWriteArgs();
    if(wArgs.isEmpty()) {
      System.err.println("err: expects output file path");
      System.exit(1);
    }

    OutputStream cOut = null;
    Path p = pathFromList(wArgs);
    try {
      cOut = new BufferedOutputStream(Files.newOutputStream(p, CREATE, WRITE, TRUNCATE_EXISTING));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return cOut;
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
    App app = new App(args, 4);
    app.run();
  }
}
