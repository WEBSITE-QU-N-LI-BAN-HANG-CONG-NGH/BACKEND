package com.webanhang.team_project.dto;

import lombok.Data;

@Data
public class ReviewRequest {
    private Long productId;
    private String content;
}
