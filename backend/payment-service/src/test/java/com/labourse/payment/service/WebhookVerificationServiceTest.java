package com.labourse.payment.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookVerificationServiceTest {

    private final WebhookVerificationService service = new WebhookVerificationService();

    @Test
    void isValid_true_whenSignatureMatchesComputedHmac() throws Exception {
        String secret = "test-webhook-secret";
        ReflectionTestUtils.setField(service, "webhookSecret", secret);

        String body = "{\"order_id\":\"order_abc\",\"payment_id\":\"pay_xyz\"}";
        String correctSignature = computeHmac(body, secret);

        assertThat(service.isValid(body, correctSignature)).isTrue();
    }

    @Test
    void isValid_false_whenSignatureIsTamperedOrWrong() {
        ReflectionTestUtils.setField(service, "webhookSecret", "test-webhook-secret");

        String body = "{\"order_id\":\"order_abc\",\"payment_id\":\"pay_xyz\"}";

        assertThat(service.isValid(body, "totally-wrong-signature")).isFalse();
    }

    @Test
    void isValid_false_whenBodyIsModifiedAfterSigning() throws Exception {
        String secret = "test-webhook-secret";
        ReflectionTestUtils.setField(service, "webhookSecret", secret);

        String originalBody = "{\"order_id\":\"order_abc\",\"amount\":1000}";
        String signatureForOriginal = computeHmac(originalBody, secret);

        // Attacker changes the amount after the signature was computed — must fail
        String tamperedBody = "{\"order_id\":\"order_abc\",\"amount\":9999999}";

        assertThat(service.isValid(tamperedBody, signatureForOriginal)).isFalse();
    }

    private String computeHmac(String body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] computed = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : computed) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
