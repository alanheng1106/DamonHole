package com.example.damonhole.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.damonhole.R;
import com.google.android.material.card.MaterialCardView;

public class M3PullRefreshLayout extends FrameLayout {

    private View scrollableChild;
    private View refreshIndicatorContainer;
    private View loadingIndicator;
    
    private float lastTouchY;
    private float totalPullDistance = 0;
    private final float refreshThreshold;
    private final float maxPullDistance;
    private final int touchSlop;
    
    private boolean isRefreshing = false;
    private boolean isBeingDragged = false;
    
    private OnRefreshListener onRefreshListener;

    public interface OnRefreshListener {
        void onRefresh();
    }

    public M3PullRefreshLayout(@NonNull Context context) {
        this(context, null);
    }

    public M3PullRefreshLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        refreshThreshold = dpToPx(80);
        maxPullDistance = dpToPx(160);
        
        initIndicator();
    }

    private void initIndicator() {
        LayoutInflater.from(getContext()).inflate(R.layout.layout_m3_refresh_indicator, this, true);
        refreshIndicatorContainer = findViewById(R.id.refreshIndicatorContainer);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        
        refreshIndicatorContainer.setVisibility(GONE);
        refreshIndicatorContainer.setScaleX(0);
        refreshIndicatorContainer.setScaleY(0);
        refreshIndicatorContainer.setAlpha(0);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() > 1) {
            // The first child is our indicator, the second is the scrollable content
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child.getId() != R.id.refreshIndicatorContainer) {
                    scrollableChild = child;
                    break;
                }
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isRefreshing || scrollableChild == null) return false;

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchY = ev.getY();
                isBeingDragged = false;
                break;
                
            case MotionEvent.ACTION_MOVE:
                float y = ev.getY();
                float diff = y - lastTouchY;
                
                if (diff > touchSlop && !canChildScrollUp()) {
                    isBeingDragged = true;
                    return true;
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (isRefreshing || scrollableChild == null) return super.onTouchEvent(ev);

        switch (ev.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float y = ev.getY();
                float diff = y - lastTouchY;
                
                if (diff > 0) {
                    // Pulling down
                    updatePullDistance(diff * 0.5f); // 0.5 friction
                    return true;
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                finishPull();
                break;
        }
        return super.onTouchEvent(ev);
    }

    private void updatePullDistance(float distance) {
        totalPullDistance = Math.min(distance, maxPullDistance);
        
        if (totalPullDistance > 0) {
            refreshIndicatorContainer.setVisibility(VISIBLE);
            
            float progress = totalPullDistance / refreshThreshold;
            float cappedProgress = Math.min(progress, 1.0f);
            
            // Move container down as we pull
            refreshIndicatorContainer.setTranslationY(totalPullDistance - dpToPx(48));
            
            // Scale and Alpha based on progress
            refreshIndicatorContainer.setScaleX(cappedProgress);
            refreshIndicatorContainer.setScaleY(cappedProgress);
            refreshIndicatorContainer.setAlpha(cappedProgress);
            
            // Note: We could potentially drive LoadingIndicator state here if it supported it
        } else {
            refreshIndicatorContainer.setVisibility(GONE);
        }
    }

    private void finishPull() {
        if (totalPullDistance >= refreshThreshold) {
            startRefreshing();
        } else {
            resetPull();
        }
    }

    private void startRefreshing() {
        isRefreshing = true;
        
        // Animate to standard position
        refreshIndicatorContainer.animate()
                .translationY(dpToPx(16))
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(200)
                .start();
        
        if (onRefreshListener != null) {
            onRefreshListener.onRefresh();
        }
    }

    public void setRefreshing(boolean refreshing) {
        if (isRefreshing == refreshing) return;
        
        isRefreshing = refreshing;
        if (!isRefreshing) {
            resetPull();
        }
    }

    private void resetPull() {
        totalPullDistance = 0;
        isBeingDragged = false;
        
        refreshIndicatorContainer.animate()
                .scaleX(0)
                .scaleY(0)
                .alpha(0)
                .setDuration(200)
                .withEndAction(() -> refreshIndicatorContainer.setVisibility(GONE))
                .start();
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        this.onRefreshListener = listener;
    }

    private boolean canChildScrollUp() {
        return scrollableChild.canScrollVertically(-1);
    }

    private float dpToPx(int dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }
}
