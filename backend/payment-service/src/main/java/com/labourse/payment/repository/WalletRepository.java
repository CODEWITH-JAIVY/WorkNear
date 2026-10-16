package com.labourse.payment.repository;

import com.labourse.payment.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByLabourId(Long labourId);
}
