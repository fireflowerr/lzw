package encoders.coder;

import java.util.stream.Stream;

import encoders.util.StreamTransformer;

/**
 * This class defines a coder a reversible pair of StreamTransformer<A,B> and StreamTransformer<B,A>
 * (encode and decode respectively). decode(encode) is expected to return an approximation of the
 * original Stream. If decode(encode) = ID the coder is said to be lossless.
 * @param <A>
 * @param <B>
 */
public interface Coder<A, B> {

  /**
   * Given a Coder<A,B> and a Coder<B,C> return a new composite Coder<A,C>.
   * @param g Coder<A,B>
   * @param f Coder<B,C>
   * @return  Coder<A,C>
   */
  public static <A, B, C> Coder<A, C> compose(Coder<A,B> g, Coder<B,C> f) {
    return new BaseCoder<>(
        StreamTransformer.compose(g::encode, f::encode)
      , StreamTransformer.compose(f::decode, g::decode));
  }

  /**
   * Convenience  method to allow chaining. Equivalent to compose(this, f).
   * @param f Coder<B,C>
   * @return  Coder<A,C>
   */
  public default <C> Coder<A, C> compose(Coder<B,C> f) {
    return compose(this, f);
  }

  /**
   * Transforms Stream<A> to Stream<B>
   * @param in Stream<A>
   * @return Stream<B>
   */
  Stream<B> encode(Stream<A> in);

  /**
   * Transforms Stream<B> to Stream<A>
   * @param in Stream<B>
   * @return Stream<A>
   */
  Stream<A> decode(Stream<B> in);
}
