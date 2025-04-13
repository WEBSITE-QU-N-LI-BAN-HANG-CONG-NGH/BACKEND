package com.webanhang.team_project.service.product;


import com.webanhang.team_project.dto.product.AddProductRequest;
import com.webanhang.team_project.dto.product.ProductDTO;
import com.webanhang.team_project.dto.product.CreateProductRequest;
import com.webanhang.team_project.enums.OrderStatus;
import com.webanhang.team_project.model.*;
import com.webanhang.team_project.repository.*;
import com.webanhang.team_project.service.user.UserService;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
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
    private final OrderRepository orderRepository;

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
    public String deleteProduct(Long id) {
        Product product = findProductById(id);
        if(product == null) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        productRepository.delete(product);
        return "Product deleted successfully";
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, Product product) {
        Product existingProduct = findProductById(id);
        if(existingProduct.getQuantity() != 0) {
            existingProduct.setQuantity(product.getQuantity());
        }
        return productRepository.save(existingProduct);
    }

    @Override
    public Product findProductById(Long id) {
        Optional<Product> product = productRepository.findById(id);
        if(product.isPresent()) {
            return product.get();
        }
        throw new RuntimeException("Product not found with id: " + id);
    }

    @Override
    public List<Product> findProductByCategory(String category) {
        return productRepository.findByCategoryName(category);
    }

    @Override
    public Page<Product> findAllProductsByFilter(String category, List<String> colors, List<String> sizes,
                                                 Integer minPrice, Integer maxPrice, Integer minDiscount, String sort, String stock,
                                                 Integer pageNumber, Integer pageSize) {

        System.out.println("Filtering products with: category=" + category + ", colors=" + colors + ", sizes=" + sizes);

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        // Nếu category là null, lấy tất cả sản phẩm
        List<Product> products;
        if (category == null || category.isEmpty()) {
            System.out.println("Category is null or empty, fetching all products");
            products = productRepository.findAll();
        } else {
            products = productRepository.filterProducts(category, minPrice, maxPrice, minDiscount, null, sort);

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

    @Override
    public ProductDTO convertToDto(Product product) {
        ProductDTO productDto = new ProductDTO();
        productDto.setId(product.getId());
        productDto.setTitle(product.getTitle());
        productDto.setDescription(product.getDescription());
        productDto.setPrice(product.getPrice());
        productDto.setDiscountedPrice(product.getDiscountedPrice());
        productDto.setQuantity(product.getQuantity());
        productDto.setBrand(product.getBrand());
        productDto.setColor(product.getColor());

        // Chuyển đổi danh sách sizes thành danh sách Strings
        List<String> sizes = product.getSizes().stream()
                .map(ProductSize::getName)
                .collect(Collectors.toList());
        productDto.setSizes(sizes);

        productDto.setImageUrl(product.getImageUrl());

        // Tính toán average rating nếu có ratings
        if (product.getRatings() != null && !product.getRatings().isEmpty()) {
            double avgRating = product.getRatings().stream()
                    .mapToInt(Rating::getRating)
                    .average()
                    .orElse(0);
            productDto.setAverageRating((int) Math.round(avgRating));
            productDto.setNumRatings(product.getRatings().size());
        } else {
            productDto.setAverageRating(0);
            productDto.setNumRatings(0);
        }

        return productDto;
    }

    @Override
    public List<Map<String, Object>> getTopSellingProducts(int limit) {
        // Lấy tất cả sản phẩm
        List<Product> allProducts = productRepository.findAll();

        // Tính toán số lượng bán và doanh thu của mỗi sản phẩm từ đơn hàng
        List<Map<String, Object>> productStats = new ArrayList<>();

        for (Product product : allProducts) {
            // Lấy các orderItem liên quan đến sản phẩm
            List<OrderItem> orderItems = orderItemRepository.findByProductId(product.getId());

            if (!orderItems.isEmpty()) {
                int totalSold = orderItems.stream().mapToInt(OrderItem::getQuantity).sum();
                int totalRevenue = orderItems.stream()
                        .mapToInt(item -> item.getPrice() * item.getQuantity())
                        .sum();

                Map<String, Object> productStat = new HashMap<>();
                productStat.put("id", product.getId());
                productStat.put("name", product.getTitle());
                productStat.put("category", product.getCategory() != null ? product.getCategory().getName() : "");
                productStat.put("price", product.getPrice());
                productStat.put("totalSold", totalSold);
                productStat.put("revenue", totalRevenue);
                productStat.put("inStock", product.getQuantity());

                productStats.add(productStat);
            }
        }

        // Sắp xếp theo số lượng bán
        productStats.sort((a, b) -> Integer.compare((int) b.get("totalSold"), (int) a.get("totalSold")));

        // Giới hạn số lượng kết quả
        return productStats.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getRevenueByCateogry() {
        // Phân tích doanh thu theo danh mục
        Map<String, Object> result = new HashMap<>();
        Map<String, Integer> categoryRevenue = new HashMap<>();

        // Lấy tất cả đơn hàng đã giao
        List<Order> completedOrders = orderRepository.findByOrderStatus(OrderStatus.DELIVERED);

        for (Order order : completedOrders) {
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                if (product != null && product.getCategory() != null) {
                    String category = product.getCategory().getName();
                    int itemRevenue = item.getPrice() * item.getQuantity();

                    categoryRevenue.put(category, categoryRevenue.getOrDefault(category, 0) + itemRevenue);
                }
            }
        }

        result.put("categoryRevenue", categoryRevenue);
        return result;
    }
}
