package com.labourse.admin.security;

import com.labourse.admin.common.ApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @Test
    void acceptsStrongPassword() {
        assertThat(PasswordPolicy.isValid("Str0ng@Pass")).isTrue();
    }

    @Test
    void rejectsWeakPasswords() {
        assertThat(PasswordPolicy.isValid(null)).isFalse();
        assertThat(PasswordPolicy.isValid("short1A@")).isTrue();      // exactly 8, meets every rule
        assertThat(PasswordPolicy.isValid("alllowercase1@")).isFalse();
        assertThat(PasswordPolicy.isValid("NoDigits@@@")).isFalse();
        assertThat(PasswordPolicy.isValid("NoSpecial123")).isFalse();
        assertThat(PasswordPolicy.isValid("Sh0rt@")).isFalse();
    }

    @Test
    void validateThrowsApiExceptionOnWeakPassword() {
        assertThatThrownBy(() -> PasswordPolicy.validate("weak")).isInstanceOf(ApiException.class);
    }

    @Test
    void generatedTemporaryPasswordsAlwaysMeetThePolicy() {
        for (int i = 0; i < 200; i++) {
            assertThat(PasswordPolicy.isValid(PasswordPolicy.generateTemporaryPassword())).isTrue();
        }
    }
}
