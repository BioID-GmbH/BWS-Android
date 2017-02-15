package com.bioid.authenticator.base.camera;

import android.Manifest;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.annotation.VisibleForTesting;
import android.util.Size;
import android.view.Surface;

import com.bioid.authenticator.base.annotations.ConfigurationOrientation;
import com.bioid.authenticator.base.annotations.Rotation;
import com.bioid.authenticator.base.annotations.SurfaceRotation;
import com.bioid.authenticator.base.functional.Consumer;
import com.bioid.authenticator.base.logging.LoggingHelper;
import com.bioid.authenticator.base.logging.LoggingHelperFactory;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Wrapper around the Camera2 API to expose a higher level interface.
 * It is the callers responsibility to ensure that the permission to access the camera is granted.
 */
@SuppressWarnings("MissingPermission")
final public class CameraHelper {

    private final LoggingHelper log;
    private final CameraManager manager;
    private final CameraCharacteristicsHelper cameraCharacteristicsHelper;

    public CameraHelper(CameraManager manager) {
        this.log = LoggingHelperFactory.create(CameraHelper.class);
        this.manager = manager;
        this.cameraCharacteristicsHelper = new CameraCharacteristicsHelper(manager);
    }

    @VisibleForTesting
    CameraHelper(LoggingHelper log, CameraManager manager, CameraCharacteristicsHelper cameraCharacteristicsHelper) {
        this.log = log;
        this.manager = manager;
        this.cameraCharacteristicsHelper = cameraCharacteristicsHelper;
    }

