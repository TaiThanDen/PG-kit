package com.pgkit.timemarkcamera;

import android.app.Activity;


import android.app.Dialog;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Calendar;

/** Tab changes preserve the draft; Apply or reset commits changes to the shared clock. */
final class WatermarkTimeSheet {
    private final Activity activity;
    private final WatermarkClock clock;
    private final Runnable onApplied;
    private final Calendar draft = Calendar.getInstance();
    private final Calendar month = Calendar.getInstance();
    private final Dialog dialog;
    private final LinearLayout body;
    private ScrollView scroll;
    private TextView summary;
    private TextView hourTab;
    private TextView dateTab;
    private boolean running;

    WatermarkTimeSheet(Activity activity, WatermarkClock clock, Runnable onApplied) {
        this.activity = activity;
        this.clock = clock;
        this.onApplied = onApplied;
        draft.setTimeInMillis(clock.now());
        month.setTimeInMillis(draft.getTimeInMillis());
        running = clock.isRunning();
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
    }

    void show() {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(16), dp(20), dp(20));
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setColor(Color.WHITE);
        float radius = dp(24);
        background.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
        root.setBackground(background);
        TextView heading = label(activity.getString(R.string.ui_watermark_date_time), 21, true);
        root.addView(heading, lp(-1, 40));
        LinearLayout tabs = new LinearLayout(activity);
        tabs.setBackground(CameraUi.shape(activity, CameraUi.SURFACE, 0, 12));
        hourTab = CameraUi.action(activity, activity.getString(R.string.ui_hour), false, () -> showTab(false));
        dateTab = CameraUi.action(activity, activity.getString(R.string.ui_date), false, () -> showTab(true));
        tabs.addView(hourTab, new LinearLayout.LayoutParams(0, dp(48), 1));
        tabs.addView(dateTab, new LinearLayout.LayoutParams(0, dp(48), 1));
        root.addView(tabs, lp(-1, 48));
        summary = label("", 14, false);
        summary.setTextColor(CameraUi.MUTED);
        summary.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams bodyParams = lp(-1, -2);
        bodyParams.topMargin = dp(12);
        root.addView(body, bodyParams);
        LinearLayout.LayoutParams summaryParams = lp(-1, 40);
        summaryParams.topMargin = dp(8);
        summaryParams.bottomMargin = dp(8);
        root.addView(summary, summaryParams);

