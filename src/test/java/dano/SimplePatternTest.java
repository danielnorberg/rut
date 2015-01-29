package dano;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SimplePatternTest {

  @Test
  public void testOf() throws Exception {
    SimplePattern.of("foo-<wildcard>-baz");
  }

  @Test
  public void testMatcherMatchesSingleValue() throws Exception {
    final SimplePattern pattern = SimplePattern.of("foo-<wildcard>-baz");
    final SimpleMatcher matcher = pattern.matcher("foo-bar-baz");
    assertThat(matcher.matches(), is(true)) ;
    assertThat(matcher.value(0), is("bar"));
  }

  @Test
  public void testMatcherMatchesSingleValueWithSuffixInValue() throws Exception {
    final SimplePattern pattern = SimplePattern.of("foo-<wildcard>-baz");
    final SimpleMatcher matcher = pattern.matcher("foo-bar-with-baz-and-dash-baz");
    assertThat(matcher.matches(), is(true)) ;
    assertThat(matcher.value(0), is("bar-with-baz-and-dash"));
  }

  @Test
  public void testMatcherMatchesMultipleValue() throws Exception {
    final SimplePattern pattern = SimplePattern.of("foo-<w1>-bar-<w2>-baz");
    final SimpleMatcher matcher = pattern.matcher("foo-data-1-bar-data-2-baz");
    assertThat(matcher.matches(), is(true)) ;
    assertThat(matcher.value(0), is("data-1"));
    assertThat(matcher.value(1), is("data-2"));
  }

  @Test
  public void testMatcherFailsSimilarSingleValue() throws Exception {
    final SimplePattern pattern = SimplePattern.of("foo-<wildcard>-baz");
    final SimpleMatcher matcher = pattern.matcher("foo-bar-baz-nopes");
    assertThat(matcher.matches(), is(false)) ;
  }

  @Test
  public void testMatcherFailsSingleValue() throws Exception {
    final SimplePattern pattern = SimplePattern.of("foo-<wildcard>-baz");
    final SimpleMatcher matcher = pattern.matcher("no-match-here");
    assertThat(matcher.matches(), is(false)) ;
  }
}