package com.webanhang.team_project.dto;

import lombok.Data;

@Data
public class AddAddressRequest {
    private String firstName;
    private String lastName;
    private String streetAddress;
    private String city;
    private String state;
    private String zipCode;
    private String mobile;

}
