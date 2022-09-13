package com.example.alan.sdkdemo.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.alan.sdkdemo.util.BitmapUtil;
import com.example.alan.sdkdemo.widget.ZoomViewPager;
import com.vcrtc.webrtc.RTCManager;

import java.util.List;

import androidx.viewpager.widget.PagerAdapter;

public class ViewPagerAdapter extends PagerAdapter {

    private Context context;
    private List<String> imagePaths;
    private ZoomViewPager pager;
    ViewGroup.LayoutParams params;

    public ViewPagerAdapter(Context context, List imagePaths) {
        this.context = context;
        this.imagePaths = imagePaths;
        params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

    }

//    public ViewPagerAdapter(Context context, List imagePaths, ZoomViewPager pager) {
//        this.context = context;
//        this.imagePaths = imagePaths;
//        this.pager = pager;
//    }


    @Override
    public int getCount() {
        return imagePaths.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
//        ZoomImageView iv = new ZoomImageView(container.getContext(), null);
        ImageView iv = new ImageView(container.getContext());
        if (onItemImageListener != null) {
            iv.setOnClickListener((view) -> onItemImageListener.onClick());
//            iv.setOnCutListener(bitmap -> onItemImageListener.onCutBitmap(bitmap));
        }

        Bitmap bitmap;
        if (RTCManager.isIsShitongPlatform()) {
            bitmap = com.example.alan.sdkdemo.util.BitmapUtil
                    .formatBitmap16_9(com.example.alan.sdkdemo.util.BitmapUtil
                            .getImage(imagePaths.get(position)), 1920, 1080);
        } else {
            bitmap = BitmapUtil.getImage(imagePaths.get(position));
        }
        iv.setScaleType(ImageView.ScaleType.CENTER);
        Glide.with(context).load(bitmap).into(iv);

//        iv.setImageBitmap(bitmap);
        // 添加到ViewPager容器
        container.addView(iv, params);

        // 返回填充的View对象
        return iv;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
//        ((ZoomImageView) object).reset();
        container.removeView((View) object);
    }

    private OnItemImageListener onItemImageListener;

    void setOnItemImageListener(OnItemImageListener onItemImageListener) {
        this.onItemImageListener = onItemImageListener;
    }

    public interface OnItemImageListener {
        void onClick();

        void onCutBitmap(Bitmap bitmap);
    }
}
