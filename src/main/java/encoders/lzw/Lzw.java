package encoders.lzw;

import encoders.lzw.dict.*;
import encoders.util.*;

import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;

public class Lzw<A> implements Coder<A, Integer>{


  private static final Integer LZW_BOT = -1;
  private BidiDict<Pair<A, Integer>, Integer> dict;
  private Optional<Runnable> counter;

  public Lzw(BidiDict<Pair<A, Integer>, Integer> dict, Runnable counter) {
    this.dict = dict;
    this.counter = Optional.ofNullable(counter);
  }

  @Override
  public Stream<Integer> encode(Stream<A> in) { // returns a lazily generated stream of the compressed sequence

    Iterator<Integer> spine = new Iterator<Integer>() {
        Iterator<A> itr = in.iterator();
        A a = null;
        Integer index = LZW_BOT;
        Optional<Integer> lup = Optional.of(index);
        boolean alive = itr.hasNext();

        @Override
        public boolean hasNext() {
          return alive;
        }

        @Override
        public Integer next() {

          // LZW encode alg
          while(lup.isPresent()) { //find the longest matching dict entry

            index = lup.get();
            if((alive = itr.hasNext())) {
              a = itr.next();
              counter.ifPresent(x -> x.run());
              lup = dict.lookup(new Pair<>(a, index));
            } else {
              lup = Optional.empty();
            }
          }
          dict.add(new Pair<>(a, index), dict.size()); //add previous key + unmatched sym to dict
          lup = dict.lookup(new Pair<>(a, LZW_BOT)); //repeat lookup starting from unmatched

          return index; // emit index
        }
    };

    return StreamSupport.stream(Spliterators.spliteratorUnknownSize
        (spine, Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE), false);
  }

  @Override
  public Stream<A> decode(Stream<Integer> in) { // returns a lazily generated stream of decoded words

    BidiDict<Integer, Pair<A, Integer>> tmpDict = this.dict.flip();
    Iterator<Integer> itr = in.iterator();
    if(itr.hasNext()) {
      Integer tmp = itr.next();

      Iterator<A> spine = new Iterator<A>() {

          BidiDict<Integer, Pair<A, Integer>> dict = tmpDict;
          //decoder must be primed
          Integer i = tmp;
          Optional<Pair<A, Integer>> lup = dict.lookup(i);
          A prevA = lup.get().fst;
          Deque<A> word = new ArrayDeque<>(Collections.singleton(prevA));

          @Override
          public boolean hasNext() {
            return !word.isEmpty();
          }

          @Override
          public A next() { //LZW decode alg

            A ret = word.pop(); // return prev match
            if(!word.isEmpty()) {
              return ret;
            }

            if(itr.hasNext()) {
              Integer prevI = i;
              i = itr.next();
              lup = dict.lookup(i);
              Pair<A, Integer> lupV = lup.orElse(new Pair<>(prevA, prevI)); //Optional acts like a Promise here

              while(lupV.snd != LZW_BOT) { // decode word
                word.push(lupV.fst);
                lupV = dict.lookupUnsafe(lupV.snd);
              }
              word.push(lupV.fst);
              prevA = lupV.fst;

              dict.add(dict.size(), new Pair<>(lupV.fst, prevI)); // add next entry
            }

            return ret;
          }
      };
      return StreamSupport.stream(Spliterators.spliteratorUnknownSize
          (spine, Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE), false);
    } else {
      return Stream.empty();
    }
  }

  public static Integer getLzwBot() {
    return LZW_BOT;
  }

  public static BidiDict<Pair<Byte, Integer>, Integer> getDict8() {
    int byteSz = 256;

    BidiDict<Pair<Byte, Integer>, Integer> ret = new Dict<>();
    for(int i = 0; i < byteSz; i++) {
      Pair<Byte, Integer> key = new Pair<>((byte)i, LZW_BOT);
      ret.add(key, i);
    }

    return ret;
  }

  public static BidiDict<Pair<Boolean, Integer>, Integer> getDict2() {
    BidiDict<Pair<Boolean, Integer>, Integer> ret = new Dict<>();
    Pair<Boolean, Integer> f = new Pair<>(false, LZW_BOT);
    Pair<Boolean, Integer> t = new Pair<>(true, LZW_BOT);
    ret.add(f, 0);
    ret.add(t, 1);
    return ret;
  }

}
