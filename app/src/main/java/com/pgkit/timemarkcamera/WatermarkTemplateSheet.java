package com.pgkit.timemarkcamera;

import android.app.Activity;
import android.app.AlertDialog;
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
                           Supplier<String> address, IntConsumer onSheetTop, IntSupplier cameraRotation,
                           Consumer<WatermarkTemplate.Snapshot> onChanged) {
        this.activity = activity;
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
    private Button button(String title, Runnable action) {
        Button view = new Button(activity);
        view.setText(title);
        view.setAllCaps(false);
        view.setOnClickListener(v -> action.run());
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
        view.setBackground(background(fill, 0, 4));
        view.setOnClickListener(v -> action.run());
        return view;
    }
    private void open(LinearLayout content) {
        if (dialog == null) {
            dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setOnDismissListener(d -> onSheetTop.accept(Integer.MAX_VALUE));
        }
        content.setBackground(background(Color.WHITE, 0, 10));
        content.setClipToOutline(true);
        int sheetHeight = (int)(activity.getResources().getDisplayMetrics().heightPixels
                * (editing ? .67f : .545f));
        FrameLayout host = new FrameLayout(activity);
        sheetPreview = new WatermarkOverlayView(activity);
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
            window.setWindowAnimations(0);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(editing ? .6f : 0f);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        dialog.show();
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
        bar.setBackgroundColor(0xfff7f7f7);
        if (!editing) bar.addView(icon(R.drawable.ic_watermark_none, "Không watermark", () -> {
            update(WatermarkTemplate.noneSnapshot()); dialog.dismiss();
        }), new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView heading = label(title, editing ? 21 : 18);
        heading.setGravity(editing ? Gravity.CENTER_VERTICAL : Gravity.CENTER);
        heading.setPadding(editing ? dp(20) : 0, 0, 0, 0);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        bar.addView(heading, new LinearLayout.LayoutParams(0, dp(editing ? 58 : 48), 1));
        bar.addView(icon(R.drawable.ic_close, "Đóng", close),
                new LinearLayout.LayoutParams(dp(48), dp(48)));
        return bar;
    }
    private String displayName(WatermarkTemplate template) {
        switch (template.id) {
            case "34": return "Thời gian & Vị trí";
            case "21": return "Điểm danh";
            case "service01": return "Dịch vụ";
            case "80": return "Đã hoàn thành";
            case "43": return "Tuần tra";
            case "150": return "Ngoài trời";
            case "10": return "Văn bản tùy chỉnh";
            case "20": return "Báo cáo công việc";
            case "110": return "Dự án";
            case "100": return "Bảng";
            case "90": return "Đã làm sạch";
            case "120": return "Nhật ký công trình";
            case "60": case "130": return "Vị trí";
            case "map01": return "Bản đồ";
            case "dasher01": return "Giao hàng";
            default: return template.name;
        }
    }
    private void showPicker() {
        editing = false;
        LinearLayout root = column();
        root.addView(header("Mẫu", () -> dialog.dismiss()));
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
            border.setStroke(dp(template.id.equals(selected.id) ? 2 : 0), 0xff1687ec);
            card.setBackground(border);
            FrameLayout thumbnail = new FrameLayout(activity);
            thumbnail.setBackground(background(0xff818891, 0, 4));
            thumbnail.setClipToOutline(true);
            View preview = new View(activity) {
                @Override protected void onDraw(Canvas canvas) {
                    canvas.drawColor(0xff818991);
                    WatermarkTemplate.Snapshot sample = template.id.equals(selected.id) ? selected : template.defaultSnapshot().withLiveData(selected.liveData);
                    float scale = getWidth() / 360f;
                    int saved = canvas.save();
                    canvas.translate(0, getHeight() - 300f * scale);
                    canvas.scale(scale, scale);
                    WatermarkRenderer.drawLive(canvas, 360, 300, new Date(), address.get(), 1f, sample);
                    canvas.restoreToCount(saved);
                }
            };
            thumbnail.addView(preview, new FrameLayout.LayoutParams(-1, -1));
            if (template.id.equals(selected.id)) {
                View shade = new View(activity);
                shade.setBackgroundColor(0x66000000);
                thumbnail.addView(shade, new FrameLayout.LayoutParams(-1, -1));
                TextView edit = flatAction("Chỉnh sửa", Color.WHITE, 0xff1296e8, this::showEditor);
                FrameLayout.LayoutParams editParams = new FrameLayout.LayoutParams(dp(102), dp(32), Gravity.CENTER);
                thumbnail.addView(edit, editParams);
            }
            card.addView(thumbnail, new LinearLayout.LayoutParams(-1, dp(82)));
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
            case 1: return "Thời gian";
            case 2: return "Vị trí";
            case 3: return "Tọa độ";
            case 4: return "Thời tiết";
            case 5: return "Độ cao";
            case 6: return "La bàn";
            case 13: return "21".equals(selected.id) ? "Tiêu đề" : "Ghi chú";
            case 88: return "Kiểu chữ nghệ thuật";
            case 210: return "Bản đồ";
            case 620: return "Danh thiếp";
            case 630: return "Thẻ";
            case 640: return "Mã ảnh";
            default: return field.title;
        }
    }
    private void showEditor() {
        editing = true;
        LinearLayout root = column();
        root.addView(header("Chỉnh sửa mẫu", () -> dialog.dismiss()));
        ScrollView scroll = new ScrollView(activity);
        LinearLayout fields = column();
        fields.setPadding(dp(14), 0, dp(14), dp(12));
        for (WatermarkTemplate.Field field : selected.fields.values()) {
            if (field.id == 1) {
                TextView time = label("Thời gian: theo đồng hồ thiết bị", 16);
                time.setPadding(dp(64), dp(16), 0, dp(16));
                fields.addView(time);
                continue;
            }
            if (field.id == 640) continue;
            LinearLayout row = new LinearLayout(activity);
            row.setGravity(Gravity.CENTER_VERTICAL);
            Switch toggle = new WatermarkSwitch(activity);
            toggle.setChecked(field.enabled);
            toggle.setContentDescription(field.title);
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
                EditText input = new EditText(activity);
                input.setText(selected.field(field.id).content);
                new AlertDialog.Builder(activity).setTitle(field.title).setView(input)
                        .setNegativeButton("Hủy", null).setPositiveButton("Lưu", (d, w) -> {
                            update(selected.withField(field.id, true, input.getText().toString()));
                            showEditor();
                        }).show();
            });
            fields.addView(row, new LinearLayout.LayoutParams(-1, dp(50)));
            View separator = new View(activity);
            separator.setBackgroundColor(0xffeeeeee);
            LinearLayout.LayoutParams separatorParams = new LinearLayout.LayoutParams(-1, dp(.5f));
            separatorParams.leftMargin = dp(64);
            fields.addView(separator, separatorParams);
        }
        TextView size = label("Kích thước", 16);
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
        fields.addView(button("Màu", () -> {
            String[] names = {"Vàng", "Trắng", "Xanh dương", "Đỏ", "Xanh lá"};
            int[] colors = {0xffffc233, Color.WHITE, 0xff1687ec, 0xffef4438, 0xff2bbf57};
            new AlertDialog.Builder(activity).setTitle("Màu nhấn").setItems(names,
                    (d, which) -> update(selected.withTheme(colors[which], selected.textColor))).show();
        }));
        scroll.addView(fields);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout footer = new LinearLayout(activity);
        footer.setPadding(dp(16), dp(12), dp(16), dp(12));
        footer.setBackgroundColor(0xfff8f8f8);
        TextView back = flatAction("Mẫu", 0xff555555, Color.WHITE, this::showPicker);
        back.setBackground(background(Color.WHITE, 0xffdddddd, 4));
        footer.addView(back, new LinearLayout.LayoutParams(dp(96), dp(44)));
        TextView done = flatAction("Xong", Color.WHITE, 0xff0065ff, () -> dialog.dismiss());
        LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(0, dp(44), 1);
        doneParams.leftMargin = dp(12);
        footer.addView(done, doneParams);
        root.addView(footer);
        open(root);
    }
}
