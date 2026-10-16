package com.labourse.admin.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StaffRoleTest {

    @Test
    void superAdminHasEveryPermission() {
        assertThat(StaffRole.SUPER_ADMIN.permissions()).containsExactlyInAnyOrder(Permission.values());
    }

    @Test
    void onlySuperAdminCanManageStaff() {
        for (StaffRole role : StaffRole.values()) {
            assertThat(role.has(Permission.STAFF_MANAGE)).isEqualTo(role == StaffRole.SUPER_ADMIN);
        }
    }

    @Test
    void analystIsReadOnly() {
        assertThat(StaffRole.ANALYST.has(Permission.DISPUTE_VIEW)).isTrue();
        assertThat(StaffRole.ANALYST.has(Permission.DISPUTE_RESOLVE)).isFalse();
        assertThat(StaffRole.ANALYST.has(Permission.USER_BAN)).isFalse();
        assertThat(StaffRole.ANALYST.has(Permission.REFUND_APPROVE)).isFalse();
    }

    @Test
    void executivesAreScopedToTheirUserType() {
        assertThat(StaffRole.CUSTOMER_EXECUTIVE.disputeScope()).isEqualTo("CUSTOMER");
        assertThat(StaffRole.LABOUR_EXECUTIVE.disputeScope()).isEqualTo("LABOUR");
        assertThat(StaffRole.ADMIN.disputeScope()).isNull();
        assertThat(StaffRole.CUSTOMER_EXECUTIVE.has(Permission.USER_BAN)).isFalse();
    }

    @Test
    void onlyFinanceCanRequestAndApproveRefunds_amongNonSuperAdmins() {
        assertThat(StaffRole.FINANCE.has(Permission.REFUND_REQUEST)).isTrue();
        assertThat(StaffRole.FINANCE.has(Permission.REFUND_APPROVE)).isTrue();
        assertThat(StaffRole.ADMIN.has(Permission.REFUND_APPROVE)).isFalse();
    }
}
