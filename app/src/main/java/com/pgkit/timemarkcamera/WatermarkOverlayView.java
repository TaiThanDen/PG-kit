package com.pgkit.timemarkcamera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.Surface;

import java.util.Date;

public final class WatermarkOverlayView extends View {
    static final String DEFAULT_ADDRESS = "Đang xác định vị trí…";

    private String address = DEFAULT_ADDRESS;
    private int cameraRotation = Surface.ROTATION_0;
    private final Date currentTime = new Date();
    private final Matrix orientationMatrix = new Matrix();
    private final RectF orientationBounds = new RectF();
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
        if (cameraRotation == Surface.ROTATION_0) {
            WatermarkRenderer.drawLive(canvas, getWidth(), getHeight(), currentTime, address,
                    getResources().getDisplayMetrics().density);
            return;
        }

        orientationMatrix.reset();
        orientationMatrix.setRotate(DeviceOrientation.watermarkDegrees(cameraRotation));
        orientationBounds.set(0f, 0f, getWidth(), getHeight());
        orientationMatrix.mapRect(orientationBounds);
        orientationMatrix.postTranslate(-orientationBounds.left, -orientationBounds.top);

        int checkpoint = canvas.save();
        canvas.concat(orientationMatrix);
        WatermarkRenderer.drawLive(canvas, getWidth(), getHeight(), currentTime, address,
                getResources().getDisplayMetrics().density);
        canvas.restoreToCount(checkpoint);
    }
}
