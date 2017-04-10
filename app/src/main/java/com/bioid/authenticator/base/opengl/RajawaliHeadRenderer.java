package com.bioid.authenticator.base.opengl;

import android.content.Context;
import android.support.annotation.ColorRes;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;

import com.bioid.authenticator.R;

import org.rajawali3d.Object3D;
import org.rajawali3d.animation.Animation3D;
import org.rajawali3d.animation.RotateOnAxisAnimation;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.loader.LoaderOBJ;
import org.rajawali3d.loader.ParsingException;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.renderer.Renderer;

/**
 * Rajawali renderer for the 3D head.
 */
class RajawaliHeadRenderer extends Renderer {

    @ColorRes
    private static final int MODEL_COLOR = R.color.color3DHead;
    @FloatRange(from = 0.0, to = 1.0)
    private static final float MODEL_TRANSPARENCY = 0.85f;

    private Object3D head;
    private Animation3D lastAnimation;

    RajawaliHeadRenderer(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void initScene() {
        getCurrentScene().addLight(setupLight());
        getCurrentCamera().setPosition(0, 0.2, 3.0);

        // preload and hide the model because this is quite expensive
        head = loadModel();
        configureModel(head);
        getCurrentScene().addChild(head);
    }


    @NonNull
    private DirectionalLight setupLight() {
        DirectionalLight light = new DirectionalLight(0, 0.2, -10);
        light.setPower(1.4f);
        return light;
    }

    @NonNull
    private Object3D loadModel() {
        LoaderOBJ objParser = new LoaderOBJ(mContext.getResources(), mTextureManager, R.raw.head_obj);
        try {
            return objParser.parse().getParsedObject();
        } catch (ParsingException e) {
            throw new RuntimeException("could not load 3D head obj-file", e);
        }
    }

    private void configureModel(@NonNull Object3D head) {
        head.setColor(ContextCompat.getColor(getContext(), MODEL_COLOR));
        head.setAlpha(MODEL_TRANSPARENCY);
        head.setVisible(false);
    }

    /**
     * Does add the 3D head to the rendered scene.
     */
    void showModel() {
        if (!getSceneInitialized()) {
            return;
        }

        head.setVisible(true);
    }

    /**
     * Does remove the 3D head from the rendered scene.
     */
    void hideModel() {
        if (!getSceneInitialized()) {
            return;
        }

        stopActiveAnimation();

        head.setVisible(false);
    }

    /**
     * Does rotate the 3D head by the specified angle around the specified axis.
     *
     * @param axis                      for the rotation
     * @param angle                     for the rotation
     * @param animationDurationInMillis how long the animation should take
     */
    @SuppressWarnings("SameParameterValue")
    void rotateModel(@NonNull Vector3.Axis axis, @IntRange(from = 0, to = 360) int angle,
                     @IntRange(from = 0) int animationDurationInMillis) {
        if (!getSceneInitialized()) {
            return;
        }

        stopActiveAnimation();

        Animation3D animation = new RotateOnAxisAnimation(axis, angle);
        animation.setDurationMilliseconds(animationDurationInMillis);
        animation.setTransformable3D(head);
        animation.play();

        getCurrentScene().registerAnimation(animation);
        lastAnimation = animation;
    }

    private void stopActiveAnimation() {
        if (lastAnimation != null && !lastAnimation.isEnded()) {
            lastAnimation.pause();
            lastAnimation = null;
        }
    }

    /**
     * Does reset the model rotation.
     */
    void resetModelRotation() {
        if (!getSceneInitialized()) {
            return;
        }

        head.resetToLookAt();
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {
        // do nothing
    }

    @Override
    public void onTouchEvent(MotionEvent event) {
        // do nothing
    }
}
