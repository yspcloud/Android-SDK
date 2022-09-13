package com.example.alan.sdkdemo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.alan.sdkdemo.R;
import com.example.alan.sdkdemo.receiver.ContentReceiver;
import com.vcrtc.VCRTCPreferences;
import com.vcrtc.entities.Call;
import com.vcrtc.entities.IncomingCall;
import com.vcrtc.registration.VCRegistrationUtil;
import com.vcrtc.utils.SystemUtil;
import com.vcrtc.utils.VCUtil;

import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.app.AppCompatActivity;

public class CallIncomingActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView name;
    private IncomingCall inComingCall;
    private Timer timer;
    private TimerTask timerTask;
    VCRTCPreferences prefs;
    private final long OUT_TIME = 45000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_incoming);
        inComingCall = (IncomingCall) getIntent().getSerializableExtra("inComingCall");
        name = findViewById(R.id.call_in_name);
        findViewById(R.id.call_in_accept_audio).setOnClickListener(this);
        findViewById(R.id.call_in_hang_up).setOnClickListener(this);
        prefs = new VCRTCPreferences(this);
        if (prefs.isShiTongPlatform() && inComingCall.getRemoteName().contains("@")) {
            inComingCall.setRemoteName(inComingCall.getRemoteName().split("@")[0]);
        }
        // 超过8个字符用...代替
        name.setText(inComingCall.getRemoteName().length() > 8 ?
                inComingCall.getRemoteName().substring(0, 8) + "..." : inComingCall.getRemoteName());
        timer = new Timer();
        Log.d("timerTask", "run: " + Thread.currentThread().toString());
        timerTask = new TimerTask() {
            @Override
            public void run() {
                if (!isFinishing()){
                    VCRegistrationUtil.hangup(CallIncomingActivity.this,
                            prefs.isShiTongPlatform()? ContentReceiver.OUT_TIME: ContentReceiver.HANG_UP);
                    finish();
                    Log.d("timerTask", "run: " + Thread.currentThread().toString());
                }
            }
        };
        timer.schedule(timerTask, OUT_TIME);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.call_in_accept_audio:
                timer.cancel();
                answer(inComingCall);
                break;
            case R.id.call_in_hang_up:
                timer.cancel();
                decline(inComingCall);
                break;
            default:
        }
    }

    private void answer(IncomingCall inComingCall){
        Intent intent = new Intent(this, ZJConferenceActivity.class);
        intent.putExtra("call", getCallBean(inComingCall));
        startActivity(intent);
        finish();
    }

    private void decline(IncomingCall inComingCall){
        VCRegistrationUtil.hangup(this, ContentReceiver.HANG_UP);
        finish();
    }

    private Call getCallBean(IncomingCall inComingCall) {
        Call call = new Call();
        call.setChannel(inComingCall.getChannel());
        call.setPassword(call.getPassword());
        call.setHideMe(false);
        call.setHost(false);
        call.setAccount(ContentReceiver.account);
        call.setNickname(ContentReceiver.accountName);
        call.setCheckDup(VCUtil.MD5(SystemUtil.getMac(this) + call.getNickname()));
        call.setMsgJson(inComingCall.getMsgJson());
        return call;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}