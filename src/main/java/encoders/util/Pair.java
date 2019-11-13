package encoders.util;

import java.util.Objects;

// Instances of this class are immutable if both A and B are immutable
public final class Pair<A, B> {
  public final A fst;
  public final B snd;

  public Pair(A fst, B snd) {
    this.fst = fst;
    this.snd = snd;
  }

  /**
   * @return A new pair P such that P.fst = snd AND p.snd = fst
   */
  public Pair<B, A> flip() {
    return new Pair<>(snd, fst);
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean equals(Object o) {
    if(o instanceof Pair<?, ?>) {
      Pair<A, B> tmp = (Pair<A,B>)o;
      return tmp.fst == fst && tmp.snd == snd;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(fst, snd);
  }

	@Override
	public String toString() {
		return "<" + fst + ", " + snd + ">";
	}
}
