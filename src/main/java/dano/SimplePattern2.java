package dano;

import java.util.concurrent.atomic.AtomicInteger;

public class SimplePattern2 {

  public Result result() {
    return result(values);
  }

  public static Result result(final int maxValues) {
    return new DefaultResult(maxValues);
  }

  public interface Result {

    void value(int i, int start, int end);

    void match(final int values);

    boolean isMatch();

    int values();

    CharSequence value(CharSequence haystack, int i);
  }

  private static final Node TERMINAL_NODE = new TerminalNode();

  private final Node root;
  private final int values;

  public SimplePattern2(final CharSequence needle) {
    final AtomicInteger values = new AtomicInteger();
    this.root = compile(needle, values);
    this.values = values.get();
  }

  private Node compile(final CharSequence needle, final AtomicInteger values) {
    return node(0, needle, values);
  }

  private Node node(final int index, final CharSequence needle, final AtomicInteger values) {
    if (index >= needle.length()) {
      return terminal();
    }
    final char c = needle.charAt(index);
    if (c == '<') {
      return wildcard(index, needle, values);
    } else {
      return text(index, needle, values);
    }
  }

  private Node text(final int index, final CharSequence needle, final AtomicInteger values) {
    final int wildcardIndex = indexOf(needle, '<', index);
    final String text;
    if (wildcardIndex == -1) {
      text = needle.subSequence(index, needle.length()).toString();
      return new TextNode(text, terminal());
    } else {
      text = needle.subSequence(index, wildcardIndex).toString();
      return new TextNode(text, wildcard(wildcardIndex, needle, values));
    }
  }

  private Node wildcard(final int index, final CharSequence needle, final AtomicInteger values) {
    final int endIndex = indexOf(needle, '>', index);
    if (endIndex == -1) {
      throw new IllegalArgumentException(
          "unclosed wildcard: " + needle.subSequence(index, needle.length()).toString());
    } else {
      values.incrementAndGet();
      return new WildcardNode(node(endIndex + 1, needle, values));
    }
  }

  private Node terminal() {
    return TERMINAL_NODE;
  }

  private static int indexOf(final CharSequence sequence, final char needle, final int index) {
    for (int i = index; i < sequence.length(); i++) {
      if (sequence.charAt(i) == needle) {
        return i;
      }
    }
    return -1;
  }

  public static SimplePattern2 of(final CharSequence needle) {
    return new SimplePattern2(needle);
  }

  public SimpleMatcher2 matcher(final CharSequence haystack) {
    return new SimpleMatcher2(root, haystack, values);
  }

  public boolean match(final CharSequence haystack, final Result result) {
    return root.match(haystack, 0, result, 0);
  }

  interface Node {

    boolean match(CharSequence haystack, int index, Result result, int valueIndex);
  }

  private static class TextNode implements Node {

    private final String text;
    private final Node next;

    public TextNode(final String text, final Node next) {
      this.text = text;
      this.next = next;
    }

    @Override
    public boolean match(final CharSequence haystack, final int index, final Result result,
                         final int valueIndex) {
      if (index + text.length() > haystack.length()) {
        return false;
      }
      for (int i = 0; i < text.length(); i++) {
        final char a = haystack.charAt(index + i);
        final char b = text.charAt(i);
        if (a != b) {
          return false;
        }
      }
      return next.match(haystack, index + text.length(), result, valueIndex);
    }
  }

  private static class WildcardNode implements Node {

    private final Node next;

    public WildcardNode(final Node next) {
      this.next = next;
    }

    @Override
    public boolean match(final CharSequence haystack, final int index, final Result result,
                         final int valueIndex) {
      final int length = haystack.length();
      for (int i = length - 1; i >= index; i--) {
        if (next.match(haystack, i, result, valueIndex + 1)) {
          result.value(valueIndex, index, i);
          return true;
        }
      }
      return false;
    }
  }

  private static class TerminalNode implements Node {

    @Override
    public boolean match(final CharSequence haystack, final int index, final Result result,
                         final int valueIndex) {
      if (index == haystack.length()) {
        result.match(valueIndex);
        return true;
      } else {
        return false;
      }
    }
  }

  static class DefaultResult implements Result {

    private final int[] start;
    private final int[] end;
    private boolean match;
    private int values;

    public DefaultResult(final int maxValues) {
      this.start = new int[maxValues];
      this.end = new int[maxValues];
    }

    @Override
    public void value(final int i, final int start, final int end) {
      this.start[i] = start;
      this.end[i] = end;
    }

    @Override
    public void match(final int values) {
      match = true;
      this.values = values;
    }

    @Override
    public boolean isMatch() {
      return match;
    }

    @Override
    public int values() {
      return values;
    }

    @Override
    public CharSequence value(final CharSequence haystack, final int i) {
      if (!match) {
        throw new IllegalStateException("not matched");
      }
      if (i > values) {
        throw new IndexOutOfBoundsException();
      }
      return haystack.subSequence(start[i], end[i]);
    }
  }
}
