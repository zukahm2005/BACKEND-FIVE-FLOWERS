package com.example.backendfiveflowers.service;

import com.example.backendfiveflowers.entity.Brand;
import com.example.backendfiveflowers.entity.Category;
import com.example.backendfiveflowers.entity.Product;
import com.example.backendfiveflowers.repository.BrandRepository;
import com.example.backendfiveflowers.repository.CategoryRepository;
import com.example.backendfiveflowers.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    public Product addProduct(Product product) {
        Optional<Brand> brand = brandRepository.findById(product.getBrand().getBrandId());
        Optional<Category> category = categoryRepository.findById(product.getCategory().getCategoryId());

        if (brand.isPresent() && category.isPresent()) {
            product.setBrand(brand.get());
            product.setCategory(category.get());
            return productRepository.save(product);
        } else {
            throw new RuntimeException("Brand or Category not found");
        }
    }

    public Product updateProduct(Integer id, Product productDetails) {
        Optional<Product> existingProduct = productRepository.findById(id);
        if (existingProduct.isPresent()) {
            Product product = existingProduct.get();
            product.setName(productDetails.getName());
            product.setDescription(productDetails.getDescription());
            product.setPrice(productDetails.getPrice());
            product.setQuantity(productDetails.getQuantity());
            product.setColor(productDetails.getColor());

            Optional<Brand> brand = brandRepository.findById(productDetails.getBrand().getBrandId());
            Optional<Category> category = categoryRepository.findById(productDetails.getCategory().getCategoryId());

            if (brand.isPresent() && category.isPresent()) {
                product.setBrand(brand.get());
                product.setCategory(category.get());
            } else {
                throw new RuntimeException("Brand or Category not found");
            }

            return productRepository.save(product);
        } else {
            throw new RuntimeException("Product not found with id: " + id);
        }
    }

    public void deleteProduct(Integer id) {
        productRepository.deleteById(id);
    }

    public Optional<Product> getProductById(Integer id) {
        return productRepository.findById(id);
    }

    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }
}
