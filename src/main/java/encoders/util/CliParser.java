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
  static {
    longFlagNames.put("file", 'f');
    longFlagNames.put("raw", 'r');
    longFlagNames.put("decode", 'd');
    longFlagNames.put("encode", 'e');
    longFlagNames.put("write", 'w');
    longFlagNames.put("verify", 'v');
    longFlagNames.put("binary", 'b');
  }

  public CliParser(String[] args) {
    shortFlagNames.put('f', false);
    shortFlagNames.put('r', false);
    shortFlagNames.put('d', false);
    shortFlagNames.put('w', false);
    shortFlagNames.put('v', false);
    shortFlagNames.put('b', false);

    boolean wSeen = false;
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

        if(key == 'w') {
          wSeen = true;
        }

      } else {
        if(wSeen) {
          wArgs.add(s);
        } else {
          fArgs.add(s);
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

  public boolean verify() {
    return shortFlagNames.get('v');
  }

  public boolean binary() {
    return shortFlagNames.get('b');
  }

  public List<String> getWriteArgs() {
    return wArgs;
  }

  public List<String> getFileArgs() {
    return fArgs;
  }

}
