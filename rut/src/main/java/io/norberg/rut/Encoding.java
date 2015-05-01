/**
 * Copyright (C) 2015 Spotify AB
 */

package io.norberg.rut;

import java.nio.CharBuffer;

import static java.lang.Character.highSurrogate;
import static java.lang.Character.lowSurrogate;

final class Encoding {

  private static final int INVALID = Integer.MIN_VALUE;

  private Encoding() {
    throw new AssertionError();
  }

  /**
   * Percent decode a {@link CharSequence}.
   *
   * @param s Percent encoded {@link CharSequence}.
   * @return Decoded CharSequence or null if invalid encoding.
   */
  static CharSequence decode(final CharSequence s) {
    if (!contains(s, '%')) {
      return s.toString();
    }
    return decode0(s);
  }

  /**
   * Percent and UTF8 decode a {@link CharSequence}. Returns null if invalid.
   *
   * @param s Percent encoded {@link CharSequence}.
   * @return Decoded CharSequence or null if invalid encoding.
   */
  private static CharSequence decode0(final CharSequence s) {
    final int length = s.length();
    final CharBuffer cb = CharBuffer.allocate(length);

    for (int i = 0; i < length; ) {
      final char c = s.charAt(i);

      // Not encoded?
      if (c != '%') {
        cb.append(c);
        i++;
        continue;
      }

      // UTF8 - 1 Byte
      int b1 = decodePercent(s, length, i);
      if (b1 == INVALID) {
        return null;
      }
      i += 3;
      final int n = utf8Length(b1);
      if (n == INVALID) {
        return null;
      }
      if (n == 1) {
        cb.append((char) b1);
        continue;
      }

      // UTF8 - 2 Bytes
      final int b2 = decodePercent(s, length, i);
      if (b2 == INVALID) {
        return null;
      }
      i += 3;
      if (n == 2) {
        final int cp = utf8Read2(b1, b2);
        if (cp == INVALID) {
          return null;
        }
        cb.append((char) cp);
        continue;
      }

      // UTF8 - 3 Bytes
      final int b3 = decodePercent(s, length, i);
      if (b3 == INVALID) {
        return null;
      }
      i += 3;
      if (n == 3) {
        final int cp = utf8Read3(b1, b2, b3);
        if (cp == INVALID) {
          return null;
        }
        cb.append((char) cp);
        continue;
      }

      // UTF8 - 4 Bytes
      final int b4 = decodePercent(s, length, i);
      if (b4 == INVALID) {
        return null;
      }
      i += 3;
      final int cp = utf8Read4(b1, b2, b3, b4);
      if (cp == INVALID) {
        return null;
      }
      cb.append(highSurrogate(cp));
      cb.append(lowSurrogate(cp));
    }

    cb.flip();
    return cb;
  }

  /**
   * Decode a percent encoded byte. E.g. "%3F" -> 63.
   */
  private static int decodePercent(final CharSequence s, final int length, final int i) {
    if (i + 2 >= length) {
      return INVALID;
    }
    final char n1 = s.charAt(i + 1);
    final char n2 = s.charAt(i + 2);
    return decodeNibbles(n1, n2);
  }

  /**
   * Decode two hex nibbles to a byte. E.g. '3' and 'F' -> 63.
   */
  private static int decodeNibbles(final char c1, final char c2) {
    final int n1 = decodeHex(c1);
    if (n1 == INVALID) {
      return INVALID;
    }
    final int n2 = decodeHex(c2);
    if (n2 == INVALID) {
      return INVALID;
    }
    return (((n1 & 0xf) << 4) | (n2 & 0xf));
  }

  /**
   * Decode a hex nibble. E.g. '3' -> 3 and 'F' -> 15.
   */
  private static int decodeHex(final char c) {
    if (c < '0') {
      return INVALID;
    }
    if (c <= '9') {
      return c - '0';
    }
    if (c < 'A') {
      return INVALID;
    }
    if (c <= 'F') {
      return c - 'A' + 10;
    }
    if (c < 'a') {
      return INVALID;
    }
    if (c <= 'f') {
      return c - 'a' + 10;
    }
    return INVALID;
  }

  /**
   * Check whether a {@link CharSequence} contains a specific character.
   */
  private static boolean contains(final CharSequence s, final char c) {
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) == c) {
        return true;
      }
    }
    return false;
  }

  /**
   * Decode UTF8 sequence length.
   */
  private static int utf8Length(int c) {
    if (c < 0x80) {
      // 1 byte, 7 bits: 0xxxxxxx
      return 1;
    } else if (c < 0xC2) {
      // continuation or overlong 2-byte sequence
      return INVALID;
    } else if (c < 0xE0) {
      // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
      return 2;
    } else if (c < 0xF0) {
      // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
      return 3;
    } else if (c < 0xF5) {
      // 4 bytes, 21 bits: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
      return 4;
    } else {
      // > U+10FFFF
      return INVALID;
    }
  }

  /**
   * Read a 2 byte UTF8 sequence.
   * @return the resulting code point or {@link #INVALID} if invalid.
   */
  private static int utf8Read2(int cu1, int cu2) {
    if ((cu2 & 0xC0) != 0x80) {
      return INVALID;
    }
    return (cu1 << 6) + cu2 - 0x3080;
  }

  /**
   * Read a 3 byte UTF8 sequence.
   * @return the resulting code point or {@link #INVALID} if invalid.
   */
  private static int utf8Read3(int cu1, int cu2, int cu3) {
    if ((cu2 & 0xC0) != 0x80) {
      return INVALID;
    }
    if (cu1 == 0xE0 && cu2 < 0xA0) {
      // overlong
      return INVALID;
    }
    if ((cu3 & 0xC0) != 0x80) {
      return INVALID;
    }
    return (cu1 << 12) + (cu2 << 6) + cu3 - 0xE2080;
  }

  /**
   * Read a 4 byte UTF8 sequence.
   * @return the resulting code point or {@link #INVALID} if invalid.
   */
  private static int utf8Read4(int cu1, int cu2, int cu3, int cu4) {
    if ((cu2 & 0xC0) != 0x80) {
      return INVALID;
    }
    if (cu1 == 0xF0 && cu2 < 0x90) {
      return INVALID; // overlong
    }
    if (cu1 == 0xF4 && cu2 >= 0x90) {
      return INVALID; // > U+10FFFF
    }
    if ((cu3 & 0xC0) != 0x80) {
      return INVALID;
    }
    if ((cu4 & 0xC0) != 0x80) {
      return INVALID;
    }
    return (cu1 << 18) + (cu2 << 12) + (cu3 << 6) + cu4 - 0x3C82080;
  }
}
