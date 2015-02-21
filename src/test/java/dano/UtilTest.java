package dano;

import org.junit.Test;

import static dano.Util.splitCaptures;
import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class UtilTest {

  @Test
  public void testSplitCaptures() throws Exception {
    assertThat(asList(splitCaptures("")).isEmpty(), is(true));

//    assertThat(asList(splitCaptures("foobar")),
//               is(asList("foobar")));
//
//    assertThat(asList(splitCaptures("foo/<bar>/baz")),
//               is(asList("foo/", "<bar>", "/baz")));
//
//    assertThat(asList(splitCaptures("foo/<bar>/baz/<quux>")),
//               is(asList("foo/", "<bar>", "/baz/", "<quux>")));
  }
}
