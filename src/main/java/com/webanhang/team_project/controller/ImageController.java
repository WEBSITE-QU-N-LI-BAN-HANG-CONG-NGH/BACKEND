package com.webanhang.team_project.controller;

import com.webanhang.team_project.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("${api.prefix}/images")
@RequiredArgsConstructor
public class ImageController {

    private final CloudinaryService cloudinaryService;

    @PostMapping
    public ResponseEntity<Map> uploadImage(@RequestParam("image") MultipartFile file) throws IOException {
        Map data = this.cloudinaryService.uploadImage(file);
        return new ResponseEntity<>(data, HttpStatus.OK);
    }
}