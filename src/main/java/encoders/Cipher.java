package encoders;

public class Cipher {

  private int shift;
  private static final int MAX_CHAR = (int)Math.pow(2,9) - 1;

  public Cipher(int shift) {
    this.shift = shift;
  }

  public Character encode(Character c) {
    int r = (int)c.charValue();
    r += shift;
    r %= MAX_CHAR;
    return Character.valueOf((char)r);
  }
}
