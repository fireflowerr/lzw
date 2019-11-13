package encoders.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import encoders.util.Pair;
import encoders.util.Tuple3;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.nio.file.StandardOpenOption.READ;

public class Test {
  private static final Logger LOGGER = Logger.getLogger(Test.class.getName());
  private static final Runtime RUNTIME = Runtime.getRuntime();
  private static String encodeDir = "encode";
  private static String decodeDir = "decode";
  private static String hashExt = ".md5";
  private static int nameMaxLengh = 15;

  private boolean stacktrace;
  private Path target;

  public Test(String[] args) {
    List<String> lArgs = Arrays.stream(args).collect(Collectors.toList());
    stacktrace = lArgs.remove("--logging");
    if(stacktrace) {
      LOGGER.setLevel(Level.ALL);
    } else {
      LOGGER.setLevel(Level.OFF);
    }

    if(lArgs.isEmpty()) {
      LOGGER.log(Level.SEVERE, "expects test directory path");
      System.exit(1);
    }

    String root = lArgs.remove(0);
    target = Paths.get(root, lArgs.toArray(new String[0]));
    initDirectory();
  }

  private void initDirectory() {
    try {
      for(Iterator<Path> delItr = Files.newDirectoryStream(target, Test::isMD5)
          .iterator(); delItr.hasNext();) {

        Path del = delItr.next();
        Files.delete(del);
      }
    } catch(IOException e) {
      LOGGER.log(Level.SEVERE
        , "error initializing target directory for testing -> " + e.getMessage()
        , e);
      System.exit(1);
    }
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

  private Pair<Path,String[]> lzwEncodeArgs(Path f) {
    String name = f.getFileName().toString();
    Path outPath = f.getParent()
        .resolve(encodeDir)
        .resolve(name + ".lzw");

    return new Pair<>(outPath, new String[] {
      "java"
        ,  "-jar"
        ,  "lzw.jar"
        ,  "-f"
        , f.toString()
        , "-w"
        , outPath.toString()
    });
  }

  private Pair<Path,String[]> lzwDecodeArgs(Path f) {
    String name = f.getFileName().toString() + ".dec";
    Path outPath = f.getParent().getParent()
        .resolve(decodeDir)
        .resolve(name);
    return new Pair<>(outPath, new String[] {
        "java"
      ,  "-jar"
      ,  "lzw.jar"
      , "-d"
      ,  "-f"
      , f.toString()
      , "-w"
      , outPath.toString()
    });
  }

  private Pair<Path,String[]> zip7EncodeArgs(Path f) {
    String name = f.getFileName().toString();
    Path outPath = f.getParent()
        .resolve(encodeDir)
        .resolve(name + ".7z");
    return new Pair<>(outPath, new String[] {
        "7z"
      , "a"
      , outPath.toString()
      , f.toString()
    });
  }

  private Pair<Path,String[]> zip7(Path f) {
    final int extLen = 3;
    String name = f.getFileName().toString();
    name = name.substring(0, name.length() - extLen);
    Path outPath = f.getParent().getParent()
        .resolve(decodeDir);
    return new Pair<>(outPath.resolve(name), new String[] {
        "7z"
      , "e"
      , f.toString()
      , "-o" + outPath.toString()
    });
  }

  private void runTest(List<Tuple3<
      String
    , Function<Path, Pair<Path,String[]>>
    , Function<Path, Pair<Path,String[]>>>> executors) {

    Iterator<Path> itr = null;
    try {
      itr = Files.list(target)
        .filter(Files::isRegularFile)
        .iterator();
    } catch(IOException e) {
      LOGGER.log(Level.SEVERE
        , "cannot stat target dir -> " + e.getMessage()
        , e);
      System.exit(1);
    }

    String fileField = "%-" + nameMaxLengh + "s  ";
    String formatter = fileField + "%-8s%-8s%-8.2f %-3s  %8.2fs %-3s  %6.2f%n";
    System.out.printf(fileField + "%-8s%-8s%-16s%-15s%s%n", "FILE", "RUNNER", "STATUS", "IN SIZE", "OUT SIZE", "CR");
    Tuple3<String, Function<Path, Pair<Path,String[]>>, Function<Path, Pair<Path,String[]>>> head = executors.remove(0);
    while(itr.hasNext()) {
      Path p = itr.next();
      String fName = formatName(p.getFileName().toString());
      runExternalUnit(formatter, p, fName, head.fst, head.snd, head.thd);
      executors.forEach(x -> runExternalUnit(formatter, p, "", x.fst, x.snd, x.thd));
    }
  }

  private void runExternalUnit(String formatter
    , Path p
    , String fName
    , String runnerName
    , Function<Path, Pair<Path,String[]>> getEnArgs
    , Function<Path, Pair<Path,String[]>> getDeArgs) {

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

    Pair<Path,String[]> enArgs = getEnArgs.apply(p);
    try {
      RUNTIME.exec(enArgs.snd).waitFor();
    } catch(Exception e) {
      LOGGER.log(Level.SEVERE
          , "external encoding invocation failed to execute -> " + e.getMessage()
          , e);
      pass = false;
    }

    // decode
    Pair<Path,String[]> deArgs = getDeArgs.apply(enArgs.fst);
    try {
      RUNTIME.exec(deArgs.snd).waitFor();
    } catch(Exception e) {
      LOGGER.log(Level.SEVERE
          , "external decoding invocation failed to execute -> " + e.getMessage()
          , e);
      pass = false;
    }

    String outHash = writeMD5(deArgs.fst);
    pass = pass && inHash.equals(outHash);
    String status = pass ? "PASS" : "FAIL";

    if(pass) {
      long inSz = 0;
      long outSz = 0;
      try {
        inSz = Files.size(p);
        outSz = Files.size(enArgs.fst);
      } catch(IOException e) {
        LOGGER.log(Level.SEVERE, "cannot fetch file size -> " + e.getMessage(), e);
      }
      Pair<Float, String> fInSz = formatSz(inSz);
      Pair<Float, String> fOutSz = formatSz(outSz);
      float ratio = inSz / (float)outSz;

      System.out.printf(formatter
        , fName
        , runnerName
        , status
        , fInSz.fst
        , fInSz.snd
        , fOutSz.fst
        , fOutSz.snd , ratio);
    } else {
      String fileField = "%-" + nameMaxLengh + "s  ";
      System.out.printf(fileField + "%-8s%-6s%n", fName, runnerName, status);
    }
  }

  private String writeMD5(Path p) {
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
    return "";
  }

  private static boolean isMD5(Path p) {
    String name = p.getFileName().toString();
    int cut = name.lastIndexOf('.');

    if(cut < 0) {
      return false;
    } else {
      return hashExt.equals(name.substring(cut));
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

  private static String formatName(String name) {
    if(name.length() > nameMaxLengh) {
      name = name.substring(0, nameMaxLengh - 3);
      return name + "...";
    } else {
      return name;
    }
  }

  public static void main(String[] args) {
    Test app = new Test(args);
    List<Tuple3<String, Function<Path, Pair<Path, String[]>>, Function<Path, Pair<Path,String[]>>>> l = new ArrayList<>();
    l.add(new Tuple3<>("lzw", app::lzwEncodeArgs, app::lzwDecodeArgs));
    l.add(new Tuple3<>("7zip", app::zip7EncodeArgs, app::zip7));
//    TODO add winzip arg generators
    app.runTest(l);
  }
}
