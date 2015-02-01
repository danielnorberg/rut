package dano;

import java.util.ArrayList;
import java.util.List;

class Trie<T> {

  private final List<Node<T>> roots = new ArrayList<Node<T>>();

  Trie<T> insert(final CharSequence path, final T value) {
    if (path.length() == 0) {
      throw new IllegalArgumentException();
    }
    final char c = path.charAt(0);
    for (final Node<T> root : roots) {
      if (root.c == c) {
        root.insert(path, 1, value);
        return this;
      }
    }
    final Node<T> root = new Node<T>(c);
    root.insert(path, 1, value);
    roots.add(root);
    return this;
  }

  RadixTrie<T> compress() {
    RadixTrie.Node<T> node = null;
    int captures = 0;
    for (final Node<T> root : roots) {
      node = root.compress(new StringBuilder().append(root.c), node);
      captures = Math.max(captures, root.captures(0));
    }
    return new RadixTrie<T>(node, captures);
  }

  private static class Node<T> {

    private final char c;
    private Node<T> capture = null;
    private List<Node<T>> edges = new ArrayList<Node<T>>();
    private T value;

    private Node(final char c) {
      this.c = c;
    }

    T insert(final CharSequence s, final int i, final T value) {
      if (i == s.length()) {
        final T old = this.value;
        this.value = value;
        return old;
      }
      final char c = s.charAt(i);
      switch (c) {
        case '<':
          if (capture == null) {
            capture = node('*');
          }
          final int end = indexOf(s, i + 1, '>');
          if (end == -1) {
            throw new IllegalArgumentException(
                "unclosed capture: " + s.subSequence(i, s.length()).toString());
          }
          return capture.insert(s, end + 1, value);

        default:
          Node<T> next = null;
          for (final Node<T> edge : edges) {
            if (edge.c == c) {
              next = edge;
            }
          }
          if (next == null) {
            next = node(c);
            edges.add(next);
          }
          return next.insert(s, i + 1, value);
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

    private Node<T> node(final char c) {
      return new Node<T>(c);
    }

    private RadixTrie.Node<T> compress(final StringBuilder prefix,
                                       final RadixTrie.Node<T> sibling) {
      final Node<T> tail = tail();
      append(prefix, this, tail);
      final T value = tail.value;
      final RadixTrie.Node<T> capture;
      if (tail.capture == null) {
        capture = null;
      } else {
        RadixTrie.Node<T> node = null;
        if (tail.capture.value != null) {
          // Capture terminal node
          node = new RadixTrie.Node<T>("", null, null, null, tail.capture.value);
        }
        for (final Node<T> root : tail.capture.edges) {
          node = root.compress(new StringBuilder().append(root.c), node);
        }
        capture = node;
      }
      RadixTrie.Node<T> edge = null;
      for (final Node<T> e : tail.edges) {
        edge = e.compress(new StringBuilder().append(e.c), edge);
      }
      return new RadixTrie.Node<T>(prefix.toString(), sibling, edge, capture, value);
    }

    private void append(final StringBuilder prefix, final Node<T> start, final Node<T> end) {
      Node<T> node = start;
      while (node != end) {
        node = node.edges.get(0);
        prefix.append(node.c);
      }
    }

    private Node<T> tail() {
      Node<T> tail = this;
      while (tail.edges.size() == 1 && tail.capture == null && tail.value == null) {
        tail = tail.edges.get(0);
      }
      return tail;
    }

    @Override
    public String toString() {
      return "Node{'" + c + "'" +
             ", capture=" + (capture != null) +
             ", edges=" + edges.size() +
             ", value=" + value +
             '}';
    }

    private int captures(final int captures) {
      int max = (capture == null) ? captures : capture.captures(captures + 1);
      for (final Node<T> edge : edges) {
        max = Math.max(max, edge.captures(captures));
      }
      return max;
    }
  }

  @Override
  public String toString() {
    return "Trie{" +
           "roots=" + roots +
           '}';
  }
}
