package encoders.util;
import java.util.stream.Stream;

@FunctionalInterface
public interface StreamTransformer<A, B> {

  /**
   * Given a Stream<A> return a transformed Stream<B>
   * @param in  Stream<A>
   * @return    Stream<B>
   */
  Stream<B> transform(Stream<A> in);

  /**
   * Allows for the composition of StreamTransformers. Given a StreamTransformer of A -> B and a
   * StreamTransformer of B -> C, return a composite StreamTransformer A -> C
   * @param g StreamTransformer A -> B
   * @param f StreamTransformer B -> C
   * @return  StreamTransformer A -> C
   */
  public static <A, B, C> StreamTransformer<A, C> compose(StreamTransformer<A, B> g, StreamTransformer<B, C> f) {
    return x -> f.transform(g.transform(x));
  }
}
