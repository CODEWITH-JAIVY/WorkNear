package com.labourse.catalog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Top-level grouping shown as the first screen when posting a job ("What service do you need?").
// code is a stable natural key (e.g. "HOME_REPAIR") — labour-service and job-service reference
// this code as a plain string, never a numeric FK, same no-cross-service-join rule as everywhere
// else in this codebase.
@Entity
@Table(name = "service_category")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class ServiceCategory {

    @Id
    @Column(length = 50)
    private String code;

    private String name; // includes the emoji, e.g. "🏠 Home Repair & Maintenance"
    private int displayOrder;
}
