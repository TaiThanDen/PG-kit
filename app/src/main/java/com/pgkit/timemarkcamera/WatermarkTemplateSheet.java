package com.pgkit.timemarkcamera;

import android.app.Activity;

import android.app.Dialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Local template picker and editor. Selection is applied immediately to the camera preview. */
final class WatermarkTemplateSheet {
    private final Activity activity;
    private final WatermarkClock clock;
    private final android.graphics.Bitmap scene;
    private final Supplier<String> address;
    private final Consumer<WatermarkTemplate.Snapshot> onChanged;
    private final List<WatermarkTemplate> templates;
    private WatermarkTemplate.Snapshot selected;
    private Dialog dialog;
    private final IntConsumer onSheetTop;
    private final IntSupplier cameraRotation;
    private WatermarkOverlayView sheetPreview;
    private boolean editing;
    private int scrollPosition;

    WatermarkTemplateSheet(Activity activity, WatermarkTemplate.Snapshot selected,
                           Supplier<String> address, WatermarkClock clock, IntConsumer onSheetTop, IntSupplier cameraRotation,
                           Consumer<WatermarkTemplate.Snapshot> onChanged, android.graphics.Bitmap scene) {
        this.activity = activity;
        this.clock = clock;
        this.scene = scene;
        this.selected = selected;
        this.address = address;
        this.onSheetTop = onSheetTop;
        this.cameraRotation = cameraRotation;
        this.onChanged = onChanged;
        templates = new java.util.ArrayList<>(WatermarkTemplate.defaults(activity));
        java.util.List<String> order = java.util.Arrays.asList("34", "21", "service01", "80", "43", "150", "10", "20", "110", "100", "90", "120", "60", "130", "map01", "dasher01");
        templates.sort(java.util.Comparator.comparingInt(t -> {
            int index = order.indexOf(t.id);
            return index < 0 ? order.size() : index;
        }));
    }

    void show(boolean edit) {
        if (edit && !"none".equals(selected.id)) showEditor(); else showPicker();
    }

