package com.webanhang.team_project.model;

import com.webanhang.team_project.enums.PaymentMethod;
import com.webanhang.team_project.enums.PaymentStatus;
import com.webanhang.team_project.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@Table(name="orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @Column(name="order_date")
    private LocalDateTime orderDate;

    @Column(name="total_amount", precision = 19, scale = 2)
    private int totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name="order_status")
    private OrderStatus orderStatus;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Collection<OrderItem> orderItems = new HashSet<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private PaymentDetail paymentDetails;

    @ManyToOne
    @JoinColumn(name = "orderAddress", unique = false)
    private Address shippingAddress;

    @Column(name = "delivery_date")
    private LocalDateTime deliveryDate;

    @Column(name = "total_discounted_price")
    private Integer totalDiscountedPrice;

    @Column(name = "discount")
    private int discount;

    @Column(name = "total_items")
    private int totalItems;

    @Enumerated(EnumType.STRING) // Vẫn cần thiết
    @Column(name = "payment_method")
    @ColumnDefault("'COD'") // <-- Đặt giá trị mặc định dạng chuỗi SQL
    private PaymentMethod paymentMethod;

    @Column(name = "payment_status")
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
}
