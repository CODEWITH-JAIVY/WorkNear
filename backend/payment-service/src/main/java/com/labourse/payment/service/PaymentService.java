package com.labourse.payment.service;

import com.labourse.payment.dto.CreateOrderRequest;
import com.labourse.payment.dto.CreateOrderResponse;
import com.labourse.payment.entity.Transaction;
import com.labourse.payment.entity.TransactionStatus;
import com.labourse.payment.entity.Wallet;
import com.labourse.payment.repository.TransactionRepository;
import com.labourse.payment.repository.WalletRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.json.JSONObject;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;
    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;
    @Value("${payment.commission-rate:0.15}")
    private double commissionRate; // 15% platform cut — tune per business model

    public CreateOrderResponse createOrder(CreateOrderRequest req) throws Exception {
        RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", (int) (req.getAmount() * 100)); // paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "job_" + req.getJobId());
        Order order = client.orders.create(orderRequest);

        double commission = req.getAmount() * commissionRate;
        Transaction txn = new Transaction();
        txn.setJobId(req.getJobId());
        txn.setCustomerId(req.getCustomerId());
        txn.setLabourId(req.getLabourId());
        txn.setAmount(req.getAmount());
        txn.setCommission(commission);
        txn.setLabourPayout(req.getAmount() - commission);
        txn.setRazorpayOrderId(order.get("id"));
        txn = transactionRepository.save(txn);

        return new CreateOrderResponse(order.get("id"), req.getAmount(), "INR", txn.getId());
    }

    // Called from Razorpay webhook on payment.captured
    @Transactional
    public void markPaidAndCreditWallet(String razorpayOrderId, String razorpayPaymentId) {
        Transaction txn = transactionRepository.findAll().stream()
                .filter(t -> razorpayOrderId.equals(t.getRazorpayOrderId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found for order " + razorpayOrderId));

        txn.setStatus(TransactionStatus.PAID);
        txn.setRazorpayPaymentId(razorpayPaymentId);
        transactionRepository.save(txn);

        Wallet wallet = walletRepository.findByLabourId(txn.getLabourId())
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setLabourId(txn.getLabourId());
                    return w;
                });
        wallet.setBalance(wallet.getBalance() + txn.getLabourPayout());
        wallet.setLifetimeEarnings(wallet.getLifetimeEarnings() + txn.getLabourPayout());
        walletRepository.save(wallet);
    }
}
