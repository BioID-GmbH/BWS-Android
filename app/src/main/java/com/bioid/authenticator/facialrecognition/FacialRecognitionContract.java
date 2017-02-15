package com.bioid.authenticator.facialrecognition;

import android.support.annotation.NonNull;

import com.bioid.authenticator.base.annotations.Rotation;
import com.bioid.authenticator.base.image.Yuv420Image;
import com.bioid.authenticator.base.mvp.LifecycleAware;
import com.bioid.authenticator.base.network.bioid.webservice.MovementDirection;

/**
 * Contract between the View and the Presenter using the MVP pattern.
 */
@SuppressWarnings("unused")
public interface FacialRecognitionContract {

    interface View {

        /**
         * Tries to obtain the permission for using the camera.
         */
        void requestCameraPermission();

        /**
         * Show a indeterminate loading indicator.
         */
        void showLoadingIndicator();

        /**
         * Hide loading indicator.
         */
        void hideLoadingIndicator();

        /**
         * Prompt to explain how the enrollment process will work.
         */
        void promptForEnrollmentProcessExplanation();

        /**
         * Prompt the user to turn himself 90 degrees to the right.
         */
        void promptToTurn90Degrees();

        /**
         * Show a info message instructing the user to look in a certain direction.
         */
        void showMovementInfo(@NonNull MovementDirection direction);

        /**
         * Show initialisation info message.
         */
        void showInitialisationInfo();

        /**
         * Show finding face info message.
         */
        void showFindFaceInfo();

        /**
         * Show uploading images info message.
         */
        void showUploadingImagesInfo();

        /**
         * Show verifying info message.
         */
        void showVerifyingInfo();

        /**
         * Show enrolling info message.
         */
        void showEnrollingInfo();

        /**
         * Show a success message indicating that the user verification succeeded.
         */
        void showVerificationSuccess();

        /**
         * Show a success message indicating that the user enrollment succeeded.
         */
        void showEnrollmentSuccess();

        /**
         * Show a warning message indicating that the user has not been recognized.
         */
        void showNotRecognizedWarning();

        /**
         * Show a warning message indicating that the images do not fulfill the challenge-response criteria.
         */
        void showChallengeResponseWarning();

        /**
         * Show a warning message indicating that no motion was detected.
         */
        void showMotionDetectionWarning();

        /**
         * Show a warning message indicating that the images do not prove that they are recorded from a live person.
         */
        void showLiveDetectionWarning();

        /**
         * Show a warning message indicating that no face could be found.
         */
        void showNoFaceFoundWarning();

        /**
         * Show a warning message indicating that multiple faces where found.
         */
        void showMultipleFacesFoundWarning();

        /**
         * Hides all info, warning or success messages.
         */
        void hideMessages();

        /**
         * Navigate back to the previous activity.
         *
         * @param success Used in the activity result to express if the operation has succeeded.
         */
        void navigateBack(boolean success);

        /**
         * Show a error message indicating that no connection could be established and navigate back to the previous activity.
         * The activity result will express that the operation has not succeeded.
         */
        void showNoConnectionErrorAndNavigateBack();

        /**
         * Show a error message indicating that the server could not process the request and navigate back to the previous activity.
         * The activity result will express that the operation has not succeeded.
         */
        void showServerErrorAndNavigateBack();

        /**
         * Show a error message indicating that the user has not been enrolled and navigate back to the previous activity.
         * The activity result will express that the operation has not succeeded.
         */
        void showNoEnrollmentErrorAndNavigateBack();

        /**
         * Show a error message indicating that the provided credentials are invalid and navigate back to the previous activity.
         * The activity result will express that the operation has not succeeded.
         */
        void showWrongCredentialsErrorAndNavigateBack();

        /**
         * Show a error message indicating that the camera permission is missing and navigates back to the previous activity.
         * The activity result will express that the operation has not succeeded.
         */
        void showCameraPermissionErrorAndNavigateBack();

        /**
         * Starts the camera preview.
         */
        void startPreview();

        /**
         * Stops the camera preview.
         */
        void stopPreview();
    }

    interface Presenter extends LifecycleAware {

        /**
         * Callback which is called if the requested camera permission was granted.
         */
        void onCameraPermissionGranted();

        /**
         * Callback which is called if the requested camera permission was denied.
         */
        void onCameraPermissionDenied();

        /**
         * Callback which is called if the user wants to continue after being prompted to explain how the process works.
         */
        void promptForProcessExplanationAccepted();

        /**
         * Callback which is called if the user does not want to continue after being prompted to explain how the process works.
         */
        void promptForProcessExplanationRejected();

        /**
         * Callback which is called if the user wants to continue after being prompted to turn 90 degrees to the right.
         */
        void promptToTurn90DegreesAccepted();

        /**
         * Callback which is called if the user does not want to continue after being prompted to turn 90 degrees to the right.
         */
        void promptToTurn90DegreesRejected();

        /**
         * Callback which is called if an image was captured.
         *
         * @param img         image within the YUV_420_888 format
         * @param imgRotation the rotation of the image
         */
        void onImageCaptured(@NonNull Yuv420Image img, @Rotation int imgRotation);
    }
}
