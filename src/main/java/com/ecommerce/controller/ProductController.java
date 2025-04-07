package com.ecommerce.controller;

import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.service.ProductService;
import com.ecommerce.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
import java.time.LocalDateTime;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping("/products")
    public ResponseEntity<Page<Product>> findProductsByCategory(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String colorsStr,
            @RequestParam(required = false) String sizesStr,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Integer minDiscount,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String stock,
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @RequestParam(defaultValue = "8") Integer pageSize
    ) throws GlobalExceptionHandler {
        // Chuyển đổi colorsStr thành List<String>
        List<String> colors = new ArrayList<>();
        if (colorsStr != null && !colorsStr.isEmpty()) {
            colors = Arrays.asList(colorsStr.split(","));
        }
        
        // Chuyển đổi sizesStr thành List<String>
        List<String> sizes = new ArrayList<>();
        if (sizesStr != null && !sizesStr.isEmpty()) {
            sizes = Arrays.asList(sizesStr.split(","));
        }
        
        // Ghi log nhận được request
        System.out.println("Request received: category=" + category + ", colors=" + colors + ", sizes=" + sizes);
        
        Page<Product> res = productService.findAllProductsByFilter(category, colors, sizes, 
            minPrice, maxPrice, minDiscount, sort, stock, pageNumber, pageSize);
        return new ResponseEntity<>(res, HttpStatus.ACCEPTED);
    }

    @GetMapping("/products/id/{productId}")
    public ResponseEntity<Product> findProductById(@PathVariable Long productId) throws GlobalExceptionHandler {
        Product res = productService.findProductById(productId);
        return new ResponseEntity<>(res, HttpStatus.ACCEPTED);
    }

    @GetMapping("/products/category/{category}")
    public ResponseEntity<List<Product>> findProductsByCategory(@PathVariable String category) throws GlobalExceptionHandler {
        List<Product> products = productService.findAllProductsByFilter(category, new ArrayList<>(), new ArrayList<>(), 
            null, null, null, null, null, 0, Integer.MAX_VALUE).getContent();
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @GetMapping("/products/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String keyword) throws GlobalExceptionHandler {
        List<Product> products = productService.searchProducts(keyword);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @GetMapping("/products/featured")
    public ResponseEntity<List<Product>> getFeaturedProducts() throws GlobalExceptionHandler {
        List<Product> products = productService.getFeaturedProducts();
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @GetMapping("/products/all-products")
    public ResponseEntity<List<Product>> getAllProductsWithoutFilter() throws GlobalExceptionHandler {
        List<Product> products = productService.findAllProducts();
        System.out.println("Tìm thấy " + products.size() + " sản phẩm trong tổng số không có bộ lọc nào");
        
        // In ra thông tin về 5 sản phẩm đầu tiên để debug
        if (!products.isEmpty()) {
            int count = Math.min(5, products.size());
            for (int i = 0; i < count; i++) {
                Product p = products.get(i);
                System.out.println("Sản phẩm " + (i+1) + ": Id=" + p.getId() + ", Tên=" + p.getTitle() + 
                                  ", Thương hiệu=" + p.getBrand() + ", Danh mục=" + 
                                  (p.getCategory() != null ? p.getCategory().getName() : "null"));
            }
        }
        
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

}
