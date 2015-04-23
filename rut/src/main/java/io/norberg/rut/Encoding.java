/**
 * Copyright (C) 2015 Spotify AB
 */

package io.norberg.rut;

import sun.nio.cs.ThreadLocalCoders;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

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
   * Percent decode a {@link CharSequence}. Returns null if invalid.
   *
   * @param s Percent encoded {@link CharSequence}.
   * @return Decoded CharSequence or null if invalid encoding.
   */
  private static CharSequence decode0(final CharSequence s) {
    final CharsetDecoder dec = ThreadLocalCoders.decoderFor("UTF-8")
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE);
    final int length = s.length();
    final CharBuffer cb = CharBuffer.allocate(length);
    ByteBuffer bb = null;

    for (int i = 0; i < length; ) {
      final char c = s.charAt(i);

      // Not encoded?
      if (c != '%') {
        cb.append(c);
        i++;
        continue;
      }

      // Ascii?
      int b = decodePercent(s, length, i);
      if (b == INVALID) {
        return null;
      }
      i += 3;
      if (isAscii((byte) b)) {
        cb.append((char) b);
        continue;
      }

      // UTF-8

      // TODO (dano): manually decode code point and eliminate this temp buffer allocation
      if (bb == null) {
        bb = ByteBuffer.allocate(length);
      }

      // Decode the percent encoded characters to raw UTF-8.
      bb.put((byte) b);
      for (; i < length && s.charAt(i) == '%'; i += 3) {
        b = decodePercent(s, length, i);
        if (b == INVALID) {
          return null;
        }
        bb.put((byte) b);
      }
      bb.flip();

      // Decode the raw UTF-8 to characters.
      dec.decode(bb, cb, true);
      bb.position(0).limit(bb.capacity());
      dec.reset();
    }

    cb.flip();
    return cb;
  }

  /**
   * Check whether a byte is ascii or the start of a multi-byte UTF-8 sequence.
   */
  private static boolean isAscii(final byte b) {
    return b >= 0;
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
}
