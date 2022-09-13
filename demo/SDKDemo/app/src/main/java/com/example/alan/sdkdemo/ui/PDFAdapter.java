package com.example.alan.sdkdemo.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.luck.picture.lib.photoview.PhotoView;
import com.vcrtc.utils.BitmapUtil;
import com.vcrtc.utils.PDFUtil;

import java.util.ArrayList;
import java.util.List;

import androidx.viewpager.widget.PagerAdapter;

public class PDFAdapter extends PagerAdapter {

    private Context context;
    private List<String> imagePaths;
    private BitmapUtil bitmapUtil;
    private PDFUtil pdfUtil;
    private int size;
    private Bitmap firstBitmap;
    private List<ImageView> zoomImageViews;

    public PDFAdapter(Context context, List imagePaths) {
        this.context = context;
        this.imagePaths = imagePaths;
        this.bitmapUtil = new BitmapUtil(context);
    }

    public PDFAdapter(Context context, int size, PDFUtil pdfUtil, Bitmap bitmap) {
        this.context = context;
        this.bitmapUtil = new BitmapUtil(context);
        this.pdfUtil = pdfUtil;
        this.size = size;
        firstBitmap = bitmap;
        zoomImageViews = new ArrayList<>();
    }

    @Override
    public int getCount() {
//        return imagePaths.size();
        return size;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        PhotoView iv = new PhotoView(container.getContext(), null);
        if (onItemImageListener != null) {
            iv.setOnClickListener((v) -> onItemImageListener.onClick());
//            iv.setOnCutListener(bitmap -> onItemImageListener.onCutBitmap(bitmap));
            Log.d("ViewPagerAdapter", "iv: " + iv);
//            onItemImageListener.onGetImageView(iv);
        }
        Bitmap bitmap;
        Log.d("ViewPagerAdapter", "instantiateItem: " + position);

        if (position == 0 && firstBitmap != null){
            Log.d("refresh_ui", "instantiateItem: pdfBitmap" + firstBitmap.isRecycled());
            bitmap = firstBitmap;
            firstBitmap = null;

        }else {
            bitmap = pdfUtil.openPage(position);
        }

        iv.setImageBitmap(bitmap);

        // 添加到ViewPager容器
        container.addView(iv);

        // 返回填充的View对象
        return iv;
    }
    public List<ImageView> getZoomImageViews(){
        return zoomImageViews;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ((PhotoView)object).setScale(1);
        container.removeView((View) object);
    }

    private OnItemImageListener onItemImageListener;

    public void setOnItemImageListener (OnItemImageListener onItemImageListener) {
        this.onItemImageListener = onItemImageListener;
    }

    public interface OnItemImageListener {
        void onClick();
        void onCutBitmap(Bitmap bitmap);
        void onGetImageView(ImageView imageView);
    }
}
