package encoders;

import java.util.HashMap;
import java.util.Optional;

public class Dict<A> extends HashMap<Pair<A, Integer>, Integer> implements IDictionary<A, Integer, Integer> {

  @Override
  public boolean add(A a, Integer i, Integer v) {
    put(new Pair<>(a, i), v);
    return true;
  }

  @Override
  public Optional<Integer> lookup(A a, Integer i) {
    return Optional.ofNullable(get(new Pair<>(a, i)));
  }


  @Override
  public Integer nextIndex() {
    return size();
  }

}
