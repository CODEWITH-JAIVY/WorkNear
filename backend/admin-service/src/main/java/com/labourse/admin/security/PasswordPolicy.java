package com.labourse.admin.security;

import com.labourse.admin.common.ApiException;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class PasswordPolicy {

    private static final Pattern POLICY =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,64}$");

    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL = "@#$%&*!?";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordPolicy() {}

    public static boolean isValid(String password) {
        return password != null && POLICY.matcher(password).matches();
    }

    public static void validate(String password) {
        if (!isValid(password)) {
            throw ApiException.badRequest(
                    "Password must be 8-64 characters with an upper case letter, a lower case letter, a digit and a special character");
        }
    }

    /** 14 random characters that always satisfy the policy. */
    public static String generateTemporaryPassword() {
        String all = LOWER + UPPER + DIGITS + SPECIAL;
        List<Character> chars = new ArrayList<>();
        chars.add(pick(LOWER));
        chars.add(pick(UPPER));
        chars.add(pick(DIGITS));
        chars.add(pick(SPECIAL));
        while (chars.size() < 14) {
            chars.add(pick(all));
        }
        Collections.shuffle(chars, RANDOM);
        StringBuilder sb = new StringBuilder();
        for (char c : chars) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static char pick(String source) {
        return source.charAt(RANDOM.nextInt(source.length()));
    }
}
