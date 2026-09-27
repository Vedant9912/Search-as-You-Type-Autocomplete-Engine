package com.autocomplete.autocompleteengine.trie;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class TrieTest {

    @Test
    public void testInsertAndSearch() {
        Trie trie = new Trie();
        trie.insert("cat");
        trie.insert("car");
        trie.insert("cart");
        trie.insert("dog");

        List<String> results = trie.searchByPrefix("ca");

        assertEquals(3, results.size());
        assertTrue(results.contains("cat"));
        assertTrue(results.contains("car"));
        assertTrue(results.contains("cart"));
    }

    @Test
    public void testPrefixNotFound() {
        Trie trie = new Trie();
        trie.insert("cat");

        List<String> results = trie.searchByPrefix("xyz");

        assertEquals(0, results.size());
    }

    @Test
    public void testEmptyPrefixTrie() {
        Trie trie = new Trie();
        List<String> results = trie.searchByPrefix("anything");
        assertEquals(0, results.size());
    }

    @Test
    public void testTopKReturnsCorrectRankingOrder() {
        Trie trie = new Trie();
        trie.insert("cat");
        trie.insert("car");
        trie.insert("cart");

        java.util.Map<String, Integer> popularity = new java.util.HashMap<>();
        popularity.put("cat", 50);
        popularity.put("car", 100);
        popularity.put("cart", 10);

        List<String> results = trie.searchTopK("ca", 2, popularity);

        assertEquals(2, results.size());
        assertEquals("car", results.get(0));  // most popular first
        assertEquals("cat", results.get(1));
    }
}
