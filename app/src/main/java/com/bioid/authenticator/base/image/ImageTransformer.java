package com.bioid.authenticator.base.image;

import android.annotation.SuppressLint;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.bioid.authenticator.base.annotations.Rotation;
import com.bioid.authenticator.base.logging.LoggingHelper;
import com.bioid.authenticator.base.logging.LoggingHelperFactory;

/**
 * Provides methods to do various image transformations.
 */
public class ImageTransformer {

    private final LoggingHelper log;

    public ImageTransformer() {
        this.log = LoggingHelperFactory.create(ImageTransformer.class);
    }

    /**
     * Rotates a {@link GrayscaleImage} clockwise by the specified number of degrees.
     * If degrees is 0 (or any multiple of 360) the original image will be returned and no transformation operation does happen.
     */
    @SuppressLint("SwitchIntDef")
    @NonNull
    public GrayscaleImage rotate(@NonNull GrayscaleImage img, @Rotation int degrees) {
        if (degrees % 360 == 0) {
            // do not apply transformation to improve performance
            return img;
        }

        String stopwatchSessionId = log.startStopwatch(getStopwatchSessionId("rotateGrayscaleImage"));

        GrayscaleImage rotatedImg;
        switch (degrees) {
            case 90:
                rotatedImg = rotate90Degrees(img);
                break;
            case 180:
                rotatedImg = rotate180Degrees(img);
                break;
            case 270:
                rotatedImg = rotate270Degrees(img);
                break;
            default:
                throw new IllegalArgumentException("degrees must be one of the rotation values");
        }

        log.stopStopwatch(stopwatchSessionId);
        return rotatedImg;
    }

    @NonNull
    private GrayscaleImage rotate90Degrees(@NonNull GrayscaleImage img) {
        byte[] data = new byte[img.data.length];
        int pos = 0;
        for (int x = 0; x < img.width; x++) {
            for (int y = img.height - 1; y >= 0; y--) {
                data[pos] = img.data[y * img.width + x];
                pos++;
            }
        }
        //noinspection SuspiciousNameCombination
        return new GrayscaleImage(data, img.height, img.width);
    }

    @NonNull
    private GrayscaleImage rotate180Degrees(@NonNull GrayscaleImage img) {
        byte[] data = new byte[img.data.length];
        int i, pos = 0;
        for (i = img.width * img.height - 1; i >= 0; i--) {
            data[pos] = img.data[i];
            pos++;
        }
        return new GrayscaleImage(data, img.width, img.height);
    }

    @NonNull
    private GrayscaleImage rotate270Degrees(@NonNull GrayscaleImage img) {
        return rotate180Degrees(rotate90Degrees(img));
    }

    private String getStopwatchSessionId(@NonNull String methodName) {
        return methodName + " (" + SystemClock.elapsedRealtimeNanos() + ")";
    }
}
