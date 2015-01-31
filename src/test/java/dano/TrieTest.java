package dano;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TrieTest {

  @Test
  public void testRealURIMap() {
    final RadixTrie<String> radixTrie = RadixTrie.builder(String.class)
        .insert("/usercount", "usercount")
        .insert("/users", "users")
        .insert("/users/<user>", "user")
        .insert("/users/<user>/playlistcount", "user-playlistcount")
        .insert("/users/<user>/playlists", "user-playlists")
        .insert("/users/<user>/playlists/<playlist>/itemcount", "user-playlist-itemcount")
        .insert("/users/<user>/playlists/<playlist>/items", "user-playlist-items")
        .insert("/users/<user>/playlists/<playlist>/name", "user-playlist-name")
        .insert("/users/<user>/playlists/<playlist>/items/<item>", "user-playlist-item")
        .insert("/users/<user>/playlists/<playlist>", "user-playlist")
        .build();
  }

  @Test
  public void testInsert() {
    final Trie<String> trie = new Trie<String>();
    trie.insert("/foo/bar-<data>-baz/quux", "quux");
    trie.insert("/foo/bar-<data>-baz/wuux", "wuux");
  }

  @Test
  public void testCompress() {
    final Trie<String> trie = new Trie<String>();
    trie.insert("/foo/bar-<data>-baz/quux", "quux");
    trie.insert("/foo/bar-<data>-baz/wuux", "wuux");
    final RadixTrie<String> rdx = trie.compress();
    assertThat(rdx.lookup("/foo/bar-data-baz/quux"), is("quux"));
    assertThat(rdx.lookup("/foo/bar-data-with-dash-baz/wuux"), is("wuux"));
  }


  @Test
  public void testRadixMatch() {
    final Trie<String> trie = new Trie<String>();
    trie.insert("/foo/1", "1");
    trie.insert("/foo/2", "2");
    final RadixTrie<String> rdx = trie.compress();
    assertThat(rdx.lookup("/foo/1"), is("1"));
    assertThat(rdx.lookup("/foo/2"), is("2"));
  }

}