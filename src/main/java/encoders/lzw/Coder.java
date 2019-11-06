package encoders.lzw;

import java.util.stream.Stream;

public abstract class Coder<A, B> {

  public static <A, B, C> Coder<A, C> compose(Coder<A,B> g, Coder<B,C> f) {
    return new BaseCoder<>(
        StreamTransformer.compose(g.getEncoder(), f.getEncoder())
      , StreamTransformer.compose(f.getDecoder(), g.getDecoder()));
  }

  public <C> Coder<A, C> compose(Coder<B,C> f) { // convience method to allow invocation chaining
    return compose(this, f);
  }

  public abstract Stream<B> encode(Stream<A> in);
  public abstract Stream<A> decode(Stream<B> in);
  public abstract StreamTransformer<A, B> getEncoder();
  public abstract StreamTransformer<B, A> getDecoder();
}
