package com.pgkit.timemarkcamera;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Surface;
import android.view.OrientationEventListener;

import androidx.activity.ComponentActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CameraX owns preview rotation and crop. This deliberately avoids TextureView matrix math,
 * which previously left unfilled (black) preview regions on portrait devices.
 */
final class CameraEngine {
    interface Callback {
        void onCameraReady();
        void onCameraRotationChanged(int rotation);
        void onPhotoSaved(Uri uri);
        void onCameraError(String message);
    }

    private final ComponentActivity activity;
    private final PreviewView previewView;
    private final Callback callback;
    private final String verificationCode;
    private final Executor mainExecutor;
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();

    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private Camera camera;
    private Preview previewUseCase;
    private OrientationEventListener orientationListener;
    private int targetRotation = Surface.ROTATION_0;
    private int sensorRotation = Surface.ROTATION_0;
    private int desiredFacing = CameraSelector.LENS_FACING_BACK;
    private boolean ready;
    private boolean torchEnabled;
    private Date captureTime;
    private String captureAddress;

    CameraEngine(ComponentActivity activity, PreviewView previewView, Callback callback,
                 String verificationCode) {
        this.activity = activity;
        this.previewView = previewView;
        this.callback = callback;
        this.verificationCode = verificationCode;
        this.mainExecutor = ContextCompat.getMainExecutor(activity);
        this.orientationListener = new OrientationEventListener(activity) {
            @Override public void onOrientationChanged(int orientation) {
                if (orientation == ORIENTATION_UNKNOWN) return;
                int nextRotation = rotationForOrientation(orientation);
                if (nextRotation == sensorRotation) return;
                sensorRotation = nextRotation;
                targetRotation = sensorRotation;
                callback.onCameraRotationChanged(sensorRotation);
                // The activity intentionally stays portrait. Rebinding makes CameraX
                // renegotiate the camera stream for the new physical orientation,
                // rather than leaving a portrait stream merely turned sideways.
                if (cameraProvider != null) previewView.post(CameraEngine.this::bindUseCases);
            }
        };
    }

    void start() {
        if (orientationListener.canDetectOrientation()) orientationListener.enable();
        ListenableFuture<ProcessCameraProvider> providerFuture =
                ProcessCameraProvider.getInstance(activity);
        providerFuture.addListener(() -> {
            try {
                cameraProvider = providerFuture.get();
                previewView.post(this::bindUseCases);
            } catch (Exception e) {
                callback.onCameraError("Không khởi tạo được CameraX: " + e.getMessage());
            }
        }, mainExecutor);
    }

    void stop() {
        ready = false;
        orientationListener.disable();
        if (cameraProvider != null) cameraProvider.unbindAll();
    }

    boolean isReady() {
        return ready && imageCapture != null && camera != null;
    }

    boolean toggleTorch() {
        if (camera == null || !camera.getCameraInfo().hasFlashUnit()) return false;
        torchEnabled = !torchEnabled;
        camera.getCameraControl().enableTorch(torchEnabled);
        return torchEnabled;
    }

    void switchCamera() {
        desiredFacing = desiredFacing == CameraSelector.LENS_FACING_BACK
                ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
        torchEnabled = false;
        bindUseCases();
    }

