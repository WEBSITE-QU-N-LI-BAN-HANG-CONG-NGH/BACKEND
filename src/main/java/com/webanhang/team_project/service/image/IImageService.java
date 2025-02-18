package com.webanhang.team_project.service.image;


import com.webanhang.team_project.dtos.ImageDto;
import com.webanhang.team_project.model.Image;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IImageService {
    Image getImageById(int imageId);
}
