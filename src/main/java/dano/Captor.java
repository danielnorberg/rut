package dano;

public class Captor {

  private final int[] start;
  private final int[] end;
  private boolean match;
  private int captured;

  public Captor(final int captures) {
    this.start = new int[captures];
    this.end = new int[captures];
  }

  public void reset() {
    match = false;
    captured = 0;
  }

  void capture(final int i, final int start, final int end) {
    this.start[i] = start;
    this.end[i] = end;
  }

  void match(final int captured) {
    match = true;
    this.captured = captured;
  }

  public boolean isMatch() {
    return match;
  }

  public int values() {
    return captured;
  }

  public int valueStart(final int i) {
    if (!match) {
      throw new IllegalStateException("not matched");
    }
    if (i > captured) {
      throw new IndexOutOfBoundsException();
    }
    return start[i];
  }

  public int valueEnd(final int i) {
    if (!match) {
      throw new IllegalStateException("not matched");
    }
    if (i > captured) {
      throw new IndexOutOfBoundsException();
    }
    return end[i];
  }

  public CharSequence value(final CharSequence haystack, final int i) {
    if (!match) {
      throw new IllegalStateException("not matched");
    }
    if (i > captured) {
      throw new IndexOutOfBoundsException();
    }
    return haystack.subSequence(start[i], end[i]);
  }
}
