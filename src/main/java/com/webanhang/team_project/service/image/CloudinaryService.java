package com.webanhang.team_project.service.image;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public Map uploadImage(MultipartFile file) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("folder", "tech_shop");

        return cloudinary.uploader().upload(file.getBytes(), params);
    }

    public Map deleteImage(String publicId) throws IOException {
        return cloudinary.uploader().destroy(publicId, Map.of());
    }
}