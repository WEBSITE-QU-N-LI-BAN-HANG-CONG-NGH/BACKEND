package com.webanhang.team_project.service.product;


import com.webanhang.team_project.dto.product.AddProductRequest;
import com.webanhang.team_project.dto.product.ProductDTO;
import com.webanhang.team_project.dto.product.CreateProductRequest;
import com.webanhang.team_project.dto.product.UpdateProductRequest;
import com.webanhang.team_project.model.*;
import com.webanhang.team_project.repository.CartItemRepository;
import com.webanhang.team_project.repository.CategoryRepository;
import com.webanhang.team_project.repository.OrderItemRepository;
import com.webanhang.team_project.repository.ProductRepository;
import com.webanhang.team_project.service.user.UserService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService implements IProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final ModelMapper modelMapper;
    private final UserService userService;

    @Override
    public Product updateProduct(UpdateProductRequest request, int productId) {
        return productRepository.findById((long) productId)
                .map(existingProduct -> updateExistingProduct(existingProduct, request))
                .map(productRepository :: save)
                .orElseThrow(() -> new EntityNotFoundException("Product not found!"));
    }

    private Product updateExistingProduct(Product existingProduct, UpdateProductRequest request) {
        existingProduct.setTitle(request.getName());
        existingProduct.setBrand(request.getBrand());
        existingProduct.setPrice(request.getPrice().intValue());
        existingProduct.setQuantity(request.getInventory());
        existingProduct.setDescription(request.getDescription());
        Category category = categoryRepository.findByName(request.getCategory().getName());
        existingProduct.setCategory(category);
        return existingProduct;
    }

    @Override
    public void deleteProduct(int productId) {
        productRepository.findById((long) productId)
                .ifPresentOrElse(product -> {
                    // Xóa các liên kết trước khi xóa sản phẩm
                    product.getCartItems().clear();
                    product.getOrderItems().forEach(orderItem -> {
                        orderItem.setProduct(null);
                    });
                    
                    Optional.ofNullable(product.getCategory())
                            .ifPresent(category -> category.getProducts().remove(product));
                    product.setCategory(null);

                    productRepository.deleteById(product.getId());
                }, () -> {
                    throw new EntityNotFoundException("Product not found!");
                });
    }

    @Override
    public Product getProductById(int productId) {
        return productRepository.findById((long) productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public List<Product> getProductsByCategoryAndBrand(String category, String brand) {
        return productRepository.findByCategoryNameAndBrand(category, brand);
    }

    @Override
    public List<Product> getProductsByBrandAndName(String brand, String name) {
        return productRepository.findByBrandAndTitle(brand, name);
    }

    @Override
    public List<Product> getProductsByName(String name) {
        return productRepository.findByName(name);
    }

    @Override
    public List<Product> getProductsByBrand(String brand) {
        return productRepository.findByBrand(brand);
    }

    @Override
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryName(category);
    }

    @Override
    public List<ProductDTO> getConvertedProducts(List<Product> products) {
        return products.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProductDTO convertToDto(Product product) {
        return modelMapper.map(product, ProductDTO.class);
    }
    
    @Override
    public Product addProduct(AddProductRequest request) {
        if (productExists(request.getName(), request.getBrand())) {
            throw new EntityExistsException(request.getName() + " already exists");
        }
        Category category = Optional.ofNullable(categoryRepository.findByName(request.getCategory().getName()))
                .orElseGet(() -> {
                    Category newCategory = new Category();
                    newCategory.setName(request.getCategory().getName());
                    newCategory.setLevel(1);
                    return categoryRepository.save(newCategory);
                });
        request.setCategory(category);
        Product product = createProduct(request, category);
        return productRepository.save(product);
    }

    private boolean productExists(String name, String brand) {
        return productRepository.existsByTitleAndBrand(name, brand);
    }

    private Product createProduct(AddProductRequest request, Category category) {
        return new Product(
                request.getName(),
                request.getBrand(),
                request.getPrice(),
                request.getInventory(),
                request.getDescription(),
                category
        );
    }
    
    @Override
    @Transactional
    public Product createProduct(CreateProductRequest req) {
        // Xử lý category theo cấp
        Category topLevel = categoryRepository.findByName(req.getTopLevelCategory());
        if(topLevel == null) {
            topLevel = new Category();
            topLevel.setName(req.getTopLevelCategory());
            topLevel.setLevel(1);
            topLevel = categoryRepository.save(topLevel);
        }

        Category secondLevel = categoryRepository.findByName(req.getSecondLevelCategory());
        if(secondLevel == null) {
            secondLevel = new Category();
            secondLevel.setName(req.getSecondLevelCategory());
            secondLevel.setLevel(2);
            secondLevel.setParentCategory(topLevel);
            secondLevel = categoryRepository.save(secondLevel);
        }

        Category thirdLevel = categoryRepository.findByName(req.getThirdLevelCategory());
        if(thirdLevel == null) {
            thirdLevel = new Category();
            thirdLevel.setName(req.getThirdLevelCategory());
            thirdLevel.setLevel(3);
            thirdLevel.setParentCategory(secondLevel);
            thirdLevel = categoryRepository.save(thirdLevel);
        }
        
        // Tạo product
        Product product = new Product();
        product.setTitle(req.getTitle());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setDiscountPersent(req.getDiscountPersent());
        product.setQuantity(req.getQuantity());
        product.setBrand(req.getBrand());
        product.setColor(req.getColor());
        product.setImageUrl(req.getImageUrl());
        product.setCategory(thirdLevel);
        product.setCreatedAt(LocalDateTime.now());
        
        // Tính giá sau khi giảm giá
        product.updateDiscountedPrice();
        
        return productRepository.save(product);
    }
    
    @Override
    public String deleteProduct(Long productId) {
        try {
            productRepository.deleteById(productId);
            return "Xóa sản phẩm thành công";
        } catch (Exception e) {
            return "Không thể xóa sản phẩm: " + e.getMessage();
        }
    }
    
    @Override
    public Product updateProduct(Long productId, Product product) {
        Product existingProduct = findProductById(productId);
        existingProduct.setTitle(product.getTitle());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setQuantity(product.getQuantity());
        existingProduct.setDiscountPersent(product.getDiscountPersent());
        existingProduct.setBrand(product.getBrand());
        existingProduct.setColor(product.getColor());
        existingProduct.setImageUrl(product.getImageUrl());
        existingProduct.updateDiscountedPrice();
        return productRepository.save(existingProduct);
    }
    
    @Override
    public Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
    }
    
    @Override
    public List<Product> findProductByCategory(String category) {
        return productRepository.findByCategoryName(category);
    }
    
    @Override
    public Page<Product> findAllProductsByFilter(String category, List<String> colors, List<String> sizes,
                                                Integer minPrice, Integer maxPrice, Integer minDiscount, String sort,
                                                String stock, Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        List<Product> products = new ArrayList<>();
        
        // Lọc theo category
        if (category != null && !category.isEmpty()) {
            products = productRepository.findByCategoryName(category);
        } else {
            products = productRepository.findAll();
        }
        
        // Lọc theo giá
        if (minPrice != null && maxPrice != null) {
            products = products.stream()
                    .filter(p -> p.getDiscountedPrice() >= minPrice && p.getDiscountedPrice() <= maxPrice)
                    .collect(Collectors.toList());
        }
        
        // Lọc theo discount
        if (minDiscount != null) {
            products = products.stream()
                    .filter(p -> p.getDiscountPersent() >= minDiscount)
                    .collect(Collectors.toList());
        }
        
        // Lọc theo màu sắc
        if (colors != null && !colors.isEmpty()) {
            products = products.stream()
                    .filter(p -> colors.contains(p.getColor()))
                    .collect(Collectors.toList());
        }
        
        // Lọc theo sizes
        if (sizes != null && !sizes.isEmpty()) {
            products = products.stream()
                    .filter(p -> p.getSizes().stream()
                            .anyMatch(size -> sizes.contains(size.getName())))
                    .collect(Collectors.toList());
        }
        
        // Lọc theo stock
        if (stock != null && stock.equals("in_stock")) {
            products = products.stream()
                    .filter(p -> p.getQuantity() > 0)
                    .collect(Collectors.toList());
        } else if (stock != null && stock.equals("out_of_stock")) {
            products = products.stream()
                    .filter(p -> p.getQuantity() == 0)
                    .collect(Collectors.toList());
        }
        
        // Sắp xếp
        if (sort != null) {
            if (sort.equals("price_low")) {
                products.sort((p1, p2) -> p1.getDiscountedPrice() - p2.getDiscountedPrice());
            } else if (sort.equals("price_high")) {
                products.sort((p1, p2) -> p2.getDiscountedPrice() - p1.getDiscountedPrice());
            }
        }
        
        // Phân trang
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), products.size());
        
        if (start > products.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }
        
        return new PageImpl<>(products.subList(start, end), pageable, products.size());
    }
    
    @Override
    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }
    
    @Override
    public List<Product> searchProducts(String keyword) {
        return productRepository.findByTitleContainingIgnoreCase(keyword);
    }
    
    @Override
    public List<Product> getFeaturedProducts() {
        return productRepository.findByDiscountPersentGreaterThan(0);
    }
}    