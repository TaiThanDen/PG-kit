package com.pgkit.timemarkcamera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Looper;

import androidx.activity.ComponentActivity;

import java.util.List;
import java.util.Locale;

/** Owns location updates and reverse geocoding; it never changes camera or UI state directly. */
final class LocationController implements LocationListener {
    interface Callback {
        void onLocationStatus(String status);
        void onAddressChanged(String address);
        void onLocationFix(Location location);
    }

    private final ComponentActivity activity;
    private final Callback callback;
    private LocationManager locationManager;

    LocationController(ComponentActivity activity, Callback callback) {
        this.activity = activity;
        this.callback = callback;
    }

    /** @return true when location permission exists and an update was requested. */
    boolean start() {
        if (!hasPermission()) {
            callback.onLocationStatus("Chạm để cấp quyền vị trí");
            return false;
        }
        if (locationManager == null) {
            locationManager = (LocationManager) activity.getSystemService(
                    ComponentActivity.LOCATION_SERVICE);
        }
        callback.onLocationStatus("Đang cập nhật vị trí hệ thống…");
        Location best = null;
        try {
            for (String provider : locationManager.getProviders(true)) {
                Location candidate = locationManager.getLastKnownLocation(provider);
                if (candidate != null && (best == null || candidate.getAccuracy() < best.getAccuracy())) {
                    best = candidate;
                }
                locationManager.requestLocationUpdates(
                        provider, 1500L, 1f, this, Looper.getMainLooper());
            }
        } catch (SecurityException ignored) {
            callback.onLocationStatus("Không thể đọc vị trí hệ thống");
        }
        if (best != null) resolveAddress(best);
        return true;
    }

    void stop() {
        if (locationManager != null) locationManager.removeUpdates(this);
    }

    @Override public void onLocationChanged(Location location) {
        resolveAddress(location);
        stop();
    }

    private boolean hasPermission() {
        return activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void resolveAddress(Location location) {
        callback.onLocationFix(new Location(location));
        String coordinates = String.format(Locale.US, "%.6f, %.6f",
                location.getLatitude(), location.getLongitude());
        // GPS coordinates belong to the coordinate field, not the postal-address field.
        // Keep any previously resolved address while a new reverse-geocoding request runs.
        callback.onLocationStatus("Vị trí hệ thống: " + coordinates);

        Geocoder geocoder = new Geocoder(activity);
        if (!Geocoder.isPresent()) return;
        if (Build.VERSION.SDK_INT >= 33) {
            geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1,
                    addresses -> {
                        if (!addresses.isEmpty()) applyAddress(addresses.get(0));
                    });
        } else {
            new Thread(() -> {
                try {
                    List<Address> addresses = geocoder.getFromLocation(
                            location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        applyAddress(addresses.get(0));
                    }
                } catch (Exception ignored) {
                    // onLocationFix already supplied coordinates to the coordinate field.
                }
            }, "reverse-geocoder").start();
        }
    }

    private void applyAddress(Address address) {
        String display = LocationAddressFormatter.format(address);
        if (display == null || display.trim().isEmpty()) return;
        String resolved = display;
        activity.runOnUiThread(() -> {
            callback.onAddressChanged(resolved);
            callback.onLocationStatus("Đã xác định vị trí • Chạm để làm mới");
        });
    }

}
