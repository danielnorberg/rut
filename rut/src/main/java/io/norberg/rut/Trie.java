package io.norberg.rut;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.util.Collections.reverse;

final class Trie<T> {

  private static final char CAPTURE_SEG = 0x1000;
  private static final char CAPTURE_PATH = 0x2000;

  private final Map<Character, Node<T>> roots = new TreeMap<Character, Node<T>>();

  T insert(final Path path, final T value) {
    return insert(path, new DefaultVisitor(value));
  }

  T insert(final Path path, final Visitor<T> visitor) {
    return insert(path, null, roots, 0, visitor);
  }

  private static <T> T insert(final Path path, final Node<T> node,
                              final Map<Character, Node<T>> edges,
                              final int partIndex, final Visitor<T> visitor) {
    if (partIndex == path.parts().size()) {
      final T old = node.value;
      node.value = visitor.finish(node.value);
      return old;
    }

    final Path.Part part = path.parts().get(partIndex);

    if (part instanceof Path.Match) {
      return insertMatch(path, node, edges, partIndex, visitor, (Path.Match) part, 0);
    }
    if (part instanceof Path.CaptureSegment) {
      return insertCaptureSegment(path, edges, partIndex, visitor);
    }
    // part instanceof Path.CapturePath
    return insertCapturePath(edges, visitor);
  }

  private static <T> T insertCapturePath(final Map<Character, Node<T>> edges,
                                         final Visitor<T> visitor) {
    Node<T> capture = edges.get(CAPTURE_PATH);
    if (capture == null) {
      capture = new Node<T>(CAPTURE_PATH);
      edges.put(CAPTURE_PATH, capture);
    }
    final T old = capture.value;
    capture.value = visitor.finish(capture.value);
    return old;
  }

  private static <T> T insertCaptureSegment(final Path path, final Map<Character, Node<T>> edges,
                                            final int partIndex, final Visitor<T> visitor) {
    Node<T> capture = edges.get(CAPTURE_SEG);
    if (capture == null) {
      capture = new Node<T>(CAPTURE_SEG);
      edges.put(CAPTURE_SEG, capture);
    }
    return insert(path, capture, capture.edges, partIndex + 1, visitor);
  }

  private static <T> T insertMatch(final Path path, final Node<T> node,
                                   final Map<Character, Node<T>> edges,
                                   final int pi, final Visitor<T> visitor,
                                   final Path.Match part, final int ci) {
    final String string = part.string();
    if (ci == string.length()) {
      return insert(path, node, edges, pi + 1, visitor);
    }
    final char c = string.charAt(ci);
    Node<T> next = edges.get(c);
    if (next == null) {
      next = new Node<T>(c);
      edges.put(c, next);
    }

    return insertMatch(path, next, next.edges, pi, visitor, part, ci + 1);
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

  private final static class Node<T> {

    private static final char SLASH = '/';

    private final char c;
    private final Map<Character, Node<T>> edges = new TreeMap<Character, Node<T>>();
    private T value;

    private Node(final char c) {
      this(c, null);
    }

    private Node(final char c, final T value) {
      this.c = c;
      this.value = value;
    }

    private RadixTrie.Node<T> compress(final RadixTrie.Node<T> sibling) {
      if (c == CAPTURE_SEG) {
        if (edges.size() == 0) {
          return RadixTrie.Node.terminalCaptureSeg(sibling, value);
        }
        if (edges.size() == 1) {
          final Node<T> edge = edges.values().iterator().next();
          if (edge.c == SLASH) {
            return RadixTrie.Node.captureFullSeg(sibling, compressEdges(edges), value);
          }
        }
        return RadixTrie.Node.captureSeg(sibling, compressEdges(edges), value);
      } else if (c == CAPTURE_PATH) {
        return RadixTrie.Node.capturePath(sibling, value);
      }

      final StringBuilder prefix = new StringBuilder();
      final Node<T> end = compress(prefix);
      final RadixTrie.Node<T> edge = compressEdges(end.edges);

      return RadixTrie.Node.match(prefix, sibling, edge, end.value);
    }

    private Node<T> compress(final StringBuilder prefix) {
      Node<T> node = this;
      while (true) {
        prefix.append(node.c);
        if (node.value != null || node.edges.size() != 1) {
          return node;
        }
        final Node<T> next = node.edges.values().iterator().next();
        if (next.c == CAPTURE_SEG || next.c == CAPTURE_PATH) {
          return node;
        }
        node = next;
      }
    }

    @Override
    public String toString() {
      return "Node{'" + name() + "'" +
             ", edges=" + edges.size() +
             ", value=" + value +
             '}';
    }

    private String name() {
      switch (c) {
        case CAPTURE_SEG:
          return "<*>";
        case CAPTURE_PATH:
          return "<*:path>";
        default:
          return String.valueOf(c);
      }
    }
  }

  @Override
  public String toString() {
    return "Trie{" +
           "roots=" + roots +
           '}';
  }

  interface Visitor<T> {

    T finish(final T currentValue);
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
    public T finish(final T currentValue) {
      return value;
    }
  }

  private static <T> Collection<T> reversed(final Collection<T> values) {
    final List<T> list = new ArrayList<T>(values);
    reverse(list);
    return list;
  }
}
