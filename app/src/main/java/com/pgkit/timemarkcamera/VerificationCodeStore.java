package com.pgkit.timemarkcamera;

import android.app.Activity;

import java.security.SecureRandom;

/** Creates one stable, local verification code for this app installation. */
final class VerificationCodeStore {
    private static final String KEY = "verification_code";
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 14;

    private VerificationCodeStore() {
    }

    static String obtain(Activity activity) {
        String saved = activity.getPreferences(Activity.MODE_PRIVATE).getString(KEY, null);
        if (saved != null && saved.length() == CODE_LENGTH) return saved;

        SecureRandom random = new SecureRandom();
        StringBuilder result = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            result.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        saved = result.toString();
        activity.getPreferences(Activity.MODE_PRIVATE).edit().putString(KEY, saved).apply();
        return saved;
    }
}
