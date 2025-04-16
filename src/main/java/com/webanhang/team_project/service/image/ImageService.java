package com.webanhang.team_project.service.image;

import com.webanhang.team_project.model.Image;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageService implements IImageService{
    private final ImageRepository imageRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    public Image uploadImageForProduct(MultipartFile file, Product product) throws IOException {
        Map uploadResult = cloudinaryService.uploadImage(file);

        Image image = new Image();
        image.setProduct(product);
        image.setFileName(file.getOriginalFilename());
        image.setFileType(file.getContentType());
        image.setDownloadUrl((String) uploadResult.get("url"));

        return imageRepository.save(image);
    }

    @Override
    @Transactional
    public void deleteImage(Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Hình ảnh không tồn tại"));

        // Xóa khỏi Cloudinary
        String publicId = extractPublicIdFromUrl(image.getDownloadUrl());
        if (publicId != null) {
            try {
                cloudinaryService.deleteImage(publicId);
            } catch (Exception e) {
                System.err.println("Không thể xóa hình ảnh từ Cloudinary: " + e.getMessage());
            }
        }
        imageRepository.delete(image);
    }

    public String extractPublicIdFromUrl(String imageUrl) {
        // URL Cloudinary có dạng: https://res.cloudinary.com/your-cloud-name/image/upload/v1234567890/folder/filename.jpg
        try {
            if (imageUrl == null || !imageUrl.contains("/upload/")) {
                return null;
            }

            String afterUpload = imageUrl.split("/upload/")[1];
            // Loại bỏ phần mở rộng tệp (.jpg, .png, ...)
            String publicId = afterUpload.substring(0, afterUpload.lastIndexOf('.'));

            return publicId;
        } catch (Exception e) {
            return null;
        }
    }
}
