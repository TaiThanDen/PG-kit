package com.pgkit.timemarkcamera;

import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** Small feedback animations. Honor the system's animator setting. */
final class UiMotion {
    static void press(View view) {
        view.setOnTouchListener((v, event) -> {
            if (!ValueAnimator.areAnimatorsEnabled() || !v.isEnabled()) return false;
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(.96f).scaleY(.96f).setDuration(90).start();
            } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
            }
            return false;
        });
    }

    static void reveal(View view) {
        if (!ValueAnimator.areAnimatorsEnabled()) return;
        view.animate().cancel();
        view.setAlpha(.6f);
        view.animate().alpha(1f).setDuration(140)
                .setInterpolator(new DecelerateInterpolator()).start();
    }
}
