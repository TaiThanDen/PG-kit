package com.pgkit.timemarkcamera;

import android.Manifest;
import android.content.ActivityNotFoundException;
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

import androidx.activity.ComponentActivity;

import java.util.ArrayList;
import java.util.List;

/** Coordinates permissions, lifecycle, camera, location and the portrait camera screen. */
public final class MainActivity extends ComponentActivity implements
        CameraEngine.Callback, CameraScreen.Actions, LocationController.Callback {
    private static final int REQUEST_PERMISSIONS = 31;

    private CameraScreen screen;
    private CameraEngine cameraEngine;
    private LocationController locationController;
    private Uri latestPhoto;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WatermarkRenderer.loadWatermarkFonts(this);
        configureSystemBars();

        screen = new CameraScreen(this, this);
        locationController = new LocationController(this, this);
        cameraEngine = new CameraEngine(this, screen.previewView(), this,
                VerificationCodeStore.obtain(this));
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
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT <= 28
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
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
            toast("Cần cấp quyền camera để chụp ảnh");
        }
        locationController.start();
    }

    @Override public void onMenu() {
        toast("Menu cài đặt");
    }

    @Override public void onToggleFlash() {
        screen.setFlashEnabled(cameraEngine != null && cameraEngine.toggleTorch());
    }

    @Override public void onSwitchCamera() {
        if (cameraEngine != null) cameraEngine.switchCamera();
    }

    @Override public void onEditWatermark() {
        toast("Mẫu watermark đang dùng: Timemark xác thực");
    }

    @Override public void onRefreshLocation() {
        if (!locationController.start()) requestNeededPermissions();
    }

    @Override public void onOpenGallery() {
        if (latestPhoto == null) {
            toast("Chưa có ảnh nào được chụp");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(latestPhoto, "image/jpeg");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            toast("Ảnh đã lưu trong Pictures/TimemarkCamera");
        }
    }

    @Override public void onCapture() {
        if (cameraEngine == null || !cameraEngine.isReady()) {
            toast("Camera chưa sẵn sàng");
            return;
        }
        screen.setCaptureEnabled(false);
        cameraEngine.capture(screen.watermarkView().getAddress());
    }

    @Override public void onOpenTemplates() {
        toast("Đang dùng bản mẫu giống ảnh tham chiếu");
    }

    @Override public void onLocationStatus(String status) {
        screen.setLocationStatus(status);
    }

    @Override public void onAddressChanged(String address) {
        screen.watermarkView().setAddress(address);
    }

    @Override public void onCameraReady() {
        runOnUiThread(() -> screen.setLocationStatus(
                WatermarkOverlayView.DEFAULT_ADDRESS.equals(screen.watermarkView().getAddress())
                        ? "Camera sẵn sàng • Đang lấy vị trí…"
                        : "Camera và vị trí đã sẵn sàng"));
    }

    @Override public void onCameraRotationChanged(int rotation) {
        screen.watermarkView().setCameraRotation(rotation);
    }

    @Override public void onPhotoSaved(Uri uri) {
        runOnUiThread(() -> {
            latestPhoto = uri;
            screen.setCaptureEnabled(true);
            toast("Đã lưu ảnh có watermark vào Pictures/TimemarkCamera");
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

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
