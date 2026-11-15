package com.pgkit.timemarkcamera;

import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import java.io.File;
import java.io.FileOutputStream;

/** Fixed debug inputs for absolute-position edge-strip comparisons, without camera scene changes. */
public final class VerificationStripInstrumentation extends Instrumentation {
    private Bundle arguments;

    @Override public void onCreate(Bundle options) {
        arguments = options == null ? new Bundle() : options;
        start();
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            SavedVerificationRenderer.load(getTargetContext());
            DisplayMetrics metrics = getTargetContext().getResources().getDisplayMetrics();
            int layoutWidth = Integer.parseInt(arguments.getString("layoutWidth",
                    Integer.toString(Math.min(metrics.widthPixels, metrics.heightPixels))));
            float density = Float.parseFloat(arguments.getString("density",
                    Float.toString(metrics.density)));
            File root = new File(getTargetContext().getFilesDir(), "verification-strip-probe");
            if (!root.exists() && !root.mkdirs()) throw new IllegalStateException("mkdir");
            String[] names = {"portrait", "landscape-top-left", "landscape-top-right", "inverted"};
            String[] codes = {"A9NMWWRKXXAXAX", "G6RDUMGNB3YXNH", "MBDUMA92P4XEK4", "TX63LHLBHGA3KT"};
            for (int direction = 0; direction < names.length; direction++) {
                int width = direction == 0 || direction == 3 ? 1536 : 2048;
                int height = direction == 0 || direction == 3 ? 2048 : 1536;
                for (int background = 0; background < 3; background++) {
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    try {
                        Canvas canvas = new Canvas(bitmap);
                        canvas.drawColor(background == 0 ? Color.TRANSPARENT
                                : background == 1 ? Color.BLACK : Color.WHITE);
                        SavedVerificationRenderer.draw(canvas, width, height, layoutWidth, density,
                                codes[direction]);
                        if (background != 0) verifyContrast(bitmap, background == 2);
                        String name = names[direction] + "-" + background;
                        write(bitmap, new File(root, name + ".png"), Bitmap.CompressFormat.PNG, 100);
                        if (background != 0)
                            write(bitmap, new File(root, name + ".jpg"), Bitmap.CompressFormat.JPEG, 99);
                    } finally {
                        bitmap.recycle();
                    }
                }
            }
            result.putString("stream", "PASS: verification strip PNG/JPEG fixtures, white/black contrast: " + root);
            finish(-1, result);
        } catch (Exception | AssertionError error) {
            result.putString("stream", android.util.Log.getStackTraceString(error));
            finish(0, result);
        }
    }

    private static void write(Bitmap bitmap, File file, Bitmap.CompressFormat format, int quality)
            throws java.io.IOException {
        try (FileOutputStream output = new FileOutputStream(file)) {
            if (!bitmap.compress(format, quality, output)) throw new java.io.IOException("compress");
        }
    }

    private static void verifyContrast(Bitmap bitmap, boolean white) {
        int visible = 0;
        for (int y = bitmap.getHeight() / 5; y < bitmap.getHeight() * 4 / 5; y++) {
            for (int x = bitmap.getWidth() - 80; x < bitmap.getWidth(); x++) {
                int color = bitmap.getPixel(x, y);
                int level = Math.min(Color.red(color), Math.min(Color.green(color), Color.blue(color)));
                if (white ? level < 220 : level > 220) visible++;
            }
        }
        if (visible < 100) throw new AssertionError("Verification strip disappears on "
                + (white ? "white" : "black"));
    }
}
