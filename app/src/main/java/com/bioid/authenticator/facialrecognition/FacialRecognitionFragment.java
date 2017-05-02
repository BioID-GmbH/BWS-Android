package com.bioid.authenticator.facialrecognition;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v13.app.FragmentCompat;
import android.support.v4.content.ContextCompat;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.bioid.authenticator.R;
import com.bioid.authenticator.base.annotations.ConfigurationOrientation;
import com.bioid.authenticator.base.annotations.SurfaceRotation;
import com.bioid.authenticator.base.camera.CameraException;
import com.bioid.authenticator.base.camera.CameraHelper;
import com.bioid.authenticator.base.functional.Consumer;
import com.bioid.authenticator.base.image.IntensityPlane;
import com.bioid.authenticator.base.logging.LoggingHelper;
import com.bioid.authenticator.base.logging.LoggingHelperFactory;
import com.bioid.authenticator.base.network.bioid.webservice.MovementDirection;
import com.bioid.authenticator.base.notification.DialogHelper;
import com.bioid.authenticator.base.opengl.HeadOverlayView.Direction;
import com.bioid.authenticator.databinding.FragmentFacialRecognitionBinding;

import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * The FacialRecognitionFragment uses the front-facing camera to either verify or enroll a user via facial recognition.
 * It is required to call {@link #setPresenter(FacialRecognitionContract.Presenter)} before using the fragment.
 */
public final class FacialRecognitionFragment extends Fragment implements FacialRecognitionContract.View {

    // request code for requestPermissions() and onRequestPermissionsResult()
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 0;

    // make sure the camera will not be opened twice because the user rotates the device while the opening callback is still waiting
    private final Semaphore cameraOpenCloseMutex = new Semaphore(1);

    private final LoggingHelper log = LoggingHelperFactory.create(FacialRecognitionFragment.class);
    private final Random random = new Random();

    private FacialRecognitionContract.Presenter presenter;
    private DialogHelper dialogHelper;
    private CameraHelper cameraHelper;
    private FragmentFacialRecognitionBinding binding;

    private CameraDevice openCamera;
    private CameraCaptureSession openPreviewSession;
    private Size previewSize;
    private ImageReader imageReader;

    /**
     * Setting the Presenter of the fragment.
     */
    public void setPresenter(FacialRecognitionContract.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);  // does retain the presenter to preserve state across configuration changes

        // as mentioned in "The Busy Coder's Guide to Android" it is important to use the app context to get the camera service,
        // because of a memory leak in Android 5.0 (fixed in 5.1)
        CameraManager cameraManager = (CameraManager) getActivity().getApplicationContext().getSystemService(Context.CAMERA_SERVICE);

        dialogHelper = new DialogHelper(getActivity());
        cameraHelper = new CameraHelper(cameraManager);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_facial_recognition, container, false);

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);  // do not rotate during biometric operation

        presenter.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        presenter.onPause();
    }

    @Override
    public void requestCameraPermission() {

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Camera permission is already granted.
            // Threw Play Store installation before Android 6.0 or threw user approval during a previous app usage.
            presenter.onCameraPermissionGranted();
            return;
        }

        // Camera permission is not yet granted, request it now.
        // Because it should be obvious for a facial recognition app to require camera access "shouldShowRequestPermissionRationale()"
        // is not queried.
        FragmentCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                presenter.onCameraPermissionGranted();
            } else {
                presenter.onCameraPermissionDenied();
            }
        }
    }

    @Override
    public void showLoadingIndicator() {
        binding.loadingIndicator.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoadingIndicator() {
        binding.loadingIndicator.setVisibility(View.INVISIBLE);
    }

    @Override
    public void promptForEnrollmentProcessExplanation() {
        dialogHelper.newTransparentDialog(R.string.enrollment_process_explanation_title,
                R.string.enrollment_process_explanation_message,
                R.string.dialog_button_continue,
                new Runnable() {
                    @Override
                    public void run() {
                        presenter.promptForProcessExplanationAccepted();
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        presenter.promptForProcessExplanationRejected();
                    }
                }).show();
    }

    @Override
    public void promptToTurn90Degrees() {
        dialogHelper.newTransparentDialog(R.string.enrollment_process_turn90degrees_title,
                R.string.enrollment_process_turn90degrees_message,
                R.string.dialog_button_continue,
                new Runnable() {
                    @Override
                    public void run() {
                        presenter.promptToTurn90DegreesAccepted();
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        presenter.promptToTurn90DegreesRejected();
                    }
                }).show();
    }

    @Override
    public void showMovementIndicator(@NonNull final MovementDirection direction) {
        binding.headOverlay.show();

        switch (direction) {
            case any:
                lookIntoRandomDirection();
                break;
            case up:
                binding.headOverlay.lookInto(Direction.UP);
                break;
            case down:
                binding.headOverlay.lookInto(Direction.DOWN);
                break;
            case left:
                binding.headOverlay.lookInto(Direction.LEFT);
                break;
            case right:
                binding.headOverlay.lookInto(Direction.RIGHT);
                break;
        }
    }

    private void lookIntoRandomDirection() {
        int direction = random.nextInt(4);  // produces a random int in the range of [0;4[
        switch (direction) {
            case 0:
                binding.headOverlay.lookInto(Direction.LEFT);
                break;
            case 1:
                binding.headOverlay.lookInto(Direction.UP);
                break;
            case 2:
                binding.headOverlay.lookInto(Direction.RIGHT);
                break;
            case 3:
                binding.headOverlay.lookInto(Direction.DOWN);
                break;
        }
    }

    @Override
    public void resetMovementIndicator() {
        binding.headOverlay.show();
        binding.headOverlay.lookInto(Direction.AHEAD);
    }

    @Override
    public void hideMovementIndicator() {
        binding.headOverlay.hide();
    }

    @Override
    public void showMovementInfo(@NonNull MovementDirection direction) {
        showUnobtrusiveMessage(R.string.facial_recognition_move);
    }

    @Override
    public void showInitialisationInfo() {
        showFullScreenMessage(R.string.facial_recognition_init);
    }

    @Override
    public void showFindFaceInfo() {
        showUnobtrusiveMessage(R.string.facial_recognition_find_face);
    }

    @Override
    public void showUploadingImagesInfo() {
        showUnobtrusiveMessage(R.string.facial_recognition_uploading);
    }

    @Override
    public void showVerifyingInfo() {
        showFullScreenMessage(R.string.verification_waiting);
    }

    @Override
    public void showEnrollingInfo() {
        showFullScreenMessage(R.string.enrollment_waiting);
    }

    @Override
    public void showVerificationSuccess() {
        showFullScreenMessage(R.string.verification_success);
    }

    @Override
    public void showEnrollmentSuccess() {
        showFullScreenMessage(R.string.enrollment_success);
    }

    @Override
    public void showNotRecognizedWarning() {
        showFullScreenMessage(R.string.verification_warning_not_recognized);
    }

    @Override
    public void showChallengeResponseWarning() {
        showFullScreenMessage(R.string.verification_warning_challenge_response);
    }

    @Override
    public void showMotionDetectionWarning() {
        showFullScreenMessage(R.string.facial_recognition_warning_motion_detection);
    }

    @Override
    public void showLiveDetectionWarning() {
        showFullScreenMessage(R.string.facial_recognition_warning_live_detection);
    }

    @Override
    public void showNoFaceFoundWarning() {
        showFullScreenMessage(R.string.facial_recognition_warning_no_face_found);
    }

    @Override
    public void showMultipleFacesFoundWarning() {
        showFullScreenMessage(R.string.facial_recognition_warning_multiple_faces_found);
    }

    @Override
    public void showNoSamplesWarning() {
        showFullScreenMessage(R.string.facial_recognition_warning_no_samples);
    }

    private void showUnobtrusiveMessage(@StringRes int message) {
        binding.fullscreenMessage.setVisibility(View.INVISIBLE);

        binding.unobtrusiveMessage.setText(message);
        binding.unobtrusiveMessage.setVisibility(View.VISIBLE);
    }

    private void showFullScreenMessage(@StringRes int message) {
        binding.unobtrusiveMessage.setVisibility(View.INVISIBLE);

        binding.fullscreenMessage.setText(message);
        binding.fullscreenMessage.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideMessages() {
        binding.unobtrusiveMessage.setVisibility(View.INVISIBLE);
        binding.fullscreenMessage.setVisibility(View.INVISIBLE);
    }

    @Override
    public void navigateBack(boolean success) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.setResult(success ? Activity.RESULT_OK : Activity.RESULT_CANCELED);
            activity.finish();
        }
    }

    @Override
    public void showNoConnectionErrorAndNavigateBack() {
        showDialogAndNavigateBack(
                R.string.dialog_title_no_connection,
                R.string.dialog_message_no_connection,
                R.drawable.ic_no_connection);
    }

    @Override
    public void showServerErrorAndNavigateBack() {
        showDialogAndNavigateBack(
                R.string.dialog_title_server_error,
                R.string.dialog_message_server_error,
                R.drawable.ic_server_error);
    }

    @Override
    public void showNoEnrollmentErrorAndNavigateBack() {
        showDialogAndNavigateBack(
                R.string.verification_error_title,
                R.string.verification_error_message_no_enrollment,
                R.drawable.ic_enrollment);
    }

    @Override
    public void showDeviceNotRegisteredErrorAndNavigateBack() {
        showDialogAndNavigateBack(
                R.string.dialog_title_device_not_registered,
                R.string.dialog_message_device_not_registered,
                R.drawable.ic_device);
    }

    @Override
    public void showWrongCredentialsErrorAndNavigateBack() {
        showDialogAndNavigateBack(
                R.string.dialog_title_wrong_credentials,
                R.string.dialog_message_wrong_credentials,
                R.drawable.ic_credentials);
    }

    @Override
    public void showCameraPermissionErrorAndNavigateBack() {
        showDialogAndNavigateBack(
                R.string.dialog_title_missing_permission,
                R.string.dialog_message_missing_camera_permission,
                R.drawable.ic_camera);
    }

    private void showDialogAndNavigateBack(@StringRes int title, @StringRes int message, @DrawableRes int icon) {
        dialogHelper.newDialog(title, message, android.R.string.ok, icon, new Runnable() {
            @Override
            public void run() {
                navigateBack(false);
            }
        }).show();
    }

    @Override
    public void startPreview() {
        // if the screen was turned off and on again the TextureView is already available
        if (binding.preview.isAvailable()) {
            openCameraAndConnectPreview();
        } else {
            // waiting for the TextureView to become available
            binding.preview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, final int width, final int height) {
                    openCameraAndConnectPreview();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                    try {
                        binding.preview.applyImageTransformation(getRelativeDisplayRotation());
                    } catch (CameraException e) {
                        logErrorAndFinish("startPreview failed: %s", e.getMessage());
                    }
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                }
            });
        }
    }

    @Override
    public void stopPreview() {
        withAcquireMutex(new Runnable() {
            @Override
            public void run() {
                cleanup();
                cameraOpenCloseMutex.release();
            }
        });
    }

    /**
     * opens the camera and connects the camera to the preview when the camera is ready
     */
    private void openCameraAndConnectPreview() {
        openCamera(new Runnable() {
            @Override
            public void run() {
                connectPreview();
            }
        });
    }


    /**
     * does open the camera and runs the callback function as soon as the camera is ready
     */
    private void openCamera(@NonNull final Runnable onCameraOpened) {
        withAcquireMutex(new Runnable() {
            @Override
            public void run() {
                try {
                    //noinspection MissingPermission (presenters responsiblity)
                    cameraHelper.openFrontFacingCamera(new CameraDevice.StateCallback() {
                        @Override
                        public void onOpened(@NonNull CameraDevice camera) {
                            openCamera = camera;
                            cameraOpenCloseMutex.release();
                            onCameraOpened.run();
                        }

                        @Override
                        public void onDisconnected(@NonNull CameraDevice camera) {
                            cleanup();
                            cameraOpenCloseMutex.release();
                            logErrorAndFinish("camera %s disconnected", camera.getId());
                        }

                        @Override
                        public void onError(@NonNull CameraDevice camera, int error) {
                            cleanup();
                            cameraOpenCloseMutex.release();
                            logErrorAndFinish("error %d for camera %s occurred", error, camera.getId());
                        }
                    });
                } catch (CameraException e) {
                    // It is always import to free up the camera!
                    // Sticking with the acquired mutex does not matter because the app  will quit anyway.
                    cleanup();
                    logErrorAndFinish("openCamera failed: %s", e.getMessage());
                }
            }
        });
    }

    /**
     * connects a new camera preview session to the UI
     */
    private void connectPreview() {
        try {
            setupPreviewSizeAndImageReader();
            cameraHelper.startCameraPreview(openCamera, binding.preview, previewSize, getDeviceOrientation(),
                    getRelativeDisplayRotation(), imageReader, new Consumer<CameraCaptureSession>() {
                        @Override
                        public void accept(CameraCaptureSession cameraCaptureSession) {
                            openPreviewSession = cameraCaptureSession;
                        }
                    });
        } catch (CameraException e) {
            cleanup();
            logErrorAndFinish("connectPreview failed: %s", e.getMessage());
        }
    }

    /**
     * lazily initialize ImageReader and select preview size
     */
    private void setupPreviewSizeAndImageReader() {
        if (previewSize == null) {
            previewSize = cameraHelper.selectPreviewSize(openCamera);
        }

        if (imageReader == null) {
            int maxImages = 2;  // should be at least 2 according to ImageReader.acquireLatestImage() documentation
            imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.YUV_420_888, maxImages);
            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image img = reader.acquireLatestImage();
                    if (img != null) {

                        // Make a in memory copy of the image to close the image from the reader as soon as possible.
                        // This helps the thread running the preview staying up to date.
                        IntensityPlane imgCopy = IntensityPlane.extract(img);
                        img.close();

                        int imageRotation = cameraHelper.getImageRotation(openCamera, getRelativeDisplayRotation());

                        presenter.onImageCaptured(imgCopy, imageRotation);
                    }
                }
            }, null);
        }
    }

    /**
     * Sometimes accessing the camera is not possible (e.g. the camera driver does perform a disconnect).
     * In these cases the activity will be closed.
     */
    private void logErrorAndFinish(@NonNull String msg, Object... args) {
        log.e(msg, args);

        Activity activity = getActivity();
        if (activity != null) {
            activity.setResult(Activity.RESULT_CANCELED);
            activity.finish();
        }
    }

    /**
     * does free up all resources
     */
    private void cleanup() {
        // It is important to close the CameraCaptureSession before closing the CameraDevice.
        // Not doing so does lead to very slow CameraDevice.close() operations on some devices like for example the Samsung Galaxy S6.
        if (openPreviewSession != null) {
            openPreviewSession.close();
            openPreviewSession = null;
        }
        if (openCamera != null) {
            openCamera.close();
            openCamera = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    /**
     * helper function to run a camera open/close operation while having exclusive access to the cameraOpenCloseMutex
     */
    private void withAcquireMutex(@NonNull final Runnable runnable) {
        try {
            if (!cameraOpenCloseMutex.tryAcquire(3, TimeUnit.SECONDS)) {
                logErrorAndFinish("could not obtain mutex to open/close camera");
            }
            runnable.run();
        } catch (InterruptedException e) {
            logErrorAndFinish("interrupted while trying to open/close camera");
        }
    }


    /**
     * Does return the actual orientation of the device.
     */
    @ConfigurationOrientation
    private int getDeviceOrientation() {
        //noinspection WrongConstant (IntDef annotations are missing on API level 24)
        return getResources().getConfiguration().orientation;
    }

    /**
     * Does return the rotation of the device relative to the native orientation which is usually portrait for phones and landscape
     * for tablets.
     */
    @SurfaceRotation
    private int getRelativeDisplayRotation() {
        return getActivity().getWindowManager().getDefaultDisplay().getRotation();
    }
}
