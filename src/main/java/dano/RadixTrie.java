package dano;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import static dano.Util.indexOf;
import static java.lang.Math.max;
import static java.lang.Math.min;

public final class RadixTrie<T> {

  private static final char NUL = '\0';
  private static final char SLASH = '/';

  private final Node<T> root;
  private final int captures;

  public static <T> RadixTrie<T> create() {
    return new RadixTrie<T>();
  }

  public static <T> RadixTrie<T> create(Class<T> clazz) {
    return new RadixTrie<T>();
  }

  private RadixTrie() {
    this.root = new Node<T>("", null, null, null, null);
    this.captures = 0;
  }

  RadixTrie(final Node<T> root, final int captures) {
    this.root = root;
    this.captures = captures;
  }

  public RadixTrie<T> insert(final CharSequence path, final T value) {
    final Node<T> newRoot = root.insert(path, 0, value);
    return new RadixTrie<T>(newRoot, newRoot.captures());
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

  private static <T> Node<T> chain(final CharSequence path, final int index, final T value) {
    return chain(null, path, index, value);
  }

  private static <T> Node<T> chain(final Node<T> sibling, final CharSequence path, final int index,
                                   final T value) {
    final int length = path.length();
    final int start = indexOf(path, '<', index);
    if (start == -1) {
      return new Node<T>(path.subSequence(index, length), sibling, null, null, value);
    }
    final int end = indexOf(path, '>', start + 1);
    if (end == -1) {
      throw new IllegalArgumentException(
          "unclosed capture: " + path.subSequence(start, length).toString());
    }
    final Node<T> capture = chain(path, end + 1, value);
    return new Node<T>(path.subSequence(index, start), sibling, null, capture, null);
  }

  static class Node<T> {

    private final char head;
    private final char[] tail;
    private final Node<T> sibling;
    private final Node<T> edge;
    private final Node<T> capture;
    private final T value;

    public Node(final CharSequence prefix, final Node<T> sibling, final Node<T> edge,
                final Node<T> capture, final T value) {
      this(prefix.length() == 0 ? NUL : prefix.charAt(0),
           prefix.length() == 0 ? null : toCharArray(prefix, 1),
           sibling, edge, capture, value);
    }

    private static char[] toCharArray(final CharSequence sequence, final int from) {
      final int length = sequence.length() - from;
      final char[] chars = new char[length];
      for (int i = 0; i < length; i++) {
        chars[i] = sequence.charAt(from + i);
      }
      return chars;
    }

    private Node(final char head, final char[] tail, final Node<T> sibling, final Node<T> edge,
                 final Node<T> capture, final T value) {
      this.head = head;
      this.tail = tail;
      this.sibling = sibling;
      this.edge = edge;
      this.capture = capture;
      this.value = value;
    }

    private Node<T> insert(final CharSequence path, final int index, final T value) {
      // Check that this node is on the path
      if (head != NUL && head != path.charAt(index)) {
        if (sibling != null) {
          final Node<T> newSibling = sibling.insert(path, index, value);
          return new Node<T>(head, tail, newSibling, edge, capture, this.value);
        } else {
          return chain(this, path, index, value);
        }
      }

      // Compare
      final String prefix = prefix();
      final int length = min(path.length() - index, prefix.length());
      int i;
      for (i = 0; i < length; i++) {
        final char c = path.charAt(index + i);
        final char p = prefix.charAt(i);
        if (c != p) {
          break;
        }
      }

      // Branch?
      if (i < prefix.length()) {
        final String newPrefix = prefix.substring(0, i);
        final String edgePrefix = prefix.substring(i);
        final Node<T> newCapture;
        final Node<T> branch;
        final T newValue;
        if (i + index == path.length()) {
          branch = null;
          newCapture = null;
          newValue = value;
        } else {
          final Node<T> next = chain(path, index + i, value);
          if (next.length() == 0) {
            branch = null;
            newCapture = next.capture;
          } else {
            branch = next;
            newCapture = null;
          }
          newValue = null;
        }
        final Node<T> newEdge = new Node<T>(edgePrefix, branch, edge, this.capture, this.value);
        return new Node<T>(newPrefix, sibling, newEdge, newCapture, newValue);
      }

      // Terminate?
      if (index + length() == path.length()) {
        return new Node<T>(head, tail, sibling, edge, capture, value);
      }

      // Extend
      final char c = path.charAt(index + length);
      final Node<T> newEdge;
      final Node<T> newCapture;
      if (c == '<') {
        final int end = indexOf(path, '>', index + length + 1);
        if (end == -1) {
          throw new IllegalArgumentException(
              "unclosed capture: " + path.subSequence(index + length, path.length()).toString());
        }
        newEdge = edge;
        if (capture == null) {
          newCapture = chain(path, end + 1, value);
        } else {
          newCapture = capture.insert(path, end + 1, value);
        }
      } else {
        newCapture = capture;
        if (edge == null) {
          newEdge = chain(path, index + prefix.length(), value);
        } else {
          newEdge = edge.insert(path, index + prefix.length(), value);
        }
      }

      return new Node<T>(head, tail, sibling, newEdge, newCapture, this.value);
    }

    private int length() {
      return head == NUL ? 0 : tail == null ? 1 : 1 + tail.length;
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
      return "Node{'" + prefix() + "\': " +
             "e=" + prefixes(edge) +
             ", c=" + (capture == null ? "" : capture.prefix() + "'") +
             ", v=" + (value == null ? "" : value.toString()) +
             '}';
    }

    private String prefix() {
      return head == NUL ? "" : String.valueOf(head) + ((tail == null) ? "" : String.valueOf(tail));
    }

    public int captures() {
      final int captures = (capture == null) ? 0 : capture.captures() + 1;
      final int edgeCaptures = (edge == null) ? 0 : edge.captures();
      final int siblingCaptures = (sibling == null) ? 0 : sibling.captures();
      return max(captures, max(edgeCaptures, siblingCaptures));
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
