package encoders;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Stream;


/**
 * Hello world!
 *
 */
public final class App
{
    public static void main( String[] args )
    {
      Dict<Character> dict = new Dict<>();
      dict.add('a', -1, 0);
      dict.add('b', -1, 1);

      String test = "ababababa";
      Stream<Character> tStream = test.chars()
          .mapToObj(x -> Character.valueOf((char)x));

      String result = encode(dict, -1, tStream)
          .collect(Collector.of(
                StringBuilder::new
              , StringBuilder::append
              , StringBuilder::append
              , StringBuilder::toString));

      System.out.println(result);
    }

    //out
    public static <A, B>  Stream<B> encode(IDictionary<A, B, B> dict, B bot, Stream<A> in) {
      Iterator<A> itr = in.iterator();

      A a = null;
      B index = bot;
      Optional<B> lup = Optional.of(index);
      Stream.Builder<B> acc = Stream.builder();

      // LZW alg
      while(itr.hasNext()) {

        if(lup.isEmpty()) {
          acc.add(index);
          dict.add(a, index, dict.nextIndex());
          lup = dict.lookup(a, bot);
        } else {
          a = itr.next();
          index = lup.get();
          lup = dict.lookup(a, index);
        }
      }
      lup.ifPresent(acc::add);

      return acc.build();
    }
}
