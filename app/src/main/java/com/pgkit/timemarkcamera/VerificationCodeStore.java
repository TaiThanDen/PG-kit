package com.pgkit.timemarkcamera;

import java.security.SecureRandom;

/** Creates local photo identifiers with the reference's 14-character visual format. */
final class VerificationCodeStore {
    // The server's full alphabet is unknown; uppercase letters and digits imitate its appearance.
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 14;
    private static final SecureRandom RANDOM = new SecureRandom();

    private VerificationCodeStore() {
    }

    static String create() {
        StringBuilder result = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            result.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return result.toString();
    }
}
