package dano;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import static dano.Util.toCharArray;
import static java.lang.Math.max;

public final class RadixTrie<T> {

  private static final char CAPTURE = 127;
  private static final char SLASH = '/';

  private final Node<T> root;
  private final int captures;

  RadixTrie(final Node<T> root) {
    this.root = root;
    this.captures = root.captures();
  }

  public T lookup(final CharSequence path) {
    return lookup(path, null);
  }

  public T lookup(final CharSequence path, @Nullable final Captor captor) {
    if (captor != null) {
      captor.reset();
    }
    return Node.lookup(root, path, 0, captor, 0);
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

  public static <T> Builder<T> builder() {
    return new Builder<T>();
  }

  public static <T> Builder<T> builder(Class<T> clazz) {
    return new Builder<T>();
  }

  static class Node<T> {

    private final char head;
    private final char[] tail;
    private final Node<T> sibling;
    private final Node<T> edge;
    private final T value;

    public Node(final CharSequence prefix, final Node<T> sibling, final Node<T> edge,
                final T value) {
      this(prefix.length() == 0 ? CAPTURE : prefix.charAt(0),
           prefix.length() == 0 ? null : toCharArray(prefix, 1),
           sibling, edge, value);
    }

    private Node(final char head, final char[] tail, final Node<T> sibling, final Node<T> edge,
                 final T value) {
      this.head = head;
      this.tail = tail;
      this.sibling = sibling;
      this.edge = edge;
      this.value = value;

      // Check that siblings are ordered
      assert sibling == null || head < sibling.head;
    }

    private static <T> T lookup(final Node<T> root, final CharSequence path, final int i,
                                final Captor captor, final int capture) {
      Node<T> node = root;
      while (node != null) {
        final T value = node.lookup(path, i, captor, capture);
        if (value != null) {
          return value;
        }
        node = node.sibling;
      }
      return null;
    }

    private T lookup(final CharSequence path, final int index, @Nullable final Captor captor,
                     final int capture) {
      // Capture?
      if (head == CAPTURE) {
        return capture(path, index, captor, capture);
      }

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

      // Edge fanout
      final T value = lookup(edge, path, next, captor, capture);
      if (value != null) {
        return value;
      }

      return null;
    }

    private int match(final CharSequence path, final int index) {
      assert head != CAPTURE;
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

    private T capture(final CharSequence path, final int index, @Nullable final Captor captor,
                      final int capture) {
      final int limit = bound(path, index);

      // Terminal?
      if (value != null && limit == path.length()) {
        if (captor != null) {
          captor.match(capture + 1);
          captor.capture(capture, index, limit);
        }
        return value;
      }

      // Fanout
      if (edge != null) {
        for (int i = limit; i >= index; i--) {
          final T value = lookup(edge, path, i, captor, capture + 1);
          if (value != null) {
            if (captor != null) {
              captor.capture(capture, index, i);
            }
            return value;
          }
        }
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
      return "Node{'" + prefix() + "\': " +
             "e=" + prefixes(edge) +
             ", v=" + (value == null ? "" : value.toString()) +
             '}';
    }

    private String prefix() {
      return head == CAPTURE
             ? "<*>"
             : head + (tail == null ? "" : String.valueOf(tail));
    }

    public int captures() {
      final int captures = (head == CAPTURE) ? 1 : 0;
      final int edgeCaptures = (edge == null) ? 0 : edge.captures();
      final int siblingCaptures = (sibling == null) ? 0 : sibling.captures();
      return captures + max(edgeCaptures, siblingCaptures);
    }
  }

  private static <T> String prefixes(Node<T> node) {
    final List<String> prefixes = new ArrayList<String>();
    while (node != null) {
      prefixes.add(node.prefix());
      node = node.sibling;
    }
    return prefixes.toString();
  }

  @Override
  public String toString() {
    return "RadixTrie{" + root + "}";
  }

  public static class Builder<T> {

    private Builder() {
    }

    private final Trie<T> trie = new Trie<T>();

    public Builder<T> insert(final CharSequence path, final T value) {
      trie.insert(path, value);
      return this;
    }

    public Builder<T> insert(final CharSequence path, final Trie.Visitor<T> visitor) {
      trie.insert(path, visitor);
      return this;
    }

    public RadixTrie<T> build() {
      return trie.compress();
    }
  }
}
