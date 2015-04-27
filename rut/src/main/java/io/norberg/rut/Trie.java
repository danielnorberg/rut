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

  private static final String TYPE_PATH = "path";

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
    // TODO (dano): stricter input validation
    if (c > 127) {
      throw new IllegalArgumentException();
    }
    switch (c) {
      case '<':
        final int end = indexOf(path, '>', i + 1, path.length());
        if (end == -1) {
          throw new IllegalArgumentException(
              "unclosed capture: " + path.subSequence(i, path.length()).toString());
        }
        final String type = captureType(path, i, end);
        if (TYPE_PATH.equals(type)) {
          if (end + 1 != path.length()) {
            throw new IllegalArgumentException("path capture must be last");
          }
          Node<T> capture = nodes.get(CAPTURE_PATH);
          if (capture == null) {
            capture = new Node<T>(CAPTURE_PATH);
            nodes.put(CAPTURE_PATH, capture);
          }
          final T old = capture.value;
          capture.value = visitor.finish(captureIndex + 1, capture.value);
          return old;
        } else if (type == null) {
          Node<T> capture = nodes.get(CAPTURE_SEG);
          if (capture == null) {
            capture = new Node<T>(CAPTURE_SEG);
            nodes.put(CAPTURE_SEG, capture);
          }
          final T value = capture.extend(path, end + 1, captureIndex + 1, visitor);
          visitor.capture(captureIndex, path.subSequence(i + 1, end));
          return value;
        } else {
          throw new IllegalArgumentException("Unknown capture type: " + type);
        }

      default:
        Node<T> next = nodes.get(c);
        if (next == null) {
          next = new Node<T>(c);
          nodes.put(c, next);
        }
        return next.extend(path, i + 1, captureIndex, visitor);
    }
  }

  private static String captureType(final CharSequence path, final int i, final int end) {
    final int colon = indexOf(path, ':', i + 1, end);
    if (colon == -1) {
      return null;
    }
    return path.subSequence(colon + 1, end).toString();
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

  public interface Visitor<T> {

    void capture(final int i, final CharSequence s);

    T finish(final int captures, final T currentValue);
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

  private static int indexOf(final CharSequence sequence, final char needle, final int index,
                             final int end) {
    for (int i = index; i < end; i++) {
      if (sequence.charAt(i) == needle) {
        return i;
      }
    }
    return -1;
  }
}
