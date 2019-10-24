package encoders;
import encoders.lzw.*;
import encoders.lzw.dict.*;
import encoders.util.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collector;
import java.util.stream.Stream;


public final class App {

    public static void main( String[] args ) {
      if(args.length == 0) {
        System.err.println("err: expects file path as args");
        System.exit(1);
      }
      String root = args[0];

      if(args.length > 1) {
        String[] tmp = args;
        args = new String[args.length - 1];
        System.arraycopy(tmp, 1, args, 0, tmp.length - 1);
      }
      for(String s : args) {
        System.out.println(s);
      }
      Path p = Paths.get(root, args);

      Stream<Character> fIn = null;
      try {
        fIn = Streams.readFileChars(p, 1024);
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
      }

      //  System.out.println(result);
      String test = "meowmeeeoooowmeowmowmeoowmoo";
      BidiDict<Pair<Character, Integer>, Integer> dict = new Dict<>();
      int lzwBot = Lzw.getLzwBot();
      dict.add(new Pair<>('m', lzwBot), 0);
      dict.add(new Pair<>('e', lzwBot), 1);
      dict.add(new Pair<>('o', lzwBot), 2);
      dict.add(new Pair<>('w', lzwBot), 3);
      BidiDict<Integer, Pair<Character, Integer>> iDict = dict.flip();

      Stream<Character> in = test.chars().mapToObj(x -> Character.valueOf((char)x));
      Lzw.decode(iDict, Lzw.encode(dict, in))
          .forEach(System.out::print);
      System.out.println();
    }

    //out
}
