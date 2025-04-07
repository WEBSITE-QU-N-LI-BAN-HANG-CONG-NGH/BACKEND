package com.ecommerce.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cart")
@Data
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<CartItem> cartItems = new ArrayList<>();

    @Column(name = "total_price")
    private int totalPrice;

    @Column(name = "total_items")
    private int totalItems;

    @Column(name = "total_discounted_price")
    private int totalDiscountedPrice;

    @Column(name = "discount")
    private int discount;

    public Cart() {
    }

    public Cart(Long id, User user, List<CartItem> cartItems, int totalPrice, 
                int totalItems, int totalDiscountedPrice, int discount) {
        this.id = id;
        this.user = user;
        this.cartItems = cartItems;
        this.totalPrice = totalPrice;
        this.totalItems = totalItems;
        this.totalDiscountedPrice = totalDiscountedPrice;
        this.discount = discount;
    }

    public int getTotalAmount() {
        return cartItems.stream()
                .mapToInt(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();
    }

    public int getTotal() {
        return totalPrice - totalDiscountedPrice;
    }
}
