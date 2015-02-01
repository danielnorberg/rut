package dano;

import java.util.ArrayList;
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
    return root.lookup(s, 1);
  }

  public static <T> Builder<T> builder(final Class<T> clazz) {
    return new Builder<T>();
  }

  public static class Node<T> {

    private final char[] tail;
    private final char head1;
    private final char head2;
    private final Node<T> edge1;
    private final Node<T> edge2;
    private final char[] heads;
    private final Node<T>[] edges;
    private final char captureHead;
    private final Node<T> capture;
    private final T value;

    public Node(final String prefix, final char captureHead, final Node<T> capture,
                final List<Character> first,
                final List<Node<T>> edges, final T value) {
      this.tail = (prefix.length() > 1) ? prefix.substring(1).toCharArray() : null;
      this.captureHead = captureHead;
      this.capture = capture;
      this.edge1 = edges.size() > 0 ? edges.get(0) : null;
      this.edge2 = edges.size() > 1 ? edges.get(1) : null;
      this.head1 = (edge1 == null ? NUL : first.get(0));
      this.head2 = (edge2 == null ? NUL : first.get(1));
      this.edges = edges.size() > 2 ? toArray(edges.subList(2, edges.size())) : null;
      if (this.edges != null) {
        this.heads = new char[this.edges.length];
        for (int i = 0; i < this.edges.length; i++) {
          this.heads[i] = first.get(i + 2);
        }
      } else {
        this.heads = null;
      }
      this.value = value;
    }

    @SuppressWarnings("unchecked")
    private static <T> RadixTrie.Node<T>[] toArray(final List<RadixTrie.Node<T>> edges) {
      return edges.toArray((RadixTrie.Node<T>[]) new RadixTrie.Node[edges.size()]);
    }

    public T lookup(final CharSequence s, final int index) {
      final int next = match(s, index);
      if (next == -1) {
        return null;
      }
      assert next >= index;
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
      if (index > s.length()) {
        return -1;
      }
      if (tail == null) {
        return index;
      }
      if (index + tail.length > s.length()) {
        return -1;
      }
      for (int i = 0; i < tail.length; i++) {
        if (tail[i] != s.charAt(index + i)) {
          return -1;
        }
      }
      return index + tail.length;
    }

    private T capture(final CharSequence s, final int index) {
      if (capture == null) {
        return null;
      }
      final int limit = seek(s, index, '/');
      for (int i = limit; i >= index; i--) {
        final int next;
        if (captureHead == NUL) {
          next = i;
        } else {
          if (i >= s.length()) {
            continue;
          }
          final char c = s.charAt(i);
          if (captureHead != c) {
            continue;
          } else {
            next = i + 1;
          }
        }
        final T value = capture.lookup(s, next);
        if (value != null) {
          return value;
        }
      }
      return null;
    }

    private T descend(final char c, final CharSequence s, final int next) {
      if (edge1 != null && head1 == c) {
        final T value = edge1.lookup(s, next + 1);
        if (value != null) {
          return value;
        }
      }
      if (edge2 != null && head2 == c) {
        final T value = edge2.lookup(s, next + 1);
        if (value != null) {
          return value;
        }
      }
      if (edges != null) {
        for (int i = 0; i < edges.length; i++) {
          if (heads[i] != c) {
            continue;
          }
          final Node<T> edge = edges[i];
          final T value = edge.lookup(s, next + 1);
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
             ", d=" + degree() +
             ", e=" + edgesToString() +
             ", c=" + (capture == null ? "" : captureHead == NUL ? "" : "'" + captureHead + "'") +
             ", v=" + value +
             '}';
    }

    private int degree() {
      return ((capture == null ? 0 : 1) +
                (edge1 == null ? 0 : 1) +
                (edge2 == null ? 0 : 1) +
                (edges == null ? 0 : edges.length));
    }

    private String edgesToString() {
      final List<Character> chars = new ArrayList<Character>();
      if (edge1 != null) {
        chars.add(head1);
      }
      if (edge2 != null) {
        chars.add(head2);
      }
      if (heads != null) {
        for (final char head : heads) {
          chars.add(head);
        }
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
