package dano;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TrieTest {

  @Test
  public void testRealURIMap() {
    final Pattern capture = Pattern.compile("<[^>]+>");
    final Map<String, String> routes = new LinkedHashMap<String, String>();
    routes.put("/usercount", "usercount");
    routes.put("/users", "users");
    routes.put("/users/<user>", "user");
    routes.put("/users/<user>/playlistcount", "user-playlistcount");
    routes.put("/users/<user>/playlists", "user-playlists");
    routes.put("/users/<user>/playlists/<playlist>/itemcount", "user-playlist-itemcount");
    routes.put("/users/<user>/playlists/<playlist>/items", "user-playlist-items");
    routes.put("/users/<user>/playlists/<playlist>/name", "user-playlist-name");
    routes.put("/users/<user>/playlists/<playlist>/items/<item>", "user-playlist-item");
    routes.put("/users/<user>/playlists/<playlist>", "user-playlist");
    final RadixTrie.Builder<String> builder = RadixTrie.builder(String.class);
    for (final Map.Entry<String, String> entry : routes.entrySet()) {
      builder.insert(entry.getKey(), entry.getValue());
    }
    final RadixTrie<String> rdx = builder.build();
    final RadixTrie.Captor captor = rdx.lookup();
    for (final Map.Entry<String, String> entry : routes.entrySet()) {
      if (!Objects.equals(rdx.lookup(entry.getKey(), captor), entry.getValue())) {
        final String value = rdx.lookup(entry.getKey(), captor);
        System.out.println("mismatch: " + entry.getKey() + " -> " + value);
      }
      assertThat(rdx.lookup(entry.getKey(), captor), is(entry.getValue()));
      final Matcher matcher = capture.matcher(entry.getKey());
      int i = 0;
      while (matcher.find()) {
        final String expectedValue = matcher.group();
        final String value = captor.value(entry.getKey(), i).toString();
        assertThat(value, is(expectedValue));
        i++;
      }
    }
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