package com.pgkit.timemarkcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

final class WatermarkRenderer {
    static final int YELLOW = 0xffffc233;
    private static final Locale VIETNAMESE = new Locale("vi", "VN");
    private static final Typeface DEFAULT_TIME_TYPEFACE = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
    private static final Typeface DEFAULT_TEXT_TYPEFACE = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
    private static Typeface timeTypeface = DEFAULT_TIME_TYPEFACE;
    private static Typeface textTypeface = DEFAULT_TEXT_TYPEFACE;
    private static Bitmap appLogoBitmap;
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
        SavedVerificationRenderer.load(context);
        if (appLogoBitmap != null) appLogoBitmap.recycle();
        appLogoBitmap = AppBrand.load(context);
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
                canvas, width, height, when, address, density, template)) {
            if (!"none".equals(template.id)) AppBrand.draw(canvas, width, height, appLogoBitmap);
            return;
        }
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
                canvas, width, height, when, address, template)) {
            if (!"none".equals(template.id)) AppBrand.draw(canvas, width, height, appLogoBitmap);
            return;
        }
        drawSaved(canvas, width, height, when, address, verificationCode);
    }

    static void drawLive(Canvas canvas, int width, int height, Date when, String address, float density) {
        TimeLocationWatermark.draw(canvas, width, height, density, time(when), date(when),
                weekday(when), address, timeTypeface, textTypeface);
        AppBrand.draw(canvas, width, height, appLogoBitmap);
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
        AppBrand.draw(canvas, width, height, appLogoBitmap);
        drawVerification(canvas, width, height, verificationCode);
    }

    private static void drawVerification(Canvas canvas, int width, int height,
                                         String code) {
        SavedVerificationRenderer.draw(canvas, width, height, nativeLayoutWidth,
                nativeLayoutDensity, code);
    }

}
