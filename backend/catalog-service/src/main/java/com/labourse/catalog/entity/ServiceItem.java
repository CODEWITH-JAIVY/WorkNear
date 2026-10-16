package com.labourse.catalog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// A leaf, bookable service — e.g. "Electrician", "Tap Repair", "Full House Cleaning". This is
// the actual unit a job gets posted against and a labour selects onto their profile; category
// and groupLabel exist purely for UI organisation (category screen -> grouped sections).
@Entity
@Table(name = "service_item")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class ServiceItem {

    @Id
    @Column(length = 120)
    private String code; // e.g. "HOME_REPAIR__ELECTRICIAN"

    private String name;             // e.g. "Electrician"
    private String categoryCode;     // FK-by-convention to ServiceCategory.code, not a DB FK
    private String groupLabel;       // e.g. "Electrical" — UI sub-heading within the category
    private int displayOrder;
}
