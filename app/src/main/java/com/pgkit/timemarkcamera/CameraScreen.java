package com.pgkit.timemarkcamera;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
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
        void onZoomSelected(float ratio);
        void onExposureChanged(int direction);
    }

    private final ComponentActivity activity;
    private final Actions actions;
    private final PreviewView previewView;
    private final WatermarkOverlayView watermarkView;
    private final FrameLayout previewLayer;
    private TextView locationState;
    private ImageButton flashButton;
    private Button shutter;
    private TextView zoomHalf;
    private TextView zoomOne;
    private TextView zoomTwo;
    private TextView exposureValue;
    private View focusIndicator;

    CameraScreen(ComponentActivity activity, Actions actions) {
        this.activity = activity;
        this.actions = actions;

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        activity.setContentView(root);

        root.addView(createTopBar(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(68)));

        previewLayer = new FrameLayout(activity);
        previewLayer.setBackgroundColor(Color.rgb(28, 28, 28));
        root.addView(previewLayer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        previewView = new PreviewView(activity);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        previewView.setContentDescription("Khung xem trước camera");
        previewLayer.addView(previewView, matchFrame());

        watermarkView = new WatermarkOverlayView(activity);
        previewLayer.addView(watermarkView, matchFrame());
        addFocusIndicator(previewLayer);
        addPreviewControls(previewLayer);

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
        flashButton.setImageTintList(ColorStateList.valueOf(
                enabled ? WatermarkRenderer.YELLOW : Color.rgb(30, 30, 30)));
        announce(enabled ? "Đã bật flash" : "Đã tắt flash");
    }

    void setCaptureEnabled(boolean enabled) {
        shutter.setEnabled(enabled);
        shutter.setAlpha(enabled ? 1f : 0.55f);
    }

    void setZoomRatio(float ratio) {
        if (!Float.isFinite(ratio)) return;
        applyZoomStyle(zoomHalf, Math.abs(ratio - 0.5f) < 0.08f);
        applyZoomStyle(zoomOne, Math.abs(ratio - 1f) < 0.08f);
        applyZoomStyle(zoomTwo, Math.abs(ratio - 2f) < 0.12f);
    }

    void setExposureValue(float ev) {
        if (!Float.isFinite(ev)) {
            announce("Thiết bị không hỗ trợ bù sáng");
            return;
        }
        exposureValue.setText(String.format(java.util.Locale.US, "EV\n%+.1f", ev));
        announce(String.format(java.util.Locale.US, "Bù sáng %+.1f", ev));
    }

    void showFocusIndicator(float x, float y) {
        int size = dp(56);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) focusIndicator.getLayoutParams();
        params.leftMargin = Math.max(0, Math.min(previewLayer.getWidth() - size,
                Math.round(x) - size / 2));
        params.topMargin = Math.max(0, Math.min(previewLayer.getHeight() - size,
                Math.round(y) - size / 2));
        focusIndicator.setLayoutParams(params);
        focusIndicator.animate().cancel();
        focusIndicator.setVisibility(View.VISIBLE);
        focusIndicator.setAlpha(1f);
        focusIndicator.setScaleX(1.3f);
        focusIndicator.setScaleY(1.3f);
        focusIndicator.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(150L)
                .withEndAction(() -> focusIndicator.animate()
                        .alpha(0f)
                        .setStartDelay(600L)
                        .setDuration(200L)
                        .withEndAction(() -> focusIndicator.setVisibility(View.GONE))
                        .start())
                .start();
    }

    private View createTopBar() {
        LinearLayout bar = new LinearLayout(activity);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10), dp(4), dp(10), dp(4));
        bar.setBackgroundColor(Color.WHITE);

        ImageButton menu = imageButton(R.drawable.ic_menu, "Mở menu", Color.rgb(30, 30, 30));
        menu.setOnClickListener(v -> actions.onMenu());
        bar.addView(menu, fixed(dp(54), dp(58)));

        flashButton = imageButton(R.drawable.ic_flash, "Bật hoặc tắt đèn flash",
                Color.rgb(30, 30, 30));
        flashButton.setOnClickListener(v -> actions.onToggleFlash());
        bar.addView(flashButton, fixed(dp(54), dp(58)));

        TextView verified = textButton("Xác thực ảnh", "Xác thực ảnh", 15, Color.BLACK);
        verified.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        verified.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_verified, 0, 0, 0);
        verified.setCompoundDrawablePadding(dp(4));
        verified.setBackgroundResource(R.drawable.pill_outline);
        LinearLayout.LayoutParams verifiedParams = new LinearLayout.LayoutParams(0, dp(42), 1f);
        verifiedParams.setMargins(dp(8), 0, dp(8), 0);
        bar.addView(verified, verifiedParams);

        TextView plus = textButton("Plus", "Gói Plus", 16, Color.WHITE);
        plus.setTypeface(Typeface.DEFAULT_BOLD);
        plus.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_plus, 0, 0, 0);
        plus.setCompoundDrawablePadding(dp(3));
        plus.setBackgroundResource(R.drawable.pill_dark);
        bar.addView(plus, fixed(dp(76), dp(42)));

        ImageButton switchCamera = imageButton(R.drawable.ic_switch_camera,
                "Đổi camera trước sau", Color.rgb(30, 30, 30));
        switchCamera.setOnClickListener(v -> actions.onSwitchCamera());
        LinearLayout.LayoutParams switchParams = fixed(dp(54), dp(58));
        switchParams.setMargins(dp(6), 0, 0, 0);
        bar.addView(switchCamera, switchParams);
        return bar;
    }

    private void addPreviewControls(FrameLayout preview) {
        LinearLayout zoom = new LinearLayout(activity);
        zoom.setOrientation(LinearLayout.VERTICAL);
        zoom.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);

        zoomTwo = zoomButton("2", 2f, "Thu phóng 2 lần");
        zoomOne = zoomButton("1x", 1f, "Thu phóng 1 lần");
        zoomHalf = zoomButton("0.5", 0.5f, "Thu phóng 0 phẩy 5 lần");
        applyZoomStyle(zoomOne, true);
        zoom.addView(zoomTwo, fixed(dp(48), dp(48)));
        zoom.addView(zoomOne, fixed(dp(48), dp(48)));
        zoom.addView(zoomHalf, fixed(dp(48), dp(48)));
        FrameLayout.LayoutParams zoomParams = new FrameLayout.LayoutParams(dp(56), dp(145),
                Gravity.END | Gravity.CENTER_VERTICAL);
        zoomParams.rightMargin = dp(6);
        preview.addView(zoom, zoomParams);

        LinearLayout exposure = new LinearLayout(activity);
        exposure.setOrientation(LinearLayout.VERTICAL);
        exposure.setGravity(Gravity.CENTER);
        TextView brighter = transparentLabel("+", 25, Color.WHITE);
        brighter.setGravity(Gravity.CENTER);
        brighter.setContentDescription("Tăng độ sáng");
        brighter.setOnClickListener(v -> actions.onExposureChanged(1));
        exposureValue = transparentLabel("EV\n+0.0", 12, Color.WHITE);
        exposureValue.setGravity(Gravity.CENTER);
        exposureValue.setContentDescription("Mức bù sáng");
        TextView darker = transparentLabel("−", 25, Color.WHITE);
        darker.setGravity(Gravity.CENTER);
        darker.setContentDescription("Giảm độ sáng");
        darker.setOnClickListener(v -> actions.onExposureChanged(-1));
        exposure.addView(brighter, fixed(dp(48), dp(42)));
        exposure.addView(exposureValue, fixed(dp(48), dp(48)));
        exposure.addView(darker, fixed(dp(48), dp(42)));
        FrameLayout.LayoutParams exposureParams = new FrameLayout.LayoutParams(dp(56), dp(132),
                Gravity.START | Gravity.CENTER_VERTICAL);
        exposureParams.leftMargin = dp(6);
        preview.addView(exposure, exposureParams);

        ImageButton edit = imageButton(R.drawable.ic_edit, "Chỉnh sửa watermark", Color.WHITE);
        edit.setOnClickListener(v -> actions.onEditWatermark());
        FrameLayout.LayoutParams editParams = new FrameLayout.LayoutParams(dp(58), dp(58),
                Gravity.END | Gravity.BOTTOM);
        editParams.rightMargin = dp(4);
        editParams.bottomMargin = dp(66);
        preview.addView(edit, editParams);

        ImageButton pin = imageButton(R.drawable.ic_location_target, "Làm mới vị trí", Color.WHITE);
        pin.setOnClickListener(v -> actions.onRefreshLocation());
        FrameLayout.LayoutParams pinParams = new FrameLayout.LayoutParams(dp(58), dp(58),
                Gravity.END | Gravity.BOTTOM);
        pinParams.rightMargin = dp(4);
        pinParams.bottomMargin = dp(6);
        preview.addView(pin, pinParams);
    }

    private void addFocusIndicator(FrameLayout preview) {
        focusIndicator = new View(activity);
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.RECTANGLE);
        ring.setColor(Color.TRANSPARENT);
        ring.setStroke(dp(2), WatermarkRenderer.YELLOW);
        ring.setCornerRadius(dp(6));
        focusIndicator.setBackground(ring);
        focusIndicator.setVisibility(View.GONE);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(56), dp(56),
                Gravity.START | Gravity.TOP);
        preview.addView(focusIndicator, params);
    }

    private TextView zoomButton(String label, float ratio, String description) {
        TextView button = transparentLabel(label, "1x".equals(label) ? 16 : 15, Color.WHITE);
        button.setGravity(Gravity.CENTER);
        button.setContentDescription(description);
        button.setClickable(true);
        button.setFocusable(true);
        button.setOnClickListener(v -> actions.onZoomSelected(ratio));
        return button;
    }

    private void applyZoomStyle(TextView button, boolean selected) {
        if (button == null) return;
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(selected ? 0x33000000 : Color.TRANSPARENT);
        if (selected) background.setStroke(dp(1), Color.WHITE);
        button.setBackground(background);
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

        TextView gallery = transparentLabel("Thư viện", 15, Color.rgb(82, 82, 82));
        gallery.setGravity(Gravity.CENTER);
        gallery.setTypeface(Typeface.DEFAULT_BOLD);
        gallery.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_gallery, 0, 0);
        gallery.setCompoundDrawablePadding(dp(4));
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

        TextView templates = transparentLabel("Bản mẫu", 15, Color.rgb(82, 82, 82));
        templates.setGravity(Gravity.CENTER);
        templates.setTypeface(Typeface.DEFAULT_BOLD);
        templates.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_template, 0, 0);
        templates.setCompoundDrawablePadding(dp(4));
        templates.setContentDescription("Mở bản mẫu watermark");
        templates.setOnClickListener(v -> actions.onOpenTemplates());
        captureRow.addView(templates, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        LinearLayout tabs = new LinearLayout(activity);
        tabs.setGravity(Gravity.CENTER);
        String[] labels = {"Báo cáo", "Video", "Ảnh", "Chấm công", "Danh sách"};
        for (String label : labels) {
            boolean selected = "Ảnh".equals(label);
            TextView tab = transparentLabel(label,
                    14, selected ? Color.BLACK : Color.rgb(150, 150, 150));
            tab.setGravity(Gravity.CENTER);
            tab.setTypeface(Typeface.DEFAULT_BOLD);
            if (selected) {
                tab.setCompoundDrawablesWithIntrinsicBounds(
                        0, 0, 0, R.drawable.ic_tab_selected);
                tab.setCompoundDrawablePadding(dp(4));
            }
            tabs.addView(tab, new LinearLayout.LayoutParams(0, dp(48), 1f));
        }
        panel.addView(tabs, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        return panel;
    }

    private ImageButton imageButton(int drawable, String description, int tint) {
        ImageButton view = new ImageButton(activity);
        view.setImageResource(drawable);
        view.setImageTintList(ColorStateList.valueOf(tint));
        view.setScaleType(ImageButton.ScaleType.CENTER);
        view.setPadding(dp(12), dp(12), dp(12), dp(12));
        view.setBackgroundColor(Color.TRANSPARENT);
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
