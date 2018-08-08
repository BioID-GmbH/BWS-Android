package com.bioid.authenticator.base.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.media.Image;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import com.bioid.authenticator.base.annotations.Rotation;
import com.bioid.authenticator.base.logging.LoggingHelper;
import com.bioid.authenticator.base.logging.LoggingHelperFactory;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Container for YUV_420_888 image data.
 * <p>
 * The main purpose of this container is to provide the raw YUV_420_888 image data in various image formats.
 * Every derived representation of the original image is computed lazily and will be cached within the container.
 */
public class Yuv420Image {

    private final LoggingHelper log;

    @NonNull
    private final byte[] yPlane;
    @NonNull
    private final byte[] uPlane;
    @NonNull
    private final byte[] vPlane;

    private final int yRowStride;
    private final int uvRowStride;
    private final int uvPixelStride;

    private final int width;
    private final int height;

    @Rotation
    private final int rotation;

    @NonNull
    private final Context ctx;

    @Nullable
    private RenderScript rs;
    @Nullable
    private Bitmap bitmapRepresentation;
    @Nullable
    private byte[] pngRepresentation;
    @Nullable
    private GrayscaleImage downscaledGrayscaleRepresentation;

    // use copyFrom() instead
    private Yuv420Image(@NonNull byte[] yPlane, @NonNull byte[] uPlane, @NonNull byte[] vPlane,
                        int yRowStride, int uvRowStride, int uvPixelStride,
                        int width, int height,
                        @Rotation int rotation,
                        @NonNull Context ctx) {
        this.log = LoggingHelperFactory.create(Yuv420Image.class);
        this.yPlane = yPlane;
        this.uPlane = uPlane;
        this.vPlane = vPlane;
        this.yRowStride = yRowStride;
        this.uvRowStride = uvRowStride;
        this.uvPixelStride = uvPixelStride;
        this.width = width;
        this.height = height;
        this.rotation = rotation;
        this.ctx = ctx;
    }

    /**
     * Does make a copy of the YUV_420_888 image data.
     * (img will not be closed)
     *
     * @param img      image in YUV_420_888 format
     * @param rotation rotation in which the image is captured
     * @param ctx      the Android application context
     * @return a container object with the copied YUV image data
     * @throws IllegalArgumentException if the image is not in the YUV_420_888 format
     */
    @NonNull
    public static Yuv420Image copyFrom(@NonNull Image img, @Rotation int rotation, @NonNull Context ctx) {

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

        return new Yuv420Image(yPlane, uPlane, vPlane, yRowStride, uvRowStride, uvPixelStride, width, height, rotation, ctx);
    }

    /**
     * Returns a rotated Bitmap representation of the original YUV_420_888 image.
     * <p>
     * This operation might take some time and should NOT be executed on the application main thread!
     * <p>
     * Currently the Bitmap is grayscale only. This will change in future versions.
     */
    @NonNull
    @WorkerThread
    public Bitmap asBitmap() {
        if (bitmapRepresentation == null) {
            String stopwatchSessionId = log.startStopwatch(getStopwatchSessionId("asBitmap"));
            createBitmapRepresentation();
            log.stopStopwatch(stopwatchSessionId);
        }
        return bitmapRepresentation;
    }

    private void createBitmapRepresentation() {
        RenderScript rs = getRenderScript();

        // ScriptIntrinsicYuvToRGB works with NV21 - U and V order reversed: it starts with V.
        // Change yuv to yvu
        byte[] yuvData = ByteBuffer.allocate(yPlane.length + uPlane.length + vPlane.length)
                .put(yPlane)
                .put(vPlane)
                .put(uPlane)
                .array();

        ScriptIntrinsicYuvToRGB yuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        Type yuvType = new Type.Builder(rs, Element.U8(rs))
                .setX(yuvData.length)
                .create();
        Type rgbType = new Type.Builder(rs, Element.RGBA_8888(rs))
                .setX(width)
                .setY(height)
                .create();

        Allocation input = Allocation.createTyped(rs, yuvType, Allocation.USAGE_SCRIPT);
        input.copyFrom(yuvData);
        yuvToRgb.setInput(input);

        Allocation output = Allocation.createTyped(rs, rgbType, Allocation.USAGE_SCRIPT);
        yuvToRgb.forEach(output);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        output.copyTo(bitmap);

        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);

        bitmapRepresentation = bitmap;
    }

    /**
     * Returns a rotated PNG representation of the original YUV_420_888 image.
     * <p>
     * This operation might take some time and should NOT be executed on the application main thread!
     * <p>
     * Currently the PNG is grayscale only. This will change in future versions.
     */
    @NonNull
    @WorkerThread
    public byte[] asPNG() {
        if (pngRepresentation == null) {
            String stopwatchSessionId = log.startStopwatch(getStopwatchSessionId("asPNG"));
            createPngRepresentation();
            log.stopStopwatch(stopwatchSessionId);
        }
        return pngRepresentation;
    }

    private void createPngRepresentation() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        asBitmap().compress(Bitmap.CompressFormat.PNG, 100, out);
        pngRepresentation = out.toByteArray();
    }

    /**
     * Returns a downscaled grayscale representation of the original YUV_420_888 image.
     * The image will have a maximum width (portrait mode) or height (landscape mode) of 96 pixels.
     * <p>
     * This operation might take some time and should NOT be executed on the application main thread!
     * <p>
     * Currently the Bitmap is grayscale only. This will change in future versions.
     */
    @NonNull
    @WorkerThread
    public GrayscaleImage asDownscaledGrayscaleImage() {
        if (downscaledGrayscaleRepresentation == null) {
            String stopwatchSessionId = log.startStopwatch(getStopwatchSessionId("asDownscaledGrayscaleImage"));
            createDownscaledGrayscaleImageRepresentation();
            log.stopStopwatch(stopwatchSessionId);
        }
        return downscaledGrayscaleRepresentation;
    }

    private void createDownscaledGrayscaleImageRepresentation() {
        Bitmap bitmap = asBitmap();

        int resizeWidth, resizeHeight;
        if (bitmap.getWidth() > bitmap.getHeight()) {
            // Landscape mode
            resizeHeight = 96;
            // Calculate new width according to aspect ratio of original image
            resizeWidth = bitmap.getWidth() * resizeHeight / bitmap.getHeight();
        } else {
            // Portrait mode
            resizeWidth = 96;
            // Calculate new height according to aspect ratio of original image
            resizeHeight = bitmap.getHeight() * resizeWidth / bitmap.getWidth();
        }

        Bitmap downscaledBitmap = Bitmap.createScaledBitmap(bitmap, resizeWidth, resizeHeight, true);

        int resizeSize = resizeWidth * resizeHeight;

        IntBuffer intBuffer = IntBuffer.allocate(resizeSize);
        downscaledBitmap.copyPixelsToBuffer(intBuffer);
        int[] buffer = intBuffer.array();

        byte[] data = new byte[resizeSize];
        for (int index = 0; index < resizeSize; index++) {
            data[index] = (byte) (buffer[index] >> 16);
        }

        downscaledGrayscaleRepresentation = new GrayscaleImage(data, resizeWidth, resizeHeight);
    }

    @NonNull
    private RenderScript getRenderScript() {
        if (rs == null) {
            rs = RenderScript.create(ctx);
        }
        return rs;
    }

    @NonNull
    private String getStopwatchSessionId(@NonNull String methodName) {
        return methodName + " (" + this + ")";
    }

    @Override
    public String toString() {
        // short version of toString() without the full name of the class
        return "Yuv420Image@" + Integer.toHexString(hashCode());
    }
}
