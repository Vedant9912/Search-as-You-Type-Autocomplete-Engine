package com.autocomplete.autocompleteengine.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.Index;

// Index is declared here (not just added manually via SQL) so that ddl-auto=update
// recreates it automatically on any fresh machine/database - matching what the
// README claims about the B-Tree index actually existing.
@Entity
@Table(name = "product", indexes = {
    @Index(name = "idx_product_name_popularity", columnList = "name, popularity")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int popularity;

    public Product() {}

    public Product(String name, int popularity) {
        this.name = name;
        this.popularity = popularity;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getPopularity() { return popularity; }

    public void setName(String name) { this.name = name; }
    public void setPopularity(int popularity) { this.popularity = popularity; }
}
