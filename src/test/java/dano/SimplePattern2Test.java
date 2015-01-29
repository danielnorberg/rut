package dano;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SimplePattern2Test {

  public static final SimplePattern2.Result RESULT = SimplePattern2.result(128);

  @Test
  public void testOf() throws Exception {
    SimplePattern2.of("foo-<wildcard>-baz");
  }

  @Test
  public void testMatcherMatchesSingleValue() throws Exception {
    final SimplePattern2 pattern = SimplePattern2.of("foo-<wildcard>-baz");
    final SimpleMatcher2 matcher = pattern.matcher("foo-bar-baz");
    assertThat(pattern.match("foo-bar-baz", RESULT), is(true));
    assertThat(matcher.matches(), is(true));
    assertThat(matcher.value(0).toString(), is("bar"));
  }

  @Test
  public void testMatcherMatchesSingleValueWithSuffixInValue() throws Exception {
    final SimplePattern2 pattern = SimplePattern2.of("foo-<wildcard>-baz");
    final SimpleMatcher2 matcher = pattern.matcher("foo-bar-with-baz-and-dash-baz");
    assertThat(pattern.match("foo-bar-with-baz-and-dash-baz", RESULT), is(true));
    assertThat(matcher.matches(), is(true));
    assertThat(matcher.value(0).toString(), is("bar-with-baz-and-dash"));
  }

  @Test
  public void testMatcherMatchesMultipleValue() throws Exception {
    final SimplePattern2 pattern = SimplePattern2.of("foo-<w1>-bar-<w2>-baz");
    final SimpleMatcher2 matcher = pattern.matcher("foo-data-1-bar-data-2-baz");
    assertThat(pattern.match("foo-data-1-bar-data-2-baz", RESULT), is(true));
    assertThat(matcher.matches(), is(true));
    assertThat(matcher.value(0).toString(), is("data-1"));
    assertThat(matcher.value(1).toString(), is("data-2"));
  }

  @Test
  public void testMatcherFailsSimilarSingleValue() throws Exception {
    final SimplePattern2 pattern = SimplePattern2.of("foo-<wildcard>-baz");
    final SimpleMatcher2 matcher = pattern.matcher("foo-bar-baz-nopes");
    assertThat(matcher.matches(), is(false));
  }

  @Test
  public void testMatcherFailsSingleValue() throws Exception {
    final SimplePattern2 pattern = SimplePattern2.of("foo-<wildcard>-baz");
    final SimpleMatcher2 matcher = pattern.matcher("no-match-here");
    assertThat(matcher.matches(), is(false));
  }
}