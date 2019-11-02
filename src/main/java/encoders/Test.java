package encoders;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import encoders.util.Pair;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.READ;

public class Test {
  private static final Logger LOGGER = Logger.getLogger(Test.class.getName());
  private static final Runtime RUNTIME = Runtime.getRuntime();
  private static String encodeDir = "encode";
  private static String decodeDir = "decode";
  private static String hashExt = ".md5";
  Path target;

  public Test(String[] args) {
    if(args.length == 0) {
      LOGGER.log(Level.SEVERE, "expects test directory path");
      System.exit(1);
    }

    String root = args[0];
    String[] tmp = new String[args.length - 1];
    System.arraycopy(args, 1, tmp, 0, args.length - 1);
    target = Paths.get(root, tmp);
    initDirectory();
  }

  private void initDirectory() {
    Stream.of(encodeDir, decodeDir)
      .map(target::resolve)
      .forEach(this::wipeCreateDir);
  }

  private void wipeCreateDir(Path dir) {
    try {
      if(Files.isDirectory(dir)) {
        for(Iterator<Path> delItr = Files.list(dir).iterator(); delItr.hasNext();) {
            Path del = delItr.next();
            Files.delete(del);
        }
      } else {
        Files.deleteIfExists(dir);
        Files.createDirectory(dir);
      }
    } catch(IOException e) {
      LOGGER.log(Level.SEVERE
        , "error initializing target directory for testing -> " + e.getMessage()
        , e);
      System.exit(1);
    }
  }

  private String[] lzwEncodeArgs(Path f) {
    String name = f.getFileName().toString();
    return new String[] {
      "java"
        ,  "-jar"
        ,  "lzw.jar"
        ,  "-f"
        , f.toString()
        , "-w"
        , f.getParent()
        .resolve(encodeDir)
        .resolve(name + "$" + "lzw").toString()
    };
  }

  private String[] lzwDecodeArgs(Path f) {
    String name = f.getFileName().toString();
    return new String[] {
      "java"
        ,  "-jar"
        ,  "lzw.jar"
        , "-d"
        ,  "-f"
        , f.toString()
        , "-w"
        , f.getParent().getParent()
        .resolve(decodeDir)
        .resolve(name).toString()
    };
  }

  private void runTest(List<Pair<
      Function<Path, String[]>
    , Function<Path, String[]>>> executors) {

    Iterator<Path> itr = null;
    try {
      itr = Files.list(target)
        .filter(Files::isRegularFile)
        .filter(x -> !isMD5(hashExt, x))
        .iterator();
    } catch(IOException e) {
      LOGGER.log(Level.SEVERE
        , "cannot stat target dir -> " + e.getMessage()
        , e);
      System.exit(1);
    }

    System.out.printf("%-12s%-8s%-14s%-14s%-4s%n", "FILE", "STATUS", "IN SIZE", "OUT SIZE", "CR");
    String formatter = "%-15s  %-4s  %8.2f %-3s  %8.2f %-3s    %-6.2f    |    ";
    while(itr.hasNext()) {
      Path p = itr.next();
      executors.forEach(x -> runExternalUnit(formatter, p, x.fst, x.snd));
      System.out.println();
    }
  }

  private static void runExternalUnit(String formatter
    , Path p
    , Function<Path, String[]> getEnArgs
    , Function<Path, String[]> getDeArgs) {

    boolean pass = true;
    String inHash = null;
    Path md5 = appendToFileName(hashExt, p);
    if(!Files.exists(md5)) {
      inHash = writeMD5(p);
    } else {
      try {
        inHash = new String(Files.readAllBytes(md5));
      } catch(IOException e) {
        LOGGER.log(Level.SEVERE
          , "failed to get md5 -> " + e.getMessage()
          , e);
        pass = false;
      }
    }

    String[] lzwEnArgs = getEnArgs.apply(p);
    try {
      RUNTIME.exec(lzwEnArgs).waitFor();
    } catch(Exception e) {
      LOGGER.log(Level.SEVERE
          , "external encoding invocation failed to execute -> " + e.getMessage()
          , e);
      pass = false;
    }

    // decode
    int tmp = lzwEnArgs.length - 1;
    Path enPath = Paths.get(lzwEnArgs[tmp]);
    String[] lzwDeArgs = getDeArgs.apply(enPath);
    try {
      RUNTIME.exec(lzwDeArgs).waitFor();
    } catch(Exception e) {
      LOGGER.log(Level.SEVERE
          , "external decoding invocation failed to execute -> " + e.getMessage()
          , e);
      pass = false;
    }

    tmp = lzwDeArgs.length - 1;
    String outHash = writeMD5(Paths.get(lzwDeArgs[tmp]));
    pass = pass && inHash.equals(outHash);
    String status = pass ? "PASS" : "FAIL";

    if(pass) {
      long inSz = 0;
      long outSz = 0;
      try {
        inSz = Files.size(p);
        outSz = Files.size(enPath);
      } catch(IOException e) {
        LOGGER.log(Level.SEVERE, "cannot fetch file size -> " + e.getMessage(), e);
      }
      Pair<Float, String> fInSz = formatSz(inSz);
      Pair<Float, String> fOutSz = formatSz(outSz);
      float ratio = inSz / (float)outSz;

      System.out.printf(formatter
        , p.getFileName()
        , status
        , fInSz.fst
        , fInSz.snd
        , fOutSz.fst
        , fOutSz.snd , ratio);
    } else {
      System.out.printf("%-15s  %-4s", p.getFileName(), status);
    }

  }

  private static String writeMD5(Path p) {
    Path hashP = appendToFileName(hashExt, p);
    MessageDigest md5 = null;
    try {
      md5 = MessageDigest.getInstance("MD5");
    } catch(NoSuchAlgorithmException e) {
      LOGGER.log(Level.SEVERE
        , "unable to retrieve md5 algorithm -> " + e.getMessage()
        , e) ;
      System.exit(1);
    }
    try(DigestInputStream dig = new DigestInputStream(new BufferedInputStream(
          Files.newInputStream(p, CREATE, TRUNCATE_EXISTING, READ))
        , md5)) {

      while(dig.read() > 0);

      BufferedOutputStream fOut = new BufferedOutputStream(
          Files.newOutputStream(hashP));

      byte[] hash = dig.getMessageDigest().digest();
      fOut.write(hash);
      fOut.close();
      return new String(hash);

    } catch(IOException e) {
      LOGGER.log(Level.SEVERE, "writeMD5 execution failure -> " + e.getMessage(), e);
    }
    throw new RuntimeException(); // unreachable
  }

  private static boolean isMD5(String ext, Path p) {
    String name = p.getFileName().toString();
    int cut = name.lastIndexOf('.');

    if(cut < 0) {
      return false;
    } else {
      return ext.equals(name.substring(cut));
    }
  }

  private static Path appendToFileName(String s, Path p) {
    String name = p.getFileName().toString();
    Path parent = p.getParent();

    if(parent == null) {
      return Paths.get(name + s);
    } else {
      return parent.resolve(name + s);
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

  public static void main(String[] args) {
    Test app = new Test(args);
    List<Pair<Function<Path, String[]>, Function<Path, String[]>>> l = new ArrayList<>();
    l.add(new Pair<>(app::lzwEncodeArgs, app::lzwDecodeArgs));
    app.runTest(l);
  }
}
