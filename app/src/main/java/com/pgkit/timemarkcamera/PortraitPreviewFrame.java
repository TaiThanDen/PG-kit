package com.pgkit.timemarkcamera;

import android.content.Context;
import android.widget.FrameLayout;

/** Keeps the camera viewport at the reference's portrait 3:4 ratio when space permits. */
final class PortraitPreviewFrame extends FrameLayout {
    PortraitPreviewFrame(Context context) {
        super(context);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int desired = Math.round(width * 4f / 3f);
        if (MeasureSpec.getMode(heightSpec) != MeasureSpec.UNSPECIFIED) {
            // Leave the capture controls usable on shorter screens and in resized windows.
            int controls = Math.round(200 * getResources().getDisplayMetrics().density);
            desired = Math.min(desired, Math.max(0, MeasureSpec.getSize(heightSpec) - controls));
        }
        super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(desired, MeasureSpec.EXACTLY));
    }
}
