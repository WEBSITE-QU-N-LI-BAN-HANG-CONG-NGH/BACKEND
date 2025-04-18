package com.webanhang.team_project.dto.image;

import com.webanhang.team_project.model.Image;
import lombok.Data;

@Data
public class ImageDTO {
    private Long id;
    private String fileName;
    private String fileType;
    private String downloadUrl;

    public ImageDTO(Image image) {
        this.id = image.getId();
        this.fileName = image.getFileName();
        this.fileType = image.getFileType();
        this.downloadUrl = image.getDownloadUrl();
    }
}