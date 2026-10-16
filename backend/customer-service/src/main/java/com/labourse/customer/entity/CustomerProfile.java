package com.labourse.customer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customer_profiles")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class CustomerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;   // FK to auth-service User.id — resolved via event, not a join

    private String name;
    private String profileImageUrl;

    private String addressLine;
    private String city;
    private String pincode;

    private Double lastKnownLat;
    private Double lastKnownLon;
}
