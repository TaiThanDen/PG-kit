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
        void onEditTime();
        void onSelectAspectRatio();
        void onOpenGallery();
        void onCapture();
        void onOpenTemplates();
        void onModeChanged(boolean videoMode);
        void onZoomSelected(float ratio);
        void onExposureChanged(int direction);
    }

    private final ComponentActivity activity;
    private final Actions actions;
    private final PreviewView previewView;
    private final WatermarkOverlayView watermarkView;
    private final FrameLayout previewLayer;
    private CameraViewportLayout viewport;
    private View bottomPanel;
    private View clockControl, locationControl;
    private PhotoAspectRatio selectedRatio = PhotoAspectRatio.STANDARD;
    private TextView locationState;
    private TextView ratioButton;
    private TextView recordingTimer;
    private ImageButton flashButton;
    private Button shutter;
    private TextView photoTab;
    private TextView videoTab;
    private FrameLayout modeSwitch;
    private View modeIndicator;
    private boolean videoMode;
    private boolean recording;
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

        previewLayer = new PortraitPreviewFrame(activity);
        previewLayer.setBackgroundColor(Color.rgb(28, 28, 28));
        viewport = new CameraViewportLayout(activity);
        root.addView(viewport, new LinearLayout.LayoutParams(-1, 0, 1));
        viewport.addView(previewLayer, new FrameLayout.LayoutParams(-1, -1));

        previewView = new PreviewView(activity);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        previewView.setContentDescription(activity.getString(R.string.ui_camera_preview));
        previewLayer.addView(previewView, matchFrame());

        watermarkView = new WatermarkOverlayView(activity);
        previewLayer.addView(watermarkView, matchFrame());
        addFocusIndicator(previewLayer);
        addPreviewControls(previewLayer);

        bottomPanel = createBottomPanel();
        viewport.addView(bottomPanel, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        viewport.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> updateViewportControls());
        previewLayer.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> updateViewportControls());
    }

    PreviewView previewView() {
        return previewView;
    }

    WatermarkOverlayView watermarkView() {
        return watermarkView;
    }

    void setLocationStatus(String text) {
        locationState.setContentDescription(activity.getString(R.string.time_status, text));
    }

    void setFlashEnabled(boolean enabled) {
        flashButton.setImageResource(enabled ? R.drawable.ic_flash : R.drawable.ic_flash_off);
        flashButton.setImageTintList(ColorStateList.valueOf(
                enabled ? CameraUi.BLUE : Color.WHITE));
        announce(enabled ? activity.getString(R.string.ui_flash_on) : activity.getString(R.string.ui_flash_off));
    }

    void setCaptureEnabled(boolean enabled) {
        shutter.setEnabled(enabled);
        shutter.setAlpha(enabled ? 1f : 0.55f);
    }

    boolean isVideoMode() {
        return videoMode;
    }

    void setRecording(boolean recording) {
        this.recording = recording;
        if (recordingTimer != null) {
            recordingTimer.setText("00:00");
            recordingTimer.setVisibility(recording ? View.VISIBLE : View.GONE);
        }
        shutter.setBackgroundResource(recording ? R.drawable.recording_stop : R.drawable.shutter_ring);
        shutter.setContentDescription(recording ? activity.getString(R.string.ui_stop_recording_video) : activity.getString(R.string.ui_start_recording_video));
        announce(recording ? activity.getString(R.string.ui_recording_video) : activity.getString(R.string.ui_video_recording_stopped));
    }

    private void setCaptureMode(boolean video) {
        if (recording) return;
        if (videoMode == video) return;
        videoMode = video;
        applyTabStyle(photoTab, !video);
        applyTabStyle(videoTab, video);
        shutter.setBackgroundResource(R.drawable.shutter_ring);
        shutter.setContentDescription(video ? activity.getString(R.string.ui_start_recording_video) : activity.getString(R.string.ui_take_photo));
        if (modeIndicator != null) {
            modeIndicator.animate().cancel();
            float destination = video ? modeSwitch.getWidth()/2f : 0f;
            if (android.animation.ValueAnimator.areAnimatorsEnabled()) {
                modeIndicator.animate().translationX(destination).setDuration(150)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
            } else modeIndicator.setTranslationX(destination);
        }
        actions.onModeChanged(video);
    }

    void setZoomRatio(float ratio) {
        if (!Float.isFinite(ratio)) return;
        boolean half = ratio < 0.99f;
        boolean two = Math.abs(ratio - 2f) < 0.03f;
        zoomOne.setText(String.format(java.util.Locale.US, "%.1fx", half || two ? 1f : ratio));
        applyZoomStyle(zoomHalf, half);
        applyZoomStyle(zoomOne, !half && !two);
        applyZoomStyle(zoomTwo, two);
    }

    /** Camera capability hook, called after binding or changing lenses. */
    void setZoomRange(float minimum, float maximum) {
        if (!Float.isFinite(minimum) || !Float.isFinite(maximum) || minimum > maximum) return;
        zoomHalf.setVisibility(minimum < 1f ? View.VISIBLE : View.GONE);
        zoomHalf.setText(String.format(java.util.Locale.US, "%.1fx", minimum));
        zoomHalf.setOnClickListener(v -> actions.onZoomSelected(minimum));
        zoomTwo.setVisibility(maximum >= 2f ? View.VISIBLE : View.GONE);
        updateZoomLayout();
    }

    void setExposureValue(float ev) {
        if (!Float.isFinite(ev)) {
            focusExposureControl.setEnabled(false);
            announce(activity.getString(R.string.ui_exposure_compensation_is_not_supported));
            return;
        }
        focusExposureControl.setEnabled(true);
        focusExposureControl.setContentDescription(activity.getString(R.string.ui_exposure_1f_ev, ev));
        announce(activity.getString(R.string.ui_exposure_1f, ev));
    }

    /** Replaces the library placeholder with the just-saved photo, without changing its action. */
    void setLatestPhotoBitmap(android.graphics.Bitmap bitmap) {
        if (bitmap == null || latestPhotoPreview == null) return;
        latestPhotoPreview.setImageTintList(null);
        latestPhotoPreview.setImageBitmap(bitmap);
        latestPhotoPreview.setContentDescription(activity.getString(R.string.ui_preview_latest_photo));
    }

    void setRecordingDuration(long durationNanos) {
        if (recording && recordingTimer != null) {
            recordingTimer.setText(RecordingTime.format(durationNanos));
        }
    }

    void setLatestVideo() {
        if (latestPhotoPreview == null) return;
        latestPhotoPreview.setImageResource(R.drawable.ic_gallery);
        latestPhotoPreview.setImageTintList(ColorStateList.valueOf(WatermarkRenderer.YELLOW));
        latestPhotoPreview.setContentDescription(activity.getString(R.string.ui_open_latest_video));
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

    void setAspectRatio(PhotoAspectRatio ratio) {
        selectedRatio = ratio;
        ((PortraitPreviewFrame) previewLayer).setAspectRatio(ratio);
        viewport.setAspectRatio(ratio);
        bottomPanel.setBackgroundColor(ratio == PhotoAspectRatio.TALL ? 0xe6ffffff : Color.WHITE);
        viewport.post(this::updateViewportControls);
        if (ratioButton != null) ratioButton.setText(ratio.label);
    }

    private View createTopBar() {
        LinearLayout bar = new LinearLayout(activity);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), dp(8), dp(12), 0);
        flashButton = previewIcon(R.drawable.ic_flash_off, activity.getString(R.string.ui_toggle_flash));
        flashButton.setOnClickListener(v -> actions.onToggleFlash());
        bar.addView(flashButton, fixed(dp(48), dp(48)));

        ImageButton editor = previewIcon(R.drawable.ic_edit, activity.getString(R.string.ui_edit_watermark));
        editor.setOnClickListener(v -> actions.onEditWatermark());
        addSpacedControl(bar, editor, 48);
        bar.addView(new View(activity), new LinearLayout.LayoutParams(0, 1, 1));

        ratioButton = textButton("3:4", activity.getString(R.string.ui_choose_photo_ratio), 14, Color.WHITE);
        ratioButton.setShadowLayer(dp(1), 0, dp(1), 0x99000000);
        ratioButton.setBackground(previewControlBackground());
        ratioButton.setOnClickListener(v -> actions.onSelectAspectRatio());
        addSpacedControl(bar, ratioButton, 48);
        ImageButton settings = previewIcon(R.drawable.ic_settings, activity.getString(R.string.ui_resolution_settings));
        settings.setOnClickListener(v -> actions.onMenu());
        addSpacedControl(bar, settings, 48);
        ImageButton flip = previewIcon(R.drawable.ic_switch_camera, activity.getString(R.string.ui_switch_front_and_rear_cameras));
        flip.setOnClickListener(v -> actions.onSwitchCamera());
        addSpacedControl(bar, flip, 48);
        return bar;
    }

    private void addSpacedControl(LinearLayout host, View control, int width) {
        LinearLayout.LayoutParams params = fixed(dp(width), dp(48));
        params.leftMargin = dp(8);
        host.addView(control, params);
    }

    private android.graphics.drawable.Drawable previewControlBackground() {
        return new InsetDrawable(CameraUi.shape(activity, 0x33666b73, 0, 24), dp(3));
    }

    private ImageButton previewIcon(int drawable, String description) {
        ImageButton button = imageButton(drawable, description, Color.WHITE);
        button.setBackground(previewControlBackground());
        UiMotion.press(button);
        return button;
    }

    private void addPreviewControls(FrameLayout preview) {
        LinearLayout zoom = new LinearLayout(activity);
        zoomControls = zoom;
        zoom.setOrientation(LinearLayout.VERTICAL);
        zoom.setBackground(CameraUi.shape(activity, 0x33666b73, 0, 24));
        zoom.setGravity(Gravity.CENTER);
        zoomTwo = zoomButton("2", 2f, activity.getString(R.string.ui_zoom_2_times));
        zoomOne = zoomButton("1.0x", 1f, activity.getString(R.string.ui_zoom_1_time));
        zoomHalf = zoomButton("0.5", 0.5f, activity.getString(R.string.ui_ultra_wide));
        applyZoomStyle(zoomOne, true);
        zoom.addView(zoomTwo, fixed(dp(48), dp(48)));
        zoom.addView(zoomOne, fixed(dp(48), dp(48)));
        zoom.addView(zoomHalf, fixed(dp(48), dp(48)));
        FrameLayout.LayoutParams zoomParams = new FrameLayout.LayoutParams(dp(48), dp(144),
                Gravity.END | Gravity.BOTTOM);
        zoomParams.rightMargin = dp(12);
        zoomParams.bottomMargin = dp(132);
        preview.addView(zoom, zoomParams);

        ImageButton edit = previewIcon(R.drawable.ic_clock, activity.getString(R.string.ui_adjust_watermark_date_and_time));
        clockControl = edit;
        edit.setOnClickListener(v -> actions.onEditTime());
        FrameLayout.LayoutParams editParams = new FrameLayout.LayoutParams(dp(48), dp(48),
                Gravity.END | Gravity.BOTTOM);
        editParams.rightMargin = dp(12);
        editParams.bottomMargin = dp(68);
        preview.addView(edit, editParams);

        ImageButton pin = previewIcon(R.drawable.ic_location_target, activity.getString(R.string.ui_change_watermark_location));
        locationControl = pin;
        pin.setOnClickListener(v -> actions.onRefreshLocation());
        FrameLayout.LayoutParams pinParams = new FrameLayout.LayoutParams(dp(48), dp(48),
                Gravity.END | Gravity.BOTTOM);
        pinParams.rightMargin = dp(12);
        pinParams.bottomMargin = dp(12);
        preview.addView(pin, pinParams);
        preview.addView(createTopBar(), new FrameLayout.LayoutParams(-1, dp(56), Gravity.TOP));
        recordingTimer = CameraUi.text(activity, "00:00", 15, Color.WHITE, true);
        recordingTimer.setGravity(Gravity.CENTER);
        recordingTimer.setPadding(dp(12), dp(6), dp(12), dp(6));
        recordingTimer.setBackground(CameraUi.shape(activity, 0xbb202124, 0, 18));
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL); dot.setColor(0xffff453a);
        dot.setBounds(0, 0, dp(8), dp(8));
        recordingTimer.setCompoundDrawables(dot, null, null, null);
        recordingTimer.setCompoundDrawablePadding(dp(8));
        recordingTimer.setContentDescription(activity.getString(R.string.ui_recording_duration));
        recordingTimer.setVisibility(View.GONE);
        FrameLayout.LayoutParams timerParams = new FrameLayout.LayoutParams(-2, dp(36), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        timerParams.topMargin = dp(64);
        preview.addView(recordingTimer, timerParams);
    }

    private void updateZoomLayout() {
        int count = 1 + (zoomHalf.getVisibility() == View.VISIBLE ? 1 : 0)
                + (zoomTwo.getVisibility() == View.VISIBLE ? 1 : 0);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) zoomControls.getLayoutParams();
        params.width = dp(48);
        params.height = dp(count * 48);
        params.bottomMargin = dp(132);
        zoomControls.setLayoutParams(params);
    }

    private void updateViewportControls() {
        if (bottomPanel == null || viewport == null) return;
        int overlap = Math.max(0, previewLayer.getBottom() - bottomPanel.getTop());
        for (View control : new View[]{zoomControls, clockControl, locationControl}) {
            if (control != null) control.setTranslationY(-overlap);
        }
        int[] origin = new int[2];
        bottomPanel.getLocationOnScreen(origin);
        watermarkView.setControlsBottomOnScreen(overlap > 0 ? origin[1] : Integer.MAX_VALUE);
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
        button.setShadowLayer(dp(1), 0, dp(1), 0x99000000);
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
        background.setColor(selected ? 0xdfffffff : Color.TRANSPARENT);
        button.setTextColor(selected ? CameraUi.INK : Color.WHITE);
        button.setShadowLayer(selected ? 0 : dp(1), 0, dp(1), 0x99000000);
        button.setBackground(new InsetDrawable(background, dp(6)));
    }

    private View createBottomPanel() {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(8), dp(16), dp(6));
        panel.setBackgroundColor(Color.WHITE);

        locationState = textButton(activity.getString(R.string.ui_adjust_time_alternate), activity.getString(R.string.ui_adjust_time_alternate), 15, CameraUi.BLUE);
        locationState.setTypeface(Typeface.DEFAULT_BOLD);
        locationState.setBackground(CameraUi.shape(activity, 0xffeaf1ff, 0, 12));
        locationState.setOnClickListener(v -> actions.onEditTime());
        panel.addView(locationState, new LinearLayout.LayoutParams(-1, dp(44)));

        modeSwitch = new FrameLayout(activity);
        modeSwitch.setBackground(CameraUi.shape(activity, CameraUi.SURFACE, 0, 24));
        modeSwitch.setClipToOutline(true);
        modeIndicator = new View(activity);
        modeIndicator.setBackground(CameraUi.shape(activity, CameraUi.BLUE, 0, 24));
        modeSwitch.addView(modeIndicator, new FrameLayout.LayoutParams(0, -1));
        LinearLayout tabs = new LinearLayout(activity);
        photoTab = textButton(activity.getString(R.string.ui_photo_alternate), activity.getString(R.string.ui_photo_mode), 15, CameraUi.BLUE);
        videoTab = textButton(activity.getString(R.string.ui_video), activity.getString(R.string.ui_video_mode), 15, CameraUi.INK);
        photoTab.setOnClickListener(v -> setCaptureMode(false));
        videoTab.setOnClickListener(v -> setCaptureMode(true));
        tabs.addView(photoTab, new LinearLayout.LayoutParams(0, -1, 1));
        tabs.addView(videoTab, new LinearLayout.LayoutParams(0, -1, 1));
        modeSwitch.addView(tabs, new FrameLayout.LayoutParams(-1, -1));
        modeSwitch.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (r-l == or-ol) return;
            ViewGroup.LayoutParams indicatorParams = modeIndicator.getLayoutParams();
            indicatorParams.width = (r-l)/2;
            modeIndicator.setLayoutParams(indicatorParams);
            modeIndicator.setTranslationX(videoMode ? (r-l)/2f : 0f);
        });
        LinearLayout.LayoutParams switchParams = fixed(dp(208), dp(44));
        switchParams.gravity = Gravity.CENTER_HORIZONTAL;
        switchParams.topMargin = dp(8);
        panel.addView(modeSwitch, switchParams);
        applyTabStyle(photoTab, true);
        applyTabStyle(videoTab, false);

        LinearLayout captureRow = new LinearLayout(activity);
        captureRow.setGravity(Gravity.CENTER);
        captureRow.setPadding(dp(12), dp(12), dp(12), dp(2));
        panel.addView(captureRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout gallery = new LinearLayout(activity);
        gallery.setOrientation(LinearLayout.VERTICAL);
        gallery.setGravity(Gravity.CENTER);
        gallery.setContentDescription(activity.getString(R.string.ui_open_latest_photo_in_gallery));
        gallery.setOnClickListener(v -> actions.onOpenGallery());
        latestPhotoPreview = new ImageView(activity);
        latestPhotoPreview.setImageResource(R.drawable.ic_gallery);
        latestPhotoPreview.setImageTintList(ColorStateList.valueOf(Color.rgb(82, 82, 82)));
        latestPhotoPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        latestPhotoPreview.setAdjustViewBounds(true);
        latestPhotoPreview.setOnClickListener(v -> actions.onOpenGallery());
        gallery.addView(latestPhotoPreview, fixed(dp(48), dp(42)));
        TextView galleryLabel = transparentLabel(activity.getString(R.string.ui_gallery), 15, Color.rgb(82, 82, 82));
        galleryLabel.setTypeface(Typeface.DEFAULT_BOLD);
        galleryLabel.setGravity(Gravity.CENTER);
        galleryLabel.setOnClickListener(v -> actions.onOpenGallery());
        gallery.addView(galleryLabel, fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(30)));
        captureRow.addView(gallery, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        shutter = new Button(activity);
        shutter.setText("");
        shutter.setStateListAnimator(null);
        shutter.setContentDescription(activity.getString(R.string.ui_take_photo));
        shutter.setBackgroundResource(R.drawable.shutter_ring);
        shutter.setOnClickListener(v -> actions.onCapture());
        UiMotion.press(shutter);
        LinearLayout.LayoutParams shutterParams = fixed(dp(80), dp(80));
        shutterParams.setMargins(dp(20), 0, dp(20), 0);
        captureRow.addView(shutter, shutterParams);

        TextView templates = transparentLabel(activity.getString(R.string.camera_templates), 15, Color.rgb(82, 82, 82));
        templates.setGravity(Gravity.CENTER);
        templates.setTypeface(Typeface.DEFAULT_BOLD);
        templates.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_template, 0, 0);
        templates.setCompoundDrawablePadding(dp(4));
        templates.setContentDescription(activity.getString(R.string.ui_open_watermark_templates));
        templates.setOnClickListener(v -> actions.onOpenTemplates());
        captureRow.addView(templates, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        return panel;
    }

    private void applyTabStyle(TextView tab, boolean selected) {
        if (tab == null) return;
        tab.setTextColor(selected ? Color.WHITE : CameraUi.INK);
        tab.setBackgroundColor(Color.TRANSPARENT);
        tab.setSelected(selected);
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
