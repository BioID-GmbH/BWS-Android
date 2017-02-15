package com.bioid.authenticator.base.image;

import android.graphics.ImageFormat;
import android.media.Image;
import android.support.annotation.NonNull;

import java.nio.ByteBuffer;

/**
 * Class representing an image in the YUV_420_888 image format.
 */
@SuppressWarnings("WeakerAccess")
public class Yuv420Image {

    @NonNull
    public final byte[] yPlane;
    @NonNull
    public final byte[] uPlane;
    @NonNull
    public final byte[] vPlane;
    public final int yRowStride;
    public final int uvRowStride;
    public final int uvPixelStride;
    public final int width;
    public final int height;

    // use makeCopy() instead
    private Yuv420Image(@NonNull byte[] yPlane, @NonNull byte[] uPlane, @NonNull byte[] vPlane,
                        int yRowStride, int uvRowStride,
                        int uvPixelStride, int width, int height) {
        this.yPlane = yPlane;
        this.uPlane = uPlane;
        this.vPlane = vPlane;
        this.yRowStride = yRowStride;
        this.uvRowStride = uvRowStride;
        this.uvPixelStride = uvPixelStride;
        this.width = width;
        this.height = height;
    }

    /**
     * Does make a copy of a YUV_420_888 image.
     * (image will not be closed)
     *
     * @throws IllegalArgumentException if the image is not in the YUV_420_888 format
     */
    @NonNull
    public static Yuv420Image makeCopy(@NonNull Image img) {

        if (img.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("image format must be YUV_420_888");
        }

        Image.Plane[] planes = img.getPlanes();

        ByteBuffer buffer = planes[0].getBuffer();
        byte[] yPlane = new byte[buffer.remaining()];
        buffer.get(yPlane);

        buffer = planes[1].getBuffer();
        byte[] uPlane = new byte[buffer.remaining()];
        buffer.get(uPlane);

        buffer = planes[2].getBuffer();
        byte[] vPlane = new byte[buffer.remaining()];
        buffer.get(vPlane);

        // From documentation we know that yPixelStride is always 1 and strides are the same for u and v.
        int yRowStride = planes[0].getRowStride();
        int uvRowStride = planes[1].getRowStride();
        int uvPixelStride = planes[1].getPixelStride();

        int width = img.getWidth();
        int height = img.getHeight();

        return new Yuv420Image(yPlane, uPlane, vPlane, yRowStride, uvRowStride, uvPixelStride, width, height);
    }
}
