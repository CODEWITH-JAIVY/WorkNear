package com.labourse.admin.service;

import com.labourse.admin.common.PageResponse;
import com.labourse.admin.entity.AuditLog;
import com.labourse.admin.repository.AuditLogRepository;
import com.labourse.admin.security.StaffPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    /** Called inside the caller's transaction, so the audit row and the action commit or roll back together. */
    public void record(StaffPrincipal actor, String action, String targetType, Object targetId, String details) {
        AuditLog log = new AuditLog();
        if (actor != null) {
            log.setActorId(actor.id());
            log.setActorEmail(actor.email());
            log.setActorRole(actor.role().name());
        }
        fill(log, action, targetType, targetId, details);
        repository.save(log);
    }

    /** For events with no authenticated staff member (failed logins). */
    public void recordAnonymous(String actorEmail, String action, String details) {
        AuditLog log = new AuditLog();
        log.setActorEmail(truncate(actorEmail, 255));
        fill(log, action, "STAFF", null, details);
        repository.save(log);
    }

    public PageResponse<AuditLog> search(Long actorId, String action, int page, int size) {
        Specification<AuditLog> spec = (root, query, cb) -> cb.conjunction();
        if (actorId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("actorId"), actorId));
        }
        if (action != null && !action.isBlank()) {
            String normalized = action.trim().toUpperCase();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), normalized));
        }
        return PageResponse.of(repository.findAll(spec, PageResponse.pageable(page, size)));
    }

    private void fill(AuditLog log, String action, String targetType, Object targetId, String details) {
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId == null ? null : String.valueOf(targetId));
        log.setDetails(truncate(details, 1000));
        log.setIpAddress(truncate(clientIp(), 64));
    }

    private String clientIp() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes sra)) {
            return null;
        }
        HttpServletRequest req = sra.getRequest();
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
