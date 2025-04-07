package com.ecommerce.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecommerce.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @Query("SELECT p FROM Product p WHERE p.category.name = :category")
    List<Product> findByCategoryName(@Param("category") String category);

    @Query("SELECT p FROM Product p WHERE p.title LIKE %:keyword%")
    List<Product> findByTitleContainingIgnoreCase(@Param("keyword") String keyword);

    @Query("SELECT p FROM Product p WHERE p.discountPersent > :minDiscount")
    List<Product> findByDiscountPersentGreaterThan(@Param("minDiscount") int minDiscount);

    @Query("SELECT p FROM Product p WHERE " +
           "(:category IS NULL OR p.category.name = :category) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:minDiscount IS NULL OR p.discountPersent >= :minDiscount) " +
           "ORDER BY " +
           "CASE WHEN :sort = 'price_low' THEN p.price END ASC, " +
           "CASE WHEN :sort = 'price_high' THEN p.price END DESC, " +
           "CASE WHEN :sort = 'discount_high' THEN p.discountPersent END DESC")
    List<Product> filterProducts(@Param("category") String category,
                                @Param("minPrice") Integer minPrice,
                                @Param("maxPrice") Integer maxPrice,
                                @Param("minDiscount") Integer minDiscount,
                                @Param("sort") String sort);
}
