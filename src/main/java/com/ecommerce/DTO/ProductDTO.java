package com.ecommerce.DTO;

import lombok.Data;

import java.util.List;

@Data
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
    private String imageUrl;
    private int averageRating;
    private int numRatings;
}
