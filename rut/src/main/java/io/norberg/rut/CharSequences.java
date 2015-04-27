package io.norberg.rut;

final class CharSequences {

  private CharSequences() {
    throw new AssertionError();
  }

  static int indexOf(final CharSequence s, final char c, final int start, final int end) {
    for (int i = start; i < end; i++) {
      if (s.charAt(i) == c) {
        return i;
      }
    }
    return -1;
  }
}
