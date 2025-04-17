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

    @Override
    @Transactional
    public Product createProduct(CreateProductRequest req)  {
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
        product.setImageUrl(req.getImageUrl());
        product.setCreatedAt(LocalDateTime.now());
        product.setQuantity(req.getQuantity());

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

        return productRepository.save(product);
    }

    @Override
    @Transactional
    public String deleteProduct(Long id) {
        Product product = findProductById(id);
        if(product == null) {
            throw new EntityNotFoundException("Product not found with id: " + id);
        }
        productRepository.delete(product);
        return "Product deleted successfully";
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, Product product)  {
        Product existingProduct = findProductById(id);

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
        if (product.getImageUrl() != null) {
            existingProduct.setImageUrl(product.getImageUrl());
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
            // Kiểm tra xem category là cấp 1 hay cấp 2
            Category foundCategory = categoryRepository.findByName(category);
            if (foundCategory != null) {
                if (foundCategory.getLevel() == 1) {
                    // Nếu là cấp 1, lấy tất cả sản phẩm có category là cấp 1 này hoặc có parent category là cấp 1 này
                    List<Category> subCategories = categoryRepository.findByParentCategoryId(foundCategory.getId());
                    List<Long> categoryIds = new ArrayList<>();
                    categoryIds.add(foundCategory.getId());
                    subCategories.forEach(sub -> categoryIds.add(sub.getId()));
                    products = productRepository.findByCategoryIdIn(categoryIds);
                } else {
                    // Nếu là cấp 2, chỉ lấy các sản phẩm thuộc category này
                    products = productRepository.findByCategoryId(foundCategory.getId());
                }
            } else {
                products = productRepository.filterProducts(category, minPrice, maxPrice, minDiscount, sort, "");
            }
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

        // Xử lý minPrice, maxPrice nếu được chỉ định
        if (minPrice != null) {
            products = products.stream()
                    .filter(p -> p.getDiscountedPrice() >= minPrice)
                    .collect(Collectors.toList());
            System.out.println("After minPrice filter: " + products.size() + " products");
        }

        if (maxPrice != null) {
            products = products.stream()
                    .filter(p -> p.getDiscountedPrice() <= maxPrice)
                    .collect(Collectors.toList());
            System.out.println("After maxPrice filter: " + products.size() + " products");
        }

        // Xử lý minDiscount nếu được chỉ định
        if (minDiscount != null) {
            products = products.stream()
                    .filter(p -> p.getDiscountPersent() >= minDiscount)
                    .collect(Collectors.toList());
            System.out.println("After minDiscount filter: " + products.size() + " products");
        }

        // Xử lý stock (in_stock, out_of_stock)
        if(stock != null) {
            if(stock.equals("in_stock")) {
                products = products.stream().filter(p -> p.getQuantity() > 0).collect(Collectors.toList());
            }
            else if(stock.equals("out_of_stock")) {
                products = products.stream().filter(p -> p.getQuantity() == 0).collect(Collectors.toList());
            }
            System.out.println("After stock filter: " + products.size() + " products");
        }

        // Sắp xếp sản phẩm nếu được chỉ định
        if (sort != null) {
            switch (sort) {
                case "price_low":
                    products.sort((p1, p2) -> Integer.compare(p1.getDiscountedPrice(), p2.getDiscountedPrice()));
                    break;
                case "price_high":
                    products.sort((p1, p2) -> Integer.compare(p2.getDiscountedPrice(), p1.getDiscountedPrice()));
                    break;
                case "discount":
                    products.sort((p1, p2) -> Integer.compare(p2.getDiscountPersent(), p1.getDiscountPersent()));
                    break;
                case "newest":
                    products.sort((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()));
                    break;
                default:
                    // Không sắp xếp
                    break;
            }
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
            productMap.put("discounted_price", p.getDiscountedPrice());
            productMap.put("quantity", p.getQuantity());
            productMap.put("category", p.getCategory() != null ? p.getCategory().getName() : "Uncategorized");
            result.add(productMap);
        }
        return result;
    }

    @Override
    public Map<String, Object> getRevenueByCateogry() {
        // Triển khai phương thức lấy doanh thu theo danh mục
        Map<String, Object> result = new HashMap<>();
        List<Product> allProducts = productRepository.findAll();

        // Nhóm sản phẩm theo danh mục cấp 1
        Map<String, Double> categoryRevenue = new HashMap<>();

        for (Product product : allProducts) {
            String categoryName;
            if (product.getCategory() != null) {
                if (product.getCategory().getLevel() == 2 && product.getCategory().getParentCategory() != null) {
                    // Nếu sản phẩm thuộc category cấp 2, lấy tên của category cấp 1 (parent)
                    categoryName = product.getCategory().getParentCategory().getName();
                } else {
                    // Nếu sản phẩm thuộc category cấp 1
                    categoryName = product.getCategory().getName();
                }
            } else {
                categoryName = "Uncategorized";
            }

            // Tính doanh thu dựa trên giá có giảm giá và số lượng
            Double revenue = categoryRevenue.getOrDefault(categoryName, 0.0);
            revenue += product.getDiscountedPrice() * product.getQuantity();
            categoryRevenue.put(categoryName, revenue);
        }

        result.put("categoryRevenue", categoryRevenue);
        return result;
    }
}