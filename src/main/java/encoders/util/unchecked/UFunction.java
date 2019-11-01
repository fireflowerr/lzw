package encoders.util.unchecked;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Function;

@FunctionalInterface
public interface UFunction <T, R, E extends Exception> {
  public R apply(T t) throws E;

  public static <T, R, E extends Exception> Function<T, R> unchecked(UFunction<T, R, E> u) {
    return x -> {
      try {
         return u.apply(x);
      } catch(Exception e) {

        if(e instanceof IOException) {
          throw new UncheckedIOException((IOException)e);
        } else {
          throw new RuntimeException(e);
        }
      }
    };
  }
}
