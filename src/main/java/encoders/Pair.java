package encoders;
import java.util.Objects;

// Instances of this class are immutable if both A and B are immutable
public final class Pair<A, B> {
  public final A a;
  public final B b;

  public Pair(A a, B b) {
    this.a = a;
    this.b = b;
  }

  public Pair<B, A> flip() {
    return new Pair<>(b, a);
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean equals(Object o) {
    if(o instanceof Pair<?, ?>) {
      Pair<A, B> tmp = (Pair<A,B>)o;
      return tmp.a == a && tmp.b == b;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(a, b);
  }
}
