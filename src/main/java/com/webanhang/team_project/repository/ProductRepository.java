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


    // Fixed to return List of Products instead of single Product
    List<Product> findByCategoryIdIn(List<Long> categoryIds);

    @Query("SELECT p FROM Product p WHERE LOWER(p.category.name) = LOWER(:secondCategoryName) AND LOWER(p.category.parentCategory.name) = LOWER(:topCategoryName) AND p.category.level = 2")
    List<Product> findProductsByTopAndSecondCategoryNames(
            @Param("topCategoryName") String topCategoryName,
            @Param("secondCategoryName") String secondCategoryName);

    @Query("SELECT p FROM Product p ORDER BY p.quantitySold DESC")
    List<Product> findTopSellingProducts(Pageable pageable);

    Product getProductById(Long productId);
    // Tìm tất cả sản phẩm theo danh mục
    List<Product> findByCategoryName(String categoryName);

    // Tìm sản phẩm theo phần tên (cho tìm kiếm)
    List<Product> findByTitleContaining(String productName);

    // Tìm sản phẩm theo người bán (sử dụng sellerId)
    List<Product> findBySellerId(Long sellerId);

    // Tìm sản phẩm theo ID và người bán
    Product findByIdAndSellerId(Long productId, Long sellerId);

    // Tìm sản phẩm bán chạy nhất của người bán
    @Query("SELECT p FROM Product p WHERE p.sellerId = :sellerId ORDER BY p.quantitySold DESC")
    List<Product> findTopSellingProductsBySeller(@Param("sellerId") Long sellerId, Pageable pageable);

    // Tìm sản phẩm theo khoảng giá
    @Query("SELECT p FROM Product p WHERE p.sellerId = :sellerId AND p.price BETWEEN :minPrice AND :maxPrice")
    List<Product> findBySellerIdAndPriceBetween(
            @Param("sellerId") Long sellerId,
            @Param("minPrice") int minPrice,
            @Param("maxPrice") int maxPrice);

    // Tính tổng số sản phẩm đã bán của một người bán
    @Query("SELECT SUM(p.quantitySold) FROM Product p WHERE p.sellerId = :sellerId")
    Long sumQuantitySoldBySellerId(@Param("sellerId") Long sellerId);

    // Tính tổng doanh thu của một người bán
    @Query("SELECT SUM(p.discountedPrice * p.quantitySold) FROM Product p WHERE p.sellerId = :sellerId")
    Integer calculateTotalRevenueBySellerId(@Param("sellerId") Long sellerId);
}