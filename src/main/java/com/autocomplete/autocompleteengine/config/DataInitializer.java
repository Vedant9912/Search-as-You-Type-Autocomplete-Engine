package com.autocomplete.autocompleteengine.config;

import com.autocomplete.autocompleteengine.model.Product;
import com.autocomplete.autocompleteengine.repository.ProductRepository;
import com.autocomplete.autocompleteengine.trie.Trie;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Single CommandLineRunner that (1) seeds 10,000 synthetic products if the
 * database is empty, then (2) builds the Trie and popularity map from ONE
 * findAll() call and pushes them into TrieHolder.
 *
 * Doing seeding and Trie-building in the same runner guarantees correct
 * ordering (fixes the empty-Trie-on-first-run bug) and avoids fetching all
 * products from MySQL twice (fixes the duplicate findAll() issue).
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final TrieHolder trieHolder;

    public DataInitializer(ProductRepository productRepository, TrieHolder trieHolder) {
        this.productRepository = productRepository;
        this.trieHolder = trieHolder;
    }

    @Override
    public void run(String... args) {
        seedIfEmpty();
        buildTrieAndPopularityMap();
    }

    private void seedIfEmpty() {
        if (productRepository.count() > 0) {
            System.out.println("Data already exists, skipping seeding.");
            return;
        }

        String[] prefixes = {"Net", "Web", "Data", "Cloud", "Smart", "Quick", "Prime", "Mega", "Ultra", "Micro"};
        String[] suffixes = {"flix", "gear", "base", "book", "phone", "tech", "hub", "pro", "max", "lite"};

        Random random = new Random();
        List<Product> products = new ArrayList<>();

        for (int i = 0; i < 10000; i++) {
            String name = prefixes[random.nextInt(prefixes.length)]
                        + suffixes[random.nextInt(suffixes.length)]
                        + random.nextInt(1000);
            int popularity = random.nextInt(100000);
            products.add(new Product(name, popularity));
        }

        productRepository.saveAll(products);
        System.out.println("10,000 products inserted successfully!");
    }

    private void buildTrieAndPopularityMap() {
        Trie trie = new Trie();
        Map<String, Integer> popularityMap = new HashMap<>();

        // Single findAll() call feeds both the Trie and the popularity map,
        // instead of two separate queries hitting MySQL for the same data.
        List<Product> allProducts = productRepository.findAll();

        for (Product product : allProducts) {
            String name = product.getName().toLowerCase();
            trie.insert(name);
            popularityMap.put(name, product.getPopularity());
        }

        trieHolder.setTrie(trie);
        trieHolder.setPopularityMap(popularityMap);

        System.out.println("Trie and popularity map built with " + allProducts.size() + " products.");
    }
}
