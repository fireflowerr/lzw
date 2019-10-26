package encoders.util;

import java.util.NoSuchElementException;
import java.util.Optional;

public class ByteBuilder {
  private final boolean[] arr = new boolean[8];
  private int i = 0;

  public Optional<Byte> append(boolean bit) {
    arr[i] = bit;
    i++;

    if(i == 8) {
      return Optional.of(reduce());
    } else {
      return Optional.empty();
    }
  }

  private Byte reduce() {
    int acc = 0;
    int pow = 1;
    if(arr[7]) {
      acc++;
    }
    i--;
    while(i >= 0) {
      pow *= 2;
      acc += pow;
    }

    return Byte.valueOf((byte)acc);
  }

  public boolean isEmpty() {
    return i > 1;
  }

  public Byte toByte() {
    if(i == 0) {
      throw new NoSuchElementException("ByteBuffer is empty");
    } else {
      return reduce();
    }
  }
}
