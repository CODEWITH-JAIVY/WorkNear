package com.labourse.admin.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static com.labourse.admin.security.Permission.*;

/**
 * Role -> permission mapping. Controllers check permissions, never roles,
 * so adding a role later is a one-line change here.
 */
public enum StaffRole {

    SUPER_ADMIN(EnumSet.allOf(Permission.class)),

    ADMIN(EnumSet.of(DASHBOARD_VIEW, DISPUTE_VIEW, DISPUTE_ASSIGN, DISPUTE_RESOLVE,
            USER_BAN, USER_UNBAN, STAFF_VIEW, AUDIT_VIEW)),

    /** Sees and resolves only disputes raised by CUSTOMER users. */
    CUSTOMER_EXECUTIVE(EnumSet.of(DASHBOARD_VIEW, DISPUTE_VIEW, DISPUTE_RESOLVE)),

    /** Sees and resolves only disputes raised by LABOUR users. */
    LABOUR_EXECUTIVE(EnumSet.of(DASHBOARD_VIEW, DISPUTE_VIEW, DISPUTE_RESOLVE)),

    FINANCE(EnumSet.of(DASHBOARD_VIEW, DISPUTE_VIEW, REFUND_REQUEST, REFUND_APPROVE)),

    /** Read-only. */
    ANALYST(EnumSet.of(DASHBOARD_VIEW, DISPUTE_VIEW));

    private final Set<Permission> permissions;

    StaffRole(Set<Permission> permissions) {
        this.permissions = Collections.unmodifiableSet(permissions);
    }

    public Set<Permission> permissions() {
        return permissions;
    }

    public boolean has(Permission permission) {
        return permissions.contains(permission);
    }

    /** raisedByType a dispute must have for this role to see it; null means unrestricted. */
    public String disputeScope() {
        return switch (this) {
            case CUSTOMER_EXECUTIVE -> "CUSTOMER";
            case LABOUR_EXECUTIVE -> "LABOUR";
            default -> null;
        };
    }
}
