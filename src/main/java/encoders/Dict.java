package encoders;

import java.util.HashMap;
import java.util.Optional;

public class Dict<A, B> extends HashMap<A, B> implements BidiDict<A, B> {

  @Override
  public void add(A a, B b) {
    put(a, b);
  }

  @Override
  public Optional<B> lookup(A a) {
    return Optional.ofNullable(get(a));
  }

  @Override
  public BidiDict<B, A> flip() {
    final BidiDict<B, A> ret = new Dict<>();
    forEach((x, y) -> ret.add(y, x));
    return ret;
  }
}