    private int dp(float value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
    private TextView label(String text, int size) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextColor(Color.BLACK);
        view.setTextSize(size);
        return view;
    }
    private LinearLayout column() {
        LinearLayout view = new LinearLayout(activity);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setBackgroundColor(Color.WHITE);
        return view;
    }
    private TextView button(String title, Runnable action) {
        TextView view = CameraUi.action(activity, title, false, action);
        view.setMinHeight(dp(48));
        return view;
    }
    private void update(WatermarkTemplate.Snapshot next) {
        selected = next;
        if (sheetPreview != null) sheetPreview.setTemplate(next);
        onChanged.accept(next);
    }
    private GradientDrawable background(int color, int stroke, float radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        if (stroke != 0) shape.setStroke(dp(1), stroke);
        return shape;
    }
    private TextView flatAction(String title, int color, int fill, Runnable action) {
        TextView view = label(title, 16);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(color);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        view.setBackground(background(fill, 0, 12));
        view.setOnClickListener(v -> action.run());
        UiMotion.press(view);
        return view;
    }
    private void open(LinearLayout content) {
        if (dialog == null) {
            dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setOnDismissListener(d -> {
                onSheetTop.accept(Integer.MAX_VALUE);
                if (scene != null && !scene.isRecycled()) scene.recycle();
            });
        }
        content.setBackground(background(Color.WHITE, 0, 24));
        content.setClipToOutline(true);
        int sheetHeight = (int)(activity.getResources().getDisplayMetrics().heightPixels
                * (editing ? .75f : .68f));
        FrameLayout host = new FrameLayout(activity);
        sheetPreview = new WatermarkOverlayView(activity);
        sheetPreview.setClock(clock);
        sheetPreview.setLiveSources(address, cameraRotation);
        sheetPreview.setTemplate(selected);
        sheetPreview.setOnClickListener(v -> dialog.dismiss());
        FrameLayout.LayoutParams previewParams = new FrameLayout.LayoutParams(-1, -1);
        previewParams.bottomMargin = sheetHeight;
        host.addView(sheetPreview, previewParams);
        host.addView(content, new FrameLayout.LayoutParams(-1, sheetHeight, Gravity.BOTTOM));
        dialog.setContentView(host);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setGravity(Gravity.BOTTOM);
            window.setWindowAnimations(R.style.CameraSheetAnimation);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(editing ? .6f : 0f);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        boolean wasShowing = dialog.isShowing();
        dialog.show();
        if (wasShowing) UiMotion.reveal(content);
        if (window != null) {
            window.setLayout(-1, -1);
            onSheetTop.accept(0);
        }
    }
    private ImageButton icon(int drawable, String description, Runnable action) {
        ImageButton button = new ImageButton(activity);
        button.setImageResource(drawable);
        button.setContentDescription(description);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        button.setOnClickListener(v -> action.run());
        return button;
    }
    private LinearLayout header(String title, Runnable close) {
        LinearLayout bar = new LinearLayout(activity);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(Color.WHITE);
        if (!editing) bar.addView(icon(R.drawable.ic_watermark_none, activity.getString(R.string.ui_no_watermark), () -> {
            update(WatermarkTemplate.noneSnapshot()); dialog.dismiss();
        }), new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView heading = label(title, editing ? 21 : 18);
        heading.setGravity(editing ? Gravity.CENTER_VERTICAL : Gravity.CENTER);
        heading.setPadding(editing ? dp(20) : 0, 0, 0, 0);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        bar.addView(heading, new LinearLayout.LayoutParams(0, dp(editing ? 58 : 48), 1));
        if (!editing) {
            TextView edit = flatAction(activity.getString(R.string.ui_edit), CameraUi.BLUE, Color.WHITE, this::showEditor);
            bar.addView(edit, new LinearLayout.LayoutParams(dp(84), dp(48)));
        }
        bar.addView(icon(R.drawable.ic_close, activity.getString(R.string.ui_close), close),
                new LinearLayout.LayoutParams(dp(48), dp(48)));
        return bar;
    }
    private String displayName(WatermarkTemplate template) {
        switch (template.id) {
            case "34": return activity.getString(R.string.ui_time_location);
            case "21": return activity.getString(R.string.ui_attendance);
            case "service01": return activity.getString(R.string.ui_service);
            case "80": return activity.getString(R.string.ui_completed);
            case "43": return activity.getString(R.string.ui_patrol);
            case "150": return activity.getString(R.string.ui_outdoor);
            case "10": return activity.getString(R.string.ui_custom_text);
            case "20": return activity.getString(R.string.ui_work_report);
            case "110": return activity.getString(R.string.ui_project);
            case "100": return activity.getString(R.string.ui_table);
            case "90": return activity.getString(R.string.ui_cleaned);
            case "120": return activity.getString(R.string.ui_construction_log);
            case "60": case "130": return activity.getString(R.string.ui_location);
            case "map01": return activity.getString(R.string.ui_map);
            case "dasher01": return activity.getString(R.string.ui_delivery);
            default: return template.name;
        }
    }
    private void showPicker() {
        editing = false;
        LinearLayout root = column();
        root.addView(header(activity.getString(R.string.ui_watermark_templates), () -> dialog.dismiss()));
        ScrollView scroll = new ScrollView(activity);
        GridLayout grid = new GridLayout(activity);
        grid.setColumnCount(2);
        grid.setPadding(dp(8), dp(8), dp(8), dp(8));
        int width = (activity.getResources().getDisplayMetrics().widthPixels - dp(32)) / 2;
        for (WatermarkTemplate template : templates) {
            LinearLayout card = column();
            card.setPadding(dp(4), dp(4), dp(4), dp(8));
            GradientDrawable border = new GradientDrawable();
            border.setColor(Color.WHITE);
            border.setCornerRadius(dp(7));
            border.setStroke(dp(template.id.equals(selected.id) ? 2 : 1),
                    template.id.equals(selected.id) ? CameraUi.BLUE : 0xffe4e8ef);
            card.setBackground(border);
            FrameLayout thumbnail = new FrameLayout(activity);
            thumbnail.setBackground(background(0xff818891, 0, 4));
            thumbnail.setClipToOutline(true);
            View preview = new View(activity) {
                @Override protected void onDraw(Canvas canvas) {
                    canvas.drawColor(0xff818991);
                    if (scene != null && !scene.isRecycled()) {
                        float fill = Math.max(getWidth() / (float) scene.getWidth(), getHeight() / (float) scene.getHeight());
                        float w = scene.getWidth() * fill, h = scene.getHeight() * fill;
                        canvas.drawBitmap(scene, null, new android.graphics.RectF((getWidth()-w)/2, (getHeight()-h)/2,
                                (getWidth()+w)/2, (getHeight()+h)/2), null);
                    }
                    WatermarkTemplate.Snapshot sample = template.id.equals(selected.id) ? selected : template.defaultSnapshot().withLiveData(selected.liveData);
                    float scale = getWidth() / 360f;
                    int saved = canvas.save();
                    canvas.translate(0, getHeight() - 300f * scale);
                    canvas.scale(scale, scale);
                    WatermarkRenderer.drawLive(canvas, 360, 300, new Date(clock.now()), address.get(), 1f, sample);
                    canvas.restoreToCount(saved);
                }
            };
            thumbnail.addView(preview, new FrameLayout.LayoutParams(-1, -1));
            if (template.id.equals(selected.id)) {
                android.widget.ImageView check = new android.widget.ImageView(activity);
                check.setImageResource(R.drawable.ic_check);
                check.setPadding(dp(5), dp(5), dp(5), dp(5));
                check.setBackground(background(CameraUi.BLUE, Color.WHITE, 18));
                FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(dp(32), dp(32), Gravity.TOP | Gravity.END);
                checkParams.setMargins(dp(6), dp(6), dp(6), dp(6));
                thumbnail.addView(check, checkParams);
            }
            card.addView(thumbnail, new LinearLayout.LayoutParams(-1, dp(126)));
            TextView name = label(displayName(template), 15);
            name.setGravity(Gravity.CENTER);
            card.addView(name, new LinearLayout.LayoutParams(-1, dp(28)));
            card.setContentDescription(displayName(template));
            card.setOnClickListener(v -> {
                WatermarkTemplate.Snapshot next = template.defaultSnapshot().withLiveData(selected.liveData);
                scrollPosition = scroll.getScrollY();
                update(next);
                showPicker();
            });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = width; lp.height = -2; lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            grid.addView(card, lp);
        }
        scroll.addView(grid);
        scroll.post(() -> scroll.scrollTo(0, scrollPosition));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        open(root);
    }
    private String fieldTitle(WatermarkTemplate.Field field) {
        switch (field.id) {
            case 1: return activity.getString(R.string.ui_time);
            case 2: return activity.getString(R.string.ui_location);
            case 3: return activity.getString(R.string.ui_coordinates);
            case 4: return activity.getString(R.string.ui_weather);
            case 5: return activity.getString(R.string.ui_altitude);
            case 6: return activity.getString(R.string.ui_compass);
            case 13: return "21".equals(selected.id) ? activity.getString(R.string.ui_title) : activity.getString(R.string.ui_note);
            case 88: return activity.getString(R.string.ui_styled_text);
            case 210: return activity.getString(R.string.ui_map);
            case 620: return activity.getString(R.string.ui_business_card);
            case 630: return activity.getString(R.string.ui_tags);
            case 640: return activity.getString(R.string.ui_photo_code);
            case 11: return activity.getString("service01".equals(selected.id) ? R.string.field_service_name : R.string.field_title);
case 12: return activity.getString(R.string.field_notes);
case 14: return activity.getString(R.string.field_project);
case 15: return activity.getString(R.string.field_area);
case 16: return activity.getString(R.string.field_inspection);
case 17: return activity.getString(R.string.field_inspector);
case 510: return activity.getString(R.string.field_staff);
case 520: return activity.getString(R.string.field_work_id);
case 530: return activity.getString(R.string.field_cleaner);
case 540: return activity.getString(R.string.field_workplace);
case 550: return activity.getString(R.string.field_company);
case 1201: return activity.getString(R.string.field_construction_content);
case 1202: return activity.getString(R.string.field_executor);
case 1203: return activity.getString(R.string.field_construction_area);
case 1204: return activity.getString(R.string.field_operator);
case 1205: return activity.getString(R.string.field_inspector);
case 1206: return activity.getString(R.string.field_inspection);
case 1208: return activity.getString(R.string.field_management);
case 1209: return activity.getString(R.string.field_design_firm);
case 1210: return activity.getString(R.string.field_description);
case 1211: return activity.getString(R.string.field_developer);
case 1213: return activity.getString(R.string.field_work_content);
case 1214: return activity.getString(R.string.field_inspector);
case 1301: return activity.getString(R.string.field_subtitle);
            default: return field.title;
        }
    }
    private void showEditor() {
        editing = true;
        LinearLayout root = column();
        root.addView(header(activity.getString(R.string.ui_edit_template), () -> dialog.dismiss()));
        ScrollView scroll = new ScrollView(activity);
        LinearLayout fields = column();
        fields.setPadding(dp(14), 0, dp(14), dp(12));
        for (WatermarkTemplate.Field field : selected.fields.values()) {
            if (field.id == 1) {
                TextView time = label(activity.getString(R.string.ui_time_use_adjust_time_to_change), 16);
                time.setPadding(dp(64), dp(16), 0, dp(16));
                fields.addView(time);
                continue;
            }
            if (field.id == 640) continue;
            LinearLayout row = new LinearLayout(activity);
            row.setGravity(Gravity.CENTER_VERTICAL);
            WatermarkSwitch toggle = new WatermarkSwitch(activity);
            toggle.setAccentColor(CameraUi.BLUE);
            toggle.setChecked(field.enabled);
            toggle.setContentDescription(fieldTitle(field));
            toggle.setThumbTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{Color.WHITE, Color.WHITE}));
            toggle.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{0xff2dca58, 0xffe6e6e8}));
            row.addView(toggle, new LinearLayout.LayoutParams(dp(48), dp(36)));
            TextView title = label(fieldTitle(field) + (field.isEditable() && !field.content.isEmpty() ? ": " + field.content : ""), 18);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            title.setTextColor(field.enabled ? Color.BLACK : 0xff888888);
            title.setPadding(dp(16), dp(12), 0, dp(12));
            row.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
            toggle.setOnCheckedChangeListener((v, checked) -> {
                title.setTextColor(checked ? Color.BLACK : 0xff888888);
                update(selected.withField(field.id, checked, selected.field(field.id).content));
            });
            if (field.isEditable()) title.setOnClickListener(v -> {
                AppSheet inputSheet = new AppSheet(activity, fieldTitle(field), activity.getString(R.string.ui_content_displayed_on_the_watermark));
                EditText input = AppSheet.input(activity, selected.field(field.id).content, fieldTitle(field), true);
                inputSheet.content.addView(input, new LinearLayout.LayoutParams(-1, -2));
                inputSheet.actions(activity.getString(R.string.ui_save), () -> {
                    update(selected.withField(field.id, true, input.getText().toString()));
                    inputSheet.dialog.dismiss();
                    showEditor();
                });
                inputSheet.show();
            });
            fields.addView(row, new LinearLayout.LayoutParams(-1, dp(50)));
            View separator = new View(activity);
            separator.setBackgroundColor(0xffeeeeee);
            LinearLayout.LayoutParams separatorParams = new LinearLayout.LayoutParams(-1, dp(.5f));
            separatorParams.leftMargin = dp(64);
            fields.addView(separator, separatorParams);
        }
        TextView size = label(activity.getString(R.string.ui_size), 16);
        fields.addView(size);
        SeekBar slider = new SeekBar(activity);
        slider.setMax(150);
        slider.setProgress(Math.round((selected.sizeScale - .5f) * 100));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean user) {
                if (user) update(selected.withScale(.5f + progress / 100f));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        fields.addView(slider);
        fields.addView(button(activity.getString(R.string.ui_color), () -> {
            String[] names = {activity.getString(R.string.ui_yellow), activity.getString(R.string.ui_white), activity.getString(R.string.ui_blue), activity.getString(R.string.ui_red), activity.getString(R.string.ui_green)};
            int[] colors = {0xffffc233, Color.WHITE, 0xff1687ec, 0xffef4438, 0xff2bbf57};
            int current = -1;
            for (int i = 0; i < colors.length; i++) if (colors[i] == selected.accentColor) current = i;
            AppSheet.choices(activity, activity.getString(R.string.ui_accent_color), activity.getString(R.string.ui_changes_only_when_you_select_a_new_color), names, null, current,
                    which -> { update(selected.withTheme(colors[which], selected.textColor)); showEditor(); });
        }));
        scroll.addView(fields);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout footer = new LinearLayout(activity);
        footer.setPadding(dp(16), dp(12), dp(16), dp(12));
        footer.setBackgroundColor(0xfff8f8f8);
        TextView back = flatAction(activity.getString(R.string.ui_templates), 0xff555555, Color.WHITE, this::showPicker);
        back.setBackground(background(Color.WHITE, 0xffdddddd, 12));
        footer.addView(back, new LinearLayout.LayoutParams(dp(96), dp(44)));
        TextView done = flatAction(activity.getString(R.string.ui_done), Color.WHITE, 0xff0065ff, () -> dialog.dismiss());
        LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(0, dp(44), 1);
        doneParams.leftMargin = dp(12);
        footer.addView(done, doneParams);
        root.addView(footer);
        open(root);
    }
}
