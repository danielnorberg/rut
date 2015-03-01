package dano;

import org.junit.Test;

import java.util.List;

import static java.lang.Math.max;
import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class RadixTrieTest {

  @Test
  public void testSingleRoot() {
    RadixTrie<String> rdx = RadixTrie.create(String.class)
        .insert("a", "a");
    assertThat(rdx.lookup("a"), is("a"));
  }

  @Test
  public void testSingleRootCapture() {
    RadixTrie<String> rdx = RadixTrie.create(String.class)
        .insert("<a>", "<a>");
    assertThat(rdx.lookup("foobar"), is("<a>"));
  }

  @Test
  public void testTwoRoots() {
    RadixTrie<String> rdx = RadixTrie.create(String.class)
        .insert("a", "a")
        .insert("b", "b");
    assertThat(rdx.lookup("a"), is("a"));
    assertThat(rdx.lookup("b"), is("b"));
  }

  @Test
  public void testOneRootOneEdge() {
    RadixTrie<String> rdx = RadixTrie.create(String.class)
        .insert("a", "a")
        .insert("ab", "ab");
    assertThat(rdx.lookup("a"), is("a"));
    assertThat(rdx.lookup("ab"), is("ab"));
  }

  @Test
  public void testOneRootTwoEdges() {
    RadixTrie<String> rdx = RadixTrie.create(String.class)
        .insert("a", "a")
        .insert("ab", "ab")
        .insert("ac", "ac");
    assertThat(rdx.lookup("a"), is("a"));
    assertThat(rdx.lookup("ab"), is("ab"));
    assertThat(rdx.lookup("ac"), is("ac"));
  }

  @Test
  public void testOneRootOneEdgeSplit() {
    RadixTrie<String> rdx = RadixTrie.create(String.class)
        .insert("a", "a")
        .insert("abbb", "abbb")
        .insert("abcc", "abcc");
    assertThat(rdx.lookup("a"), is("a"));
    assertThat(rdx.lookup("abbb"), is("abbb"));
    assertThat(rdx.lookup("abcc"), is("abcc"));
  }

  @Test
  public void testOneRootOneEdgeReverse() {
    RadixTrie<String> rdx = RadixTrie.create(String.class)
        .insert("ab", "ab")
        .insert("a", "a");
    assertThat(rdx.lookup("a"), is("a"));
    assertThat(rdx.lookup("ab"), is("ab"));
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
        "/<a>/b/<c>/<d>/e"
    );
    RadixTrie<String> rdx = RadixTrie.create();
    for (int i = 0; i < paths.size(); i++) {
      final String path = paths.get(i);
      rdx = rdx.insert(path, path);
      verifyPaths(rdx, paths.subList(0, i + 1));
    }
  }

  private void verifyPaths(final RadixTrie<String> rdx, final List<String> paths) {
    for (final String path : paths) {
      assertThat(rdx.lookup(path), is(path));
      assertThat(rdx.captures(), is(captures(paths)));
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
