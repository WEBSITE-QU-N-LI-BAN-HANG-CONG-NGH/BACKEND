package com.webanhang.team_project.controller;

import com.webanhang.team_project.model.Image;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.service.image.CloudinaryService;
import com.webanhang.team_project.service.image.ImageService;
import com.webanhang.team_project.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("${api.prefix}/images")
@RequiredArgsConstructor
public class ImageController {
    private final ImageService imageService;
    private final CloudinaryService cloudinaryService;
    private final ProductService productService;

    @PostMapping("{productId}")
    public ResponseEntity<?> uploadImage(
            @PathVariable Long productId,
            @RequestParam("image") MultipartFile file) throws IOException {

        Product product = productService.findProductById(productId);
        Image image = imageService.uploadImageForProduct(file, product);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "imageId", image.getId(),
                "url", image.getDownloadUrl()
        ));
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<?> deleteImage(@PathVariable Long imageId) {
        try {
            imageService.deleteImage(imageId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Xóa hình ảnh thành công"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Không thể xóa hình ảnh: " + e.getMessage()
                    ));
        }
    }
}