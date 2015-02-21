package dano;

import java.util.ArrayList;
import java.util.List;

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
}
