package dano;

import java.util.List;

public class RadixTrie<T> {

  private final Node<T> root;

  public RadixTrie(final Node<T> root) {
    this.root = root;
  }

  public T lookup(final CharSequence s) {
    return root.lookup(s, 0);
  }

  public static <T> Builder<T> builder(final Class<T> clazz) {
    return new Builder<T>();
  }

  public static class Node<T> {

    private final String prefix;
    private final List<Node<T>> children;
    private final Node<T> capture;
    private final T value;

    public Node(final String prefix, final Node<T> capture,
                final List<Node<T>> children, final T value) {

      this.prefix = prefix;
      this.capture = capture;
      this.children = children;
      this.value = value;
    }

    public T lookup(final CharSequence s, final int index) {
      if (!matchPrefix(s, index)) {
        return null;
      }
      final int newIndex = index + prefix.length();
      if (newIndex == s.length()) {
        return value;
      }
      final T childMatch = lookupChildren(s, newIndex);
      if (childMatch != null) {
        return childMatch;
      }
      return lookupCapture(s, newIndex);
    }

    private boolean matchPrefix(final CharSequence s, final int index) {
      if (prefix.length() + index > s.length()) {
        return false;
      }
      for (int i = 0; i < prefix.length(); i++) {
        final char a = prefix.charAt(i);
        final char b = s.charAt(index + i);
        if (a != b) {
          return false;
        }
      }
      return true;
    }

    private T lookupCapture(final CharSequence s, final int newIndex) {
      if (capture == null) {
        return null;
      }
      final int maxCapture = indexOf(s, newIndex, '/') - 1;
      for (int i = maxCapture; i >= newIndex; i--) {
        final T value = capture.lookup(s, i);
        if (value != null) {
          return value;
        }
      }
      return null;
    }

    private T lookupChildren(final CharSequence s, final int newIndex) {
      for (int i = 0; i < children.size(); i++) {
        final Node<T> child = children.get(i);
        final T value = child.lookup(s, newIndex);
        if (value != null) {
          return value;
        }
      }
      return null;
    }

    private int indexOf(final CharSequence s, final int start, final char c) {
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
      return "Node{" +
             "prefix='" + prefix + '\'' +
             ", children=" + children.size() +
             ", capture=" + (capture != null) +
             ", value=" + value +
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
