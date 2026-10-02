package com.labourse.admin.repository;

import com.labourse.admin.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Transactional
    @Modifying
    @Query("update RefreshToken t set t.revoked = true where t.staffId = :staffId and t.revoked = false")
    int revokeAllForStaff(@Param("staffId") Long staffId);
}
