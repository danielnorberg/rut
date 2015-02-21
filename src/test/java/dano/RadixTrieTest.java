package dano;

import org.junit.Test;

public class RadixTrieTest {

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
