package com.bioid.authenticator.facialrecognition;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import com.bioid.authenticator.base.annotations.FrameRotation;
import com.bioid.authenticator.base.annotations.Rotation;
import com.bioid.authenticator.base.logging.LoggingHelper;
import com.bioid.authenticator.base.logging.LoggingHelperFactory;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

/**
 * Uses the Google Mobile Vision API (included within Play Services) for face detection.
 * <p>
 * A instance of this class should be reused for multiple images.
 */
class FaceDetection {

    private static final String STOPWATCH_SESSION_ID = "face detection algorithm";

    private final LoggingHelper log = LoggingHelperFactory.create(FaceDetection.class);
    private final FaceDetector detector;

    FaceDetection(Context ctx) {
        this.detector = new FaceDetector.Builder(ctx)
                .setProminentFaceOnly(true)  // only detect large face that is most central within the frame
                .setTrackingEnabled(false)
                .build();
    }

    /**
     * Does return true if the detector is operational and can be used.
     * <p>
     * If this method returns false all other methods will throw a {@link NotOperationalException}.
     */
    boolean isOperational() {
        return detector.isOperational();
    }

    /**
     * Can detect if the image contains a human face.
     *
     * @param img      image which might contain a human face
     * @param rotation rotation of the image
     * @return true if the image contains at least one prominent face
     * @throws NotOperationalException if the binaries needed for Google Mobile Vision API are not downloaded yet
     */
    boolean containsFace(@NonNull Bitmap img, @Rotation int rotation) {
        if (!detector.isOperational()) {
            throw new NotOperationalException();
        }

        log.startStopwatch(STOPWATCH_SESSION_ID);
        int faceCount = getFaceCount(img, rotation);
        log.stopStopwatch(STOPWATCH_SESSION_ID);

        return faceCount > 0;
    }

    private int getFaceCount(@NonNull Bitmap img, @Rotation int rotation) {
        Frame frame = new Frame.Builder()
                .setBitmap(img)
                .setRotation(toFrameRotation(rotation))
                .build();

        SparseArray<Face> faces = detector.detect(frame);
        log.d("%d faces detected within image %s", faces.size(), img);

        return faces.size();
    }

    @FrameRotation
    private int toFrameRotation(@Rotation int rotation) {
        //noinspection WrongConstant
        return rotation / 90;
    }

    /**
     * Will be thrown if the binaries needed for Google Mobile Vision API are not downloaded yet.
     */
    @SuppressWarnings("WeakerAccess")
    static class NotOperationalException extends RuntimeException {
    }
}
