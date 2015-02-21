package dano;

import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class RadixTrieTest {

  private static final String TARGET = "dummy";

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

  @Test
  public void testPaths() {
    RadixTrie<String> rdx = RadixTrie.create();
    for (int i = 0; i < paths.size(); i++) {
      final String path = paths.get(i);
      rdx = rdx.insert(path, path);
      verifyPaths(rdx, i);
    }
  }

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

  private void verifyPaths(final RadixTrie<String> rdx, final int n) {
    for (int i = 0; i < n; i++) {
      final String path = paths.get(i) ;
      final String result = rdx.lookup(path);
      if (!path.equals(result)) {
        System.out.println(rdx.lookup(path) + " != " + path);
        assertThat(rdx.lookup(path), is(path));
      }
    }
  }

  @Test
  public void testInsert() {
    final RadixTrie<String> rdx0 = RadixTrie.create();
    final RadixTrie<String> rdx1 = rdx0.insert("/aa", "aa");
    final RadixTrie<String> rdx2 = rdx1.insert("/ab", "ab");
    final RadixTrie<String> foo = rdx1.insert("/aa/foo", "foo");
    final RadixTrie<String> bar = foo.insert("/aa/fo", "foo");
    System.out.println(foo);
  }
}
