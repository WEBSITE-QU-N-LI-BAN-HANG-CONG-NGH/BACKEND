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
    public ResponseEntity<List<ProductDTO>> findProductsByCategory(
            @RequestParam(required = false) String colorsStr,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Integer minDiscount,
            @RequestParam(required = false) String sort
    ) {
        // Chuyển đổi colorsStr thành List<String>
        List<String> colors = new ArrayList<>();
        if (colorsStr != null && !colorsStr.isEmpty()) {
            colors = Arrays.asList(colorsStr.split(","));
        }

        List<Product> res = productService.findAllProductsByFilter(
                colors,
                minPrice,
                maxPrice,
                minDiscount,
                sort
        );

        List<ProductDTO> productDTOs = res.stream()
                .map(ProductDTO::new)
                .toList();

        return new ResponseEntity<>(productDTOs, HttpStatus.OK);
    }

    @GetMapping("/id/{productId}")
    public ResponseEntity<ProductDTO> findProductById(@PathVariable Long productId) {
        Product res = productService.findProductById(productId);
        ProductDTO productDTO = new ProductDTO(res);
        return new ResponseEntity<>(productDTO, HttpStatus.OK);
    }

    @GetMapping("/{topCategoryName}/{secondCategoryName}")
    public ResponseEntity<List<ProductDTO>> findProductByPath(
            @PathVariable String topCategoryName,
            @PathVariable String secondCategoryName) {
        List<Product> res = productService.findByCategoryTopAndSecond(topCategoryName,secondCategoryName);
        List<ProductDTO> productDTOs = res.stream()
                .map(ProductDTO::new)
                .toList();
        return new ResponseEntity<>(productDTOs, HttpStatus.OK);
    }

    @GetMapping("/{categoryName}")
    public ResponseEntity<List<ProductDTO>> findProductByCategory(
            @PathVariable String categoryName) {
        List<Product> products = productService.findProductByCategory(categoryName);
        List<ProductDTO> productDTOs = products.stream()
                .map(ProductDTO::new)
                .toList();
        return new ResponseEntity<>(productDTOs, HttpStatus.OK);
    }

    @GetMapping("/search/{productName}")
    public ResponseEntity<List<ProductDTO>> searchProducts(@PathVariable String productName) {
        List<Product> products = productService.searchProducts(productName);
        List<ProductDTO> productDTOs = products.stream()
                .map(ProductDTO::new)
                .toList();
        return new ResponseEntity<>(productDTOs, HttpStatus.OK);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProductDTO>> getAllProductsWithoutFilter() {
        List<Product> products = productService.findAllProducts();
        List<ProductDTO> productDTOs = products.stream()
                .map(ProductDTO::new)
                .toList();
        return new ResponseEntity<>(productDTOs, HttpStatus.OK);
    }
}
