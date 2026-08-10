package com.pgkit.timemarkcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class WatermarkRenderer {
    static final int YELLOW = Color.rgb(255, 184, 43);
    private static final Locale VIETNAMESE = new Locale("vi", "VN");
    private static final Typeface DEFAULT_TIME_TYPEFACE = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
    private static final Typeface DEFAULT_TEXT_TYPEFACE = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
    private static Typeface timeTypeface = DEFAULT_TIME_TYPEFACE;
    private static Typeface textTypeface = DEFAULT_TEXT_TYPEFACE;

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
            case Calendar.MONDAY: return "Thứ hai";
            case Calendar.TUESDAY: return "Thứ ba";
            case Calendar.WEDNESDAY: return "Thứ tư";
            case Calendar.THURSDAY: return "Thứ năm";
            case Calendar.FRIDAY: return "Thứ sáu";
            default: return "Thứ bảy";
        }
    }

    static void drawLive(Canvas canvas, int width, int height, Date when, String address, float density) {
        if (width > height) {
            drawLandscapeLive(canvas, width, height, when, address);
            return;
        }
        float scale = Math.max(0.76f, width / 945f);
        float margin = 25f * scale;
        float addressSize = 34f * scale;
        float bottom = height - 33f * scale;
        Paint addressPaint = paint(Color.WHITE, addressSize, textTypeface);
        addressPaint.setTextScaleX(1f);
        addressPaint.setShadowLayer(3f * scale, 0f, 1f * scale, 0xA0000000);
        List<String> lines = wrap(address, addressPaint, width - 96f * scale);
        int lineCount = Math.min(2, lines.size());
        float addressTop = bottom - (lineCount - 1) * addressSize * 1.18f;
        for (int i = 0; i < lineCount; i++) {
            drawOutlinedText(canvas, lines.get(i), margin,
                    addressTop + i * addressSize * 1.18f, addressPaint, 2.5f * scale);
        }

        float timeBaseline = addressTop - 50f * scale;
        Paint timePaint = paint(Color.WHITE, 100f * scale, timeTypeface);
        timePaint.setTextScaleX(1f);
        timePaint.setShadowLayer(3f * scale, 0f, 1f * scale, 0xA0000000);
        String time = time(when);
        drawOutlinedText(canvas, time, margin, timeBaseline, timePaint, 3.3f * scale);

        float timeWidth = timePaint.measureText(time);
        float dividerX = margin + timeWidth + 15f * scale;
        Paint divider = paint(YELLOW, 5f * scale, textTypeface);
        divider.setStrokeWidth(4f * scale);
        canvas.drawLine(dividerX, timeBaseline - 88f * scale, dividerX, timeBaseline + 4f * scale, divider);

        Paint detailPaint = paint(Color.WHITE, 34f * scale, textTypeface);
        detailPaint.setTextScaleX(1f);
        detailPaint.setShadowLayer(3f * scale, 0f, 1f * scale, 0xA0000000);
        float detailX = dividerX + 20f * scale;
        drawOutlinedText(canvas, date(when), detailX, timeBaseline - 52f * scale,
                detailPaint, 2.5f * scale);
        drawOutlinedText(canvas, weekday(when), detailX, timeBaseline,
                detailPaint, 2.5f * scale);
    }

    private static void drawLandscapeLive(Canvas canvas, int width, int height, Date when,
                                          String address) {
        float scale = Math.max(0.76f, width / 2048f);
        float margin = 34f * scale;
        float timeBaseline = margin + 142f * scale;
        Paint timePaint = paint(Color.WHITE, 142f * scale, timeTypeface);
        timePaint.setShadowLayer(4f * scale, 0f, 1f * scale, 0xB0000000);
        String time = time(when);
        drawOutlinedText(canvas, time, margin, timeBaseline, timePaint, 3.3f * scale);

        float dividerX = margin + timePaint.measureText(time) + 22f * scale;
        Paint divider = paint(YELLOW, 5f * scale, textTypeface);
        divider.setStrokeWidth(5f * scale);
        canvas.drawLine(dividerX, margin, dividerX, timeBaseline + 4f * scale, divider);

        Paint detailPaint = paint(Color.WHITE, 48f * scale, textTypeface);
        detailPaint.setShadowLayer(4f * scale, 0f, 1f * scale, 0xB0000000);
        float detailX = dividerX + 30f * scale;
        drawOutlinedText(canvas, date(when), detailX, margin + 66f * scale,
                detailPaint, 2.5f * scale);
        drawOutlinedText(canvas, weekday(when), detailX, timeBaseline,
                detailPaint, 2.5f * scale);

        Paint addressPaint = paint(Color.WHITE, 48f * scale, textTypeface);
        addressPaint.setShadowLayer(4f * scale, 0f, 1f * scale, 0xB0000000);
        List<String> lines = wrap(address, addressPaint, width - margin * 2f);
        float addressBaseline = timeBaseline + 74f * scale;
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            drawOutlinedText(canvas, lines.get(i), margin,
                    addressBaseline + i * 56f * scale, addressPaint, 2.5f * scale);
        }
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
        addressPaint.setTextScaleX(1f);
        addressPaint.setShadowLayer(4f * scale, 0f, 1f * scale, 0xB0000000);
        List<String> lines = wrap(address, addressPaint, width - margin - reservedRight);
        int lineCount = Math.min(2, lines.size());
        float addressTop = bottom - (lineCount - 1) * addressSize * 1.14f;
        for (int i = 0; i < lineCount; i++) {
            drawOutlinedText(canvas, lines.get(i), margin,
                    addressTop + i * addressSize * 1.14f, addressPaint, 2.5f * scale);
        }

        float timeBaseline = addressTop - (landscape ? 92f : 50f) * scale;
        Paint timePaint = paint(Color.WHITE, 100f * scale, timeTypeface);
        timePaint.setTextScaleX(1f);
        timePaint.setShadowLayer(4f * scale, 0f, 1f * scale, 0xB0000000);
        String time = time(when);
        drawOutlinedText(canvas, time, margin, timeBaseline, timePaint, 3.3f * scale);

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
        detailPaint.setTextScaleX(1f);
        detailPaint.setShadowLayer(4f * scale, 0f, 1f * scale, 0xB0000000);
        float detailX = dividerX + 20f * scale;
        drawOutlinedText(canvas, date(when), detailX,
                timeBaseline - 52f * scale,
                detailPaint, 2.5f * scale);
        drawOutlinedText(canvas, weekday(when), detailX, timeBaseline,
                detailPaint, 2.5f * scale);

        drawBrand(canvas, width, height, scale, landscape);
        drawVerification(canvas, width, height, scale, verificationCode, landscape);
    }

    private static void drawLandscapeSaved(Canvas canvas, int width, int height, Date when,
                                           String address, String verificationCode, float scale) {
        float margin = 38f * scale;
        Paint addressPaint = paint(Color.WHITE, 48f * scale, textTypeface);
        addressPaint.setShadowLayer(4f * scale, 0f, 1f * scale, 0xB0000000);
        List<String> lines = wrap(address, addressPaint, width - margin * 2f);
        float addressBaseline = height - margin - 26f * scale;
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            drawOutlinedText(canvas, lines.get(i), margin,
                    addressBaseline - (Math.min(2, lines.size()) - 1 - i) * 56f * scale,
                    addressPaint, 2.5f * scale);
        }

        float timeBaseline = addressBaseline - 74f * scale;
        Paint timePaint = paint(Color.WHITE, 142f * scale, timeTypeface);
        timePaint.setShadowLayer(4f * scale, 0f, 1f * scale, 0xB0000000);
        String time = time(when);
        drawOutlinedText(canvas, time, margin, timeBaseline, timePaint, 3.3f * scale);

        float dividerX = margin + timePaint.measureText(time) + 22f * scale;
        Paint divider = paint(YELLOW, 5f * scale, textTypeface);
        divider.setStrokeWidth(5f * scale);
        canvas.drawLine(dividerX, timeBaseline - 132f * scale,
                dividerX, timeBaseline + 4f * scale, divider);

        Paint detailPaint = paint(Color.WHITE, 48f * scale, textTypeface);
        detailPaint.setShadowLayer(4f * scale, 0f, 1f * scale, 0xB0000000);
        float detailX = dividerX + 30f * scale;
        drawOutlinedText(canvas, date(when), detailX, timeBaseline - 76f * scale,
                detailPaint, 2.5f * scale);
        drawOutlinedText(canvas, weekday(when), detailX, timeBaseline,
                detailPaint, 2.5f * scale);

        drawBrand(canvas, width, height, scale, true);
        drawVerification(canvas, width, height, scale, verificationCode, true);
    }

    private static void drawBrand(Canvas canvas, int width, int height, float scale, boolean landscape) {
        float right = width - (landscape ? 35f : 21f) * scale;
        float logoBaseline = height - (landscape ? 76f : 63f) * scale;
        Paint logo = paint(YELLOW, 48f * scale, textTypeface);
        logo.setTextAlign(Paint.Align.RIGHT);
        logo.setShadowLayer(3f * scale, 0f, 1f * scale, 0xA0000000);
        drawOutlinedText(canvas, "Timemark", right, logoBaseline, logo, 2.2f * scale);
        Paint subtitle = paint(0xFFD6D6D6, 30f * scale, textTypeface);
        subtitle.setTextAlign(Paint.Align.RIGHT);
        subtitle.setShadowLayer(3f * scale, 0f, 1f * scale, 0xA0000000);
        drawOutlinedText(canvas, "100% Chân thực", right,
                height - (landscape ? 29f : 24f) * scale, subtitle, 1.8f * scale);
    }

    private static void drawVerification(Canvas canvas, int width, int height, float scale,
                                         String code, boolean landscape) {
        Paint verified = paint(Color.WHITE, (landscape ? 31f : 20f) * scale, textTypeface);
        verified.setTextScaleX(0.88f);
        verified.setTextAlign(Paint.Align.CENTER);
        verified.setShadowLayer(3f * scale, 0f, 1f * scale, 0xA0000000);
        canvas.save();
        canvas.rotate(-90f, width - (landscape ? 34f : 22f) * scale, height * 0.40f);
        drawOutlinedText(canvas, "© " + code + "  Timemark Verified",
                width - (landscape ? 34f : 22f) * scale,
                height * 0.40f,
                verified, 1.8f * scale);
        canvas.restore();
    }

    private static Paint paint(int color, float textSize, Typeface typeface) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        paint.setColor(color);
        paint.setTextSize(textSize);
        paint.setTypeface(typeface);
        return paint;
    }

    private static void drawOutlinedText(Canvas canvas, String text, float x, float y,
                                         Paint paint, float outlineWidth) {
        int fillColor = paint.getColor();
        Paint.Style originalStyle = paint.getStyle();
        float originalStrokeWidth = paint.getStrokeWidth();
        paint.clearShadowLayer();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(outlineWidth);
        paint.setColor(0xDD000000);
        canvas.drawText(text, x, y, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(originalStrokeWidth);
        paint.setColor(fillColor);
        canvas.drawText(text, x, y, paint);
        paint.setStyle(originalStyle);
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
