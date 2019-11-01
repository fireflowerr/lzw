package encoders.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CliParser {

  private char flagSym = '-';
  private static Map<String, Character> longFlagNames = new HashMap<>();
  private Map<Character, Boolean> shortFlagNames = new HashMap<>();
  private List<String> fArgs = new ArrayList<>();
  private List<String> wArgs = new ArrayList<>();
  private String kArg;
  static {
    longFlagNames.put("file", 'f');
    longFlagNames.put("write", 'w');
    longFlagNames.put("decode", 'd');
    longFlagNames.put("binary", 'b');
    longFlagNames.put("tunable", 'k');
    longFlagNames.put("raw", 'r');
    longFlagNames.put("identity", 'i');
    longFlagNames.put("verbose", 'v');
    longFlagNames.put("silent" , 's');
  }

  public CliParser(String[] args) {
    shortFlagNames.put('f', false);
    shortFlagNames.put('r', false);
    shortFlagNames.put('d', false);
    shortFlagNames.put('w', false);
    shortFlagNames.put('i', false);
    shortFlagNames.put('b', false);
    shortFlagNames.put('k', false);
    shortFlagNames.put('v', false);
    shortFlagNames.put('s', false);

    int seen = 0;
    int prevSeen = 0;
    int l = args.length;
    for(int i = 0; i < l; i++) {
      String s = args[i] ;

      if(s.charAt(0) == flagSym) {
char key = '\0'; if(s.charAt(1) == flagSym) {
          key = longFlagNames.get(s.substring(2));
        } else {
          key = s.charAt(1);
        }
        shortFlagNames.put(key, true);

        switch(key) {
          case 'w':
            seen = 1;
            break;
          case 'k':
            prevSeen = seen;
            seen = 2;
            break;
        }

      } else {
        switch(seen) {
          case 0:
            fArgs.add(s);
            break;
          case 1:
            wArgs.add(s);
            break;
          case 2:
            kArg = s;
            seen = prevSeen;
            break;
        }
      }
    }
  }

  public boolean file() {
    return shortFlagNames.get('f');
  }

  public boolean raw() {
    return shortFlagNames.get('r');
  }

  public boolean decode() {
    return shortFlagNames.get('d');
  }

  public boolean write() {
    return shortFlagNames.get('w');
  }

  public boolean identity() {
    return shortFlagNames.get('i');
  }

  public boolean tunable() {
    return shortFlagNames.get('k');
  }

  public boolean binary() {
    return shortFlagNames.get('b');
  }

  public boolean verbose() {
    return shortFlagNames.get('v');
  }

  public boolean silent() {
    return shortFlagNames.get('s');
  }

  public List<String> getWriteArgs() {
    return wArgs;
  }

  public List<String> getFileArgs() {
    return fArgs;
  }

  public String getTunableArg() {
    return kArg;
  }

}
