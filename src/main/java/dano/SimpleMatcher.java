package dano;

public class SimpleMatcher {

  private final String[] segments;
  private final String haystack;
  private final String[] values;

  private boolean matches;

  public SimpleMatcher(final String[] segments, final String haystack) {
    this.segments = segments;
    this.haystack = haystack;
    this.values = new String[segments.length - 1];
    this.matches = match(haystack);
  }

  private boolean match(final String haystack) {
    return match(haystack, 0, 0);
  }

  private boolean match(final String haystack, final int index, final int segmentIndex) {
    if (segmentIndex >= segments.length) {
      return false;
    }
    final String segment = segments[segmentIndex];
    if (!equals(haystack, index, segment)) {
      return false;
    }
    final int newIndex = index + segment.length();
    final int newSegmentIndex = segmentIndex + 1;
    final int remaining = haystack.length() - newIndex;
    if (remaining == 0) {
      return true;
    }
    for (int i = 0; i < remaining; i++) {
      if (match(haystack, newIndex + i, newSegmentIndex)) {
        final String value = haystack.substring(newIndex, newIndex + i);
        values[segmentIndex] = value;
        return true;
      }
    }
    return false;
  }

  private boolean equals(final String haystack, final int index, final String needle) {
    if (index + needle.length() > haystack.length()) {
      return false;
    }
    for (int i = 0; i < needle.length(); i++) {
      if (haystack.charAt(index + i) != needle.charAt(i)) {
        return false;
      }
    }
    return true;
  }

  public boolean matches() {
    return matches;
  }

  public String value(final int i) {
    return values[i];
  }
}
