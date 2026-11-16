package com.pgkit.timemarkcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import java.io.IOException;
import java.io.InputStream;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

final class WatermarkRenderer {
    static final int YELLOW = 0xffffc233;
    /**
     * Output-photo logo coordinates are authored against the Timemark 4:3 reference images:
     * 1536x1152 in landscape and 1152x1536 in portrait.  They intentionally do not use
     * preview dp values or an independent horizontal/vertical percentage scale.
     */
    private static final int SAVED_YELLOW = Color.rgb(248, 194, 62);
    private static final int SAVED_SUBTITLE = Color.rgb(190, 190, 190);
    private static final float REFERENCE_WORDMARK_WIDTH = 177f;
    private static final Locale VIETNAMESE = new Locale("vi", "VN");
    private static final Typeface DEFAULT_TIME_TYPEFACE = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
    private static final Typeface DEFAULT_TEXT_TYPEFACE = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
    private static Typeface timeTypeface = DEFAULT_TIME_TYPEFACE;
    private static Typeface textTypeface = DEFAULT_TEXT_TYPEFACE;
    private static Typeface brandTypeface = DEFAULT_TEXT_TYPEFACE;
    private static Bitmap wordmarkBitmap;
    // Display metrics reproduce native dp rounding; JPEG bounds own the final scale.
    private static int nativeLayoutWidth = 1080;
    private static float nativeLayoutDensity = 450f / 160f;

    private WatermarkRenderer() {}

    static void loadWatermarkFonts(Context context) {
        android.util.DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        nativeLayoutWidth = Math.min(metrics.widthPixels, metrics.heightPixels);
        nativeLayoutDensity = metrics.density;
        try {
            timeTypeface = Typeface.createFromAsset(context.getAssets(),
                    "fonts/BigShouldersText-Medium.ttf");
        } catch (RuntimeException ignored) {
            timeTypeface = DEFAULT_TIME_TYPEFACE;
        }
        try {
            textTypeface = Typeface.createFromAsset(context.getAssets(),
                    "fonts/RobotoCondensed-Regular.ttf");
        } catch (RuntimeException ignored) {
            textTypeface = DEFAULT_TEXT_TYPEFACE;
        }
        try {
            brandTypeface = Typeface.createFromAsset(context.getAssets(),
                    "fonts/RobotoCondensed-Medium.ttf");
        } catch (RuntimeException ignored) {
            brandTypeface = DEFAULT_TEXT_TYPEFACE;
        }
        SavedVerificationRenderer.load(context);
        try (InputStream source = context.getAssets().open("video/official_vi.png")) {
            Bitmap original = BitmapFactory.decodeStream(source);
            if (original == null) throw new IOException("Official logo decode failed");
            // Timemark j.a draws the 100dp ImageView into a native-size bitmap first.
            // Scaling the 300px source straight onto the JPEG skips that raster pass:
            // subtitle strokes and antialiasing become visibly sharper/heavier.
            int nativeSize = Math.round(100f * nativeLayoutDensity);
            Bitmap nativeLogo = Bitmap.createBitmap(nativeSize, nativeSize,
                    Bitmap.Config.ARGB_8888);
            try {
                android.widget.ImageView logo = new android.widget.ImageView(context);
                logo.setImageBitmap(original);
                logo.layout(0, 0, nativeSize, nativeSize);
                logo.draw(new Canvas(nativeLogo));
            } finally {
                original.recycle();
            }
            if (wordmarkBitmap != null) wordmarkBitmap.recycle();
            wordmarkBitmap = nativeLogo;
        } catch (IOException ignored) {
            wordmarkBitmap = null;
        }
    }

    static String time(Date when) {
        return new SimpleDateFormat("HH:mm", Locale.US).format(when);
    }

