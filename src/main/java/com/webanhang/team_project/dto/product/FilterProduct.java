package com.webanhang.team_project.dto.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class FilterProduct {
    private String topLevelCategory;
    private String secondLevelCategory;
    private String color;
    private Integer minPrice;
    private Integer maxPrice;
    private String sort;


}
