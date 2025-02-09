package com.example.appdev.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.appdev.R;

public class LoadingDotsView extends LinearLayout {
    private TextView[] dots;
    private int currentDotIndex = 0;
    private Handler dotsHandler = new Handler();
    private Runnable dotsAnimation;
    private int dotColor = 0xFF2196F3; // Default blue color
    private float dotSize = 48f; // Default size
    private int animationDuration = 400; // Default duration
    private int animationDelay = 300; // Default delay between dots
    private float scaleMultiplier = 3.0f; // Default scale multiplier

    public LoadingDotsView(Context context) {
        super(context);
        init(null);
    }

    public LoadingDotsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public LoadingDotsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.LoadingDotsView);
            dotColor = a.getColor(R.styleable.LoadingDotsView_dotColor, dotColor);
            dotSize = a.getDimension(R.styleable.LoadingDotsView_dotSize, dotSize);
            animationDuration = a.getInteger(R.styleable.LoadingDotsView_animationDuration, animationDuration);
            animationDelay = a.getInteger(R.styleable.LoadingDotsView_animationDelay, animationDelay);
            scaleMultiplier = a.getFloat(R.styleable.LoadingDotsView_scaleMultiplier, scaleMultiplier);
            a.recycle();
        }

        LayoutInflater.from(getContext()).inflate(R.layout.translation_loading_dots, this, true);
        
        // Initialize dots array
        dots = new TextView[] {
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3),
            findViewById(R.id.dot4),
            findViewById(R.id.dot5)
        };

        // Apply custom attributes to dots
        for (TextView dot : dots) {
            dot.setTextColor(dotColor);
            dot.setTextSize(dotSize);
        }
    }

    public void startAnimation() {
        stopAnimation(); // Stop any existing animation
        
        final Animation dotAnim = AnimationUtils.loadAnimation(getContext(), R.anim.dot_pulse);
        dotAnim.setDuration(animationDuration);
        
        dotsAnimation = new Runnable() {
            @Override
            public void run() {
                // Reset previous dot
                dots[currentDotIndex].clearAnimation();
                dots[currentDotIndex].setScaleX(1.0f);
                dots[currentDotIndex].setScaleY(1.0f);
                dots[currentDotIndex].setAlpha(0.5f);
                
                // Move to next dot
                currentDotIndex = (currentDotIndex + 1) % dots.length;
                
                // Animate current dot
                dots[currentDotIndex].startAnimation(dotAnim);
                
                // Schedule next animation
                dotsHandler.postDelayed(this, animationDelay);
            }
        };
        
        dotsHandler.post(dotsAnimation);
    }

    public void stopAnimation() {
        if (dotsHandler != null && dotsAnimation != null) {
            dotsHandler.removeCallbacks(dotsAnimation);
        }
        
        if (dots != null) {
            for (TextView dot : dots) {
                dot.clearAnimation();
                dot.setScaleX(1.0f);
                dot.setScaleY(1.0f);
                dot.setAlpha(0.5f);
            }
        }
    }

    // Setters for customization
    public void setDotColor(int color) {
        this.dotColor = color;
        for (TextView dot : dots) {
            dot.setTextColor(color);
        }
    }

    public void setDotSize(float size) {
        this.dotSize = size;
        for (TextView dot : dots) {
            dot.setTextSize(size);
        }
    }

    public void setAnimationDuration(int duration) {
        this.animationDuration = duration;
    }

    public void setAnimationDelay(int delay) {
        this.animationDelay = delay;
    }

    public void setScaleMultiplier(float multiplier) {
        this.scaleMultiplier = multiplier;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
        dotsHandler.removeCallbacksAndMessages(null);
    }
} 