package encoders.app;
import encoders.app.cli.Cli;
import encoders.app.cli.CliBuilder;
import encoders.app.cli.NoSuchFlagException;
import encoders.coder.*;
import encoders.dict.*;
import encoders.util.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;;


public final class App<A> {
  private static final Logger LOGGER = Logger.getLogger(App.class.getName());
  private static List<Tuple3<String, Integer, Character>> tui;
  static {
    LOGGER.setLevel(Level.WARNING);
    tui = List.of(
        new Tuple3<>("file", -1, 'f')
      , new Tuple3<>("write", -1, 'w')
      , new Tuple3<>("decode", 0, 'd')
      , new Tuple3<>("binary", 0, 'b')
      , new Tuple3<>("tunable", 1, 'k')
      , new Tuple3<>("identity", 0, 'i')
      , new Tuple3<>("verbose", 0, 'v')
      , new Tuple3<>("silent", 0, 's')
      , new Tuple3<>("logging", 1, 'l')
    );
  }

  private Cli cli;
  private long inSz;
  private long outSz;
  private long byteC;
  private long statusMod;
  private boolean statusEnd = false;
  private int geParam;
  private GolombRice ge;
  private Lzw<A> lzw;
  private Stream<Byte> cIn;
  private OutputStream cOut;
  private Coder<Byte, Byte> coder;
  private int lvl = 1;

  public App(Cli cli
    , BidiDict<Pair<A, Integer>, Integer> dict
    , Coder<Byte,A> primer
    , String fmt) {

    this.cli = cli;
    initCin();
    initCout();
    initLogging();
    initCoder(dict, primer, fmt);
  }

  public void run() {
    if(cli.isSet("decode")) {
      decode();
    } else {
      encode();
    }

    try {
      cIn.close();
      cOut.close();
    } catch(Exception e) {
      LOGGER.log(Level.WARNING, "unable to close IO stream -> " + e.getMessage(), e);
    }
  }

  private void initLogging() {
    if(cli.isSet("logging")) { // enable printing extra log info

      lvl = Integer.valueOf(cli.getArgs("logging").get(0));
      Level level = null;
      switch(lvl) {
        case 0:
          level = Level.OFF;
          break;
        case 1:
          level = Level.WARNING;
          break;
        case 2:
          level = Level.ALL;
          break;
        default:
          level = Level.ALL;
          break;
      }
      LOGGER.setLevel(level);
      Handler dbgInfo = new ConsoleHandler();
      dbgInfo.setLevel(Level.ALL);
      dbgInfo.setFilter(x -> x.getLevel().intValue() < Level.INFO.intValue());
      LOGGER.addHandler(dbgInfo);
    }
  }

  private void initCoder(
      BidiDict<Pair<A, Integer>, Integer> dict
    , Coder<Byte, A> primer
    , String fmt) {

    Runnable counter = cli.isSet("verbose") ? () -> byteC++ : null;
    lzw = new Lzw<>(dict, counter);
    if(ge == null) {
      if(cli.isSet("tunable")) {
        geParam = Integer.valueOf(cli.getArgs("tunable").get(0));
      }
      ge = new GolombRice(geParam);
    }

    Coder<Boolean, Byte> finisher = new BaseCoder<>(Streams::mapToByte
      , x -> x.flatMap(Streams::streamToBin));

    if(cli.isSet("logging") && lvl > 1) {
      coder = interweaveLog(primer, "primer", "0x%02x", fmt)
          .compose(interweaveLog(lzw, "lzw", fmt, "%d"))
          .compose(interweaveLog(ge, "rice", "%d", "%b"))
          .compose(interweaveLog(finisher, "finisher", "%b", "0x%02x"));
    } else {
      coder = primer.compose(lzw)
          .compose(ge)
          .compose(finisher);
    }
  }

  private void initCin() {
    List<String> fArgs = cli.getArgs("file");
    if(fArgs.isEmpty()) {
      LOGGER.log(Level.SEVERE, "expects input or file path as CLI args");
      abort();
    }

    if(cli.isSet("file")) {

      Path p = pathFromList(fArgs);
      if(!Files.isRegularFile(p)) {
        LOGGER.log(Level.SEVERE, p + " is not a file or cannot be read");
        abort();
      }

      try {
        inSz = Files.size(p);
      } catch(IOException e) {
        logWithException(Level.SEVERE
          , "cannot stat file" + p + " -> " + e.getMessage()
          , e);
      }

      InputStream in = null;
      try {
        in = new BufferedInputStream(Files.newInputStream(p, READ));
      } catch(IOException e) {
        logWithException(Level.SEVERE
          , "failed to get input stream -> " + e.getMessage()
          , e);
        abort();
      }

      if(cli.isSet("decode")) {
        try{
          geParam = in.read();
        } catch(IOException e) {
          logWithException(Level.SEVERE
            , "cannot retrieve rice paramter for decoding -> " + e.getMessage()
            , e);
        }
        ge = new GolombRice(geParam);
      } else {
        geParam = (int)Math.ceil(Math.log(inSz));
        ge = new GolombRice(geParam);
      }

      try {
        cIn = Streams.readFileBytes(in);
      } catch(IOException e) {
        logWithException(Level.SEVERE
          , "failed to open file input stream -> " + e.getMessage()
          , e);
        abort();
      }

    } else {
      Deque<Byte> tmp = new ArrayDeque<>();
      for(Byte b : String.join(" ", fArgs).getBytes()) {
        tmp.addLast(b);
      }

      inSz = tmp.size();
      if(cli.isSet("decode")) {
        geParam = tmp.poll();
        ge = new GolombRice(geParam);
      } else {
        geParam = (int)Math.ceil(Math.log(inSz));
        ge = new GolombRice(geParam);
      }

      Stream.Builder<Byte> acc = Stream.builder();
      while(!tmp.isEmpty()) {
        acc.accept(tmp.poll());
      }
      cIn = acc.build();
    }
  }

