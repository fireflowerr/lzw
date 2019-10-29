package encoders.util.unchecked;

import java.io.IOException;
import java.io.UncheckedIOException;

@FunctionalInterface
public interface URunnable <E extends Exception> {
  void run () throws E;

  static <E extends Exception> Runnable unchecked(URunnable<E> u) {

    return () -> {
      try {
        u.run();
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
