package encoders.coder;

import java.util.stream.Stream;

import encoders.util.StreamTransformer;

public interface Coder<A, B> {

  public static <A, B, C> Coder<A, C> compose(Coder<A,B> g, Coder<B,C> f) {
    return new BaseCoder<>(
        StreamTransformer.compose(g::encode, f::encode)
      , StreamTransformer.compose(f::decode, g::decode));
  }

  public default <C> Coder<A, C> compose(Coder<B,C> f) { // convience method to allow invocation chaining
    return compose(this, f);
  }

  Stream<B> encode(Stream<A> in);
  Stream<A> decode(Stream<B> in);
}
