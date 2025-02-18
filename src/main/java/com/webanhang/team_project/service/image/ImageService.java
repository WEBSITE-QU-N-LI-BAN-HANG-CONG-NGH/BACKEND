package com.webanhang.team_project.service.image;


import com.webanhang.team_project.dtos.ImageDto;
import com.webanhang.team_project.model.Image;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.repository.ImageRepository;
import com.webanhang.team_project.service.product.IProductService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageService implements IImageService {
    private final ImageRepository imageRepository;
    private final IProductService productService;

    @Override
    public Image getImageById(int imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Image not found"));
    }
}
