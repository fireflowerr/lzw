package encoders.app.cli;


public class NoSuchFlagException extends IllegalArgumentException {
  public NoSuchFlagException() {
    super();
  }

  public NoSuchFlagException(String err) {
    super("NoSuchFlagException: " + err);
  }
}
