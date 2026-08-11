package com.pgkit.timemarkcamera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.View;
import android.view.Surface;

import java.util.Date;

public final class WatermarkOverlayView extends View {
    static final String DEFAULT_ADDRESS = "Đang xác định vị trí…";

    private String address = DEFAULT_ADDRESS;
    private int cameraRotation = Surface.ROTATION_0;
    private final Date currentTime = new Date();
    private Date displayTimeOverride;
    private final Matrix orientationMatrix = new Matrix();
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

    /**
     * Uses {@code value} for the live watermark until cleared. Passing {@code null} restores
     * the device clock. The supplied mutable Date is copied so later caller changes cannot
     * alter the preview unexpectedly.
     */
    public void setDisplayTimeOverride(Date value) {
        displayTimeOverride = value == null ? null : new Date(value.getTime());
        invalidate();
    }

    public void clearDisplayTimeOverride() {
        setDisplayTimeOverride(null);
    }

    /** Returns a defensive snapshot of the active override, or {@code null} for device time. */
    public Date getDisplayTimeOverride() {
        return displayTimeOverride == null ? null : new Date(displayTimeOverride.getTime());
    }

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
        currentTime.setTime(displayTimeOverride == null
                ? System.currentTimeMillis()
                : displayTimeOverride.getTime());
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        if (viewWidth == 0 || viewHeight == 0) return;

        // Keep the renderer in the portrait coordinate system. DeviceOrientation owns the full
        // rotation/translation that maps the bottom-left base watermark to each physical anchor.
        DeviceOrientation.setWatermarkTransform(
                orientationMatrix, cameraRotation, viewWidth, viewHeight);
        int checkpoint = canvas.save();
        canvas.concat(orientationMatrix);
        WatermarkRenderer.drawLive(canvas, viewWidth, viewHeight, currentTime, address,
                getResources().getDisplayMetrics().density);
        canvas.restoreToCount(checkpoint);
    }
}
