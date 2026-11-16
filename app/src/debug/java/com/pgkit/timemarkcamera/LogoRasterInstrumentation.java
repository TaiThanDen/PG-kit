package com.pgkit.timemarkcamera;

import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.widget.ImageView;
import java.lang.reflect.Method;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** Isolates the official logo's native view raster from camera, time and location. */
public final class LogoRasterInstrumentation extends Instrumentation {
    private Bundle options;
    @Override public void onCreate(Bundle arguments) { options = arguments; start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            int width = Integer.parseInt(options.getString("width", "1536"));
            int height = Integer.parseInt(options.getString("height", "2048"));
            float density = getTargetContext().getResources().getDisplayMetrics().density;
            int displayWidth = getTargetContext().getResources().getDisplayMetrics().widthPixels;
            int nativeSize = Math.round(100 * density);
            File root = new File(getTargetContext().getFilesDir(), "logo-probe");
            if (!root.exists() && !root.mkdirs()) throw new IllegalStateException("mkdir");
            Bitmap source;
            try (InputStream input = getTargetContext().getAssets().open("video/official_vi.png")) {
                source = BitmapFactory.decodeStream(input);
            }
            if (source == null) throw new IllegalStateException("Logo decode failed");
            Bitmap nativeLayer = Bitmap.createBitmap(nativeSize, nativeSize, Bitmap.Config.ARGB_8888);
            runOnMainSync(() -> {
                ImageView view = new ImageView(getTargetContext());
                view.setImageBitmap(source);
                view.layout(0, 0, nativeSize, nativeSize);
                view.draw(new Canvas(nativeLayer));
            });
            float size = nativeSize * (Math.min(width, height) / (float) displayWidth);
            for (int mode = 0; mode < 4; mode++) {
                Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                float offset = mode % 2 == 0 ? Math.min(width, height) / 1536f : 0;
                Canvas canvas = new Canvas(output);
                canvas.drawBitmap(mode < 2 ? source : nativeLayer, null,
                        new RectF(width - size + offset, height - size, width + offset, height),
                        new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
                save(output, new File(root, "logo-" + mode + ".png"));
                if (mode == 2) {
                    Bitmap production = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    try {
                        runOnMainSync(() -> WatermarkRenderer.loadWatermarkFonts(getTargetContext()));
                        Method draw = WatermarkRenderer.class.getDeclaredMethod("drawBrand",
                                Canvas.class, int.class, int.class, float.class);
                        draw.setAccessible(true);
                        draw.invoke(null, new Canvas(production), width, height,
                                WatermarkLayoutSpec.scaleFor(width, height));
                        if (!output.sameAs(production))
                            throw new AssertionError("Production logo differs from native ImageView raster");
                        save(production, new File(root, "logo-production.png"));
                    } finally {
                        production.recycle();
                    }
                }
                output.recycle();
            }
            save(nativeLayer, new File(root, "native-logo.png"));
            nativeLayer.recycle();
            source.recycle();
            result.putString("stream", "Logo fixtures; nativeSize=" + nativeSize
                    + ", displayWidth=" + displayWidth + ", outputSize=" + size);
            finish(-1, result);
        } catch (Exception | AssertionError error) {
            result.putString("stream", error.toString());
            finish(0, result);
        }
    }
    private static void save(Bitmap bitmap, File path) throws Exception {
        try (FileOutputStream output = new FileOutputStream(path)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                throw new IllegalStateException("PNG write failed");
        }
    }
}
