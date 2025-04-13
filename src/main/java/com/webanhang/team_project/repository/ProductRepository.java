package com.webanhang.team_project.repository;


import com.webanhang.team_project.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByBrandAndTitle(String brand, String name);
    List<Product> findByBrand(String brand);
    boolean existsByTitleAndBrand(String name, String brand);

    @Query("SELECT p FROM Product p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Product> findByName(String name);

    @Query("SELECT p FROM Product p WHERE p.category.name = :category")
    List<Product> findByCategoryName(@Param("category") String category);

    @Query("SELECT p FROM Product p WHERE p.category.name = :category AND p.brand = :brand")
    List<Product> findByCategoryNameAndBrand(@Param("category") String category, @Param("brand") String brand);

    @Query("SELECT p FROM Product p WHERE p.title LIKE %:keyword%")
    List<Product> findByTitleContainingIgnoreCase(@Param("keyword") String keyword);

    @Query("SELECT p FROM Product p WHERE p.discountPersent > :minDiscount")
    List<Product> findByDiscountPersentGreaterThan(@Param("minDiscount") int minDiscount);

    @Query("SELECT p FROM Product p WHERE " +
            "(:category IS NULL OR p.category.name = :category) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "(:minDiscount IS NULL OR p.discountPersent >= :minDiscount) AND " +
            "(:brand IS NULL OR p.brand = :brand) " +
            "ORDER BY " +
            "CASE WHEN :sort = 'price_low' THEN p.price END ASC, " +
            "CASE WHEN :sort = 'price_high' THEN p.price END DESC, " +
            "CASE WHEN :sort = 'discount_high' THEN p.discountPersent END DESC")
    List<Product> filterProducts(@Param("category") String category,
                                 @Param("minPrice") Integer minPrice,
                                 @Param("maxPrice") Integer maxPrice,
                                 @Param("minDiscount") Integer minDiscount,
                                 @Param("brand") String brand,
                                 @Param("sort") String sort);
}