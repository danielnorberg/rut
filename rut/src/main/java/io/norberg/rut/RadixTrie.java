package io.norberg.rut;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static io.norberg.rut.RadixTrie.Node.fanout;
import static java.lang.Math.max;

final class RadixTrie<T> {

  private static final Charset ASCII = Charset.forName("US-ASCII");

  private static final byte CAPTURE_SEG = -128;
  private static final byte CAPTURE_PATH = -127;

  private static final byte SLASH = '/';
  private static final byte QUERY = '?';

  private final Node<T> root;
  private final int captures;

  RadixTrie(final Node<T> root) {
    this.root = root;
    this.captures = root.captures();
  }

  T lookup(final CharSequence path) {
    return lookup(path, captor());
  }

  T lookup(final CharSequence path, final Captor captor) {
    captor.reset();
    return fanout(root, path, 0, captor, 0);
  }

  int captures() {
    return captures;
  }

  Captor captor() {
    return captor(captures);
  }

  static Captor captor(final int captures) {
    return new Captor(captures);
  }

  static <T> Builder<T> builder() {
    return new Builder<T>();
  }

  @SuppressWarnings("UnusedParameters")
  static <T> Builder<T> builder(Class<T> clazz) {
    return new Builder<T>();
  }

  static final class Node<T> {

    private final byte head;
    private final byte[] tail;
    private final Node<T> sibling;
    private final Node<T> edge;
    private final T value;

    private Node(final byte head, final byte[] tail, final Node<T> sibling, final Node<T> edge,
                 final T value) {
      this.head = head;
      this.tail = tail;
      this.sibling = sibling;
      this.edge = edge;
      this.value = value;

      // Verify that match siblings are ordered
      if (sibling != null && head > 0 && sibling.head > 0 && head > sibling.head) {
        throw new IllegalArgumentException("unordered sibling");
      }

      // Verify that sibling heads are unique
      if (sibling != null && head == sibling.head) {
        throw new IllegalArgumentException("duplicate sibling head");
      }

      // Verify that the seg capture is last or followed by path capture
      if (head == CAPTURE_SEG && sibling != null && sibling.head != CAPTURE_PATH) {
        throw new IllegalArgumentException("seg capture must be last or followed by path capture");
      }

      // Verify that the path capture is last
      if (head == CAPTURE_PATH && sibling != null) {
        throw new IllegalArgumentException("path capture must be last sibling");
      }

      // Verify that terminal nodes have values
      if (value == null && edge == null) {
        throw new IllegalArgumentException("terminal node without value");
      }
    }

    private int captures() {
      final int captures = (head < 0) ? 1 : 0;
      final int edgeCaptures = (edge == null) ? 0 : edge.captures();
      final int siblingCaptures = (sibling == null) ? 0 : sibling.captures();
      return captures + max(edgeCaptures, siblingCaptures);
    }

    static <T> T fanout(final Node<T> root, final CharSequence path, final int i,
                        final Captor captor, final int capture) {
      if (root == null) {
        return null;
      }

      if (i == path.length()) {
        return terminalFanout(root, captor, capture);
      }

      final char c = path.charAt(i);

      if (c == QUERY) {
        return terminalFanout(root, captor, capture);
      }

      Node<T> node = root;
      byte head;

      // Seek single potential matching node. This will be at any place in the ordered list.
      do {
        head = node.head;
        if (head < 0) {
          break;
        }
        if (head == c) {
          final T value = node.match(path, i, captor, capture);
          if (value != null) {
            return value;
          }
          break;
        }
        if (node.sibling == null) {
          break;
        }
        node = node.sibling;
      } while (true);

      // Seek potential capture nodes. These can be the second two last nodes in the list,
      // with the seg capture node before the path capture node.
      do {
        if (node.head == CAPTURE_SEG) {
          final T value = node.captureSeg(path, i, captor, capture);
          if (value != null) {
            return value;
          }
        }
        if (node.head == CAPTURE_PATH) {
          return node.capturePath(path, i, captor, capture);
        }
        node = node.sibling;
      } while (node != null);

      return null;
    }

