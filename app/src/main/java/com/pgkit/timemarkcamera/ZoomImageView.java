package com.pgkit.timemarkcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import java.util.function.IntConsumer;

/** Fit, pinch, pan and double-tap. A horizontal swipe changes photo only at base scale. */
final class ZoomImageView extends ImageView {
    private final Matrix transform = new Matrix();
    private final GestureDetector taps;
    private final ScaleGestureDetector pinch;
    private float zoom = 1f;
    private float base = 1f;
    private float offsetX;
    private float offsetY;
    private float downX, downY, lastX, lastY;
    private boolean multi;
    private final IntConsumer onSwipe;
    private final Runnable onTap;

    ZoomImageView(Context context, IntConsumer onSwipe, Runnable onTap) {
        super(context);
        this.onSwipe = onSwipe;
        this.onTap = onTap;
        setScaleType(ScaleType.MATRIX);
        setContentDescription(context.getString(R.string.ui_photo_pinch_to_zoom_double_tap_to_zoom));
        setFocusable(true);
        taps = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public boolean onSingleTapConfirmed(MotionEvent e) { performClick(); return true; }
            @Override public boolean onDoubleTap(MotionEvent e) {
                float target = zoom > 1.05f ? 1f : 2.5f;
                float start = zoom, startX = offsetX, startY = offsetY;
                float finalX = target == 1 ? 0 : (getWidth()/2f - e.getX()) * (target-1);
                float finalY = target == 1 ? 0 : (getHeight()/2f - e.getY()) * (target-1);
                if (!android.animation.ValueAnimator.areAnimatorsEnabled()) {
                    zoom = target; offsetX = finalX; offsetY = finalY; update(); return true;
                }
                cancelZoomAnimation();
                zoomAnimation = android.animation.ValueAnimator.ofFloat(0f, 1f);
                zoomAnimation.setDuration(180);
                zoomAnimation.setInterpolator(new android.view.animation.DecelerateInterpolator());
                zoomAnimation.addUpdateListener(a -> {
                    float f = (float) a.getAnimatedValue();
                    zoom = start + (target-start)*f;
                    offsetX = startX + (finalX-startX)*f;
                    offsetY = startY + (finalY-startY)*f;
                    update();
                });
                zoomAnimation.start();
                return true;
            }
        });
        pinch = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector detector) {
                float old = zoom;
                zoom = Math.max(1f, Math.min(5f, zoom * detector.getScaleFactor()));
                float factor = zoom / old;
                offsetX = (offsetX + getWidth()/2f - detector.getFocusX()) * factor
                        - (getWidth()/2f - detector.getFocusX());
                offsetY = (offsetY + getHeight()/2f - detector.getFocusY()) * factor
                        - (getHeight()/2f - detector.getFocusY());
                update();
                return true;
            }
        });
    }
    private android.animation.ValueAnimator zoomAnimation;
    private void cancelZoomAnimation() { if (zoomAnimation != null) zoomAnimation.cancel(); }
    @Override public void setImageBitmap(Bitmap bitmap) { super.setImageBitmap(bitmap); reset(); }
    @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) { reset(); }
    private void reset() {
        cancelZoomAnimation();
        zoom = 1; offsetX = offsetY = 0;
        Drawable d = getDrawable();
        if (d != null && getWidth() > 0 && getHeight() > 0) {
            base = Math.min(getWidth()/(float)d.getIntrinsicWidth(), getHeight()/(float)d.getIntrinsicHeight());
            update();
        }
    }
    private void update() {
        Drawable d = getDrawable();
        if (d == null) return;
        float width = d.getIntrinsicWidth() * base * zoom, height = d.getIntrinsicHeight() * base * zoom;
        float maxX = Math.max(0, (width-getWidth())/2f), maxY = Math.max(0, (height-getHeight())/2f);
        offsetX = Math.max(-maxX, Math.min(maxX, offsetX));
        offsetY = Math.max(-maxY, Math.min(maxY, offsetY));
        transform.setScale(base*zoom, base*zoom);
        transform.postTranslate((getWidth()-width)/2+offsetX, (getHeight()-height)/2+offsetY);
        setImageMatrix(transform);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            cancelZoomAnimation();
            downX = lastX = event.getX(); downY = lastY = event.getY(); multi = false;
        }
        if (event.getPointerCount() > 1 && !multi) {
            multi = true;
            MotionEvent cancel = MotionEvent.obtain(event);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            taps.onTouchEvent(cancel);
            cancel.recycle();
        }
        pinch.onTouchEvent(event);
        if (!multi) taps.onTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE && zoom > 1.02f && !pinch.isInProgress() && !multi) {
            offsetX += event.getX()-lastX; offsetY += event.getY()-lastY; update();
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP && !multi && zoom <= 1.02f) {
            float x = event.getX()-downX, y = event.getY()-downY;
            if (Math.abs(x) > CameraUi.dp(getContext(), 64) && Math.abs(x) > Math.abs(y)*1.5f) {
                onSwipe.accept(x < 0 ? 1 : -1);
            }
        }
        lastX = event.getX(); lastY = event.getY();
        return true;
    }
    @Override public boolean performClick() { super.performClick(); onTap.run(); return true; }
    @Override protected void onDetachedFromWindow() { cancelZoomAnimation(); super.onDetachedFromWindow(); }
}
