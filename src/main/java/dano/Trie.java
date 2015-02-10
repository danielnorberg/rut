package dano;

import java.util.ArrayList;
import java.util.List;

class Trie<T> {

  private final List<Node<T>> roots = new ArrayList<Node<T>>();

  T insert(final CharSequence path, final T value) {
    return insert(path, new DefaultVisitor(value));
  }

  T insert(final CharSequence path, final Visitor<T> visitor) {
    if (path.length() == 0) {
      throw new IllegalArgumentException();
    }
    final char c = path.charAt(0);
    for (final Node<T> root : roots) {
      if (root.c == c) {
        return root.insert(path, 1, 0, visitor);
      }
    }
    final Node<T> root = new Node<T>(c);
    final T v = root.insert(path, 1, 0, visitor);
    roots.add(root);
    return v;
  }

  RadixTrie<T> compress() {
    RadixTrie.Node<T> node = null;
    int captures = 0;
    for (final Node<T> root : roots) {
      node = root.compress(node);
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

    T insert(final CharSequence s, final int i, final int captureIndex,
             final Visitor<T> visitor) {
      if (i == s.length()) {
        final T old = this.value;
        this.value = visitor.finish(captureIndex, this.value);
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
          final T value = capture.insert(s, end + 1, captureIndex + 1, visitor);
          visitor.capture(captureIndex, s.subSequence(i + 1, end));
          return value;

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
          return next.insert(s, i + 1, captureIndex, visitor);
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

    private RadixTrie.Node<T> compress(final RadixTrie.Node<T> sibling) {
      // Compute compressed prefix
      final Node<T> tail = tail();
      final StringBuilder prefix = new StringBuilder();
      append(prefix, this, tail);

      // Create capture node
      final RadixTrie.Node<T> capture;
      if (tail.capture == null) {
        capture = null;
      } else {
        RadixTrie.Node<T> node = null;
        if (tail.capture.value != null) {
          // Terminal capture node
          node = new RadixTrie.Node<T>("", null, null, null, tail.capture.value);
        }
        // Capture suffix branches
        for (final Node<T> root : tail.capture.edges) {
          node = root.compress(node);
        }
        capture = node;
      }

      // Suffix branches
      RadixTrie.Node<T> edge = null;
      for (final Node<T> e : tail.edges) {
        edge = e.compress(edge);
      }

      return new RadixTrie.Node<T>(prefix.toString(), sibling, edge, capture, tail.value);
    }

    private void append(final StringBuilder prefix, final Node<T> start, final Node<T> end) {
      Node<T> node = start;
      while (true) {
        prefix.append(node.c);
        if (node == end) {
          return;
        }
        node = node.edges.get(0);
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

  public static interface Visitor<T> {

    public void capture(final int i, final CharSequence s);

    public T finish(final int captures, final T currentValue);
  }

  /**
   * A visitor that just sets a new value.
   */
  private class DefaultVisitor implements Visitor<T> {

    private final T value;

    public DefaultVisitor(final T value) {
      this.value = value;
    }

    @Override
    public void capture(final int i, final CharSequence s) {
    }

    @Override
    public T finish(final int captures, final T currentValue) {
      return value;
    }
  }
}
