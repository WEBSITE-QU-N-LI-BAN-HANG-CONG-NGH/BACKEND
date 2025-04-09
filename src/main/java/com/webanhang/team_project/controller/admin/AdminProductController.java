package com.webanhang.team_project.controller.admin;


import com.ecommerce.request.CreateProductRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;

    @PostMapping("/products/create")
    public ResponseEntity<Product> createProduct(@RequestBody CreateProductRequest req) {
        Product product = productService.createProduct(req);
        return new ResponseEntity<>(product, HttpStatus.CREATED);
    }
    
    
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long productId) {
        String result = productService.deleteProduct(productId);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/products/all")
    public ResponseEntity<List<Product>> findAllProducts() {
        List<Product> p = productService.findAllProducts();
        return new ResponseEntity<>(p, HttpStatus.ACCEPTED);
    }

    @PutMapping("/products/{productId}/update")
    public ResponseEntity<Product> updateProduct(@PathVariable Long productId, @RequestBody Product product)  {
        Product p = productService.updateProduct(productId, product);
        return new ResponseEntity<>(p, HttpStatus.OK);
    }

    @PostMapping("/products/create-multiple")
    public ResponseEntity<ApiResponse> createMultipleProducts(@RequestBody CreateProductRequest[] createProductRequests) {
        for(CreateProductRequest temp: createProductRequests) {
            productService.createProduct(temp);
        }

        ApiResponse res = new ApiResponse();
        res.setStatus(true);
        res.setMessage("Products created successfully");

        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }
}
