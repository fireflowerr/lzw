package encoders.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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
              throw new RuntimeException(e);
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

    return wrapClosable(ret, in, null);
  }

  // nessecary to close the backing resource of Stream
  private static <T> Stream<T> wrapClosable(Stream<T> s, AutoCloseable backing, Runnable closeHandler) {
    return new Stream<T>() {
      final AutoCloseable res = backing;
      final Stream<T> str = s;
      Runnable cHandle = closeHandler;

      @Override
      public void close() {
        if(cHandle != null) {
          cHandle.run();
        }

        try {
          res.close();
        } catch(Exception e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public Stream<T> onClose(Runnable closeHandler) {
        return wrapClosable(this, this, closeHandler);
      }

      //-------Below are trivial overrides of Stream interface-------
      @Override
      public boolean isParallel() {
        return str.isParallel();
      }

      @Override
      public Iterator<T> iterator() {
        return str.iterator();
      }

      @Override
      public Stream<T> parallel() {
        return str.parallel();
      }

      @Override
      public Stream<T> sequential() {
        return str.sequential();
      }

      @Override
      public Spliterator<T> spliterator() {
        return str.spliterator();
      }

      @Override
      public Stream<T> unordered() {
        return str.unordered();
      }

      @Override
      public boolean allMatch(Predicate<? super T> predicate) {
        return str.allMatch(predicate);
      }

      @Override
      public boolean anyMatch(Predicate<? super T> predicate) {
        return str.anyMatch(predicate);
      }

      @Override
      public <R, B> R collect(Collector<? super T,B,R> collector) {
        return str.collect(collector);
      }

      @Override
      public <R> R collect(Supplier<R> supplier, BiConsumer<R,? super T> accumulator, BiConsumer<R,R> combiner) {
        return str.collect(supplier, accumulator, combiner);
      }

      @Override
      public long count() {
        return str.count();
      }

      @Override
      public Stream<T> distinct() {
        return str.distinct();
      }

      @Override
      public Stream<T> filter(Predicate<? super T> predicate) {
        return str.filter(predicate);
      }

      @Override
      public Optional<T> findAny() {
        return str.findAny();
      }

      @Override
      public Optional<T> findFirst() {
        return str.findFirst();
      }

      @Override
      public <R> Stream<R> flatMap(Function<? super T,? extends Stream<? extends R>> mapper) {
        return str.flatMap(mapper);
      }

      @Override
      public DoubleStream	flatMapToDouble(Function<? super T,? extends DoubleStream> mapper) {
        return str.flatMapToDouble(mapper);
      }

      @Override
      public IntStream flatMapToInt(Function<? super T,? extends IntStream> mapper) {
        return str.flatMapToInt(mapper);
      }

      @Override
      public LongStream	flatMapToLong(Function<? super T,? extends LongStream> mapper) {
        return str.flatMapToLong(mapper);
      }

      @Override
      public void	forEach(Consumer<? super T> action) {
        str.forEach(action);
      }

      @Override
      public void	forEachOrdered(Consumer<? super T> action) {
        str.forEachOrdered(action);
      }

      @Override
      public Stream<T> limit(long maxSize) {
        return str.limit(maxSize);
      }

      @Override
      public <R> Stream<R> map(Function<? super T,? extends R> mapper) {
        return str.map(mapper);
      }

      @Override
      public DoubleStream	mapToDouble(ToDoubleFunction<? super T> mapper) {
        return str.mapToDouble(mapper);
      }

      @Override
      public IntStream mapToInt(ToIntFunction<? super T> mapper) {
        return str.mapToInt(mapper);
      }

      @Override
      public LongStream	mapToLong(ToLongFunction<? super T> mapper) {
        return str.mapToLong(mapper);
      }

      @Override
      public Optional<T> max(Comparator<? super T> comparator) {
        return str.max(comparator);
      }

      @Override
      public Optional<T> min(Comparator<? super T> comparator) {
        return str.min(comparator);
      }

      @Override
      public boolean noneMatch(Predicate<? super T> predicate) {
        return str.noneMatch(predicate);
      }

      @Override
      public Stream<T> peek(Consumer<? super T> action) {
        return str.peek(action);
      }

      @Override
      public Optional<T> reduce(BinaryOperator<T> accumulator) {
        return str.reduce(accumulator);
      }

      @Override
      public T reduce(T identity, BinaryOperator<T> accumulator) {
        return str.reduce(identity, accumulator);
      }

      @Override
      public <U> U reduce(U identity, BiFunction<U,? super T,U> accumulator, BinaryOperator<U> combiner) {
        return str.reduce(identity, accumulator, combiner);
      }

      @Override
      public Stream<T> skip(long n) {
        return str.skip(n);
      }

      @Override
      public Stream<T> sorted() {
        return str.sorted();
      }

      @Override
      public Stream<T> sorted(Comparator<? super T> comparator) {
        return str.sorted(comparator);
      }

      @Override
      public Object[]	toArray() {
        return str.toArray();
      }

      @Override
      public <A> A[] toArray(IntFunction<A[]> generator) {
        return str.toArray(generator);
      }

    };
  }

}
