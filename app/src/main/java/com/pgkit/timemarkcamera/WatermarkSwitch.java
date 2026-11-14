package com.pgkit.timemarkcamera;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.Switch;

/** Native Switch behavior/accessibility with the rounded track used by the reference editor. */
final class WatermarkSwitch extends Switch {
    private final Paint brush = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float progress;
    private ValueAnimator motion;

    WatermarkSwitch(Context context) {
        super(context);
        setShowText(false);
        setBackgroundColor(Color.TRANSPARENT);
    }

    @Override public void setChecked(boolean checked) {
        super.setChecked(checked);
        if (motion != null) motion.cancel();
        float target = checked ? 1f : 0f;
        if (!isAttachedToWindow() || !ValueAnimator.areAnimatorsEnabled()) {
            progress = target;
            invalidate();
            return;
        }
        motion = ValueAnimator.ofFloat(progress, target);
        motion.setDuration(120);
        motion.addUpdateListener(value -> {
            progress = (float) value.getAnimatedValue();
            invalidate();
        });
        motion.start();
    }

    @Override protected void onDraw(Canvas canvas) {
        float density = getResources().getDisplayMetrics().density;
        float trackWidth = 44 * density, trackHeight = 26 * density;
        float left = (getWidth() - trackWidth) / 2, top = (getHeight() - trackHeight) / 2;
        int off = 0xffe9e9eb, on = 0xff2dca58;
        brush.setColor(Color.rgb(
                Math.round(Color.red(off) + (Color.red(on) - Color.red(off)) * progress),
                Math.round(Color.green(off) + (Color.green(on) - Color.green(off)) * progress),
                Math.round(Color.blue(off) + (Color.blue(on) - Color.blue(off)) * progress)));
        canvas.drawRoundRect(left, top, left + trackWidth, top + trackHeight,
                trackHeight / 2, trackHeight / 2, brush);
        float radius = 12 * density;
        float x = left + trackHeight / 2 + progress * (trackWidth - trackHeight);
        brush.setColor(0xffd9d9dc);
        canvas.drawCircle(x, top + trackHeight / 2, radius + .5f * density, brush);
        brush.setColor(Color.WHITE);
        canvas.drawCircle(x, top + trackHeight / 2, radius, brush);
    }

    @Override protected void onDetachedFromWindow() {
        if (motion != null) motion.cancel();
        super.onDetachedFromWindow();
    }
}
