package encoders;
import encoders.lzw.*;
import encoders.lzw.dict.*;
import encoders.util.*;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;


public final class App {

  public static void main(String[] args) {
    List<String> argL = new ArrayList<>(); // parse cli flags
    for(String arg : args) {
      argL.add(arg);
    }
    boolean encode = argL.remove("-e");
    boolean decode = (decode = argL.remove("-d")) && decode ^ encode;
    boolean raw = argL.remove("-r");
    boolean str = argL.remove("-s");

    if(argL.isEmpty()) {
      System.err.println("err: expects file path as args");
      System.exit(1);
    }

    Stream<Character> cIn = null;
    if(str) {
      cIn = String.join("", argL.toArray(String[]::new))
          .chars()
          .mapToObj(x -> Character.valueOf((char)x));
    } else {

      String root = argL.remove(0);
      Path p = null;
      if(!argL.isEmpty()) {
        p = Paths.get(root, argL.toArray(String[]::new));
      } else {
        p = Paths.get(root);
      }

      try {
        cIn = Streams.readFileChars(p, 1024);
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }

    final GolombRice ge = new GolombRice(4);
    if(decode) {
      BidiDict<Integer, Pair<Character, Integer>> dict = Lzw.getUTF8Dict().flip();

      Stream<Integer> decIn = ge.decode(cIn.flatMap(Streams::streamToBin));
      Lzw.decode(dict, decIn)
          .forEach(System.out::print);
      System.out.println();
      cIn.close();

    } else {
      BidiDict<Pair<Character, Integer>, Integer> dict = Lzw.getUTF8Dict();

      if(raw) {
        Lzw.encode(dict, cIn)
            .map(x -> x.toString() + ",")
            .forEach(System.out::print);
        System.out.println();

      } else {
        Streams.mapToChar(Lzw.encode(dict, cIn)
            .flatMap(ge::encode))
            .forEach(System.out::print);
        System.out.println();
      }
    }

   // BidiDict<Pair<Character, Integer>, Integer> dict = Lzw.getUTF8Dict();
   // int lzwBot = Lzw.getLzwBot();
   // dict.add(new Pair<>('m', lzwBot), 0);
   // dict.add(new Pair<>('e', lzwBot), 1);
   // dict.add(new Pair<>('o', lzwBot), 2);
   // dict.add(new Pair<>('w', lzwBot), 3);
   // BidiDict<Integer, Pair<Character, Integer>> iDict = dict.flip();
   // String str = "meowmoooooowmeowmoeoooomwoooomoew";
   // Stream<Character> in = str.chars().mapToObj(x -> Character.valueOf((char)x));
   // Lzw.decode(iDict, Lzw.encode(dict, in))
   //     .map(App::flattenStrL)
   //     .forEach(System.out::print);
   // System.out.println();
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

  //TODO add flag for elias delta encoding of indexes

}
