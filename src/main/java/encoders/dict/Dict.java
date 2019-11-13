package encoders.dict;

import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Optional;

public class Dict<A, B> extends HashMap<A, B> implements BidiDict<A, B> {

  /**
   * GENERATED-STUB
   */
  private static final long serialVersionUID = 7236607238720683668L;

  @Override
  public void add(A a, B b) {
    put(a, b);
  }

  @Override
  public Optional<B> lookup(A a) {
    return Optional.ofNullable(get(a));
  }

  @Override
  public B lookupUnsafe(A a) {
    B ret = get(a) ;
    if(ret == null) {
      throw new NoSuchElementException();
    } else {
      return ret;
    }
  }

  @Override
  public BidiDict<B, A> flip() {
    final BidiDict<B, A> ret = new Dict<>();
    forEach((x, y) -> ret.add(y, x));
    return ret;
  }

}
