package dano;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public final class RadixTrie<T> {

  private static final char NUL = '\0';
  private static final char SLASH = '/';

  private final Node<T> root;
  private final int captures;

  RadixTrie(final Node<T> root, final int captures) {
    this.root = root;
    this.captures = captures;
  }

  public T lookup(final CharSequence path) {
    return lookup(path, null);
  }

  public T lookup(final CharSequence path, @Nullable final Captor captor) {
    if (captor != null) {
      captor.reset();
    }
    Node<T> root = this.root;
    while (root != null) {
      final T value = root.lookup(path, 0, captor, 0);
      if (value != null) {
        return value;
      }
      root = root.sibling;
    }
    return null;
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

    T lookup(final CharSequence path, final int index, @Nullable final Captor captor,
             final int capture) {
      // Match prefix
      final int next = match(path, index);
      if (next == -1) {
        return null;
      }
      assert next >= index;

      // Terminal?
      if (next == path.length()) {
        if (captor != null) {
          captor.match(capture);
        }
        return value;
      }

      // Edges
      final T value = fanout(path, next, captor, capture);
      if (value != null) {
        return value;
      }

      // Capture
      return capture(path, next, captor, capture);
    }

    private int match(final CharSequence path, final int index) {
      if (head == NUL) {
        return index;
      }
      if (index >= path.length()) {
        return -1;
      }
      if (head != path.charAt(index)) {
        return -1;
      }
      if (tail == null) {
        return index + 1;
      }
      if (index + 1 + tail.length > path.length()) {
        return -1;
      }
      for (int i = 0; i < tail.length; i++) {
        if (tail[i] != path.charAt(index + 1 + i)) {
          return -1;
        }
      }
      return index + 1 + tail.length;
    }

    private T fanout(final CharSequence path, final int next, @Nullable final Captor captor,
                     final int capture) {
      Node<T> edge = this.edge;
      while (edge != null) {
        final T value = edge.lookup(path, next, captor, capture);
        if (value != null) {
          return value;
        }
        edge = edge.sibling;
      }
      return null;
    }

    private T capture(final CharSequence path, final int index, @Nullable final Captor captor,
                      final int capture) {
      if (this.capture == null) {
        return null;
      }
      final int limit = bound(path, index);
      for (int i = limit; i >= index; i--) {
        Node<T> node = this.capture;
        do {
          final T value = node.lookup(path, i, captor, capture + 1);
          if (value != null) {
            if (captor != null) {
              captor.capture(capture, index, i);
            }
            return value;
          }
          node = node.sibling;
        } while (node != null);
      }
      return null;
    }

    private int bound(final CharSequence path, final int start) {
      int i = start;
      for (; i < path.length(); i++) {
        if (path.charAt(i) == SLASH) {
          return i;
        }
      }
      return i;
    }

    @Override
    public String toString() {
      return "Node{'" + prefix() + "\':" +
             ", e=" + edgePrefixes() +
             ", c=" + (capture == null ? "" : capture.prefix() + "'") +
             ", v=" + (value == null ? "" : value.toString()) +
             '}';
    }

    private String edgePrefixes() {
      final List<String> edgeStrings = new ArrayList<String>();
      Node<T> edge = this.edge;
      while (edge != null) {
        edgeStrings.add(edge.prefix());
        edge = edge.sibling;
      }
      return edgeStrings.toString();
    }

    private String prefix() {
      return head == NUL ? "" : String.valueOf(head) + ((tail == null) ? "" : String.valueOf(tail));
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
