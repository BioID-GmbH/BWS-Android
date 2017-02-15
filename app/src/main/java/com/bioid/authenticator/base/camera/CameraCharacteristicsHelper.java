package com.bioid.authenticator.base.camera;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Size;

import com.bioid.authenticator.base.annotations.Rotation;

/**
 * Class which provides helper methods to deal with CameraCharacteristics.
 * Because {@link CameraCharacteristics} and {@link StreamConfigurationMap} are final this is helper is also useful for testing.
 */
class CameraCharacteristicsHelper {

    private final CameraManager manager;

    public CameraCharacteristicsHelper(CameraManager manager) {
        this.manager = manager;
    }

    /**
     * Returns the {@link CameraCharacteristics#LENS_FACING} characteristic.
     *
     * @throws CameraException if the characteristic could not be determined
     */
    public int getLensFacing(@NonNull String cameraId) {
        return getOrThrow(cameraId, CameraCharacteristics.LENS_FACING);
    }

    /**
     * Returns the available output sizes for SurfaceTextures from the {@link CameraCharacteristics#SCALER_STREAM_CONFIGURATION_MAP}
     * characteristic.
     *
     * @throws CameraException if the sizes could not be determined
     */
    @Nullable
    public Size[] getPreviewOutputSizes(@NonNull String cameraId) {
        StreamConfigurationMap configurationMap = getOrThrow(cameraId, CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        return configurationMap.getOutputSizes(SurfaceTexture.class);
    }

    /**
     * Returns the {@link CameraCharacteristics#SENSOR_ORIENTATION} characteristic.
     *
     * @throws CameraException if the characteristic could not be determined
     */
    @Rotation
    @SuppressWarnings("WrongConstant")
    public int getSensorOrientation(@NonNull String cameraId) {
        return getOrThrow(cameraId, CameraCharacteristics.SENSOR_ORIENTATION);
    }

    /**
     * Returns the {@link CameraCharacteristics#INFO_SUPPORTED_HARDWARE_LEVEL} characteristic.
     *
     * @throws CameraException if the characteristic could not be determined
     */
    public int getSupportedHardwareLevel(@NonNull String cameraId) {
        return getOrThrow(cameraId, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
    }

    @NonNull
    private <T> T getOrThrow(@NonNull String cameraId, @NonNull CameraCharacteristics.Key<T> key) {
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            T value = characteristics.get(key);

            if (value == null) {
                throw new CameraException("could not determine camera characteristic " + key.getName() + " from camera " + cameraId);
            } else {
                return value;
            }
        } catch (CameraAccessException e) {
            throw new CameraException(e);
        }
    }
}
