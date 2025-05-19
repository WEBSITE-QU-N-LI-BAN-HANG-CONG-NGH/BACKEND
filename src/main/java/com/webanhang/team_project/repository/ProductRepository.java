package com.webanhang.team_project.repository;

import com.webanhang.team_project.model.Category;
import com.webanhang.team_project.model.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // Find products by title containing keyword (case insensitive)
    @Query("SELECT p FROM Product p WHERE LOWER(p.title) LIKE LOWER(concat('%', :keyword, '%'))")
    List<Product> findByTitleContainingIgnoreCase(@Param("keyword") String keyword);

    // More efficient search that can combine keyword and category
    @Query("SELECT p FROM Product p " +
            "WHERE (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(concat('%', :keyword, '%'))) " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId)")
    List<Product> findByKeywordAndCategory(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId);

    // Find products by multiple category IDs
    List<Product> findByCategoryIdIn(List<Long> categoryIds);

    // Find products by top and second level category names
    @Query("SELECT p FROM Product p WHERE LOWER(p.category.name) = LOWER(:secondCategoryName) " +
            "AND LOWER(p.category.parentCategory.name) = LOWER(:topCategoryName) " +
            "AND p.category.level = 2")
    List<Product> findProductsByTopAndSecondCategoryNames(
            @Param("topCategoryName") String topCategoryName,
            @Param("secondCategoryName") String secondCategoryName);

    // Find top selling products
    @Query("SELECT p FROM Product p ORDER BY p.quantitySold DESC")
    List<Product> findTopSellingProducts(Pageable pageable);

    // Advanced search with multiple filters
    @Query("SELECT p FROM Product p " +
            "WHERE (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(concat('%', :keyword, '%'))) " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
            "AND (:color IS NULL OR LOWER(p.color) = LOWER(:color)) " +
            "AND (:minPrice IS NULL OR p.discountedPrice >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.discountedPrice <= :maxPrice) " +
            "ORDER BY " +
            "CASE WHEN :sort = 'price_low' THEN p.discountedPrice END ASC, " +
            "CASE WHEN :sort = 'price_high' THEN p.discountedPrice END DESC, " +
            "CASE WHEN :sort = 'discount' THEN p.discountPersent END DESC, " +
            "CASE WHEN :sort = 'newest' THEN p.createdAt END DESC")
    List<Product> findByFilters(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("color") String color,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("sort") String sort);

    // Get product by ID
    Product getProductById(Long productId);

    // Find products by category name
    List<Product> findByCategoryName(String categoryName);

    // Find products by seller ID
    List<Product> findBySellerId(Long sellerId);

    // Find product by ID and seller ID
    Product findByIdAndSellerId(Long productId, Long sellerId);

    // Find top selling products by seller
    @Query("SELECT p FROM Product p WHERE p.sellerId = :sellerId ORDER BY p.quantitySold DESC")
    List<Product> findTopSellingProductsBySeller(@Param("sellerId") Long sellerId, Pageable pageable);

    // Find products by price range for a specific seller
    @Query("SELECT p FROM Product p WHERE p.sellerId = :sellerId AND p.price BETWEEN :minPrice AND :maxPrice")
    List<Product> findBySellerIdAndPriceBetween(
            @Param("sellerId") Long sellerId,
            @Param("minPrice") int minPrice,
            @Param("maxPrice") int maxPrice);

    // Calculate total quantity sold by seller
    @Query("SELECT SUM(p.quantitySold) FROM Product p WHERE p.sellerId = :sellerId")
    Long sumQuantitySoldBySellerId(@Param("sellerId") Long sellerId);

    // Calculate total revenue by seller
    @Query("SELECT SUM(p.discountedPrice * p.quantitySold) FROM Product p WHERE p.sellerId = :sellerId")
    Integer calculateTotalRevenueBySellerId(@Param("sellerId") Long sellerId);

    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchProducts(@Param("keyword") String keyword);

    List<Product> findByCategory(Category category);
}