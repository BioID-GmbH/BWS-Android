package com.bioid.authenticator.base.opengl;

import android.support.annotation.NonNull;

/**
 * Custom view which does display a 3D head.
 * The view is transparent and can be used as an overlay over other views.
 * <p>
 * Implementations must use the {@link #ANIMATION_DURATION_IN_MILLIS} constant.
 * Implementations must not implement diagonal movements.
 */
@SuppressWarnings("unused")
public interface HeadOverlayView {

    /**
     * Specifies how long a movement animation should take.
     */
    int ANIMATION_DURATION_IN_MILLIS = 1_000;

    /**
     * Direction in which the 3D head looks.
     * Meaning for example left from the heads point of view, the user does look to the right from his point of view.
     */
    enum Direction {
        AHEAD,
        LEFT,
        RIGHT,
        UP,
        DOWN
    }

    /**
     * Makes the 3D head visible.
     */
    void show();

    /**
     * Does hide the 3D head.
     */
    void hide();

    /**
     * Does reset the view state which means the 3D head looks straight ahead.
     */
    void reset();

    /**
     * Starts an animation to let the head look in the specified direction.
     *
     * @throws IllegalStateException if a diagonal movement was triggered
     */
    void lookInto(@NonNull Direction direction);
}
