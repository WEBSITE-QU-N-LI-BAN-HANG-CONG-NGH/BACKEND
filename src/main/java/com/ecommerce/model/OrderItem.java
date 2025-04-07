package com.ecommerce.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_item")
@Data
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity")
    private int quantity;

    @Column(name = "price")
    private int price;

    @Column(name = "size")
    private String size;

    @Column(name = "discounted_price")
    private Integer discountedPrice;

    @Column(name = "delivery_date")
    private LocalDateTime deliveryDate;

    @Column(name = "discount_percent")
    private Integer discountPercent;

    // Helper method to get user through order
    public User getUser() {
        return order != null ? order.getUser() : null;
    }
}