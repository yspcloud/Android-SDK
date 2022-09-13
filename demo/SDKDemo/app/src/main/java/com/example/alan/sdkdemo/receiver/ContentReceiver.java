package com.example.alan.sdkdemo.receiver;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.example.alan.sdkdemo.ui.CallIncomingActivity;
import com.example.alan.sdkdemo.ui.ZJConferenceActivity;
import com.vcrtc.entities.IncomingCall;
import com.vcrtc.registration.VCRegistrationUtil;
import com.vcrtc.registration.VCService;

import org.json.JSONException;
import org.json.JSONObject;

import static android.content.Context.ACTIVITY_SERVICE;
import static com.vcrtc.registration.VCService.MSG;

public class ContentReceiver extends BroadcastReceiver {
    public final static int IN_CONFERENCE = 1;
    public final static int HANG_UP = 2;
    public final static int OUT_TIME = 3;
    public static String account = "";
    public static String accountName = "";
    @Override
    public void onReceive(Context context, Intent intent) {
        String message = intent.getStringExtra(MSG);
        final String TAG = "ContentReceiver";
        switch (message){
            case VCService.MSG_LOGIN_SUCCESS:
                Log.i(TAG, "登录成功");
                break;
            case VCService.MSG_LOGIN_FAILED:
                String reason = intent.getStringExtra(VCService.DATA_BROADCAST);
                Log.i(TAG, "登录失败" + reason);
                break;
            case VCService.MSG_USER_INFO:
                String userJson = intent.getStringExtra(VCService.DATA_BROADCAST);
                Log.i(TAG, "用户信息" + userJson);
                // 此处用于被呼测试，正式开发须自己保存用户信息
                try {
                    JSONObject jsonObject = new JSONObject(userJson);
                    account = jsonObject.optString("account");
                    accountName = jsonObject.optString("trueName");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case VCService.MSG_SESSION_ID:
                String sessionID = intent.getStringExtra(VCService.DATA_BROADCAST);
                Log.i(TAG, "sessionID:" + sessionID);
                break;
            case VCService.MSG_ONLINE_STATUS:
                boolean onLine = intent.getBooleanExtra(VCService.DATA_BROADCAST, false);
                Log.i(TAG, "在线状态:" + onLine);
                break;
            case VCService.MSG_LOGOUT:
                Log.i(TAG, "账号在别的端登录");
                break;
            case VCService.MSG_INCOMING:
                IncomingCall incomingCall = (IncomingCall) intent.getSerializableExtra(VCService.DATA_BROADCAST);
                //已经在通话中，直接挂掉
                if (isInConference(context)) {
                    VCRegistrationUtil.hangup(context, IN_CONFERENCE);
                    break;
                }
                showInComingView(context, incomingCall);
                //显示来电界面，需要自己开发界面，并把incomingCall传过去展示相应信息
                //在来电界面，单机接听的话可以执行下面代码
//                joinConference(context, incomingCall);
                // 如果是拒绝，需要执行hangup方法
//                VCRegistrationUtil.hangup(context, 1);
                break;
            case VCService.MSG_INCOMING_CANCELLED:
                // 公有云会收到对方取消通话的消息
                // 还没接听的情况下，收到此消息把来电界面关闭
                break;
            default:
        }
    }

    //判断是否正在开会
    public boolean isInConference(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        return cn.getClassName().equals(ZJConferenceActivity.class.getName());
    }

    private void showInComingView(Context context, IncomingCall inComingCall) {
        Bundle b = new Bundle();
        b.putSerializable("inComingCall", inComingCall);
        Intent i = new Intent(context, CallIncomingActivity.class);
        i.putExtras(b);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(i);
    }
}
