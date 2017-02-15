package com.bioid.authenticator.base.camera;

import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.util.Size;
import android.view.Surface;

import com.bioid.authenticator.base.annotations.Rotation;
import com.bioid.authenticator.base.annotations.SurfaceRotation;
import com.bioid.authenticator.base.logging.LoggingHelper;
import com.bioid.authenticator.testutil.Mocks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.theInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("MissingPermission")
public class CameraHelperTest {

    private static final String CAMERA_ID_1 = "back_camera";
    private static final String CAMERA_ID_2 = "font_camera";
    private static final Size PREVIEWSIZE_1 = Mocks.mockSize(10, 10);
    private static final Size PREVIEWSIZE_640x480 = Mocks.mockSize(640, 480);
    private static final Size PREVIEWSIZE_2 = Mocks.mockSize(50, 50);
    @Rotation
    private static final int SENSOR_ROTATION = 270;
    @SurfaceRotation
    private static final int RELATIVE_DISPLAY_ROTATION = Surface.ROTATION_270;
    @Rotation
    private static final int IMAGE_ROTATION = 180;

    private List<Surface> surfaces;

    @Mock
    private LoggingHelper log;
    @Mock
    private CameraManager manager;
    @Mock
    private CameraCharacteristicsHelper characteristicsHelper;
    @Mock
    private CameraDevice.StateCallback stateCallback;
    @Mock
    private CameraDevice camera;
    @Mock
    private ProportionalTextureView textureView;
    @Mock
    private SurfaceTexture surfaceTexture;
    @Mock
    private CameraCaptureSession session;
    @Mock
    private Surface surface1;
    @Mock
    private Surface surface2;
    @Mock
    private CaptureRequest.Builder captureRequestBuilder;

    @InjectMocks
    private CameraHelper cameraHelper;

    @Before
    public void setUp() throws Exception {
        surfaces = asList(surface1, surface2);

        when(manager.getCameraIdList()).thenReturn(new String[]{CAMERA_ID_1, CAMERA_ID_2});

        when(characteristicsHelper.getLensFacing(CAMERA_ID_1)).thenReturn(CameraCharacteristics.LENS_FACING_BACK);
        when(characteristicsHelper.getLensFacing(CAMERA_ID_2)).thenReturn(CameraCharacteristics.LENS_FACING_FRONT);
        when(characteristicsHelper.getSensorOrientation(CAMERA_ID_1)).thenReturn(SENSOR_ROTATION);
        when(characteristicsHelper.getPreviewOutputSizes(CAMERA_ID_1)).thenReturn(
                new Size[]{PREVIEWSIZE_1, PREVIEWSIZE_640x480, PREVIEWSIZE_2});

        when(camera.getId()).thenReturn(CAMERA_ID_1);
        when(camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)).thenReturn(captureRequestBuilder);

