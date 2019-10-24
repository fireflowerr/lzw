package encoders;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collector;
import java.util.stream.Stream;


public final class App
{

    public static void main( String[] args )
    {
      Path p = Paths.get("test.txt");
      System.out.println(p.toAbsolutePath().toString());

      Stream<Character> fIn = null;
      try {
        fIn = Streams.readFileChars(p, 256, StandardCharsets.UTF_8);
      } catch(IOException e) {
        e.printStackTrace();
      }
      if(fIn != null) {
      //System.out.println(fIn.findFirst().get());
      //  String result = fIn .collect(Collector.of(
      //      StringBuilder::new
      //    , StringBuilder::append
      //    , StringBuilder::append
      //    , StringBuilder::toString));

      //  System.out.println(result);
      }
    }

    //out
}
