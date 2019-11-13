package encoders.util;

import java.util.Objects;

public final class Tuple3<A, B, C> {
  public final A fst;
  public final B snd;
  public final C thd;

  public Tuple3(A fst, B snd, C thd) {
    this.fst = fst;
    this.snd = snd;
    this.thd = thd;
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
    return Objects.hash(fst, snd, thd);
  }

	@Override
	public String toString() {
		return "<" + fst + ", " + snd + ", " + thd + ">";
	}
}
