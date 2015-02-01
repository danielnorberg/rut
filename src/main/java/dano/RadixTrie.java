package dano;

import java.util.List;

public class RadixTrie<T> {

  private static final char NUL = '\0';

  private final Node<T> root;

  public RadixTrie(final Node<T> root) {
    this.root = root;
  }

  public T lookup(final CharSequence s) {
    final char c = s.length() == 0 ? NUL : s.charAt(0);
    return root.lookup(c, s, 0);
  }

  public static <T> Builder<T> builder(final Class<T> clazz) {
    return new Builder<T>();
  }

  public static class Node<T> {

    private final char first;
    private final char[] tail;
    private final Node<T> child1;
    private final Node<T> child2;
    private final Node<T>[] children;
    private final Node<T> capture;
    private final T value;

    public Node(final String prefix, final Node<T> capture,
                final List<Node<T>> children, final T value) {
      this.first = (prefix.length() == 0) ? NUL : prefix.charAt(0);
      this.tail = (prefix.length() > 1) ? prefix.substring(1).toCharArray() : null;
      this.capture = capture;
      this.child1 = children.size() > 0 ? children.get(0) : null;
      this.child2 = children.size() > 1 ? children.get(1) : null;
      this.children = children.size() > 2 ? toArray(children.subList(2, children.size())) : null;
      this.value = value;
    }

    @SuppressWarnings("unchecked")
    private static <T> RadixTrie.Node<T>[] toArray(final List<RadixTrie.Node<T>> children) {
      return children.toArray((RadixTrie.Node<T>[]) new RadixTrie.Node[children.size()]);
    }

    public T lookup(final char c, final CharSequence s, final int index) {
      if (!matchPrefix(c, s, index)) {
        return null;
      }
      final int nextIndex = index + length();
      if (nextIndex == s.length()) {
        return value;
      }
      final char nextC = s.charAt(nextIndex);
      final T childMatch = lookupChildren(nextC, s, nextIndex);
      if (childMatch != null) {
        return childMatch;
      }
      return lookupCapture(s, nextIndex);
    }

    private int length() {
      if (tail != null) {
        return 1 + tail.length;
      } else {
        return (first == NUL) ? 0 : 1;
      }
    }

    private boolean matchPrefix(final char c, final CharSequence s, final int index) {
      if (first == NUL) {
        return true;
      }
      if (first != c) {
        return false;
      }
      if (index >= s.length()) {
        return false;
      }
      if (tail == null) {
        return true;
      }
      if (index + 1 + tail.length > s.length()) {
        return false;
      }
      for (int i = 0; i < tail.length; i++) {
        if (tail[i] != s.charAt(1 + index + i)) {
          return false;
        }
      }
      return true;
    }

    private T lookupCapture(final CharSequence s, final int newIndex) {
      if (capture == null) {
        return null;
      }
      final int length = s.length();
      final int end = seek(s, newIndex, length, '/');
      for (int i = end; i >= newIndex; i--) {
        final char c = (i == length) ? NUL : s.charAt(i);
        final T value = capture.lookup(c, s, i);
        if (value != null) {
          return value;
        }
      }
      return null;
    }

    private T lookupChildren(final char c, final CharSequence s, final int newIndex) {
      if (child1 != null) {
        final T value = child1.lookup(c, s, newIndex);
        if (value != null) {
          return value;
        }
      }
      if (child2 != null) {
        final T value = child2.lookup(c, s, newIndex);
        if (value != null) {
          return value;
        }
      }
      if (children != null) {
        for (final Node<T> child : children) {
          final T value = child.lookup(c, s, newIndex);
          if (value != null) {
            return value;
          }
        }
      }
      return null;
    }

    private int seek(final CharSequence s, final int start, final int end, final char c) {
      int i = start;
      for (; i < end; i++) {
        if (s.charAt(i) == c) {
          return i;
        }
      }
      return i;
    }

    @Override
    public String toString() {
      return "Node{'" + (first == NUL ? "" : String.valueOf(first)) +
                        (tail == null ? "" : new String(tail)) + "\':" +
             ", d=" + ((capture == null ? 0 : 1) +
                       (child1 == null ? 0 : 1) +
                       (child2 == null ? 0 : 1) +
                       (children == null ? 0 : children.length)) +
             ", c=" + (capture != null) +
             ", v=" + value +
             '}';
    }
  }

  @Override
  public String toString() {
    return "RadixTrie{" +
           "root=" + root +
           '}';
  }

  public static class Builder<T> {

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
