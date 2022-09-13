package com.example.alan.sdkdemo.util;

import android.text.TextUtils;

import com.vcrtc.utils.OkHttpUtil;

import java.io.IOException;

import okhttp3.Callback;
import okhttp3.Response;

public class CheckUtil {

    /**
     * 检测会议室合法性
     * @param num 会议室号
     * @param apiServer 服务器地址
     */
    public static void checkConference(String num, String apiServer, CheckConferenceListener listener) {
        if (TextUtils.isEmpty(apiServer)) {
            Exception exception = new Exception("请输入服务器地址");
            listener.onFail(exception);
            return;
        }
        String url = String.format("https://" + apiServer + "/api/" + "getmeetinginfo?addr=%s", num);

        OkHttpUtil.doGet(url, new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                listener.onFail(e);
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                String rep = response.body().string();
                listener.onSuccess(rep);
            }
        });
    }

    public interface CheckConferenceListener{
        void onSuccess(String response);
        void onFail(Exception e);
    }
}