        LinearLayout mode = new LinearLayout(activity);
        mode.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label(activity.getString(R.string.ui_keep_clock_running), 16, true);
        mode.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1));
        title.setGravity(Gravity.CENTER_VERTICAL);
        WatermarkSwitch toggle = new WatermarkSwitch(activity);
        toggle.setAccentColor(CameraUi.BLUE);
        toggle.setChecked(running);
        toggle.setContentDescription(activity.getString(R.string.ui_keep_clock_running));
        toggle.setOnCheckedChangeListener((v, checked) -> running = checked);
        mode.addView(toggle, lp(56, 48));
        root.addView(mode, lp(-1, 56));
        TextView helper = label(activity.getString(R.string.ui_turn_off_to_keep_the_selected_date_and), 13, false);
        helper.setTextColor(CameraUi.MUTED);
        root.addView(helper, lp(-1, 28));

        TextView reset = CameraUi.action(activity, activity.getString(R.string.ui_use_device_date_time), false, () -> {
            clock.reset();
            onApplied.run();
            dialog.dismiss();
        });
        LinearLayout.LayoutParams resetParams = lp(-1, 48);
        resetParams.bottomMargin = dp(12);
        root.addView(reset, resetParams);

        LinearLayout footer = new LinearLayout(activity);
        TextView cancel = CameraUi.action(activity, activity.getString(R.string.ui_cancel), false, dialog::dismiss);
        TextView apply = CameraUi.action(activity, activity.getString(R.string.ui_apply), true, () -> {
            draft.set(Calendar.SECOND, 0);
            draft.set(Calendar.MILLISECOND, 0);
            clock.set(draft.getTimeInMillis(), System.currentTimeMillis(), running);
            onApplied.run();
            dialog.dismiss();
        });
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, dp(48), 1);
        left.rightMargin = dp(12);
        footer.addView(cancel, left);
        footer.addView(apply, new LinearLayout.LayoutParams(0, dp(48), 1));
        root.addView(footer, lp(-1, 48));
        scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        scroll.addView(root);
        dialog.setContentView(scroll);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(.35f);
            window.setWindowAnimations(R.style.CameraSheetAnimation);
            window.setGravity(Gravity.BOTTOM);
        }
        showTab(false);
        dialog.show();
        resizeToContent();
    }

    private void showTab(boolean date) {
        hourTab.setBackground(CameraUi.shape(activity, date ? CameraUi.SURFACE : CameraUi.BLUE, 0, 12));
        dateTab.setBackground(CameraUi.shape(activity, date ? CameraUi.BLUE : CameraUi.SURFACE, 0, 12));
        hourTab.setTextColor(date ? CameraUi.INK : Color.WHITE);
        dateTab.setTextColor(date ? Color.WHITE : CameraUi.INK);
        hourTab.setSelected(!date);
        dateTab.setSelected(date);
        body.removeAllViews();
        updateSummary();
        if (date) showCalendar(); else showWheels();
        resizeToContent();
        UiMotion.reveal(body);
    }

    private void updateSummary() {
        summary.setText(UiDateTime.dateTime(activity, draft.getTimeInMillis(), false));
    }

    private void showWheels() {
        LinearLayout columns = new LinearLayout(activity);
        columns.setGravity(Gravity.CENTER);
        addWheel(columns, activity.getString(R.string.ui_hour), 23, Calendar.HOUR_OF_DAY);
        addWheel(columns, activity.getString(R.string.ui_minute), 59, Calendar.MINUTE);
        body.addView(columns, lp(-1, 250));
    }

    private void addWheel(LinearLayout host, String title, int max, int field) {
        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        TextView label = label(title, 15, true);
        label.setTextColor(CameraUi.MUTED);
        label.setGravity(Gravity.CENTER);
        column.addView(label, lp(-1, 36));
        NumberPicker picker = new NumberPicker(activity);
        picker.setMinValue(0);
        picker.setMaxValue(max);
        picker.setFormatter(i -> String.format(UiDateTime.locale(activity), "%02d", i));
        picker.setValue(draft.get(field));
        picker.setWrapSelectorWheel(true);
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            picker.setSelectionDividerHeight(0);
            picker.setTextColor(CameraUi.INK);
            picker.setTextSize(24 * activity.getResources().getDisplayMetrics().scaledDensity);
        }
        picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        picker.setContentDescription(title);
        picker.setOnValueChangedListener((v, oldValue, newValue) -> {
            draft.set(field, newValue);
            updateSummary();
        });
        FrameLayout wheel = new FrameLayout(activity);
        View band = new View(activity);
        band.setBackground(CameraUi.shape(activity, 0xffeaf1ff, 0, 12));
        FrameLayout.LayoutParams bandParams = new FrameLayout.LayoutParams(-1, dp(48), Gravity.CENTER);
        bandParams.setMargins(dp(8), 0, dp(8), 0);
        wheel.addView(band, bandParams);
        wheel.addView(picker, new FrameLayout.LayoutParams(-1, -1));
        column.addView(wheel, lp(-1, 210));
        host.addView(column, new LinearLayout.LayoutParams(0, -1, 1));
    }

    private void resizeToContent() {
        if (scroll == null || !dialog.isShowing()) return;
        Window window = dialog.getWindow();
        if (window == null) return;
        android.graphics.Rect visible = new android.graphics.Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(visible);
        int available = Math.max(dp(180), visible.height() - dp(16));
        scroll.measure(View.MeasureSpec.makeMeasureSpec(visible.width(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(available, View.MeasureSpec.AT_MOST));
        // The window must fit the painted sheet; extra transparent height floats it above the bottom.
        window.setLayout(-1, scroll.getMeasuredHeight());
    }

    private void showCalendar() {
        LinearLayout navigation = new LinearLayout(activity);
        TextView back = CameraUi.action(activity, "‹", false, () -> changeMonth(-1));
        back.setContentDescription(activity.getString(R.string.ui_previous_month));
        TextView next = CameraUi.action(activity, "›", false, () -> changeMonth(1));
        next.setContentDescription(activity.getString(R.string.ui_next_month));
        String name = UiDateTime.format(activity, month.getTimeInMillis(), "yMMMM");
        TextView heading = CameraUi.action(activity, name, false, this::showMonthYear);
        back.setBackgroundColor(Color.TRANSPARENT);
        next.setBackgroundColor(Color.TRANSPARENT);
        heading.setBackgroundColor(Color.TRANSPARENT);
        heading.setTextColor(CameraUi.INK);
        navigation.addView(back, lp(48, 48));
        navigation.addView(heading, new LinearLayout.LayoutParams(0, dp(48), 1));
        navigation.addView(next, lp(48, 48));
        body.addView(navigation, lp(-1, 48));
        GridLayout grid = new GridLayout(activity);
        grid.setColumnCount(7);
        String[] weekdays = new java.text.DateFormatSymbols(UiDateTime.locale(activity)).getShortWeekdays();
        int firstDay = Calendar.getInstance(UiDateTime.locale(activity)).getFirstDayOfWeek();
        for (int i = 0; i < 7; i++) {
            int day = (firstDay - 1 + i) % 7 + 1;
            addCell(grid, weekdays[day], CameraUi.MUTED, false, null, 24);
        }
        Calendar cursor = (Calendar) month.clone();
        cursor.set(Calendar.DAY_OF_MONTH, 1);
        int offset = (cursor.get(Calendar.DAY_OF_WEEK) - firstDay + 7) % 7;
        cursor.add(Calendar.DAY_OF_MONTH, -offset);
        for (int i = 0; i < 42; i++) {
            Calendar date = (Calendar) cursor.clone();
            boolean selected = date.get(Calendar.YEAR) == draft.get(Calendar.YEAR)
                    && date.get(Calendar.DAY_OF_YEAR) == draft.get(Calendar.DAY_OF_YEAR);
            boolean current = date.get(Calendar.MONTH) == month.get(Calendar.MONTH);
            addCell(grid, String.valueOf(date.get(Calendar.DAY_OF_MONTH)),
                    current ? CameraUi.INK : 0xffa1a8b5, selected, () -> {
                        draft.set(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH));
                        month.setTimeInMillis(draft.getTimeInMillis());
                        showTab(true);
                    }, 42);
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }
        body.addView(grid, lp(-1, 276));
    }

    private void showMonthYear() {
        AppSheet sheet = new AppSheet(activity, activity.getString(R.string.ui_month_year), activity.getString(R.string.ui_choose_a_month_to_view_in_the_calendar));
        LinearLayout row = new LinearLayout(activity);
        NumberPicker monthWheel = new NumberPicker(activity);
        monthWheel.setMinValue(1); monthWheel.setMaxValue(12);
        monthWheel.setValue(month.get(Calendar.MONTH)+1);
        monthWheel.setContentDescription(activity.getString(R.string.ui_month_alternate));
        NumberPicker yearWheel = new NumberPicker(activity);
        yearWheel.setMinValue(Math.min(1900, month.get(Calendar.YEAR)));
        yearWheel.setMaxValue(Math.max(2200, month.get(Calendar.YEAR)));
        yearWheel.setValue(month.get(Calendar.YEAR));
        yearWheel.setContentDescription(activity.getString(R.string.ui_year));
        row.addView(monthWheel, new LinearLayout.LayoutParams(0, dp(180), 1));
        row.addView(yearWheel, new LinearLayout.LayoutParams(0, dp(180), 1));
        sheet.content.addView(row, new LinearLayout.LayoutParams(-1, -2));
        sheet.actions(activity.getString(R.string.ui_view_calendar), () -> {
            month.set(Calendar.DAY_OF_MONTH, 1);
            month.set(Calendar.YEAR, yearWheel.getValue());
            month.set(Calendar.MONTH, monthWheel.getValue()-1);
            sheet.dialog.dismiss();
            showTab(true);
        });
        sheet.show();
    }

    private void changeMonth(int delta) {
        month.set(Calendar.DAY_OF_MONTH, 1);
        month.add(Calendar.MONTH, delta);
        showTab(true);
    }

    private void addCell(GridLayout grid, String value, int color, boolean selected,
                         Runnable action, int height) {
        TextView cell = label(value, 15, selected);
        cell.setTextColor(selected ? Color.WHITE : color);
        cell.setGravity(Gravity.CENTER);
        if (selected) cell.setBackground(CameraUi.shape(activity, CameraUi.BLUE, 0, 20));
        if (action != null) {
            cell.setFocusable(true);
            cell.setOnClickListener(v -> action.run());
        }
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dp(height);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        grid.addView(cell, params);
    }

    private TextView label(String text, int size, boolean bold) {
        return CameraUi.text(activity, text, size, CameraUi.INK, bold);
    }
    private int dp(int n) { return CameraUi.dp(activity, n); }
    private LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width < 0 ? width : dp(width), height < 0 ? height : dp(height));
    }
}