    private static <T> T terminalFanout(Node<T> node, final Captor captor, final int capture) {
      if (!captor.optionalTrailingSlash) {
        return null;
      }

      // Trailing slash in prefix?
      byte head;
      do {
        head = node.head;
        if (head < 0) {
          break;
        }
        if (head == SLASH && node.tail == null) {
          if (node.value != null) {
            captor.match(capture);
          }
          return node.value;
        }
        node = node.sibling;
      } while (node != null);

      return null;
    }

    private T match(final CharSequence path, final int index, final Captor captor,
                    final int capture) {
      // Match prefix
      final int length = path.length();
      final int next;
      if (tail == null) {
        next = index + 1;
      } else {
        next = index + 1 + tail.length;
        if (next > length) {
          // Trailing slash in prefix?
          if (captor.optionalTrailingSlash) {
            if (value != null &&
                tail[tail.length - 1] == SLASH &&
                next == length + 1) {
              for (int i = 0; i < tail.length - 1; i++) {
                if (tail[i] != path.charAt(index + 1 + i)) {
                  return null;
                }
              }
              captor.match(capture);
              return value;
            }
          }
          return null;
        }
        for (int i = 0; i < tail.length; i++) {
          if (tail[i] != path.charAt(index + 1 + i)) {
            // Trailing slash in prefix?
            if (captor.optionalTrailingSlash) {
              if (value != null &&
                  i == tail.length - 1 &&
                  tail[tail.length - 1] == SLASH &&
                  path.charAt(index + 1 + i) == QUERY) {
                captor.query(index + 2 + i, length);
                captor.match(capture);
                return value;
              }
            }
            return null;
          }
        }
      }

      // Terminal?
      if (next == length) {
        if (value != null) {
          captor.match(capture);
          return value;
        }
        return terminalFanout(edge, captor, capture);
      }

      // Query?
      final char c = path.charAt(next);
      if (c == QUERY) {
        if (value != null) {
          captor.query(next + 1, length);
          captor.match(capture);
          return value;
        }
        final T value = terminalFanout(edge, captor, capture);
        if (value != null) {
          captor.query(next + 1, length);
          return value;
        }
        return null;
      }

      // Edge fanout
      final T value = fanout(edge, path, next, captor, capture);
      if (value != null) {
        return value;
      }

      // Trailing slash in path?
      if (captor.optionalTrailingSlash) {
        if (this.value != null && c == SLASH) {
          if (next + 1 == length) {
            captor.match(capture);
            return this.value;
          } else if (path.charAt(next + 1) == QUERY) {
            captor.match(capture);
            captor.query(next + 2, length);
            return this.value;
          }
        }
      }

      return null;
    }

    private T capturePath(final CharSequence path, final int index, final Captor captor,
                          final int capture) {
      // value != null

      int i;
      char c;

      // Find capture bound
      final int length = path.length();
      for (i = index; i < length; i++) {
        c = path.charAt(i);
        if (c == QUERY) {
          captor.query(i + 1, length);
          break;
        }
      }

      captor.match(capture + 1);
      captor.capture(capture, index, i);
      return value;
    }

    private T captureSeg(final CharSequence path, final int index, final Captor captor,
                         final int capture) {
      int i;
      char c;

      // Find capture bound
      final int length = path.length();
      boolean terminal = true;
      for (i = index; i < length; i++) {
        c = path.charAt(i);
        if (c == SLASH) {
          terminal = false;
          break;
        }
        if (c == QUERY) {
          captor.query(i + 1, length);
          break;
        }
      }
      final int limit = i;

      // Terminal?
      if (value != null) {
        if (terminal) {
          captor.match(capture + 1);
          captor.capture(capture, index, limit);
          return value;
        }

        // Trailing slash in path?
        if (captor.optionalTrailingSlash) {
          if (limit + 1 == length) { // c == SLASH
            captor.match(capture + 1);
            captor.capture(capture, index, limit);
            return value;
          } else if (path.charAt(limit + 1) == QUERY) { // limit + 1 < length
            captor.match(capture + 1);
            captor.capture(capture, index, i);
            captor.query(limit + 2, length);
            return value;
          }
        }
      }

      // Fanout
      if (edge != null) {
        for (i = limit; i >= index; i--) {
          final T value = fanout(edge, path, i, captor, capture + 1);
          if (value != null) {
            captor.capture(capture, index, i);
            return value;
          }
        }
      }

      return null;
    }

