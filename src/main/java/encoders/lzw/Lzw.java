package encoders.lzw;

import encoders.lzw.dict.*;
import encoders.util.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;

public class Lzw {


  private static final Integer LZW_BOT = -1;

  public static <A> Stream<Integer> encode( // returns a lazily generated stream of the compressed sequence
      BidiDict<Pair<A, Integer>, Integer> dictionary
    , Stream<A> in) {

    Iterator<Integer> spine = new Iterator<Integer>() {
        BidiDict<Pair<A, Integer>, Integer> dict = dictionary;
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

  public static <A> Stream<List<A>> decode( // returns a lazily generated stream of decoded words
      Class<A> a
    , BidiDict<Integer, Pair<A, Integer>> dictionary
    , Stream<Integer> in) {

    Iterator<Integer> itr = in.iterator();
    if(itr.hasNext()) {
      Integer tmp = itr.next();

      Iterator<List<A>> spine = new Iterator<List<A>>() {
          BidiDict<Integer, Pair<A, Integer>> dict = dictionary;

          //decoder must be primed
          Integer i = tmp;
          Optional<Pair<A, Integer>> lup = dict.lookup(i);
          A prevA = lup.get().fst;
          Deque<A> word = new ArrayDeque<>(Collections.singleton(prevA));
          int wordSz = 1;

          @Override
          public boolean hasNext() {
            return wordSz > 0;
          }

          @Override
          public List<A> next() { //LZW decode alg

            List<A> ret = new ArrayList<>(wordSz); // return prev match
            while(wordSz > 0) {
              ret.add(word.pop());
              wordSz--;
            }

            if(itr.hasNext()) {
              Integer prevI = i;
              i = itr.next();
              lup = dict.lookup(i);
              Pair<A, Integer> lupV = lup.orElse(new Pair<>(prevA, prevI)); //Optional acts like a Promise here

              while(lupV.snd != LZW_BOT) { // decode word
                word.push(lupV.fst);
                wordSz++;
                lupV = dict.lookupUnsafe(lupV.snd);
              }
              wordSz++;
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

  public static BidiDict<Pair<Character, Integer>, Integer> getUTF8Dict() {
    final int utfMax = 256;
    BidiDict<Pair<Character, Integer>, Integer> ret = new Dict<>();

    for(int i = 0; i < utfMax; i++) {
      Pair<Character, Integer> key = new Pair<>((char)i, LZW_BOT);
      ret.add(key, i);
    }

    return ret;
  }

}
