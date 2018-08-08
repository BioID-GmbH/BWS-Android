package com.bioid.authenticator.facialrecognition;

import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.bioid.authenticator.base.image.GrayscaleImage;
import com.bioid.authenticator.base.image.Yuv420Image;
import com.bioid.authenticator.base.logging.LoggingHelper;
import com.bioid.authenticator.base.logging.LoggingHelperFactory;


/**
 * Contains stateful algorithm for motion detection.
 * <p/>
 * Because this algorithm is shared across multiple platforms it should not be modified!
 * Therefore all inspections are disabled and no tests do exist.
 */
@SuppressWarnings("ConstantConditions")
public class MotionDetection {

    private static final int MIN_MOVEMENT_PERCENTAGE = 15;

    private final LoggingHelper log = LoggingHelperFactory.create(MotionDetection.class);

    // Template for motion detection
    private int templateWidth;
    private int templateHeight;
    private int templateXpos;
    private int templateYpos;
    private int resizeCenterX;
    private int resizeCenterY;
    private int[] templateBuffer;

    /**
     * Cut out the template that is used by the motion detection.
     *
     * @param first the image which is used for the template matching
     */
    @WorkerThread
    void createTemplate(@NonNull Yuv420Image first) {

        String stopwatchSessionId = log.startStopwatch("creating template for motion detection");

        GrayscaleImage resizedGrayImage = first.asDownscaledGrayscaleImage();

        resizeCenterX = resizedGrayImage.width / 2;
        resizeCenterY = resizedGrayImage.height / 2;

        if (resizedGrayImage.width > resizedGrayImage.height) {
            // Landscape mode
            templateWidth = resizedGrayImage.width / 10;
            templateHeight = resizedGrayImage.height / 3;
        } else {
            // Portrait mode
            templateWidth = resizedGrayImage.width / 10 * 4 / 3;
            templateHeight = resizedGrayImage.height / 4;
        }

        templateXpos = resizeCenterX - templateWidth / 2;
        templateYpos = resizeCenterY - templateHeight / 2;

        templateBuffer = new int[templateWidth * templateHeight];

        int counter = 0;
        for (int y = templateYpos; y < templateYpos + templateHeight; y++) {
            int offset = y * resizedGrayImage.width;
            for (int x = templateXpos; x < templateXpos + templateWidth; x++) {
                int templatePixel = resizedGrayImage.data[x + offset] & 0xff;
                templateBuffer[counter++] = templatePixel;
            }
        }

        log.stopStopwatch(stopwatchSessionId);
    }

    /**
     * Does remove the currently stored template.
     */
    @AnyThread
    void resetTemplate() {
        templateWidth = 0;
        templateHeight = 0;
        templateXpos = 0;
        templateYpos = 0;
        resizeCenterX = 0;
        resizeCenterY = 0;
        templateBuffer = null;
    }

    /**
     * Can detect if a change in position did happen.
     * This algorithm is basically called: "Template Matching" - we use the normalized cross correlation to be independent of lighting images.
     * We calculate the correlation of template and image over whole image area.
     *
     * @param current the image which might contain a change in position as compared with the first image
     * @return true if motion was detected
     * @throws IllegalStateException if {@link #createTemplate(Yuv420Image)} was not called
     */
    @WorkerThread
    boolean detect(@NonNull Yuv420Image current) {
        if (templateBuffer == null) {
            throw new IllegalStateException("missing template");
        }

        String stopwatchSessionId = log.startStopwatch("motion detection algorithm");

        GrayscaleImage resizedGrayImage = current.asDownscaledGrayscaleImage();

        int bestHitX = 0;
        int bestHitY = 0;
        double maxCorr = 0.0;
        boolean triggered = false;

        int searchWidth = resizedGrayImage.width / 4;
        int searchHeight = resizedGrayImage.height / 4;

        for (int y = resizeCenterY - searchHeight; y <= resizeCenterY + searchHeight - templateHeight; y++) {
            for (int x = resizeCenterX - searchWidth; x <= resizeCenterX + searchWidth - templateWidth; x++) {
                int nominator = 0;
                int denominator = 0;
                int templateIndex = 0;

                // Calculate the normalized cross-correlation coefficient for this position
                for (int ty = 0; ty < templateHeight; ty++) {
                    int bufferIndex = x + (y + ty) * resizedGrayImage.width;
                    for (int tx = 0; tx < templateWidth; tx++) {
                        int imagePixel = resizedGrayImage.data[bufferIndex++] & 0xff;
                        nominator += templateBuffer[templateIndex++] * imagePixel;
                        denominator += imagePixel * imagePixel;
                    }
                }

                // The NCC coefficient is then (watch out for division-by-zero errors for pure black images)
                double ncc = 0.0;
                if (denominator > 0) {
                    ncc = (double) nominator * (double) nominator / (double) denominator;
                }
                // Is it higher that what we had before?
                if (ncc > maxCorr) {
                    maxCorr = ncc;
                    bestHitX = x;
                    bestHitY = y;
                }
            }
        }

        // Now the most similar position of the template is (bestHitX, bestHitY). Calculate the difference from the origin
        int distX = bestHitX - templateXpos;
        int distY = bestHitY - templateYpos;
        double movementDiff = Math.sqrt(distX * distX + distY * distY);

        // The maximum movement possible is a complete shift into one of the corners, i.e.
        int maxDistX = searchWidth - templateWidth / 2;
        int maxDistY = searchHeight - templateHeight / 2;
        double maximumMovement = Math.sqrt((double) maxDistX * maxDistX + (double) maxDistY * maxDistY);

        // The percentage of the detected movement is therefore
        double movementPercentage = movementDiff / maximumMovement * 100.0;

        if (movementPercentage > 100.0) {
            movementPercentage = 100.0;
        }

        log.d("detected motion of %.2f%%", movementPercentage);

        // Trigger if movementPercentage is above threshold (default: when 15% of the maximum movement is exceeded)
        if (movementPercentage > MIN_MOVEMENT_PERCENTAGE) {
            triggered = true;
        }

        log.stopStopwatch(stopwatchSessionId);
        return triggered;
    }
}
