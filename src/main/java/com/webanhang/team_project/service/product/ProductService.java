package com.webanhang.team_project.service.product;

import com.webanhang.team_project.dto.product.*;
import com.webanhang.team_project.model.*;
import com.webanhang.team_project.repository.CategoryRepository;
import com.webanhang.team_project.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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

    @Override
    public List<ProductDTO> getAllProducts() {
        List<Product> products = productRepository.findAll();
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
    public List<Product> findAllProductsByFilter(FilterProduct filter) {
        // IMPROVED: Use a more efficient approach for filtering

        List<Product> filteredProducts;

        // Step 1: First filter by keyword if provided (most restrictive filter first)
        if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
            filteredProducts = productRepository.findByTitleContainingIgnoreCase(filter.getKeyword());
        } else {
            // Step 2: If no keyword, filter by category
            if (filter.getTopLevelCategory() != null && filter.getSecondLevelCategory() != null) {
                filteredProducts = findByCategoryTopAndSecond(filter.getTopLevelCategory(), filter.getSecondLevelCategory());
            } else if (filter.getTopLevelCategory() != null) {
                filteredProducts = findProductByCategory(filter.getTopLevelCategory());
            } else if (filter.getSecondLevelCategory() != null) {
                filteredProducts = findProductByCategory(filter.getSecondLevelCategory());
            } else {
                filteredProducts = findAllProducts();
            }
        }

        // Step 3: Apply additional filters

        // Filter by color
        if (filter.getColor() != null && !filter.getColor().isEmpty()) {
            filteredProducts = filteredProducts.stream()
                    .filter(p -> p.getColor() != null && p.getColor().equalsIgnoreCase(filter.getColor()))
                    .collect(Collectors.toList());
        }

        // Filter by price range
        if (filter.getMinPrice() != null) {
            filteredProducts = filteredProducts.stream()
                    .filter(p -> p.getDiscountedPrice() >= filter.getMinPrice())
                    .collect(Collectors.toList());
        }

        if (filter.getMaxPrice() != null) {
            filteredProducts = filteredProducts.stream()
                    .filter(p -> p.getDiscountedPrice() <= filter.getMaxPrice())
                    .collect(Collectors.toList());
        }

        // Step 4: Sort the results if sort parameter is provided
        if (filter.getSort() != null) {
            switch (filter.getSort()) {
                case "price_low":
                    filteredProducts.sort((p1, p2) -> Integer.compare(p1.getDiscountedPrice(), p2.getDiscountedPrice()));
                    break;
                case "price_high":
                    filteredProducts.sort((p1, p2) -> Integer.compare(p2.getDiscountedPrice(), p1.getDiscountedPrice()));
                    break;
                case "discount":
                    filteredProducts.sort((p1, p2) -> Integer.compare(p2.getDiscountPersent(), p1.getDiscountPersent()));
                    break;
                case "newest":
                    filteredProducts.sort((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()));
                    break;
                default:
                    // Default sorting can be by ID or any other criteria
                    break;
            }
        }

        return filteredProducts;
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
}