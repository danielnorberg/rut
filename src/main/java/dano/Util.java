package dano;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.reverse;

public class Util {

  static CharSequence[] splitCaptures(final CharSequence s) {
    final List<CharSequence> segments = new ArrayList<CharSequence>();
    int i = 0;
    while (i < s.length()) {
      final int start = indexOf(s, '<', i);
      if (start == -1) {
        segments.add(s.subSequence(i, s.length()));
        break;
      }
      segments.add(s.subSequence(i, start));
      final int end = indexOf(s, '>', start + 1);
      if (end == -1) {
        throw new AssertionError("unfinished capture");
      }
      segments.add(s.subSequence(start, end + 1));
      i = end + 1;
    }
    return segments.toArray(new CharSequence[segments.size()]);
  }

  static int indexOf(final CharSequence sequence, final char needle, final int index) {
    for (int i = index; i < sequence.length(); i++) {
      if (sequence.charAt(i) == needle) {
        return i;
      }
    }
    return -1;
  }

  static char[] toCharArray(final CharSequence sequence, final int from) {
    final int length = sequence.length() - from;
    final char[] chars = new char[length];
    for (int i = 0; i < length; i++) {
      chars[i] = sequence.charAt(from + i);
    }
    return chars;
  }

  static <T> Collection<T> reversed(final Collection<T> values) {
    final List<T> list = new ArrayList<T>(values);
    reverse(list);
    return list;
  }
}
