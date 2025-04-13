package com.webanhang.team_project.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String color;

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must be less than 100 characters")
    @Column(name = "title")
    private String title;

    @NotBlank(message = "Brand is required")
    @Size(max = 50, message = "Brand must be less than 50 characters")
    @Column(name = "brand")
    private String brand;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be greater than or equal to 0")
    @Column(precision = 19, scale = 2)
    private int price;

    @Min(value = 0, message = "Quantity must be greater than or equal to 0")
    @Column(name = "quantity")
    private int quantity;

    @Size(max = 500, message = "Description must be less than 500 characters")
    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Min(value = 0, message = "Discount percent must be greater than or equal to 0")
    @Max(value = 100, message = "Discount percent must be less than or equal to 100")
    @Column(name = "discount_persent")
    private int discountPersent;

    @Min(value = 0, message = "Discounted price must be greater than or equal to 0")
    @Column(name = "discounted_price")
    private int discountedPrice;

    @Size(max = 255, message = "Image URL must be less than 255 characters")
    @Column(name = "image_url")
    private String imageUrl;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSize> sizes = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    private List<CartItem> cartItems = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Rating> ratings = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Review> reviews = new ArrayList<>();

    @Min(value = 0, message = "Number of ratings must be greater than or equal to 0")
    @Column(name = "num_rating")
    private int numRating;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Product(String title, String brand, int price, int quantity, String description, Category category) {
        this.title = title;
        this.brand = brand;
        this.price = price;
        this.quantity = quantity;
        this.description = description;
        this.category = category;
    }

    public Product(String title, String description, int price, int discountPersent,
                   String brand, String color, String imageUrl, Category category) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.discountPersent = discountPersent;
        this.discountedPrice = price - (price * discountPersent / 100);
        this.brand = brand;
        this.imageUrl = imageUrl;
        this.category = category;
        this.createdAt = LocalDateTime.now();
        this.numRating = 0;
        this.quantity = 0;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void updateDiscountedPrice() {
        this.discountedPrice = price - (price * discountPersent / 100);
    }
}
