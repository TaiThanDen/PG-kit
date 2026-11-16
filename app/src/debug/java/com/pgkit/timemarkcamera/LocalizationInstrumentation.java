package com.pgkit.timemarkcamera;

import android.app.Instrumentation;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.LocaleList;

/** Exercises compiled Android resource selection without changing the device or app locale. */
public final class LocalizationInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { start(); }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            Context en = localized("en-US");
            Context vi = localized("vi-VN");
            Context fr = localized("fr-FR");
            Context enThenVi = localized("en-US,vi-VN");
            Context viThenEn = localized("vi-VN,en-US");
            equal("Gallery", enThenVi.getString(R.string.ui_gallery));
            equal("Thư viện", viThenEn.getString(R.string.ui_gallery));
            equal("Gallery", en.getString(R.string.ui_gallery));
            equal("Thư viện", vi.getString(R.string.ui_gallery));
            equal("Gallery", fr.getString(R.string.ui_gallery));
            for (java.lang.reflect.Field field : R.string.class.getFields()) {
                int id = field.getInt(null);
                String fallback = fr.getString(id);
                equal(en.getString(id), fallback);
                if (fallback.isEmpty() || vi.getString(id).isEmpty())
                    throw new AssertionError("Empty resource: " + field.getName());
            }
            equal("1 item selected", en.getResources().getQuantityString(R.plurals.selected_count, 1, 1));
            equal("2 items selected", en.getResources().getQuantityString(R.plurals.selected_count, 2, 2));
            equal("Đã chọn 2 mục", vi.getResources().getQuantityString(R.plurals.selected_count, 2, 2));
            equal("Select 2026", en.getString(R.string.select_day, "2026"));
            equal("Chọn 2026", vi.getString(R.string.select_day, "2026"));
            long now = System.currentTimeMillis();
            if (UiDateTime.date(en, now).equals(UiDateTime.date(vi, now)))
                throw new AssertionError("Date format did not follow resource locale");
            result.putString("stream", "PASS: English, Vietnamese, unsupported-language fallback, all compiled strings, plurals, formatted accessibility and UI dates");
            finish(-1, result);
        } catch (Exception | AssertionError error) {
            result.putString("stream", error.toString());
            finish(0, result);
        }
    }

    private Context localized(String tag) {
        Configuration config = new Configuration(getTargetContext().getResources().getConfiguration());
        config.setLocales(LocaleList.forLanguageTags(tag));
        return getTargetContext().createConfigurationContext(config);
    }

    private static void equal(String expected, String actual) {
        if (!expected.equals(actual)) throw new AssertionError(expected + " != " + actual);
    }
}
