package com.bioid.authenticator.base.image;

import android.graphics.ImageFormat;
import android.media.Image;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Yuv420ImageTest {

    private static final int IMG_WIDTH = 800;
    private static final int IMG_HEIGHT = 600;

    private static final int Y_ROW_STRIDE = 10;
    private static final int U_ROW_STRIDE = 20;
    private static final int U_PIXEL_STRIDE = 30;

    private static final int Y_PLANES_VALUES = 0;
    private static final int U_PLANES_VALUES = 1;
    private static final int V_PLANES_VALUES = 2;

    @Mock
    private Image jpeg;
    @Mock
    private Image yuv420;
    @Mock
    private Image.Plane yPlane;
    @Mock
    private Image.Plane uPlane;
    @Mock
    private Image.Plane vPlane;

    private byte[] yData;
    private byte[] uData;
    private byte[] vData;

    @Before
    public void setUp() throws Exception {
        when(jpeg.getFormat()).thenReturn(ImageFormat.JPEG);
        when(yuv420.getFormat()).thenReturn(ImageFormat.YUV_420_888);

        when(yuv420.getWidth()).thenReturn(IMG_WIDTH);
        when(yuv420.getHeight()).thenReturn(IMG_HEIGHT);

        when(yuv420.getPlanes()).thenReturn(new Image.Plane[]{yPlane, uPlane, vPlane});

        when(yPlane.getRowStride()).thenReturn(Y_ROW_STRIDE);
        when(uPlane.getRowStride()).thenReturn(U_ROW_STRIDE);
        when(uPlane.getPixelStride()).thenReturn(U_PIXEL_STRIDE);

        yData = new byte[]{Y_PLANES_VALUES, Y_PLANES_VALUES, Y_PLANES_VALUES};
        uData = new byte[]{U_PLANES_VALUES, U_PLANES_VALUES, U_PLANES_VALUES};
        vData = new byte[]{V_PLANES_VALUES, V_PLANES_VALUES, V_PLANES_VALUES};

        when(yPlane.getBuffer()).thenReturn(ByteBuffer.wrap(yData));
        when(uPlane.getBuffer()).thenReturn(ByteBuffer.wrap(uData));
        when(vPlane.getBuffer()).thenReturn(ByteBuffer.wrap(vData));
    }

    @Test
    public void testMakeCopy_allValuesAreCorrect() throws Exception {
        Yuv420Image result = Yuv420Image.makeCopy(yuv420);

        assertThat(result.width, is(IMG_WIDTH));
        assertThat(result.height, is(IMG_HEIGHT));

        assertThat(result.yPlane, is(yData));
        assertThat(result.uPlane, is(uData));
        assertThat(result.vPlane, is(vData));

        assertThat(result.yRowStride, is(Y_ROW_STRIDE));
        assertThat(result.uvRowStride, is(U_ROW_STRIDE));
        assertThat(result.uvPixelStride, is(U_PIXEL_STRIDE));
    }

    @Test
    public void testMakeCopy_ensureDataIsReallyCopied() throws Exception {
        Yuv420Image result = Yuv420Image.makeCopy(yuv420);

        // modify original image
        yData[0] = 9;
        uData[0] = 9;
        vData[0] = 9;

        assertThat(result.yPlane[0], is((byte) Y_PLANES_VALUES));
        assertThat(result.uPlane[0], is((byte) U_PLANES_VALUES));
        assertThat(result.vPlane[0], is((byte) V_PLANES_VALUES));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMakeCopy_passingNonYuv420ImageThrowsException() throws Exception {
        Yuv420Image.makeCopy(jpeg);
    }
}