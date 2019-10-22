package encoders;
import java.util.Optional;

interface IDictionary<K, I, V> { // things that have a (key,index)-value assoc

  Optional<V> lookup(K k, I i); // returns the result of the lookup if the entry is present

  boolean add(K k, I i, V v); // returns true if the the dictionary changed as a result of this call

  I nextIndex(); // returns the first available index from the base
}

