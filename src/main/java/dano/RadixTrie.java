package dano;

import java.util.List;

public class RadixTrie<T> {

  private static final char NUL = '\0';

  private final char first;
  private final Node<T> root;

  public RadixTrie(final char first, final Node<T> root) {
    this.first = first;
    this.root = root;
  }

  public T lookup(final CharSequence s) {
    final char c = s.length() == 0 ? NUL : s.charAt(0);
    if (c != first) {
      return null;
    }
    return root.lookup(s, 0);
  }

  public static <T> Builder<T> builder(final Class<T> clazz) {
    return new Builder<T>();
  }

  public static class Node<T> {

    private final char[] tail;
    private final char child1First;
    private final char child2First;
    private final Node<T> child1;
    private final Node<T> child2;
    private final char[] childrenFirst;
    private final Node<T>[] children;
    private final char captureHead;
    private final Node<T> capture;
    private final T value;

    public Node(final String prefix, final char captureHead, final Node<T> capture,
                final List<Character> first,
                final List<Node<T>> children, final T value) {
      this.tail = (prefix.length() > 1) ? prefix.substring(1).toCharArray() : null;
      this.captureHead = captureHead;
      this.capture = capture;
      this.child1 = children.size() > 0 ? children.get(0) : null;
      this.child2 = children.size() > 1 ? children.get(1) : null;
      this.child1First = (child1 == null ? NUL : first.get(0));
      this.child2First = (child2 == null ? NUL : first.get(1));
      this.children = children.size() > 2 ? toArray(children.subList(2, children.size())) : null;
      if (this.children != null) {
        this.childrenFirst = new char[this.children.length];
        for (int i = 0; i < this.children.length; i++) {
          this.childrenFirst[i] = first.get(i + 2);
        }
      } else {
        this.childrenFirst = null;
      }
      this.value = value;
    }

    @SuppressWarnings("unchecked")
    private static <T> RadixTrie.Node<T>[] toArray(final List<RadixTrie.Node<T>> children) {
      return children.toArray((RadixTrie.Node<T>[]) new RadixTrie.Node[children.size()]);
    }

    public T lookup(final CharSequence s, final int index) {
      final int next = match(s, index);
      if (next == -1) {
        return null;
      }
      assert next > index;
      if (next == s.length()) {
        return value;
      }
      final char c = s.charAt(next);
      final T value = descend(c, s, next);
      if (value != null) {
        return value;
      }
      return capture(s, next);
    }

    private int match(final CharSequence s, final int index) {
      if (index >= s.length()) {
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

    private T capture(final CharSequence s, final int index) {
      if (capture == null) {
        return null;
      }
      final int maxCapture = seek(s, index, '/') - 1;
      for (int i = maxCapture; i >= index; i--) {
        final char c = s.charAt(i);
        if (c == '/') {
          return null;
        }
        if (captureHead != NUL && captureHead != c) {
          continue;
        }
        final T value = capture.lookup(s, i);
        if (value != null) {
          return value;
        }
      }
      return null;
    }

    private T descend(final char c, final CharSequence s, final int newIndex) {
      if (child1 != null && child1First == c) {
        final T value = child1.lookup(s, newIndex);
        if (value != null) {
          return value;
        }
      }
      if (child2 != null && child2First == c) {
        final T value = child2.lookup(s, newIndex);
        if (value != null) {
          return value;
        }
      }
      if (children != null) {
        for (int i = 0; i < children.length; i++) {
          if (childrenFirst[i] != c) {
            continue;
          }
          final Node<T> child = children[i];
          final T value = child.lookup(s, newIndex);
          if (value != null) {
            return value;
          }
        }
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
      return "Node{'" + (tail == null ? "" : new String(tail)) + "\':" +
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
