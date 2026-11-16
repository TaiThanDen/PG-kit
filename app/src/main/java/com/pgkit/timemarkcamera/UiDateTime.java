package com.pgkit.timemarkcamera;

import android.content.Context;
import android.text.format.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Formats UI dates with the resource locale and the device's 12/24-hour preference. */
final class UiDateTime {
    private UiDateTime() { }

    static Locale locale(Context context) {
        return context.getResources().getConfiguration().getLocales().get(0);
    }

    static String date(Context context, long millis) {
        return java.text.DateFormat.getDateInstance(java.text.DateFormat.LONG, locale(context))
                .format(new Date(millis));
    }

    static String dateTime(Context context, long millis, boolean seconds) {
        String time = DateFormat.is24HourFormat(context) ? (seconds ? "Hms" : "Hm")
                : (seconds ? "hms" : "hm");
        return format(context, millis, "yMd" + time);
    }

    static String format(Context context, long millis, String skeleton) {
        Locale locale = locale(context);
        return new SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
                .format(new Date(millis));
    }
}
