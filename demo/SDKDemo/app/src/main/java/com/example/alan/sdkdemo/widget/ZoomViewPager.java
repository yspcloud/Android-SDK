package com.example.alan.sdkdemo.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

public class ZoomViewPager extends ViewPager {
    private boolean canSlide = true;
    public ZoomViewPager(Context context) {
        super(context);
    }

    public ZoomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (canSlide){
            return super.onInterceptTouchEvent(ev);
        }else {
            return false;
        }
    }

    public void setCanSlide(boolean canSlide){
        this.canSlide = canSlide;
    }
}
