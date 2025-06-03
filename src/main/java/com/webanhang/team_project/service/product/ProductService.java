package com.webanhang.team_project.service.product;

import com.webanhang.team_project.dto.product.*;
import com.webanhang.team_project.model.*;
import com.webanhang.team_project.repository.*;
import com.webanhang.team_project.service.image.ImageService;
import com.webanhang.team_project.service.order.OrderService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    private final ModelMapper modelMapper;
    private final CartItemRepository cartItemRepository;
    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ImageService imageService;

    @Override
    @Transactional
    public Product createProduct(CreateProductRequest req) {
        // Existing code for product creation...
        // No changes needed

        // Logic for handling categories
        Category parentCategory = null;
        Category category = null;

        // Handle top level category
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

            // Handle second level category if provided
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
                // If no second level, use top level
                category = parentCategory;
            }
        }

        // Create the product
        Product product = new Product();
        product.setTitle(req.getTitle());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setDiscountPersent(req.getDiscountPersent());
        product.setBrand(req.getBrand());
        product.setColor(req.getColor());
        product.setWeight(req.getWeight());
        product.setDimension(req.getDimension());
        product.setBatteryType(req.getBatteryType());
        product.setBatteryCapacity(req.getBatteryCapacity());
        product.setRamCapacity(req.getRamCapacity());
        product.setRomCapacity(req.getRomCapacity());
        product.setScreenSize(req.getScreenSize());
        product.setDetailedReview(req.getDetailedReview());
        product.setPowerfulPerformance(req.getPowerfulPerformance());
        product.setConnectionPort(req.getConnectionPort());
        product.setCreatedAt(LocalDateTime.now());
        product.setQuantity(req.getQuantity());
        product.setCategory(category);
        product.setSellerId(req.getSellerId());

        // Update discounted price
        product.updateDiscountedPrice();

        // Handle sizes if provided
        if (req.getSizes() != null) {
            for (ProductSize size : req.getSizes()) {
                size.setProduct(product);
            }
            product.setSizes(req.getSizes());
        }

        // Handle images if provided
        if (req.getImageUrls() != null && !req.getImageUrls().isEmpty()) {
            for (Image imageUrl : req.getImageUrls()) {
                imageUrl.setProduct(product);
            }
            product.setImages(req.getImageUrls());
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
    public Product findProductById(Long id) {
        Optional<Product> product = productRepository.findById(id);
        if(product.isPresent()) {
            return product.get();
        }
        throw new EntityNotFoundException("Product not found with id: " + id);
    }

    @Override
    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }

    @Transactional
    @Override
    public List<ProductDTO> getAllProducts(String search, String categoryName, String sort, String order) {
        List<Product> products = productRepository.findAll();

        if (search != null && !search.isEmpty()) {
            products = productRepository.searchProducts(search);
        } else if (categoryName != null && !categoryName.isEmpty()) {
            Category category = categoryRepository.findByName(categoryName);

            if (category != null) {
                products = productRepository.findByCategory(category);
            } else {
                products = new ArrayList<>();
            }
            products = productRepository.findByCategory(category);
        } else {
            products = productRepository.findAll();
        }

        if (sort != null && order != null) {
            sortProducts(products, sort, order);
        }

        List<ProductDTO> productDTOs = products.stream()
                .map(ProductDTO::new)
                .toList();
        return productDTOs;
    }

    @Override
    public List<Product> searchProducts(String keyword) {
        return productRepository.findByTitleContainingIgnoreCase(keyword);
    }

    @Override
    public List<Product> findProductByCategory(String categoryName) {
        Category category = categoryRepository.findByName(categoryName);
        if (category == null) {
            return new ArrayList<>();
        }

        List<Long> categoryIdsToSearch = new ArrayList<>();
        if (category.getLevel() == 1) {
            // If level 1, get its ID and all its level 2 children
            categoryIdsToSearch.add(category.getId());
            List<Category> subCategories = categoryRepository.findByParentCategoryId(category.getId());
            subCategories.forEach(sub -> categoryIdsToSearch.add(sub.getId()));
        } else if (category.getLevel() == 2) {
            // If level 2, just get its own ID
            categoryIdsToSearch.add(category.getId());
        } else {
            return new ArrayList<>();
        }

        if (categoryIdsToSearch.isEmpty()) {
            return new ArrayList<>();
        }

        return productRepository.findByCategoryIdIn(categoryIdsToSearch);
    }

    @Override
    public List<Product> findByCategoryTopAndSecond(String topCategory, String secondCategory) {
        return productRepository.findProductsByTopAndSecondCategoryNames(topCategory, secondCategory);
    }

    @Override
    public Page<ProductDTO> getProductsWithFilter(Pageable pageable, FilterProduct filter, String status) {

        Boolean inStock = null;
        if (status != null && !status.equals("all")) {
            switch (status) {
                case "inStock":
                    inStock = true;
                    break;
                case "outOfStock":
                    inStock = false;
                    break;
            }
        }

        // Add debug logging
        System.out.println("Filter params:");
        System.out.println("- status: " + status);
        System.out.println("- inStock: " + inStock);
        if (filter != null) {
            System.out.println("- keyword: " + filter.getKeyword());
            System.out.println("- topLevelCategory: " + filter.getTopLevelCategory());
        }

        // Apply custom sorting if specified in filter
        Pageable finalPageable = pageable;
        if (filter != null && filter.getSort() != null && !filter.getSort().isEmpty()) {
            finalPageable = applySorting(pageable, filter.getSort());
        }

        Page<Product> productPage = productRepository.getProductsWithFilter(
                filter != null ? filter.getKeyword() : null,
                filter != null ? filter.getTopLevelCategory() : null,
                filter != null ? filter.getSecondLevelCategory() : null,
                filter != null ? filter.getColor() : null,
                filter != null ? filter.getMinPrice() : null,
                filter != null ? filter.getMaxPrice() : null,
                inStock,
                finalPageable
        );

        System.out.println("Query returned " + productPage.getTotalElements() + " products");

        return productPage.map(ProductDTO::new);
    }

    @Override
    public Map<String, Object> getAdminFilterStatistics() {
        List<Product> allProducts = productRepository.findAll();

        Map<String, Object> stats = new HashMap<>();

        // Price range
        OptionalInt minPrice = allProducts.stream()
                .filter(p -> p.getDiscountedPrice() > 0)
                .mapToInt(Product::getDiscountedPrice)
                .min();
        OptionalInt maxPrice = allProducts.stream()
                .mapToInt(Product::getDiscountedPrice)
                .max();

        stats.put("priceRange", Map.of(
                "min", minPrice.orElse(0),
                "max", maxPrice.orElse(0)
        ));

        // Stock status counts
        long inStockCount = allProducts.stream().filter(p -> p.getQuantity() > 0).count();
        long outOfStockCount = allProducts.size() - inStockCount;

        stats.put("stockStatus", Map.of(
                "inStock", inStockCount,
                "outOfStock", outOfStockCount,
                "total", allProducts.size()
        ));

        // Available colors
        List<String> colors = allProducts.stream()
                .map(Product::getColor)
                .filter(color -> color != null && !color.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        stats.put("colors", colors);

        // Categories
        stats.put("categories", getAllCategories());

        return stats;
    }

    @Override
    public Map<String, Object> getAllCategories() {
        Map<String, Object> categoriesMap = new HashMap<>();

        // Get top-level categories
        List<String> topLevelCategories = productRepository.findDistinctTopLevelCategories();
        categoriesMap.put("topLevel", topLevelCategories);

        // Get second-level categories grouped by top-level
        Map<String, List<String>> secondLevelByTopLevel = new HashMap<>();
        for (String topLevel : topLevelCategories) {
            List<String> secondLevel = productRepository.findDistinctSecondLevelCategoriesByTopLevel(topLevel);
            if (!secondLevel.isEmpty()) {
                secondLevelByTopLevel.put(topLevel, secondLevel);
            }
        }
        categoriesMap.put("secondLevel", secondLevelByTopLevel);

        return categoriesMap;
    }

    @Override
    @Transactional
    public void adminDeleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        // 1. Remove from active carts first
        List<CartItem> cartItems = cartItemRepository.findByProductId(productId);
        if (!cartItems.isEmpty()) {
            cartItemRepository.deleteAll(cartItems);
        }

        // 2. Delete all reviews for this product
        List<Review> reviews = reviewRepository.findAllByProductId(productId);
        if (!reviews.isEmpty()) {
            reviewRepository.deleteAll(reviews);
        }

        // 3. Delete order items completely (loses order history)
        List<OrderItem> orderItems = orderItemRepository.findByProductId(productId);
        if (!orderItems.isEmpty()) {
            orderItemRepository.deleteAll(orderItems);
        }

        // 4. Delete product images
        imageService.deleteAllProductImages(productId);

        // 5. Finally delete the product
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    @Override
    public List<Product> findAllProductsByFilter(FilterProduct filter) {
        Specification<Product> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by keyword (title)
            if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), "%" + filter.getKeyword().toLowerCase() + "%"));
            }

            // Filter by color
            if (filter.getColor() != null && !filter.getColor().isEmpty()) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("color")), filter.getColor().toLowerCase()));
            }

            // Filter by price range
            if (filter.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("discountedPrice"), filter.getMinPrice()));
            }
            if (filter.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("discountedPrice"), filter.getMaxPrice()));
            }

            // Filter by category
            if (filter.getTopLevelCategory() != null && !filter.getTopLevelCategory().isEmpty()) {
                Join<Product, Category> categoryJoin = root.join("category", JoinType.LEFT); // Join với bảng Category
                Join<Category, Category> parentCategoryJoin = categoryJoin.join("parentCategory", JoinType.LEFT); // Join với parentCategory

                if (filter.getSecondLevelCategory() != null && !filter.getSecondLevelCategory().isEmpty()) {
                    // Lọc theo cả top-level và second-level category
                    predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(parentCategoryJoin.get("name")), filter.getTopLevelCategory().toLowerCase()));
                    predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(categoryJoin.get("name")), filter.getSecondLevelCategory().toLowerCase()));
                    predicates.add(criteriaBuilder.equal(categoryJoin.get("level"), 2)); // Đảm bảo category là level 2
                } else {
                    // Chỉ lọc theo top-level category (bao gồm cả sản phẩm thuộc category level 1 đó và các sub-category level 2 của nó)
                    Predicate topLevelDirect = criteriaBuilder.and(
                            criteriaBuilder.equal(criteriaBuilder.lower(categoryJoin.get("name")), filter.getTopLevelCategory().toLowerCase()),
                            criteriaBuilder.equal(categoryJoin.get("level"), 1)
                    );
                    Predicate topLevelViaParent = criteriaBuilder.and(
                            criteriaBuilder.equal(criteriaBuilder.lower(parentCategoryJoin.get("name")), filter.getTopLevelCategory().toLowerCase()),
                            criteriaBuilder.equal(categoryJoin.get("level"), 2)
                    );
                    predicates.add(criteriaBuilder.or(topLevelDirect, topLevelViaParent));
                }
            } else if (filter.getSecondLevelCategory() != null && !filter.getSecondLevelCategory().isEmpty()) {
                // Trường hợp chỉ cung cấp secondLevelCategory (ít phổ biến, nhưng vẫn xử lý)
                Join<Product, Category> categoryJoin = root.join("category", JoinType.LEFT);
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(categoryJoin.get("name")), filter.getSecondLevelCategory().toLowerCase()));
                predicates.add(criteriaBuilder.equal(categoryJoin.get("level"), 2));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.unsorted();
        if (filter.getSort() != null && !filter.getSort().isEmpty()) {
            switch (filter.getSort().toLowerCase()) {
                case "price_low":
                    sort = Sort.by(Sort.Direction.ASC, "discountedPrice");
                    break;
                case "price_high":
                    sort = Sort.by(Sort.Direction.DESC, "discountedPrice");
                    break;
                case "discount":
                    sort = Sort.by(Sort.Direction.DESC, "discountPersent"); // Giả sử tên trường là "discountPersent"
                    break;
                case "newest":
                    sort = Sort.by(Sort.Direction.DESC, "createdAt");
                    break;
                // Có thể thêm các trường hợp sort khác nếu cần
            }
        }

        return productRepository.findAll(spec, sort);
    }

    @Override
    public List<Map<String, Object>> getTopSellingProducts(int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        Pageable pageable = PageRequest.of(0, limit);
        List<Product> products = productRepository.findTopSellingProducts(pageable);

        for (Product p : products) {
            result.add(mapProductToMap(p));
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
            long quantitySoldValue = (product.getQuantitySold() != null) ? product.getQuantitySold() : 0L;
            revenue += (double) product.getDiscountedPrice() * quantitySoldValue;
            categoryRevenue.put(categoryName, revenue);
        }

        result.put("categoryRevenue", categoryRevenue);
        return result;
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, Product product) {
        Product existingProduct = findProductById(id);

        // Update basic properties
        if (product.getTitle() != null) existingProduct.setTitle(product.getTitle());
        if (product.getDescription() != null) existingProduct.setDescription(product.getDescription());
        if (product.getBrand() != null) existingProduct.setBrand(product.getBrand());
        if (product.getColor() != null) existingProduct.setColor(product.getColor());

        // Handle images
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            existingProduct.getImages().clear();
            for (Image image : product.getImages()) {
                image.setProduct(existingProduct);
                existingProduct.getImages().add(image);
            }
        }

        // Update price and discount
        if (product.getPrice() > 0) existingProduct.setPrice(product.getPrice());
        if (product.getDiscountPersent() >= 0) existingProduct.setDiscountPersent(product.getDiscountPersent());
        existingProduct.updateDiscountedPrice();

        // Update quantity
        if (product.getQuantity() >= 0) existingProduct.setQuantity(product.getQuantity());

        // Update category if provided
        if (product.getCategory() != null && product.getCategory().getId() != null) {
            Category category = categoryRepository.findById(product.getCategory().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found"));
            existingProduct.setCategory(category);
        }

        return productRepository.save(existingProduct);
    }

    @Transactional
    @Override
    public ProductDTO updateProductByID(Long productId, Product product) {
        Product curProduct = findProductById(productId);

        // Update basic properties
        if (product.getTitle() != null) curProduct.setTitle(product.getTitle());
        if (product.getDescription() != null) curProduct.setDescription(product.getDescription());
        if (product.getBrand() != null) curProduct.setBrand(product.getBrand());
        if (product.getColor() != null) curProduct.setColor(product.getColor());

        // Handle images
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            curProduct.getImages().clear();
            for (Image image : product.getImages()) {
                image.setProduct(curProduct);
                curProduct.getImages().add(image);
            }
        }

        // Update price and discount
        if (product.getPrice() > 0) curProduct.setPrice(product.getPrice());
        if (product.getDiscountPersent() >= 0) curProduct.setDiscountPersent(product.getDiscountPersent());
        curProduct.updateDiscountedPrice();

        // Update quantity
        if (product.getQuantity() >= 0) curProduct.setQuantity(product.getQuantity());

        // Update category if provided
        if (product.getCategory() != null && product.getCategory().getId() != null) {
            Category category = categoryRepository.findById(product.getCategory().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found"));
            curProduct.setCategory(category);
        }
        //save new info
        Product updatedProduct = productRepository.save(curProduct);
        return modelMapper.map(updatedProduct, ProductDTO.class);
    }

    private Map<String, Object> mapProductToMap(Product p) {
        Map<String, Object> productMap = new HashMap<>();
        productMap.put("id", p.getId());
        productMap.put("title", p.getTitle());
        productMap.put("brand", p.getBrand());
        productMap.put("price", p.getPrice());
        productMap.put("discounted_price", p.getDiscountedPrice());
        productMap.put("quantity", p.getQuantity());
        productMap.put("category", p.getCategory() != null ? p.getCategory().getName() : "Uncategorized");
        productMap.put("quantity_sold", p.getQuantitySold());
        return productMap;
    }

    // helper
    private void sortProducts(List<Product> products, String sortBy, String order) {
        Comparator<Product> comparator = null;

        switch (sortBy) {
            case "price":
                comparator = Comparator.comparing(Product::getPrice);
                break;
            case "createdAt":
                comparator = Comparator.comparing(Product::getCreatedAt);
                break;
            case "quantitySold":
                comparator = Comparator.comparing(Product::getQuantitySold);
                break;
            case "quantity":
                comparator = Comparator.comparing(Product::getQuantity);
                break;
            default:
                return;
        }

        if ("desc".equalsIgnoreCase(order)) {
            comparator = comparator.reversed();
        }

        products.sort(comparator);
    }

    // helper methods
    private Pageable applySorting(Pageable pageable, String sortType) {
        Sort sort;
        switch (sortType) {
            case "price_low":
                sort = Sort.by(Sort.Direction.ASC, "discountedPrice");
                break;
            case "price_high":
                sort = Sort.by(Sort.Direction.DESC, "discountedPrice");
                break;
            case "discount":
                sort = Sort.by(Sort.Direction.DESC, "discountPersent");
                break;
            case "newest":
                sort = Sort.by(Sort.Direction.DESC, "createdAt");
                break;
            case "name_asc":
                sort = Sort.by(Sort.Direction.ASC, "title");
                break;
            case "name_desc":
                sort = Sort.by(Sort.Direction.DESC, "title");
                break;
            default:
                return pageable; // Keep original sorting
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

}