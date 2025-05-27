package com.webanhang.team_project.service.seller;

import com.webanhang.team_project.dto.product.CreateProductRequest;
import com.webanhang.team_project.dto.product.ProductDTO;
import com.webanhang.team_project.enums.OrderStatus;
import com.webanhang.team_project.enums.PaymentStatus;
import com.webanhang.team_project.model.*;
import com.webanhang.team_project.repository.*;
import com.webanhang.team_project.service.product.IProductService;
import jakarta.persistence.EntityNotFoundException;
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
import java.util.ArrayList;
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
    private final ImageRepository imageRepository;
    private final IProductService productService;

    @Override
    @Transactional
    public ProductDTO createProduct(CreateProductRequest req)  {
        // Xử lý category theo cấp bậc mới (tối đa 2 cấp)
        Category parentCategory = null;
        Category category = null;

        // Xử lý cấp 1 (parent category)
        if (req.getTopLevelCategory() != null && !req.getTopLevelCategory().isEmpty()) {
            parentCategory = categoryRepository.findByName(req.getTopLevelCategory());
            if (parentCategory == null) {
                parentCategory = new Category();
                parentCategory.setName(req.getTopLevelCategory());
                parentCategory.setLevel(1);
                parentCategory.setParent(true);
                parentCategory = categoryRepository.save(parentCategory);
            } else if (parentCategory.getLevel() != 1) {
                throw new IllegalArgumentException("Top level category must have level 1");
            }

            // Xử lý cấp 2 (nếu có)
            if (req.getSecondLevelCategory() != null && !req.getSecondLevelCategory().isEmpty()) {
                category = categoryRepository.findByName(req.getSecondLevelCategory());
                if (category == null) {
                    category = new Category();
                    category.setName(req.getSecondLevelCategory());
                    category.setLevel(2);
                    category.setParent(false);
                    category.setParentCategory(parentCategory);
                    category = categoryRepository.save(category);
                } else if (category.getLevel() != 2) {
                    throw new IllegalArgumentException("Second level category must have level 2");
                }
            } else {
                // Nếu không có cấp 2, sử dụng cấp 1
                category = parentCategory;
            }
        }

        // Tạo sản phẩm mới
        Product product = new Product();
        product.setTitle(req.getTitle());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setDiscountPersent(req.getDiscountPersent());
        product.setBrand(req.getBrand());
        product.setColor(req.getColor());
        product.setCreatedAt(LocalDateTime.now());
        product.setQuantity(req.getQuantity());
        product.setImages(req.getImageUrls());

        // Lưu sellerId nếu có
        if (req.getSellerId() != null) {
            product.setSellerId(req.getSellerId());
        }

        // Cập nhật discountedPrice dựa vào price và discountPersent
        product.updateDiscountedPrice();

        // Gán category cho sản phẩm (sẽ là cấp 2 nếu có, nếu không thì là cấp 1)
        product.setCategory(category);

        // Xử lý sizes
        if (req.getSizes() != null) {
            for (ProductSize size : req.getSizes()) {
                size.setProduct(product);
            }
            product.setSizes(req.getSizes());
        }

        // xu ly hinh anh
        if (req.getImageUrls() != null && !req.getImageUrls().isEmpty()) {
            for (Image imageUrl : req.getImageUrls()) {
                imageUrl.setProduct(product);
            }
            product.setImages(req.getImageUrls());
        }
        product = productRepository.save(product);
        ProductDTO productDto = new ProductDTO(product);

        return productDto;
    }

    @Override
    @Transactional
    public ProductDTO updateProduct(Long productId, Product product)  {
        Product existingProduct = productRepository.getProductById(productId);

        // Cập nhật các thuộc tính cơ bản
        if (product.getTitle() != null) {
            existingProduct.setTitle(product.getTitle());
        }
        if (product.getDescription() != null) {
            existingProduct.setDescription(product.getDescription());
        }
        if (product.getBrand() != null) {
            existingProduct.setBrand(product.getBrand());
        }
        if (product.getColor() != null) {
            existingProduct.setColor(product.getColor());
        }

        // Xử lý danh sách hình ảnh mới
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            // Xóa tất cả hình ảnh cũ
            existingProduct.getImages().clear();

            // Thêm hình ảnh mới
            for (Image image : product.getImages()) {
                image.setProduct(existingProduct);
                existingProduct.getImages().add(image);
            }
        }

        // Cập nhật giá và giảm giá
        if (product.getPrice() > 0) {
            existingProduct.setPrice(product.getPrice());
        }
        if (product.getDiscountPersent() >= 0) {
            existingProduct.setDiscountPersent(product.getDiscountPersent());
        }

        // Cập nhật discountedPrice
        existingProduct.updateDiscountedPrice();

        // Cập nhật số lượng
        if (product.getQuantity() >= 0) {
            existingProduct.setQuantity(product.getQuantity());
        }

        // Cập nhật category nếu có thay đổi
        if (product.getCategory() != null && product.getCategory().getId() != null) {
            Category category = categoryRepository.findById(product.getCategory().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found"));
            existingProduct.setCategory(category);
        }

        Product res = productRepository.save(existingProduct);
        ProductDTO productDto = new ProductDTO(res);

        return productDto;
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        productService.deleteProduct(productId);
    }


    @Override
    public Page<ProductDTO> getSellerProducts(Long sellerId, Pageable pageable) {
        Page<Product> productPage = productRepository.findBySellerIdWithPagination(sellerId, pageable);

        return productPage.map(ProductDTO::new);
    }

    @Override
    public ProductDTO getProductDetail(Long productId) {
        Product product = productService.findProductById(productId);
        ProductDTO productDTO = new ProductDTO(product);
        return productDTO;
    }

    @Override
    @Transactional
    public Map<String, Object> getProductStatOfSeller(Long sellerId) {

        List<Product> products = productRepository.findBySellerId(sellerId);

        Map<String, Object> stats = new HashMap<>();

        // Tổng số sản phẩm
        stats.put("totalProducts", products.size());

        // Tổng số lượng đã bán
        long totalSold = products.stream()
                .filter(p -> p.getQuantitySold() != null)
                .mapToLong(Product::getQuantitySold)
                .sum();
        stats.put("totalSold", totalSold);

        // Tổng doanh thu
        int totalRevenue = products.stream()
                .filter(p -> p.getQuantitySold() != null)
                .mapToInt(p -> p.getDiscountedPrice() * p.getQuantitySold().intValue())
                .sum();
        stats.put("totalRevenue", totalRevenue);

        // Sản phẩm bán chạy nhất
        Product bestSeller = products.stream()
                .filter(p -> p.getQuantitySold() != null && p.getQuantitySold() > 0)
                .max((p1, p2) -> p1.getQuantitySold().compareTo(p2.getQuantitySold()))
                .orElse(null);

        if (bestSeller != null) {
            Map<String, Object> bestSellerInfo = new HashMap<>();
            bestSellerInfo.put("id", bestSeller.getId());
            bestSellerInfo.put("title", bestSeller.getTitle());
            bestSellerInfo.put("sold", bestSeller.getQuantitySold());
            bestSellerInfo.put("revenue", bestSeller.getDiscountedPrice() * bestSeller.getQuantitySold());
            stats.put("bestSeller", bestSellerInfo);
        }
        return stats;
    }

    @Override
    @Transactional
    public List<ProductDTO> createMultipleProducts(List<CreateProductRequest> requests) {
        return requests.stream()
                .map(req -> productService.createProduct(req))
                .map(product -> new ProductDTO(product))
                .toList();
    }
}


