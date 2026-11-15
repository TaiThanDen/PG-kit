package com.pgkit.timemarkcamera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.RectF;
import android.graphics.Matrix;
import android.text.Layout;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

/** Debug hypothesis: use TextView's own layout selection rather than StaticLayout everywhere. */
final class NativeTextWatermarkProbe {
    static String metrics;
    static void draw(Context context, Canvas canvas, int width, int height,
                     Typeface clockFont, Typeface textFont, String address, java.util.Date when) {
        draw(context, canvas, width, height, clockFont, textFont, address, false, 0, when);
    }

    static void drawLayer(Context context, Canvas canvas, int width, int height,
                          Typeface clockFont, Typeface textFont, String address, int angle,
                          java.util.Date when) {
        draw(context, canvas, width, height, clockFont, textFont, address, true, angle, when);
    }

    private static void draw(Context context, Canvas canvas, int width, int height,
                             Typeface clockFont, Typeface textFont, String address,
                             boolean layerMode, int angle, java.util.Date when) {
        float density = context.getResources().getDisplayMetrics().density;
        int nativeWidth = Math.min(context.getResources().getDisplayMetrics().widthPixels,
                context.getResources().getDisplayMetrics().heightPixels);
        float normalization = nativeWidth / density / 375f;
        float scale = Math.min(width, height) / (float) nativeWidth;
        int nativeHeight = Math.round(height / scale);
        TextView clock = text(context, WatermarkRenderer.time(when), Math.round(45 * density) * normalization,
                clockFont, false, false, 0);
        TextView date = text(context, WatermarkRenderer.date(when), Math.round(14 * density) * normalization,
                textFont, true, true, 0);
        TextView weekday = text(context, WatermarkRenderer.weekday(when), Math.round(14 * density) * normalization,
                textFont, true, true, 0);
        int locationInset = Math.round(3 * density);
        TextView location = text(context, address, Math.round(15 * density) * normalization,
                textFont, true, true, Math.min((int) (Math.round(245 * density) * normalization),
                        Math.round(260 * density)) - locationInset);
        int margin = Math.round(8 * density);
        int inset = Math.round(4 * density);
        int headerHeight = Math.max(clock.getHeight(), date.getHeight() + weekday.getHeight()
                + 2 * inset);
        boolean showLocation = address != null && !address.trim().isEmpty();
        int rootHeight = margin * 2 + headerHeight + (showLocation ? location.getHeight() : 0);
        int rootWidth = margin * 2 + (int) (Math.round(245 * density) * normalization);
        Bitmap layer = null;
        Canvas destinationCanvas = canvas;
        int rotatedWidth = angle % 180 == 0 ? rootWidth : rootHeight;
        int rotatedHeight = angle % 180 == 0 ? rootHeight : rootWidth;
        if (layerMode) {
            // framework.util.n.d floors the scaled width and derives height from its aspect ratio.
            int bitmapWidth = (int) (rotatedWidth * scale);
            int bitmapHeight = (int) (bitmapWidth / (rotatedWidth / (float) rotatedHeight));
            layer = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
            canvas = new Canvas(layer);
            nativeHeight = rootHeight;
        }
        int top = nativeHeight - margin - headerHeight - (showLocation ? location.getHeight() : 0);
        int dividerX = margin + clock.getWidth() + Math.round(10 * density);
        int dividerWidth = Math.round(2 * density);
        int dividerHeight = (int) (Math.round(38 * density) * normalization);
        int detailX = dividerX + dividerWidth + Math.round(6 * density);
        metrics = "nativeWidth=" + nativeWidth + ", density=" + density
                + ", normalization=" + normalization + ", scale=" + scale
                + ", clock=" + clock.getWidth() + "x" + clock.getHeight()
                + ", date=" + date.getWidth() + "x" + date.getHeight()
                + ", weekday=" + weekday.getWidth() + "x" + weekday.getHeight()
                + ", location=" + location.getWidth() + "x" + location.getHeight()
                + ", clockSize=" + clock.getTextSize() + ", dateSize=" + date.getTextSize()
                + ", headerHeight=" + headerHeight + ", top=" + top
                + ", detailX=" + detailX;
        int save = canvas.save();
        float jpegInset = Math.min(width, height) / (width > height ? 768f : 1536f);
        if (!layerMode) canvas.translate(jpegInset, 0);
        canvas.scale(scale, scale);
        if (layerMode && angle != 0) {
            // REA RotateLayout.onLayout centers its child using integer division.
            canvas.rotate(-angle, rotatedWidth / 2f, rotatedHeight / 2f);
            canvas.translate((rotatedWidth - rootWidth) / 2, (rotatedHeight - rootHeight) / 2);
        }
        clock.getPaint().setStyle(Paint.Style.STROKE);
        clock.getPaint().setStrokeWidth(Math.round(.5f * density));
        clock.setTextColor(0x80000000);
        drawView(canvas, clock, margin, top);
        clock.getPaint().setStyle(Paint.Style.FILL);
        clock.setTextColor(Color.WHITE);
        drawView(canvas, clock, margin, top);
        Paint paint = new Paint();
        paint.setColor(WatermarkRenderer.YELLOW);
        int dividerTop = top + Math.round((headerHeight - dividerHeight) / 2f);
        canvas.drawRect(dividerX, dividerTop, dividerX + dividerWidth,
                dividerTop + dividerHeight, paint);
        drawView(canvas, date, detailX, top + inset);
        drawView(canvas, weekday, detailX, top + headerHeight - inset - weekday.getHeight());
        if (showLocation) drawView(canvas, location, margin + locationInset, top + headerHeight);
        canvas.restoreToCount(save);
        if (layer != null) {
            try {
                if (angle != 0) {
                    Matrix matrix = new Matrix();
                    matrix.setRotate(angle);
                    Bitmap rotated = Bitmap.createBitmap(layer, 0, 0, layer.getWidth(),
                            layer.getHeight(), matrix, true);
                    if (rotated != layer) layer.recycle();
                    layer = rotated;
                }
                float layerTop = height - (angle == 0 ? rootHeight * scale : layer.getHeight());
                destinationCanvas.drawBitmap(layer, null, new RectF(jpegInset, layerTop,
                                jpegInset + layer.getWidth(), layerTop + layer.getHeight()),
                        new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            } finally {
                layer.recycle();
            }
        }
    }

    private static TextView text(Context context, String value, float size, Typeface font,
                                 boolean padding, boolean shadow, int width) {
        TextView view = new TextView(context);
        view.setPadding(0, 0, 0, 0);
        view.setTypeface(font);
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        view.setTextColor(Color.WHITE);
        view.setIncludeFontPadding(padding);
        view.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE);
        if (shadow) view.setShadowLayer(3.5f, 0, 1.5f, 0x66000000);
        view.setText(value);
        view.measure(View.MeasureSpec.makeMeasureSpec(width > 0 ? width : 4096,
                        width > 0 ? View.MeasureSpec.EXACTLY : View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        return view;
    }

    private static void drawView(Canvas canvas, View view, int x, int y) {
        int save = canvas.save();
        canvas.translate(x, y);
        view.draw(canvas);
        canvas.restoreToCount(save);
    }
}
