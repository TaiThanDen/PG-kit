package com.pgkit.timemarkcamera;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.TextView;

/** Shared visual tokens for the approved blue camera and sheets. */
final class CameraUi {
    static final int BLUE = 0xff1256df;
    static final int INK = 0xff17202d;
    static final int MUTED = 0xff687386;
    static final int SURFACE = 0xfff0f3f8;
    static int dp(Context c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }
    static GradientDrawable shape(Context c, int fill, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(c, radius));
        if (stroke != 0) d.setStroke(dp(c, 1), stroke);
        return d;
    }
    static TextView text(Context c, String text, int size, int color, boolean bold) {
        TextView v = new TextView(c);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setFontFeatureSettings("tnum");
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }
    static TextView action(Context c, String label, boolean primary, Runnable action) {
        TextView v = text(c, label, 16, primary ? Color.WHITE : BLUE, true);
        v.setGravity(Gravity.CENTER);
        v.setBackground(shape(c, primary ? BLUE : Color.WHITE, primary ? 0 : 0xffcdd5e2, 12));
        v.setClickable(true);
        v.setFocusable(true);
        v.setOnClickListener(w -> action.run());
        UiMotion.press(v);
        return v;
    }
}
