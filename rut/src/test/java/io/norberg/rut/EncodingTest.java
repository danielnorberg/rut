package io.norberg.rut;

import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static com.google.common.net.UrlEscapers.urlPathSegmentEscaper;
import static io.norberg.rut.Encoding.decode;
import static java.lang.Character.MAX_CODE_POINT;
import static java.lang.Character.MAX_SURROGATE;
import static java.lang.Character.MIN_CODE_POINT;
import static java.lang.Character.MIN_SURROGATE;
import static java.lang.Character.toChars;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class EncodingTest {

  public static final String ONE_BYTE = "$";
  public static final String ONE_BYTE_MIN = String.valueOf(toChars(0x0000));
  public static final String ONE_BYTE_MAX = String.valueOf(toChars(0x007F));

  public static final String ONE_BYTE_ENCODED = "%24";
  public static final String ONE_BYTE_MIN_ENCODED = "%00";
  public static final String ONE_BYTE_MAX_ENCODED = "%7F";
  public static final String ONE_BYTE_ENCODED_OVERLONG2 = "%C0%A4";

  public static final String TWO_BYTES = "¢";
  public static final String TWO_BYTES_MIN = String.valueOf(toChars(0x0080));
  public static final String TWO_BYTES_MAX = String.valueOf(toChars(0x07FF));

  // 11000010 10100010
  // C2       A2
  public static final String TWO_BYTES_ENCODED = "%C2%A2";
  public static final String TWO_BYTES_ENCODED_PERCENT_INVALID = "%C2%AG";

  // 11000010 10000010
  // C2       80
  public static final String TWO_BYTES_MIN_ENCODED = "%C2%80";

  // 11011111 10111111
  // DF       BF
  public static final String TWO_BYTES_MAX_ENCODED = "%DF%BF";

  // 11000010 11100010
  // C2       E2
  public static final String TWO_BYTES_ENCODED_INVALID = "%C2%E2";

  // 11100000 10000010 10000000
  // E0       9F       80
  public static final String TWO_BYTES_ENCODED_OVERLONG3A = "%E0%9F%80";

  // 11100000 10000010 10100010
  // E0       82       A2
  public static final String TWO_BYTES_ENCODED_OVERLONG3B = "%E0%82%A2";

  // 11110000 10000000 10000010 10100010
  // F0       80       82       A2
  public static final String TWO_BYTES_ENCODED_OVERLONG4 = "%F0%80%82%A2";

  public static final String THREE_BYTES_A = "€";
  public static final String THREE_BYTES_B = String.valueOf(toChars(0x82C));
  public static final String THREE_BYTES_MIN = String.valueOf(toChars(0x0800));
  public static final String THREE_BYTES_MAX = String.valueOf(toChars(0xFFFF));

  // 11100010 10000010 10101100
  // E2       82       AC
  public static final String THREE_BYTES_ENCODED_A = "%E2%82%AC";
  public static final String THREE_BYTES_ENCODED_A_PERCENT_INVALID = "%E2%82%AG";

  // 11100000 10100000 10000000
  // E0       A0       AC
  public static final String THREE_BYTES_ENCODED_B = "%E0%A0%AC";

  // 11100000 10100000 10000000
  // E0       A0       80
  public static final String THREE_BYTES_MIN_ENCODED = "%E0%A0%80";

  // 11101111 10111111 10111111
  // EF       BF       BF
  public static final String THREE_BYTES_MAX_ENCODED = "%EF%BF%BF";

  // 11100010 11000010 10101100
  // E2       C2       AC
  public static final String THREE_BYTES_ENCODED_INVALID1 = "%E2%C2%AC";

  // 11100010 10000010 11101100
  // E2       82       EC
  public static final String THREE_BYTES_ENCODED_INVALID2 = "%E2%82%EC";

  // 11110000 10000010 10000010 10101100
  // F0       82       82       AC
  public static final String THREE_BYTES_ENCODED_OVERLONG4 = "%F0%82%82%AC";

  public static final String FOUR_BYTES = String.valueOf(toChars(0x10348));

  public static final String FOUR_BYTES_MIN = String.valueOf(toChars(0x100000));
  public static final String FOUR_BYTES_MAX = String.valueOf(toChars(0x10FFFF));

  // 11110000 10010000 10001101 10001000
  // F0       90       8D       88
  public static final String FOUR_BYTES_ENCODED = "%F0%90%8D%88";
  public static final String FOUR_BYTES_ENCODED_PERCENT_INVALID = "%F0%90%8D%8G";

  // 11110100 10001111 10111111 10111111
  // F4       8F       BF       BF
  public static final String FOUR_BYTES_MAX_ENCODED = "%F4%8F%BF%BF";

  // 11110000 11010000 10001101 10001000
  // F0       D0       8D       88
  public static final String FOUR_BYTES_ENCODED_INVALID1 = "%F0%D0%8D%88";

  // 11110000 10010000 11001101 10001000
  // F0       90       CD       88
  public static final String FOUR_BYTES_ENCODED_INVALID2 = "%F0%90%CD%88";

  // 11110000 10010000 10001101 11001000
  // F0       90       8D       C8
  public static final String FOUR_BYTES_ENCODED_INVALID3 = "%F0%90%8D%C8";

  // 11110100 10010000 10000000 10000000
  // F4       90       80       80
  public static final String FOUR_BYTES_ENCODED_TOO_GREAT_F4 = "%F4%90%80%80";

  // 11110101 10000000 10000000 10000000
  // F5       80       80       80
  public static final String FOUR_BYTES_ENCODED_TOO_GREAT_F5 = "%F5%80%80%80";

  @Test
  public void verifyUninstantiable() {
    Access.verifyUninstantiable(Encoding.class);
  }

  @Test
  public void testDecode1() throws Exception {
    assertThat(decode(ONE_BYTE_ENCODED).toString(), is(ONE_BYTE));
    assertThat(decode(ONE_BYTE_MIN_ENCODED).toString(), is(ONE_BYTE_MIN));
    assertThat(decode(ONE_BYTE_MAX_ENCODED).toString(), is(ONE_BYTE_MAX));
  }

  @Test
  public void testDecode2() throws Exception {
    assertThat(decode(TWO_BYTES_ENCODED).toString(), is(TWO_BYTES));
    assertThat(decode(TWO_BYTES_MIN_ENCODED).toString(), is(TWO_BYTES_MIN));
    assertThat(decode(TWO_BYTES_MAX_ENCODED).toString(), is(TWO_BYTES_MAX));
  }

  @Test
  public void testDecode3() throws Exception {
    assertThat(decode(THREE_BYTES_ENCODED_A).toString(), is(THREE_BYTES_A));
    assertThat(decode(THREE_BYTES_ENCODED_B).toString(), is(THREE_BYTES_B));
    assertThat(decode(THREE_BYTES_MIN_ENCODED).toString(), is(THREE_BYTES_MIN));
    assertThat(decode(THREE_BYTES_MAX_ENCODED).toString(), is(THREE_BYTES_MAX));
  }

  @Test
  public void testDecode4() throws Exception {
    assertThat(decode(FOUR_BYTES_ENCODED).toString(), is(FOUR_BYTES));
    assertThat(decode(FOUR_BYTES_MAX_ENCODED).toString(), is(FOUR_BYTES_MAX));
  }

  @Test
  public void testDecodeMix() {
    final String expected = "mixed ascii with " +
                            "1 byte sequences (" + ONE_BYTE + ") and " +
                            "2 byte sequences (" + TWO_BYTES + ") and " +
                            "3 byte sequences (" + THREE_BYTES_A + ") and " +
                            "4 byte sequences (" + FOUR_BYTES + ")";

    final String encoded = "mixed ascii with " +
                           "1 byte sequences (" + ONE_BYTE_ENCODED + ") and " +
                           "2 byte sequences (" + TWO_BYTES_ENCODED + ") and " +
                           "3 byte sequences (" + THREE_BYTES_ENCODED_A + ") and " +
                           "4 byte sequences (" + FOUR_BYTES_ENCODED + ")";

    assertThat(String.valueOf(decode(encoded)), is(expected));
  }

  @Test
  public void verifyDecode1OverlongFails() throws Exception {
    assertThat(decode(ONE_BYTE_ENCODED_OVERLONG2), is(nullValue()));
  }

  @Test
  public void verifyDecode2Overlong3Fails() throws Exception {
    assertThat(decode(TWO_BYTES_ENCODED_OVERLONG3A), is(nullValue()));
    assertThat(decode(TWO_BYTES_ENCODED_OVERLONG3B), is(nullValue()));
  }

  @Test
  public void verifyDecode2Overlong4Fails() throws Exception {
    assertThat(decode(TWO_BYTES_ENCODED_OVERLONG4), is(nullValue()));
  }

  @Test
  public void verifyDecode3OverlongFails() throws Exception {
    assertThat(decode(THREE_BYTES_ENCODED_OVERLONG4), is(nullValue()));
  }

  @Test
  public void verifyDecode4TooGreatFails() throws Exception {
    assertThat(decode(FOUR_BYTES_ENCODED_TOO_GREAT_F4), is(nullValue()));
    assertThat(decode(FOUR_BYTES_ENCODED_TOO_GREAT_F5), is(nullValue()));
  }

  @Test
  public void verifyDecode2InvalidFails() throws Exception {
    assertThat(decode(TWO_BYTES_ENCODED_INVALID), is(nullValue()));
    assertThat(decode(TWO_BYTES_ENCODED_PERCENT_INVALID), is(nullValue()));
  }

  @Test
  public void verifyDecode3InvalidFails() throws Exception {
    assertThat(decode(THREE_BYTES_ENCODED_INVALID1), is(nullValue()));
    assertThat(decode(THREE_BYTES_ENCODED_INVALID2), is(nullValue()));
    assertThat(decode(THREE_BYTES_ENCODED_A_PERCENT_INVALID), is(nullValue()));
  }

  @Test
  public void verifyDecode4InvalidFails() throws Exception {
    assertThat(decode(FOUR_BYTES_ENCODED_INVALID1), is(nullValue()));
    assertThat(decode(FOUR_BYTES_ENCODED_INVALID2), is(nullValue()));
    assertThat(decode(FOUR_BYTES_ENCODED_INVALID3), is(nullValue()));
    assertThat(decode(FOUR_BYTES_ENCODED_PERCENT_INVALID), is(nullValue()));
  }

  @Test
  public void testEntireUnicodeRange() throws UnsupportedEncodingException {
    for (int i = MIN_CODE_POINT; i < MAX_CODE_POINT; i++) {
      if (isSurrogate((char) i)) {
        continue;
      }
      final String s = String.valueOf(Character.toChars(i));
      final String encoded = urlPathSegmentEscaper().escape(s);
      final CharSequence decoded = Encoding.decode(encoded);
      assertThat(decoded.toString(), is(s));
    }
  }

  public static boolean isSurrogate(char ch) {
    return ch >= MIN_SURROGATE && ch < (MAX_SURROGATE + 1);
  }
}
