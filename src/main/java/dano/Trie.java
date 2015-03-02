package dano;

import java.util.Map;
import java.util.TreeMap;

import static dano.Util.reversed;

class Trie<T> {

  private static final char CAPTURE = 127;

  private final Map<Character, Node<T>> roots = new TreeMap<Character, Node<T>>();

  T insert(final CharSequence path, final T value) {
    return insert(path, new DefaultVisitor(value));
  }

  T insert(final CharSequence path, final Visitor<T> visitor) {
    if (path.length() == 0) {
      throw new IllegalArgumentException();
    }
    return insert(roots, path, 0, 0, visitor);
  }

  private static <T> T insert(final Map<Character, Node<T>> nodes, final CharSequence path,
                              final int i, final int captureIndex, final Visitor<T> visitor) {
    final char c = path.charAt(i);
    switch (c) {
      case '<':
        Node<T> capture = nodes.get(CAPTURE);
        if (capture == null) {
          capture = Node.of(CAPTURE);
          nodes.put(CAPTURE, capture);
        }
        final int end = Util.indexOf(path, '>', i + 1);
        if (end == -1) {
          throw new IllegalArgumentException(
              "unclosed capture: " + path.subSequence(i, path.length()).toString());
        }
        final T value = capture.extend(path, end + 1, captureIndex + 1, visitor);
        visitor.capture(captureIndex, path.subSequence(i + 1, end));
        return value;

      default:
        Node<T> next = nodes.get(c);
        if (next == null) {
          next = Node.of(c);
          nodes.put(c, next);
        }
        return next.extend(path, i + 1, captureIndex, visitor);
    }
  }

  RadixTrie<T> compress() {
    return new RadixTrie<T>(compressEdges(roots));
  }

  private static <T> RadixTrie.Node<T> compressEdges(final Map<Character, Node<T>> nodes) {
    RadixTrie.Node<T> node = null;
    for (final Node<T> e : reversed(nodes.values())) {
      node = e.compress(node);
    }
    return node;
  }

  private static class Node<T> {

    private final char c;
    private final Map<Character, Node<T>> edges = new TreeMap<Character, Node<T>>();
    private T value;

    private Node(final char c) {
      this.c = c;
    }

    private static <T> Node<T> of(final char c) {
      return new Node<T>(c);
    }

    private T extend(final CharSequence path, final int i,
                     final int captureIndex, final Visitor<T> visitor) {
      if (i == path.length()) {
        final T old = value;
        value = visitor.finish(captureIndex, value);
        return old;
      }
      return insert(edges, path, i, captureIndex, visitor);
    }

    private RadixTrie.Node<T> compress(final RadixTrie.Node<T> sibling) {
      // Compute compressed prefix
      final StringBuilder prefix = new StringBuilder();
      final Node<T> tail = tail(prefix);

      // Suffix branches
      final RadixTrie.Node<T> edge = compressEdges(tail.edges);

      return new RadixTrie.Node<T>(prefix.toString(), sibling, edge, tail.value);
    }

    private Node<T> tail(final StringBuilder prefix) {
      Node<T> node = this;
      while (true) {
        prefix.append(node.c);
        if (node.c == CAPTURE || node.value != null || node.edges.size() != 1) {
          return node;
        }
        final Node<T> next = node.edges.values().iterator().next();
        if (next.c == CAPTURE) {
          return node;
        }
        node = next;
      }
    }

    @Override
    public String toString() {
      return "Node{'" + (c == CAPTURE ? "<*>" : c) + "'" +
             ", capture=" + (c == CAPTURE) +
             ", edges=" + edges.size() +
             ", value=" + value +
             '}';
    }

    private int captures() {
      int captures = 0;
      for (final Node<T> edge : edges.values()) {
        captures = Math.max(captures, edge.captures());
      }
      return (c == CAPTURE) ? captures + 1 : captures;
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
