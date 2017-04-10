package com.bioid.authenticator.base.image;

import android.graphics.Bitmap;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.bioid.authenticator.base.logging.LoggingHelper;
import com.bioid.authenticator.base.logging.LoggingHelperFactory;

import java.io.ByteArrayOutputStream;
import java.nio.IntBuffer;

/**
 * Provides methods to convert from one image format to another.
 */
public class ImageFormatConverter {

    private final LoggingHelper log;

    public ImageFormatConverter() {
        this.log = LoggingHelperFactory.create(ImageFormatConverter.class);
    }

    /**
     * Converts a GrayscaleImage to a Bitmap.
     */
    @NonNull
    public Bitmap grayscaleImageToBitmap(@NonNull GrayscaleImage img) {
        String stopwatchSessionId = log.startStopwatch(getStopwatchSessionId("grayscaleImageToBitmap"));

        int size = img.width * img.height;
        int[] buffer = new int[size];

        for (int index = 0; index < size; index++) {
            // "AND 0xff" for the signed byte issue
            int luminance = img.data[index] & 0xff;
            // normal encoding for bitmap
            buffer[index] = (0xff000000 | luminance << 16 | luminance << 8 | luminance);
        }

        Bitmap bitmap = Bitmap.createBitmap(img.width, img.height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(IntBuffer.wrap(buffer));

        log.stopStopwatch(stopwatchSessionId);
        return bitmap;
    }

    /**
     * Converts a YUV_420_888 IntensityPlane to a GrayscaleImage.
     */
    @NonNull
    public GrayscaleImage intensityPlaneToGrayscaleImage(@NonNull IntensityPlane intensity) {

        if (intensity.rowStride == intensity.width) {
            // we can take the plane (yPlane from YUV) as it is
            return new GrayscaleImage(intensity.plane, intensity.width, intensity.height);
        }

        int counter = 0;
        byte[] data = new byte[intensity.width * intensity.height];
        for (int y = 0; y < intensity.height; y++) {
            int offset = y * intensity.rowStride;
            for (int x = 0; x < intensity.width; x++) {
                data[counter++] = intensity.plane[x + offset];
            }
        }
        return new GrayscaleImage(data, intensity.width, intensity.height);
    }

    /**
     * Converts a Bitmap to a GrayscaleImage.
     */
    @NonNull
    public GrayscaleImage bitmapToGrayscaleImage(@NonNull Bitmap bitmap) {
        String stopwatchSessionId = log.startStopwatch(getStopwatchSessionId("bitmapToGrayscaleImage"));

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = width * height;

        IntBuffer intBuffer = IntBuffer.allocate(size);
        bitmap.copyPixelsToBuffer(intBuffer);
        int[] buffer = intBuffer.array();

        byte[] data = new byte[size];
        for (int index = 0; index < size; index++) {
            data[index] = (byte) (buffer[index] >> 16);
        }

        log.stopStopwatch(stopwatchSessionId);
        return new GrayscaleImage(data, width, height);
    }

    /**
     * Converts a Bitmap to a PNG image.
     */
    @NonNull
    public byte[] bitmapToPng(@NonNull Bitmap bitmap) {
        String stopwatchSessionId = log.startStopwatch(getStopwatchSessionId("bitmapToPng"));

        // compress Bitmap to PNG
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

        log.stopStopwatch(stopwatchSessionId);
        return out.toByteArray();
    }

    private String getStopwatchSessionId(@NonNull String methodName) {
        return methodName + " (" + SystemClock.elapsedRealtimeNanos() + ")";
    }
}
