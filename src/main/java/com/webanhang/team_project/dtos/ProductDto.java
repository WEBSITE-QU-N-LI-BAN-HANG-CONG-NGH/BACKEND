package com.webanhang.team_project.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDto {
    private int id;
    private String name;
    private String brand;
    private BigDecimal price;
    private int inventory;
    private String description;
    private List<ImageDto> images;
}
