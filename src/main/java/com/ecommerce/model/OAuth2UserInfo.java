package com.ecommerce.model;

import lombok.Data;

@Data
public class OAuth2UserInfo {
    private String id;
    private String name;
    private String email;
    private String imageUrl;
    private String provider;

    public OAuth2UserInfo(String id, String name, String email, String imageUrl, String provider) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.imageUrl = imageUrl;
        this.provider = provider;
    }
} 