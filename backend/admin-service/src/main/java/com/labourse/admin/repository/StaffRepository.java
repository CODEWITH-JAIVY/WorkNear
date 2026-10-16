package com.labourse.admin.repository;

import com.labourse.admin.entity.Staff;
import com.labourse.admin.entity.StaffStatus;
import com.labourse.admin.security.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    Optional<Staff> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    long countByRoleAndStatus(StaffRole role, StaffStatus status);
    long countByStatus(StaffStatus status);
}
