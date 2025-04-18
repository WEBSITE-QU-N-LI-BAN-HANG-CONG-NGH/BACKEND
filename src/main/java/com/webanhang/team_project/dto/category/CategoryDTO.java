package com.webanhang.team_project.dto.category;

import lombok.Data;
import java.util.List;

@Data
public class CategoryDTO {
    private Long categoryId;
    private String name;
    private int level;
    private Long parentCategoryId;
    private List<CategoryDTO> subCategories;
} 