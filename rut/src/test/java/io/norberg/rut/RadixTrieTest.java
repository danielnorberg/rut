package io.norberg.rut;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import io.norberg.rut.RadixTrie.Node;

import static java.lang.Math.max;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class RadixTrieTest {

  @Rule public ExpectedException exception = ExpectedException.none();

  @Test
  public void testSingleRoot() {
    final RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("a", "a")
        .build();
    assertThat(rdx.toString(), not(Matchers.isEmptyOrNullString()));
    assertThat(rdx.lookup("a"), is("a"));
  }

  @Test
  public void testSingleRootNoMatch() {
    final RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("aaa", "aaa")
        .build();
    assertThat(rdx.toString(), not(Matchers.isEmptyOrNullString()));
    assertThat(rdx.lookup("aa"), is(nullValue()));
  }

  @Test
  public void testCaptureSuffixMismatch() {
    final RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("aaa<value>bbb", "foobar")
        .build();
    assertThat(rdx.toString(), not(Matchers.isEmptyOrNullString()));
    assertThat(rdx.lookup("aaabb"), is(nullValue()));
  }

  @Test
  public void testCaptureNoMatchCaptorValueThrows() {
    final RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("aaa<value>bbb", "foobar")
        .build();
    assertThat(rdx.toString(), not(Matchers.isEmptyOrNullString()));
    final RadixTrie.Captor captor = rdx.captor();
    rdx.lookup("aaabb", captor);
    exception.expect(IllegalStateException.class);
    captor.value("aaabb", 0);
  }

  @Test
  public void testCaptureNoMatchCaptorValueStartThrows() {
    final RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("aaa<value>bbb", "foobar")
        .build();
    assertThat(rdx.toString(), not(Matchers.isEmptyOrNullString()));
    final RadixTrie.Captor captor = rdx.captor();
    rdx.lookup("aaabb", captor);
    exception.expect(IllegalStateException.class);
    captor.valueStart(0);
  }

  @Test
  public void testCaptureNoMatchCaptorValueEndThrows() {
    final RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("aaa<value>bbb", "foobar")
        .build();
    assertThat(rdx.toString(), not(Matchers.isEmptyOrNullString()));
    final RadixTrie.Captor captor = rdx.captor();
    rdx.lookup("aaabb", captor);
    exception.expect(IllegalStateException.class);
    captor.valueEnd(0);
  }

  @Test
  public void testCaptureMatchCaptorValueIndexOutOfBoundsThrows() {
    final RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("a<value>b", "foobar")
        .build();
    assertThat(rdx.toString(), not(Matchers.isEmptyOrNullString()));
    final RadixTrie.Captor captor = rdx.captor();
    rdx.lookup("afoob", captor);
    exception.expect(IndexOutOfBoundsException.class);
    captor.value("afoob", 1);
  }


  @Test
  public void testCaptureMatchCaptorValueStartIndexOutOfBoundsThrows() {
    final RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("a<value>b", "foobar")
        .build();
    assertThat(rdx.toString(), not(Matchers.isEmptyOrNullString()));
    final RadixTrie.Captor captor = rdx.captor();
    rdx.lookup("afoob", captor);
    exception.expect(IndexOutOfBoundsException.class);
    captor.valueStart(1);
  }


  @Test
  public void testCaptureMatchCaptorValueEndIndexOutOfBoundsThrows() {
    final RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("a<value>b", "foobar")
        .build();
    assertThat(rdx.toString(), not(Matchers.isEmptyOrNullString()));
    final RadixTrie.Captor captor = rdx.captor();
    rdx.lookup("afoob", captor);
    exception.expect(IndexOutOfBoundsException.class);
    captor.valueEnd(1);
  }

  @Test
  public void testSingleRootCapture() {
    final RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("<a>", "<a>")
        .build();
    assertThat(rdx.toString(), not(Matchers.isEmptyOrNullString()));
    assertThat(rdx.lookup("foobar"), is("<a>"));
  }

  @Test
  public void testTwoRoots() {
    final RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("a", "a")
        .insert("b", "b")
        .build();
    assertThat(rdx.toString(), not(Matchers.isEmptyOrNullString()));
    assertThat(rdx.lookup("a"), is("a"));
    assertThat(rdx.lookup("b"), is("b"));
  }

  @Test
  public void testOneRootOneEdge() {
    final RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("a", "a")
        .insert("ab", "ab")
        .build();
    assertThat(rdx.toString(), not(Matchers.isEmptyOrNullString()));
    assertThat(rdx.lookup("a"), is("a"));
    assertThat(rdx.lookup("ab"), is("ab"));
  }

  @Test
  public void testOneRootTwoEdges() {
    final RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("a", "a")
        .insert("ab", "ab")
        .insert("ac", "ac")
        .build();
    assertThat(rdx.toString(), not(Matchers.isEmptyOrNullString()));
    assertThat(rdx.lookup("a"), is("a"));
    assertThat(rdx.lookup("ab"), is("ab"));
    assertThat(rdx.lookup("ac"), is("ac"));
  }

  @Test
  public void testOneRootOneEdgeSplit() {
    final RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("a", "a")
        .insert("abbb", "abbb")
        .insert("abcc", "abcc")
        .build();
    assertThat(rdx.toString(), not(Matchers.isEmptyOrNullString()));
    assertThat(rdx.lookup("a"), is("a"));
    assertThat(rdx.lookup("abbb"), is("abbb"));
    assertThat(rdx.lookup("abcc"), is("abcc"));
  }

  @Test
  public void testOneRootOneEdgeReverse() {
    final RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("ab", "ab")
        .insert("a", "a")
        .build();
    assertThat(rdx.toString(), not(Matchers.isEmptyOrNullString()));
    assertThat(rdx.lookup("a"), is("a"));
    assertThat(rdx.lookup("ab"), is("ab"));
  }

  @Test
  public void testQueryAtSplitDoesNotMatch() {
    final RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("a1", "a1")
        .insert("a2", "a2")
        .build();
    assertThat(rdx.toString(), not(Matchers.isEmptyOrNullString()));
    final RadixTrie.Captor captor = rdx.captor();
    assertThat(rdx.lookup("a?q", captor), is(nullValue()));
    assertThat(captor.isMatch(), is(false));
    assertThat(captor.queryStart(), is(-1));
    assertThat(captor.queryEnd(), is(-1));
  }

  @Test
  public void testEndAtSplitDoesNotMatch() {
    final RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("a1", "a1")
        .insert("a2", "a2")
        .build();
    assertThat(rdx.toString(), not(Matchers.isEmptyOrNullString()));
    final RadixTrie.Captor captor = rdx.captor();
    assertThat(rdx.lookup("a", captor), is(nullValue()));
    assertThat(captor.isMatch(), is(false));
  }

  @Test(expected = IllegalArgumentException.class)
  public void verifyUnclosedCaptureFails() {
    RadixTrie.builder(String.class)
        .insert("a<", "a<");
  }

  @Test(expected = IllegalArgumentException.class)
  public void verifyEmptyPathFails() {
    RadixTrie.builder(String.class)
        .insert("", "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void verifyUnorderedSiblingsThrow() {
    final Node<String> sibling = Node.match("a", null, null, "foo");
    Node.match("b", sibling, null, "bar");
  }

  @Test(expected = IllegalArgumentException.class)
  public void verifyPathBeforeSegCaptureSiblingsThrow() {
    final Node<String> sibling = Node.captureSeg(null, null, "foo");
    Node.capturePath(sibling, "bar");
  }

  @Test(expected = IllegalArgumentException.class)
  public void verifySegCaptureBeforeMatchSiblingThrows() {
    final Node<String> sibling = Node.match("foo", null, null, "foo");
    Node.captureSeg(sibling, null, "bar");
  }

  @Test
  public void testSegCaptureBeforePathCapture() {
    final Node<String> sibling = Node.capturePath(null, "foo");
    Node.captureSeg(sibling, null, "bar");
  }

  @Test(expected = IllegalArgumentException.class)
  public void verifyPathCaptureSiblingNotLastThrows() {
    final Node<String> sibling = Node.match("foo", null, null, "foo");
    Node.capturePath(sibling, "bar");
  }

  @Test(expected = IllegalArgumentException.class)
  public void verifyDuplicateSiblingHeadsThrow() {
    final Node<String> sibling = Node.match("aa", null, null, "foo");
    Node.match("ab", sibling, null, "bar");
  }

  @Test(expected = IllegalArgumentException.class)
  public void verifyTerminalNodeWithoutValueThrows() {
    Node.match("a", null, null, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void verifyNonAsciiInsertThrows() {
    RadixTrie.builder(String.class).insert("" + (char) 128, "foo");
  }

  @Test(expected = IllegalArgumentException.class)
  public void verifyNonTerminalPathCaptureThrows() {
    RadixTrie.builder(String.class).insert("<foo:path>bar", "foo");
  }

  @Test(expected = IllegalArgumentException.class)
  public void verifyUnknownCaptureTypeThrows() {
    RadixTrie.builder(String.class).insert("<foo:bar>", "foo");
  }

  @Test
  public void testPathCaptureReplace() {
    final RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("<foo:path>", "foo")
        .insert("<bar:path>", "bar")
        .build();
    final RadixTrie.Captor captor = rdx.captor();

    {
      final String path = "/some/path/with/slashes";
      final String value = rdx.lookup(path, captor);
      assertThat(value, is("bar"));
      assertThat(captor.isMatch(), is(true));
      assertThat(captor.values(), is(1));
      assertThat(captor.value(path, 0).toString(), is(path));
    }
  }

  @Test
  public void testPathCaptureSingleTrivialRoute() {
    final RadixTrie.Builder<String> builder = RadixTrie.builder(String.class)
        .insert("<foo:path>", "foo");
    builder.toString();
    final RadixTrie<String> rdx = builder.build();
    final RadixTrie.Captor captor = rdx.captor();

    {
      final String path = "/some/path/with/slashes";
      final String value = rdx.lookup(path, captor);
      assertThat(value, is("foo"));
      assertThat(captor.isMatch(), is(true));
      assertThat(captor.values(), is(1));
      assertThat(captor.value(path, 0).toString(), is(path));
    }

    {
      final String path = "string-without-slash";
      final String value = rdx.lookup(path, captor);
      assertThat(value, is("foo"));
      assertThat(captor.isMatch(), is(true));
      assertThat(captor.values(), is(1));
      assertThat(captor.value(path, 0).toString(), is(path));
    }

    {
      final String path = "/some/path/with/slash";
      final String uri = path + "?and=query";
      final String value = rdx.lookup(uri, captor);
      assertThat(value, is("foo"));
      assertThat(captor.isMatch(), is(true));
      assertThat(captor.values(), is(1));
      assertThat(captor.value(uri, 0).toString(), is(path));
    }

    {
      final String path = "string-without-slash";
      final String uri = path + "?and=query";
      final String value = rdx.lookup(uri, captor);
      assertThat(value, is("foo"));
      assertThat(captor.isMatch(), is(true));
      assertThat(captor.values(), is(1));
      assertThat(captor.value(path, 0).toString(), is(path));
    }
  }

  @Test
  public void testPathCaptureTwoRoutes() {
    final RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("<foo:path>", "foo")
        .insert("bar<bar:path>", "bar")
        .build();
    final RadixTrie.Captor captor = rdx.captor();

    {
      final String uri = "/foo/path/with/slashes";
      final String value = rdx.lookup(uri, captor);
      assertThat(value, is("foo"));
      assertThat(captor.isMatch(), is(true));
      assertThat(captor.values(), is(1));
      assertThat(captor.value(uri, 0).toString(), is(uri));
    }

    {
      final String uri = "foo-string-without-slash";
      final String value = rdx.lookup(uri, captor);
      assertThat(value, is("foo"));
      assertThat(captor.isMatch(), is(true));
      assertThat(captor.values(), is(1));
      assertThat(captor.value(uri, 0).toString(), is(uri));
    }

    {
      final String path = "/foo/path/with/slash";
      final String uri = path + "?and=query";
      final String value = rdx.lookup(uri, captor);
      assertThat(value, is("foo"));
      assertThat(captor.isMatch(), is(true));
      assertThat(captor.values(), is(1));
      assertThat(captor.value(uri, 0).toString(), is(path));
    }

    {
      final String path = "foo-string-without-slash";
      final String uri = path + "?and=query";
      final String value = rdx.lookup(uri, captor);
      assertThat(value, is("foo"));
      assertThat(captor.isMatch(), is(true));
      assertThat(captor.values(), is(1));
      assertThat(captor.value(uri, 0).toString(), is(path));
    }

    {
      final String path = "/path/with/slashes";
      final String uri = "bar" + path;
      final String value = rdx.lookup(uri, captor);
      assertThat(value, is("bar"));
      assertThat(captor.isMatch(), is(true));
      assertThat(captor.values(), is(1));
      assertThat(captor.value(uri, 0).toString(), is(path));
    }

    {
      final String path = "string-without-slash";
      final String uri = "bar" + path;
      final String value = rdx.lookup(uri, captor);
      assertThat(value, is("bar"));
      assertThat(captor.isMatch(), is(true));
      assertThat(captor.values(), is(1));
      assertThat(captor.value(uri, 0).toString(), is(path));
    }

    {
      final String path = "/path/with/slash";
      final String uri = "bar" + path + "?and=query";
      final String value = rdx.lookup(uri, captor);
      assertThat(value, is("bar"));
      assertThat(captor.isMatch(), is(true));
      assertThat(captor.values(), is(1));
      assertThat(captor.value(uri, 0).toString(), is(path));
    }

    {
      final String path = "string-without-slash";
      final String uri = "bar" + path + "?and=query";
      final String value = rdx.lookup(uri, captor);
      assertThat(value, is("bar"));
      assertThat(captor.isMatch(), is(true));
      assertThat(captor.values(), is(1));
      assertThat(captor.value(uri, 0).toString(), is(path));
    }
  }

  @Test
  public void testPaths() {
    final List<String> paths = asList(
        "/a",
        "/aa",
        "/ab",
        "/a/b",
        "/a/<b>",
        "/a/<b>/c",
        "/bb/c",
        "/b/c",
        "/c/<d>",
        "/c/d/e",
        "/<a>/b/<c>/<d>/e",
        "<foo>/bar"
    );
    RadixTrie.Builder<String> rdx = RadixTrie.builder();
    for (int i = 0; i < paths.size(); i++) {
      final String path = paths.get(i);
      rdx = rdx.insert(path, path);
      assertThat(rdx.toString(), not(isEmptyOrNullString()));
      verifyPaths(rdx.build(), paths.subList(0, i + 1));
    }
  }

  private void verifyPaths(final RadixTrie<String> rdx, final List<String> paths) {
    for (final String path : paths) {
      final RadixTrie.Captor captor = rdx.captor();

      assertThat(rdx.lookup(path), is(path));
      assertThat(rdx.lookup(path, captor), is(path));
      assertThat(captor.isMatch(), is(true));
      assertThat(captor.queryStart(), is(-1));
      assertThat(captor.queryEnd(), is(-1));
      assertThat(captor.query(path), is(nullValue()));

      final String pathWithSlash = path + '/';
      assertThat(rdx.lookup(pathWithSlash), is(nullValue()));

      final String query = "query";
      final String pathWithQuery = path + "?" + query;
      assertThat(rdx.lookup(pathWithQuery), is(path));
      assertThat(rdx.lookup(pathWithQuery, captor), is(path));
      assertThat(captor.isMatch(), is(true));
      assertThat(captor.queryStart(), is(path.length() + 1));
      assertThat(captor.queryEnd(), is(pathWithQuery.length()));
      final CharSequence queryParams = captor.query(pathWithQuery);
      assertNotNull(queryParams);
      assertThat(queryParams.toString(), is(query));

      assertThat(rdx.captures(), is(captures(paths)));

      assertThat(rdx.toString(), not(isEmptyOrNullString()));
    }
  }

  private static int captures(final List<String> paths) {
    int maxCaptures = 0;
    for (final String path : paths) {
      maxCaptures = max(maxCaptures, captures(path));
    }
    return maxCaptures;
  }

  private static int captures(final String path) {
    int count = 0;
    for (int i = 0; i < path.length(); i++) {
      if (path.charAt(i) == '<') {
        count++;
      }
    }
    return count;
  }
}
