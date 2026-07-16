package com.scrollstop.app.overlay;

import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.scrollstop.app.R;
import com.scrollstop.app.data.ScrollCounterState;

public class FloatingOverlayManager implements ScrollCounterState.StateListener {

    private final Context context;
    private final WindowManager windowManager;
    private View overlayView;
    private WindowManager.LayoutParams wmParams;

    // Drag states
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;

    // View references
    private View pillView;
    private TextView textIgCount;
    private TextView textYtCount;
    private View btnResetOverlay;
    private ImageView imgIg;
    private ImageView imgYt;

    // Animation states
    private AnimatorSet pulseAnimator;
    private int currentBgColor = Color.parseColor("#CC1A1A2E");
    private ValueAnimator colorAnimator;

    public FloatingOverlayManager(Context context) {
        this.context = context.getApplicationContext();
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public void show() {
        if (overlayView != null) return; // Already showing

        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            // Deprecated but needed for pre-O support
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        wmParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        wmParams.gravity = Gravity.TOP | Gravity.START;
        wmParams.x = 50;
        wmParams.y = 200;

        overlayView = LayoutInflater.from(context).inflate(R.layout.floating_overlay, null);

        // Bind views
        pillView = overlayView.findViewById(R.id.overlay_pill);
        textIgCount = overlayView.findViewById(R.id.text_ig_count);
        textYtCount = overlayView.findViewById(R.id.text_yt_count);
        btnResetOverlay = overlayView.findViewById(R.id.btn_reset_overlay);
        imgIg = overlayView.findViewById(R.id.img_ig);
        imgYt = overlayView.findViewById(R.id.img_yt);

        // Colors setup
        imgIg.setImageTintList(ColorStateList.valueOf(Color.parseColor("#E91E8C"))); // Instagram Pink
        imgYt.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FF0000"))); // YouTube Red

        // Drag/Move touch listener
        overlayView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (wmParams == null) return false;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = wmParams.x;
                        initialY = wmParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return false; // return false to let child clicks work

                    case MotionEvent.ACTION_MOVE:
                        wmParams.x = initialX + Math.round(event.getRawX() - initialTouchX);
                        wmParams.y = initialY + Math.round(event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(overlayView, wmParams);
                        return false;
                }
                return false;
            }
        });

        // Reset button listener
        btnResetOverlay.setOnClickListener(v -> ScrollCounterState.resetAll(context));

        // Register to state changes
        ScrollCounterState.addListener(this);

        // Initial UI update
        updateUi();

        // Add view to WindowManager
        windowManager.addView(overlayView, wmParams);
    }

    public void hide() {
        if (overlayView != null) {
            ScrollCounterState.removeListener(this);
            if (pulseAnimator != null) {
                pulseAnimator.cancel();
                pulseAnimator = null;
            }
            if (colorAnimator != null) {
                colorAnimator.cancel();
                colorAnimator = null;
            }
            windowManager.removeView(overlayView);
            overlayView = null;
            wmParams = null;
        }
    }

    @Override
    public void onStateChanged() {
        updateUi();
    }

    private void updateUi() {
        if (overlayView == null) return;

        int igCount = ScrollCounterState.getIgReelsCount();
        int ytCount = ScrollCounterState.getYtShortsCount();
        boolean isAlert = ScrollCounterState.isAnyAlertActive();

        textIgCount.setText(String.valueOf(igCount));
        textYtCount.setText(String.valueOf(ytCount));

        // Colors
        int targetBgColor = isAlert ? Color.parseColor("#FF5252") : Color.parseColor("#CC1A1A2E");
        int textTextColor = isAlert ? Color.WHITE : Color.parseColor("#B0BEC5");

        textIgCount.setTextColor(textTextColor);
        textYtCount.setTextColor(textTextColor);

        // Highlight over-limit state with extra bold text style
        textIgCount.setTypeface(null, ScrollCounterState.isIgAlertActive() ? android.graphics.Typeface.BOLD_ITALIC : android.graphics.Typeface.BOLD);
        textYtCount.setTypeface(null, ScrollCounterState.isYtAlertActive() ? android.graphics.Typeface.BOLD_ITALIC : android.graphics.Typeface.BOLD);

        // Animate background color smoothly if color changes
        if (currentBgColor != targetBgColor) {
            if (colorAnimator != null) {
                colorAnimator.cancel();
            }
            colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), currentBgColor, targetBgColor);
            colorAnimator.setDuration(600);
            colorAnimator.addUpdateListener(animator -> {
                int color = (int) animator.getAnimatedValue();
                currentBgColor = color;
                if (pillView != null) {
                    pillView.setBackgroundTintList(ColorStateList.valueOf(color));
                }
            });
            colorAnimator.start();
        } else {
            pillView.setBackgroundTintList(ColorStateList.valueOf(currentBgColor));
        }

        // Pulse scale animation on alert
        if (isAlert) {
            if (pulseAnimator == null) {
                ObjectAnimator scaleX = ObjectAnimator.ofFloat(pillView, "scaleX", 1f, 1.06f);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(pillView, "scaleY", 1f, 1.06f);
                scaleX.setRepeatCount(ValueAnimator.INFINITE);
                scaleX.setRepeatMode(ValueAnimator.REVERSE);
                scaleY.setRepeatCount(ValueAnimator.INFINITE);
                scaleY.setRepeatMode(ValueAnimator.REVERSE);

                pulseAnimator = new AnimatorSet();
                pulseAnimator.playTogether(scaleX, scaleY);
                pulseAnimator.setDuration(600);
                pulseAnimator.start();
            }
        } else {
            if (pulseAnimator != null) {
                pulseAnimator.cancel();
                pulseAnimator = null;
                pillView.setScaleX(1f);
                pillView.setScaleY(1f);
            }
        }
    }
}
