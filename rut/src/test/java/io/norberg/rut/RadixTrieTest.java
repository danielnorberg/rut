package io.norberg.rut;

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
    RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("a", "a")
        .build();
    assertThat(rdx.lookup("a"), is("a"));
  }

  @Test
  public void testSingleRootNoMatch() {
    RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("aaa", "aaa")
        .build();
    assertThat(rdx.lookup("aa"), is(nullValue()));
  }

  @Test
  public void testCaptureSuffixMismatch() {
    RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("aaa<value>bbb", "foobar")
        .build();
    assertThat(rdx.lookup("aaabb"), is(nullValue()));
  }

  @Test
  public void testCaptureNoMatchCaptorValueThrows() {
    RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("aaa<value>bbb", "foobar")
        .build();
    final RadixTrie.Captor captor = rdx.captor();
    rdx.lookup("aaabb", captor);
    exception.expect(IllegalStateException.class);
    captor.value("aaabb", 0);
  }

  @Test
  public void testCaptureNoMatchCaptorValueStartThrows() {
    RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("aaa<value>bbb", "foobar")
        .build();
    final RadixTrie.Captor captor = rdx.captor();
    rdx.lookup("aaabb", captor);
    exception.expect(IllegalStateException.class);
    captor.valueStart(0);
  }

  @Test
  public void testCaptureNoMatchCaptorValueEndThrows() {
    RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("aaa<value>bbb", "foobar")
        .build();
    final RadixTrie.Captor captor = rdx.captor();
    rdx.lookup("aaabb", captor);
    exception.expect(IllegalStateException.class);
    captor.valueEnd(0);
  }

  @Test
  public void testCaptureMatchCaptorValueIndexOutOfBoundsThrows() {
    RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("a<value>b", "foobar")
        .build();
    final RadixTrie.Captor captor = rdx.captor();
    rdx.lookup("afoob", captor);
    exception.expect(IndexOutOfBoundsException.class);
    captor.value("afoob", 1);
  }


  @Test
  public void testCaptureMatchCaptorValueStartIndexOutOfBoundsThrows() {
    RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("a<value>b", "foobar")
        .build();
    final RadixTrie.Captor captor = rdx.captor();
    rdx.lookup("afoob", captor);
    exception.expect(IndexOutOfBoundsException.class);
    captor.valueStart(1);
  }


  @Test
  public void testCaptureMatchCaptorValueEndIndexOutOfBoundsThrows() {
    RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("a<value>b", "foobar")
        .build();
    final RadixTrie.Captor captor = rdx.captor();
    rdx.lookup("afoob", captor);
    exception.expect(IndexOutOfBoundsException.class);
    captor.valueEnd(1);
  }

  @Test
  public void testSingleRootCapture() {
    RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("<a>", "<a>")
        .build();
    assertThat(rdx.lookup("foobar"), is("<a>"));
  }

  @Test
  public void testTwoRoots() {
    RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("a", "a")
        .insert("b", "b")
        .build();
    assertThat(rdx.lookup("a"), is("a"));
    assertThat(rdx.lookup("b"), is("b"));
  }

  @Test
  public void testOneRootOneEdge() {
    RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("a", "a")
        .insert("ab", "ab")
        .build();
    assertThat(rdx.lookup("a"), is("a"));
    assertThat(rdx.lookup("ab"), is("ab"));
  }

  @Test
  public void testOneRootTwoEdges() {
    RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("a", "a")
        .insert("ab", "ab")
        .insert("ac", "ac")
        .build();
    assertThat(rdx.lookup("a"), is("a"));
    assertThat(rdx.lookup("ab"), is("ab"));
    assertThat(rdx.lookup("ac"), is("ac"));
  }

  @Test
  public void testOneRootOneEdgeSplit() {
    RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("a", "a")
        .insert("abbb", "abbb")
        .insert("abcc", "abcc")
        .build();
    assertThat(rdx.lookup("a"), is("a"));
    assertThat(rdx.lookup("abbb"), is("abbb"));
    assertThat(rdx.lookup("abcc"), is("abcc"));
  }

  @Test
  public void testOneRootOneEdgeReverse() {
    RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("ab", "ab")
        .insert("a", "a")
        .build();
    assertThat(rdx.lookup("a"), is("a"));
    assertThat(rdx.lookup("ab"), is("ab"));
  }

  @Test
  public void testQueryAtSplitDoesNotMatch() {
    RadixTrie<String> rdx = RadixTrie.builder(String.class)
        .insert("a1", "a1")
        .insert("a2", "a2")
        .build();
    final RadixTrie.Captor captor = rdx.captor();
    assertThat(rdx.lookup("a?q", captor), is(nullValue()));
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
    final Node<String> sibling = new Node<String>((byte) 1, null, null, null, "foo");
    new Node<String>((byte) 2, null, sibling, null, "bar");
  }

  @Test(expected = IllegalArgumentException.class)
  public void verifyTerminalNodeWithoutValueThrows() {
    new Node<String>((byte) 1, null, null, null, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void verifyNonAsciiInsertThrows() {
    RadixTrie.builder(String.class).insert("" + (char) 128, "foo");
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
