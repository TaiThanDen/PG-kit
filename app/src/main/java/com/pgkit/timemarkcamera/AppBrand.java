package com.pgkit.timemarkcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/** One source image for the launcher, live preview and exported media. */
final class AppBrand {
    private AppBrand() {}

    static Bitmap load(Context context) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.app_icon, options);
        if (bitmap == null) throw new IllegalStateException("App logo could not be decoded");
        return bitmap;
    }

    static void draw(Canvas canvas, int width, int height, Bitmap bitmap) {
        if (bitmap == null || width <= 0 || height <= 0) return;
        float shortEdge = Math.min(width, height);
        float size = shortEdge * 0.09f;
        float right = width - shortEdge * 0.02f;
        // Leave room below the image for the saved-photo verification strip.
        float bottom = height - shortEdge * 0.04f;
        canvas.drawBitmap(bitmap, null, new RectF(right - size, bottom - size, right, bottom),
                new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
    }
}
