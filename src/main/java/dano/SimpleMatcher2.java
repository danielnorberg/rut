package dano;

import dano.SimplePattern2.Node;

public class SimpleMatcher2 {

  private final SimplePattern2.Result result;
  private final CharSequence haystack;

  private boolean matches;

  public SimpleMatcher2(final Node root, final CharSequence haystack, final int values) {
    this.haystack = haystack;
    this.result = new SimplePattern2.DefaultResult(values);
    this.matches = root.match(haystack, 0, result, 0);
  }

  public boolean matches() {
    return matches;
  }

  public CharSequence value(final int i) {
    return result.value(haystack, i);
  }
}
