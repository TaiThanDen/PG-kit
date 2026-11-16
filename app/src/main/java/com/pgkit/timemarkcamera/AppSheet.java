package com.pgkit.timemarkcamera;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.function.IntConsumer;

/** Shared, keyboard-safe sheet shell for settings and small editing tasks. */
final class AppSheet {
    final Dialog dialog;
    final LinearLayout content;
    private final Activity activity;
    private final LinearLayout root;
    private final LinearLayout footer;

    AppSheet(Activity activity, String title, String description) {
        this.activity = activity;
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(12), dp(20), dp(20));
        root.setBackground(CameraUi.shape(activity, Color.WHITE, 0, 24));
        View handle = new View(activity);
        handle.setBackground(CameraUi.shape(activity, 0xffd8dce4, 0, 4));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(36), dp(4));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = dp(12);
        root.addView(handle, handleParams);
        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = CameraUi.text(activity, title, 21, CameraUi.INK, true);
        if (android.os.Build.VERSION.SDK_INT >= 28) heading.setAccessibilityHeading(true);
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        ImageButton close = new ImageButton(activity);
        close.setImageResource(R.drawable.ic_close);
        close.setImageTintList(ColorStateList.valueOf(CameraUi.INK));
        close.setBackgroundColor(Color.TRANSPARENT);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription(activity.getString(R.string.ui_close));
        close.setOnClickListener(v -> dialog.dismiss());
        UiMotion.press(close);
        header.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));
        if (description != null && !description.isEmpty()) {
            TextView help = CameraUi.text(activity, description, 14, CameraUi.MUTED, false);
            help.setPadding(0, dp(4), 0, dp(16));
            root.addView(help, new LinearLayout.LayoutParams(-1, -2));
        }
        content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(8), 0, dp(8));
        root.addView(content, new LinearLayout.LayoutParams(-1, -2));
        footer = new LinearLayout(activity);
        footer.setPadding(0, dp(12), 0, 0);
        root.addView(footer, new LinearLayout.LayoutParams(-1, -2));
    }

    void actions(String primary, Runnable accept) {
        TextView cancel = CameraUi.action(activity, activity.getString(R.string.ui_cancel), false, dialog::dismiss);
        TextView apply = CameraUi.action(activity, primary, true, accept);
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, dp(48), 1);
        left.rightMargin = dp(12);
        footer.addView(cancel, left);
        footer.addView(apply, new LinearLayout.LayoutParams(0, dp(48), 1));
    }

    void show() {
        ScrollView scroll = new ScrollView(activity);
        scroll.setClipToPadding(false);
        scroll.addView(root);
        dialog.setContentView(scroll);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setGravity(Gravity.BOTTOM);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(.32f);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            window.setWindowAnimations(R.style.CameraSheetAnimation);
        }
        dialog.show();
        if (window != null) {
            int maxHeight = activity.getWindow().getDecorView().getHeight() - dp(80);
            int width = activity.getResources().getDisplayMetrics().widthPixels;
            scroll.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(Math.max(dp(180), maxHeight), View.MeasureSpec.AT_MOST));
            window.setLayout(-1, Math.min(scroll.getMeasuredHeight(), maxHeight));
        }
    }

    static void choices(Activity activity, String title, String description, String[] labels,
                        String[] details, int selected, IntConsumer onSelected) {
        AppSheet sheet = new AppSheet(activity, title, description);
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            LinearLayout row = new LinearLayout(activity);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(sheet.dp(16), sheet.dp(12), sheet.dp(16), sheet.dp(12));
            boolean active = i == selected;
            row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x221256df),
                    CameraUi.shape(activity, active ? 0xffeaf1ff : CameraUi.SURFACE, 0, 14), null));
            LinearLayout words = new LinearLayout(activity);
            words.setOrientation(LinearLayout.VERTICAL);
            words.addView(CameraUi.text(activity, labels[i], 17,
                    active ? CameraUi.BLUE : CameraUi.INK, true));
            if (details != null && details[i] != null) {
                TextView detail = CameraUi.text(activity, details[i], 13, CameraUi.MUTED, false);
                detail.setPadding(0, sheet.dp(4), 0, 0);
                words.addView(detail);
            }
            row.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
            android.widget.ImageView mark = new android.widget.ImageView(activity);
            mark.setImageResource(R.drawable.ic_check);
            mark.setImageTintList(ColorStateList.valueOf(active ? CameraUi.BLUE : 0xffcbd1dc));
            mark.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
            row.addView(mark, new LinearLayout.LayoutParams(sheet.dp(24), sheet.dp(24)));
            row.setMinimumHeight(sheet.dp(64));
            row.setFocusable(true);
            row.setSelected(active);
            row.setContentDescription(labels[i] + (active ? activity.getString(R.string.ui_selected_alternate) : ""));
            row.setOnClickListener(v -> { onSelected.accept(index); sheet.dialog.dismiss(); });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.bottomMargin = sheet.dp(8);
            sheet.content.addView(row, params);
        }
        sheet.show();
    }

    static EditText input(Activity activity, String value, String hint, boolean multiline) {
        EditText input = new EditText(activity);
        input.setText(value);
        input.setHint(hint);
        input.setContentDescription(hint);
        input.setTextSize(17);
        input.setTextColor(CameraUi.INK);
        input.setHintTextColor(CameraUi.MUTED);
        input.setPadding(CameraUi.dp(activity, 16), CameraUi.dp(activity, 12),
                CameraUi.dp(activity, 16), CameraUi.dp(activity, 12));
        input.setBackground(CameraUi.shape(activity, CameraUi.SURFACE, 0xffdce3ee, 12));
        input.setMinLines(multiline ? 3 : 1);
        input.setSingleLine(!multiline);
        input.setGravity(Gravity.TOP | Gravity.START);
        return input;
    }
    private int dp(int value) { return CameraUi.dp(activity, value); }
}
