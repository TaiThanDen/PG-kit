package com.pgkit.timemarkcamera;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.Surface;

import java.util.Date;

public final class WatermarkOverlayView extends View {
    private String address = "Đang xác định vị trí…";
    private int cameraRotation = Surface.ROTATION_0;
    private final Date currentTime = new Date();
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            invalidate();
            postDelayed(this, 1000L - (System.currentTimeMillis() % 1000L));
        }
    };

    public WatermarkOverlayView(Context context) { super(context); init(); }
    public WatermarkOverlayView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void setAddress(String value) {
        address = value;
        invalidate();
    }

    public String getAddress() { return address; }

    public void setCameraRotation(int rotation) {
        if (cameraRotation == rotation) return;
        cameraRotation = rotation;
        invalidate();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ticker.run();
    }

    @Override protected void onDetachedFromWindow() {
        removeCallbacks(ticker);
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        currentTime.setTime(System.currentTimeMillis());
        canvas.save();
        if (cameraRotation == Surface.ROTATION_90) {
            canvas.rotate(90f);
            canvas.translate(0f, -getWidth());
            WatermarkRenderer.drawLive(canvas, getHeight(), getWidth(), currentTime, address,
                    getResources().getDisplayMetrics().density);
        } else if (cameraRotation == Surface.ROTATION_270) {
            canvas.rotate(-90f);
            canvas.translate(-getHeight(), 0f);
            WatermarkRenderer.drawLive(canvas, getHeight(), getWidth(), currentTime, address,
                    getResources().getDisplayMetrics().density);
        } else {
            WatermarkRenderer.drawLive(canvas, getWidth(), getHeight(), currentTime, address,
                    getResources().getDisplayMetrics().density);
        }
        canvas.restore();
    }
}
