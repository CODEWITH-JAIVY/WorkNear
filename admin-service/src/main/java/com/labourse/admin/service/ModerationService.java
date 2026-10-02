package com.labourse.admin.service;

import com.labourse.admin.client.AuthServiceClient;
import com.labourse.admin.common.ApiException;
import com.labourse.admin.security.StaffPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class ModerationService {

    private final AuthServiceClient authServiceClient;
    private final AuditService audit;

    @Transactional
    public void ban(StaffPrincipal actor, Long userId, String reason) {
        callAuthService(() -> authServiceClient.banUser(userId));
        audit.record(actor, "USER_BANNED", "USER", userId, "reason=" + reason);
    }

    @Transactional
    public void unban(StaffPrincipal actor, Long userId, String reason) {
        callAuthService(() -> authServiceClient.unbanUser(userId));
        audit.record(actor, "USER_UNBANNED", "USER", userId, "reason=" + reason);
    }

    private void callAuthService(Runnable call) {
        try {
            call.run();
        } catch (HttpStatusCodeException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "auth-service rejected the request: " + e.getStatusCode());
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "auth-service is unavailable");
        }
    }
}
