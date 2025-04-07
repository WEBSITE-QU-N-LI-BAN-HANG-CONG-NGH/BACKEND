package com.ecommerce.controller;

import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.request.CreateProductRequest;
import com.ecommerce.response.ApiResponse;
import com.ecommerce.service.ProductService;
import com.ecommerce.repository.CategoryRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
public class AdminProductController {

    @Autowired
    private ProductService productService;
    
    @Autowired
    private CategoryRepository categoryRepository;

    @PostMapping("/products/create")
    public ResponseEntity<Product> createProduct(@RequestBody CreateProductRequest req) throws GlobalExceptionHandler {
        Product product = productService.createProduct(req);
        return new ResponseEntity<>(product, HttpStatus.CREATED);
    }
    
    
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long productId) throws GlobalExceptionHandler {
        String result = productService.deleteProduct(productId);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/products/all")
    public ResponseEntity<List<Product>> findAllProducts() throws GlobalExceptionHandler {
        List<Product> p = productService.findAllProducts();
        return new ResponseEntity<>(p, HttpStatus.ACCEPTED);
    }

    @PutMapping("/products/{productId}/update")
    public ResponseEntity<Product> updateProduct(@PathVariable Long productId, @RequestBody Product product) throws GlobalExceptionHandler {
        Product p = productService.updateProduct(productId, product);
        return new ResponseEntity<>(p, HttpStatus.OK);
    }

    @PostMapping("/products/create-multiple")
    public ResponseEntity<ApiResponse> createMultipleProducts(@RequestBody CreateProductRequest[] createProductRequests) throws GlobalExceptionHandler {
        for(CreateProductRequest temp: createProductRequests) {
            productService.createProduct(temp);
        }

        ApiResponse res = new ApiResponse();
        res.setStatus(true);
        res.setMessage("Products created successfully");

        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }
}
