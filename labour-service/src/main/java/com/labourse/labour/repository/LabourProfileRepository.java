package com.labourse.labour.repository;

import com.labourse.labour.entity.LabourProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LabourProfileRepository extends JpaRepository<LabourProfile, Long> {
    Optional<LabourProfile> findByUserId(Long userId);
}
