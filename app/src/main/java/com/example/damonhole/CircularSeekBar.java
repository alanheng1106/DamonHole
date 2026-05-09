package com.example.damonhole;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class CircularSeekBar extends View {

    private int progress = 0;
    private int max = 100;
    private int accentColor = Color.WHITE;
    private float trackWidth;
    private float progressWidth;

    private Paint backgroundPaint;
    private Paint progressPaint;
    private RectF arcRect;

    private OnSeekBarChangeListener listener;

    public interface OnSeekBarChangeListener {
        void onProgressChanged(CircularSeekBar seekBar, int progress, boolean fromUser);
        void onStartTrackingTouch(CircularSeekBar seekBar);
        void onStopTrackingTouch(CircularSeekBar seekBar);
    }

    public CircularSeekBar(Context context) {
        super(context);
        init(context, null);
    }

    public CircularSeekBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CircularSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        float density = context.getResources().getDisplayMetrics().density;
        // Material 3 progress indicators are thicker
        trackWidth = 4f * density;
        progressWidth = 4f * density;

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(trackWidth);
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);
        
        // Use colorOutlineVariant or colorSurfaceVariant for the track
        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOutlineVariant, tv, true);
        backgroundPaint.setColor(tv.data);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(progressWidth);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        
        // Primary color for progress
        context.getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, tv, true);
        accentColor = tv.data;
        progressPaint.setColor(accentColor);

        arcRect = new RectF();
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        this.listener = listener;
    }

    public void setProgress(int progress) {
        this.progress = Math.min(progress, max);
        invalidate();
    }

    public void setMax(int max) {
        this.max = max;
        invalidate();
    }

    public void setAccentColor(int color) {
        this.accentColor = color;
        progressPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float centerX = width / 2f;
        float centerY = height / 2f;
        float radius = (Math.min(width, height) - Math.max(trackWidth, progressWidth)) / 2f;

        arcRect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        // Draw background circle (Track)
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint);

        // Draw progress arc
        if (max > 0) {
            float sweepAngle = 360f * progress / max;
            canvas.drawArc(arcRect, -90, sweepAngle, false, progressPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX() - getWidth() / 2f;
        float y = event.getY() - getHeight() / 2f;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (listener != null) listener.onStartTrackingTouch(this);
                updateProgressFromTouch(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                updateProgressFromTouch(x, y);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (listener != null) listener.onStopTrackingTouch(this);
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void updateProgressFromTouch(float x, float y) {
        double angleRad = Math.atan2(y, x);
        float angleDeg = (float) Math.toDegrees(angleRad) + 90f;
        if (angleDeg < 0) angleDeg += 360f;

        progress = Math.round(max * angleDeg / 360f);
        if (listener != null) listener.onProgressChanged(this, progress, true);
        invalidate();
    }
}