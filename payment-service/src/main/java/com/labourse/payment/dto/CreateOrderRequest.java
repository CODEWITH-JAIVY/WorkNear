package com.labourse.payment.dto;

import lombok.Data;

@Data
public class CreateOrderRequest {
    private Long jobId;
    private Long customerId;
    private Long labourId;
    private double amount; // INR
}
