package com.pgkit.timemarkcamera;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.camera.view.PreviewView;

/** Builds and owns the portrait-only camera screen. */
final class CameraScreen {
    interface Actions {
        void onMenu();
        void onToggleFlash();
        void onSwitchCamera();
        void onEditWatermark();
        void onRefreshLocation();
        void onOpenGallery();
        void onCapture();
        void onOpenTemplates();
    }

    private final ComponentActivity activity;
    private final Actions actions;
    private final PreviewView previewView;
    private final WatermarkOverlayView watermarkView;
    private TextView locationState;
    private TextView flashButton;
    private Button shutter;

    CameraScreen(ComponentActivity activity, Actions actions) {
        this.activity = activity;
        this.actions = actions;

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        activity.setContentView(root);

        root.addView(createTopBar(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(68)));

        FrameLayout preview = new FrameLayout(activity);
        preview.setBackgroundColor(Color.rgb(28, 28, 28));
        root.addView(preview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        previewView = new PreviewView(activity);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        previewView.setContentDescription("Khung xem trước camera");
        preview.addView(previewView, matchFrame());

        watermarkView = new WatermarkOverlayView(activity);
        preview.addView(watermarkView, matchFrame());
        addPreviewControls(preview);

        root.addView(createBottomPanel(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(250)));
    }

    PreviewView previewView() {
        return previewView;
    }

    WatermarkOverlayView watermarkView() {
        return watermarkView;
    }

    void setLocationStatus(String text) {
        locationState.setText(text);
    }

    void setFlashEnabled(boolean enabled) {
        flashButton.setTextColor(enabled ? WatermarkRenderer.YELLOW : Color.rgb(30, 30, 30));
        announce(enabled ? "Đã bật flash" : "Đã tắt flash");
    }

    void setCaptureEnabled(boolean enabled) {
        shutter.setEnabled(enabled);
        shutter.setAlpha(enabled ? 1f : 0.55f);
    }

    private View createTopBar() {
        LinearLayout bar = new LinearLayout(activity);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10), dp(4), dp(10), dp(4));
        bar.setBackgroundColor(Color.WHITE);

        TextView menu = iconButton("☰", "Mở menu");
        menu.setTextSize(31);
        menu.setOnClickListener(v -> actions.onMenu());
        bar.addView(menu, fixed(dp(54), dp(58)));

        flashButton = iconButton("ϟ", "Bật hoặc tắt đèn flash");
        flashButton.setTextSize(34);
        flashButton.setOnClickListener(v -> actions.onToggleFlash());
        bar.addView(flashButton, fixed(dp(54), dp(58)));

        TextView verified = textButton("♢  Xác thực ảnh", "Xác thực ảnh", 15, Color.BLACK);
        verified.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        verified.setBackgroundResource(R.drawable.pill_outline);
        LinearLayout.LayoutParams verifiedParams = new LinearLayout.LayoutParams(0, dp(42), 1f);
        verifiedParams.setMargins(dp(8), 0, dp(8), 0);
        bar.addView(verified, verifiedParams);

        TextView plus = textButton("✦ Plus", "Gói Plus", 16, Color.WHITE);
        plus.setTypeface(Typeface.DEFAULT_BOLD);
        plus.setBackgroundResource(R.drawable.pill_dark);
        bar.addView(plus, fixed(dp(76), dp(42)));

