package com.bioid.authenticator.base.image;

import android.graphics.ImageFormat;
import android.media.Image;
import android.support.annotation.NonNull;

import java.nio.ByteBuffer;

/**
 * IntensityPlane of a YUV_420_888 image.
 */
@SuppressWarnings("WeakerAccess")
public class IntensityPlane {

    public final int width;
    public final int height;
    public final byte[] plane;
    public final int rowStride;

    // use IntensityPlane.extract instead
    private IntensityPlane(int width, int height, byte[] plane, int rowStride) {
        this.width = width;
        this.height = height;
        this.plane = plane;
        this.rowStride = rowStride;
    }

    /**
     * Extracts the Y-Plane from the YUV_420_8888 image to creates a IntensityPlane.
     * The actual plane data will be copied into the new IntensityPlane object.
     *
     * @throws IllegalArgumentException if the provided images is not in the YUV_420_888 format
     */
    @NonNull
    public static IntensityPlane extract(@NonNull Image img) {
        if (img.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("image format must be YUV_420_888");
        }

        Image.Plane[] planes = img.getPlanes();

        ByteBuffer buffer = planes[0].getBuffer();
        byte[] yPlane = new byte[buffer.remaining()];
        buffer.get(yPlane);

        int yRowStride = planes[0].getRowStride();

        return new IntensityPlane(img.getWidth(), img.getHeight(), yPlane, yRowStride);
    }
}
