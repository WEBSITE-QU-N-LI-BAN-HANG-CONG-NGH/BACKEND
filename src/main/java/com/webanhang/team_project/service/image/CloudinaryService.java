package com.webanhang.team_project.service.image;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private static final String DEFAULT_FOLDER = "tech_shop";

    public Map<String, Object> uploadImage(MultipartFile file) throws IOException {

        Map<String, Object> params = new HashMap<>();
        params.put("folder", "tech_shop");
        params.put("resource_type", "auto");
        params.put("unique_filename", true);

        try {
            return cloudinary.uploader().upload(file.getBytes(), params);
        } catch (IOException e) {
            log.error("Lỗi khi tải hình ảnh lên Cloudinary", e);
            throw e;
        }
    }

    public Map deleteImage(String publicId) throws IOException {
        try {
            return cloudinary.uploader().destroy(publicId, Map.of());
        } catch (IOException e) {
            log.error("Lỗi khi xóa hình ảnh từ Cloudinary: " + publicId, e);
            throw e;
        }
    }

    /**
     * Trích xuất publicId từ URL ảnh Cloudinary.
     * @param imageUrl URL đầy đủ của ảnh trên Cloudinary
     * @return publicId (để dùng cho xoá ảnh, tạo lại URL, v.v.), hoặc null nếu không hợp lệ
     */
    public String extractPublicIdFromUrl(String imageUrl) {
        try {
            if (imageUrl == null || !imageUrl.contains("/upload/")) {
                return null;
            }

            String[] parts = imageUrl.split("/upload/");
            if (parts.length < 2) return null;

            String afterUpload = parts[1];
            // Lấy phần trước dấu chấm cuối cùng (loại bỏ định dạng file như .jpg, .png)
            int lastDotIndex = afterUpload.lastIndexOf('.');
            String publicId = lastDotIndex > 0 ? afterUpload.substring(0, lastDotIndex) : afterUpload;

            return publicId;
        } catch (Exception e) {
            log.error("Lỗi khi trích xuất publicId từ URL: " + imageUrl, e);
            return null;
        }
    }

    /**
     * Tạo URL hình ảnh từ Cloudinary với các tùy chọn biến đổi kích thước, crop, chất lượng,...
     *
     * @param publicId ID công khai của ảnh trên Cloudinary
     * @param width    Chiều rộng mong muốn của ảnh
     * @param height   Chiều cao mong muốn của ảnh
     * @param crop     Kiểu cắt ảnh (ví dụ: "fill", "crop", "scale", "fit", v.v.)
     * @return URL ảnh đã được biến đổi, hoặc null nếu xảy ra lỗi
     */
    public String generateUrl(String publicId, int width, int height, String crop) {
        try {
            Map<String, String> options = new HashMap<>();
            options.put("width", String.valueOf(width));
            options.put("height", String.valueOf(height));
            options.put("crop", crop); // fill, crop, scale, etc.
            options.put("quality", "auto");
            options.put("fetch_format", "auto");

            return cloudinary.url().transformation(new Transformation().params(options)).generate(publicId);
        } catch (Exception e) {
            log.error("Lỗi khi tạo URL hình ảnh từ Cloudinary", e);
            return null;
        }
    }

}