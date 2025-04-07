package com.ecommerce.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ecommerce.exception.GlobalExceptionHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.model.ProductSize;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.request.CreateProductRequest;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductServiceImpl implements ProductService {

    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    private UserService userService;
    
    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository, UserService userService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userService = userService;
    }

    @Override
    @Transactional
    public Product createProduct(CreateProductRequest req) throws GlobalExceptionHandler {
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

        // Tạo sản phẩm mới
        Product product = new Product();
        product.setTitle(req.getTitle());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setDiscountPersent(req.getDiscountPersent());
        product.setDiscountedPrice(req.getDiscountedPrice());
        product.setQuantity(req.getQuantity());
        product.setBrand(req.getBrand());
        product.setColor(req.getColor());
        product.setImageUrl(req.getImageUrl());
        product.setCategory(thirdLevel);
        product.setCreatedAt(LocalDateTime.now());

        // Xử lý sizes
        if (req.getSizes() != null) {
            for (ProductSize size : req.getSizes()) {
                size.setProduct(product);
            }
            product.setSizes(req.getSizes());
        }

        return productRepository.save(product);
    }

    @Override
    @Transactional
    public String deleteProduct(Long id) throws GlobalExceptionHandler {
        Product product = findProductById(id);
        if(product == null) {
            throw new GlobalExceptionHandler("Product not found with id: " + id, "PRODUCT_NOT_FOUND");
        }
        productRepository.delete(product);
        return "Product deleted successfully";
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, Product product) throws GlobalExceptionHandler {
        Product existingProduct = findProductById(id);
        if(existingProduct.getQuantity() != 0) {
            existingProduct.setQuantity(product.getQuantity());
        }
        return productRepository.save(existingProduct);
    }

    @Override
    public Product findProductById(Long id) throws GlobalExceptionHandler {
        Optional<Product> product = productRepository.findById(id);
        if(product.isPresent()) {
            return product.get();
        }
        throw new GlobalExceptionHandler("Product not found with id: " + id, "PRODUCT_NOT_FOUND");
    }

    @Override
    public List<Product> findProductByCategory(String category) throws GlobalExceptionHandler {
        return productRepository.findByCategoryName(category);
    }

    @Override
    public Page<Product> findAllProductsByFilter(String category, List<String> colors, List<String> sizes,
            Integer minPrice, Integer maxPrice, Integer minDiscount, String sort, String stock, 
            Integer pageNumber, Integer pageSize) throws GlobalExceptionHandler {
        
        System.out.println("Filtering products with: category=" + category + ", colors=" + colors + ", sizes=" + sizes);
        
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        
        // Nếu category là null, lấy tất cả sản phẩm
        List<Product> products;
        if (category == null || category.isEmpty()) {
            System.out.println("Category is null or empty, fetching all products");
            products = productRepository.findAll();
        } else {
            products = productRepository.filterProducts(category, minPrice, maxPrice, minDiscount, sort);
        }
        
        System.out.println("Found " + products.size() + " products from initial query");

        // Xử lý colors nếu không null và không trống
        if(colors != null && !colors.isEmpty()) {
            List<String> finalColors = colors;
            products = products.stream()
                .filter(product -> product.getColor() != null && finalColors.stream()
                    .anyMatch(c -> c.equalsIgnoreCase(product.getColor())))
                .collect(Collectors.toList());
            System.out.println("After color filter: " + products.size() + " products");
        }

        // Xử lý sizes nếu không null và không trống
        if(sizes != null && !sizes.isEmpty()) {
            products = products.stream()
                .filter(product -> {
                    // Nếu sản phẩm không có danh sách size hoặc danh sách rỗng thì bỏ qua
                    if(product.getSizes() == null || product.getSizes().isEmpty()) {
                        return false;
                    }
                    
                    // Kiểm tra xem có size nào khớp với yêu cầu không
                    return product.getSizes().stream()
                        .anyMatch(size -> size != null && size.getName() != null && 
                                sizes.stream().anyMatch(s -> 
                                    s.equalsIgnoreCase(size.getName()) || 
                                    size.getName().toLowerCase().contains(s.toLowerCase())));
                })
                .collect(Collectors.toList());
            System.out.println("After size filter: " + products.size() + " products");
        }

        if(stock != null) {
            if(stock.equals("in_stock")) {
                products = products.stream().filter(p -> p.getQuantity() > 0).collect(Collectors.toList());
            }
            else if(stock.equals("out_of_stock")) {
                products = products.stream().filter(p -> p.getQuantity() == 0).collect(Collectors.toList());
            }
            System.out.println("After stock filter: " + products.size() + " products");
        }

        int startIndex = (int) pageable.getOffset();
        int endIndex = Math.min((startIndex + pageable.getPageSize()), products.size());
        
        if (startIndex >= products.size()) {
            System.out.println("Returning empty page because startIndex >= products.size()");
            return new PageImpl<>(new ArrayList<>(), pageable, products.size());
        }
        
        List<Product> pageProducts = products.subList(startIndex, endIndex);
        System.out.println("Returning " + pageProducts.size() + " products on page " + pageNumber);

        return new PageImpl<>(pageProducts, pageable, products.size());
    }

    @Override
    public List<Product> findAllProducts() throws GlobalExceptionHandler {
        return productRepository.findAll();
    }

    @Override
    public List<Product> searchProducts(String keyword) throws GlobalExceptionHandler {
        return productRepository.findByTitleContainingIgnoreCase(keyword);
    }

    @Override
    public List<Product> getFeaturedProducts() throws GlobalExceptionHandler {
        return productRepository.findByDiscountPersentGreaterThan(0);
    }
}
