package com.pgkit.timemarkcamera;

import android.content.Context;
import android.graphics.Bitmap;
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

        Bitmap portraitLayer = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas layerCanvas = new Canvas(portraitLayer);
        WatermarkRenderer.drawLive(layerCanvas, getWidth(), getHeight(), currentTime, address,
                getResources().getDisplayMetrics().density);
        Matrix matrix = new Matrix();
        matrix.postRotate(DeviceOrientation.watermarkDegrees(cameraRotation));
        Bitmap rotatedLayer = Bitmap.createBitmap(portraitLayer, 0, 0,
                portraitLayer.getWidth(), portraitLayer.getHeight(), matrix, true);
        canvas.drawBitmap(rotatedLayer, 0f, 0f, null);
        portraitLayer.recycle();
        if (rotatedLayer != portraitLayer) rotatedLayer.recycle();
    }
}
