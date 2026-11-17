package com.pgkit.timemarkcamera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import androidx.camera.view.PreviewView;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.Consumer;

/** Camera2 still-photo fallback for OEM auxiliary lenses omitted from CameraX enumeration. */
final class AuxiliaryCamera {
    interface Listener {
        void ready(float maximumZoom);
        void error(String message);
    }
    private final Activity activity;
    private final PreviewView preview;
    private final TextureView texture;
    private final HandlerThread thread = new HandlerThread("auxiliary-camera");
    private final Handler handler;
    private final Listener listener;
    private CameraDevice device;
    private CameraCaptureSession session;
    private ImageReader reader;
    private Surface surface;
    private CaptureRequest.Builder repeating;
    private CameraCharacteristics characteristics;
    private Size previewSize;
    private volatile int generation;
    private volatile boolean ready;
    private File pendingFile;
    private Consumer<File> onCaptured;

    AuxiliaryCamera(Activity activity, PreviewView preview, Listener listener) {
        this.activity = activity;
        this.preview = preview;
        this.listener = listener;
        thread.start();
        handler = new Handler(thread.getLooper());
        texture = new TextureView(activity);
        texture.setVisibility(View.GONE);
        texture.setOnTouchListener((view, event) -> preview.dispatchTouchEvent(event));
        ViewGroup host = (ViewGroup) preview.getParent();
        host.addView(texture, host.indexOfChild(preview) + 1, new ViewGroup.LayoutParams(-1, -1));
        texture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override public void onSurfaceTextureAvailable(SurfaceTexture t, int w, int h) {
                if (characteristics != null) configureTexture();
            }
            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture t, int w, int h) { transform(); }
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture t) { close(); return true; }
            @Override public void onSurfaceTextureUpdated(SurfaceTexture t) { }
        });
    }

    boolean isReady() { return ready; }
    android.graphics.Bitmap bitmap() { return texture.getBitmap(); }

    @SuppressLint("MissingPermission")
    void open(String id) {
        close();
        final int ticket = ++generation;
        try {
            CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            characteristics = manager.getCameraCharacteristics(id);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] jpeg = map.getOutputSizes(ImageFormat.JPEG);
            Size largest = Arrays.stream(jpeg).max(java.util.Comparator.comparingLong(s -> (long)s.getWidth()*s.getHeight())).get();
            previewSize = Arrays.stream(map.getOutputSizes(SurfaceTexture.class))
                    .filter(s -> s.getWidth() <= 1920)
                    .min(java.util.Comparator.comparingDouble(s -> Math.abs(s.getWidth()/(double)s.getHeight()-4.0/3.0)
                            + Math.abs(s.getWidth()-1280)/10000.0)).orElse(new Size(1280, 960));
            reader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, 2);
            reader.setOnImageAvailableListener(source -> {
                try (Image image = source.acquireNextImage()) {
                    if (image == null || pendingFile == null) return;
                    ByteBuffer bytes = image.getPlanes()[0].getBuffer();
                    byte[] data = new byte[bytes.remaining()];
                    bytes.get(data);
                    File file = pendingFile;
                    pendingFile = null;
                    try (FileOutputStream output = new FileOutputStream(file)) { output.write(data); }
                    Consumer<File> callback = onCaptured;
                    onCaptured = null;
                    if (callback != null) callback.accept(file);
                } catch (Exception error) { if (ticket == generation) fail(activity.getString(R.string.ui_could_not_save_wide_angle_photo) + error.getMessage()); }
            }, handler);
            preview.setVisibility(View.INVISIBLE);
            texture.setVisibility(View.VISIBLE);
            if (activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return;
            manager.openCamera(id, new CameraDevice.StateCallback() {
                @Override public void onOpened(CameraDevice camera) {
                    if (ticket != generation) { camera.close(); return; }
                    device = camera;
                    if (texture.isAvailable()) configureTexture();
                }
                @Override public void onDisconnected(CameraDevice camera) { camera.close(); if (ticket == generation) fail(activity.getString(R.string.ui_wide_angle_camera_disconnected)); }
                @Override public void onError(CameraDevice camera, int error) { camera.close(); if (ticket == generation) fail(activity.getString(R.string.ui_could_not_open_wide_angle_camera) + error + ")"); }
            }, new Handler(activity.getMainLooper()));
        } catch (Exception error) { fail(activity.getString(R.string.ui_could_not_open_wide_angle_camera_alternate) + error.getMessage()); }
    }

    private void configureTexture() {
        if (device == null || session != null || !texture.isAvailable()) return;
        try {
            texture.getSurfaceTexture().setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            surface = new Surface(texture.getSurfaceTexture());
            transform();
            repeating = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            repeating.addTarget(surface);
            repeating.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
            int[] modes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            int focusMode = CaptureRequest.CONTROL_AF_MODE_OFF;
            if (modes != null) for (int mode : modes) {
                if (mode == CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE) focusMode = mode;
            }
            repeating.set(CaptureRequest.CONTROL_AF_MODE, focusMode);
            int ticket = generation;
            device.createCaptureSession(Arrays.asList(surface, reader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override public void onConfigured(CameraCaptureSession configured) {
                    if (ticket != generation) { configured.close(); return; }
                    session = configured;
                    try {
                        session.setRepeatingRequest(repeating.build(), null, handler);
                        ready = true;
                        Float max = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
                        activity.runOnUiThread(() -> listener.ready(max == null ? 1f : max));
                    } catch (Exception error) { fail(error.getMessage()); }
                }
                @Override public void onConfigureFailed(CameraCaptureSession configured) { fail(activity.getString(R.string.ui_wide_angle_camera_does_not_support_this_photo)); }
            }, handler);
        } catch (Exception error) { fail(activity.getString(R.string.ui_could_not_create_wide_angle_preview) + error.getMessage()); }
    }

    private void transform() {
        // LG supplies the sensor-to-display transform with its SurfaceTexture buffers.
        // Applying the sensor orientation again rotates and stretches the preview twice.
        Matrix matrix = new Matrix();
        if (previewSize != null && texture.getWidth() > 0 && texture.getHeight() > 0) {
            float sourceRatio = previewSize.getHeight() / (float) previewSize.getWidth();
            float viewRatio = texture.getWidth() / (float) texture.getHeight();
            float x = sourceRatio > viewRatio ? sourceRatio / viewRatio : 1f;
            float y = viewRatio > sourceRatio ? viewRatio / sourceRatio : 1f;
            matrix.setScale(x, y, texture.getWidth()/2f, texture.getHeight()/2f);
        }
        texture.setTransform(matrix);
    }

    void zoom(float ratio) {
        if (!ready) return;
        Rect full = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        int width = Math.round(full.width() / ratio), height = Math.round(full.height() / ratio);
        repeating.set(CaptureRequest.SCALER_CROP_REGION, new Rect(full.centerX()-width/2,
                full.centerY()-height/2, full.centerX()+width/2, full.centerY()+height/2));
        repeat();
    }

    boolean torch(boolean enabled) {
        if (!ready || !Boolean.TRUE.equals(characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE))) return false;
        repeating.set(CaptureRequest.FLASH_MODE, enabled ? CaptureRequest.FLASH_MODE_TORCH : CaptureRequest.FLASH_MODE_OFF);
        repeat();
        return enabled;
    }

    void capture(File target, int rotation, Consumer<File> callback) {
        if (!ready || pendingFile != null) { fail(activity.getString(R.string.ui_wide_angle_camera_is_not_ready)); return; }
        try {
            pendingFile = target;
            onCaptured = callback;
            CaptureRequest.Builder request = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            request.addTarget(reader.getSurface());
            request.set(CaptureRequest.SCALER_CROP_REGION, repeating.get(CaptureRequest.SCALER_CROP_REGION));
            request.set(CaptureRequest.FLASH_MODE, repeating.get(CaptureRequest.FLASH_MODE));
            Integer sensor = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            request.set(CaptureRequest.JPEG_ORIENTATION, ((sensor == null ? 90 : sensor)
                    - Math.round(DeviceOrientation.watermarkDegrees(rotation)) + 360) % 360);
            request.set(CaptureRequest.JPEG_QUALITY, (byte) 100);
            session.capture(request.build(), new CameraCaptureSession.CaptureCallback() {
                @Override public void onCaptureFailed(CameraCaptureSession s, CaptureRequest r, CaptureFailure f) {
                    fail(activity.getString(R.string.ui_wide_angle_capture_failed) + f.getReason() + ")");
                }
            }, handler);
        } catch (Exception error) { fail(activity.getString(R.string.ui_could_not_capture_wide_angle_photo) + error.getMessage()); }
    }

    private void repeat() {
        try { session.setRepeatingRequest(repeating.build(), null, handler); }
        catch (Exception error) { fail(error.getMessage()); }
    }

    void close() {
        generation++;
        ready = false;
        if (session != null) { session.close(); session = null; }
        if (device != null) { device.close(); device = null; }
        if (reader != null) { reader.close(); reader = null; }
        if (surface != null) { surface.release(); surface = null; }
        if (pendingFile != null) { pendingFile.delete(); pendingFile = null; }
        onCaptured = null;
        texture.setVisibility(View.GONE);
        preview.setVisibility(View.VISIBLE);
    }

    void release() { close(); thread.quitSafely(); }

    private void fail(String message) {
        activity.runOnUiThread(() -> {
            close();
            listener.error(message == null ? activity.getString(R.string.ui_wide_angle_camera_error) : message);
        });
    }
}
