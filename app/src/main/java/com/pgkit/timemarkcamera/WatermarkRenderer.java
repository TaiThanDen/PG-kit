package com.pgkit.timemarkcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class WatermarkRenderer {
    static final int YELLOW = Color.rgb(255, 194, 51);
    private static final Locale VIETNAMESE = new Locale("vi", "VN");
    private static final Typeface DEFAULT_TIME_TYPEFACE = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
    private static final Typeface DEFAULT_TEXT_TYPEFACE = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
    private static Typeface timeTypeface = DEFAULT_TIME_TYPEFACE;
    private static Typeface textTypeface = DEFAULT_TEXT_TYPEFACE;
    private static Typeface brandTypeface = DEFAULT_TEXT_TYPEFACE;
    private static Typeface numbersTypeface = DEFAULT_TEXT_TYPEFACE;
    private static Bitmap shieldBitmap;

    private WatermarkRenderer() {}

    static void loadWatermarkFonts(Context context) {
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
        try {
            numbersTypeface = context.getResources().getFont(R.font.roboto_medium_numbers);
        } catch (RuntimeException ignored) {
            numbersTypeface = DEFAULT_TEXT_TYPEFACE;
        }
        shieldBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.watermark_shield);
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
            case Calendar.SUNDAY: return "Chủ Nhật";
            case Calendar.MONDAY: return "Thứ Hai";
            case Calendar.TUESDAY: return "Thứ Ba";
            case Calendar.WEDNESDAY: return "Thứ Tư";
            case Calendar.THURSDAY: return "Thứ Năm";
            case Calendar.FRIDAY: return "Thứ Sáu";
            default: return "Thứ Bảy";
        }
    }

    static void drawLive(Canvas canvas, int width, int height, Date when, String address, float density) {
        float margin = 8f * density;
        float addressSize = 14f * density;
        float bottom = height - margin;
        Paint addressPaint = paint(Color.WHITE, addressSize, textTypeface);
        addressPaint.setShadowLayer(3.5f * density, 0f, 1.5f * density, 0x66000000);
        float addressMaxWidth = Math.min(width - margin * 2f, 260f * density);
        List<String> lines = wrap(address, addressPaint, addressMaxWidth);
        int lineCount = Math.min(2, lines.size());
        float addressLineHeight = 16f * density;
        float addressTop = bottom - (lineCount - 1) * addressLineHeight;
        for (int i = 0; i < lineCount; i++) {
            drawText(canvas, lines.get(i), margin,
                    addressTop + i * addressLineHeight, addressPaint);
        }

        float timeBaseline = addressTop - 18f * density;
        Paint timePaint = paint(Color.WHITE, 38f * density, timeTypeface);
        timePaint.setShadowLayer(3.5f * density, 0f, 1.5f * density, 0x66000000);
        String time = time(when);
        drawText(canvas, time, margin, timeBaseline, timePaint);

        float timeWidth = timePaint.measureText(time);
        float dividerX = margin + timeWidth + 8f * density;
        Paint divider = paint(YELLOW, 2f * density, textTypeface);
        divider.setStrokeWidth(2f * density);
        canvas.drawLine(dividerX, timeBaseline - 38f * density + 5f * density,
                dividerX, timeBaseline + 3f * density, divider);

        Paint detailPaint = paint(Color.WHITE, 16f * density, textTypeface);
        detailPaint.setShadowLayer(3.5f * density, 0f, 1.5f * density, 0x66000000);
        float detailX = dividerX + 8f * density;
        drawText(canvas, date(when), detailX, timeBaseline - 18f * density, detailPaint);
        drawText(canvas, weekday(when), detailX, timeBaseline, detailPaint);
    }

    static void drawSaved(Bitmap bitmap, Date when, String address, String verificationCode) {
        Canvas canvas = new Canvas(bitmap);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        boolean landscape = width > height;
        float scale = landscape ? width / 2048f : width / 945f;
        if (landscape) {
            drawLandscapeSaved(canvas, width, height, when, address, verificationCode, scale);
            return;
        }
        float margin = (landscape ? 38f : 25f) * scale;
        float addressSize = (landscape ? 47f : 34f) * scale;
        float bottom = height - (landscape ? 50f : 34f) * scale;
        float reservedRight = landscape ? 650f * scale : 290f * scale;

        Paint addressPaint = paint(Color.WHITE, addressSize, textTypeface);
        addressPaint.setShadowLayer(3.5f * scale, 0f, 1.5f * scale, 0x66000000);
        List<String> lines = wrap(address, addressPaint, width - margin - reservedRight);
        int lineCount = Math.min(2, lines.size());
        float addressTop = bottom - (lineCount - 1) * addressSize * 1.14f;
        for (int i = 0; i < lineCount; i++) {
            drawText(canvas, lines.get(i), margin,
                    addressTop + i * addressSize * 1.14f, addressPaint);
        }

        float timeBaseline = addressTop - (landscape ? 92f : 50f) * scale;
        Paint timePaint = paint(Color.WHITE, 100f * scale, timeTypeface);
        timePaint.setShadowLayer(3.5f * scale, 0f, 1.5f * scale, 0x66000000);
        String time = time(when);
        drawText(canvas, time, margin, timeBaseline, timePaint);

        float timeWidth = timePaint.measureText(time);
        float dividerX = margin + timeWidth + (landscape ? 20f : 15f) * scale;
        Paint divider = paint(YELLOW, 5f * scale, textTypeface);
        divider.setStrokeWidth(4f * scale);
        canvas.drawLine(dividerX,
                timeBaseline - 88f * scale,
                dividerX,
                timeBaseline + 4f * scale,
                divider);

        Paint detailPaint = paint(Color.WHITE, 34f * scale, textTypeface);
        detailPaint.setShadowLayer(3.5f * scale, 0f, 1.5f * scale, 0x66000000);
        float detailX = dividerX + 20f * scale;
        drawText(canvas, date(when), detailX, timeBaseline - 52f * scale, detailPaint);
        drawText(canvas, weekday(when), detailX, timeBaseline, detailPaint);

        drawBrand(canvas, width, height, scale, landscape);
        drawVerification(canvas, width, height, scale, verificationCode, landscape);
    }

    private static void drawLandscapeSaved(Canvas canvas, int width, int height, Date when,
                                           String address, String verificationCode, float scale) {
        float margin = 38f * scale;
        Paint addressPaint = paint(Color.WHITE, 48f * scale, textTypeface);
        addressPaint.setShadowLayer(3.5f * scale, 0f, 1.5f * scale, 0x66000000);
        float addressMaxWidth = Math.min(width - margin * 2f, 760f * scale);
        List<String> lines = wrap(address, addressPaint, addressMaxWidth);
        float addressBaseline = height - margin - 26f * scale;
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            drawText(canvas, lines.get(i), margin,
                    addressBaseline - (Math.min(2, lines.size()) - 1 - i) * 56f * scale,
                    addressPaint);
        }

        float timeBaseline = addressBaseline - 74f * scale;
        Paint timePaint = paint(Color.WHITE, 142f * scale, timeTypeface);
        timePaint.setShadowLayer(3.5f * scale, 0f, 1.5f * scale, 0x66000000);
        String time = time(when);
        drawText(canvas, time, margin, timeBaseline, timePaint);

        float dividerX = margin + timePaint.measureText(time) + 22f * scale;
        Paint divider = paint(YELLOW, 5f * scale, textTypeface);
        divider.setStrokeWidth(5f * scale);
        canvas.drawLine(dividerX, timeBaseline - 132f * scale,
                dividerX, timeBaseline + 4f * scale, divider);

        Paint detailPaint = paint(Color.WHITE, 48f * scale, textTypeface);
        detailPaint.setShadowLayer(3.5f * scale, 0f, 1.5f * scale, 0x66000000);
        float detailX = dividerX + 30f * scale;
        drawText(canvas, date(when), detailX, timeBaseline - 76f * scale, detailPaint);
        drawText(canvas, weekday(when), detailX, timeBaseline, detailPaint);

        drawBrand(canvas, width, height, scale, true);
        drawVerification(canvas, width, height, scale, verificationCode, true);
    }

    private static void drawBrand(Canvas canvas, int width, int height, float scale, boolean landscape) {
        float right = width - (landscape ? 35f : 21f) * scale;
        float logoBaseline = height - (landscape ? 76f : 63f) * scale;
        Paint timeLogo = paint(YELLOW, 48f * scale, brandTypeface);
        timeLogo.setTextAlign(Paint.Align.LEFT);
        timeLogo.setShadowLayer(3.5f * scale, 0f, 1.5f * scale, 0x66000000);
        Paint markLogo = new Paint(timeLogo);
        markLogo.setColor(Color.WHITE);
        float timeWidth = timeLogo.measureText("Time");
        float markWidth = markLogo.measureText("mark");
        float logoLeft = right - timeWidth - markWidth;
        drawText(canvas, "Time", logoLeft, logoBaseline, timeLogo);
        drawText(canvas, "mark", logoLeft + timeWidth, logoBaseline, markLogo);
        Paint subtitle = paint(0xFFD6D6D6, 30f * scale, numbersTypeface);
        subtitle.setTextAlign(Paint.Align.RIGHT);
        subtitle.setShadowLayer(3.5f * scale, 0f, 1.5f * scale, 0x66000000);
        drawText(canvas, "100% Chân thực", right,
                height - (landscape ? 29f : 24f) * scale, subtitle);
    }

    private static void drawVerification(Canvas canvas, int width, int height, float scale,
                                         String code, boolean landscape) {
        Paint verified = paint(Color.WHITE, (landscape ? 31f : 20f) * scale, textTypeface);
        verified.setTextAlign(Paint.Align.LEFT);
        verified.setShadowLayer(3.5f * scale, 0f, 1.5f * scale, 0x66000000);
        String verificationText = code + "  Timemark Verified";
        float iconSize = verified.getTextSize() * 0.9f;
        float iconGap = verified.getTextSize() * 0.35f;
        float groupWidth = iconSize + iconGap + verified.measureText(verificationText);
        float pivotX = width - (landscape ? 34f : 22f) * scale;
        float baseline = height * 0.40f;
        float groupLeft = pivotX - groupWidth / 2f;
        canvas.save();
        canvas.rotate(-90f, pivotX, baseline);
        drawShield(canvas,
                groupLeft + iconSize / 2f,
                baseline - verified.getTextSize() * 0.34f,
                iconSize,
                scale);
        drawText(canvas, verificationText,
                groupLeft + iconSize + iconGap,
                baseline,
                verified);
        canvas.restore();
    }

    private static void drawShield(Canvas canvas, float centerX, float centerY,
                                   float size, float scale) {
        if (shieldBitmap == null || shieldBitmap.isRecycled()) return;
        Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        iconPaint.setShadowLayer(3.5f * scale, 0f, 1.5f * scale, 0x66000000);
        float half = size / 2f;
        canvas.drawBitmap(shieldBitmap, null,
                new RectF(centerX - half, centerY - half, centerX + half, centerY + half),
                iconPaint);
    }

    private static Paint paint(int color, float textSize, Typeface typeface) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        paint.setColor(color);
        paint.setTextSize(textSize);
        paint.setTypeface(typeface);
        return paint;
    }

    private static void drawText(Canvas canvas, String text, float x, float y, Paint paint) {
        canvas.drawText(text, x, y, paint);
    }

    private static List<String> wrap(String source, Paint paint, float maxWidth) {
        String safe = source == null || source.trim().isEmpty() ? "Đang xác định vị trí…" : source.trim();
        String[] words = safe.split("\\s+");
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (paint.measureText(candidate) <= maxWidth || line.length() == 0) {
                line.setLength(0);
                line.append(candidate);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        if (lines.size() > 2) {
            String second = lines.get(1);
            while (paint.measureText(second + "…") > maxWidth && second.length() > 1) {
                second = second.substring(0, second.length() - 1);
            }
            lines.set(1, second + "…");
        }
        return lines;
    }
}
