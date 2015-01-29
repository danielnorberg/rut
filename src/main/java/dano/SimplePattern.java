package dano;

import java.util.ArrayList;
import java.util.List;

public class SimplePattern {

  private final String[] segments;

  public SimplePattern(final String needle) {
    this.segments = segments(needle);
  }

  private static String[] segments(final String needle) {
    final List<String> segments = new ArrayList<String>();
    StringBuilder segment = new StringBuilder();
    for (int i = 0; i < needle.length(); i++) {
      final char c = needle.charAt(i);
      if (segment != null) {
        // Segment
        if (c == '<') {
          segments.add(segment.toString());
          segment = null;
        } else {
          segment.append(c);
        }
      } else {
        // Wildcard
        if (c == '>') {
          segment = new StringBuilder();
        }
      }
    }
    if (segment == null) {
      throw new AssertionError("unfinished wildcard");
    }
    segments.add(segment.toString());
    return segments.toArray(new String[segments.size()]);
  }

  public static SimplePattern of(final String needle) {
    return new SimplePattern(needle);
  }


  public SimpleMatcher matcher(final String haystack) {
    return new SimpleMatcher(segments, haystack);
  }
}
