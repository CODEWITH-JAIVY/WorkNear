package com.labourse.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class CreateOrderResponse {
    private String razorpayOrderId;
    private double amount;
    private String currency;
    private Long transactionId;
}
