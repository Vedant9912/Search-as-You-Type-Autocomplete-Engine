package com.autocomplete.autocompleteengine.controller;

import com.autocomplete.autocompleteengine.config.TrieHolder;
import com.autocomplete.autocompleteengine.model.Product;
import com.autocomplete.autocompleteengine.repository.ProductRepository;
import com.autocomplete.autocompleteengine.trie.Trie;
import com.autocomplete.autocompleteengine.util.LatencyStats;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class SearchController {

    private final ProductRepository productRepository;
    private final TrieHolder trieHolder;

    private static final String[] BENCHMARK_PREFIXES = {
        "net", "web", "data", "cloud", "smart", "quick", "prime", "mega", "ultra", "micro"
    };

    public SearchController(ProductRepository productRepository, TrieHolder trieHolder) {
        this.productRepository = productRepository;
        this.trieHolder = trieHolder;
    }

    /** Phase 1 baseline: naive MySQL LIKE 'prefix%' search, top 10 by popularity. */
    @GetMapping("/search")
    public List<Product> search(@RequestParam("q") String q) {
        if (isBlank(q)) {
            return List.of();
        }
        Pageable top10 = PageRequest.of(0, 10);
        return productRepository.findByNaivePrefixSearch(q, top10);
    }

    /** Phase 2: Trie-based prefix search, returns ALL matches, unranked. */
    @GetMapping("/search-trie")
    public List<String> searchTrie(@RequestParam("q") String q) {
        if (isBlank(q)) {
            return List.of();
        }
        return trieHolder.getTrie().searchByPrefix(q.toLowerCase());
    }

    /** Phase 3: Trie + Min-Heap, returns top 10 matches ranked by popularity. */
    @GetMapping("/search-topk")
    public List<String> searchTopK(@RequestParam("q") String q) {
        if (isBlank(q)) {
            return List.of();
        }
        return trieHolder.getTrie().searchTopK(q.toLowerCase(), 10, trieHolder.getPopularityMap());
    }

    @GetMapping("/benchmark")
    public String benchmark() {
        List<Long> latencies = new ArrayList<>();
        Pageable top10 = PageRequest.of(0, 10);

        for (int i = 0; i < 100; i++) {
            String prefix = BENCHMARK_PREFIXES[i % BENCHMARK_PREFIXES.length];

            long start = System.nanoTime();
            productRepository.findByNaivePrefixSearch(prefix, top10);
            long end = System.nanoTime();

            latencies.add((end - start) / 1_000_000);
        }

        return formatLatency("MySQL", latencies);
    }

    @GetMapping("/benchmark-trie")
    public String benchmarkTrie() {
        Trie trie = trieHolder.getTrie();
        List<Long> latencies = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            String prefix = BENCHMARK_PREFIXES[i % BENCHMARK_PREFIXES.length];

            long start = System.nanoTime();
            trie.searchByPrefix(prefix);
            long end = System.nanoTime();

            latencies.add((end - start) / 1_000_000);
        }

        return formatLatency("Trie", latencies);
    }

    @GetMapping("/benchmark-topk")
    public String benchmarkTopK() {
        Trie trie = trieHolder.getTrie();
        Map<String, Integer> popularityMap = trieHolder.getPopularityMap();
        List<Long> latencies = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            String prefix = BENCHMARK_PREFIXES[i % BENCHMARK_PREFIXES.length];

            long start = System.nanoTime();
            trie.searchTopK(prefix, 10, popularityMap);
            long end = System.nanoTime();

            latencies.add((end - start) / 1_000_000);
        }

        return formatLatency("TopK", latencies);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String formatLatency(String label, List<Long> latencies) {
        long p50 = LatencyStats.getPercentile(latencies, 50);
        long p95 = LatencyStats.getPercentile(latencies, 95);
        long p99 = LatencyStats.getPercentile(latencies, 99);
        return label + " p50: " + p50 + "ms, p95: " + p95 + "ms, p99: " + p99 + "ms";
    }
}
