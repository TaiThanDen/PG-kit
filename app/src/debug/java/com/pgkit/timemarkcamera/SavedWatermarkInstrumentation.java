package com.pgkit.timemarkcamera;

import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
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
            String address = options.getString("address", "44/12 Tổ 44 KP3, Example District, Hồ Chí Minh");
            Calendar date = Calendar.getInstance();
            date.set(2026, Calendar.NOVEMBER, 15,
                    Integer.parseInt(options.getString("hour", "9")),
                    Integer.parseInt(options.getString("minute", "23")), 0);
            verifyMissingAddress(date.getTime());
            WatermarkTemplate.Snapshot fixtureTemplate = null;
            if ("false".equals(options.getString("location"))) {
                for (WatermarkTemplate candidate : WatermarkTemplate.defaults(getTargetContext())) {
                    if ("34".equals(candidate.id))
                        fixtureTemplate = candidate.defaultSnapshot().withField(2, false, "");
                }
                if (fixtureTemplate == null) throw new IllegalStateException("Missing template 34");
            }
            String nativeProbeAddress = fixtureTemplate == null ? address : "";
            java.util.Date fixtureWhen = date.getTime();
            for (int mode = 0; mode < 10; mode++) for (int background = 0; background < 3; background++) {
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawColor(background == 0 ? Color.BLACK
                        : background == 1 ? Color.WHITE : Color.TRANSPARENT);
                if (mode == 0 || mode == 1) {
                    WatermarkRenderer.drawSaved(canvas, width, height, date.getTime(), address,
                            "TESTCODE", fixtureTemplate);
                } else if (mode >= 6) {
                    int angle = (mode - 6) * 90;
                    runOnMainSync(() -> NativeTextWatermarkProbe.drawLayer(getTargetContext(), canvas,
                            width, height, clock, text, nativeProbeAddress, angle, fixtureWhen));
                } else if (mode == 5) {
                    // Diagnostic: stamp at the LG camera's 4032-pixel long side, then reduce.
                    float sourceScale = 4032f / Math.max(width, height);
                    Bitmap layer = Bitmap.createBitmap(Math.round(width * sourceScale),
                            Math.round(height * sourceScale), Bitmap.Config.ARGB_8888);
                    try {
                        WatermarkRenderer.drawSaved(new Canvas(layer), layer.getWidth(),
                                layer.getHeight(), date.getTime(), address, "TESTCODE", fixtureTemplate);
                        canvas.drawBitmap(layer, null, new Rect(0, 0, width, height),
                                new Paint(Paint.FILTER_BITMAP_FLAG));
                    } finally {
                        layer.recycle();
                    }
                } else if (mode == 4) {
                    runOnMainSync(() -> NativeTextWatermarkProbe.draw(getTargetContext(), canvas,
                            width, height, clock, text, nativeProbeAddress, fixtureWhen));
                } else if (mode == 3) {
                    // Diagnostic: distinguish scaled glyph rasterization from scaling a cached layer.
                    int nativeShortSide = Math.min(getTargetContext().getResources()
                            .getDisplayMetrics().widthPixels, getTargetContext().getResources()
                            .getDisplayMetrics().heightPixels);
                    float scale = Math.min(width, height) / (float) nativeShortSide;
                    Bitmap layer = Bitmap.createBitmap(Math.round(width / scale),
                            Math.round(height / scale), Bitmap.Config.ARGB_8888);
                    try {
                        WatermarkRenderer.drawSaved(new Canvas(layer), layer.getWidth(),
                                layer.getHeight(), date.getTime(), address, "TESTCODE", fixtureTemplate);
                        canvas.drawBitmap(layer, null, new Rect(0, 0, width, height),
                                new Paint(Paint.FILTER_BITMAP_FLAG));
                    } finally {
                        layer.recycle();
                    }
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
            try (FileOutputStream output = new FileOutputStream(new File(root, "native-metrics.txt"))) {
                output.write(NativeTextWatermarkProbe.metrics.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            SavedLogoTypefaceProbe.render(getTargetContext(), root);
            result.putString("stream", "Rendered watermark fixtures at " + root);
            finish(-1, result);
        } catch (Exception | AssertionError error) {
            result.putString("stream", error.toString());
            finish(0, result);
        }
    }

    private void verifyMissingAddress(java.util.Date when) {
        WatermarkTemplate.Snapshot disabled = null;
        for (WatermarkTemplate candidate : WatermarkTemplate.defaults(getTargetContext())) {
            if ("34".equals(candidate.id))
                disabled = candidate.defaultSnapshot().withField(2, false, "");
        }
        if (disabled == null) throw new IllegalStateException("Missing template 34");
        Bitmap expected = Bitmap.createBitmap(768, 1024, Bitmap.Config.ARGB_8888);
        Bitmap actual = Bitmap.createBitmap(768, 1024, Bitmap.Config.ARGB_8888);
        try {
            WatermarkRenderer.drawSaved(new Canvas(expected), 768, 1024, when, "", "TESTCODE", disabled);
            for (String missing : new String[]{null, "", "   "}) {
                actual.eraseColor(Color.TRANSPARENT);
                WatermarkRenderer.drawSaved(new Canvas(actual), 768, 1024, when, missing, "TESTCODE");
                if (!expected.sameAs(actual))
                    throw new AssertionError("Missing address reserves a blank watermark row");
            }
        } finally {
            actual.recycle();
            expected.recycle();
        }
    }
}
