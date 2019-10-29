package encoders.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import encoders.util.unchecked.URunnable;

import static java.nio.file.StandardOpenOption.READ;

public class Streams {
  private static final int BYTE_SZ = 8;
  //
  public static <T> Stream<T> streamOptional(Optional<T> opt) {
    return opt.isPresent() ? Stream.of(opt.get()) : Stream.empty();
  }

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

  // helper method to get around finality restrection in anonymous classes
  private static InputStream getByteIn(Path p, OptionalInt bSz) throws IOException {
    InputStream in = null;
    if(bSz.isPresent()) {
      in = new BufferedInputStream(Files.newInputStream(p, READ)
        , bSz.getAsInt());
    } else {
      in = new BufferedInputStream(Files.newInputStream(p, READ));
    }
     return in;
  }

  // Abstracts the action of contiously reading from a BufferedReader to a Stream
  public static Stream<Byte> readFileBytes(Path p, OptionalInt bSz) throws IOException {
   final InputStream in = getByteIn(p, bSz);
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
