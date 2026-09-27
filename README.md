# Search-as-You-Type Autocomplete Engine

A backend search-as-you-type autocomplete system built with Java and Spring Boot. It starts from a naive MySQL `LIKE` query baseline, measures its real performance, and progressively replaces it with a custom in-memory Trie and a Min-Heap-based Top-K ranking algorithm — validating correctness and latency at every step rather than assuming improvements.

## Architecture

```
Client (browser)
      |
Spring Boot REST API   (/search-topk?q=...)
      |
Trie (in-memory)       -- built once at startup from MySQL, prefix traversal
      |
Min-Heap ranking       -- Top-10 by popularity, O(n log k)
      |
JSON response

MySQL --------------------------------> loaded once at startup into the Trie
                                          (not on the live search path)
```

MySQL is only used once, at application startup, to build the Trie and a `name -> popularity` lookup map. Every live search request is served entirely from memory — no database round-trip.

## Benchmark results (local, 10,000-item dataset)

| Approach                              | p50  | p95  | p99  |
|----------------------------------------|------|------|------|
| MySQL - naive `LIKE` (no index)        | 6ms  | 12ms | 19ms |
| MySQL - with B-Tree index              | 3ms  | 5ms  | 8ms  |
| Trie - full collect (unranked)         | 0ms  | 0ms  | 5-7ms|
| Trie - Top-K with Min-Heap (ranked)    | 0ms  | 0ms  | 2ms  |

*These are local benchmark numbers from a custom p50/p95/p99 latency tool over 100 sample searches on a 10,000-row dataset — not production-scale figures.*

## What's implemented

**Phase 1 - Baseline**
- MySQL-backed naive prefix search (`LIKE 'prefix%'`)
- Diagnosed a full table scan using `EXPLAIN`; added a B-Tree index, declared directly
  on the `Product` entity via `@Table(indexes = ...)` so it's recreated automatically
  on any fresh machine/database, not just the one it was manually tested on
- Custom latency benchmarking utility (p50/p95/p99)

**Phase 2 - Trie**
- Custom `Trie` / `TrieNode` implementation from scratch (no libraries)
- `insert()`, `searchByPrefix()` (recursive DFS collection)
- JUnit tests covering insertion, prefix lookup, missing-prefix, and top-k ranking order

**Phase 3 - Ranking**
- `searchTopK()` using a Min-Heap (`PriorityQueue`) to return the top 10 results by popularity in O(n log k) time, without collecting or sorting the full result set
- Comparator uses `Integer.compare(...getOrDefault(word, 0))` rather than raw subtraction, avoiding a possible NullPointerException (if a word were ever missing from the popularity map) and integer overflow on large popularity gaps

**Startup / initialization**
- `DataInitializer` (a single `CommandLineRunner`) seeds the database if empty, then builds the Trie and popularity map from **one** `findAll()` call. Earlier this project built the Trie via a `@Bean` factory method, which runs *before* any `CommandLineRunner` during Spring context startup - on a genuinely empty database, that meant the Trie was built from zero rows and never got the seeded data until a restart. Combining seeding and Trie-building into a single ordered runner fixes this, and also removes a redundant second `findAll()` that was fetching the same 10,000 rows twice.
- `TrieHolder` is a small mutable holder bean; `DataInitializer` populates it once at startup, and `SearchController` reads from it for every request.

**Edge cases handled**
- Blank/empty `q` parameter returns an empty list instead of walking the whole Trie and returning every product.

**Not yet implemented (planned next)**
- Typo tolerance via Levenshtein distance + BK-tree
- LRU cache for hot prefixes
- Returning DTOs instead of the JPA `Product` entity directly from `/search`
- Load testing at larger scale (100K-1M items)

## Tech stack

Java 21, Spring Boot, Spring Data JPA, MySQL 8, Maven, JUnit 5

## Project structure

```
src/main/java/com/autocomplete/autocompleteengine/
├── model/          Product entity
├── repository/      ProductRepository (naive MySQL search)
├── config/          DataInitializer (seeds 10K products + builds Trie/popularity map, in order), TrieHolder (shared bean)
├── controller/       SearchController (all REST endpoints)
├── trie/             Trie, TrieNode (custom data structure)
└── util/             LatencyStats (p50/p95/p99 calculator)

src/test/java/.../trie/TrieTest.java   JUnit tests
```

## Setup

1. Create the database in MySQL:
   ```sql
   CREATE DATABASE autocomplete_db;
   ```
2. Set your MySQL password in `src/main/resources/application.properties`.
3. Run the application (`AutocompleteEngineApplication.java`). It will seed 10,000 products on first run and build the Trie + popularity map at startup.

## API endpoints

| Endpoint | Description |
|---|---|
| `GET /search?q=net` | Naive MySQL search, top 10 by popularity |
| `GET /search-trie?q=net` | Trie search, all matches, unranked |
| `GET /search-topk?q=net` | Trie + Min-Heap, top 10 ranked by popularity |
| `GET /benchmark` | p50/p95/p99 latency for the MySQL approach |
| `GET /benchmark-trie` | p50/p95/p99 latency for the Trie (unranked) |
| `GET /benchmark-topk` | p50/p95/p99 latency for the Trie + Top-K approach |

## Design notes / trade-offs

- `spring.jpa.hibernate.ddl-auto=update` is used for development convenience; a production system would use a migration tool (Flyway/Liquibase) instead.
- The Trie is built once at startup and treated as read-only during requests, so no thread-safety mechanism is needed yet. If live product inserts were added, the Trie's `insert()` calls would need synchronization.
- All product names are lowercased before insertion/search to avoid case-sensitivity mismatches (Java `char` comparison is case-sensitive; MySQL's default collation is not).
