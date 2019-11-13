package encoders.app.cli;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import encoders.util.Pair;

public class Cli {

  private final HashMap<String, Pair<Boolean, List<String>>> flags;
  private final HashMap<Character, String> shortNames;

  public Cli(HashMap<String, Pair<Boolean, List<String>>> flags
    , HashMap<Character, String> shortNames) {

    this.flags = new HashMap<>(flags);
    this.shortNames = new HashMap<>(shortNames);
  }

  public boolean isSet(String flag) {
    Pair<Boolean, List<String>> tmp = getOrThrow(flag, flags.get(flag));
    return tmp.fst;
  }

  public boolean isSet(Character flag) {
    String flagName = getOrThrow(flag, shortNames.get(flag));
    Pair<Boolean, List<String>> tmp = getOrThrow(flagName, flags.get(flagName));
    return tmp.fst;
  }

  public List<String> getArgs(String flag) {
    Pair<Boolean, List<String>> tmp = getOrThrow(flag, flags.get(flag));
    return new ArrayList<>(tmp.snd);
  }

  public List<String> getArgs(Character flag) {
    String flagName = getOrThrow(flag, shortNames.get(flag));
    Pair<Boolean, List<String>> tmp = getOrThrow(flagName, flags.get(flagName));
    return new ArrayList<>(tmp.snd);
  }

  private static <T, R> R getOrThrow(T t, R r) {
    if(r == null) {
      throw new NoSuchFlagException(t.toString());
    }
    return r;
  }
}
