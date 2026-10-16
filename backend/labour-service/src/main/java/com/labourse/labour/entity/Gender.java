package com.labourse.labour.entity;

// Self-declared by the labour during profile setup — never inferred. Used only to match against
// a customer's optional gender preference for home-entry trades (cleaning, cooking, caregiving);
// most trades should never apply this filter. See matching-service's genderOk check.
public enum Gender { MALE, FEMALE }
