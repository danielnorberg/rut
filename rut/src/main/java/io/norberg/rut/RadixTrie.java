package io.norberg.rut;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import static java.lang.Math.max;

class RadixTrie<T> {

  private static final byte CAPTURE = 127;
  private static final byte SLASH = '/';
  private static final byte QUERY = '?';

  private final Node<T> root;
  private final int captures;

  RadixTrie(final Node<T> root) {
    this.root = root;
    this.captures = root.captures();
  }

  T lookup(final CharSequence path) {
    return lookup(path, null);
  }

  T lookup(final CharSequence path, @Nullable final Trie.Captor captor) {
    if (captor != null) {
      captor.reset();
    }
    return Node.lookup(root, path, 0, captor, 0);
  }

  int captures() {
    return captures;
  }

  Trie.Captor captor() {
    return captor(captures);
  }

  static Trie.Captor captor(final int captures) {
    return new Trie.Captor(captures);
  }

  static <T> Builder<T> builder() {
    return new Builder<T>();
  }

  @SuppressWarnings("UnusedParameters")
  static <T> Builder<T> builder(Class<T> clazz) {
    return new Builder<T>();
  }

  static class Node<T> {

    private final byte head;
    private final byte[] tail;
    private final Node<T> sibling;
    private final Node<T> edge;
    private final T value;

    Node(final CharSequence prefix, final Node<T> sibling, final Node<T> edge,
         final T value) {
      this(prefix.length() == 0 ? CAPTURE : (byte) prefix.charAt(0),
           prefix.length() == 0 ? null : toAsciiByteArray(prefix, 1),
           sibling, edge, value);
    }

    private Node(final byte head, final byte[] tail, final Node<T> sibling, final Node<T> edge,
                 final T value) {
      this.head = head;
      this.tail = tail;
      this.sibling = sibling;
      this.edge = edge;
      this.value = value;

      // Check that siblings are ordered
      assert sibling == null || head < sibling.head;
    }

    private int captures() {
      final int captures = (head == CAPTURE) ? 1 : 0;
      final int edgeCaptures = (edge == null) ? 0 : edge.captures();
      final int siblingCaptures = (sibling == null) ? 0 : sibling.captures();
      return captures + max(edgeCaptures, siblingCaptures);
    }

    private static <T> T lookup(final Node<T> root, final CharSequence path, final int i,
                                final Trie.Captor captor, final int capture) {
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

    private T lookup(final CharSequence path, final int index, @Nullable final Trie.Captor captor,
                     final int capture) {

      if (index >= path.length()) {
        return null;
      }

      // Capture?
      if (head == CAPTURE) {
        return capture(path, index, captor, capture);
      }

      // Match prefix
      final int next;
      if (head != path.charAt(index)) {
        return null;
      } else if (tail == null) {
        next = index + 1;
      } else if (index + 1 + tail.length > path.length()) {
        return null;
      } else {
        for (int i = 0; i < tail.length; i++) {
          if (tail[i] != path.charAt(index + 1 + i)) {
            return null;
          }
        }
        next = index + 1 + tail.length;
      }
      assert next >= index;

      // Terminal?
      if (next == path.length() || path.charAt(next) == QUERY) {
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

    private T capture(final CharSequence path, final int index, @Nullable final Trie.Captor captor,
                      final int capture) {
      final int limit = bound(path, index);

      // Terminal?
      if (value != null && (limit == path.length() || path.charAt(limit) == QUERY)) {
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
        final char c = path.charAt(i);
        if (c == SLASH || c == QUERY) {
          return i;
        }
      }
      return i;
    }

    private String prefix() {
      if (head == CAPTURE) {
        return "<*>";
      } else {
        if (tail == null) {
          return String.valueOf((char) head);
        } else {
          final StringBuilder b = new StringBuilder().append((char) head);
          for (final byte c : tail) {
            b.append((char) c);
          }
          return b.toString();
        }
      }
    }

    @Override
    public String toString() {
      return "Node{'" + prefix() + "\': " +
             "e=" + prefixes(edge) +
             ", v=" + (value == null ? "" : value.toString()) +
             '}';
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

  static class Builder<T> {

    private Builder() {
    }

    private final Trie<T> trie = new Trie<T>();

    Builder<T> insert(final CharSequence path, final T value) {
      trie.insert(path, value);
      return this;
    }

    Builder<T> insert(final CharSequence path, final Trie.Visitor<T> visitor) {
      trie.insert(path, visitor);
      return this;
    }

    RadixTrie<T> build() {
      return trie.compress();
    }
  }

  private static byte[] toAsciiByteArray(final CharSequence sequence, final int from) {
    final int length = sequence.length() - from;
    final byte[] chars = new byte[length];
    for (int i = 0; i < length; i++) {
      final char c = sequence.charAt(from + i);
      if (c > 127) {
        throw new IllegalArgumentException();
      }
      chars[i] = (byte) c;
    }
    return chars;
  }
}
