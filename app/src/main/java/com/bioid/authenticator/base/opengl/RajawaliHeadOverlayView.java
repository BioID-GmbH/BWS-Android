package com.bioid.authenticator.base.opengl;

import android.content.Context;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.util.AttributeSet;

import com.bioid.authenticator.base.logging.LoggingHelper;
import com.bioid.authenticator.base.logging.LoggingHelperFactory;

import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.view.SurfaceView;

import java.util.EnumMap;
import java.util.Map;


/**
 * Implementation of the {@link HeadOverlayView} interface using the Rajawali library.
 */
public class RajawaliHeadOverlayView extends SurfaceView implements HeadOverlayView {

    private static final LoggingHelper LOG = LoggingHelperFactory.create(RajawaliHeadOverlayView.class);
    private static final String STOPWATCH_SESSION_ID = "initial rendering of 3D head";

    /**
     * How many degrees the head does look to the left, right, up or down.
     */
    @IntRange(from = 0, to = 90)
    private static final int ANGLE_HORIZONTAL = 20;
    private static final int ANGLE_VERTICAL = 20;

    private static final Map<Direction, Vector3.Axis> DIRECTION_TO_AXIS = new EnumMap<Direction, Vector3.Axis>(Direction.class) {
        {
            put(Direction.LEFT, Vector3.Axis.Y);
            put(Direction.RIGHT, Vector3.Axis.Y);
            put(Direction.UP, Vector3.Axis.X);
            put(Direction.DOWN, Vector3.Axis.X);
        }
    };

    private static final Map<Direction, Integer> DIRECTION_TO_ANGLE = new EnumMap<Direction, Integer>(Direction.class) {
        {
            put(Direction.AHEAD, 0);
            put(Direction.LEFT, -ANGLE_HORIZONTAL);
            put(Direction.RIGHT, ANGLE_HORIZONTAL);
            put(Direction.UP, ANGLE_VERTICAL);
            put(Direction.DOWN, -ANGLE_VERTICAL);
        }
    };

    @NonNull
    private final RajawaliHeadRenderer renderer;

    @NonNull
    private Direction currentDirection = Direction.AHEAD;

    public RajawaliHeadOverlayView(Context context) {
        this(context, null);
    }

    public RajawaliHeadOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (isInEditMode()) {
            // Rajawali renderer does not work in preview
            //noinspection ConstantConditions
            renderer = null;
            return;
        }

        LOG.startStopwatch(STOPWATCH_SESSION_ID);

        // make sure the view can be used as an overlay
        setTransparent(true);

        // setup renderer
        renderer = new RajawaliHeadRenderer(context);
        setSurfaceRenderer(renderer);

        LOG.stopStopwatch(STOPWATCH_SESSION_ID);
    }

    @Override
    public void show() {
        renderer.showModel();
    }

    @Override
    public void hide() {
        renderer.hideModel();
    }

    @Override
    public void reset() {
        renderer.resetModelRotation();
        currentDirection = Direction.AHEAD;
    }

    @Override
    public void lookInto(@NonNull Direction targetDirection) {
        LOG.d("lookInto(%s) [currentDirection=%s]", targetDirection, currentDirection);

        if (isDiagonalAnimation(targetDirection) || renderer.isAnimationRunning()) {
            reset();  // diagonal or multiple animations are not supported -> reset and move to target direction
        }

        if (currentDirection == targetDirection) {
            return;  // nothing to do
        }

        Vector3.Axis axis = currentDirection == Direction.AHEAD ?
                DIRECTION_TO_AXIS.get(targetDirection) :
                DIRECTION_TO_AXIS.get(currentDirection);

        int angle = DIRECTION_TO_ANGLE.get(targetDirection) - DIRECTION_TO_ANGLE.get(currentDirection);

        renderer.rotateModel(axis, angle, HeadOverlayView.ANIMATION_DURATION_IN_MILLIS);

        currentDirection = targetDirection;
    }

    private boolean isDiagonalAnimation(@NonNull Direction targetDirection) {
        if (currentDirection == Direction.AHEAD || targetDirection == Direction.AHEAD) {
            return false;
        }

        Vector3.Axis axisForCurrentDirection = DIRECTION_TO_AXIS.get(currentDirection);
        Vector3.Axis axisForTargetDirection = DIRECTION_TO_AXIS.get(targetDirection);

        return axisForCurrentDirection != axisForTargetDirection;
    }
}
