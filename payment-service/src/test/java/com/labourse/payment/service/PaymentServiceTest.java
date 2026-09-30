package com.labourse.payment.service;

import com.labourse.payment.entity.Transaction;
import com.labourse.payment.entity.TransactionStatus;
import com.labourse.payment.entity.Wallet;
import com.labourse.payment.repository.TransactionRepository;
import com.labourse.payment.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Razorpay's SDK makes createOrder() hard to unit test without a live sandbox call, so this
// focuses on the part that's actually ours: commission math and wallet crediting on webhook.
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock WalletRepository walletRepository;
    @InjectMocks PaymentService paymentService;

    @Test
    void markPaidAndCreditWallet_appliesCommissionSplit_forNewWallet() {
        ReflectionTestUtils.setField(paymentService, "commissionRate", 0.15);

        Transaction txn = new Transaction();
        txn.setId(1L);
        txn.setLabourId(7L);
        txn.setAmount(1000.0);
        txn.setCommission(150.0);
        txn.setLabourPayout(850.0);
        txn.setRazorpayOrderId("order_abc");
        txn.setStatus(TransactionStatus.CREATED);

        when(transactionRepository.findAll()).thenReturn(List.of(txn));
        when(walletRepository.findByLabourId(7L)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.markPaidAndCreditWallet("order_abc", "pay_xyz");

        assertThat(txn.getStatus()).isEqualTo(TransactionStatus.PAID);
        verify(walletRepository).save(argThat(w ->
                w.getBalance() == 850.0 && w.getLifetimeEarnings() == 850.0));
    }

    @Test
    void markPaidAndCreditWallet_addsToExistingBalance_notOverwrites() {
        Transaction txn = new Transaction();
        txn.setId(2L);
        txn.setLabourId(8L);
        txn.setLabourPayout(500.0);
        txn.setRazorpayOrderId("order_def");
        txn.setStatus(TransactionStatus.CREATED);

        Wallet existingWallet = new Wallet();
        existingWallet.setLabourId(8L);
        existingWallet.setBalance(2000.0);
        existingWallet.setLifetimeEarnings(10000.0);

        when(transactionRepository.findAll()).thenReturn(List.of(txn));
        when(walletRepository.findByLabourId(8L)).thenReturn(Optional.of(existingWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.markPaidAndCreditWallet("order_def", "pay_ghi");

        // Existing balance + new payout, never a flat overwrite — this is the bug class that
        // silently loses a labour's prior earnings if someone "fixes" this with wallet.setBalance(payout)
        verify(walletRepository).save(argThat(w -> w.getBalance() == 2500.0));
    }

    @Test
    void markPaidAndCreditWallet_throws_whenOrderIdUnknown() {
        when(transactionRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> paymentService.markPaidAndCreditWallet("nonexistent_order", "pay_1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
