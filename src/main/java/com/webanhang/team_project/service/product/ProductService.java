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
    public List<Product> findProductByCategory(String categoryName) {
        // 1. Tìm category theo tên (có thể cần kiểm tra null)
        Category category = categoryRepository.findByName(categoryName); // Giả sử findByName tồn tại và trả về đúng category

        if (category == null) {
            return new ArrayList<>(); // Hoặc ném lỗi NotFound
        }

        List<Long> categoryIdsToSearch = new ArrayList<>();
        if (category.getLevel() == 1) {
            // Nếu là cấp 1, lấy ID của nó và ID của tất cả con cấp 2
            categoryIdsToSearch.add(category.getId());
            List<Category> subCategories = categoryRepository.findByParentCategoryId(category.getId());
            subCategories.forEach(sub -> categoryIdsToSearch.add(sub.getId()));
        } else if (category.getLevel() == 2) {
            // Nếu là cấp 2, chỉ lấy ID của chính nó
            categoryIdsToSearch.add(category.getId());
        } else {
            // Trường hợp không mong muốn (level khác 1 hoặc 2)
            return new ArrayList<>();
        }

        if (categoryIdsToSearch.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. Gọi repository để tìm sản phẩm theo danh sách ID
        return productRepository.findByCategoryIdIn(categoryIdsToSearch);
    }

    @Override
    public Page<Product> findAllProductsByFilter(
            List<String> colors,
            Integer minPrice, Integer maxPrice, Integer minDiscount,
            String sort,
            Integer pageNumber,
            Integer pageSize) {

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        List<Product> products = productRepository.findAll();
        System.out.println("Found " + products.size() + " total products initially");

        // Lọc theo colors (giữ nguyên)
        if(colors != null && !colors.isEmpty()) {
            List<String> finalColors = colors; // Biến final hoặc effectively final cho lambda
            products = products.stream()
                    .filter(product -> product.getColor() != null && finalColors.stream()
                            .anyMatch(c -> c.equalsIgnoreCase(product.getColor())))
                    .collect(Collectors.toList());
            System.out.println("After color filter: " + products.size() + " products");
        }


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

        if (minDiscount != null) {
            products = products.stream()
                    .filter(p -> p.getDiscountPersent() >= minDiscount)
                    .collect(Collectors.toList());
            System.out.println("After minDiscount filter: " + products.size() + " products");
        }


        if (sort != null) {
            switch (sort) {
                case "price_low":
                    products.sort((p1, p2) -> Integer.compare(p1.getDiscountedPrice(), p2.getDiscountedPrice()));
                    break;
                case "price_high":
                    products.sort((p1, p2) -> Integer.compare(p2.getDiscountedPrice(), p1.getDiscountedPrice()));
                    break;
                case "discount": // Giả sử muốn sắp xếp giảm dần theo % giảm giá
                    products.sort((p1, p2) -> Integer.compare(p2.getDiscountPersent(), p1.getDiscountPersent()));
                    break;
                case "newest":
                    products.sort((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()));
                    break;
                default:
                    // Không sắp xếp hoặc sắp xếp mặc định (ví dụ theo ID)
                    // products.sort((p1, p2) -> p1.getId().compareTo(p2.getId()));
                    break;
            }
            System.out.println("After sorting by " + sort);
        }

        int startIndex = (int) pageable.getOffset();
        int endIndex = Math.min((startIndex + pageable.getPageSize()), products.size());

        List<Product> pageProducts;
        if (startIndex >= products.size()) {
            System.out.println("Returning empty page because startIndex >= filtered products size");
            pageProducts = new ArrayList<>();
        } else {
            pageProducts = products.subList(startIndex, endIndex);
        }
        System.out.println("Returning " + pageProducts.size() + " products on page " + pageNumber + " (Total filtered: " + products.size() + ")");

        // Trả về đối tượng PageImpl chứa dữ liệu trang hiện tại và tổng số phần tử sau khi lọc
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
    public List<Map<String, Object>> getTopSellingProducts(int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Product> products = productRepository.findAll();

        // --- SỬA LỖI SẮP XẾP Ở ĐÂY ---
        products.sort((p1, p2) -> {
            // Lấy giá trị quantitySold, gán mặc định là 0 nếu null
            Long sold1 = (p1.getQuantitySold() != null) ? p1.getQuantitySold() : 0L;
            Long sold2 = (p2.getQuantitySold() != null) ? p2.getQuantitySold() : 0L;

            // So sánh giá trị đã xử lý null (sắp xếp giảm dần nên so sánh sold2 với sold1)
            return Long.compare(sold2, sold1);
        });
        // --- KẾT THÚC SỬA LỖI ---

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
            productMap.put("quantity", p.getQuantity()); // Số lượng tồn kho (từ @Formula)

            // Lấy ảnh đầu tiên làm link (cần kiểm tra null và list rỗng)
            String productLink = null;
            if (p.getImages() != null && !p.getImages().isEmpty() && p.getImages().getFirst() != null) {
                productLink = p.getImages().getFirst().getDownloadUrl();
            }
            productMap.put("product_link", productLink);

            productMap.put("category", p.getCategory() != null ? p.getCategory().getName() : "Uncategorized");
            productMap.put("quantity_sold", p.getQuantitySold()); // Số lượng đã bán
            result.add(productMap);
        }
        return result;
    }

    @Override
    public Map<String, Object> getRevenueByCateogry() {
        Map<String, Object> result = new HashMap<>();
        List<Product> allProducts = productRepository.findAll();
        Map<String, Double> categoryRevenue = new HashMap<>();

        for (Product product : allProducts) {
            String categoryName;
            if (product.getCategory() != null) {
                if (product.getCategory().getLevel() == 2 && product.getCategory().getParentCategory() != null) {
                    categoryName = product.getCategory().getParentCategory().getName();
                } else {
                    categoryName = product.getCategory().getName();
                }
            } else {
                categoryName = "Uncategorized";
            }

            Double revenue = categoryRevenue.getOrDefault(categoryName, 0.0);

            // --- SỬA LỖI Ở ĐÂY ---
            // 1. Lấy giá trị quantitySold và kiểm tra null
            long quantitySoldValue = (product.getQuantitySold() != null) ? product.getQuantitySold() : 0L; // Gán 0 nếu là null

            // 2. Thực hiện phép nhân với giá trị đã được kiểm tra null
            //    (Nên ép kiểu sang double để đảm bảo tính toán doanh thu chính xác)
            revenue += (double) product.getDiscountedPrice() * quantitySoldValue;
            // --- KẾT THÚC SỬA LỖI ---

            categoryRevenue.put(categoryName, revenue);
        }

        result.put("categoryRevenue", categoryRevenue);
        return result;
    }

    @Override
    public List<Product> findByCategoryTopAndSecond(String topCategory, String secondCategory) {
        return productRepository.findProductsByTopAndSecondCategoryNames(topCategory, secondCategory);
    }
}