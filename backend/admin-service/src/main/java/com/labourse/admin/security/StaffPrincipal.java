package com.labourse.admin.security;

public record StaffPrincipal(Long id, String email, String name, StaffRole role) {}
