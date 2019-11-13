package encoders.coder;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import encoders.dict.BidiDict;
import encoders.dict.Dict;
import encoders.util.Pair;
import encoders.util.Streams;

public class Lzw<A> implements Coder<A, Integer>{


  private static final Integer LZW_BOT = -1;
  private BidiDict<Pair<A, Integer>, Integer> dict;
  private Optional<Runnable> counter;

  /**
   * Creates a new Lzw coder given an appropriate dictionary. If counter is non-null it will be ran
   * once per received A to be encoded.
   * @param dict
   * @param counter
   */
  public Lzw(BidiDict<Pair<A, Integer>, Integer> dict, Runnable counter) {
    this.dict = dict;
    this.counter = Optional.ofNullable(counter);
  }

  /**
   * Encodes a Stream<A> using lzw algorithm backed by provided dictionary.
   * @return    Stream<Integer>
   */
  @Override
  public Stream<Integer> encode(Stream<A> in) { // returns a lazily generated stream of the compressed sequence

    BiFunction<A, Boolean, Deque<Integer>> gen = new BiFunction<A,Boolean,Deque<Integer>>() {
      private Optional<Integer> lup = Optional.empty();
      private Integer index = LZW_BOT;
      private Deque<Integer> cache = new ArrayDeque<>();

      @Override
      public Deque<Integer> apply(A a, Boolean end) { // lzw enc alg
        lup = dict.lookup(new Pair<>(a, index));
        counter.ifPresent(x -> x.run());

        if(lup.isPresent()) { //lookup longest matching entry
          index = lup.get();

          if(end) {
            cache.addLast(index);
          }
          return cache;
        }

        cache.addLast(index);
        dict.add(new Pair<>(a, index), dict.size()); // add unmatched sym + previous match to dict
        index = dict.lookupUnsafe(new Pair<>(a, LZW_BOT));

        if(end) {
          cache.addLast(index);
        }

        return cache; // emit index
      }
    };
    return Streams.fold(gen, in);
  }

  /**
   * Decodes a Stream<A> using lzw algorithm backed by provided dictionary.
   * @return    Stream<A>
   */
  @Override
  public Stream<A> decode(Stream<Integer> in) { // returns a lazily generated stream of decoded words

    BiFunction<Integer, Boolean, Deque<A>> gen = new BiFunction<Integer,Boolean,Deque<A>>() {
      private BidiDict<Integer, Pair<A,Integer>> dict = Lzw.this.dict.flip();
      private Deque<A> result = new ArrayDeque<>();
      private A prevA = null;
      private int prevI = LZW_BOT;
      private boolean fst = true;


      public Deque<A> apply(Integer i, Boolean end) { // lzw dec alg
        Optional<Pair<A, Integer>> lup = dict.lookup(i);
        Pair<A, Integer> lupV = lup.orElse(new Pair<>(prevA, prevI)); //Optional acts like a Promise here

        while(lupV.snd != LZW_BOT) { // decode word
          result.push(lupV.fst);
          lupV = dict.lookupUnsafe(lupV.snd);
        }
        result.push(lupV.fst);
        prevA = lupV.fst;

        if(fst) {
          fst = false;
        } else {
          dict.add(dict.size(), new Pair<>(lupV.fst, prevI)); // add next entry
        }
        prevI = i;

        return result; // emit word
      }

    };
    return Streams.fold(gen, in);
  }

  /**
   * @return The default index value of root dictionary entries
   */
  public static Integer getLzwBot() {
    return LZW_BOT;
  }

  /**
   * @return Byte dictionary for lzw coding.
   */
  public static BidiDict<Pair<Byte, Integer>, Integer> getDict8() {
    int byteSz = 256;

    BidiDict<Pair<Byte, Integer>, Integer> ret = new Dict<>();
    for(int i = 0; i < byteSz; i++) {
      Pair<Byte, Integer> key = new Pair<>((byte)i, LZW_BOT);
      ret.add(key, i);
    }

    return ret;
  }

  /**
   * @return Bit dictionary for lzw coding.
   */
  public static BidiDict<Pair<Boolean, Integer>, Integer> getDict2() {
    BidiDict<Pair<Boolean, Integer>, Integer> ret = new Dict<>();
    Pair<Boolean, Integer> f = new Pair<>(false, LZW_BOT);
    Pair<Boolean, Integer> t = new Pair<>(true, LZW_BOT);
    ret.add(f, 0);
    ret.add(t, 1);
    return ret;
  }

}