        TextView switchCamera = iconButton("▣", "Đổi camera trước sau");
        switchCamera.setTextSize(28);
        switchCamera.setOnClickListener(v -> actions.onSwitchCamera());
        LinearLayout.LayoutParams switchParams = fixed(dp(54), dp(58));
        switchParams.setMargins(dp(6), 0, 0, 0);
        bar.addView(switchCamera, switchParams);
        return bar;
    }

    private void addPreviewControls(FrameLayout preview) {
        LinearLayout zoom = new LinearLayout(activity);
        zoom.setOrientation(LinearLayout.VERTICAL);
        zoom.setGravity(Gravity.CENTER);
        zoom.setPadding(0, dp(4), 0, dp(4));

        TextView two = transparentLabel("2", 15, Color.WHITE);
        two.setContentDescription("Thu phóng 2 lần");
        TextView one = transparentLabel("1x", 16, Color.WHITE);
        one.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(0x22000000);
        circle.setStroke(dp(1), Color.WHITE);
        one.setBackground(circle);
        one.setContentDescription("Thu phóng 1 lần");
        TextView half = transparentLabel("0.5", 15, Color.WHITE);
        half.setContentDescription("Thu phóng 0 phẩy 5 lần");
        zoom.addView(two, fixed(dp(48), dp(40)));
        zoom.addView(one, fixed(dp(48), dp(48)));
        zoom.addView(half, fixed(dp(48), dp(40)));
        FrameLayout.LayoutParams zoomParams = new FrameLayout.LayoutParams(dp(56), dp(145),
                Gravity.END | Gravity.CENTER_VERTICAL);
        zoomParams.rightMargin = dp(6);
        preview.addView(zoom, zoomParams);

        TextView edit = iconButton("□", "Chỉnh sửa watermark");
        edit.setTextColor(Color.WHITE);
        edit.setTextSize(33);
        edit.setBackgroundColor(Color.TRANSPARENT);
        edit.setOnClickListener(v -> actions.onEditWatermark());
        FrameLayout.LayoutParams editParams = new FrameLayout.LayoutParams(dp(58), dp(58),
                Gravity.END | Gravity.BOTTOM);
        editParams.rightMargin = dp(4);
        editParams.bottomMargin = dp(66);
        preview.addView(edit, editParams);

        TextView pin = iconButton("⌾", "Làm mới vị trí");
        pin.setTextColor(Color.WHITE);
        pin.setTextSize(38);
        pin.setBackgroundColor(Color.TRANSPARENT);
        pin.setOnClickListener(v -> actions.onRefreshLocation());
        FrameLayout.LayoutParams pinParams = new FrameLayout.LayoutParams(dp(58), dp(58),
                Gravity.END | Gravity.BOTTOM);
        pinParams.rightMargin = dp(4);
        pinParams.bottomMargin = dp(6);
        preview.addView(pin, pinParams);
    }

    private View createBottomPanel() {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(8), dp(16), dp(6));
        panel.setBackgroundColor(Color.WHITE);

        locationState = textButton("Đang lấy thời gian và vị trí hệ thống…",
                "Trạng thái vị trí", 14, Color.rgb(23, 75, 131));
        locationState.setBackgroundResource(R.drawable.login_background);
        locationState.setOnClickListener(v -> actions.onRefreshLocation());
        panel.addView(locationState, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        LinearLayout captureRow = new LinearLayout(activity);
        captureRow.setGravity(Gravity.CENTER);
        captureRow.setPadding(dp(12), dp(12), dp(12), dp(2));
        panel.addView(captureRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView gallery = transparentLabel("▣\nThư viện", 15, Color.rgb(82, 82, 82));
        gallery.setGravity(Gravity.CENTER);
        gallery.setTypeface(Typeface.DEFAULT_BOLD);
        gallery.setContentDescription("Mở ảnh vừa chụp trong thư viện");
        gallery.setOnClickListener(v -> actions.onOpenGallery());
        captureRow.addView(gallery, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        shutter = new Button(activity);
        shutter.setText("");
        shutter.setContentDescription("Chụp ảnh");
        shutter.setBackgroundResource(R.drawable.shutter_ring);
        shutter.setOnClickListener(v -> actions.onCapture());
        LinearLayout.LayoutParams shutterParams = fixed(dp(76), dp(76));
        shutterParams.setMargins(dp(20), 0, dp(20), 0);
        captureRow.addView(shutter, shutterParams);

        TextView templates = transparentLabel("▤\nBản mẫu", 15, Color.rgb(82, 82, 82));
        templates.setGravity(Gravity.CENTER);
        templates.setTypeface(Typeface.DEFAULT_BOLD);
        templates.setContentDescription("Mở bản mẫu watermark");
        templates.setOnClickListener(v -> actions.onOpenTemplates());
        captureRow.addView(templates, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        LinearLayout tabs = new LinearLayout(activity);
        tabs.setGravity(Gravity.CENTER);
        String[] labels = {"Báo cáo", "Video", "Ảnh", "Chấm công", "Danh sách"};
        for (String label : labels) {
            TextView tab = transparentLabel(label + ("Ảnh".equals(label) ? "\n•" : ""),
                    14, "Ảnh".equals(label) ? Color.BLACK : Color.rgb(150, 150, 150));
            tab.setGravity(Gravity.CENTER);
            tab.setSingleLine(false);
            tab.setTypeface(Typeface.DEFAULT_BOLD);
            tabs.addView(tab, new LinearLayout.LayoutParams(0, dp(48), 1f));
        }
        panel.addView(tabs, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        return panel;
    }

    private TextView iconButton(String text, String description) {
        TextView view = transparentLabel(text, 28, Color.rgb(30, 30, 30));
        view.setGravity(Gravity.CENTER);
        view.setContentDescription(description);
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private TextView textButton(String text, String description, float size, int color) {
        TextView view = transparentLabel(text, size, color);
        view.setGravity(Gravity.CENTER);
        view.setContentDescription(description);
        view.setClickable(true);
        view.setFocusable(true);
        view.setPadding(dp(7), 0, dp(7), 0);
        return view;
    }

    private TextView transparentLabel(String text, float size, int color) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setFontFeatureSettings("tnum");
        view.setBackgroundColor(Color.TRANSPARENT);
        return view;
    }

    private void announce(String text) {
        activity.getWindow().getDecorView().announceForAccessibility(text);
    }

    private int dp(float value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static FrameLayout.LayoutParams matchFrame() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private static LinearLayout.LayoutParams fixed(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }
}
