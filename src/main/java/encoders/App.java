package encoders;
import encoders.lzw.*;
import encoders.lzw.dict.*;
import encoders.util.*;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;;


public final class App {
  private CliParser cli;
  private GolombRice ge;
  private Stream<Character> cIn;
  private PrintWriter cOut;

  public App(String[] args, int golombRiceK) {
    cli = new CliParser(args);
    ge = new GolombRice(golombRiceK);
    cIn = initCin();
    cOut = initCout();
  }

  public void run() {
    if(cli.decode()) {
      decode();
    } else {
      encode();
    }
    cIn.close();
    cOut.close();
  }

  private Stream<Character> initCin() {
    List<String> fArgs = cli.getFileArgs();
    if(fArgs.isEmpty()) {
      System.err.println("err: expects input or file path");
      System.exit(1);
    }

    Stream<Character> cIn = null;
    if(cli.file()) {

      Path p = pathFromList(fArgs);
      try {
        cIn = Streams.readFileChars(p, 1024, StandardCharsets.UTF_8)
            .filter(x -> x != '\n'); // unsatisfactory, find a better way
     } catch (IOException e) {
       e.printStackTrace();
       System.exit(1);
     }

   } else {
     cIn = String.join(" ", fArgs.toArray(String[]::new))
         .chars()
         .mapToObj(x -> Character.valueOf((char)x));
   }
   return cIn;
 }

 private PrintWriter initCout() {
   if(!cli.write()) {
     cOut = new PrintWriter(System.out, false);
     return cOut;
   }

   List<String> wArgs = cli.getWriteArgs();
   if(wArgs.isEmpty()) {
     System.err.println("err: expects output file path");
     System.exit(1);
   }

   PrintWriter cOut = null;
   Path p = pathFromList(wArgs);
   try {
     OutputStream tmp = Files.newOutputStream(p, CREATE, WRITE, TRUNCATE_EXISTING);
     OutputStreamWriter tmp2 = new OutputStreamWriter(tmp, StandardCharsets.UTF_8);
     BufferedWriter tmp3 = new BufferedWriter(tmp2, 1024);
     cOut = new PrintWriter(tmp3, false);

   } catch (IOException e) {
     e.printStackTrace();
     System.exit(1);
   }

   return cOut;
 }

 private void decode() {
   BidiDict<Integer, Pair<Character, Integer>> dict = Lzw.getUTF8Dict().flip();

  Lzw.decode(dict
    , ge.decode(cIn.flatMap(Streams::streamToBin)))
      .flatMap(x -> x.stream())
      .forEach(cOut::print);
  cOut.println();

  //  ge.decode(cIn.flatMap(Streams::streamToBin))
  //      .map(x -> x.toString() + ",")
  //      .forEach(cOut::print);
  //  cOut.println();

  // cIn.forEach(cOut::print);
  // cOut.println();
 }

 private void encode() {
   BidiDict<Pair<Character, Integer>, Integer> dict = Lzw.getUTF8Dict();

   if(cli.verify()) {
     Lzw.decode(dict.flip() // proof of correctness
       , ge.decode(Streams.mapToChar(Lzw.encode(dict, cIn)
             .flatMap(ge::encode))
             .flatMap(Streams::streamToBin)))
         .flatMap(x -> x.stream())
         .forEach(cOut::print);
     cOut.println();

   } else if(cli.raw()) {
     Lzw.encode(dict, cIn)
         .map(x -> x.toString() + ",")
         .forEach(cOut::print);
     cOut.println();

   } else {
     Streams.mapToChar(Lzw.encode(dict, cIn)
         .flatMap(ge::encode))
         .forEach(cOut::print);
   }
 }

 private static char[] flattenStrL(List<Character> l) {
   int sz = l.size();
   char[] out = new char[sz];

    int i = 0;
    for(Iterator<Character> itr = l.iterator(); itr.hasNext(); i++) {
      out[i] = itr.next();
    }
    return out;
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
