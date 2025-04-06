package com.webanhang.team_project.dto.image;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImageDto {
    private int id;
    private String fileName;
    private String downloadUrl;
}
