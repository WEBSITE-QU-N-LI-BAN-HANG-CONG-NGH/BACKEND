package com.ecommerce.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "payment_information")
public class PaymentInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Size(min = 16, max = 16)
    @Column(name = "card_number")
    private String cardNumber;

    @NotBlank
    @Size(min = 3, max = 3)
    @Column(name = "cvv")
    private String cvv;

    @NotBlank
    @Size(max = 100)
    @Column(name = "cardholder_name")
    private String cardholderName;

    @NotBlank
    @Size(min = 5, max = 5)
    @Column(name = "expiration_date")
    private String expirationDate;

    public PaymentInformation() {
    }

    public PaymentInformation(Long id, User user, String cardNumber, String cvv, String cardholderName, String expirationDate) {
        this.id = id;
        this.user = user;
        this.cardNumber = cardNumber;
        this.cvv = cvv;
        this.cardholderName = cardholderName;
        this.expirationDate = expirationDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public void setCardholderName(String cardholderName) {
        this.cardholderName = cardholderName;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }
}