package com.pgkit.timemarkcamera;

import android.content.Context;
import android.widget.FrameLayout;

/** Keeps the camera viewport at the reference's portrait 3:4 ratio when space permits. */
final class PortraitPreviewFrame extends FrameLayout {
    PortraitPreviewFrame(Context context) {
        super(context);
    }

    private PhotoAspectRatio ratio = PhotoAspectRatio.STANDARD;

    void setAspectRatio(PhotoAspectRatio ratio) {
        this.ratio = ratio;
        requestLayout();
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        if (MeasureSpec.getMode(widthSpec) == MeasureSpec.EXACTLY
                && MeasureSpec.getMode(heightSpec) == MeasureSpec.EXACTLY) {
            super.onMeasure(widthSpec, heightSpec);
            return;
        }
        int width = MeasureSpec.getSize(widthSpec);
        int desired = Math.round(width * ratio.height / (float) ratio.width);
        if (MeasureSpec.getMode(heightSpec) != MeasureSpec.UNSPECIFIED) {
            // Leave the capture controls usable on shorter screens and in resized windows.
            int controls = Math.round(200 * getResources().getDisplayMetrics().density);
            desired = Math.min(desired, Math.max(0, MeasureSpec.getSize(heightSpec) - controls));
        }
        width = Math.min(width, Math.round(desired * ratio.width / (float) ratio.height));
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(desired, MeasureSpec.EXACTLY));
    }
}
