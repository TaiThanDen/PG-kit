package com.pgkit.timemarkcamera;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Range;
import android.util.Rational;
import android.view.Surface;
import android.view.OrientationEventListener;

import androidx.activity.ComponentActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.ViewPort;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import android.hardware.camera2.CameraCharacteristics;
import android.util.SizeF;
import java.util.ArrayList;
import java.util.List;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExposureState;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ZoomState;
import androidx.camera.effects.Frame;
import androidx.camera.effects.OverlayEffect;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
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
        void onZoomCapabilities(float minimum, float maximum, float current);
        void onCameraRotationChanged(int rotation);
        void onPhotoSaved(Uri uri);
        void onVideoRecordingChanged(boolean recording);
        void onVideoDuration(long durationNanos);
        void onVideoSaved(Uri uri);
        void onCameraError(String message);
    }

    private final ComponentActivity activity;
    private final PreviewView previewView;
    private final Callback callback;
    private final Executor mainExecutor;
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private final HandlerThread effectThread = new HandlerThread("timemark-video-watermark");
    private final Handler effectHandler;

    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private VideoCapture<Recorder> videoCapture;
    private OverlayEffect videoWatermarkEffect;
    private volatile Recording activeRecording;
    private Camera camera;
    private WatermarkClock watermarkClock = new WatermarkClock();
    private PhotoAspectRatio photoRatio = PhotoAspectRatio.STANDARD;
    private String mainCameraId;
    private String wideCameraId;
    private float wideFactor = 1f;
    private boolean usingWide;
    private float requestedZoom = 1f;
    private AuxiliaryCamera auxiliaryCamera;
    private boolean wideNeedsFallback;
    private float auxiliaryMaxZoom = 1f;
    private boolean videoMode;

    android.graphics.Bitmap previewBitmap() {
        return usingWide && wideNeedsFallback && auxiliaryCamera != null
                ? auxiliaryCamera.bitmap() : previewView.getBitmap();
    }

    void setVideoMode(boolean video) {
        videoMode = video;
        if (video && usingWide && wideNeedsFallback) {
            usingWide = false;
            requestedZoom = 1f;
            bindUseCases();
            callback.onCameraError(activity.getString(R.string.ui_the_lg_auxiliary_camera_supports_photos_use_the));
        } else if (camera != null) publishZoom();
    }

    void release() {
        stop();
        cameraExecutor.shutdown();
        effectThread.quitSafely();
        if (auxiliaryCamera != null) auxiliaryCamera.release();
    }

    void setClock(WatermarkClock clock) { watermarkClock = clock; }

    void setAspectRatio(PhotoAspectRatio ratio) {
        if (activeRecording != null) return;
        photoRatio = ratio;
        if (cameraProvider != null) bindUseCases();
    }
    private OrientationEventListener orientationListener;
    private int targetRotation = Surface.ROTATION_0;
    private int sensorRotation = Surface.ROTATION_0;
    private int desiredFacing = CameraSelector.LENS_FACING_BACK;
    private boolean ready;
    private boolean torchEnabled;
    private volatile VideoWatermarkRenderer videoWatermarkRenderer;
    private volatile int recordingRotation = Surface.ROTATION_0;

    CameraEngine(ComponentActivity activity, PreviewView previewView, Callback callback) {
        this.activity = activity;
        this.previewView = previewView;
        this.callback = callback;
        this.mainExecutor = ContextCompat.getMainExecutor(activity);
        effectThread.start();
        effectHandler = new Handler(effectThread.getLooper());
        this.orientationListener = new OrientationEventListener(activity) {
            @Override public void onOrientationChanged(int orientation) {
                if (orientation == ORIENTATION_UNKNOWN) return;
                int nextRotation = DeviceOrientation.fromSensorDegrees(orientation, sensorRotation);
                if (nextRotation == sensorRotation) return;
                sensorRotation = nextRotation;
                targetRotation = sensorRotation;
                callback.onCameraRotationChanged(sensorRotation);
                // The UI is deliberately locked to portrait. CameraX keeps Preview
                // at the display rotation; rotating it here turns a portrait stream
                // sideways. Only the captured image follows the physical sensor.
                if (imageCapture != null) imageCapture.setTargetRotation(targetRotation);
                // A single MP4 has one orientation. Reconfiguring VideoCapture during a
                // recording mixes transformed frame buffers and leaves old watermark layers.
                if (videoCapture != null && activeRecording == null) {
                    videoCapture.setTargetRotation(targetRotation);
                }
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
                callback.onCameraError(activity.getString(R.string.ui_could_not_initialize_camerax) + e.getMessage());
            }
        }, mainExecutor);
    }

    void stop() {
        ready = false;
        if (auxiliaryCamera != null) auxiliaryCamera.close();
        stopRecordingIfActive();
        orientationListener.disable();
        if (cameraProvider != null) cameraProvider.unbindAll();
        closeVideoWatermarkEffect();
    }

    boolean isReady() {
        return ready && (usingWide && wideNeedsFallback ? auxiliaryCamera != null
                && auxiliaryCamera.isReady() : imageCapture != null && videoCapture != null && camera != null);
    }

    boolean toggleTorch() {
        if (usingWide && wideNeedsFallback && auxiliaryCamera != null) {
            torchEnabled = auxiliaryCamera.torch(!torchEnabled);
            return torchEnabled;
        }
        if (camera == null || !camera.getCameraInfo().hasFlashUnit()) return false;
        torchEnabled = !torchEnabled;
        camera.getCameraControl().enableTorch(torchEnabled);
        return torchEnabled;
    }

    float setZoomRatio(float requestedRatio) {
        if (!Float.isFinite(requestedRatio)) return Float.NaN;
        if (usingWide && wideNeedsFallback && auxiliaryCamera != null) {
            if (requestedRatio >= 1f) {
                usingWide = false;
                requestedZoom = requestedRatio;
                bindUseCases();
            } else {
                requestedZoom = Math.max(wideFactor, Math.min(wideFactor * auxiliaryMaxZoom, requestedRatio));
                auxiliaryCamera.zoom(requestedZoom / wideFactor);
            }
            return requestedZoom;
        }
        Camera activeCamera = camera;
        if (activeCamera == null || !Float.isFinite(requestedRatio)) return Float.NaN;
        ZoomState zoomState = activeCamera.getCameraInfo().getZoomState().getValue();
        if (zoomState == null) return Float.NaN;
        float factor = usingWide ? wideFactor : 1f;
        boolean needWide = requestedRatio < 1f && wideCameraId != null
                && !(videoMode && wideNeedsFallback)
                && desiredFacing == CameraSelector.LENS_FACING_BACK;
        if (needWide != usingWide) {
            if (activeRecording != null) {
                callback.onCameraError(activity.getString(R.string.ui_stop_recording_before_changing_lenses));
                return zoomState.getZoomRatio() * factor;
            }
            requestedZoom = requestedRatio;
            usingWide = needWide;
            bindUseCases();
            return requestedZoom;
        }
        float clampedRatio = Math.max(zoomState.getMinZoomRatio(),
                Math.min(zoomState.getMaxZoomRatio(), requestedRatio / factor));
        requestedZoom = clampedRatio * factor;
        activeCamera.getCameraControl().setZoomRatio(clampedRatio);
        return requestedZoom;
    }

    float pinchZoom(float scaleFactor) {
        if (!Float.isFinite(scaleFactor) || scaleFactor <= 0f) return Float.NaN;
        return setZoomRatio(requestedZoom * scaleFactor);
    }

    boolean tapToFocus(float x, float y) {
        if (usingWide && wideNeedsFallback) return false;
        Camera activeCamera = camera;
        if (activeCamera == null || !Float.isFinite(x) || !Float.isFinite(y)) return false;
        MeteringPoint point = previewView.getMeteringPointFactory().createPoint(x, y);
        FocusMeteringAction action = new FocusMeteringAction.Builder(point,
                FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE)
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build();
        activeCamera.getCameraControl().startFocusAndMetering(action);
        return true;
    }

    float increaseExposureCompensation() {
        return adjustExposureCompensation(1);
    }

    float decreaseExposureCompensation() {
        return adjustExposureCompensation(-1);
    }

    void switchCamera() {
        if (activeRecording != null) {
            callback.onCameraError(activity.getString(R.string.ui_stop_recording_before_switching_cameras));
            return;
        }
        desiredFacing = desiredFacing == CameraSelector.LENS_FACING_BACK
                ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
        torchEnabled = false;
        usingWide = false;
        requestedZoom = 1f;
        bindUseCases();
    }

    void capture(String address) {
        capture(address, null);
    }

    /** Freezes selected watermark time, location, orientation and template at the shutter press. */
    void capture(String address, WatermarkTemplate.Snapshot template) {
        ImageCapture capture = imageCapture;
        if (!isReady() || (!(usingWide && wideNeedsFallback) && capture == null)) {
            callback.onCameraError(activity.getString(R.string.ui_camera_is_not_ready));
            return;
        }
        final long captureStarted = android.os.SystemClock.elapsedRealtime();
        final Date requestedTime = new Date(watermarkClock.now());
        final PhotoAspectRatio requestedRatio = photoRatio;
        final String requestedVerificationCode = VerificationCodeStore.create();
        final String requestedAddress = address == null || address.trim().isEmpty()
                ? "" : address.trim();
        final int requestedRotation = sensorRotation;
        final int requestedLongEdge = activity.getSharedPreferences(PhotoOutputSize.PREFERENCES,
                        ComponentActivity.MODE_PRIVATE).getInt(PhotoOutputSize.LONG_EDGE_KEY,
                        PhotoOutputSize.REFERENCE_LONG_EDGE);
        if (capture != null) capture.setTargetRotation(requestedRotation);
        try {
            File rawFile = File.createTempFile("timemark_raw_", ".jpg", activity.getCacheDir());
            if (usingWide && wideNeedsFallback) {
                auxiliaryCamera.capture(rawFile, requestedRotation, file -> cameraExecutor.execute(() ->
                        renderAndSave(file, requestedTime, requestedAddress, requestedRotation,
                                template, requestedLongEdge, requestedVerificationCode, requestedRatio, captureStarted)));
                return;
            }
            ImageCapture.OutputFileOptions output = new ImageCapture.OutputFileOptions.Builder(rawFile).build();
            capture.takePicture(output, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
                @Override public void onImageSaved(ImageCapture.OutputFileResults result) {
                    renderAndSave(rawFile, requestedTime, requestedAddress, requestedRotation,
                            template, requestedLongEdge, requestedVerificationCode, requestedRatio, captureStarted);
                }

                @Override public void onError(ImageCaptureException exception) {
                    rawFile.delete();
                    callback.onCameraError(activity.getString(R.string.ui_could_not_take_photo) + exception.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onCameraError(activity.getString(R.string.ui_could_not_create_temporary_photo) + e.getMessage());
        }
    }

    boolean isRecording() {
        return activeRecording != null;
    }

    int recordingRotation() {
        return recordingRotation;
    }

    /** Freezes template/location/orientation at start; uses the shared watermark clock per frame. */
    void toggleVideoRecording(String address, WatermarkTemplate.Snapshot template) {
        if (activeRecording != null) {
            activeRecording.stop();
            return;
        }
        if (usingWide && wideNeedsFallback) {
            callback.onCameraError(activity.getString(R.string.ui_switch_to_1x_to_record_video_on_lg));
            return;
        }
        VideoCapture<Recorder> capture = videoCapture;
        if (!ready || capture == null) {
            callback.onCameraError(activity.getString(R.string.ui_camera_is_not_ready_to_record_video));
            return;
        }
        final Date requestedTime = new Date(watermarkClock.now());
        final String requestedAddress = address == null || address.trim().isEmpty()
                ? "Chưa xác định vị trí" : address.trim();
        videoWatermarkRenderer = new VideoWatermarkRenderer(activity, requestedAddress, template);
        recordingRotation = sensorRotation;
        capture.setTargetRotation(recordingRotation);

        String fileName = "TIMEMARK_VIDEO_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(requestedTime) + "_" + safeAddressSlug(requestedAddress) + ".mp4";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.DATE_TAKEN, requestedTime.getTime());
        if (Build.VERSION.SDK_INT >= 29) {
            values.put(MediaStore.Video.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_DCIM + "/Camera");
        }
        MediaStoreOutputOptions output = new MediaStoreOutputOptions.Builder(
                activity.getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(values)
                .build();
        PendingRecording pending = capture.getOutput().prepareRecording(activity, output);
        if (activity.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            pending = pending.withAudioEnabled();
        }
        try {
            activeRecording = pending.start(mainExecutor, this::handleVideoEvent);
        } catch (RuntimeException exception) {
            releaseVideoWatermark();
            callback.onVideoRecordingChanged(false);
            callback.onCameraError(activity.getString(R.string.ui_could_not_start_recording) + exception.getMessage());
        }
    }

    @androidx.annotation.OptIn(markerClass = androidx.camera.camera2.interop.ExperimentalCamera2Interop.class)
    private void bindUseCases() {
        if (cameraProvider == null || previewView.getWidth() == 0 || previewView.getHeight() == 0) {
            previewView.postDelayed(this::bindUseCases, 80L);
            return;
        }
        ready = false;
        torchEnabled = false;
        try {
            cameraProvider.unbindAll();
            if (auxiliaryCamera != null) auxiliaryCamera.close();
            closeVideoWatermarkEffect();
            discoverRearLenses();
            if (usingWide && wideNeedsFallback) {
                if (auxiliaryCamera == null) auxiliaryCamera = new AuxiliaryCamera(activity, previewView, new AuxiliaryCamera.Listener() {
                    @Override public void ready(float maximumZoom) {
                        auxiliaryMaxZoom = maximumZoom;
                        ready = true;
                        auxiliaryCamera.zoom(Math.max(1f, requestedZoom / wideFactor));
                        callback.onZoomCapabilities(wideFactor, Math.max(2f, maximumZoom * wideFactor), requestedZoom);
                        callback.onCameraReady();
                    }
                    @Override public void error(String message) {
                        usingWide = false;
                        requestedZoom = 1f;
                        callback.onCameraError(message);
                        bindUseCases();
                    }
                });
                camera = null;
                imageCapture = null;
                auxiliaryCamera.open(wideCameraId);
                return;
            }
            targetRotation = sensorRotation;
            Preview.Builder previewBuilder = new Preview.Builder()
                    .setTargetRotation(displayRotation());
            TimemarkDefaultFilter.apply(previewBuilder);
            Preview previewUseCase = previewBuilder.build();
            previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());

            ImageCapture.Builder imageBuilder = new ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(targetRotation);
            TimemarkDefaultFilter.apply(imageBuilder);
            imageCapture = imageBuilder.build();
            Recorder recorder = new Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HD,
                            FallbackStrategy.lowerQualityOrHigherThan(Quality.HD)))
                    .build();
            videoCapture = new VideoCapture.Builder<>(recorder)
                    .setTargetFrameRate(new Range<>(24, 24))
                    .build();
            videoCapture.setTargetRotation(targetRotation);
            UseCaseGroup group = new UseCaseGroup.Builder()
                    .setViewPort(new ViewPort.Builder(new Rational(photoRatio.width, photoRatio.height),
                            displayRotation()).setScaleType(ViewPort.FILL_CENTER).build())
                    .addUseCase(previewUseCase)
                    .addUseCase(imageCapture)
                    .addUseCase(videoCapture)
                    .addEffect(createVideoWatermarkEffect())
                    .build();
            String selectedId = desiredFacing == CameraSelector.LENS_FACING_BACK
                    ? (usingWide && wideCameraId != null ? wideCameraId : mainCameraId) : null;
            CameraSelector.Builder selectorBuilder = new CameraSelector.Builder().requireLensFacing(desiredFacing);
            if (selectedId != null) selectorBuilder.addCameraFilter(infos -> {
                List<CameraInfo> matches = new ArrayList<>();
                for (CameraInfo info : infos) {
                    if (selectedId.equals(Camera2CameraInfo.from(info).getCameraId())) matches.add(info);
                }
                return matches;
            });
            CameraSelector selector = selectorBuilder.build();
            camera = cameraProvider.bindToLifecycle(activity, selector, group);
            ready = true;
            ZoomState zoom = camera.getCameraInfo().getZoomState().getValue();
            if (zoom != null) {
                float factor = usingWide ? wideFactor : 1f;
                float actual = Math.max(zoom.getMinZoomRatio(), Math.min(zoom.getMaxZoomRatio(), requestedZoom / factor));
                camera.getCameraControl().setZoomRatio(actual);
                requestedZoom = actual * factor;
            }
            publishZoom();
            torchEnabled = false;
            callback.onCameraReady();
        } catch (Exception e) {
            ready = false;
            closeVideoWatermarkEffect();
            callback.onCameraError(activity.getString(R.string.ui_could_not_open_camera) + e.getMessage());
        }
    }

    /** Compare horizontal field of view, rather than assuming vendor camera IDs or focal lengths. */
    @android.annotation.SuppressLint("UnsafeOptInUsageError")
    private void discoverRearLenses() {
        List<CameraInfo> rear = CameraSelector.DEFAULT_BACK_CAMERA.filter(cameraProvider.getAvailableCameraInfos());
        if (rear.isEmpty()) return;
        mainCameraId = Camera2CameraInfo.from(rear.get(0)).getCameraId();
        float mainFocal = normalizedFocal(rear.get(0));
        wideCameraId = null;
        wideFactor = 1f;
        wideNeedsFallback = false;
        for (CameraInfo info : rear) {
            float normalized = normalizedFocal(info);
            float relative = normalized / mainFocal;
            if (Float.isFinite(relative) && relative > 0f && relative < wideFactor * .85f) {
                wideFactor = relative;
                wideCameraId = Camera2CameraInfo.from(info).getCameraId();
            }
        }
        // Some LG ROMs omit usable auxiliary IDs from getCameraIdList(). Probe the reference LG's
        // numeric auxiliary slots only on LG, then validate rear facing, streams and actual FOV.
        if (wideCameraId == null && "lge".equalsIgnoreCase(Build.MANUFACTURER)) {
            android.hardware.camera2.CameraManager manager = (android.hardware.camera2.CameraManager)
                    activity.getSystemService(android.content.Context.CAMERA_SERVICE);
            for (int index = 2; index <= 8; index++) {
                try {
                    String id = String.valueOf(index);
                    CameraCharacteristics c = manager.getCameraCharacteristics(id);
                    Integer facing = c.get(CameraCharacteristics.LENS_FACING);
                    float[] focal = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                    SizeF sensor = c.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                    android.hardware.camera2.params.StreamConfigurationMap streams = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (facing == null || facing != CameraCharacteristics.LENS_FACING_BACK || focal == null
                            || focal.length == 0 || sensor == null || streams == null
                            || streams.getOutputSizes(android.graphics.ImageFormat.JPEG) == null) continue;
                    float relative = (focal[0] / sensor.getWidth()) / mainFocal;
                    if (Float.isFinite(relative) && relative > 0f && relative < wideFactor * .85f) {
                        wideFactor = relative;
                        wideCameraId = id;
                        wideNeedsFallback = true;
                    }
                } catch (Exception ignored) { /* Unsupported OEM slot: keep the public cameras. */ }
            }
        }

    }

    private void publishZoom() {
        if (camera == null) return;
        ZoomState zoom = camera.getCameraInfo().getZoomState().getValue();
        if (zoom == null) return;
        boolean wideAvailable = desiredFacing == CameraSelector.LENS_FACING_BACK && wideCameraId != null
                && !(videoMode && wideNeedsFallback);
        callback.onZoomCapabilities(wideAvailable ? wideFactor : zoom.getMinZoomRatio(),
                zoom.getMaxZoomRatio() * (usingWide ? wideFactor : 1f), requestedZoom);
    }

    @android.annotation.SuppressLint("UnsafeOptInUsageError")
    private float normalizedFocal(CameraInfo info) {
        Camera2CameraInfo details = Camera2CameraInfo.from(info);
        float[] focal = details.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        SizeF sensor = details.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
        if (focal == null || focal.length == 0 || sensor == null || sensor.getWidth() <= 0) return Float.NaN;
        return focal[0] / sensor.getWidth();
    }

    private OverlayEffect createVideoWatermarkEffect() {
        OverlayEffect effect = new OverlayEffect(CameraEffect.VIDEO_CAPTURE, 0, effectHandler,
                throwable -> callback.onCameraError(activity.getString(R.string.ui_could_not_render_video_watermark)
                        + throwable.getMessage()));
        videoWatermarkEffect = effect;
        effect.setOnDrawListener(frame -> {
            // OverlayEffect reuses its SurfaceTexture. Without clearing it, the alpha from
            // every previous frame is composited again, producing a thick/dotted outline and,
            // after device rotation, an apparent second watermark at the old orientation.
            Canvas overlayCanvas = frame.getOverlayCanvas();
            overlayCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            VideoWatermarkRenderer renderer = videoWatermarkRenderer;
            if (renderer != null) {
                drawVideoWatermark(frame, overlayCanvas, renderer);
            }
            return true;
        });
        return effect;
    }

    private void drawVideoWatermark(Frame frame, Canvas canvas, VideoWatermarkRenderer renderer) {
        android.graphics.Rect crop = frame.getCropRect();
        int sourceWidth = crop.width();
        int sourceHeight = crop.height();
        int rotation = frame.getRotationDegrees();
        int checkpoint = canvas.save();
        canvas.clipRect(crop);
        canvas.translate(crop.left, crop.top);
        int outputWidth = sourceWidth;
        int outputHeight = sourceHeight;
        // The effect canvas is in sensor-buffer coordinates. Convert it back to the final
        // upright MP4 coordinate system before placing the live-layout video bitmap.
        if (rotation == 90) {
            canvas.translate(0f, sourceHeight);
            canvas.rotate(-90f);
            outputWidth = sourceHeight;
            outputHeight = sourceWidth;
        } else if (rotation == 180) {
            canvas.translate(sourceWidth, sourceHeight);
            canvas.rotate(180f);
        } else if (rotation == 270) {
            canvas.translate(sourceWidth, 0f);
            canvas.rotate(90f);
            outputWidth = sourceHeight;
            outputHeight = sourceWidth;
        }
        if (frame.isMirroring()) {
            canvas.translate(outputWidth, 0);
            canvas.scale(-1f, 1f);
        }
        renderer.draw(canvas, outputWidth, outputHeight, watermarkClock.now());
        canvas.restoreToCount(checkpoint);
    }

    private void handleVideoEvent(VideoRecordEvent event) {
        if (event instanceof VideoRecordEvent.Start) {
            callback.onVideoRecordingChanged(true);
            return;
        }
        if (event instanceof VideoRecordEvent.Status && activeRecording != null) {
            callback.onVideoDuration(event.getRecordingStats().getRecordedDurationNanos());
        }
        if (!(event instanceof VideoRecordEvent.Finalize)) return;
        VideoRecordEvent.Finalize finalized = (VideoRecordEvent.Finalize) event;
        activeRecording = null;
        releaseVideoWatermark();
        if (videoCapture != null) videoCapture.setTargetRotation(sensorRotation);
        callback.onVideoRecordingChanged(false);
        if (finalized.hasError()) {
            callback.onCameraError(activity.getString(R.string.ui_could_not_save_video) + finalized.getError());
            return;
        }
        Uri uri = finalized.getOutputResults().getOutputUri();
        if (uri == null || Uri.EMPTY.equals(uri)) {
            callback.onCameraError(activity.getString(R.string.ui_video_was_recorded_but_no_output_file_was));
            return;
        }
        callback.onVideoSaved(uri);
    }

    private void stopRecordingIfActive() {
        Recording recording = activeRecording;
        if (recording != null) recording.stop();
    }

    private void releaseVideoWatermark() {
        VideoWatermarkRenderer renderer = videoWatermarkRenderer;
        videoWatermarkRenderer = null;
        // Draw callbacks and bitmap recycling share one thread, including a frame in flight.
        if (renderer != null) effectHandler.post(renderer::release);
    }

    private void closeVideoWatermarkEffect() {
        if (videoWatermarkEffect != null) {
            videoWatermarkEffect.close();
            videoWatermarkEffect = null;
        }
    }

    private int displayRotation() {
        return previewView.getDisplay() == null ? Surface.ROTATION_0 : previewView.getDisplay().getRotation();
    }

    private float adjustExposureCompensation(int delta) {
        Camera activeCamera = camera;
        if (activeCamera == null) return Float.NaN;
        ExposureState exposureState = activeCamera.getCameraInfo().getExposureState();
        if (!exposureState.isExposureCompensationSupported()) return Float.NaN;
        int requestedIndex = exposureState.getExposureCompensationIndex() + delta;
        return setExposureCompensationIndex(exposureState, requestedIndex);
    }

    private float setExposureCompensationIndex(ExposureState exposureState, int requestedIndex) {
        Range<Integer> range = exposureState.getExposureCompensationRange();
        int clampedIndex = Math.max(range.getLower(), Math.min(range.getUpper(), requestedIndex));
        Camera activeCamera = camera;
        if (activeCamera == null) return Float.NaN;
        activeCamera.getCameraControl().setExposureCompensationIndex(clampedIndex);
        Rational step = exposureState.getExposureCompensationStep();
        return clampedIndex * step.floatValue();
    }

    private void renderAndSave(File rawFile, Date requestedTime, String requestedAddress,
                               int requestedRotation, WatermarkTemplate.Snapshot template,
                               int requestedLongEdge, String requestedVerificationCode, PhotoAspectRatio ratio, long captureStarted) {
        long rawReady = android.os.SystemClock.elapsedRealtime();
        Bitmap decoded = null;
        Bitmap oriented = null;
        Bitmap mutable = null;
        try {
            decoded = BitmapFactory.decodeFile(rawFile.getAbsolutePath());
            if (decoded == null) throw new IllegalStateException(activity.getString(R.string.ui_invalid_image_data));
            oriented = applyExifRotation(decoded, rawFile);
            if (oriented != decoded) decoded.recycle();
            Bitmap landscape = ensureLandscape(oriented, requestedRotation);
            if (landscape != oriented) {
                oriented.recycle();
                oriented = landscape;
            }
            double aspect = DeviceOrientation.isLandscape(requestedRotation)
                    ? ratio.height / (double) ratio.width : ratio.width / (double) ratio.height;
            int cropWidth = oriented.getWidth(), cropHeight = oriented.getHeight();
            if (cropWidth / (double) cropHeight > aspect) cropWidth = (int) Math.round(cropHeight * aspect);
            else cropHeight = (int) Math.round(cropWidth / aspect);
            int[] outputSize = PhotoOutputSize.fit(cropWidth, cropHeight, requestedLongEdge);
            // Crop and resize into the final mutable target in one pass, avoiding several
            // full-resolution intermediate bitmaps before applying the watermark.
            mutable = Bitmap.createBitmap(outputSize[0], outputSize[1], Bitmap.Config.ARGB_8888);
            int left = (oriented.getWidth()-cropWidth)/2, top = (oriented.getHeight()-cropHeight)/2;
            new Canvas(mutable).drawBitmap(oriented,
                    new android.graphics.Rect(left, top, left+cropWidth, top+cropHeight),
                    new android.graphics.Rect(0, 0, outputSize[0], outputSize[1]),
                    new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG));
            oriented.recycle();
            WatermarkRenderer.drawSaved(new Canvas(mutable), mutable.getWidth(), mutable.getHeight(),
                    requestedTime, requestedAddress, requestedVerificationCode, template);
            long rendered = android.os.SystemClock.elapsedRealtime();
            Uri saved = saveBitmap(mutable, requestedTime, requestedAddress, rawFile);
            android.util.Log.i("PgCaptureTiming", "raw_ms=" + (rawReady-captureStarted)
                    + " render_ms=" + (rendered-rawReady) + " save_ms="
                    + (android.os.SystemClock.elapsedRealtime()-rendered) + " total_ms="
                    + (android.os.SystemClock.elapsedRealtime()-captureStarted));
            callback.onPhotoSaved(saved);
        } catch (Exception e) {
            callback.onCameraError(activity.getString(R.string.ui_could_not_process_photo) + e.getMessage());
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
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180f);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180f);
                matrix.postScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90f);
                matrix.postScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90f);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90f);
                matrix.postScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90f);
                break;
            default: return bitmap;
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private static Bitmap ensureLandscape(Bitmap bitmap, int rotation) {
        if (!DeviceOrientation.isLandscape(rotation) || bitmap.getWidth() >= bitmap.getHeight()) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation == Surface.ROTATION_90 ? 90f : -90f);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                matrix, true);
    }

    private Uri saveBitmap(Bitmap bitmap, Date when, String address, File rawFile) throws Exception {
        String fileName = "TIMEMARK_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(when) + "_" + safeAddressSlug(address) + ".jpg";
        ContentResolver resolver = activity.getContentResolver();
        int quality = PhotoSaveMetadata.jpegQuality(bitmap.getWidth(), bitmap.getHeight());
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/TimemarkCamera");
            values.put(MediaStore.Images.Media.DATE_TAKEN, when.getTime());
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IllegalStateException(activity.getString(R.string.ui_could_not_create_photo_file));
            try {
                try (OutputStream stream = resolver.openOutputStream(uri)) {
                    if (stream == null || !bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)) {
                        throw new IllegalStateException(activity.getString(R.string.ui_could_not_write_jpeg));
                    }
                }
                try (ParcelFileDescriptor descriptor = resolver.openFileDescriptor(uri, "rw")) {
                    if (descriptor == null) throw new IllegalStateException(activity.getString(R.string.ui_could_not_open_exif));
                    PhotoSaveMetadata.write(rawFile, new ExifInterface(descriptor.getFileDescriptor()),
                            when, bitmap.getWidth(), bitmap.getHeight());
                }
                ContentValues readyValues = new ContentValues();
                readyValues.put(MediaStore.Images.Media.IS_PENDING, 0);
                resolver.update(uri, readyValues, null, null);
                return uri;
            } catch (Exception error) {
                resolver.delete(uri, null, null);
                throw error;
            }
        }
        File directory = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "TimemarkCamera");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException(activity.getString(R.string.ui_could_not_create_photo_folder));
        }
        File file = new File(directory, fileName);
        try {
            try (OutputStream stream = new FileOutputStream(file)) {
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)) {
                    throw new IllegalStateException(activity.getString(R.string.ui_could_not_write_jpeg));
                }
            }
            PhotoSaveMetadata.write(rawFile, new ExifInterface(file.getAbsolutePath()),
                    when, bitmap.getWidth(), bitmap.getHeight());
        } catch (Exception error) {
            file.delete();
            throw error;
        }
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.DATE_TAKEN, when.getTime());
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        return uri == null ? Uri.fromFile(file) : uri;
    }

    private static String safeAddressSlug(String address) {
        String normalized = Normalizer.normalize(address == null ? "" : address,
                Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        String slug = normalized.toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (slug.isEmpty()) return "unknown-location";
        return slug.substring(0, Math.min(slug.length(), 32));
    }
}
