package io.norberg.rut;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.util.Collections.reverse;

final class Trie<T> {

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
    if (c >= CAPTURE) {
      throw new IllegalArgumentException();
    }
    switch (c) {
      case '<':
        Node<T> capture = nodes.get(CAPTURE);
        if (capture == null) {
          capture = new Node<T>(CAPTURE);
          nodes.put(CAPTURE, capture);
        }
        final int end = indexOf(path, '>', i + 1);
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
          next = new Node<T>(c);
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

  private final static class Node<T> {

    private final char c;
    private final Map<Character, Node<T>> edges = new TreeMap<Character, Node<T>>();
    private T value;

    private Node(final char c) {
      this.c = c;
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
      if (c == CAPTURE) {
        return new RadixTrie.Node<T>((byte) c, null, sibling, compressEdges(edges), value);
      }

      final StringBuilder prefix = new StringBuilder();
      final Node<T> end = compress(prefix);
      final RadixTrie.Node<T> edge = compressEdges(end.edges);

      final byte head = (byte) prefix.charAt(0);
      final byte[] tail = prefix.length() == 1 ? null : asciiBytes(prefix, 1);

      return new RadixTrie.Node<T>(head, tail, sibling, edge, end.value);
    }

    private Node<T> compress(final StringBuilder prefix) {
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

  private static <T> Collection<T> reversed(final Collection<T> values) {
    final List<T> list = new ArrayList<T>(values);
    reverse(list);
    return list;
  }

  private static int indexOf(final CharSequence sequence, final char needle, final int index) {
    for (int i = index; i < sequence.length(); i++) {
      if (sequence.charAt(i) == needle) {
        return i;
      }
    }
    return -1;
  }

  private static byte[] asciiBytes(final CharSequence sequence, final int from) {
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
