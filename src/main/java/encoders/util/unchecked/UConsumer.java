package encoders.util.unchecked;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

@FunctionalInterface
public interface UConsumer <T, E extends Exception> {

  public void accept(T t) throws E;

  public static <T, E extends Exception> Consumer<T>  unchecked(UConsumer<T, E> u) {
    return x -> {
      try {
        u.accept(x);
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
