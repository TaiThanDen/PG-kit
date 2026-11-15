package com.pgkit.timemarkcamera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.camera.view.PreviewView;

/** Converts PreviewView touch input into camera actions without owning camera state. */
final class CameraGestureController {
    interface Listener {
        void onTapToFocus(float x, float y);
        void onPinchZoom(float scaleFactor);
    }

    private final GestureDetector tapDetector;
    private final ScaleGestureDetector scaleDetector;
    private float pendingFocusX = Float.NaN;
    private float pendingFocusY = Float.NaN;
    private boolean hadMultiplePointers;

    @SuppressLint("ClickableViewAccessibility")
    CameraGestureController(Context context, PreviewView previewView, Listener listener) {
        previewView.setOnClickListener(view -> {
            float x = Float.isFinite(pendingFocusX) ? pendingFocusX : view.getWidth() / 2f;
            float y = Float.isFinite(pendingFocusY) ? pendingFocusY : view.getHeight() / 2f;
            pendingFocusX = Float.NaN;
            pendingFocusY = Float.NaN;
            listener.onTapToFocus(x, y);
        });
        tapDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onDown(MotionEvent event) {
                        return true;
                    }

                    @Override public boolean onSingleTapUp(MotionEvent event) {
                        pendingFocusX = event.getX();
                        pendingFocusY = event.getY();
                        previewView.performClick();
                        return true;
                    }

                });
        scaleDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override public boolean onScale(ScaleGestureDetector detector) {
                        listener.onPinchZoom(detector.getScaleFactor());
                        return true;
                    }
                });

        previewView.setOnTouchListener((view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                hadMultiplePointers = false;
            }
            if (event.getPointerCount() > 1) {
                hadMultiplePointers = true;
                // Cancel the pending tap before feeding the rest of a pinch to the scaler.
                MotionEvent cancel = MotionEvent.obtain(event);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                tapDetector.onTouchEvent(cancel);
                cancel.recycle();
            }
            boolean scaling = scaleDetector.onTouchEvent(event);
            boolean tapping = false;
            if (!hadMultiplePointers && !scaleDetector.isInProgress()) {
                tapping = tapDetector.onTouchEvent(event);
            }
            return scaling || tapping;
        });
    }
}
