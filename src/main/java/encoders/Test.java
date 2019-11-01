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
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import encoders.util.Pair;
import encoders.util.unchecked.UConsumer;
import encoders.util.unchecked.URunnable;
import encoders.util.unchecked.USupplier;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

public class Test {
  private static String encodeDir = "encode";
  private static String decodeDir = "decode";
  private static String hashExt = ".md5";
  Path target;

  public Test(String[] args) {
    if(args.length == 0) {
      System.err.println("err: expects test directory path");
      System.exit(1);
    }

    String root = args[0];
    String[] tmp = new String[args.length - 1];
    System.arraycopy(args, 1, tmp, 0, args.length - 1);
    target = Paths.get(root, tmp);

    try {
      initDirectory();
    } catch(IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void initDirectory() throws IOException {
    Stream.of(encodeDir, decodeDir)
      .map(target::resolve)
      .forEach(UConsumer.unchecked(this::wipeCreateDir));
  }

  private void wipeCreateDir(Path dir) throws IOException {
    if(Files.isDirectory(dir)) {
      Files.list(dir)
        .forEach(UConsumer.unchecked(Files::delete));
    } else {
      Files.deleteIfExists(dir);
      Files.createDirectory(dir);
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

  private void encode() throws IOException {
    Runtime runtime = Runtime.getRuntime();

    Iterator<Path> itr = Files.list(target)
      .filter(Files::isRegularFile)
      .filter(x -> !isMD5(hashExt, x))
      .iterator();

    System.out.printf("%-10s  %11s  %11s      %-6s%n", "FILE", "IN SIZE", "OUT SIZE", "CR");
    Path p = null;
    while(itr.hasNext()) {
      p = itr.next();

      String inHash = null;
      Path md5 = appendToFileName(hashExt, p);
      if(!Files.exists(md5)) {
        inHash = writeMD5(p);
      } else {
        inHash = new String(Files.readAllBytes(md5));
      }

      String[] lzwEnArgs =  lzwEncodeArgs(p);
      URunnable.unchecked(() -> runtime.exec(lzwEnArgs).waitFor()).run();

      // decode
      int tmp = lzwEnArgs.length - 1;
      Path enPath = Paths.get(lzwEnArgs[tmp]);
      String[] lzwDeArgs = lzwDecodeArgs(enPath);
      URunnable.unchecked(() -> runtime.exec(lzwDeArgs).waitFor()).run();

      tmp = lzwDeArgs.length - 1;
      String outHash = writeMD5(Paths.get(lzwDeArgs[tmp]));
      boolean pass = inHash.equals(outHash);
      String status =  pass ? "pass" : "fail";

      if(pass) {
        long inSz = Files.size(p);
        long outSz = Files.size(enPath);
        Pair<Float, String> fInSz = formatSz(inSz);
        Pair<Float, String> fOutSz = formatSz(outSz);
        float ratio = inSz / (float)outSz;

        System.out.printf("%-10s  %8.2f %-3s  %8.2f %-3s    %-6.2f%n"
          , p.getFileName().toString()
          , fInSz.fst
          , fInSz.snd
          , fOutSz.fst
          , fOutSz.snd
          , ratio);
      }

    }
  }

  private static String writeMD5(Path p) {
    Path hashP = appendToFileName(hashExt, p);
    MessageDigest md5 = USupplier.unchecked(() -> MessageDigest.getInstance("MD5")).get(); try(DigestInputStream dig = new DigestInputStream(new BufferedInputStream(
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
      throw new UncheckedIOException(e);
    }
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

  public void run() {
    URunnable.unchecked(this::encode).run();
  }

  public static void main(String[] args) {
    Test app = new Test(args);
    app.run();
  }
}
