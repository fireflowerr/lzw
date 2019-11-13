package encoders.dict;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * A reversible dictionary with a bijective mapping between keys and values.
 */
public interface BidiDict<K, V> { // for dictionaries whose key and value set are unique

  /**
   * Adds key/value pair to dictionary.
   */
  void add(K k, V v);

  /**
   * @param k
   * @return Optional.of(value) if k is a valid key else return empty Optional.
   */
  Optional<V> lookup(K k);

  /**
   * @param k
   * @return V if k is a valid key else throws a NoSuchElementException
   */
  V lookupUnsafe(K k) throws NoSuchElementException;

  /**
   * @return A flipped dictionary where value set becomes key set and key set becomes value set.
   */
  BidiDict<V, K> flip();

  /**
   * @return The number of entries in the dictionary.
   */
  public int size();
}