    private String prefix() {
      if (head == CAPTURE_SEG) {
        return "<*>";
      } else {
        if (tail == null) {
          return String.valueOf((char) head);
        } else {
          final StringBuilder b = new StringBuilder().append((char) head);
          for (final byte c : tail) {
            b.append((char) c);
          }
          return b.toString();
        }
      }
    }

    @Override
    public String toString() {
      return "Node{'" + prefix() + "\': " +
             "e=" + prefixes(edge) +
             ", v=" + (value == null ? "" : value.toString()) +
             '}';
    }

    static <T> Node<T> captureSeg(final Node<T> sibling, final Node<T> edge, final T value) {
      return new Node<T>(CAPTURE_SEG, null, sibling, edge, value);
    }

    static <T> Node<T> capturePath(final Node<T> sibling, final T value) {
      return new Node<T>(CAPTURE_PATH, null, sibling, null, value);
    }

    static <T> Node<T> match(final CharSequence prefix, final Node<T> sibling,
                             final Node<T> edge, final T value) {
      final byte head = (byte) prefix.charAt(0);
      final byte[] tail = prefix.length() == 1
                          ? null
                          : prefix.subSequence(1, prefix.length()).toString().getBytes(ASCII);
      return new Node<T>(head, tail, sibling, edge, value);
    }
  }

  private static <T> String prefixes(Node<T> node) {
    final List<String> prefixes = new ArrayList<String>();
    while (node != null) {
      prefixes.add(node.prefix());
      node = node.sibling;
    }
    return prefixes.toString();
  }

  @Override
  public String toString() {
    return "RadixTrie{" + root + "}";
  }

  final static class Builder<T> {

    private Builder() {
    }

    private final Trie<T> trie = new Trie<T>();

    Builder<T> insert(final CharSequence path, final T value) {
      trie.insert(path, value);
      return this;
    }

    Builder<T> insert(final CharSequence path, final Trie.Visitor<T> visitor) {
      trie.insert(path, visitor);
      return this;
    }

    RadixTrie<T> build() {
      return trie.compress();
    }

    @Override
    public String toString() {
      return "Builder{" +
             "trie=" + trie +
             '}';
    }
  }

  final static class Captor {

    private final int[] start;
    private final int[] end;

    private boolean match;
    private int captured;
    private int queryStart;
    private int queryEnd;

    private boolean optionalTrailingSlash;

    Captor(final int captures) {
      this.start = new int[captures];
      this.end = new int[captures];
    }

    void optionalTrailingSlash(final boolean optionalTrailingSlash) {
      this.optionalTrailingSlash = optionalTrailingSlash;
    }

    private void reset() {
      match = false;
      captured = 0;
      queryStart = -1;
      queryEnd = -1;
    }

    private void capture(final int i, final int start, final int end) {
      this.start[i] = start;
      this.end[i] = end;
    }

    private void match(final int captured) {
      match = true;
      this.captured = captured;
    }

    boolean isMatch() {
      return match;
    }

    int values() {
      return captured;
    }

    int valueStart(final int i) {
      if (!match) {
        throw new IllegalStateException("not matched");
      }
      if (i >= captured) {
        throw new IndexOutOfBoundsException();
      }
      return start[i];
    }

    int valueEnd(final int i) {
      if (!match) {
        throw new IllegalStateException("not matched");
      }
      if (i >= captured) {
        throw new IndexOutOfBoundsException();
      }
      return end[i];
    }

    CharSequence value(final CharSequence haystack, final int i) {
      if (!match) {
        throw new IllegalStateException("not matched");
      }
      if (i >= captured) {
        throw new IndexOutOfBoundsException();
      }
      return haystack.subSequence(start[i], end[i]);
    }

    private void query(final int start, final int end) {
      this.queryStart = start;
      this.queryEnd = end;
    }

    public int queryStart() {
      return queryStart;
    }

    public int queryEnd() {
      return queryEnd;
    }

    public CharSequence query(final CharSequence haystack) {
      if (queryStart == -1) {
        return null;
      }
      return haystack.subSequence(queryStart, queryEnd);
    }
  }
}
