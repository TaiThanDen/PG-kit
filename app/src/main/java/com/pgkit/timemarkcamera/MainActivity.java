package com.pgkit.timemarkcamera;

import android.Manifest;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.Toast;
import android.widget.EditText;


import androidx.activity.ComponentActivity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Coordinates permissions, lifecycle, camera, location and the portrait camera screen. */
public final class MainActivity extends ComponentActivity implements
        CameraEngine.Callback, CameraScreen.Actions, LocationController.Callback {
    private static final int REQUEST_PERMISSIONS = 31;

    private final java.util.concurrent.ExecutorService thumbnailWorker = java.util.concurrent.Executors.newSingleThreadExecutor();
    private int thumbnailRequest;
    private CameraScreen screen;
    private CameraEngine cameraEngine;
    private CameraGestureController gestureController;
    private LocationController locationController;
    private Uri latestPhoto;
    private String latestMediaMimeType = "image/jpeg";
    private WatermarkTemplate.Snapshot watermarkTemplate;
    private String manualAddress;
    private final WatermarkClock watermarkClock = new WatermarkClock();
    private PhotoAspectRatio photoRatio = PhotoAspectRatio.STANDARD;
    private int latestCameraRotation;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WatermarkRenderer.loadWatermarkFonts(this);
        configureSystemBars();

        watermarkClock.restore(getPreferences(MODE_PRIVATE).getBoolean("clock_custom", false),
                getPreferences(MODE_PRIVATE).getBoolean("clock_running", true),
                getPreferences(MODE_PRIVATE).getLong("clock_value", 0L));
        photoRatio = PhotoAspectRatio.read(getPreferences(MODE_PRIVATE).getString("photo_ratio", "STANDARD"));
        screen = new CameraScreen(this, this);
        screen.watermarkView().setClock(watermarkClock);
        screen.setAspectRatio(photoRatio);
        String storedTemplate = getPreferences(MODE_PRIVATE).getString("watermark_template", null);
        watermarkTemplate = WatermarkTemplate.fromJson(this, storedTemplate);
        screen.watermarkView().setTemplate(watermarkTemplate);
        locationController = new LocationController(this, this);
        cameraEngine = new CameraEngine(this, screen.previewView(), this);
        cameraEngine.setClock(watermarkClock);
        cameraEngine.setAspectRatio(photoRatio);
        gestureController = new CameraGestureController(this, screen.previewView(),
                new CameraGestureController.Listener() {
                    @Override public void onTapToFocus(float x, float y) {
                        if (cameraEngine.tapToFocus(x, y)) screen.showFocusIndicator(x, y);
                    }

                    @Override public void onPinchZoom(float scaleFactor) {
                        screen.setZoomRatio(cameraEngine.pinchZoom(scaleFactor));
                    }
                });
        requestNeededPermissions();
    }

    private void configureSystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(Color.WHITE);
        window.setNavigationBarColor(Color.WHITE);
        View decorView = window.getDecorView();
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = decorView.getWindowInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        } else {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }

    private void requestNeededPermissions() {
        List<String> missing = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.CAMERA);
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.RECORD_AUDIO);
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT <= 28
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        }
        if (Build.VERSION.SDK_INT <= 28 && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        if (missing.isEmpty()) {
            startFeatures();
        } else {
            requestPermissions(missing.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }

    @Override public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQUEST_PERMISSIONS) startFeatures();
    }

    private void startFeatures() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraEngine.start();
        } else {
            toast(getString(R.string.ui_camera_permission_is_required_to_take_photos));
        }
        locationController.start();
    }

    @Override public void onMenu() {
        int current = getSharedPreferences(PhotoOutputSize.PREFERENCES, MODE_PRIVATE)
                .getInt(PhotoOutputSize.LONG_EDGE_KEY, PhotoOutputSize.REFERENCE_LONG_EDGE);
        AppSheet.choices(this, getString(R.string.ui_photo_resolution), getString(R.string.ui_choose_the_long_edge_size_of_saved_photos),
                new String[]{"2048 px", "2560 px", getString(R.string.ui_native_camera)},
                new String[]{getString(R.string.ui_compact_and_easy_to_share), getString(R.string.ui_more_detail), getString(R.string.ui_keep_the_sensor_resolution)},
                current == PhotoOutputSize.CAMERA_ORIGINAL ? 2
                        : current == PhotoOutputSize.REFERENCE_LARGE_LONG_EDGE ? 1 : 0, which -> {
                    int longEdge = which == 0 ? PhotoOutputSize.REFERENCE_LONG_EDGE
                            : which == 1 ? PhotoOutputSize.REFERENCE_LARGE_LONG_EDGE : PhotoOutputSize.CAMERA_ORIGINAL;
                    getSharedPreferences(PhotoOutputSize.PREFERENCES, MODE_PRIVATE).edit()
                            .putInt(PhotoOutputSize.LONG_EDGE_KEY, longEdge).apply();
                });
    }

    @Override public void onEditTime() {
        if (cameraEngine != null && cameraEngine.isRecording()) {
            toast(getString(R.string.ui_stop_recording_before_adjusting_time));
            return;
        }
        new WatermarkTimeSheet(this, watermarkClock, () -> {
            getPreferences(MODE_PRIVATE).edit()
                    .putBoolean("clock_custom", watermarkClock.isCustom())
                    .putBoolean("clock_running", watermarkClock.isRunning())
                    .putLong("clock_value", watermarkClock.storedValue()).apply();
            screen.watermarkView().invalidate();
        }).show();
    }

    @Override public void onSelectAspectRatio() {
        if (cameraEngine != null && cameraEngine.isRecording()) {
            toast(getString(R.string.ui_stop_recording_before_changing_ratio));
            return;
        }
        AppSheet.choices(this, getString(R.string.ui_photo_ratio), getString(R.string.ui_preview_and_saved_photos_use_the_same_ratio),
                new String[]{"3:4", "1:1", "9:16"}, new String[]{getString(R.string.ui_standard), getString(R.string.ui_square), getString(R.string.ui_full_portrait)},
                photoRatio.ordinal(), index -> {
                    photoRatio = PhotoAspectRatio.values()[index];
                    getPreferences(MODE_PRIVATE).edit().putString("photo_ratio", photoRatio.name()).apply();
                    screen.setAspectRatio(photoRatio);
                    screen.previewView().post(() -> cameraEngine.setAspectRatio(photoRatio));
                });
    }

    @Override public void onZoomCapabilities(float minimum, float maximum, float current) {
        runOnUiThread(() -> {
            screen.setZoomRange(minimum, maximum);
            screen.setZoomRatio(current);
        });
    }

    @Override public void onToggleFlash() {
        screen.setFlashEnabled(cameraEngine != null && cameraEngine.toggleTorch());
    }

    @Override public void onSwitchCamera() {
        if (cameraEngine != null) {
            cameraEngine.switchCamera();
            screen.setFlashEnabled(false);
            screen.setZoomRatio(1f);
            screen.setExposureValue(0f);
        }
    }

    @Override public void onEditWatermark() {
        showWatermarkTemplates(true);
    }

    @Override public void onRefreshLocation() {
        AppSheet sheet = new AppSheet(this, getString(R.string.ui_watermark_location), getString(R.string.ui_shown_in_the_preview_and_saved_photos));
        EditText input = AppSheet.input(this, screen.watermarkView().getAddress(), getString(R.string.ui_enter_address), true);
        sheet.content.addView(input, new android.widget.LinearLayout.LayoutParams(-1, -2));
        android.widget.TextView system = CameraUi.action(this, getString(R.string.ui_use_system_location), false, () -> {
            manualAddress = null;
            screen.setLocationStatus(getString(R.string.ui_getting_system_location));
            if (!locationController.start()) requestNeededPermissions();
            sheet.dialog.dismiss();
        });
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(-1, CameraUi.dp(this, 48));
        params.topMargin = CameraUi.dp(this, 12);
        sheet.content.addView(system, params);
        sheet.actions(getString(R.string.ui_apply), () -> {
            String value = input.getText().toString().trim();
            if (value.isEmpty()) { input.setError(getString(R.string.ui_please_enter_a_location)); return; }
            manualAddress = value;
            screen.watermarkView().setAddress(value);
            screen.setLocationStatus(getString(R.string.ui_using_custom_location));
            sheet.dialog.dismiss();
        });
        sheet.show();
    }

    @Override public void onOpenGallery() {
        Intent intent = new Intent(this, MediaViewerActivity.class);
        if (latestPhoto != null) intent.setData(latestPhoto);
        startActivity(intent);
    }

    @Override public void onCapture() {
        if (cameraEngine == null || !cameraEngine.isReady()) {
            toast(getString(R.string.ui_camera_is_not_ready));
            return;
        }
        if (screen.isVideoMode()) {
            cameraEngine.toggleVideoRecording(screen.watermarkView().getAddress(),
                    watermarkTemplate);
        } else {
            screen.setCaptureEnabled(false);
            cameraEngine.capture(screen.watermarkView().getAddress(),
                    watermarkTemplate);
        }
    }

    @Override public void onOpenTemplates() {
        showWatermarkTemplates(false);
    }

    private void showWatermarkTemplates(boolean edit) {
        if (cameraEngine != null && cameraEngine.isRecording()) {
            toast(getString(R.string.ui_stop_recording_before_changing_templates));
            return;
        }
        WatermarkTemplateSheet sheet = new WatermarkTemplateSheet(this, watermarkTemplate,
                () -> screen.watermarkView().getAddress(), watermarkClock,
                bottom -> screen.watermarkView().setVisibleBottomOnScreen(bottom),
                () -> latestCameraRotation, updated -> {
                    watermarkTemplate = updated;
                    screen.watermarkView().setTemplate(updated);
                    getPreferences(MODE_PRIVATE).edit()
                            .putString("watermark_template", updated.toJson()).apply();
                }, cameraEngine.previewBitmap());
        sheet.show(edit);
    }

    @Override public void onModeChanged(boolean videoMode) {
        cameraEngine.setVideoMode(videoMode);

    }

    @Override public void onZoomSelected(float ratio) {
        if (cameraEngine != null) screen.setZoomRatio(cameraEngine.setZoomRatio(ratio));
    }

    @Override public void onExposureChanged(int direction) {
        if (cameraEngine == null) return;
        float ev = direction > 0
                ? cameraEngine.increaseExposureCompensation()
                : cameraEngine.decreaseExposureCompensation();
        screen.setExposureValue(ev);
    }

    @Override public void onLocationStatus(String status) {
        if (manualAddress != null) return;
        screen.setLocationStatus(status);
    }

    @Override public void onLocationFix(android.location.Location location) {
        watermarkTemplate = watermarkTemplate.withSensorData(location.getLatitude(),
                location.getLongitude(), location.hasAltitude() ? location.getAltitude() : null, null);
        screen.watermarkView().setTemplate(watermarkTemplate);
    }

    @Override public void onAddressChanged(String address) {
        if (manualAddress != null) return;
        screen.watermarkView().setAddress(address);
    }

    @Override public void onCameraReady() {
        runOnUiThread(() -> {
            screen.setFlashEnabled(false);
            screen.setLocationStatus(
                    WatermarkOverlayView.DEFAULT_ADDRESS.equals(screen.watermarkView().getAddress())
                            ? getString(R.string.ui_camera_ready_getting_location)
                            : getString(R.string.ui_camera_and_location_are_ready));
        });
    }

    @Override public void onCameraRotationChanged(int rotation) {
        latestCameraRotation = rotation;
        if (cameraEngine == null || !cameraEngine.isRecording()) {
            screen.watermarkView().setCameraRotation(rotation);
        }
    }

    @Override public void onPhotoSaved(Uri uri) {
        runOnUiThread(() -> {
            latestPhoto = uri;
            latestMediaMimeType = "image/jpeg";
            int ticket = ++thumbnailRequest;
            thumbnailWorker.execute(() -> {
                try {
                    android.graphics.Bitmap thumbnail = Build.VERSION.SDK_INT >= 29
                            ? getContentResolver().loadThumbnail(uri, new android.util.Size(128, 128), null)
                            : MediaLibrary.image(this, uri, 128);
                    runOnUiThread(() -> {
                        if (!isDestroyed() && ticket == thumbnailRequest) screen.setLatestPhotoBitmap(thumbnail);
                        else if (thumbnail != null) thumbnail.recycle();
                    });
                } catch (Exception ignored) { /* Keep the previous thumbnail; the photo is already saved. */ }
            });
            screen.setCaptureEnabled(true);

        });
    }

    @Override public void onVideoRecordingChanged(boolean recording) {
        runOnUiThread(() -> {
            screen.setRecording(recording);
            screen.watermarkView().setCameraRotation(recording
                    ? cameraEngine.recordingRotation() : latestCameraRotation);
        });
    }

    @Override public void onVideoDuration(long durationNanos) {
        runOnUiThread(() -> screen.setRecordingDuration(durationNanos));
    }

    @Override public void onVideoSaved(Uri uri) {
        runOnUiThread(() -> {
            latestPhoto = uri;
            latestMediaMimeType = "video/mp4";
            thumbnailRequest++;
            screen.setLatestVideo();

        });
    }

    @Override public void onCameraError(String message) {
        runOnUiThread(() -> {
            screen.setCaptureEnabled(true);
            toast(message);
        });
    }

    @Override protected void onResume() {
        super.onResume();
        if (cameraEngine != null
                && checkSelfPermission(Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            cameraEngine.start();
        }
    }

    @Override protected void onPause() {
        if (locationController != null) locationController.stop();
        if (cameraEngine != null) cameraEngine.stop();
        super.onPause();
    }

    @Override protected void onDestroy() {
        thumbnailRequest++;
        thumbnailWorker.shutdown();
        if (cameraEngine != null) cameraEngine.release();
        super.onDestroy();
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
