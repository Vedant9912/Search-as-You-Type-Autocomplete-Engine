package com.autocomplete.autocompleteengine.config;

import com.autocomplete.autocompleteengine.trie.Trie;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds the shared Trie and popularity map as a single Spring-managed bean.
 *
 * This exists instead of building the Trie/map via @Bean factory methods because
 * @Bean methods run during context initialization, which happens BEFORE any
 * CommandLineRunner (like our data seeder). On a fresh/empty database, that
 * ordering meant the Trie was built from zero rows and never refreshed.
 *
 * This holder starts empty and is populated by DataInitializer, which is a
 * CommandLineRunner and therefore guaranteed to run after seeding completes.
 */
@Component
public class TrieHolder {

    private volatile Trie trie = new Trie();
    private volatile Map<String, Integer> popularityMap = new HashMap<>();

    public Trie getTrie() {
        return trie;
    }

    public Map<String, Integer> getPopularityMap() {
        return popularityMap;
    }

    public void setTrie(Trie trie) {
        this.trie = trie;
    }

    public void setPopularityMap(Map<String, Integer> popularityMap) {
        this.popularityMap = popularityMap;
    }
}
