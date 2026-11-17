package com.pgkit.timemarkcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import java.io.IOException;
import java.io.InputStream;

/** Saved-photo edge strip, reconstructed from v10.0.316 BottomToTopTextView. */
final class SavedVerificationRenderer {
    private static Typeface codeTypeface = Typeface.MONOSPACE;
    private static Typeface labelTypeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
    private static Bitmap icon;

    private SavedVerificationRenderer() {}

    static void load(Context context) {
        codeTypeface = Typeface.createFromAsset(context.getAssets(), "fonts/PTMono-Regular.ttf");
        labelTypeface = Typeface.createFromAsset(context.getAssets(), "fonts/RobotoCondensed-Regular.ttf");
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                int targetDensity = context.getResources().getDisplayMetrics().densityDpi;
                icon = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getAssets(),
                        "verification/anti_code_icon_small.webp"), (decoder, info, source) -> {
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                    decoder.setTargetSize(Math.round(info.getSize().getWidth() * targetDensity / 480f),
                            Math.round(info.getSize().getHeight() * targetDensity / 480f));
                });
                icon.setDensity(targetDensity);
                return;
            } catch (IOException ignored) {
                // Android 8 and decode failures retain the BitmapFactory resource-density path.
            }
        }
        try (InputStream input = context.getAssets().open("verification/anti_code_icon_small.webp")) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inDensity = 480; // The original drawable is in drawable-xxhdpi.
            options.inTargetDensity = context.getResources().getDisplayMetrics().densityDpi;
            options.inScaled = true;
            icon = BitmapFactory.decodeStream(input, null, options);
        } catch (IOException ignored) {
            icon = null;
        }
    }

    static void draw(Canvas canvas, int width, int height, int layoutWidth, float density,
                     String code) {
        if (code == null || code.isEmpty()) return;
        // The original captures the native view layer, then scales that layer to the JPEG.
        float scale = Math.min(width, height) / (float) layoutWidth;
        // TextView reads XML textSize with getDimensionPixelSize before the custom scale.
        float textSize = Math.round(9f * density) * layoutWidth / (375f * density);
        Paint codePaint = textPaint(codeTypeface, textSize);
        Paint labelPaint = textPaint(labelTypeface, textSize);
        String label = "Timemark Verified";
        int codeWidth = measuredWidth(codePaint);
        int labelWidth = measuredWidth(labelPaint);
        int stripWidth = Math.max(codeWidth, labelWidth);
        int layerHeight = Math.round(height / scale);
        Bitmap layer = Bitmap.createBitmap(stripWidth, layerHeight, Bitmap.Config.ARGB_8888);
        try {
            Canvas nativeCanvas = new Canvas(layer);
            int codeHeight = measuredHeight(code, codePaint);
            int codeTop = (layerHeight - codeHeight) / 2;
            int labelHeight = measuredHeight(label, labelPaint);
            drawCharacters(nativeCanvas, code, codePaint, codeWidth, codeHeight, codeTop, 8);
            drawCharacters(nativeCanvas, label, labelPaint, labelWidth, labelHeight,
                    codeTop - Math.round(2f * density) - labelHeight, 6);
            if (icon != null) {
                int iconWidth = Math.round(8f * density);
                int iconHeight = Math.round(9f * density);
                // ConstraintLayout rounds the centered ImageView's native-pixel position.
                float iconLeft = Math.round((codeWidth - iconWidth - Math.round(density)) / 2f);
                float iconTop = codeTop + codeHeight + Math.round(density);
                nativeCanvas.save();
                nativeCanvas.rotate(-90f, iconLeft + iconWidth / 2f,
                        iconTop + iconHeight / 2f);
                nativeCanvas.drawBitmap(icon, null,
                        new RectF(iconLeft, iconTop, iconLeft + iconWidth, iconTop + iconHeight),
                        new Paint(Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG));
                nativeCanvas.restore();
            }
            canvas.drawBitmap(layer, null,
                    new RectF(width - stripWidth * scale, 0, width, layerHeight * scale),
                    new Paint(Paint.FILTER_BITMAP_FLAG));
        } finally {
            layer.recycle();
        }
    }

    private static Paint textPaint(Typeface typeface, float size) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(typeface);
        paint.setTextSize(size);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        return paint;
    }

    private static int measuredWidth(Paint paint) {
        Paint.FontMetrics metrics = paint.getFontMetrics();
        return (int) (metrics.bottom - metrics.top);
    }

    private static int measuredHeight(String text, Paint paint) {
        float advance = 0f;
        for (int i = 0; i < text.length(); i++) advance += paint.measureText(text.substring(i, i + 1));
        return (int) (advance + 10f);
    }

    private static void drawCharacters(Canvas canvas, String text, Paint fill, int width,
                                       int height, int top, int offset) {
        Paint stroke = new Paint(fill);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2f);
        stroke.setColor(0x80000000);
        float centerY = top + height - fill.getFontMetrics().bottom - 5f;
        float baselineX = width / 2f + offset;
        for (int i = 0; i < text.length(); i++) {
            String character = text.substring(i, i + 1);
            canvas.save();
            canvas.rotate(-90f, baselineX, centerY);
            canvas.drawText(character, baselineX, centerY, stroke);
            canvas.drawText(character, baselineX, centerY, fill);
            canvas.restore();
            float next = i + 1 < text.length() ? fill.measureText(text.substring(i + 1, i + 2)) : 0f;
            centerY -= (fill.measureText(character) + next) / 2f;
        }
    }
}
