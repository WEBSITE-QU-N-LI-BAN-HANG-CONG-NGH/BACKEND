package com.ecommerce.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "address")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must be less than 50 characters")
    @Column(name = "first_name")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must be less than 50 characters")
    @Column(name = "last_name")
    private String lastName;

    @NotBlank(message = "Street address is required")
    @Size(max = 100, message = "Street address must be less than 100 characters")
    @Column(name = "street_address")
    private String streetAddress;

    @NotBlank(message = "City is required")
    @Size(max = 50, message = "City must be less than 50 characters")
    @Column(name = "city")
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 50, message = "State must be less than 50 characters")
    @Column(name = "state")
    private String state;

    @NotBlank(message = "Zip code is required")
    @Size(max = 10, message = "Zip code must be less than 10 characters")
    @Column(name = "zip_code")
    private String zipCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @NotBlank(message = "Mobile number is required")
    @Size(max = 15, message = "Mobile number must be less than 15 characters")
    @Column(name = "mobile")
    private String mobile;

    @OneToMany(mappedBy = "shippingAddress")
    @JsonIgnore
    private List<Order> orders;

    public Address() {
    }

    public Address(String firstName, String lastName, String streetAddress,
                  String city, String state, String zipCode, User user, String mobile) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.streetAddress = streetAddress;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.user = user;
        this.mobile = mobile;
    }
}
