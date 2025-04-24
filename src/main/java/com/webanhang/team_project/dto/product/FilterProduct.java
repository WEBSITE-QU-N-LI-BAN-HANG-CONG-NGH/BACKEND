package com.webanhang.team_project.dto.product;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FilterProduct {
    private String color;
    private Integer minPrice;
    private Integer maxPrice;
    private Integer minDiscount;
    private String sort;

    public FilterProduct(String color, Integer minPrice, Integer maxPrice, Integer minDiscount, String sort) {
        this.color = color;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.minDiscount = minDiscount;
        this.sort = sort;
    }
}
