package com.pgkit.timemarkcamera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.camera.view.PreviewView;

/** Sun and vertical exposure rail; camera range and metering stay in CameraEngine. */
final class FocusExposureView extends View {
    interface Listener { void onStep(int direction); }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private final Listener listener;
    private final Runnable onStart;
    private final Runnable onEnd;
    private final PreviewView previewView;
    private final RectF touchRegion = new RectF();
    private float sunX;
    private float sunY;
    private float startX;
    private float startY;
    private float previousY;
    private float remainder;
    private float sunOffset;
    private boolean dragging;
    private boolean multiplePointers;
    private boolean forwardingPreview;

    FocusExposureView(Context context, PreviewView previewView, Listener listener,
                      Runnable onStart, Runnable onEnd) {
        super(context);
        this.previewView = previewView;
        this.listener = listener;
        this.onStart = onStart;
        this.onEnd = onEnd;
        density = getResources().getDisplayMetrics().density;
        setContentDescription(context.getString(R.string.ui_drag_up_to_brighten_down_to_darken));
        setFocusable(true);
        setClickable(true);
    }

    void setFocusPoint(float x, float y) {
        boolean right = x < getWidth() / 2f;
        sunX = x + (right ? 60 : -60) * density;
        sunY = y;
        sunOffset = 0;
        touchRegion.set(x - (right ? 78 : 152) * density, y - 115 * density,
                x + (right ? 152 : 78) * density, y + 115 * density);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float x = sunX;
        float y = sunY + sunOffset;
        paint.setColor(0x4dffffff);
        paint.setStrokeWidth(density);
        float top = sunY - 52.5f * density;
        float bottom = sunY + 52.5f * density;
        float gap = 20f * density;
        if (y - gap > top) canvas.drawLine(x, top, x, y - gap, paint);
        if (y + gap < bottom) {
            canvas.drawLine(x, y + gap, x, bottom, paint);
        }
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x, y, 4.5f * density, paint);
        paint.setStrokeWidth(1.7f * density);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            float dx = (float) Math.cos(angle);
            float dy = (float) Math.sin(angle);
            canvas.drawLine(x + dx * 8 * density, y + dy * 8 * density,
                    x + dx * 11 * density, y + dy * 11 * density, paint);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!touchRegion.contains(event.getX(), event.getY())) return false;
                startX = event.getX();
                startY = event.getY();
                previousY = event.getY();
                remainder = 0;
                dragging = false;
                multiplePointers = false;
                forwardingPreview = true;
                previewView.dispatchTouchEvent(event);
                onStart.run();
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (multiplePointers) {
                    previewView.dispatchTouchEvent(event);
                    return true;
                }
                float delta = previousY - event.getY();
                previousY = event.getY();
                if (!dragging) {
                    if (Math.abs(event.getY() - startY) < 4f * density
                            || Math.abs(event.getY() - startY)
                            <= Math.abs(event.getX() - startX) * 3f) {
                        previewView.dispatchTouchEvent(event);
                        return true;
                    }
                    dragging = true;
                    cancelPreview(event);
                    delta = startY - event.getY();
                }
                remainder += delta;
                // Keep steps independent of how many MotionEvents the device delivers.
                float step = 16f * density;
                while (Math.abs(remainder) >= step) {
                    int direction = remainder > 0 ? 1 : -1;
                    listener.onStep(direction);
                    remainder -= direction * step;
                }
                float limit = 52.5f * density;
                sunOffset = Math.max(-limit, Math.min(limit, sunOffset - delta * 0.15f));
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                if (forwardingPreview) previewView.dispatchTouchEvent(event);
                if (!dragging && !multiplePointers) performClick();
                onEnd.run();
                return true;
            case MotionEvent.ACTION_CANCEL:
                cancelPreview(event);
                onEnd.run();
                return true;
            default:
                if (event.getPointerCount() > 1) {
                    multiplePointers = true;
                    if (!forwardingPreview) {
                        MotionEvent down = MotionEvent.obtain(event.getDownTime(),
                                event.getEventTime(), MotionEvent.ACTION_DOWN,
                                event.getX(), event.getY(), 0);
                        previewView.dispatchTouchEvent(down);
                        down.recycle();
                        forwardingPreview = true;
                    }
                    previewView.dispatchTouchEvent(event);
                }
                return true;
        }
    }

    private void cancelPreview(MotionEvent event) {
        if (!forwardingPreview) return;
        MotionEvent cancel = MotionEvent.obtain(event);
        cancel.setAction(MotionEvent.ACTION_CANCEL);
        previewView.dispatchTouchEvent(cancel);
        cancel.recycle();
        forwardingPreview = false;
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.SeekBar");
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
    }

    @Override public boolean performAccessibilityAction(int action, android.os.Bundle arguments) {
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                || action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
            listener.onStep(action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ? 1 : -1);
            onEnd.run();
            return true;
        }
        return super.performAccessibilityAction(action, arguments);
    }
}
