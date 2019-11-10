package encoders.app.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import encoders.util.Pair;
import encoders.util.Tuple3;

public final class CliBuilder {

  private HashMap<String, Integer> flagQts = new HashMap<>();
  private HashMap<Character, String> shortNames = new HashMap<>();
  private HashMap<String, Pair<Boolean, List<String>>> output = new HashMap<>();
  private String dflt;

  public CliBuilder() {}

  public CliBuilder init(List<Pair<String, Integer>> l) {
    l.forEach(x -> addEntry(x.fst, x.snd));
    return this;
  }

  public CliBuilder init3(List<Tuple3<String, Integer, Character>> l) {
    l.forEach(x -> addEntry(x.fst, x.snd, x.thd));
    return this;
  }

  public CliBuilder addEntry(String flag, Integer argQt) {
    flagQts.put(flag, argQt);
    output.put(flag, new Pair<>(false, new ArrayList<>(2)));
    return this;
  }

  public CliBuilder addEntry(String flag, Integer argQt, Character shortName) {
    addEntry(flag, argQt);
    shortNames.put(shortName, flag);
    return this;
  }

  public CliBuilder addShortName(String flag, Character shortName) {
    shortNames.put(shortName, flag);
    return this;
  }

  public CliBuilder setDefaultFlag(String flag) {
    dflt = flag;
    return this;
  }

  @SuppressWarnings("unchecked")
  public Cli build(String[] args) throws NoSuchFlagException {

    String ownerName = dflt;
    Pair<Boolean, List<String>> tmpDflt = output.get(dflt);
    Supplier<List<String>> getDflt = () -> tmpDflt == null ? null : tmpDflt.snd;
    List<String> argOwner = getDflt.get();
    int argsOwed = -1;
    for(String arg : args) {

      if(arg.charAt(0) == '-') {

        String flagName = null;
        if(arg.charAt(1) == '-' && arg.length() > 2) {
          flagName = arg.substring(2);
        } else {
          flagName = shortNames.get(arg.charAt(1));
        }

        Pair<Boolean, List<String>> tmp = output.get(flagName);
        if(tmp == null) {
          throw new NoSuchFlagException(arg);
        }

        int tmpQt = flagQts.get(flagName);
        if(tmpQt != 0) {
          ownerName = flagName;
          argOwner = output.get(flagName).snd;
          argsOwed = tmpQt;
          output.put(flagName, new Pair<>(true, argOwner));
        } else {
          output.put(flagName, new Pair<>(true, Collections.EMPTY_LIST));
        }


      } else {

        if(argOwner == null) {
          throw new NoSuchFlagException(arg);
        }

        argOwner.add(arg);
        argsOwed--;

        if(argsOwed == 0) {
          output.put(ownerName, new Pair<>(true, argOwner));
          ownerName = dflt;
          argOwner = getDflt.get();
          argsOwed = -1;
        }
      }
    }

    return new Cli(output, shortNames);
  }

}
