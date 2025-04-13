package com.webanhang.team_project.service.seller;

import com.webanhang.team_project.dto.product.CreateProductRequest;
import com.webanhang.team_project.enums.OrderStatus;
import com.webanhang.team_project.enums.PaymentStatus;
import com.webanhang.team_project.model.*;
import com.webanhang.team_project.repository.CategoryRepository;
import com.webanhang.team_project.repository.OrderRepository;
import com.webanhang.team_project.repository.ProductRepository;
import com.webanhang.team_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SellerProductService implements ISellerProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    public Page<Product> getSellerProducts(Long sellerId, int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);

        // Đảm bảo người bán tồn tại
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người bán"));

        // Lấy tất cả sản phẩm
        List<Product> allProducts = productRepository.findAll();

        // Lọc sản phẩm của người bán
        List<Product> sellerProducts = allProducts.stream()
                .filter(product -> product.getCategory() != null &&
                        product.getCategory().getProducts() != null &&
                        product.getCategory().getProducts().stream()
                                .anyMatch(p -> p.getId().equals(sellerId)))
                .collect(Collectors.toList());

        // Tìm kiếm nếu có từ khóa
        if (search != null && !search.isEmpty()) {
            sellerProducts = sellerProducts.stream()
                    .filter(product ->
                            (product.getTitle() != null && product.getTitle().toLowerCase().contains(search.toLowerCase())) ||
                                    (product.getDescription() != null && product.getDescription().toLowerCase().contains(search.toLowerCase())) ||
                                    (product.getBrand() != null && product.getBrand().toLowerCase().contains(search.toLowerCase()))
                    )
                    .collect(Collectors.toList());
        }

        // Phân trang kết quả
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sellerProducts.size());

        return new PageImpl<>(
                sellerProducts.subList(start, end),
                pageable,
                sellerProducts.size()
        );
    }

    @Override
    @Transactional
    public Product createProduct(Long sellerId, CreateProductRequest request) {
        // Đảm bảo người bán tồn tại
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người bán"));

        // Xử lý category
        Category topLevel = categoryRepository.findByName(request.getTopLevelCategory());
        if(topLevel == null) {
            topLevel = new Category();
            topLevel.setName(request.getTopLevelCategory());
            topLevel.setLevel(1);
            topLevel = categoryRepository.save(topLevel);
        }

        Category secondLevel = categoryRepository.findByName(request.getSecondLevelCategory());
        if(secondLevel == null) {
            secondLevel = new Category();
            secondLevel.setName(request.getSecondLevelCategory());
            secondLevel.setLevel(2);
            secondLevel.setParentCategory(topLevel);
            secondLevel = categoryRepository.save(secondLevel);
        }

        Category thirdLevel = categoryRepository.findByName(request.getThirdLevelCategory());
        if(thirdLevel == null) {
            thirdLevel = new Category();
            thirdLevel.setName(request.getThirdLevelCategory());
            thirdLevel.setLevel(3);
            thirdLevel.setParentCategory(secondLevel);
            thirdLevel = categoryRepository.save(thirdLevel);
        }

        // Tạo sản phẩm mới
        Product product = new Product();
        product.setTitle(request.getTitle());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setDiscountPersent(request.getDiscountPersent());
        product.setDiscountedPrice(request.getPrice() - (request.getPrice() * request.getDiscountPersent() / 100));
        product.setQuantity(request.getQuantity());
        product.setBrand(request.getBrand());
        product.setColor(request.getColor());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(thirdLevel);
        product.setCreatedAt(LocalDateTime.now());

        // Xử lý sizes
        if (request.getSizes() != null) {
            for (ProductSize size : request.getSizes()) {
                size.setProduct(product);
            }
            product.setSizes(request.getSizes());
        }

        return productRepository.save(product);
    }

    @Override
    public Product getProductDetail(Long sellerId, Long productId) {
        // Đảm bảo người bán tồn tại
        userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người bán"));

        // Lấy sản phẩm
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        // Kiểm tra sản phẩm thuộc người bán
        if (product.getCategory() == null ||
                product.getCategory().getProducts() == null ||
                product.getCategory().getProducts().stream()
                        .noneMatch(p -> p.getId().equals(sellerId))) {
            throw new RuntimeException("Sản phẩm không thuộc người bán này");
        }

        return product;
    }

    @Override
    @Transactional
    public Product updateProduct(Long sellerId, Long productId, Product productRequest) {
        // Kiểm tra sản phẩm
        Product existingProduct = getProductDetail(sellerId, productId);

        // Cập nhật thông tin
        if (productRequest.getTitle() != null) {
            existingProduct.setTitle(productRequest.getTitle());
        }
        if (productRequest.getDescription() != null) {
            existingProduct.setDescription(productRequest.getDescription());
        }
        if (productRequest.getPrice() > 0) {
            existingProduct.setPrice(productRequest.getPrice());
            // Cập nhật giá sau giảm giá
            existingProduct.updateDiscountedPrice();
        }
        if (productRequest.getDiscountPersent() >= 0) {
            existingProduct.setDiscountPersent(productRequest.getDiscountPersent());
            // Cập nhật giá sau giảm giá
            existingProduct.updateDiscountedPrice();
        }
        if (productRequest.getQuantity() >= 0) {
            existingProduct.setQuantity(productRequest.getQuantity());
        }
        if (productRequest.getBrand() != null) {
            existingProduct.setBrand(productRequest.getBrand());
        }
        if (productRequest.getColor() != null) {
            existingProduct.setColor(productRequest.getColor());
        }
        if (productRequest.getImageUrl() != null) {
            existingProduct.setImageUrl(productRequest.getImageUrl());
        }

        // Cập nhật sizes nếu có
        if (productRequest.getSizes() != null && !productRequest.getSizes().isEmpty()) {
            // Xóa sizes cũ
            existingProduct.getSizes().clear();

            // Thêm sizes mới
            for (ProductSize size : productRequest.getSizes()) {
                size.setProduct(existingProduct);
                existingProduct.getSizes().add(size);
            }
        }

        return productRepository.save(existingProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long sellerId, Long productId) {
        // Kiểm tra sản phẩm
        Product product = getProductDetail(sellerId, productId);

        // Xóa sản phẩm
        productRepository.delete(product);
    }

    @Override
    @Transactional
    public Product updateInventory(Long sellerId, Long productId, int quantity) {
        // Kiểm tra sản phẩm
        Product product = getProductDetail(sellerId, productId);

        // Cập nhật số lượng
        product.setQuantity(quantity);

        return productRepository.save(product);
    }
}


