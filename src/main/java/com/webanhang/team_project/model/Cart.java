package com.webanhang.team_project.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @OneToOne
    @JoinColumn(name="user_id")
    @JsonBackReference
    private User user;

    @Column(name = "total_price")
    private int totalPrice;

    @Column(name = "total_items")
    private int totalItems;

    @Column(name = "total_discounted_price")
    private int totalDiscountedPrice;

    @Column(name = "discount")
    private int discount;

    @OneToMany(mappedBy="cart", cascade=CascadeType.ALL, orphanRemoval=true)
    private Set<CartItem> cartItems = new HashSet<>();

    public int getTotalAmount() {
        return cartItems.stream()
                .mapToInt(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();
    }

    public int getTotal() {
        return totalPrice - totalDiscountedPrice;
    }

}
