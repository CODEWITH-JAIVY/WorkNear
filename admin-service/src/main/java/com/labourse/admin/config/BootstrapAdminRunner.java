package com.labourse.admin.config;

import com.labourse.admin.common.ApiException;
import com.labourse.admin.entity.Staff;
import com.labourse.admin.repository.StaffRepository;
import com.labourse.admin.security.PasswordPolicy;
import com.labourse.admin.security.StaffRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the very first SUPER_ADMIN when the staff table is empty. Everyone else is created
 * through POST /api/admin/staff. The account must change its password on first login.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapAdminRunner implements ApplicationRunner {

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.bootstrap.email:}")
    private String email;

    @Value("${admin.bootstrap.password:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (staffRepository.count() > 0) {
            return;
        }
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("No staff exist yet. Set ADMIN_BOOTSTRAP_EMAIL and ADMIN_BOOTSTRAP_PASSWORD to create the first SUPER_ADMIN.");
            return;
        }
        try {
            PasswordPolicy.validate(password);
        } catch (ApiException e) {
            log.error("ADMIN_BOOTSTRAP_PASSWORD does not meet the password policy; first SUPER_ADMIN was NOT created. {}", e.getMessage());
            return;
        }

        Staff admin = new Staff();
        admin.setName("Super Admin");
        admin.setEmail(email.trim().toLowerCase());
        admin.setRole(StaffRole.SUPER_ADMIN);
        admin.setDepartment("Management");
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setMustChangePassword(true);
        staffRepository.save(admin);
        log.info("Created first SUPER_ADMIN {} (password change required on first login)", admin.getEmail());
    }
}
