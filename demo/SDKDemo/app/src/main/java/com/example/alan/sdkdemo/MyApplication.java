package com.example.alan.sdkdemo;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import com.example.alan.sdkdemo.contact.SPUtil;
import com.qw.soul.permission.SoulPermission;
import com.tencent.bugly.crashreport.CrashReport;
import com.vcrtc.VCRTCPreferences;
import com.vcrtc.utils.LogUtil;
import com.vcrtc.webrtc.RTCManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import androidx.multidex.MultiDex;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        VCRTCPreferences prefs = new VCRTCPreferences(this);

        // rtsp
//        prefs.setCaptureVideoFps(34);
//        prefs.setCameraVideoFps(30);
//        prefs.setVideoFps(30);
//        prefs.setMaxVideoFps(30);
        prefs.setDisableFrameDropper(true);
//        prefs.setCameraVideoSize(1920, 1080);
//        prefs.setRtspEncoder(true);
//        prefs.setRtspURL("");
//        prefs.setEnableH265Encoder(true);
//        prefs.setEnableH265Decoder(false);
        if (isAppMainProcess()){
            SPUtil.Companion.instance(getApplicationContext()).setLogin(false);
            SPUtil.Companion.instance(getApplicationContext()).setModel("");
            SPUtil.Companion.instance(getApplicationContext()).setSessionId("");
            SPUtil.Companion.instance(getApplicationContext()).setCompanyId("");
            // 设置开发者token和deviceId
            prefs.setDeviceId("b6ecdce5-demo-4fc1-957d-b9bda59daf4f");
            prefs.setToken("f1d91d35-demo-468f-a3e0-f7ae9f365841");
            //复制关闭摄像头的图片到手机
            copyCloseVideoImageFromRaw(prefs);
        }
        prefs.setPrintLogs(true);
        LogUtil.startWriteLog(this, BuildConfig.DEBUG);
        SoulPermission.init(this);
        RTCManager.init(this);
        RTCManager.DEVICE_TYPE = "Android";
        RTCManager.OEM = "";
        CrashReport.initCrashReport(getApplicationContext(), "941e592e23", true);

    }

    private boolean isAppMainProcess(){
        String process = getProcessName(this);
        return TextUtils.isEmpty(process) || BuildConfig.APPLICATION_ID.equalsIgnoreCase(process);
    }

    public static String getProcessName(Context cxt) {
        int pid = android.os.Process.myPid();
        ActivityManager am = (ActivityManager) cxt.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
        if (runningApps == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo procInfo : runningApps) {
            if (procInfo.pid == pid) {
                return procInfo.processName;
            }
        }
        return null;
    }

    private void copyCloseVideoImageFromRaw(VCRTCPreferences prefs) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        AssetManager manager = getAssets();
        InputStream inputStream = null;
        try {
            inputStream = manager.open("novideo.png");
        } catch (IOException e) {
            e.printStackTrace();
        }

        String imagePath = getFilesDir().getAbsolutePath() + File.separator + "close_video.png";
//        InputStream inputStream = getResources().openRawResource(R.raw.close_video);
        File file = new File(imagePath);
        try {
            if (!file.exists()) {
                FileOutputStream fos = new FileOutputStream(file);
                byte[] buffer = new byte[inputStream.available()];
                int lenght;
                while ((lenght = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, lenght);
                }
                fos.flush();
                fos.close();
                inputStream.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        prefs.setImageFilePath(imagePath);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
