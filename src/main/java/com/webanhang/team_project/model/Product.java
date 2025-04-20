package com.webanhang.team_project.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    @Formula("(SELECT COALESCE(SUM(s.quantity), 0) FROM sizes s WHERE s.product_id = id)")
    private int quantity; // Vẫn giữ trường này để lấy giá trị tính toán

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
    @Column(name = "image_urls")
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY) // Quan trọng: FetchType.LAZY
    private List<ProductSize> sizes = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    private List<CartItem> cartItems = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Review> reviews = new ArrayList<>();

    // so luong danh gia
    @Formula("(SELECT COUNT(r.id) FROM review r WHERE r.product_id = id)")
    private int numRating;

    // average rating
    @Formula("(SELECT COALESCE(AVG(r.rating), 0.0) FROM review r WHERE r.product_id = id)") // Dùng 0.0 cho double
    private double averageRating;

//
    @Column(name = "quantity_sold")
    private Long quantitySold;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void updateDiscountedPrice() {
        this.discountedPrice = price - (price * discountPersent / 100);
    }
}
