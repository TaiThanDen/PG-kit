package com.pgkit.timemarkcamera;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.camera.view.PreviewView;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends ComponentActivity implements LocationListener, CameraEngine.Callback {
    private static final int REQUEST_PERMISSIONS = 31;
    private static final String DEFAULT_ADDRESS = "Đang xác định vị trí…";

    private PreviewView previewView;
    private WatermarkOverlayView watermarkView;
    private CameraEngine cameraEngine;
    private LocationManager locationManager;
    private TextView locationState;
    private Button shutter;
    private Uri latestPhoto;
    private String verificationCode;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WatermarkRenderer.loadWatermarkFonts(this);
        configureSystemBars();
        verificationCode = obtainVerificationCode();
        buildInterface();
        cameraEngine = new CameraEngine(this, previewView, this, verificationCode);
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
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        } else {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }

    private void buildInterface() {
        boolean landscape = getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        setContentView(root);

        root.addView(createTopBar(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(landscape ? 52 : 68)));

        FrameLayout preview = new FrameLayout(this);
        preview.setBackgroundColor(Color.rgb(28, 28, 28));
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(preview, previewParams);

        previewView = new PreviewView(this);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        previewView.setContentDescription("Khung xem trước camera");
        preview.addView(previewView, matchFrame());

        watermarkView = new WatermarkOverlayView(this);
        preview.addView(watermarkView, matchFrame());
        addPreviewControls(preview);

        root.addView(landscape ? createLandscapeBottomPanel() : createBottomPanel(),
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(landscape ? 108 : 250)));
    }

    private View createTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10), dp(4), dp(10), dp(4));
        bar.setBackgroundColor(Color.WHITE);

        TextView menu = iconButton("☰", "Mở menu");
        menu.setTextSize(31);
        menu.setOnClickListener(v -> toast("Menu cài đặt"));
        bar.addView(menu, fixed(dp(54), dp(58)));

        TextView flash = iconButton("ϟ", "Bật hoặc tắt đèn flash");
        flash.setTextSize(34);
        flash.setOnClickListener(v -> {
            boolean enabled = cameraEngine != null && cameraEngine.toggleTorch();
            flash.setTextColor(enabled ? WatermarkRenderer.YELLOW : Color.rgb(30, 30, 30));
            announce(enabled ? "Đã bật flash" : "Đã tắt flash");
        });
        bar.addView(flash, fixed(dp(54), dp(58)));

        TextView verified = textButton("♢  Xác thực ảnh", "Xác thực ảnh", 15, Color.BLACK);
        verified.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        verified.setBackgroundResource(com.pgkit.timemarkcamera.R.drawable.pill_outline);
        LinearLayout.LayoutParams verifiedParams = new LinearLayout.LayoutParams(0, dp(42), 1f);
        verifiedParams.setMargins(dp(8), 0, dp(8), 0);
        bar.addView(verified, verifiedParams);

        TextView plus = textButton("✦ Plus", "Gói Plus", 16, Color.WHITE);
        plus.setTypeface(Typeface.DEFAULT_BOLD);
        plus.setBackgroundResource(com.pgkit.timemarkcamera.R.drawable.pill_dark);
        bar.addView(plus, fixed(dp(76), dp(42)));

        TextView switchCamera = iconButton("▣", "Đổi camera trước sau");
        switchCamera.setTextSize(28);
        switchCamera.setOnClickListener(v -> {
            if (cameraEngine != null) cameraEngine.switchCamera();
        });
        LinearLayout.LayoutParams switchParams = fixed(dp(54), dp(58));
        switchParams.setMargins(dp(6), 0, 0, 0);
        bar.addView(switchCamera, switchParams);
        return bar;
    }

    private void addPreviewControls(FrameLayout preview) {
        LinearLayout zoom = new LinearLayout(this);
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
                Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        zoomParams.rightMargin = dp(6);
        preview.addView(zoom, zoomParams);

        TextView edit = iconButton("□", "Chỉnh sửa watermark");
        edit.setTextColor(Color.WHITE);
        edit.setTextSize(33);
        edit.setBackgroundColor(Color.TRANSPARENT);
        edit.setOnClickListener(v -> toast("Mẫu watermark đang dùng: Timemark xác thực"));
        FrameLayout.LayoutParams editParams = new FrameLayout.LayoutParams(dp(58), dp(58),
                Gravity.RIGHT | Gravity.BOTTOM);
        editParams.rightMargin = dp(4);
        editParams.bottomMargin = dp(66);
        preview.addView(edit, editParams);

        TextView pin = iconButton("⌾", "Làm mới vị trí");
        pin.setTextColor(Color.WHITE);
        pin.setTextSize(38);
        pin.setBackgroundColor(Color.TRANSPARENT);
        pin.setOnClickListener(v -> startLocation());
        FrameLayout.LayoutParams pinParams = new FrameLayout.LayoutParams(dp(58), dp(58),
                Gravity.RIGHT | Gravity.BOTTOM);
        pinParams.rightMargin = dp(4);
        pinParams.bottomMargin = dp(6);
        preview.addView(pin, pinParams);
    }

    private View createBottomPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(8), dp(16), dp(6));
        panel.setBackgroundColor(Color.WHITE);

        locationState = textButton("Đang lấy thời gian và vị trí hệ thống…", "Trạng thái vị trí", 14,
                Color.rgb(23, 75, 131));
        locationState.setBackgroundResource(com.pgkit.timemarkcamera.R.drawable.login_background);
        locationState.setOnClickListener(v -> startLocation());
        panel.addView(locationState, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        LinearLayout captureRow = new LinearLayout(this);
        captureRow.setGravity(Gravity.CENTER);
        captureRow.setPadding(dp(12), dp(12), dp(12), dp(2));
        panel.addView(captureRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView gallery = transparentLabel("▣\nThư viện", 15, Color.rgb(82, 82, 82));
        gallery.setGravity(Gravity.CENTER);
        gallery.setTypeface(Typeface.DEFAULT_BOLD);
        gallery.setContentDescription("Mở ảnh vừa chụp trong thư viện");
        gallery.setOnClickListener(v -> openLatestPhoto());
        captureRow.addView(gallery, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        shutter = new Button(this);
        shutter.setText("");
        shutter.setContentDescription("Chụp ảnh");
        shutter.setBackgroundResource(com.pgkit.timemarkcamera.R.drawable.shutter_ring);
        shutter.setOnClickListener(v -> capture());
        LinearLayout.LayoutParams shutterParams = fixed(dp(76), dp(76));
        shutterParams.setMargins(dp(20), 0, dp(20), 0);
        captureRow.addView(shutter, shutterParams);

        TextView templates = transparentLabel("▤\nBản mẫu", 15, Color.rgb(82, 82, 82));
        templates.setGravity(Gravity.CENTER);
        templates.setTypeface(Typeface.DEFAULT_BOLD);
        templates.setContentDescription("Mở bản mẫu watermark");
        templates.setOnClickListener(v -> toast("Đang dùng bản mẫu giống ảnh tham chiếu"));
        captureRow.addView(templates, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        LinearLayout tabs = new LinearLayout(this);
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

    private View createLandscapeBottomPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setPadding(dp(16), dp(8), dp(16), dp(8));
        panel.setBackgroundColor(Color.WHITE);

        locationState = textButton("Đang lấy thời gian và vị trí hệ thống…",
                "Trạng thái vị trí", 14, Color.rgb(23, 75, 131));
        locationState.setBackgroundResource(com.pgkit.timemarkcamera.R.drawable.login_background);
        locationState.setOnClickListener(v -> startLocation());
        panel.addView(locationState, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        TextView gallery = transparentLabel("▣", 26, Color.rgb(82, 82, 82));
        gallery.setGravity(Gravity.CENTER);
        gallery.setContentDescription("Mở ảnh vừa chụp trong thư viện");
        gallery.setOnClickListener(v -> openLatestPhoto());
        LinearLayout.LayoutParams galleryParams = fixed(dp(52), dp(76));
        galleryParams.setMargins(dp(12), 0, dp(6), 0);
        panel.addView(gallery, galleryParams);

        shutter = new Button(this);
        shutter.setText("");
        shutter.setContentDescription("Chụp ảnh");
        shutter.setBackgroundResource(com.pgkit.timemarkcamera.R.drawable.shutter_ring);
        shutter.setOnClickListener(v -> capture());
        panel.addView(shutter, fixed(dp(76), dp(76)));

        TextView templates = transparentLabel("▤", 26, Color.rgb(82, 82, 82));
        templates.setGravity(Gravity.CENTER);
        templates.setContentDescription("Mở bản mẫu watermark");
        templates.setOnClickListener(v -> toast("Đang dùng bản mẫu Timemark"));
        LinearLayout.LayoutParams templateParams = fixed(dp(52), dp(76));
        templateParams.setMargins(dp(6), 0, 0, 0);
        panel.addView(templates, templateParams);
        return panel;
    }

    private void requestNeededPermissions() {
        List<String> missing = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.CAMERA);
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT <= 28 &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (missing.isEmpty()) {
            startFeatures();
        } else {
            requestPermissions(missing.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQUEST_PERMISSIONS) startFeatures();
    }

    private void startFeatures() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraEngine.start();
        } else {
            toast("Cần cấp quyền camera để chụp ảnh");
        }
        startLocation();
    }

    private void startLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationState.setText("Chạm để cấp quyền vị trí");
            requestNeededPermissions();
            return;
        }
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        }
        locationState.setText("Đang cập nhật vị trí hệ thống…");
        Location best = null;
        try {
            for (String provider : locationManager.getProviders(true)) {
                Location candidate = locationManager.getLastKnownLocation(provider);
                if (candidate != null && (best == null || candidate.getAccuracy() < best.getAccuracy())) {
                    best = candidate;
                }
                locationManager.requestLocationUpdates(provider, 1500L, 1f, this, Looper.getMainLooper());
            }
        } catch (SecurityException ignored) {
        }
        if (best != null) resolveAddress(best);
    }

    @Override public void onLocationChanged(Location location) {
        resolveAddress(location);
        if (locationManager != null) locationManager.removeUpdates(this);
    }

    private void resolveAddress(Location location) {
        String coordinates = String.format(Locale.US, "%.6f, %.6f",
                location.getLatitude(), location.getLongitude());
        watermarkView.setAddress(coordinates);
        locationState.setText("Vị trí hệ thống: " + coordinates);
        Geocoder geocoder = new Geocoder(this, new Locale("vi", "VN"));
        if (!Geocoder.isPresent()) return;
        if (Build.VERSION.SDK_INT >= 33) {
            geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1, addresses -> {
                if (!addresses.isEmpty()) applyAddress(addresses.get(0));
            });
        } else {
            new Thread(() -> {
                try {
                    List<Address> addresses = geocoder.getFromLocation(
                            location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        runOnUiThread(() -> applyAddress(addresses.get(0)));
                    }
                } catch (Exception ignored) {
                }
            }, "reverse-geocoder").start();
        }
    }

    private void applyAddress(Address address) {
        String display = address.getAddressLine(0);
        if (display == null || display.trim().isEmpty()) {
            List<String> pieces = new ArrayList<>();
            addIfPresent(pieces, address.getFeatureName());
            addIfPresent(pieces, address.getThoroughfare());
            addIfPresent(pieces, address.getSubLocality());
            addIfPresent(pieces, address.getLocality());
            addIfPresent(pieces, address.getAdminArea());
            display = android.text.TextUtils.join(", ", pieces);
        }
        if (display != null && !display.trim().isEmpty()) {
            watermarkView.setAddress(display);
            locationState.setText("Đã xác định vị trí • Chạm để làm mới");
        }
    }

    private static void addIfPresent(List<String> list, String value) {
        if (value != null && !value.trim().isEmpty() && !list.contains(value)) list.add(value);
    }

    private void capture() {
        if (cameraEngine == null || !cameraEngine.isReady()) {
            toast("Camera chưa sẵn sàng");
            return;
        }
        shutter.setEnabled(false);
        shutter.setAlpha(0.55f);
        cameraEngine.capture(watermarkView.getAddress());
    }

    private void openLatestPhoto() {
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

    @Override public void onCameraReady() {
        runOnUiThread(() -> locationState.setText(
                DEFAULT_ADDRESS.equals(watermarkView.getAddress())
                        ? "Camera sẵn sàng • Đang lấy vị trí…"
                        : "Camera và vị trí đã sẵn sàng"));
    }

    @Override public void onPhotoSaved(Uri uri) {
        runOnUiThread(() -> {
            latestPhoto = uri;
            shutter.setEnabled(true);
            shutter.setAlpha(1f);
            toast("Đã lưu ảnh có watermark vào Pictures/TimemarkCamera");
        });
    }

    @Override public void onCameraError(String message) {
        runOnUiThread(() -> {
            if (shutter != null) {
                shutter.setEnabled(true);
                shutter.setAlpha(1f);
            }
            toast(message);
        });
    }

    @Override protected void onResume() {
        super.onResume();
        if (cameraEngine != null && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraEngine.start();
        }
    }

    @Override protected void onPause() {
        if (locationManager != null) locationManager.removeUpdates(this);
        if (cameraEngine != null) cameraEngine.stop();
        super.onPause();
    }

    private String obtainVerificationCode() {
        String saved = getPreferences(MODE_PRIVATE).getString("verification_code", null);
        if (saved != null) return saved;
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder result = new StringBuilder(20);
        for (int i = 0; i < 20; i++) result.append(alphabet.charAt(random.nextInt(alphabet.length())));
        saved = result.toString();
        getPreferences(MODE_PRIVATE).edit().putString("verification_code", saved).apply();
        return saved;
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
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setFontFeatureSettings("tnum");
        view.setBackgroundColor(Color.TRANSPARENT);
        return view;
    }

    private void announce(String text) {
        getWindow().getDecorView().announceForAccessibility(text);
    }

    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_SHORT).show(); }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private static FrameLayout.LayoutParams matchFrame() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
    }
    private static LinearLayout.LayoutParams fixed(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }
}
