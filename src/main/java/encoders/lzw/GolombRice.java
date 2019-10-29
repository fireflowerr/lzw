package encoders.lzw;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class GolombRice {

  private int k;
  private int m;

  public GolombRice(int k) {
    setTunable(k);
  }

  public void setTunable(int k) {
    this.k = k;

    int acc = 1;
    for(int i = 0; i < k; i++) {
      acc *= 2;
    }
    m = acc;
  }

  public Stream<Boolean> encode(Integer x) {
    Stream.Builder<Boolean> ret = Stream.builder();

    for(int q = x / m; q > 0; q--) {
      ret.add(Boolean.TRUE);
    }
    ret.add(Boolean.FALSE);

    Deque<Boolean> acc = new ArrayDeque<>();
    for(int i = 0; i < k; i++) {
      if(x % 2 == 0) {
        acc.push(Boolean.FALSE);
      } else {
        acc.push(Boolean.TRUE);
      }

      x = x >>> 1;
    }

    while(!acc.isEmpty()) {
      ret.add(acc.pop());
    }

    return ret.build();
  }

  public Stream<Integer> decode(Stream<Boolean> in) {
    Iterator<Integer> itr = new Iterator<Integer>() { // will cause exception if imput is malformed
      final Iterator<Boolean> backing = in.iterator();
      final int mRef = m;
      final int kRef = k;
      Integer nxt = null;

      @Override
      public boolean hasNext() {
        if(nxt == null) {
          if(backing.hasNext()) {

            int q = 0;
            try { // GolombRice alg
              while(backing.next()) {
                q++;
              }

              int tmp = mRef >>> 1;
              int r = 0;

              int i = 0;
              for(; i < kRef; i++) {
                if(backing.next()) {
                  r += tmp;
                }
                tmp = tmp >>> 1;
              }
              nxt = q * m + r;
              return true;

            } catch (NoSuchElementException e) {
              return false;
            }
          } else {
            return false;
          }
        } else {
          return true;
        }
      }

      @Override
      public Integer next() {
        if(nxt != null || hasNext()) {
          Integer ret = nxt;
          nxt = null;
          return ret;
        } else {
          throw new NoSuchElementException("stream is exhausted");
        }
      }
    };

    Stream<Integer> ret =  StreamSupport.stream(Spliterators.spliteratorUnknownSize
        (itr, Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE), false);
    return ret;
  }
}
