package com.ecommerce.DTO;

import com.ecommerce.model.Address;
import lombok.Data;

@Data
public class AddressDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String streetAddress;
    private String city;
    private String state;
    private String zipCode;
    private String mobile;

    public AddressDTO(Address address) {
        this.id = address.getId();
        this.firstName = address.getFirstName();
        this.lastName = address.getLastName();
        this.streetAddress = address.getStreetAddress();
        this.city = address.getCity();
        this.state = address.getState();
        this.zipCode = address.getZipCode();
        this.mobile = address.getMobile();
    }
} 