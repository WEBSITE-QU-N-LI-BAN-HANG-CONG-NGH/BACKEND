package com.ecommerce.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product")
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must be less than 100 characters")
    @Column(name = "title")
    private String title;

    @Size(max = 500, message = "Description must be less than 500 characters")
    @Column(name = "description")
    private String description;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be greater than or equal to 0")
    @Column(name = "price")
    private int price;

    @Min(value = 0, message = "Discount percent must be greater than or equal to 0")
    @Max(value = 100, message = "Discount percent must be less than or equal to 100")
    @Column(name = "discount_persent")
    private int discountPersent;

    @Min(value = 0, message = "Discounted price must be greater than or equal to 0")
    @Column(name = "discounted_price")
    private int discountedPrice;

    @Min(value = 0, message = "Quantity must be greater than or equal to 0")
    @Column(name = "quantity")
    private int quantity;

    @NotBlank(message = "Brand is required")
    @Size(max = 50, message = "Brand must be less than 50 characters")
    @Column(name = "brand")
    private String brand;

    @Size(max = 20, message = "Color must be less than 20 characters")
    @Column(name = "color")
    private String color;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductSize> sizes = new ArrayList<>();

    @Size(max = 255, message = "Image URL must be less than 255 characters")
    @Column(name = "image_url")
    private String imageUrl;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<CartItem> cartItems = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Rating> ratings = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Review> reviews = new ArrayList<>();

    @Min(value = 0, message = "Number of ratings must be greater than or equal to 0")
    @Column(name = "num_rating")
    private int numRating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @JsonIgnore
    private Category category;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Product() {
    }

    public Product(String title, String description, int price, int discountPersent,
                  String brand, String color, String imageUrl, Category category) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.discountPersent = discountPersent;
        this.discountedPrice = price - (price * discountPersent / 100);
        this.brand = brand;
        this.color = color;
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