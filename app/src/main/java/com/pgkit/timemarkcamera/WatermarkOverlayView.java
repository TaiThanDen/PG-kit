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
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        if (viewWidth == 0 || viewHeight == 0) return;

        if (cameraRotation == Surface.ROTATION_0) {
            WatermarkRenderer.drawLive(canvas, viewWidth, viewHeight, currentTime, address,
                    getResources().getDisplayMetrics().density);
            return;
        }

        // The Activity remains portrait, so the physical View is always viewWidth x viewHeight.
        // A quarter-turn must be laid out in the pre-rotation landscape coordinate system;
        // otherwise the renderer receives portrait bounds and the transformed watermark is clipped.
        boolean quarterTurn = cameraRotation == Surface.ROTATION_90
                || cameraRotation == Surface.ROTATION_270;
        int logicalWidth = quarterTurn ? viewHeight : viewWidth;
        int logicalHeight = quarterTurn ? viewWidth : viewHeight;

        orientationMatrix.reset();
        orientationMatrix.setRotate(DeviceOrientation.watermarkDegrees(cameraRotation));
        orientationBounds.set(0f, 0f, logicalWidth, logicalHeight);
        orientationMatrix.mapRect(orientationBounds);
        orientationMatrix.postTranslate(-orientationBounds.left, -orientationBounds.top);

        int checkpoint = canvas.save();
        canvas.concat(orientationMatrix);
        WatermarkRenderer.drawLive(canvas, logicalWidth, logicalHeight, currentTime, address,
                getResources().getDisplayMetrics().density);
        canvas.restoreToCount(checkpoint);
    }
}
