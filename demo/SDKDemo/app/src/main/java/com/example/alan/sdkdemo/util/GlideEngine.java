package com.example.alan.sdkdemo.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.huantansheng.easyphotos.EasyPhotos;
import com.huantansheng.easyphotos.engine.ImageEngine;

import androidx.fragment.app.Fragment;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

/**
 * Glide4.x的加载图片引擎实现,单例模式
 * Glide4.x的缓存机制更加智能，已经达到无需配置的境界。如果使用Glide3.x，需要考虑缓存机制。
 * Created by huan on 2018/1/15.
 */

public class GlideEngine implements ImageEngine {
    public static final int REQUEST_CODE = 101;

    //单例
    private static GlideEngine instance = null;
    //单例模式，私有构造方法
    private GlideEngine() {
    }
    //获取单例
    public static GlideEngine getInstance() {
        if (null == instance) {
            synchronized (GlideEngine.class) {
                if (null == instance) {
                    instance = new GlideEngine();
                }
            }
        }
        return instance;
    }

    public void getPicture(Fragment context){
        EasyPhotos.createAlbum(context, false, GlideEngine.getInstance())
                .setCount(9)
                .start(REQUEST_CODE);
    }

    @Override
    public void loadPhoto(Context context, String photoPath, ImageView imageView) {
        Glide.with(context).load(photoPath).transition(withCrossFade()).into(imageView);
    }

    @Override
    public void loadGifAsBitmap(Context context, String gifPath, ImageView imageView) {
        Glide.with(context).asBitmap().load(gifPath).into(imageView);

    }

    @Override
    public void loadGif(Context context, String gifPath, ImageView imageView) {
        Glide.with(context).asGif().load(gifPath).transition(withCrossFade()).into(imageView);

    }

    @Override
    public Bitmap getCacheBitmap(Context context, String path, int width, int height) throws Exception {
        return Glide.with(context).asBitmap().load(path).submit(width, height).get();

    }
}
