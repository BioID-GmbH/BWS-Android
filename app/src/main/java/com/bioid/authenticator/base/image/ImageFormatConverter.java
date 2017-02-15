package com.bioid.authenticator.base.image;

import android.graphics.Bitmap;
import android.os.SystemClock;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.Script;
import android.renderscript.Type;
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
    private final RenderScript rs;
    private final ScriptC_yuv420888 yuv420888;

    public ImageFormatConverter(RenderScript rs) {
        this.log = LoggingHelperFactory.create(ImageFormatConverter.class);
        this.rs = rs;
        this.yuv420888 = new ScriptC_yuv420888(rs);
    }

    /**
     * Does convert a YUV_420_888 image to a RGB bitmap.
     */
    @NonNull
    public Bitmap yuv420ToRgb(@NonNull Yuv420Image img) {
        String stopwatchSessionId = log.startStopwatch(getStopwatchSessionId("yuv420ToRgb"));

        // Source is taken from (with some modifications):
        // http://stackoverflow.com/questions/36212904/yuv-420-888-interpretation-on-samsung-galaxy-s7-camera2

        // Y,U,V are defined as global allocations, the out-Allocation is the Bitmap.
        // Note also that uAlloc and vAlloc are 1-dimensional while yAlloc is 2-dimensional.
        Type.Builder typeUcharY = new Type.Builder(rs, Element.U8(rs));
        typeUcharY.setX(img.yRowStride).setY(img.height);
        Allocation yAlloc = Allocation.createTyped(rs, typeUcharY.create());
        yAlloc.copyFrom(img.yPlane);
        yuv420888.set_ypsIn(yAlloc);

        Type.Builder typeUcharUV = new Type.Builder(rs, Element.U8(rs));
        // note that the size of the u's and v's are as follows:
        //      (  (width/2)*PixelStride + padding  ) * (height/2)
        // =    (RowStride                          ) * (height/2)
        // but I noted that on the S7 it is 1 less...
        typeUcharUV.setX(img.uPlane.length);
        Allocation uAlloc = Allocation.createTyped(rs, typeUcharUV.create());
        uAlloc.copyFrom(img.uPlane);
        yuv420888.set_uIn(uAlloc);

        Allocation vAlloc = Allocation.createTyped(rs, typeUcharUV.create());
        vAlloc.copyFrom(img.vPlane);
        yuv420888.set_vIn(vAlloc);

        // handover parameters
        yuv420888.set_picWidth(img.width);
        yuv420888.set_uvRowStride(img.uvRowStride);
        yuv420888.set_uvPixelStride(img.uvPixelStride);

        Bitmap outBitmap = Bitmap.createBitmap(img.width, img.height, Bitmap.Config.ARGB_8888);
        Allocation outAlloc = Allocation.createFromBitmap(rs, outBitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);

        Script.LaunchOptions lo = new Script.LaunchOptions();
        lo.setX(0, img.width);  // by this we ignore the y’s padding zone, i.e. the right side of x between width and yRowStride
        lo.setY(0, img.height);

        yuv420888.forEach_doConvert(outAlloc, lo);
        outAlloc.copyTo(outBitmap);

        log.stopStopwatch(stopwatchSessionId);
        return outBitmap;
    }

    /**
     * Does extract the grayscale part of a YUV_420_888 image.
     */
    @NonNull
    public GrayscaleImage yuv420ToGrayscaleImage(@NonNull Yuv420Image img) {
        byte[] data = new byte[img.width * img.height];

        int i = 0;
        for (int y = 0; y < img.height; y++) {
            for (int x = 0; x < img.width; x++) {
                data[i++] = img.yPlane[x + y * img.yRowStride];
            }
        }

        return new GrayscaleImage(data, img.width, img.height);
    }

    /**
     * Does convert a {@link GrayscaleImage} to a uncompressed PNG.
     */
    @NonNull
    public byte[] grayscaleImageToPng(@NonNull GrayscaleImage img) {
        String stopwatchSessionId = log.startStopwatch(getStopwatchSessionId("grayscaleImageToPng"));

        Bitmap bitmap = Bitmap.createBitmap(img.width, img.height, Bitmap.Config.ARGB_8888);

        IntBuffer buffer = IntBuffer.allocate(img.width * img.height);
        for (int y = 0; y < img.height; y++) {
            for (int x = 0; x < img.width; x++) {
                // "AND 0xff" for the signed byte issue
                int luminance = img.data[y * img.width + x] & 0xff;
                // put that pixel in the buffer
                buffer.put(0xff000000 | luminance << 16 | luminance << 8 | luminance);
            }
        }

        // get buffer ready to be read
        buffer.flip();

        bitmap.copyPixelsFromBuffer(buffer);

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
