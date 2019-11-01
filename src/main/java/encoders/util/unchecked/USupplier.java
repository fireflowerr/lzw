package encoders.util.unchecked;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Supplier;

@FunctionalInterface
public interface USupplier <T, E extends Exception> {

  T get() throws E;

  public static <T> Supplier<T> unchecked(USupplier<T, ?> u) {
    return () -> {
      try {
        return u.get();
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
