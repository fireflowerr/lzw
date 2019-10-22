package encoders;

import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Stream;

public final class App
{
    public static void main( String[] args )
    {
      Dict<Pair<Character, Integer>, Integer> dict = new Dict<>();
      dict.add(new Pair<>('a', -1), 0);
      dict.add(new Pair<>('b', -1), 1);

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
    public static <A> Stream<Integer> encode(
        BidiDict<Pair<A, Integer>, Integer> dict
      , Integer bot, Stream<A> in) {

      Iterator<A> itr = in.iterator();
      A a = null;
      Integer index = bot;
      Optional<Integer> lup = Optional.of(index);
      Stream.Builder<Integer> acc = Stream.builder();

      // LZW alg
      while(itr.hasNext()) {

        if(lup.isEmpty()) {
          acc.add(index);
          dict.add(new Pair<A, Integer>(a, index), Integer.valueOf(dict.size()));
          lup = dict.lookup(new Pair<>(a, bot));
        } else {
          a = itr.next();
          index = lup.get();
          lup = dict.lookup(new Pair<>(a, index));
        }
      }
      lup.ifPresent(acc::add);

      return acc.build();
    }

    //public static <B, A> Stream<B> decode(JDictionary<B, B, A> dict, Stream<B> in) {
    //    return null;
    //}
}
