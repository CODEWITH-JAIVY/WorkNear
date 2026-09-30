package com.labourse.customer.dto;

import lombok.Data;

@Data
public class CustomerProfileDto {
    private String name;
    private String profileImageUrl;
    private String addressLine;
    private String city;
    private String pincode;
}
