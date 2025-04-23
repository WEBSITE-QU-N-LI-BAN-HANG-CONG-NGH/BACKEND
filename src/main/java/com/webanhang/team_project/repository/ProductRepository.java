package com.webanhang.team_project.repository;


import com.webanhang.team_project.model.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE p.title LIKE %:keyword%")
    List<Product> findByTitleContainingIgnoreCase(@Param("keyword") String keyword);

    @Query("SELECT p FROM Product p WHERE p.discountPersent > :minDiscount")
    List<Product> findByDiscountPersentGreaterThan(@Param("minDiscount") int minDiscount);


    // Fixed to return List of Products instead of single Product
    List<Product> findByCategoryIdIn(List<Long> categoryIds);

    // Added for direct product lookup by category ID
    List<Product> findByCategoryId(Long categoryId);

    @Query("SELECT p FROM Product p WHERE LOWER(p.category.name) = LOWER(:secondCategoryName) AND LOWER(p.category.parentCategory.name) = LOWER(:topCategoryName) AND p.category.level = 2")
    List<Product> findProductsByTopAndSecondCategoryNames(
            @Param("topCategoryName") String topCategoryName,
            @Param("secondCategoryName") String secondCategoryName);

    @Query("SELECT p FROM Product p ORDER BY p.quantitySold DESC")
    List<Product> findTopSellingProducts(Pageable pageable);
}