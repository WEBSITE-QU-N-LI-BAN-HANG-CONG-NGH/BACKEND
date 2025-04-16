package com.webanhang.team_project.service.product;


import com.webanhang.team_project.dto.product.AddProductRequest;
import com.webanhang.team_project.dto.product.ProductDTO;
import com.webanhang.team_project.dto.product.CreateProductRequest;
import com.webanhang.team_project.dto.product.UpdateProductRequest;
import com.webanhang.team_project.exceptions.GlobalExceptionHandler;
import com.webanhang.team_project.model.*;
import com.webanhang.team_project.repository.CartItemRepository;
import com.webanhang.team_project.repository.CategoryRepository;
import com.webanhang.team_project.repository.OrderItemRepository;
import com.webanhang.team_project.repository.ProductRepository;
import com.webanhang.team_project.service.image.ImageService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final ImageService imageService;

    @Override
    @Transactional
    public Product createProduct(CreateProductRequest req)  {
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
            throw new EntityNotFoundException("Product not found with id: " + id);
        }
        // Xóa tất cả hình ảnh của sản phẩm
        imageService.deleteAllProductImages(id);

        productRepository.delete(product);
        return "Product deleted successfully";
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, Product product)  {
        Product existingProduct = findProductById(id);
        if(existingProduct.getQuantity() != 0) {
            existingProduct.setQuantity(product.getQuantity());
        }
        return productRepository.save(existingProduct);
    }

    @Override
    public Product findProductById(Long id)  {
        Optional<Product> product = productRepository.findById(id);
        if(product.isPresent()) {
            return product.get();
        }
        throw new EntityNotFoundException("Product not found with id: " + id);
    }

    @Override
    public List<Product> findProductByCategory(String category) {
        return productRepository.findByCategoryName(category);
    }

    @Override
    public Page<Product> findAllProductsByFilter(String category, List<String> colors, List<String> sizes,
                                                 Integer minPrice, Integer maxPrice, Integer minDiscount, String sort, String stock,
                                                 Integer pageNumber, Integer pageSize){

        System.out.println("Filtering products with: category=" + category + ", colors=" + colors + ", sizes=" + sizes);

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        // Nếu category là null, lấy tất cả sản phẩm
        List<Product> products;
        if (category == null || category.isEmpty()) {
            System.out.println("Category is null or empty, fetching all products");
            products = productRepository.findAll();
        } else {
            products = productRepository.filterProducts(category, minPrice, maxPrice, minDiscount, sort, "");
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
    public List<Product> findAllProducts()  {
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
    public List<Map<String, Object>> getTopSellingProducts(int limit) {
        // Triển khai phương thức lấy các sản phẩm bán chạy nhất
        // Đây là triển khai mẫu, cần sửa theo logic thực tế của ứng dụng
        List<Map<String, Object>> result = new ArrayList<>();
        List<Product> products = productRepository.findAll();
        // Sắp xếp theo số lượng đã bán giảm dần (ở đây giả định)
        products.sort((p1, p2) -> Integer.compare(p2.getQuantity(), p1.getQuantity()));
        // Lấy limit sản phẩm đầu tiên
        int count = Math.min(limit, products.size());
        for (int i = 0; i < count; i++) {
            Product p = products.get(i);
            Map<String, Object> productMap = new HashMap<>();
            productMap.put("id", p.getId());
            productMap.put("title", p.getTitle());
            productMap.put("brand", p.getBrand());
            productMap.put("price", p.getPrice());
            productMap.put("quantity", p.getQuantity());
            result.add(productMap);
        }
        return result;
    }
    
    @Override
    public Map<String, Object> getRevenueByCateogry() {
        // Triển khai phương thức lấy doanh thu theo danh mục
        // Đây là triển khai mẫu, cần sửa theo logic thực tế của ứng dụng
        Map<String, Object> result = new HashMap<>();
        List<Product> allProducts = productRepository.findAll();
        // Nhóm sản phẩm theo danh mục
        Map<String, Double> categoryRevenue = new HashMap<>();
        for (Product product : allProducts) {
            String category = product.getCategory() != null ? product.getCategory().getName() : "Uncategorized";
            Double revenue = categoryRevenue.getOrDefault(category, 0.0);
            // Giả định doanh thu dựa trên giá và số lượng
            revenue += product.getPrice() * product.getQuantity();
            categoryRevenue.put(category, revenue);
        }
        result.put("categoryRevenue", categoryRevenue);
        return result;
    }

    public Product addImageToProduct(Long productId, Image image) {
        Product product = findProductById(productId);
        image.setProduct(product);
        product.getImages().add(image);
        return productRepository.save(product);
    }

    public Product removeImageFromProduct(Long productId, Long imageId) {
        Product product = findProductById(productId);
        product.getImages().removeIf(image -> image.getId().equals(imageId));
        return productRepository.save(product);
    }
}    