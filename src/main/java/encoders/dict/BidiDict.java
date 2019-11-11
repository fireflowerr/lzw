package encoders.dict;
import java.util.Optional;

public interface BidiDict<K, V> { // for dictionaries whose key and value set are unique

  void add(K k, V v);

  Optional<V> lookup(K k);

  V lookupUnsafe(K k);

  BidiDict<V, K> flip();

  public int size();
}
