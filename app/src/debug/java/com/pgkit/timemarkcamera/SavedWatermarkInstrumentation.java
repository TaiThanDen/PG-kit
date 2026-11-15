package com.pgkit.timemarkcamera;

import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;

/** Debug-only Android rasterizer fixture. Fixed inputs never enter the release app. */
public final class SavedWatermarkInstrumentation extends Instrumentation {
    private Bundle options;
    @Override public void onCreate(Bundle arguments) { options = arguments; start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            WatermarkRenderer.loadWatermarkFonts(getTargetContext());
            Typeface clock = Typeface.createFromAsset(getTargetContext().getAssets(),
                    "fonts/BigShouldersText-Medium.ttf");
            Typeface text = Typeface.createFromAsset(getTargetContext().getAssets(),
                    "fonts/RobotoCondensed-Regular.ttf");
            int width = Integer.parseInt(options.getString("width", "1536"));
            int height = Integer.parseInt(options.getString("height", "2048"));
            File root = new File(getTargetContext().getFilesDir(), "watermark-probe");
            if (!root.exists() && !root.mkdirs()) throw new IllegalStateException("mkdir");
            String address = "44/12 Tổ 44 KP3, Example District, Hồ Chí Minh";
            Calendar date = Calendar.getInstance();
            date.set(2026, Calendar.NOVEMBER, 15, 9, 23, 0);
            for (int mode = 0; mode < 3; mode++) for (int background = 0; background < 3; background++) {
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawColor(background == 0 ? Color.BLACK
                        : background == 1 ? Color.WHITE : Color.TRANSPARENT);
                if (mode == 0 || mode == 1) {
                    WatermarkRenderer.drawSaved(canvas, width, height, date.getTime(), address, "TESTCODE");
                } else {
                    float base = mode == 1 ? 1440 : 1536;
                    float scale = Math.min(width, height) / base;
                    canvas.scale(scale, scale);
                    TimeLocationWatermark.draw(canvas, (int) base, Math.round(height / scale),
                            401f / 160f, "09:23", "15 Tháng 11,2026", "Chủ nhật", address, clock, text);
                }
                if (mode == 1 && background == 1) {
                    int darkPixels = 0;
                    int shortSide = Math.min(width, height);
                    int left = Math.round(shortSide * .02f);
                    int right = Math.round(shortSide * .20f);
                    int top = height - Math.round(shortSide * .26f);
                    int bottom = height - Math.round(shortSide * .12f);
                    for (int y = top; y < bottom; y++) for (int x = left; x < right; x++) {
                        int color = bitmap.getPixel(x, y);
                        if (Math.min(Color.red(color), Math.min(Color.green(color),
                                Color.blue(color))) < 220) darkPixels++;
                    }
                    if (darkPixels < 100) throw new AssertionError("Clock disappears on white background");
                    result.putInt("clockOutlinePixels", darkPixels);
                }
                try (FileOutputStream output = new FileOutputStream(new File(root,
                        width + "-" + height + "-" + mode + "-" + background + ".png"))) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
                }
                bitmap.recycle();
            }
            result.putString("stream", "Rendered watermark fixtures at " + root);
            finish(-1, result);
        } catch (Exception | AssertionError error) {
            result.putString("stream", error.toString());
            finish(0, result);
        }
    }
}