  private void initCout() {
    if(cli.isSet("silent")) { // null output stream. Useful with verbose flag
      cOut = new OutputStream(){
        @Override
        public void write(int arg0) throws IOException {
          return;
        }
      };
    } else if(cli.isSet("write")) {

      List<String> wArgs = cli.getArgs("write");
      if(wArgs.isEmpty()) {
        LOGGER.log(Level.SEVERE, "expects output file path");
        abort();
      }

      Path p = pathFromList(wArgs);
      try {
        cOut = new BufferedOutputStream(Files.newOutputStream(p, CREATE, WRITE, TRUNCATE_EXISTING));
      } catch(IOException e) {
        logWithException(Level.SEVERE
          , "failed to open file output stream -> " + e.getMessage()
          , e);
        abort();
      }
    } else {
      cOut = System.out;
    }
  }

  private void decode() {
    coder.decode(cIn)
        .forEach(this::writeOut);
  }

  private void encode() {
    writeOut((byte)geParam);
    Stream<Byte> tmp = coder.encode(cIn);

    if(cli.isSet("verbose") && cli.isSet("file") && (cli.isSet("write") || cli.isSet("silent"))) {
      statusMod = inSz / 20;
      tmp.forEach(this::writeStatus);
      statusEnd();
    } else {

      if(cli.isSet("identity")) {
        tmp = coder.decode(tmp);
      }
      tmp.forEach(this::writeOut);
    }
  }

  private void writeOut(Byte b) { //exit on write fail
    try {
      cOut.write(b);
    } catch(IOException e) {
      logWithException(Level.SEVERE
        , "error writing to output stream -> " + e.getMessage()
        , e);
      abort();
    }
  }

  private void writeStatus(Byte b) { // this is only an estimate there may be trailing bytes and there is the golomb prefix to consider
    writeOut(b);

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
      statusEnd();
    }
  }

  private void statusEnd() {
    if(statusEnd) {
      return;
    }

    float cr = inSz / (float)outSz;

    Pair<Float, String> n = formatSz(outSz);
    Pair<Float, String> d = formatSz(inSz);

    System.out.printf(">  CR: (%.2f %s/%.2f %s) -> %.3f%n", d.fst, d.snd, n.fst, n.snd, cr);
    byteC++;
    statusEnd = true;
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

  private void logWithException(Level l, String s, Exception e) { // don't print stacktrace if logging is disabled
    if(cli.isSet("logging")) {
      LOGGER.log(l, s, e);
    } else {
      LOGGER.log(l, s);
    }
  }

  private static <A, B> Coder<A, B> interweaveLog(
      Coder<A,B> t
    , String passName
    , String aFmt
    , String bFmt) {

    String aFormat = "%s %c %s: " + aFmt;
    String bFormat = "%s %c %s: " + bFmt;

    Coder<A, A> inLog = new BaseCoder<>(
        x -> logDbg(x, aFormat, passName, 'e', "in")
      , x -> logDbg(x, aFormat, passName, 'd', "out")
    );

    Coder<B, B> outLog = new BaseCoder<>(
        x -> logDbg(x, bFormat, passName, 'e', "out")
      , x -> logDbg(x, bFormat, passName, 'd', "in")
    );

    return Coder.compose(inLog, t)
        .compose(outLog);
  }

  private static <A> Stream<A> logDbg(
      Stream<A> s
    , String fmt
    , String passName
    , char key
    , String io) {

    return s.peek(x -> LOGGER.log(Level.FINE, String.format(fmt, passName, key, io, x)));
  }

  private static void abort() {
    LOGGER.log(Level.INFO, "exit 1");
    System.err.println("FAILURE: aborted");
    System.exit(1);
  }


  public static void main(String[] args) {
    Cli cli = null;
    try {
      cli = new CliBuilder()
          .init3(tui)
          .setDefaultFlag("file")
          .build(args);
    } catch(NoSuchFlagException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
      abort();
    }

    if(cli.isSet("binary")) {

      Coder<Byte, Boolean> primer = new BaseCoder<>(
          x -> x.flatMap(Streams::streamToBin)
        , Streams::mapToByte);

      App<Boolean> app = new App<Boolean>(
          cli
        , Lzw.getDict2()
        , primer
        , "%b");

      app.run();

    } else {
      Coder<Byte, Byte> primer = new BaseCoder<>(x -> x, x -> x);
      App<Byte> app = new App<>(
          cli
        , Lzw.getDict8()
        , primer
        , "0x%2x");
      app.run();
    }

    LOGGER.log(Level.INFO, "exit 0");
    System.exit(0);
  }

}
