package com.pgkit.timemarkcamera;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

/** Ratio-aware portrait composition: tall photos use space behind capture controls. */
final class CameraViewportLayout extends FrameLayout {
    private PhotoAspectRatio ratio = PhotoAspectRatio.STANDARD;
    CameraViewportLayout(Context context) { super(context); }
    void setAspectRatio(PhotoAspectRatio value) { ratio = value; requestLayout(); }
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec), height = MeasureSpec.getSize(heightSpec);
        setMeasuredDimension(width, height);
        if (getChildCount() < 2) return;
        int panelMinimum = CameraUi.dp(getContext(), 200);
        int available = ratio == PhotoAspectRatio.TALL ? height : Math.max(1, height-panelMinimum);
        int previewWidth = Math.min(width, Math.round(available * ratio.width/(float)ratio.height));
        int previewHeight = Math.round(previewWidth * ratio.height/(float)ratio.width);
        getChildAt(0).measure(MeasureSpec.makeMeasureSpec(previewWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(previewHeight, MeasureSpec.EXACTLY));
        int panelHeight = ratio == PhotoAspectRatio.TALL ? Math.min(height, panelMinimum) : height-previewHeight;
        getChildAt(1).measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(panelHeight, MeasureSpec.EXACTLY));
    }
    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (getChildCount() < 2) return;
        View preview = getChildAt(0), panel = getChildAt(1);
        int x = (getWidth()-preview.getMeasuredWidth())/2;
        preview.layout(x, 0, x+preview.getMeasuredWidth(), preview.getMeasuredHeight());
        panel.layout(0, getHeight()-panel.getMeasuredHeight(), getWidth(), getHeight());
    }
}
