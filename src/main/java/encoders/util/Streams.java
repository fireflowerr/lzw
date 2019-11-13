package encoders.util;


import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;



public class Streams {
  private static final int BYTE_SZ = 8;

  /**
   * Converts a Byte into a stream of bits represented by booleans.
   * @param b byte
   * @return  Stream<Boolean>
   */
  public static Stream<Boolean> streamToBin(Byte b) {
    Deque<Boolean> acc = new ArrayDeque<>();
    Stream.Builder<Boolean> ret = Stream.builder();

    int x = b;
    for(int i = 0; i < BYTE_SZ; i++) {
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

  /**
   * Folds a stream of bits represented by Booleans into a Stream of bytes.
   * @param in  bit stream
   * @return    byte stream
   */
  public static Stream<Byte> foldToBytes(Stream<Boolean> in) {
    BiFunction<Boolean,Boolean,Deque<Byte>> gen = new BiFunction<Boolean,Boolean,Deque<Byte>>() {
      final static int maxDigVal = 256;
      int pow = maxDigVal;
      int i = 0;
      int acc = 0;
      Deque<Byte> result = new ArrayDeque<>(1);

      public Deque<Byte> apply(Boolean bit, Boolean end) {
        pow = pow >>> 1;

        if(bit) {
          acc += pow;
        }

        i++;
        if(end) {
          while(i < BYTE_SZ) { // all 1 serves as eos marker
            pow = pow >>> 1;
            acc += pow;
            i++;
          }
        }

        if(i == BYTE_SZ) {
          result.push((byte)acc);
          acc = 0;
          i = 0;
          pow = maxDigVal;
        }
        return result;
      }
    };

    return Streams.fold(gen, in);
  }

  /**
   * Abstracts the action of contiously reading from an InputStream to a lazy Stream<Byte>.
   * @param in  backing input stream
   * @return    Lazy Stream<Byte>
   * @throws IOException
   */
  public static Stream<Byte> inputstreamToStream(InputStream in) throws IOException {
    Iterator<Byte> itr = new Iterator<Byte>() {
       int nxt = -1;
       InputStream bb = in;

        @Override
        public boolean hasNext() {

          if(nxt == -1) {

            try {
              nxt = bb.read();
              if(nxt == -1) {
                return false;
              }

              return true;
            } catch(IOException e) {
              throw new UncheckedIOException(e);
            }

          } else {
            return true;
          }
        }

        @Override
        public Byte next() {
          if(nxt != -1 || hasNext()) { // leverage short circuting here
            Byte tmp = (byte)nxt;
            nxt = -1;
            return tmp;
          } else {
            throw new NoSuchElementException();
          }
        }
    };

    Stream<Byte> ret =  StreamSupport.stream(Spliterators.spliteratorUnknownSize
        (itr, Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE), false);

    return ret.onClose(() -> {
        try {
          in.close();
        }catch(IOException e) {
          throw new UncheckedIOException(e);
        }
    });
  }

  /**
   *  Takes a BiFunction that iterates over a Stream<A> passing each element to the BiFunction as
   *  it's first argument. The second argument describes wheather the end of the backing Stream<A>
   *  has been reached. May return an empty passing each element to the BiFunction as it's first
   *  argument. The second argument describes wheather the end of the backing Stream<A> has been
   *  reached. The returned Deque will be emptied and placed into the output stream lazily. An empty
   *  Deque may be returned and iteration will continue.
   *
   * @param gen generator function
   * @param in  stream to transform
   * @return    transformed stream
   */
  public static <A,B> Stream<B> fold(BiFunction<A,Boolean,Deque<B>> gen, Stream<A> in) {

    Iterator<B> spine = new Iterator<B>() {
      private Iterator<A> itr = in.iterator();
      private Deque<B> cache = new ArrayDeque<>(0);

      @Override
      public boolean hasNext() {
        boolean alive = itr.hasNext();
        while(cache.isEmpty() && alive) {
          cache = gen.apply(itr.next(), !(alive = itr.hasNext()));
        }

        return !cache.isEmpty();
      }

      @Override
      public B next() {
        boolean alive = true;
        while(cache.isEmpty() && alive) {
          cache = gen.apply(itr.next(), !(alive = itr.hasNext()));
        }

        return cache.pop();
      }
    };
    Stream<B> ret =  StreamSupport.stream(Spliterators.spliteratorUnknownSize
        (spine, Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE), false);
    return ret;
  }

}
