package com.example.backendfiveflowers.entity;


import jakarta.persistence.*;

@Entity
class ProductImages {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productImageId;
    private Long productId;
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "productId", insertable = false, updatable = false)
    private Products product;

    // Getters and Setters
}