    static String date(Date when) {
        Calendar calendar = Calendar.getInstance(VIETNAMESE);
        calendar.setTime(when);
        return String.format(VIETNAMESE, "%02d Tháng %d,%d",
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR));
    }

    static String weekday(Date when) {
        Calendar calendar = Calendar.getInstance(VIETNAMESE);
        calendar.setTime(when);
        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.SUNDAY: return "Chủ nhật";
            case Calendar.MONDAY: return "Thứ Hai";
            case Calendar.TUESDAY: return "Thứ Ba";
            case Calendar.WEDNESDAY: return "Thứ Tư";
            case Calendar.THURSDAY: return "Thứ Năm";
            case Calendar.FRIDAY: return "Thứ Sáu";
            default: return "Thứ Bảy";
        }
    }

    static void drawLive(Canvas canvas, int width, int height, Date when, String address,
                         float density, WatermarkTemplate.Snapshot template) {
        if (template != null && TemplateWatermarkRenderer.drawLive(
                canvas, width, height, when, address, density, template)) return;
        drawLive(canvas, width, height, when, address, density);
    }

    static void drawSaved(Canvas canvas, int width, int height, Date when, String address,
                          String verificationCode, WatermarkTemplate.Snapshot template) {
        if (template != null && "34".equals(template.id)
                && TemplateWatermarkRenderer.isBasic34(template)) {
            TimeLocationWatermark.drawExportStyled(canvas, width, height, nativeLayoutWidth,
                    nativeLayoutDensity, time(when), date(when), weekday(when), address,
                    timeTypeface, textTypeface, template.sizeScale, template.textColor,
                    template.accentColor, template.field(2) != null && template.field(2).enabled);
            drawSavedExtras(canvas, width, height, verificationCode);
            return;
        }
        if (template != null && TemplateWatermarkRenderer.drawSaved(
                canvas, width, height, when, address, template)) return;
        drawSaved(canvas, width, height, when, address, verificationCode);
    }

    static void drawLive(Canvas canvas, int width, int height, Date when, String address, float density) {
        TimeLocationWatermark.draw(canvas, width, height, density, time(when), date(when),
                weekday(when), address, timeTypeface, textTypeface);
    }

    static void drawStyled34(Canvas canvas, int width, int height, Date when, String address,
                             float density, WatermarkTemplate.Snapshot template) {
        TimeLocationWatermark.drawStyled(canvas, width, height, density, time(when), date(when),
                weekday(when), address, timeTypeface, textTypeface, template.sizeScale,
                template.textColor, template.accentColor,
                template.field(2) != null && template.field(2).enabled);
    }

    static void drawSaved(Bitmap bitmap, Date when, String address, String verificationCode) {
        drawSaved(new Canvas(bitmap), bitmap.getWidth(), bitmap.getHeight(), when, address,
                verificationCode);
    }

    /** Draws the exported watermark on any output canvas, including a CameraX video frame. */
    static void drawSaved(Canvas canvas, int width, int height, Date when, String address,
                          String verificationCode) {
        TimeLocationWatermark.drawExport(canvas, width, height, nativeLayoutWidth,
                nativeLayoutDensity, time(when), date(when), weekday(when), address,
                timeTypeface, textTypeface);
        drawSavedExtras(canvas, width, height, verificationCode);
    }

    private static void drawSavedExtras(Canvas canvas, int width, int height, String verificationCode) {
        float scale = WatermarkLayoutSpec.scaleFor(width, height);
        // The exported reference logo is a separate layer, smaller than the content text.
        drawBrand(canvas, width, height, scale);
        drawVerification(canvas, width, height, verificationCode);
    }

    private static void drawBrand(Canvas canvas, int width, int height, float scale) {
        if (wordmarkBitmap != null) {
            // This is the native 100dp raster, including padding, alpha and baked shadow.
            // JPEG projection is separate from preview and video geometry.
            float outputScale = Math.min(width, height) / 1536f;
            float size = WatermarkLayoutSpec.savedBrandSize(width, height,
                    nativeLayoutWidth, nativeLayoutDensity);
            RectF destination = new RectF(width - size + outputScale, height - size,
                    width + outputScale, height);
            canvas.drawBitmap(wordmarkBitmap, null, destination,
                    new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        } else {
            drawFallbackWordmark(canvas, width, height, scale * 2f / 3f);
            drawBrandSubtitle(canvas, width, height, Math.min(width, height) / 1536f);
        }
    }

    private static void drawFallbackWordmark(Canvas canvas, int width, int height, float scale) {
        Paint timeLogo = savedPaint(SAVED_YELLOW, 40f * scale, brandTypeface, scale);
        timeLogo.setTextAlign(Paint.Align.LEFT);
        Rect wordmarkBounds = textBounds("Timemark", timeLogo);
        timeLogo.setTextScaleX(REFERENCE_WORDMARK_WIDTH * scale
                / Math.max(1f, wordmarkBounds.width()));
        Paint markLogo = new Paint(timeLogo);
        markLogo.setColor(Color.WHITE);
        float timeWidth = timeLogo.measureText("Time");
        wordmarkBounds = textBounds("Timemark", timeLogo);
        float wordmarkBaseline = baselineForTextBottom("Timemark", timeLogo,
                height - WatermarkLayoutSpec.WORDMARK_BOTTOM * scale);
        float wordmarkLeft = width - WatermarkLayoutSpec.WORDMARK_RIGHT * scale - wordmarkBounds.right;
        drawText(canvas, "Time", wordmarkLeft, wordmarkBaseline, timeLogo);
        drawText(canvas, "mark", wordmarkLeft + timeWidth, wordmarkBaseline, markLogo);

    }

    private static void drawBrandSubtitle(Canvas canvas, int width, int height, float scale) {
        Paint subtitle = savedPaint(SAVED_SUBTITLE, 20f * scale,
                Typeface.create("sans-serif-medium", Typeface.NORMAL), scale);
        subtitle.setShadowLayer(3.111111f * scale, 0f, 1.333333f * scale, 0x66000000);
        Rect subtitleBounds = textBounds("100% Chân thực", subtitle);
        subtitle.setTextScaleX(155f * scale
                / Math.max(1f, subtitleBounds.width()));
        subtitleBounds = textBounds("100% Chân thực", subtitle);
        float subtitleLeft = width - 16f * scale - subtitleBounds.right;
        drawText(canvas, "100% Chân thực", subtitleLeft,
                baselineForTextBottom("100% Chân thực", subtitle,
                        height - 18f * scale), subtitle);
    }

    private static void drawVerification(Canvas canvas, int width, int height,
                                         String code) {
        SavedVerificationRenderer.draw(canvas, width, height, nativeLayoutWidth,
                nativeLayoutDensity, code);
    }

    private static Paint paint(int color, float textSize, Typeface typeface) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        paint.setColor(color);
        paint.setTextSize(textSize);
        paint.setTypeface(typeface);
        return paint;
    }

    private static Paint savedPaint(int color, float textSize, Typeface typeface, float scale) {
        return paint(color, textSize, typeface);
    }

    private static void drawText(Canvas canvas, String text, float x, float y, Paint paint) {
        canvas.drawText(text, x, y, paint);
    }

    private static float baselineForTextBottom(String text, Paint paint, float bottom) {
        Rect bounds = textBounds(text, paint);
        return bottom - bounds.bottom;
    }

    private static Rect textBounds(String text, Paint paint) {
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        return bounds;
    }

}
