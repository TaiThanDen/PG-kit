package com.pgkit.timemarkcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Bounded subtitle-family experiment, compared with retained real JPEGs outside the app. */
final class SavedLogoTypefaceProbe {
    static void render(Context context, File root) throws IOException {
        String[] families = {"sans-serif", "sans-serif-light", "sans-serif-medium",
                "sans-serif-condensed", "sans-serif-condensed-light", "bundled-regular",
                "bundled-medium"};
        float[] sizes = {20f, 21.333334f, 22.222223f};
        String value = "100% Chân thực";
        for (String family : families) for (int sizeIndex = 0; sizeIndex < sizes.length; sizeIndex++) {
            Typeface typeface = family.startsWith("bundled-")
                    ? Typeface.createFromAsset(context.getAssets(), "fonts/RobotoCondensed-"
                            + (family.endsWith("medium") ? "Medium.ttf" : "Regular.ttf"))
                    : Typeface.create(family, Typeface.NORMAL);
            Bitmap bitmap = Bitmap.createBitmap(220, 100, Bitmap.Config.ARGB_8888);
            try {
                Canvas canvas = new Canvas(bitmap);
                canvas.drawColor(Color.BLACK);
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setTypeface(typeface);
                paint.setTextSize(sizes[sizeIndex]);
                paint.setColor(0xffbebebe);
                paint.setShadowLayer(3.111111f, 0, 1.333333f, 0x66000000);
                Rect bounds = new Rect();
                paint.getTextBounds(value, 0, value.length(), bounds);
                paint.setTextScaleX(155f / bounds.width());
                paint.getTextBounds(value, 0, value.length(), bounds);
                canvas.drawText(value, 204f - bounds.right, 82f - bounds.bottom, paint);
                try (FileOutputStream output = new FileOutputStream(new File(root,
                        "subtitle-" + family + "-" + sizeIndex + ".png"))) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
                }
            } finally {
                bitmap.recycle();
            }
        }
    }
}
