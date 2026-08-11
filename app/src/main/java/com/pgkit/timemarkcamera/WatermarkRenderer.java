package com.pgkit.timemarkcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
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
    /**
     * Output-photo coordinates are authored against the Timemark 4:3 reference images:
     * 1536x1152 in landscape and 1152x1536 in portrait.  They intentionally do not use
     * preview dp values or an independent horizontal/vertical percentage scale.
     */
    private static final int SAVED_YELLOW = Color.rgb(248, 194, 62);
    private static final int SAVED_SUBTITLE = Color.rgb(180, 189, 196);
    // Insets only affect the yellow divider; date and weekday remain anchored in place.
    // Increase either value to shorten that end of the divider.
    private static final float DIVIDER_LINE_TOP_INSET = 3f;
    private static final float DIVIDER_LINE_BOTTOM_INSET = 3f;
    // Keep label alignment independent when only the divider length changes.
    private static final float DETAIL_TEXT_TOP_INSET = 3f;
    private static final float DETAIL_TEXT_BOTTOM_INSET = 3f;
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
        String time = time(when);
        drawText(canvas, time, margin, timeBaseline, timePaint);

        float timeWidth = timePaint.measureText(time);
        float dividerX = margin + timeWidth + 8f * density;
        Paint divider = paint(YELLOW, 2f * density, textTypeface);
        divider.setStrokeWidth(2f * density);
        Paint detailPaint = paint(Color.WHITE, 16f * density, textTypeface);
        float dividerTop = timeBaseline + timePaint.ascent() + 5f * density;
        float dividerBottom = timeBaseline + timePaint.descent() - 3f * density;
        float dividerLineTop = dividerTop + DIVIDER_LINE_TOP_INSET * density;
        float dividerLineBottom = dividerBottom - DIVIDER_LINE_BOTTOM_INSET * density;
        canvas.drawLine(dividerX, dividerLineTop, dividerX, dividerLineBottom, divider);
        float detailX = dividerX + 8f * density;
        String dateText = date(when);
        String weekdayText = weekday(when);
        drawText(canvas, dateText, detailX,
                baselineForTextTop(dateText, detailPaint, dividerTop + DETAIL_TEXT_TOP_INSET * density), detailPaint);
        drawText(canvas, weekdayText, detailX,
                baselineForTextBottom(weekdayText, detailPaint, dividerBottom - DETAIL_TEXT_BOTTOM_INSET * density), detailPaint);
    }

    static void drawSaved(Bitmap bitmap, Date when, String address, String verificationCode) {
        Canvas canvas = new Canvas(bitmap);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scale = WatermarkLayoutSpec.scaleFor(width, height);
        float dividerTop = WatermarkLayoutSpec.dividerTop(height, scale);

        Paint addressPaint = savedPaint(Color.WHITE, 46f * scale, textTypeface, scale);
        addressPaint.setShadowLayer(3.5f * scale, 0f, 1.5f * scale, 0x59000000);
        float maxAddressWidth = Math.min(WatermarkLayoutSpec.ADDRESS_MAX_WIDTH * scale,
                width - WatermarkLayoutSpec.ADDRESS_LEFT * scale
                        - (177f + WatermarkLayoutSpec.WORDMARK_RIGHT) * scale);
        List<String> lines = wrap(address, addressPaint, maxAddressWidth);
        int lineCount = Math.min(2, lines.size());
        String lastLine = lines.get(lineCount - 1);
        float addressLastBaseline = baselineForTextBottom(lastLine, addressPaint,
                height - WatermarkLayoutSpec.ADDRESS_BOTTOM * scale);
        for (int i = 0; i < lineCount; i++) {
            drawText(canvas, lines.get(i), WatermarkLayoutSpec.ADDRESS_LEFT * scale,
                    addressLastBaseline - (lineCount - 1 - i)
                            * WatermarkLayoutSpec.ADDRESS_LINE_STEP * scale,
                    addressPaint);
        }

        Paint timePaint = savedPaint(Color.WHITE, 120f * scale, timeTypeface, scale);
        String time = time(when);
        drawText(canvas, time, WatermarkLayoutSpec.TIME_LEFT * scale,
                baselineForTextTop(time, timePaint, dividerTop), timePaint);

        float timeWidth = timePaint.measureText(time);
        float dividerX = WatermarkLayoutSpec.dividerX(timeWidth, scale);
        Paint divider = paint(SAVED_YELLOW, 1f, textTypeface);
        canvas.drawRect(dividerX, dividerTop, dividerX + WatermarkLayoutSpec.DIVIDER_WIDTH * scale,
                dividerTop + WatermarkLayoutSpec.DIVIDER_HEIGHT * scale, divider);
        Paint datePaint = savedPaint(Color.WHITE, 44.8f * scale, textTypeface, scale);
        Paint weekdayPaint = savedPaint(Color.WHITE, 38f * scale, textTypeface, scale);
        String dateText = date(when);
        String weekdayText = weekday(when);
        drawText(canvas, dateText, dividerX + (WatermarkLayoutSpec.DIVIDER_WIDTH
                        + WatermarkLayoutSpec.DATE_GAP) * scale,
                baselineForTextTop(dateText, datePaint, dividerTop + scale), datePaint);
        drawText(canvas, weekdayText,
                dividerX + (WatermarkLayoutSpec.DIVIDER_WIDTH
                        + WatermarkLayoutSpec.WEEKDAY_GAP) * scale,
                baselineForTextTop(weekdayText, weekdayPaint,
                        dividerTop + WatermarkLayoutSpec.WEEKDAY_TOP_GAP * scale), weekdayPaint);

        drawBrand(canvas, width, height, scale);
        drawVerification(canvas, width, height, scale, verificationCode);
    }

    private static void drawBrand(Canvas canvas, int width, int height, float scale) {
        Paint timeLogo = savedPaint(SAVED_YELLOW, 34.2f * scale, brandTypeface, scale);
        timeLogo.setTextAlign(Paint.Align.LEFT);
        Paint markLogo = new Paint(timeLogo);
        markLogo.setColor(Color.WHITE);
        float timeWidth = timeLogo.measureText("Time");
        Rect wordmarkBounds = textBounds("Timemark", timeLogo);
        float wordmarkBaseline = baselineForTextBottom("Timemark", timeLogo,
                height - WatermarkLayoutSpec.WORDMARK_BOTTOM * scale);
        float wordmarkLeft = width - WatermarkLayoutSpec.WORDMARK_RIGHT * scale - wordmarkBounds.right;
        drawText(canvas, "Time", wordmarkLeft, wordmarkBaseline, timeLogo);
        drawText(canvas, "mark", wordmarkLeft + timeWidth, wordmarkBaseline, markLogo);

        Paint subtitle = savedPaint(SAVED_SUBTITLE, 23.6f * scale, textTypeface, scale);
        Rect subtitleBounds = textBounds("100% Chân thực", subtitle);
        subtitle.setTextSize(subtitle.getTextSize() * wordmarkBounds.width()
                / Math.max(1, subtitleBounds.width()));
        subtitleBounds = textBounds("100% Chân thực", subtitle);
        float subtitleLeft = width - WatermarkLayoutSpec.SUBTITLE_RIGHT * scale
                - subtitleBounds.right;
        drawText(canvas, "100% Chân thực", subtitleLeft,
                baselineForTextBottom("100% Chân thực", subtitle,
                        height - WatermarkLayoutSpec.SUBTITLE_BOTTOM * scale), subtitle);
    }

    private static void drawVerification(Canvas canvas, int width, int height, float scale,
                                         String code) {
        Paint verified = savedPaint(Color.WHITE, 21f * scale, textTypeface, scale);
        verified.setTextAlign(Paint.Align.LEFT);
        String verificationText = code + "  Timemark Verified";
        float iconSize = 12f * scale;
        float iconGap = 3f * scale;
        Rect textBounds = textBounds(verificationText, verified);
        float groupRight = iconSize + iconGap + textBounds.right;
        float groupLeft = 0f;
        float pivotX = width - WatermarkLayoutSpec.VERIFICATION_RIGHT * scale
                - Math.max(textBounds.bottom, iconSize / 2f);
        float baseline = height / 2f;
        float groupStart = pivotX - (groupLeft + groupRight) / 2f;
        canvas.save();
        canvas.rotate(-90f, pivotX, baseline);
        drawShield(canvas,
                groupStart + iconSize / 2f,
                baseline - verified.getTextSize() * 0.34f,
                iconSize,
                scale);
        drawText(canvas, verificationText,
                groupStart + iconSize + iconGap,
                baseline,
                verified);
        canvas.restore();
    }

    private static void drawShield(Canvas canvas, float centerX, float centerY,
                                   float size, float scale) {
        if (shieldBitmap == null || shieldBitmap.isRecycled()) return;
        Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        iconPaint.setColorFilter(new PorterDuffColorFilter(0xFFD6D6D6, PorterDuff.Mode.SRC_IN));
        iconPaint.setShadowLayer(2f * scale, 0f, 1f * scale, 0x33000000);
        float half = size / 2f;
        canvas.drawBitmap(shieldBitmap, null,
                new RectF(centerX - half, centerY - half, centerX + half, centerY + half),
                iconPaint);
        Paint checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        checkPaint.setColor(0xFF666666);
        checkPaint.setStyle(Paint.Style.STROKE);
        checkPaint.setStrokeCap(Paint.Cap.ROUND);
        checkPaint.setStrokeJoin(Paint.Join.ROUND);
        checkPaint.setStrokeWidth(Math.max(1f, size * 0.12f));
        Path check = new Path();
        check.moveTo(centerX - size * 0.23f, centerY);
        check.lineTo(centerX - size * 0.05f, centerY + size * 0.18f);
        check.lineTo(centerX + size * 0.27f, centerY - size * 0.20f);
        canvas.drawPath(check, checkPaint);
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

    private static float baselineForTextTop(String text, Paint paint, float top) {
        Rect bounds = textBounds(text, paint);
        return top - bounds.top;
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
