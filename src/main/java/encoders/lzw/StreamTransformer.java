package encoders.lzw;
import java.util.stream.Stream;

@FunctionalInterface
public interface StreamTransformer<A, B> {

  Stream<B> transform(Stream<A> in);

  public static <A, B, C> StreamTransformer<A, C> compose(StreamTransformer<A, B> g, StreamTransformer<B, C> f) {
    return x -> f.transform(g.transform(x));
  }
}
