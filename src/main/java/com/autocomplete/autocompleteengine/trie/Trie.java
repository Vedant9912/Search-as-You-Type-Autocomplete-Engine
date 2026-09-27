package com.autocomplete.autocompleteengine.trie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * In-memory Trie for fast prefix search.
 *
 * insert(word)          -> O(L), L = word length
 * searchByPrefix(prefix) -> O(L + K) where K = total characters across all matches (returns ALL matches, unranked)
 * searchTopK(prefix, k)  -> O(L + n log k), n = number of matches, k = fixed heap size (returns top-k by popularity)
 */
public class Trie {

    private TrieNode root;

    public Trie() {
        root = new TrieNode();
    }

    public void insert(String word) {
        TrieNode current = root;

        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);

            if (current.children.containsKey(ch)) {
                current = current.children.get(ch);
            } else {
                TrieNode newNode = new TrieNode();
                current.children.put(ch, newNode);
                current = newNode;
            }
        }

        current.isEndOfWord = true;
    }

    /** Returns every word matching the prefix, unranked. Useful for correctness checks / small datasets. */
    public List<String> searchByPrefix(String prefix) {
        TrieNode current = root;

        for (int i = 0; i < prefix.length(); i++) {
            char ch = prefix.charAt(i);

            if (current.children.containsKey(ch)) {
                current = current.children.get(ch);
            } else {
                return new ArrayList<>();
            }
        }

        List<String> results = new ArrayList<>();
        collectAllWords(current, prefix, results);
        return results;
    }

    private void collectAllWords(TrieNode node, String currentPrefix, List<String> results) {
        if (node.isEndOfWord) {
            results.add(currentPrefix);
        }

        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            char childChar = entry.getKey();
            TrieNode childNode = entry.getValue();
            collectAllWords(childNode, currentPrefix + childChar, results);
        }
    }

    /**
     * Returns the top-k matches for a prefix, ranked by popularity (descending),
     * using a min-heap so we never collect/sort the full result set.
     */
    public List<String> searchTopK(String prefix, int k, Map<String, Integer> popularityMap) {
        TrieNode current = root;

        for (int i = 0; i < prefix.length(); i++) {
            char ch = prefix.charAt(i);

            if (current.children.containsKey(ch)) {
                current = current.children.get(ch);
            } else {
                return new ArrayList<>();
            }
        }

        // Integer.compare + getOrDefault avoids two issues with a plain subtraction:
        // (1) NPE if a word somehow isn't in the popularity map (get() would return null,
        //     and unboxing null to int throws NullPointerException).
        // (2) integer overflow if the two popularity values are far enough apart that
        //     their difference doesn't fit in an int.
        PriorityQueue<String> minHeap = new PriorityQueue<>(
            (word1, word2) -> Integer.compare(
                popularityMap.getOrDefault(word1, 0),
                popularityMap.getOrDefault(word2, 0)
            )
        );

        collectTopK(current, prefix, minHeap, k, popularityMap);

        List<String> results = new ArrayList<>();
        while (!minHeap.isEmpty()) {
            results.add(minHeap.poll());
        }

        Collections.reverse(results);

        return results;
    }

    private void collectTopK(TrieNode node, String currentPrefix, PriorityQueue<String> heap, int k, Map<String, Integer> popularityMap) {

        if (node.isEndOfWord) {
            if (heap.size() < k) {
                heap.add(currentPrefix);
            } else {
                int currentWordPopularity = popularityMap.getOrDefault(currentPrefix, 0);
                int weakestInHeap = popularityMap.getOrDefault(heap.peek(), 0);

                if (currentWordPopularity > weakestInHeap) {
                    heap.poll();
                    heap.add(currentPrefix);
                }
            }
        }

        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            char childChar = entry.getKey();
            TrieNode childNode = entry.getValue();
            collectTopK(childNode, currentPrefix + childChar, heap, k, popularityMap);
        }
    }
}
