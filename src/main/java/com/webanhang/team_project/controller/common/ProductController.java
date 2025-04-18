package com.webanhang.team_project.controller.common;

import com.webanhang.team_project.dto.product.ProductDTO;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.service.product.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/products")
public class ProductController {

    private final IProductService productService;
//    private CategoryRepository categoryRepository;

    @GetMapping("/")
    public ResponseEntity<Page<Product>> findProductsByCategory(
            @RequestParam(required = false) String colorsStr,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Integer minDiscount,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @RequestParam(defaultValue = "8") Integer pageSize
    ) {
        // Chuyển đổi colorsStr thành List<String>
        List<String> colors = new ArrayList<>();
        if (colorsStr != null && !colorsStr.isEmpty()) {
            colors = Arrays.asList(colorsStr.split(","));
        }

        Page<Product> res = productService.findAllProductsByFilter(
                colors,
                minPrice,
                maxPrice,
                minDiscount,
                sort,
                pageNumber,
                pageSize);

        return new ResponseEntity<>(res, HttpStatus.ACCEPTED);
    }

    @GetMapping("/id/{productId}")
    public ResponseEntity<ProductDTO> findProductById(@PathVariable Long productId) {
        Product res = productService.findProductById(productId);
        ProductDTO productDTO = new ProductDTO(res);
        return new ResponseEntity<>(productDTO, HttpStatus.OK);
    }

    @GetMapping("/{topCategoryName}/{secondCategoryName}")
    public ResponseEntity<List<Product>> findProductByPath(
            @PathVariable String topCategoryName,
            @PathVariable String secondCategoryName) {
        List<Product> res = productService.findByCategoryTopAndSecond(topCategoryName,secondCategoryName);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @GetMapping("/{categoryName}")
    public ResponseEntity<List<Product>> findProductByCategory(
            @PathVariable String categoryName) {
        List<Product> products = productService.findProductByCategory(categoryName);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @GetMapping("/search/{productName}")
    public ResponseEntity<List<Product>> searchProducts(@PathVariable String productName) {
        List<Product> products = productService.searchProducts(productName);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Product>> getAllProductsWithoutFilter() {
        List<Product> products = productService.findAllProducts();
        return new ResponseEntity<>(products, HttpStatus.OK);
    }
}
