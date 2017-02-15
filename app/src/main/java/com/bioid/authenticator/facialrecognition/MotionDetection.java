package com.bioid.authenticator.facialrecognition;

import android.support.annotation.NonNull;

import com.bioid.authenticator.base.image.GrayscaleImage;
import com.bioid.authenticator.base.logging.LoggingHelper;
import com.bioid.authenticator.base.logging.LoggingHelperFactory;

/**
 * Contains simple algorithm for motion detection.
 * <p/>
 * Because this algorithm is shared across multiple platforms it should not be modified!
 * Therefore all inspections are disabled and no tests do exist.
 */
@SuppressWarnings("ConstantConditions")
class MotionDetection {

    private static final String STOPWATCH_SESSION_ID = "motion detection algorithm";
    private static final int THRESHOLD = 18;
    private static final int NOISE_INTENSITY_LEVEL = 36;

    private final LoggingHelper log = LoggingHelperFactory.create(MotionDetection.class);

    /**
     * Can detect if a change in position did happen.
     *
     * @param first   the first image of a series which is used as a baseline
     * @param current the image which might contain a change in position as compared with the first image
     * @return true if motion was detected
     */
    boolean detect(@NonNull GrayscaleImage first, @NonNull GrayscaleImage current) {
        log.startStopwatch(STOPWATCH_SESSION_ID);

        boolean trigger = false;

        int difference;
        int width = first.width;
        int height = first.height;
        int width2 = current.width;
        int height2 = current.height;

        if (width != width2 || height != height2) {
            return trigger;
        }

        // Calculate simple difference of the two images - concentrating on the centre
        int differenceSum = 0;

        // Moving objects in the background often disturb automatic motion detection, so we will concentrate on the middle of the images
        // We will cut out the inner half difference image, i.e. we want to get rid of 1/4th margin on all sides
        int differenceImageWidth = width / 2;
        int differenceImageHeight = height / 2;

        int[] differenceImage = new int[differenceImageWidth * differenceImageHeight];

        int numberOfPixels = 0;

        // We leave 1/4 margin to top and bottom
        int verticalStart = first.height / 4;
        int verticalEnd = 3 * first.height / 4;
        // And 1/4 margin to left and right
        int horizontalStart = first.width / 4;
        int horizontalEnd = 3 * first.width / 4;

        for (int y = verticalStart; y < verticalEnd; y++) {
            int positionCounter = (y * width) + horizontalStart;
            for (int x = horizontalStart; x < horizontalEnd; x++) {
                // Use the absolute difference of source and target pixel intensity as a motion measurement
                difference = Math.abs((first.data[positionCounter] & 0xff) - (current.data[positionCounter] & 0xff));
                differenceSum += difference;
                differenceImage[numberOfPixels++] = difference;
                positionCounter++;
            }
        }

        // Mean difference: Divide by ROI
        int meanDiff = (int) (((double) differenceSum / (double) numberOfPixels) + 0.5);
        // Mean Difference: Never lower than noise level
        meanDiff = (meanDiff < NOISE_INTENSITY_LEVEL) ? NOISE_INTENSITY_LEVEL : meanDiff;

        // We want to roughly calculate the bounding moving box
        int movingAreaX1 = 0;
        int movingAreaX2 = 0;
        int movingAreaY1 = 0;
        int movingAreaY2 = 0;

        // This will count all pixels that changed within the moving area
        int changedPixels = 0;

        // This is the main loop to determine a human's head bounding box
        // Basically, we start from top to bottom,
        // then try to find the horizontal coordinates of the head width,
        // and stop before the shoulder area would enlarge that box again
        int posCount = 0;
        for (int y = 1; y < differenceImageHeight; y++) {
            for (int x = 0; x < differenceImageWidth; x++) {
                difference = differenceImage[posCount++];

                // For moving area detection, we only count differences higher than normal noise
                if (difference > meanDiff) {
                    // The first movement pixel will determine the starting coordinates
                    if (movingAreaY1 == 0) {
                        // This is typically the top head position
                        movingAreaX1 = x;
                        movingAreaY1 = y;
                        movingAreaX2 = x;
                        movingAreaY2 = y;
                    }

                    // We do not want to get into the shoulder area
                    if (y < 3 * differenceImageHeight / 5) {
                        if (x < movingAreaX1) {
                            // New left coordinate of bounding box
                            movingAreaX1 = x;
                        } else if (x > movingAreaX2) {
                            // New right coordinate of bounding box
                            movingAreaX2 = x;
                        }
                    }

                    // We assume here that the vertical height of a human head is not exceeding 1.33 times the head width
                    if ((y >= movingAreaY2) && (y - movingAreaY1 < 4 * (movingAreaX2 - movingAreaX1) / 3)) {
                        // Only if the condition above is true we will use this lower vertical coordinate
                        // This avoids expanding the bounding box to the bottom of the screen
                        movingAreaY2 = y;

                        // If the current location is within this calculated area, then we have a new movement within the head area
                        if ((x >= movingAreaX1) && (x <= movingAreaX2)) {
                            changedPixels++;
                        }
                    }
                }
            }
        }

        // Calculate area of moving object
        int movingAreaWidth = movingAreaX2 - movingAreaX1 + 1;
        int movingAreaHeight = movingAreaY2 - movingAreaY1 + 1;

        // Was there any suitable movement at all?
        if ((movingAreaWidth <= 0) || (movingAreaHeight <= 0)) {
            movingAreaWidth = 1;
            movingAreaHeight = 1;
            changedPixels = 0;
        }

        // Moving area difference: Calculate changes according to size
        double movementDiff = (double) changedPixels / (double) (movingAreaWidth * movingAreaHeight);

        // movementDiff now holds the percentage of moving pixels within the moving area. Let's bring that to [0.0; 100.0]
        movementDiff *= 100.0;
        log.d("moving pixels within moving area: %.2f%%", movementDiff);

        // If moving area is big enough to be a human face, then let's trigger
        // We accept only faces that are at least 1/20th of the image dimensions
        if ((movingAreaWidth > first.width / 20) && (movingAreaHeight > first.height / 20)) {
            // Trigger if movementDiff is above threshold (default: when 10% of face bounding box pixels changed)
            trigger = movementDiff > THRESHOLD;
        }

        log.stopStopwatch(STOPWATCH_SESSION_ID);
        return trigger;
    }
}
