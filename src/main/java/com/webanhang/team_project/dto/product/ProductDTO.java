package com.webanhang.team_project.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDTO {
    private Long id;
    private String title;
    private String description;
    private int price;
    private int discountedPrice;
    private int quantity;
    private String brand;
    private String color;
    private List<String> sizes;
//    private String imageUrl;
    private List<String> imageUrls;
    private int averageRating;
    private int numRatings;
}