    /**
     * Does open the front-facing camera of the device.
     * Make sure to close the camera object which is returned within the callback!
     *
     * @param callback set of callback functions which are called when the opening process completed
     * @throws CameraException if the camera could not be opened
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    public void openFrontFacingCamera(@NonNull CameraDevice.StateCallback callback) {
        try {

            String cameraId = getFrontFacingCameraId();
            if (cameraId == null) {
                throw new CameraException("device is missing a front-facing camera");
            }
            logSupportedHardwareLevel(cameraId);

            manager.openCamera(cameraId, callback, null);

        } catch (CameraAccessException e) {
            throw new CameraException(e);
        }
    }

    private void logSupportedHardwareLevel(@NonNull String cameraId) {
        int hardwareLevel = cameraCharacteristicsHelper.getSupportedHardwareLevel(cameraId);

        String hardwareLevelName = String.valueOf(hardwareLevel);  // for level 3 and higher
        switch (hardwareLevel) {
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY:
                hardwareLevelName = "legacy";
                break;
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED:
                hardwareLevelName = "limited";
                break;
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL:
                hardwareLevelName = "full";
                break;
        }

        log.d("using camera with supported hardware level: %s", hardwareLevelName);
    }

    @Nullable
    private String getFrontFacingCameraId() throws CameraAccessException {
        for (String cameraId : manager.getCameraIdList()) {
            try {
                if (cameraCharacteristicsHelper.getLensFacing(cameraId) == CameraCharacteristics.LENS_FACING_FRONT) {
                    return cameraId;
                }
            } catch (CameraException e) {
                // could not determine lens facing characteristic -> continue
            }
        }
        return null;
    }

    /**
     * Starts a camera preview on the given TextureView.
     * Make sure to close the CameraCaptureSession which is returned within the callback!
     *
     * @param camera                  camera to use for preview
     * @param textureView             where the preview is displayed
     * @param previewSize             size of the images within the preview stream
     * @param deviceOrientation       the actual orientation of the device
     * @param relativeDisplayRotation the rotation of the device relative to the native orientation
     * @param imageReader             will receive each image from the preview stream
     * @param onPreviewSessionStarted callback which can be used to obtain a reference to the open CameraCaptureSession
     * @throws CameraException if the preview could not be started
     */
    public void startCameraPreview(@NonNull final CameraDevice camera, @NonNull ProportionalTextureView textureView,
                                   @NonNull Size previewSize, @ConfigurationOrientation int deviceOrientation,
                                   @SurfaceRotation int relativeDisplayRotation,
                                   @NonNull final ImageReader imageReader,
                                   @NonNull final Consumer<CameraCaptureSession> onPreviewSessionStarted) {
        try {
            configureTextureView(textureView, deviceOrientation, relativeDisplayRotation, previewSize);

            final List<Surface> surfaces = asList(new Surface(textureView.getSurfaceTexture()), imageReader.getSurface());
            camera.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        setupCaptureRequestForPreview(session, camera, surfaces);
                    } catch (CameraException e) {
                        // Do not throw exception in this case because code is running asynchronously which would lead to app crash!
                        log.e("onConfigured failed: %s", e.getMessage());
                    }
                    onPreviewSessionStarted.accept(session);
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    // Do not throw exception in this case because code is running asynchronously which would lead to app crash!
                    log.e("configuring capture session for preview failed");
                }
            }, null);

        } catch (CameraAccessException e) {
            throw new CameraException(e);
        }
    }

    /**
     * configures the ProportionalTextureView to respect the aspect ratio of the image and using an appropriate buffer size
     */
    @VisibleForTesting
    void configureTextureView(@NonNull ProportionalTextureView textureView, @ConfigurationOrientation int deviceOrientation,
                              @SurfaceRotation int relativeDisplayRotation, @NonNull Size previewSize) {
        switch (deviceOrientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                // swap values because preview sizes are always landscape
                textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth(), relativeDisplayRotation);
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight(), relativeDisplayRotation);
                break;
        }

        // working more memory efficient
        SurfaceTexture surface = textureView.getSurfaceTexture();
        if (surface != null) {
            surface.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        } else {
            throw new CameraException("surface texture not attached to view");
        }
    }

    /**
     * does setup the repeating capture request for taking images for the preview
     */
    @VisibleForTesting
    void setupCaptureRequestForPreview(@NonNull CameraCaptureSession previewSession, @NonNull CameraDevice camera,
                                       @NonNull List<Surface> surfaces) {
        try {
            CaptureRequest.Builder previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            for (Surface surface : surfaces) {
                previewRequestBuilder.addTarget(surface);
            }
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            previewSession.setRepeatingRequest(previewRequestBuilder.build(), null, null);

        } catch (CameraAccessException | IllegalStateException e) {
            throw new CameraException(e);
        }
    }

    /**
     * Chooses an appropriate size for the images within the preview stream.
     *
     * @param camera camera to get available preview sizes
     * @return the preview size to use
     * @throws CameraException if the preview size could not be determined
     */
    @NonNull
    public Size selectPreviewSize(@NonNull CameraDevice camera) {

        Size[] previewSizes = cameraCharacteristicsHelper.getPreviewOutputSizes(camera.getId());
        if (previewSizes == null || previewSizes.length == 0) {
            throw new CameraException("camera did not provide any preview size");
        }

        // preferably the preview has a size of 640x480 to save bandwidth while being large enough for the backend to be acceptable
        for (Size imageSize : previewSizes) {
            if (imageSize.getWidth() == 640 && imageSize.getHeight() == 480) {
                return imageSize;
            }
        }

        // fallback to first one which might not be optimal
        log.w("preferred preview size of 640x480 is not available, using %s", previewSizes[0]);
        return previewSizes[0];
    }

    /**
     * Determines the rotation of images taken by the camera. Takes sensor and device rotation into account.
     *
     * @param camera                  to get the sensor rotation
     * @param relativeDisplayRotation rotation of the device relative to the native orientation
     * @return rotation of images taken by the camera
     * @throws CameraException if the sensor rotation could not be determined
     */
    @Rotation
    public int getImageRotation(@NonNull CameraDevice camera, @SurfaceRotation int relativeDisplayRotation) {

        @Rotation int sensorRotation = cameraCharacteristicsHelper.getSensorOrientation(camera.getId());
        @Rotation int relativeDisplayRotationInDegrees = surfaceRotationConstantToRotationDegrees(relativeDisplayRotation);

        //noinspection WrongConstant
        return (relativeDisplayRotationInDegrees + sensorRotation) % 360;
    }

    @Rotation
    private int surfaceRotationConstantToRotationDegrees(@SurfaceRotation int surfaceRotationConstant) {
        switch (surfaceRotationConstant) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_90:
                return 90;
        }
        throw new IllegalArgumentException("surfaceRotationConstant must be one of the rotation values");
    }
}
