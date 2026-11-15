package com.pgkit.timemarkcamera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/** Focus geometry measured from CameraGestureCaptureView in the reference APK. */
final class FocusRingView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;

    FocusRingView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        paint.setStyle(Paint.Style.STROKE);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float x = getWidth() / 2f;
        float y = getHeight() / 2f;
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(2f * density);
        canvas.drawCircle(x, y, 35f * density, paint);
        paint.setStrokeWidth(1.5f * density);
        canvas.drawCircle(x, y, 5.5f * density, paint);
        paint.setColor(0x33000000);
        paint.setStrokeWidth(0.5f * density);
        canvas.drawCircle(x, y, 34f * density, paint);
        canvas.drawCircle(x, y, 36f * density, paint);
        paint.setStrokeWidth(0.2f * density);
        canvas.drawCircle(x, y, 5f * density, paint);
        canvas.drawCircle(x, y, 6f * density, paint);
    }
}
