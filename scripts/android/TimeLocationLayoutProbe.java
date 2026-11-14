package com.pgkit.timemarkcamera;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import java.io.File;
import java.io.FileOutputStream;

/** Shell-only fixture exercising the application's actual layout, with reference input strings.
 * Compile after assembleDebug, then D8 this probe and the application's TimeLocationWatermark
 * classes together. Run that DEX JAR with app_process; installing an APK is not required.
 * This class is outside app/src and cannot replace production clock or location data.
 */
public final class TimeLocationLayoutProbe {
    public static void main(String[] args) throws Exception {
        File root = new File(args[0]);
        Typeface clock = Typeface.createFromFile(new File(root, "BigShouldersText-Medium.ttf"));
        Typeface text = Typeface.createFromFile(new File(root, "RobotoCondensed-Regular.ttf"));
        String[] locations = {
                "234 Example Street, Example District, Hồ Chí Minh",
                "Hồ Chí Minh",
                "",
                "234 Example Street, Example District, Hồ Chí Minh, Việt Nam"
        };
        for (int i = 0; i < locations.length; i++) {
            Bitmap bitmap = Bitmap.createBitmap(1080, 1440, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.BLACK);
            TimeLocationWatermark.draw(canvas, 1080, 1440, 450f / 160f, "15:32",
                    "14 Tháng 11,2026", "Thứ Bảy", locations[i], clock, text);
            try (FileOutputStream output = new FileOutputStream(new File(root, "layout-" + i + ".png"))) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
            }
            bitmap.recycle();
        }
        System.out.println("Rendered 4 template-34 layout fixtures using application code.");
    }
}
