package com.pgkit.timemarkcamera;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
    private LinearLayout zoomControls;
    private FocusExposureView focusExposureControl;
    private ImageView latestPhotoPreview;
    private View focusIndicator;
    private final Runnable settleFocusControls = () -> {
        if (focusIndicator != null) focusIndicator.setAlpha(0.6f);
        if (focusExposureControl != null) focusExposureControl.setAlpha(0.6f);
    };
    private final Runnable hideFocusControls = () -> {
        if (focusIndicator != null) focusIndicator.animate().alpha(0f).setDuration(300L)
                .withEndAction(() -> focusIndicator.setVisibility(View.GONE)).start();
        if (focusExposureControl != null) focusExposureControl.animate().alpha(0f)
                .setDuration(300L)
                .withEndAction(() -> focusExposureControl.setVisibility(View.GONE)).start();
    };

    CameraScreen(ComponentActivity activity, Actions actions) {
        this.activity = activity;
        this.actions = actions;

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            // targetSdk 35 draws edge-to-edge. Keep the fixed camera header below the status bar
            // on both compact and tall devices while preserving a portrait-only composition.
            view.setPadding(0, insets.getSystemWindowInsetTop(), 0,
                    insets.getSystemWindowInsetBottom());
            return insets;
        });
        activity.getWindow().setStatusBarColor(Color.WHITE);
        activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        activity.setContentView(root);

        root.addView(createTopBar(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        previewLayer = new PortraitPreviewFrame(activity);
        previewLayer.setBackgroundColor(Color.rgb(28, 28, 28));
        root.addView(previewLayer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

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
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
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
        flashButton.setImageResource(enabled ? R.drawable.ic_flash : R.drawable.ic_flash_off);
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
        boolean half = Math.abs(ratio - 0.5f) < 0.03f;
        boolean two = Math.abs(ratio - 2f) < 0.03f;
        zoomOne.setText(String.format(java.util.Locale.US, "%.1fx", half || two ? 1f : ratio));
        applyZoomStyle(zoomHalf, half);
        applyZoomStyle(zoomOne, !half && !two);
        applyZoomStyle(zoomTwo, two);
    }

    /** Camera capability hook, called after binding or changing lenses. */
    void setZoomRange(float minimum, float maximum) {
        if (!Float.isFinite(minimum) || !Float.isFinite(maximum) || minimum > maximum) return;
        zoomHalf.setVisibility(minimum <= 0.5f && maximum >= 0.5f ? View.VISIBLE : View.GONE);
        zoomTwo.setVisibility(maximum >= 2f ? View.VISIBLE : View.GONE);
        updateZoomLayout();
    }

    void setExposureValue(float ev) {
        if (!Float.isFinite(ev)) {
            focusExposureControl.setEnabled(false);
            announce("Thiết bị không hỗ trợ bù sáng");
            return;
        }
        focusExposureControl.setEnabled(true);
        focusExposureControl.setContentDescription(String.format(java.util.Locale.US,
                "Bù sáng %+.1f EV", ev));
        announce(String.format(java.util.Locale.US, "Bù sáng %+.1f", ev));
    }

    /** Replaces the library placeholder with the just-saved photo, without changing its action. */
    void setLatestPhoto(Uri uri) {
        if (uri == null || latestPhotoPreview == null) return;
        latestPhotoPreview.setImageTintList(null);
        latestPhotoPreview.setImageURI(uri);
        latestPhotoPreview.setContentDescription("Xem trước ảnh vừa chụp");
    }

    void showFocusIndicator(float x, float y) {
        int size = dp(72);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) focusIndicator.getLayoutParams();
        // Keep the target centered on the metering point, clipping naturally at preview edges.
        params.leftMargin = Math.round(x) - size / 2;
        params.topMargin = Math.round(y) - size / 2;
        focusIndicator.setLayoutParams(params);
        focusIndicator.animate().cancel();
        focusIndicator.animate().withEndAction(null);
        focusIndicator.setVisibility(View.VISIBLE);
        focusIndicator.setAlpha(1f);
        focusIndicator.setScaleX(1.05f);
        focusIndicator.setScaleY(1.05f);
        focusIndicator.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(150L)
                .start();
        showFocusExposureControl(x, y);
        previewLayer.removeCallbacks(hideFocusControls);
        previewLayer.removeCallbacks(settleFocusControls);
        previewLayer.postDelayed(settleFocusControls, 1200L);
        previewLayer.postDelayed(hideFocusControls, 2700L);
    }

    private View createTopBar() {
        LinearLayout bar = new LinearLayout(activity);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(4), dp(2), dp(14), dp(2));
        bar.setBackgroundColor(Color.WHITE);

        ImageButton menu = imageButton(R.drawable.ic_menu, "Mở menu", Color.rgb(30, 30, 30));
        menu.setOnClickListener(v -> actions.onMenu());
        bar.addView(menu, fixed(dp(54), dp(48)));

        flashButton = imageButton(R.drawable.ic_flash_off, "Bật hoặc tắt đèn flash",
                Color.rgb(30, 30, 30));
        flashButton.setOnClickListener(v -> actions.onToggleFlash());
        bar.addView(flashButton, fixed(dp(54), dp(48)));

        bar.addView(new View(activity), fixed(dp(20), 1));

        TextView verified = textButton("Xác thực ảnh", "Xác thực ảnh", 15, Color.BLACK);
        verified.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        verified.setSingleLine(true);
        verified.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_verified, 0, 0, 0);
        verified.setCompoundDrawablePadding(dp(4));
        verified.setBackgroundResource(R.drawable.pill_outline);
        bar.addView(verified, fixed(dp(106), dp(27)));

        View flexibleSpace = new View(activity);
        bar.addView(flexibleSpace, new LinearLayout.LayoutParams(0, 1, 1f));

        TextView plus = textButton("Plus", "Gói Plus", 15, Color.WHITE);
        plus.setTypeface(Typeface.DEFAULT_BOLD);
        plus.setSingleLine(true);
        plus.setPadding(dp(5), 0, dp(5), 0);
        plus.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_plus, 0, 0, 0);
        plus.setCompoundDrawablePadding(dp(3));
        plus.setBackgroundResource(R.drawable.pill_dark);
        bar.addView(plus, fixed(dp(58), dp(24)));

        ImageButton switchCamera = imageButton(R.drawable.ic_switch_camera,
                "Đổi camera trước sau", Color.rgb(30, 30, 30));
        switchCamera.setOnClickListener(v -> actions.onSwitchCamera());
        bar.addView(switchCamera, fixed(dp(44), dp(48)));
        return bar;
    }

    private void addPreviewControls(FrameLayout preview) {
        LinearLayout zoom = new LinearLayout(activity);
        zoomControls = zoom;
        zoom.setOrientation(LinearLayout.VERTICAL);
        zoom.setGravity(Gravity.CENTER);
        zoomTwo = zoomButton("2", 2f, "Thu phóng 2 lần");
        zoomOne = zoomButton("1.0x", 1f, "Thu phóng 1 lần");
        zoomHalf = zoomButton("0.5", 0.5f, "Góc siêu rộng 0 phẩy 5 lần");
        applyZoomStyle(zoomOne, true);
        zoom.addView(zoomTwo, fixed(dp(44), dp(44)));
        zoom.addView(zoomOne, fixed(dp(44), dp(44)));
        zoom.addView(zoomHalf, fixed(dp(44), dp(44)));
        FrameLayout.LayoutParams zoomParams = new FrameLayout.LayoutParams(dp(48), dp(132),
                Gravity.END | Gravity.BOTTOM);
        zoomParams.rightMargin = 0;
        zoomParams.bottomMargin = dp(105);
        preview.addView(zoom, zoomParams);

        ImageButton edit = imageButton(R.drawable.ic_edit, "Chỉnh sửa watermark", Color.WHITE);
        edit.setOnClickListener(v -> actions.onEditWatermark());
        FrameLayout.LayoutParams editParams = new FrameLayout.LayoutParams(dp(48), dp(48),
                Gravity.END | Gravity.BOTTOM);
        editParams.rightMargin = 0;
        editParams.bottomMargin = dp(51);
        preview.addView(edit, editParams);

        ImageButton pin = imageButton(R.drawable.ic_location_target, "Làm mới vị trí", Color.WHITE);
        pin.setOnClickListener(v -> actions.onRefreshLocation());
        FrameLayout.LayoutParams pinParams = new FrameLayout.LayoutParams(dp(48), dp(48),
                Gravity.END | Gravity.BOTTOM);
        pinParams.rightMargin = 0;
        pinParams.bottomMargin = 0;
        preview.addView(pin, pinParams);
    }

    private void updateZoomLayout() {
        int count = 1 + (zoomHalf.getVisibility() == View.VISIBLE ? 1 : 0)
                + (zoomTwo.getVisibility() == View.VISIBLE ? 1 : 0);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) zoomControls.getLayoutParams();
        params.height = dp(count * 44);
        params.bottomMargin = dp(105);
        zoomControls.setLayoutParams(params);
    }

    private void addFocusIndicator(FrameLayout preview) {
        focusIndicator = new FocusRingView(activity);
        focusIndicator.setVisibility(View.GONE);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(72), dp(72),
                Gravity.START | Gravity.TOP);
        preview.addView(focusIndicator, params);

        focusExposureControl = new FocusExposureView(activity, previewView,
                actions::onExposureChanged, this::beginExposureDrag,
                this::keepFocusControlsVisible);
        focusExposureControl.setVisibility(View.GONE);
        preview.addView(focusExposureControl, matchFrame());
    }

    private void showFocusExposureControl(float x, float y) {
        if (focusExposureControl == null) return;
        focusExposureControl.setFocusPoint(x, y);
        focusExposureControl.animate().cancel();
        focusExposureControl.animate().withEndAction(null);
        focusExposureControl.setAlpha(1f);
        focusExposureControl.setVisibility(View.VISIBLE);
    }

    private void beginExposureDrag() {
        previewLayer.removeCallbacks(hideFocusControls);
        previewLayer.removeCallbacks(settleFocusControls);
        focusIndicator.animate().cancel();
        focusExposureControl.animate().cancel();
        focusIndicator.setAlpha(1f);
        focusExposureControl.setAlpha(1f);
    }

    private void keepFocusControlsVisible() {
        previewLayer.removeCallbacks(hideFocusControls);
        previewLayer.postDelayed(hideFocusControls, 1200L);
    }

    private TextView zoomButton(String label, float ratio, String description) {
        TextView button = transparentLabel(label, "1.0x".equals(label) ? 12 : 11, Color.WHITE);
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
        background.setColor(selected ? 0x33000000 : 0x4d000000);
        if (selected) background.setStroke(dp(1), Color.WHITE);
        button.setBackground(new InsetDrawable(background, dp(6)));
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

        LinearLayout gallery = new LinearLayout(activity);
        gallery.setOrientation(LinearLayout.VERTICAL);
        gallery.setGravity(Gravity.CENTER);
        gallery.setContentDescription("Mở ảnh vừa chụp trong thư viện");
        gallery.setOnClickListener(v -> actions.onOpenGallery());
        latestPhotoPreview = new ImageView(activity);
        latestPhotoPreview.setImageResource(R.drawable.ic_gallery);
        latestPhotoPreview.setImageTintList(ColorStateList.valueOf(Color.rgb(82, 82, 82)));
        latestPhotoPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        latestPhotoPreview.setAdjustViewBounds(true);
        latestPhotoPreview.setOnClickListener(v -> actions.onOpenGallery());
        gallery.addView(latestPhotoPreview, fixed(dp(48), dp(42)));
        TextView galleryLabel = transparentLabel("Thư viện", 15, Color.rgb(82, 82, 82));
        galleryLabel.setTypeface(Typeface.DEFAULT_BOLD);
        galleryLabel.setGravity(Gravity.CENTER);
        galleryLabel.setOnClickListener(v -> actions.onOpenGallery());
        gallery.addView(galleryLabel, fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(30)));
        captureRow.addView(gallery, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        shutter = new Button(activity);
        shutter.setText("");
        shutter.setContentDescription("Chụp ảnh");
        shutter.setBackgroundResource(R.drawable.shutter_ring);
        shutter.setOnClickListener(v -> actions.onCapture());
        LinearLayout.LayoutParams shutterParams = fixed(dp(80), dp(80));
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
