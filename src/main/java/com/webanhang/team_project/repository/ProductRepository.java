package com.webanhang.team_project.repository;


import com.webanhang.team_project.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByCategoryNameAndBrand(String category, String brand);
    List<Product> findByBrandAndName(String brand, String name);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Product> findByName(String name);
    List<Product> findByBrand(String brand);
    List<Product> findByCategoryName(String category);

    boolean existsByNameAndBrand(String name, String brand);
}
