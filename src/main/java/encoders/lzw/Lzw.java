package encoders.lzw;

import encoders.lzw.dict.*;
import encoders.util.*;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.ArrayDeque;
import java.util.Deque;

public class Lzw {


  private static final Integer LZW_BOT = -1;

  public static <A> Stream<Integer> encode( //TODO consider replacing with generator backed lazy stream for scalability
      BidiDict<Pair<A, Integer>, Integer> dict
    , Stream<A> in) {

    Iterator<A> itr = in.iterator();
    A a = null;
    Integer index = LZW_BOT;
    Optional<Integer> lup = Optional.of(index);
    Stream.Builder<Integer> acc = Stream.builder();

    // LZW encode alg
    boolean alive = true;
    while(alive) {

      while(lup.isPresent()) { //find the longest matching dict entry

        index = lup.get();
        if((alive = itr.hasNext())) {
          a = itr.next();
          lup = dict.lookup(new Pair<>(a, index));
        } else {
          lup = Optional.empty();
        }
      }

      acc.add(index); //add dict key to accumulator
      dict.add(new Pair<>(a, index), dict.size()); //add previous key + unmatched sym to dict
      lup = dict.lookup(new Pair<>(a, LZW_BOT)); //repeat lookup starting from unmatched
    }

    return acc.build();
  }

  public static <A> Stream<A> decode(
      BidiDict<Integer, Pair<A, Integer>> dict
    , Stream<Integer> in) {

    Iterator<Integer> itr = in.iterator();
    Stream.Builder<A> acc = Stream.builder();

    // algorithm must be primed
    Integer i = 0;
    Optional<Pair<A, Integer>> lup = null;
    A prevA = null;
    if(itr.hasNext()) {
      i = itr.next();
      lup = dict.lookup(i);
      acc.add((prevA = lup.get().fst));
    }

    // LZW decode alg
    Deque<A> word = new ArrayDeque<>();
    while(itr.hasNext()) {
      Integer prevI = i;
      i = itr.next();
      lup = dict.lookup(i);
      Pair<A, Integer> lupV = lup.orElse(new Pair<>(prevA, prevI)); //Optional acts like a Promise here

      while(lupV.snd != LZW_BOT) { // decode word
        word.push(lupV.fst);
        lupV = dict.lookupUnsafe(lupV.snd);
      }
      prevA = lupV.fst;

      acc.add(lupV.fst); // add decoded word to acc
      while(!word.isEmpty()) { // based on openJDK source clear is O(n) so it is more effecient to combine tasks here
        acc.add(word.pop());
      }
      dict.add(dict.size(), new Pair<>(lupV.fst, prevI)); // add next entry

    }
    return acc.build();
  }

  public static Integer getLzwBot() {
    return LZW_BOT;
  }

}
