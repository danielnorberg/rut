package dano;

import java.util.ArrayList;
import java.util.List;

public class Trie<T> {

  private final Node<T> root = new Node<T>();

  public Trie<T> insert(final CharSequence path, final T value) {
    root.insert(path, 0, value);
    return this;
  }

  public RadixTrie<T> compress() {
    return new RadixTrie<T>(root.compress(new StringBuilder()));
  }

  private static class Edge<T> {

    private final char c;
    private final Node<T> node = new Node<T>();

    public Edge(final char c) {
      this.c = c;
    }

    public RadixTrie.Node<T> compress() {
      return node.compress(new StringBuilder().append(c));
    }

    @Override
    public String toString() {
      return "Edge{" +
             "c=" + c +
             ", node=" + node +
             '}';
    }
  }

  private static class Node<T> {

    private Edge<T> capture = null;
    private List<Edge<T>> children = new ArrayList<Edge<T>>();
    private T value;

    public T insert(final CharSequence s, final int i, final T value) {
      if (i == s.length()) {
        final T old = this.value;
        this.value = value;
        return old;
      }
      final char c = s.charAt(i);
      switch (c) {
        case '<':
          if (capture == null) {
            capture = edge(c);
          }
          final int end = indexOf(s, i + 1, '>');
          if (end == -1) {
            throw new IllegalArgumentException(
                "unclosed capture: " + s.subSequence(i, s.length()).toString());
          }
          return capture.node.insert(s, end + 1, value);

        default:
          Edge<T> next = null;
          for (final Edge<T> child : children) {
            if (child.c == c) {
              next = child;
            }
          }
          if (next == null) {
            next = edge(c);
            children.add(next);
          }
          return next.node.insert(s, i + 1, value);
      }
    }

    private int indexOf(final CharSequence s, final int start, final char c) {
      for (int i = start; i < s.length(); i++) {
        if (s.charAt(i) == c) {
          return i;
        }
      }
      return -1;
    }

    private Edge<T> edge(final char c) {
      return new Edge<T>(c);
    }

    public RadixTrie.Node<T> compress(final StringBuilder prefix) {
      final Node<T> tail = tail();
      append(prefix, this, tail);
      final T value = tail.value;
      final RadixTrie.Node<T> capture = (tail.capture == null)
                                        ? null :
                                        tail.capture.node.compress(new StringBuilder());
      final List<RadixTrie.Node<T>> children = new ArrayList<RadixTrie.Node<T>>();
      for (final Edge<T> child : tail.children) {
        children.add(child.compress());
      }
      return new RadixTrie.Node<T>(prefix.toString(), capture, toArray(children), value);
    }

    @SuppressWarnings("unchecked")
    private static <T> RadixTrie.Node<T>[] toArray(final List<RadixTrie.Node<T>> children) {
      return children.toArray((RadixTrie.Node<T>[]) new RadixTrie.Node[children.size()]);
    }

    private void append(final StringBuilder prefix, final Node<T> start, final Node<T> end) {
      Node<T> node = start;
      while (node != end) {
        final Edge<T> edge = node.children.get(0);
        prefix.append(edge.c);
        node = edge.node;
      }
    }

    private Node<T> tail() {
      Node<T> tail = this;
      while (tail.children.size() == 1 && tail.capture == null && tail.value == null) {
        tail = tail.children.get(0).node;
      }
      return tail;
    }

    @Override
    public String toString() {
      return "Node{" +
             "capture=" + (capture != null) +
             ", children=" + children.size() +
             ", value=" + value +
             '}';
    }
  }

  @Override
  public String toString() {
    return "Trie{" +
           "root=" + root +
           '}';
  }
}
