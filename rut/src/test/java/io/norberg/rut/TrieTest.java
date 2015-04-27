package io.norberg.rut;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class TrieTest {

  private final Trie<String> trie = new Trie<String>();

  @Test
  public void testToString() {
    assertThat(trie.toString(), not(Matchers.isEmptyOrNullString()));

    trie.insert(Path.of("/"), "/");
    assertThat(trie.toString(), not(Matchers.isEmptyOrNullString()));

    trie.insert(Path.of("/<foo>"), "/<foo>");
    assertThat(trie.toString(), not(Matchers.isEmptyOrNullString()));

    trie.insert(Path.of("/<foo>/<bar:path>"), "/<foo>/<bar:path>");
    assertThat(trie.toString(), not(Matchers.isEmptyOrNullString()));
  }
}