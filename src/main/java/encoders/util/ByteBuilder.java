package encoders.util;

import java.util.NoSuchElementException;
import java.util.Optional;

public class ByteBuilder {
  private static final int CHAR_BITS = 16;
  private static final int M;
  static {
    int pow = 1;
    for(int i = 0; i < CHAR_BITS; i++) {
      pow *= 2;
    }
    M = pow;
  }

  private final boolean[] arr = new boolean[CHAR_BITS];
  private int i = 0;

  public Optional<Character> append(boolean bit) {
    arr[i] = bit;
    i++;

    if(i == CHAR_BITS) {
      return Optional.of(reduce());
    } else {
      return Optional.empty();
    }
  }

  private Character reduce() { // TODO figure out what's wrong with this class
    int acc = 0;
    int pow = M;
    while(i > 0) {
      pow = pow >>> 1;
      if(arr[CHAR_BITS - (i--)]) {
        acc += pow;
      }
    }

    return Character.valueOf((char)acc);
  }

  public boolean isEmpty() {
    return i > 1;
  }

  public Character toCharacter() {
    if(i == 0) {
      throw new NoSuchElementException("ByteBuffer is empty");
    } else {
      return reduce();
    }
  }

}