    void capture(String address) {
        ImageCapture capture = imageCapture;
        if (!ready || capture == null) {
            callback.onCameraError("Camera chưa sẵn sàng");
            return;
        }
        captureTime = new Date();
        captureAddress = address;
        capture.setTargetRotation(targetRotation);
        final int requestedRotation = sensorRotation;
        try {
            File rawFile = File.createTempFile("timemark_raw_", ".jpg", activity.getCacheDir());
            ImageCapture.OutputFileOptions output = new ImageCapture.OutputFileOptions.Builder(rawFile).build();
            capture.takePicture(output, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
                @Override public void onImageSaved(ImageCapture.OutputFileResults result) {
                    renderAndSave(rawFile, requestedRotation);
                }

                @Override public void onError(ImageCaptureException exception) {
                    rawFile.delete();
                    callback.onCameraError("Không thể chụp ảnh: " + exception.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onCameraError("Không thể tạo ảnh tạm: " + e.getMessage());
        }
    }

    private void bindUseCases() {
        if (cameraProvider == null || previewView.getWidth() == 0 || previewView.getHeight() == 0) {
            previewView.postDelayed(this::bindUseCases, 80L);
            return;
        }
        ready = false;
        try {
            cameraProvider.unbindAll();
            targetRotation = sensorRotation;
            previewUseCase = new Preview.Builder()
                    .setTargetRotation(targetRotation)
                    .build();
            previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());

            imageCapture = new ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setTargetRotation(targetRotation)
                    .build();
            CameraSelector selector = new CameraSelector.Builder()
                    .requireLensFacing(desiredFacing)
                    .build();
            camera = cameraProvider.bindToLifecycle(activity, selector, previewUseCase, imageCapture);
            ready = true;
            callback.onCameraReady();
        } catch (Exception e) {
            ready = false;
            callback.onCameraError("Không mở được camera: " + e.getMessage());
        }
    }

    private int displayRotation() {
        return previewView.getDisplay() == null ? Surface.ROTATION_0 : previewView.getDisplay().getRotation();
    }

    private static int rotationForOrientation(int orientation) {
        // OrientationEventListener reports the physical tilt in the opposite
        // direction from the clockwise Surface rotation constants.
        if (orientation >= 45 && orientation < 135) return Surface.ROTATION_270;
        if (orientation >= 135 && orientation < 225) return Surface.ROTATION_180;
        if (orientation >= 225 && orientation < 315) return Surface.ROTATION_90;
        return Surface.ROTATION_0;
    }

    private void renderAndSave(File rawFile, int requestedRotation) {
        Bitmap decoded = null;
        Bitmap oriented = null;
        Bitmap mutable = null;
        try {
            decoded = BitmapFactory.decodeFile(rawFile.getAbsolutePath());
            if (decoded == null) throw new IllegalStateException("Dữ liệu ảnh không hợp lệ");
            oriented = applyExifRotation(decoded, rawFile);
            if (oriented != decoded) decoded.recycle();
            Bitmap landscape = ensureLandscape(oriented, requestedRotation);
            if (landscape != oriented) {
                oriented.recycle();
                oriented = landscape;
            }
            mutable = oriented.copy(Bitmap.Config.ARGB_8888, true);
            if (mutable == null) throw new IllegalStateException("Không tạo được bitmap để render");
            if (mutable != oriented) oriented.recycle();
            Date time = captureTime == null ? new Date() : captureTime;
            String address = captureAddress == null ? "Chưa xác định vị trí" : captureAddress;
            WatermarkRenderer.drawSaved(mutable, time, address, verificationCode);
            Uri saved = saveBitmap(mutable, time);
            callback.onPhotoSaved(saved);
        } catch (Exception e) {
            callback.onCameraError("Không xử lý được ảnh: " + e.getMessage());
        } finally {
            if (mutable != null && !mutable.isRecycled()) mutable.recycle();
            if (oriented != null && oriented != mutable && !oriented.isRecycled()) oriented.recycle();
            if (decoded != null && decoded != oriented && decoded != mutable && !decoded.isRecycled()) decoded.recycle();
            rawFile.delete();
        }
    }

    private static Bitmap applyExifRotation(Bitmap bitmap, File imageFile) throws Exception {
        int orientation = new ExifInterface(imageFile.getAbsolutePath()).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        float degrees;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90: degrees = 90f; break;
            case ExifInterface.ORIENTATION_ROTATE_180: degrees = 180f; break;
            case ExifInterface.ORIENTATION_ROTATE_270: degrees = 270f; break;
            default: return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private static Bitmap ensureLandscape(Bitmap bitmap, int rotation) {
        if ((rotation != Surface.ROTATION_90 && rotation != Surface.ROTATION_270)
                || bitmap.getWidth() >= bitmap.getHeight()) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation == Surface.ROTATION_90 ? 90f : -90f);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                matrix, true);
    }

    private Uri saveBitmap(Bitmap bitmap, Date when) throws Exception {
        String fileName = "TIMEMARK_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(when) + ".jpg";
        ContentResolver resolver = activity.getContentResolver();
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/TimemarkCamera");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IllegalStateException("Không tạo được tệp ảnh");
            try (OutputStream stream = resolver.openOutputStream(uri)) {
                if (stream == null || !bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    resolver.delete(uri, null, null);
                    throw new IllegalStateException("Không ghi được JPEG");
                }
            }
            ContentValues readyValues = new ContentValues();
            readyValues.put(MediaStore.Images.Media.IS_PENDING, 0);
            resolver.update(uri, readyValues, null, null);
            return uri;
        }
        File directory = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "TimemarkCamera");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Không tạo được thư mục ảnh");
        }
        File file = new File(directory, fileName);
        try (OutputStream stream = new FileOutputStream(file)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                throw new IllegalStateException("Không ghi được JPEG");
            }
        }
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        return uri == null ? Uri.fromFile(file) : uri;
    }
}
