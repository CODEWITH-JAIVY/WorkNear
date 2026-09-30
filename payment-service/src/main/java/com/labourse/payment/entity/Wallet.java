package com.labourse.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// One wallet per labour — holds earnings pending payout after commission cut
@Entity
@Table(name = "wallets")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long labourId;

    private double balance = 0.0;
    private double lifetimeEarnings = 0.0;
}
