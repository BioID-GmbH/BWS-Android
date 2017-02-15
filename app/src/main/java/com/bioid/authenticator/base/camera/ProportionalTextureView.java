package com.bioid.authenticator.base.camera;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.bioid.authenticator.base.annotations.SurfaceRotation;

/**
 * The ProportionalTextureView preserves the aspect ratio of the displayed preview images to avoid unnecessary skewing.
 * Preview images will be cropped to use as much screen size for the preview as possible.
 */
public class ProportionalTextureView extends TextureView {

    private Size previewSize = null;

    public ProportionalTextureView(Context context) {
        super(context);
    }

    public ProportionalTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProportionalTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Does make sure the TextureView is proportionally aligned with the given size of the preview images.
     * Should be called if the preview images which should be displayed do change in size.
     */
    public void setAspectRatio(int previewWidth, int previewHeight, @SurfaceRotation int relativeDisplayRotation) {
        this.previewSize = new Size(previewWidth, previewHeight);

        applyImageTransformation(relativeDisplayRotation);
        requestLayout();
    }

    /**
     * Does make sure the TextureView is proportionally aligned with the given size of the preview images.
     * Should be called if the view does change in size.
     */
    public void applyImageTransformation(@SurfaceRotation int relativeDisplayRotation) {
        // do not perform any adjustments if the aspect ratio of the preview images is not set
        if (previewSize == null) {
            return;
        }

        Matrix matrix = new Matrix();

        int viewWidth = getWidth();
        int viewHeight = getHeight();

        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF previewRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());

        switch (relativeDisplayRotation) {

            case Surface.ROTATION_0:
                // no transformation needed
                break;

            case Surface.ROTATION_180:
                matrix.postRotate(180, viewRect.centerX(), viewRect.centerY());
                break;

            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                previewRect.offset(viewRect.centerX() - previewRect.centerX(), viewRect.centerY() - previewRect.centerY());
                matrix.setRectToRect(viewRect, previewRect, Matrix.ScaleToFit.FILL);

                float scale = Math.max(
                        (float) viewHeight / previewSize.getHeight(),
                        (float) viewWidth / previewSize.getWidth());
                matrix.postScale(scale, scale, viewRect.centerX(), viewRect.centerY());

                matrix.postRotate(90 * (relativeDisplayRotation - 2), viewRect.centerX(), viewRect.centerY());
                break;
        }

        setTransform(matrix);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        int viewHeight = MeasureSpec.getSize(heightMeasureSpec);

        // do not perform any adjustments if the aspect ratio of the preview images is not set
        if (previewSize == null) {
            setMeasuredDimension(viewWidth, viewHeight);
            return;
        }

        if (viewWidth > viewHeight * previewSize.getWidth() / previewSize.getHeight()) {
            setMeasuredDimension(viewWidth, viewWidth * previewSize.getHeight() / previewSize.getWidth());
        } else {
            setMeasuredDimension(viewHeight * previewSize.getWidth() / previewSize.getHeight(), viewHeight);
        }
    }
}
