package dano;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public final class RadixTrie<T> {

  private static final char NUL = '\0';

  private final Node<T> root;
  private final int captures;

  RadixTrie(final Node<T> root, final int captures) {
    this.root = root;
    this.captures = captures;
  }

  public int captures() {
    return captures;
  }

  public Captor captor() {
    return captor(captures);
  }

  public static Captor captor(final int captures) {
    return new Captor(captures);
  }

  public T lookup(final CharSequence s, @Nullable final Captor captor) {
    if (captor != null) {
      captor.reset();
    }
    Node<T> root = this.root;
    while (root != null) {
      final T value = root.lookup(s, 0, captor, 0);
      if (value != null) {
        return value;
      }
      root = root.sibling;
    }
    return null;
  }

  public T lookup(final CharSequence s) {
    return lookup(s, null);
  }

  public static <T> Builder<T> builder(final Class<T> clazz) {
    return new Builder<T>();
  }

  static class Node<T> {

    private final char head;
    private final char[] tail;

    private final Node<T> sibling;
    private final Node<T> edge;
    private final Node<T> capture;
    private final T value;

    public Node(final String prefix, final Node<T> sibling, final Node<T> edge,
                final Node<T> capture, final T value) {
      this.head = prefix.length() == 0 ? NUL : prefix.charAt(0);
      this.tail = prefix.length() == 0 ? null : prefix.substring(1).toCharArray();
      this.sibling = sibling;
      this.edge = edge;
      this.capture = capture;
      this.value = value;
    }

    T lookup(final CharSequence s, final int index, @Nullable final Captor captor,
             final int capture) {
      final int next = match(s, index);
      if (next == -1) {
        return null;
      }
      assert next >= index;
      if (next == s.length()) {
        if (captor != null) {
          captor.match(capture);
        }
        return value;
      }
      final T value = descend(s, next, captor, capture);
      if (value != null) {
        return value;
      }
      return capture(s, next, captor, capture);
    }

    private int match(final CharSequence s, final int index) {
      if (head == NUL) {
        return index;
      }
      if (index >= s.length()) {
        return -1;
      }
      if (head != s.charAt(index)) {
        return -1;
      }
      if (tail == null) {
        return index + 1;
      }
      if (index + 1 + tail.length > s.length()) {
        return -1;
      }
      for (int i = 0; i < tail.length; i++) {
        if (tail[i] != s.charAt(index + 1 + i)) {
          return -1;
        }
      }
      return index + 1 + tail.length;
    }

    private T capture(final CharSequence s, final int index, @Nullable final Captor captor,
                      final int capture) {
      if (this.capture == null) {
        return null;
      }
      final int limit = seek(s, index, '/');
      for (int i = limit; i >= index; i--) {
        final T value = this.capture.lookup(s, i, captor, capture + 1);
        if (value != null) {
          if (captor != null) {
            captor.capture(capture, index, i);
          }
          return value;
        }
      }
      return null;
    }

    private T descend(final CharSequence s, final int next, @Nullable final Captor captor,
                      final int capture) {
      Node<T> edge = this.edge;
      while (edge != null) {
        final T value = edge.lookup(s, next, captor, capture);
        if (value != null) {
          return value;
        }
        edge = edge.sibling;
      }
      return null;
    }

    private int seek(final CharSequence s, final int start, final char c) {
      int i = start;
      for (; i < s.length(); i++) {
        if (s.charAt(i) == c) {
          return i;
        }
      }
      return i;
    }

    @Override
    public String toString() {
      return "Node{'" + ((head == NUL ? "" : String.valueOf(head)) +
                         (tail == null ? "" : new String(tail))) + "\':" +
             ", e=" + edgesToString() +
             ", c=" + (capture == null ? "" : capture.head == NUL ? "" : "'" + capture.head + "'") +
             ", v=" + value +
             '}';
    }

    private String edgesToString() {
      final List<String> chars = new ArrayList<String>();
      Node<T> edge = this.edge;
      while (edge != null) {
        chars.add(edge.head == NUL ? "" : String.valueOf(edge.head));
        edge = edge.sibling;
      }
      return chars.toString();
    }

  }

  @Override
  public String toString() {
    return "RadixTrie{" +
           "root=" + root +
           '}';
  }

  public static class Builder<T> {

    private Builder() {
    }

    private final Trie<T> trie = new Trie<T>();

    public Builder<T> insert(final CharSequence path, final T value) {
      trie.insert(path, value);
      return this;
    }

    public RadixTrie<T> build() {
      return trie.compress();
    }
  }
}
