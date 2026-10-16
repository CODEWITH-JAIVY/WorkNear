package com.labourse.payment.controller;

import com.labourse.payment.dto.CreateOrderRequest;
import com.labourse.payment.dto.CreateOrderResponse;
import com.labourse.payment.entity.Wallet;
import com.labourse.payment.repository.WalletRepository;
import com.labourse.payment.service.PaymentService;
import com.labourse.payment.service.WebhookVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final WalletRepository walletRepository;
    private final WebhookVerificationService webhookVerificationService;

    @PostMapping("/orders")
    public CreateOrderResponse createOrder(@RequestBody CreateOrderRequest req) throws Exception {
        return paymentService.createOrder(req);
    }

    // Raw body is required for HMAC verification — a parsed/re-serialized JSON object
    // can differ byte-for-byte from what Razorpay signed, breaking the check.
    @PostMapping("/webhook/razorpay")
    public ResponseEntity<?> handleWebhook(@RequestBody String rawBody,
                                            @RequestHeader("X-Razorpay-Signature") String signature) {
        if (!webhookVerificationService.isValid(rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid signature"));
        }

        // Minimal shape — real payload is nested under payload.payment.entity.*; parse rawBody with
        // Jackson here in a full implementation instead of trusting flat top-level keys.
        org.json.JSONObject json = new org.json.JSONObject(rawBody);
        String orderId = json.optString("order_id", null);
        String paymentId = json.optString("payment_id", null);
        if (orderId != null && paymentId != null) {
            paymentService.markPaidAndCreditWallet(orderId, paymentId);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/wallet/{labourId}")
    public Wallet getWallet(@PathVariable Long labourId) {
        return walletRepository.findByLabourId(labourId)
                .orElseThrow(() -> new IllegalArgumentException("No wallet yet"));
    }
}

