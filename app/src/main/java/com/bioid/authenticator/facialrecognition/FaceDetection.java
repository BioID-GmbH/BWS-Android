package com.bioid.authenticator.facialrecognition;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.SparseArray;

import com.bioid.authenticator.base.image.Yuv420Image;
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
    @AnyThread
    boolean isOperational() {
        return detector.isOperational();
    }

    /**
     * Can detect if the image contains a human face.
     *
     * @param img which might contain a human face
     * @return true if the image contains at least one prominent face
     * @throws NotOperationalException if the binaries needed for Google Mobile Vision API are not downloaded yet
     */
    @WorkerThread
    boolean containsFace(@NonNull Yuv420Image img) {
        if (!detector.isOperational()) {
            throw new NotOperationalException();
        }

        log.startStopwatch(STOPWATCH_SESSION_ID);
        int faceCount = getFaceCount(img.asBitmap());
        log.stopStopwatch(STOPWATCH_SESSION_ID);

        return faceCount > 0;
    }

    private int getFaceCount(@NonNull Bitmap img) {
        Frame frame = new Frame.Builder()
                .setBitmap(img)
                .build();

        SparseArray<Face> faces = detector.detect(frame);
        log.d("%d faces detected within image %s", faces.size(), img);

        return faces.size();
    }

    /**
     * Will be thrown if the binaries needed for Google Mobile Vision API are not downloaded yet.
     */
    @SuppressWarnings("WeakerAccess")
    static class NotOperationalException extends RuntimeException {
    }
}
