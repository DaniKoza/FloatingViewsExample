package com.danikoza.floatingviewslib;

import android.animation.TimeAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class FloatingViews extends View {

    /**
     * Basic constructors
     */
    public FloatingViews(Context context) {
        super(context);
    }

    public FloatingViews(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FloatingViews(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Class representing the state of the floating object
     **/
    private static class myFloatingObject {
        private float x;
        private float y;
        private float scale;
        private float alpha;
        private float speed;
    }

    private static final int BASE_SPEED_DP_PER_S = 200;
    private static final int COUNT = 32;
    private static final int SEED = 1337;

    private TimeAnimator mTimeAnimator;
    private Drawable mDrawable;

    private float mBaseSpeed;
    private float mBaseSize;
    private long mCurrentPlayTime;

    /**
     * The minimum scale of a floating object
     */
    private static final float SCALE_MIN_PART = 0.45f;
    /**
     * How much of the scale that's based on randomness
     */
    private static final float SCALE_RANDOM_PART = 0.55f;
    /**
     * How much of the alpha that's based on the scale of the floating object
     */
    private static final float ALPHA_SCALE_PART = 0.5f;
    /**
     * How much of the alpha that's based on randomness
     */
    private static final float ALPHA_RANDOM_PART = 0.5f;

    private final myFloatingObject[] mFloatingObjects = new myFloatingObject[COUNT];
    private final Random myRand = new Random(SEED);


    /**
     * Initialize the wanted drawable to be floating by randomizing it's position, scale and alpha
     *
     * @param drawableID the floating object's ID.
     */
    public void init(int drawableID) {
        mDrawable = ContextCompat.getDrawable(getContext(), drawableID);
        mBaseSize = Math.max(mDrawable.getIntrinsicWidth(), mDrawable.getIntrinsicHeight()) / 2f;
        mBaseSpeed = BASE_SPEED_DP_PER_S * getResources().getDisplayMetrics().density;
    }


    @Override
    protected void onSizeChanged(int width, int height, int oldW, int oldH) {
        super.onSizeChanged(width, height, oldW, oldH);
        // The starting position is dependent on the size of the view,
        // which is why the model is initialized here, when the view is measured.
        for (int i = 0; i < mFloatingObjects.length; i++) {
            final myFloatingObject floatingObject = new myFloatingObject();
            initializeFloatingObject(floatingObject, width, height);
            mFloatingObjects[i] = floatingObject;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int viewHeight = getHeight();
        for (final myFloatingObject floatingObject : mFloatingObjects) {
            // Ignore the floating object if it's outside of the view bounds
            final float floatingObjectSize = floatingObject.scale * mBaseSize;
            if (floatingObject.y + floatingObjectSize < 0 || floatingObject.y - floatingObjectSize > viewHeight) {
                continue;
            }

            // Save the current canvas state
            final int save = canvas.save();

            // Move the canvas to the center of the floating object
            canvas.translate(floatingObject.x, floatingObject.y);

            // Rotate the canvas based on how far the floating object has moved
            final float progress = (floatingObject.y + floatingObjectSize) / viewHeight;
            canvas.rotate(360 * progress);

            // Prepare the size and alpha of the drawable
            final int size = Math.round(floatingObjectSize);
            mDrawable.setBounds(-size, -size, size, size);
            mDrawable.setAlpha(Math.round(255 * floatingObject.alpha));

            // Draw the floating object to the canvas
            mDrawable.draw(canvas);

            // Restore the canvas to it's previous position and rotation
            canvas.restoreToCount(save);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mTimeAnimator = new TimeAnimator();
        mTimeAnimator.setTimeListener(new TimeAnimator.TimeListener() {
            @Override
            public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
                if (!isLaidOut()) {
                    // Ignore all calls before the view has been measured and laid out.
                    return;
                }
                updateState(deltaTime);
                invalidate();
            }
        });
        mTimeAnimator.start();
    }

    /**
     * Remove all listeners and animations
     **/
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mTimeAnimator.cancel();
        mTimeAnimator.setTimeListener(null);
        mTimeAnimator.removeAllListeners();
        mTimeAnimator = null;
    }

    /**
     * Pause the floating animation if it's running
     */
    public void pause() {
        if (mTimeAnimator != null && mTimeAnimator.isRunning()) {
            // Store the current play time for later.
            mCurrentPlayTime = mTimeAnimator.getCurrentPlayTime();
            mTimeAnimator.pause();
        }
    }

    /**
     * Resume the animation if not already running
     */
    public void resume() {
        if (mTimeAnimator != null && mTimeAnimator.isPaused()) {
            mTimeAnimator.start();
            // By setting the current play time, it will pick of where it left off.
            mTimeAnimator.setCurrentPlayTime(mCurrentPlayTime);
        }
    }

    private void updateState(float deltaMs) {
        // Converting to seconds since PX/S constants are easier to understand
        final float deltaSeconds = deltaMs / 1000f;
        final int viewWidth = getWidth();
        final int viewHeight = getHeight();

        for (final myFloatingObject floatingObject : mFloatingObjects) {
            // Move the floating object based on the elapsed time and it's speed
            floatingObject.y -= floatingObject.speed * deltaSeconds;

            // If the floating object is completely outside of the view bounds after
            // updating it's position, recycle it.
            final float size = floatingObject.scale * mBaseSize;
            if (floatingObject.y + size < 0) {
                initializeFloatingObject(floatingObject, viewWidth, viewHeight);
            }
        }
    }

    /**
     * Initialize the given floating object by randomizing it's position, scale and alpha
     *
     * @param floatingObject the floating object to initialize
     * @param viewWidth      the view width
     * @param viewHeight     the view height
     */
    private void initializeFloatingObject(myFloatingObject floatingObject, int viewWidth, int viewHeight) {
        // Set the scale based on a min value and a random multiplier
        floatingObject.scale = SCALE_MIN_PART + SCALE_RANDOM_PART * myRand.nextFloat();

        // Set X to a random value within the width of the view
        floatingObject.x = viewWidth * myRand.nextFloat();

        // Set the Y position
        // Start at the bottom of the view
        floatingObject.y = viewHeight;
        // The Y value is in the center of the floating object, add the size
        // to make sure it starts outside of the view bound
        floatingObject.y += floatingObject.scale * mBaseSize;
        // Add a random offset to create a small delay before the
        // floating object appears again.
        floatingObject.y += viewHeight * myRand.nextFloat() / 4f;

        // The alpha is determined by the scale of the floating object and a random multiplier.
        floatingObject.alpha = ALPHA_SCALE_PART * floatingObject.scale + ALPHA_RANDOM_PART * myRand.nextFloat();
        // The bigger and brighter a floating object is, the faster it moves
        floatingObject.speed = mBaseSpeed * floatingObject.alpha * floatingObject.scale;
    }
}
