package com.pgkit.timemarkcamera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.View;
import android.view.Surface;

import java.util.Date;
import java.util.function.Supplier;
import java.util.function.IntSupplier;

public final class WatermarkOverlayView extends View {
    static final String DEFAULT_ADDRESS = "";

    private String address = DEFAULT_ADDRESS;
    private int cameraRotation = Surface.ROTATION_0;
    private final Date currentTime = new Date();
    private WatermarkTemplate.Snapshot template;
    private int visibleBottomOnScreen = Integer.MAX_VALUE;
    private final int[] screenPosition = new int[2];
    private Supplier<String> addressSource;
    private IntSupplier rotationSource;
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

    public void setTemplate(WatermarkTemplate.Snapshot value) {
        template = value;
        invalidate();
    }

    /** Moves the watermark above a template sheet without resizing or rebinding the camera. */
    public void setVisibleBottomOnScreen(int bottom) {
        visibleBottomOnScreen = bottom;
        invalidate();
    }

    void setLiveSources(Supplier<String> addressSource, IntSupplier rotationSource) {
        this.addressSource = addressSource;
        this.rotationSource = rotationSource;
        invalidate();
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
        if (addressSource != null) {
            String value = addressSource.get();
            if (value != null) address = value;
        }
        if (rotationSource != null) cameraRotation = rotationSource.getAsInt();
        currentTime.setTime(System.currentTimeMillis());
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        if (visibleBottomOnScreen != Integer.MAX_VALUE) {
            getLocationOnScreen(screenPosition);
            viewHeight = Math.min(viewHeight, Math.max(0, visibleBottomOnScreen - screenPosition[1]));
        }
        if (viewWidth == 0 || viewHeight == 0) return;

        // Keep the renderer in the portrait coordinate system. DeviceOrientation owns the full
        // rotation/translation that maps the bottom-left base watermark to each physical anchor.
        DeviceOrientation.setWatermarkTransform(
                orientationMatrix, cameraRotation, viewWidth, viewHeight);
        int checkpoint = canvas.save();
        canvas.concat(orientationMatrix);
        WatermarkRenderer.drawLive(canvas, viewWidth, viewHeight, currentTime, address,
                getResources().getDisplayMetrics().density, template);
        canvas.restoreToCount(checkpoint);
    }
}
