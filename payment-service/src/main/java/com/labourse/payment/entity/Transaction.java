package com.labourse.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long jobId;
    private Long customerId;
    private Long labourId;

    private double amount;
    private double commission;       // platform cut
    private double labourPayout;     // amount - commission

    private String razorpayOrderId;
    private String razorpayPaymentId;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status = TransactionStatus.CREATED;

    private LocalDateTime createdAt = LocalDateTime.now();
}