        when(textureView.getSurfaceTexture()).thenReturn(surfaceTexture);
    }

    @Test
    public void testOpenFrontFacingCamera_frontFacingCameraOpened() throws Exception {
        cameraHelper.openFrontFacingCamera(stateCallback);
        verify(manager).openCamera(CAMERA_ID_2, stateCallback, null);
    }

    @Test(expected = CameraException.class)
    public void testOpenFrontFacingCamera_noFrontFacingCameraOnDeviceThrowsException_onlyOneBackFacingCameraOnly() throws Exception {
        when(manager.getCameraIdList()).thenReturn(new String[]{CAMERA_ID_1});
        cameraHelper.openFrontFacingCamera(stateCallback);
    }

    @Test(expected = CameraException.class)
    public void testOpenFrontFacingCamera_noFrontFacingCameraOnDeviceThrowsException_characteristicNotAvailable() throws Exception {
        doThrow(CameraException.class).when(characteristicsHelper).getLensFacing(CAMERA_ID_2);
        cameraHelper.openFrontFacingCamera(stateCallback);
    }

    @Test(expected = CameraException.class)
    public void testOpenFrontFacingCamera_cameraAccessExceptionOnGetCameraIdListThrowsException() throws Exception {
        doThrow(CameraAccessException.class).when(manager).getCameraIdList();
        cameraHelper.openFrontFacingCamera(stateCallback);
    }

    @Test(expected = CameraException.class)
    public void testOpenFrontFacingCamera_cameraAccessExceptionOnOpenCameraThrowsException() throws Exception {
        doThrow(CameraAccessException.class).when(manager).openCamera(CAMERA_ID_2, stateCallback, null);
        cameraHelper.openFrontFacingCamera(stateCallback);
    }

    @Test
    public void testSetupCaptureRequestForPreview_captureRequestBuilderIsObtainedCorrectly() throws Exception {
        cameraHelper.setupCaptureRequestForPreview(session, camera, surfaces);
        verify(camera).createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
    }

    @Test
    public void testSetupCaptureRequestForPreview_captureRequestBuilderIsConfiguredCorrectly() throws Exception {
        cameraHelper.setupCaptureRequestForPreview(session, camera, surfaces);
        verify(captureRequestBuilder).addTarget(surface1);
        verify(captureRequestBuilder).addTarget(surface2);
        verify(captureRequestBuilder).set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
    }

    @Test(expected = CameraException.class)
    public void testSetupCaptureRequestForPreview_cameraAccessExceptionOnCreateCaptureRequestThrowsException() throws Exception {
        doThrow(CameraAccessException.class).when(camera).createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        cameraHelper.setupCaptureRequestForPreview(session, camera, surfaces);
    }

    @Test(expected = CameraException.class)
    public void testSetupCaptureRequestForPreview_illegalStateExceptionOnCreateCaptureRequestThrowsException() throws Exception {
        doThrow(IllegalStateException.class).when(camera).createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        cameraHelper.setupCaptureRequestForPreview(session, camera, surfaces);
    }

    @Test(expected = CameraException.class)
    public void testSetupCaptureRequestForPreview_cameraAccessExceptionOnSetRepeatingRequestThrowsException() throws Exception {
        // do always throw CameraAccessException
        session = mock(CameraCaptureSession.class, new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                throw new CameraAccessException(CameraAccessException.CAMERA_ERROR);
            }
        });

        cameraHelper.setupCaptureRequestForPreview(session, camera, surfaces);
    }

    @Test
    public void testConfigureTextureView_AspectRatioWillBeSetToPreviewSizeInLandscapeMode() throws Exception {
        cameraHelper.configureTextureView(textureView, Configuration.ORIENTATION_LANDSCAPE, RELATIVE_DISPLAY_ROTATION,
                PREVIEWSIZE_640x480);
        verify(textureView).setAspectRatio(PREVIEWSIZE_640x480.getWidth(), PREVIEWSIZE_640x480.getHeight(), RELATIVE_DISPLAY_ROTATION);
    }

    @Test
    public void testConfigureTextureView_AspectRatioWillBeSetToReversePreviewSizeInPortraitMode() throws Exception {
        cameraHelper.configureTextureView(textureView, Configuration.ORIENTATION_PORTRAIT, RELATIVE_DISPLAY_ROTATION,
                PREVIEWSIZE_640x480);
        verify(textureView).setAspectRatio(PREVIEWSIZE_640x480.getHeight(), PREVIEWSIZE_640x480.getWidth(), RELATIVE_DISPLAY_ROTATION);
    }

    @Test(expected = CameraException.class)
    public void testConfigureTextureView_SurfaceTextureBeingNullThrowsException() throws Exception {
        when(textureView.getSurfaceTexture()).thenReturn(null);
        cameraHelper.configureTextureView(textureView, Configuration.ORIENTATION_PORTRAIT, RELATIVE_DISPLAY_ROTATION,
                PREVIEWSIZE_640x480);
    }

    @Test
    public void testConfigureTextureView_BufferSizeWillBeSetToPreviewSize() throws Exception {
        cameraHelper.configureTextureView(textureView, Configuration.ORIENTATION_PORTRAIT, RELATIVE_DISPLAY_ROTATION,
                PREVIEWSIZE_640x480);
        verify(surfaceTexture).setDefaultBufferSize(PREVIEWSIZE_640x480.getWidth(), PREVIEWSIZE_640x480.getHeight());
    }

    @Test
    public void testSelectPreviewSize_choosing640x480ifAvailable() throws Exception {
        Size actual = cameraHelper.selectPreviewSize(camera);
        assertThat(actual, is(theInstance(PREVIEWSIZE_640x480)));
    }

    @Test
    public void testSelectPreviewSize_fallbackToFirstPreviewSizeIf640x480notAvailable() throws Exception {
        when(characteristicsHelper.getPreviewOutputSizes(CAMERA_ID_1)).thenReturn(new Size[]{PREVIEWSIZE_1, PREVIEWSIZE_2});
        Size actual = cameraHelper.selectPreviewSize(camera);
        assertThat(actual, is(theInstance(PREVIEWSIZE_1)));
    }

    @Test(expected = CameraException.class)
    public void testSelectPreviewSize_noAvailablePreviewSizeThrowsException_arrayIsNull() throws Exception {
        when(characteristicsHelper.getPreviewOutputSizes(CAMERA_ID_1)).thenReturn(null);
        cameraHelper.selectPreviewSize(camera);
    }

    @Test(expected = CameraException.class)
    public void testSelectPreviewSize_noAvailablePreviewSizeThrowsException_arrayIsEmpty() throws Exception {
        when(characteristicsHelper.getPreviewOutputSizes(CAMERA_ID_1)).thenReturn(new Size[]{});
        cameraHelper.selectPreviewSize(camera);
    }

    @Test
    public void testGetImageRotation_sensorAndDeviceRotationAreInterpretedCorrectly() throws Exception {
        int result = cameraHelper.getImageRotation(camera, RELATIVE_DISPLAY_ROTATION);
        assertThat(result, is(IMAGE_ROTATION));
    }

    @Test(expected = CameraException.class)
    public void testGetImageRotation_noSensorRotationThrowsException() throws Exception {
        doThrow(CameraException.class).when(characteristicsHelper).getSensorOrientation(CAMERA_ID_1);
        cameraHelper.getImageRotation(camera, RELATIVE_DISPLAY_ROTATION);
    }
}