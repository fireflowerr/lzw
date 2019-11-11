package encoders.coder;

import java.util.stream.Stream;

import encoders.util.StreamTransformer;

public class BaseCoder<A,B> implements Coder<A,B> {

  private StreamTransformer<A, B> encoder;
  private StreamTransformer<B, A> decoder;

  public BaseCoder(StreamTransformer<A, B> encoder
    , StreamTransformer<B, A> decoder) {

    this.encoder = encoder;
    this.decoder = decoder;
  }

  @Override
  public Stream<B> encode(Stream<A> in) {
    return encoder.transform(in);
  }

  @Override
  public Stream<A> decode(Stream<B> in) {
    return decoder.transform(in);
  }

}
