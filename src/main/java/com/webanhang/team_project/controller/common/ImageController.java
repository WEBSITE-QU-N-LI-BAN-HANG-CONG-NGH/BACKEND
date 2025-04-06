package com.webanhang.team_project.controller.common;


import com.webanhang.team_project.model.Image;
import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.service.image.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/images")
public class    ImageController {
    private final ImageService imageService;

    @GetMapping("/image/{imageId}")
    public ResponseEntity<ApiResponse> getImageById(@PathVariable int imageId) {
        Image image = imageService.getImageById(imageId);
        return ResponseEntity.ok().body(ApiResponse.success(image, "Found!"));
    }
}
