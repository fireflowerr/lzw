package encoders.util;

import encoders.util.unchecked.URunnable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;



public class Streams {
  private static final int BYTE_SZ = 8;

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

  public static Stream<Byte> mapToByte(Stream<Boolean> in) {
    Iterator <Byte> spine = new Iterator<Byte>() {
      final Iterator<Boolean> backing = in.iterator();
      int nxt = -1;
      final static int maxDigVal = 256;
      int i = 0;

      @Override
      public boolean hasNext() {
        if(nxt == -1) {

          int pow = maxDigVal;
          int acc = 0;
          while(backing.hasNext() && i < BYTE_SZ) {
            pow = pow >>> 1;
            if(backing.next()) {
              acc += pow;
            }
            i++;
          }

          if(i == 0) {
            return false;
          }

          while(i < BYTE_SZ) { // all 1 serves as eos marker
            pow = pow >>> 1;
            acc += pow;
            i++;
          }

          i = 0;
          nxt = acc;

          return true;
        } else {
          return false;
        }
      }

      @Override
      public Byte next() {
        if(nxt != -1 || hasNext()) {
          Byte ret = (byte)nxt;
          nxt = -1;
          return ret;
        } else {
          throw new NoSuchElementException();
        }
      }
    };

    Stream<Byte> ret =  StreamSupport.stream(Spliterators.spliteratorUnknownSize
        (spine, Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE), false);
    return ret;
  }

  // Abstracts the action of contiously reading from a BufferedReader to a Stream
  public static Stream<Byte> readFileBytes(InputStream in) throws IOException {
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

    return ret.onClose(URunnable.unchecked(in::close));
  }

}
