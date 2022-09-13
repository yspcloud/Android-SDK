package com.example.alan.sdkdemo.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.os.Process;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.example.alan.sdkdemo.R;
import com.qw.soul.permission.SoulPermission;
import com.qw.soul.permission.bean.Permission;
import com.qw.soul.permission.bean.Permissions;
import com.qw.soul.permission.callbcak.CheckRequestPermissionsListener;
import com.vcrtc.VCRTC;
import com.vcrtc.VCRTCPreferences;
import com.vcrtc.VCRTCView;
import com.vcrtc.callbacks.CallBack;
import com.vcrtc.entities.Call;
import com.vcrtc.entities.ConferenceStatus;
import com.vcrtc.entities.ErrorCode;
import com.vcrtc.entities.Participant;
import com.vcrtc.entities.Stage;
import com.vcrtc.entities.WhiteboardPayload;
import com.vcrtc.listeners.VCRTCListener;
import com.vcrtc.listeners.VCRTCListenerImpl;
import com.vcrtc.utils.VCAudioManager;
import com.vcrtc.utils.VCHomeListener;
import com.vcrtc.utils.VCNetworkListener;
import com.vcrtc.utils.VCPhoneListener;
import com.vcrtc.utils.VCScreenListener;
import com.vcrtc.utils.VCUtil;
import com.vcrtc.utils.VCWindowManager;
import com.vcrtc.webrtc.RTCManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class ZJConferenceActivity extends AppCompatActivity {

    private MediaFragment mediaFragment;
    private MediaSimulcastFragment mediaSimulcastFragment;
    public MediaShiTongFragment mediaShiTongFragment;

    VCRTCPreferences prefs;
    VCRTC vcrtc;
    Call call;
    String myUUID;
    public static boolean isHost;
    static String shareUrl;
    static boolean joinMuteAudio, isForeground;
    static boolean isAllMute, isLock;
    static boolean allowRecordAudio, allowCamera;

    static Map<String, Participant> participants;
    /**
     * 是否说话列表
     **/
    static List<Stage> vadStage;

    private VCAudioManager audioManager;
    private VCAudioManager.AudioDevice audioDevice;
    /**
     * home键监听
     **/
    private VCHomeListener homeListener;
    /**
     * 锁屏监听
     **/
    private VCScreenListener screenListener;
    /**
     * 来电监听
     **/
    private VCPhoneListener phoneListener;
    /**
     * 网络监听
     **/
    private VCNetworkListener networkListener;

    private Timer noNetworkTimer;
    private TimerTask noNetworkToFinishTask;

    public Bitmap closeVideoBitmap, audioModelBitmap;

    static boolean isTurnOn;

    public boolean isShowWhite = false;



    /**
     * if (RTCManager.isIsShitongPlatform()) {
     * // 专属云平台
     * if (!mediaShiTongFragment.isMuteAudio) {
     * vcrtc.setAudioEnable(enable);
     * }
     * } else {
     * // 公有云平台
     * if (prefs.isSimulcast()) {
     * // 转发模式
     * if (mediaSimulcastFragment != null && !mediaSimulcastFragment.isMuteAudio) {
     * vcrtc.setAudioEnable(enable);
     * }
     * } else {
     * // 全编全解
     * if (!mediaFragment.isMuteAudio) {
     * vcrtc.setAudioEnable(enable);
     * }
     * }
     * }
     */




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_zjconference);

        call = (Call) getIntent().getSerializableExtra("call");
        joinMuteAudio = getIntent().getBooleanExtra("muteAudio", false);
        if (call == null) {
            Toast.makeText(getApplicationContext(), "请设置呼叫参数call不能为null", Toast.LENGTH_SHORT).show();
            finish();
            return;
        } else {
            if (call.getNickname() == null || "".equals(call.getNickname())) {
                Toast.makeText(getApplicationContext(), "请设置显示名称", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            if (call.getChannel() == null || "".equals(call.getChannel())) {
                Toast.makeText(getApplicationContext(), "请设置呼叫地址", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }
        if (joinMuteAudio) {
            showToast(getString(R.string.toast_join_mute), Toast.LENGTH_SHORT);
        }
        initData();
        showMediaFragment();
        checkPermission();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (audioManager != null) {
            if (audioDevice.equals(VCAudioManager.AudioDevice.BLUETOOTH)) {
                Log.d("audioManager", "bluetooth: ");
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                audioManager.startBluetoothSco();
                audioManager.setBluetoothScoOn(true);
                audioManager.setSpeakerphoneOn(false);

            } else {
                Log.d("audioManager", "onIdle: " + audioDevice);
                if (audioManager != null) {
                    audioManager.selectAudioDevice(audioDevice);
                    new Handler().postDelayed(() -> {
                                if (audioManager != null) {
                                    audioManager.updateAudioDeviceState();
                                }
                            }
                            , 2000);

                }

            }

        }
    }

    private void initData() {
        prefs = new VCRTCPreferences(this);
        Log.d("activity_instance", "initData: " + this);
        vcrtc = new VCRTC(this);
        if (null != call.getApiServer()) {
            vcrtc.setApiServer(call.getApiServer());
            vcrtc.setIsShitongPlatform(call.isShitongPlatform());
        } else {
            vcrtc.setIsShitongPlatform(prefs.isShiTongPlatform());
        }

        isHost = call.isHost();
        shareUrl = null;
        isForeground = false;
        isAllMute = false;
        isLock = false;
        allowRecordAudio = false;
        allowCamera = false;
        participants = new LinkedHashMap<>();
        vadStage = new ArrayList<>();

        audioManager = VCAudioManager.create(getApplicationContext());
        // This method will be called each time the number of available audio devices has changed.
        audioManager.start((audioDevice, availableAudioDevices) -> this.audioDevice = audioDevice);

        initListener();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        AssetManager manager = getAssets();
        InputStream novideo = null;
        InputStream audioModel = null;
        try {
            novideo = manager.open("novideo.png");
            audioModel = manager.open("img_audio_model.png");
        } catch (IOException e) {
            e.printStackTrace();
        }
        closeVideoBitmap = BitmapFactory.decodeStream(novideo);
        audioModelBitmap = BitmapFactory.decodeStream(audioModel);
    }

    /**
     * 初始化监听器
     */
    private void initListener() {
        homeListener = new VCHomeListener(getApplicationContext());
        homeListener.setKeyListener(new VCHomeListener.KeyListener() {
            @Override
            public void home() {
                if (RTCManager.isIsShitongPlatform()) {
                    // isShitongplatform true 专属云平台
                    if (!mediaShiTongFragment.isMuteVideo && !mediaShiTongFragment.isAudioModel) {
                        vcrtc.updateVideoImage(closeVideoBitmap);
                        vcrtc.setVideoEnable(false, true);
                    }
                } else {

                    // false 公有云平台
                    vcrtc.updateVideoImage(closeVideoBitmap);
                    // 判断是否是转发模式 true为转发模式 false全编全解
                    if (prefs.isSimulcast()) {
                        if (!mediaSimulcastFragment.isMuteVideo && !mediaSimulcastFragment.isShareScreen) {
                            vcrtc.setVideoEnable(false, true);
                        }
                    } else {
                        if (!mediaFragment.isMuteVideo && !mediaFragment.isShareScreen) {
                            vcrtc.setVideoEnable(false, true);
                        }
                    }
                }
            }

            @Override
            public void recent() {

            }

            @Override
            public void longHome() {

            }
        });

        screenListener = new VCScreenListener(getApplicationContext());
        screenListener.setScreenStateListener(new VCScreenListener.ScreenStateListener() {
            @Override
            public void onScreenOn() {

            }

            @Override
            public void onScreenOff() {
                if (RTCManager.isIsShitongPlatform()) {
                    if (!mediaShiTongFragment.isMuteVideo && !mediaShiTongFragment.isShareScreen && !mediaShiTongFragment.isAudioModel) {
                        vcrtc.updateVideoImage(closeVideoBitmap);
                        vcrtc.setVideoEnable(false, true);
                    }
                } else {
                    vcrtc.updateVideoImage(closeVideoBitmap);
                    if (prefs.isSimulcast()) {
                        if (!mediaSimulcastFragment.isMuteVideo && !mediaSimulcastFragment.isShareScreen) {
                            vcrtc.setVideoEnable(false, true);
                        }
                    } else {
                        if (!mediaFragment.isMuteVideo && !mediaFragment.isShareScreen) {
                            vcrtc.setVideoEnable(false, true);
                        }
                    }
                }
            }

            @Override
            public void onUserPresent() {

            }
        });
        screenListener.startListen();

        phoneListener = new VCPhoneListener(getApplicationContext());
        phoneListener.setPhoneListener(new VCPhoneListener.MyPhoneStateListener() {
            @Override
            public void onIdle() {
                Log.d("phoneListener", "onIdle: " + audioDevice);
                if (audioDevice.equals(VCAudioManager.AudioDevice.BLUETOOTH)) {
                    AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    audioManager.startBluetoothSco();
                    audioManager.setBluetoothScoOn(true);
                    audioManager.setSpeakerphoneOn(false);
                } else {
                    if (audioManager != null) {
                        audioManager.selectAudioDevice(audioDevice);
                    }
                }
                if (vcrtc != null) {
                    vcrtc.setRemoteAudioEnable(true);
                }
                muteAudio(true);
                muteVideo(true);
            }

            @Override
            public void onOffhook() {
                if (vcrtc != null) {
                    vcrtc.setRemoteAudioEnable(false);
                }
                muteAudio(false);
                muteVideo(false);
            }
        });

        networkListener = new VCNetworkListener(getApplicationContext());
        networkListener.setmNetworkListener(new VCNetworkListener.NetworkListener() {
            @Override
            public void onNetworkChange(int networkType) {
                stopNoNetworkTimer();
                vcrtc.reconnectOnlyMediaCall();
            }

            @Override
            public void onNetworkDisconnect() {
                startNoNetworkTimer();
            }
        });
    }

    /**
     * 静音
     *
     * @param enable
     */
    private void muteAudio(boolean enable) {
        if (RTCManager.isIsShitongPlatform()) {
            // 专属云平台
            if (!mediaShiTongFragment.isMuteAudio) {
                vcrtc.setAudioEnable(enable);
            }
        } else {
            // 公有云平台
            if (prefs.isSimulcast()) {
                // 转发模式
                if (mediaSimulcastFragment != null && !mediaSimulcastFragment.isMuteAudio) {
                    vcrtc.setAudioEnable(enable);
                }
            } else {
                // 全编全解
                if (!mediaFragment.isMuteAudio) {
                    vcrtc.setAudioEnable(enable);
                }
            }
        }
    }

    /**
     * 禁止摄像头
     *
     * @param enable
     */
    private void muteVideo(boolean enable) {
        if (RTCManager.isIsShitongPlatform()) {
            if (!mediaShiTongFragment.isMuteVideo && !mediaShiTongFragment.isAudioModel) {
                vcrtc.updateVideoImage(closeVideoBitmap);
                vcrtc.setVideoEnable(enable, true);
            }
        } else {
            vcrtc.updateVideoImage(closeVideoBitmap);
            if (prefs.isSimulcast()) {
                if (!mediaSimulcastFragment.isMuteVideo) {
                    vcrtc.setVideoEnable(enable, true);
                }
            } else {
                if (!mediaFragment.isMuteVideo) {
                    vcrtc.setVideoEnable(enable, true);
                }
            }
        }
    }

    private void startNoNetworkTimer() {
        if (noNetworkTimer == null) {
            noNetworkTimer = new Timer();
        }
        if (noNetworkToFinishTask == null) {
            noNetworkToFinishTask = new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(() -> disconnect());
                }
            };
        }

        noNetworkTimer.schedule(noNetworkToFinishTask, 40000);
    }

    private void stopNoNetworkTimer() {
        if (noNetworkToFinishTask != null) {
            noNetworkToFinishTask.cancel();
            noNetworkToFinishTask = null;
        }
        if (noNetworkTimer != null) {
            noNetworkTimer.cancel();
            noNetworkTimer = null;
        }
    }

    /**
     * 呼叫
     *
     * @param callType 呼叫类型，具体可参考api说明文档
     */
    private void makeCall(VCRTC.CallType callType) {
        refreshAudioVideoIcon();
        vcrtc.setCallType(callType);
        vcrtc.setVCRTCListener(listener);
//        vcrtc.setCheckdup(call.getCheckDup());
        vcrtc.setClayout("1:4"); // 默认1：5
        if (null != call.getAccount()) {
            vcrtc.setAccount(call.getAccount());
        }
//         如果是被呼msgjson中会有对应信息，主动入会不需要
        if (call.getMsgJson() != null && !"".equals(call.getMsgJson())) {
            try {
                JSONObject root = new JSONObject(call.getMsgJson());
                String chanel = root.optString("conference_alias");
                String time = root.optString("time");
                String bssKey = root.optString("bsskey");
                String token = root.optString("token");
                call.setChannel(chanel);
                vcrtc.setTime(time);
                vcrtc.setBsskey(bssKey);
                vcrtc.setOneTimeToken(token);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        vcrtc.connect(call.getChannel(), call.getPassword(), call.getNickname(), new CallBack() {

            @Override
            public void success(String response) {

            }

            @Override
            public void failure(String reason) {
                runOnUiThread(() -> {
                    if (isForeground)
                        showErrorDialog(getString(R.string.call_failed), getString(R.string.can_not_connect_server));
                });
            }

        });
    }

    /**
     * 检测录音权限
     */
    public void checkPermission() {
        SoulPermission.getInstance().checkAndRequestPermissions(
                Permissions.build(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA),
                new CheckRequestPermissionsListener() {
                    @Override
                    public void onAllPermissionOk(Permission[] allPermissions) {
                        allowRecordAudio = true;
                        allowCamera = true;
                        makeCall(VCRTC.CallType.video);
                    }

                    @Override
                    public void onPermissionDenied(Permission[] refusedPermissions) {
                        if (refusedPermissions.length == 2) {
                            makeCall(VCRTC.CallType.recvAndSendBitmap);
                        } else {
                            for (int i = 0; i < refusedPermissions.length; i++) {
                                if (refusedPermissions[i].permissionName.equals(Manifest.permission.RECORD_AUDIO)) {
                                    allowCamera = true;
                                    makeCall(VCRTC.CallType.recvAndSendVideo);
                                }
                                if (refusedPermissions[i].permissionName.equals(Manifest.permission.CAMERA)) {
                                    allowRecordAudio = true;
                                    makeCall(VCRTC.CallType.recvAndSendAudioBitmap);
                                }
                            }
                        }
                    }
                });
    }

    /**
     * 根据对应平台，跳转到对应fragment
     */
    private void showMediaFragment() {
        if (RTCManager.isIsShitongPlatform()) {
            mediaShiTongFragment = new MediaShiTongFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.fl_content, mediaShiTongFragment);
            transaction.commit();
        } else {
            if (prefs.isSimulcast()) {
                mediaSimulcastFragment = new MediaSimulcastFragment();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.add(R.id.fl_content, mediaSimulcastFragment);
                transaction.commit();
            } else {
                mediaFragment = new MediaFragment();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.add(R.id.fl_content, mediaFragment);
                transaction.commit();
            }
        }
    }


    private void refreshAudioVideoIcon() {
        if (RTCManager.isIsShitongPlatform()) {
            mediaShiTongFragment.refreshAudioVideoIcon();
        } else {
            if (prefs.isSimulcast()) {
                mediaSimulcastFragment.refreshAudioVideoIcon();
            } else {
                mediaFragment.refreshAudioVideoIcon();
            }
        }
    }


    /**
     * 屏幕共享返回时调用
     *
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            boolean isStopShare = intent.getBooleanExtra("isStopShare", false);
            if (RTCManager.isIsShitongPlatform()) {
                if (isStopShare && mediaShiTongFragment.isShareScreen) {
                    mediaShiTongFragment.toggleShare();
                } else {
                    mediaShiTongFragment.rlShareScreen.setVisibility(View.VISIBLE);
                }
            } else {
                if (prefs.isSimulcast()) {
                    if (isStopShare && mediaSimulcastFragment.isShareScreen) {
                        mediaSimulcastFragment.toggleShare();
                    } else {
                        mediaSimulcastFragment.rlShareScreen.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (isStopShare && mediaFragment.isShareScreen) {
                        mediaFragment.toggleShare();
                    } else {
                        mediaFragment.rlShareScreen.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isForeground = true;
        Log.d("activity_instance", "onResume: " + this);
        RTCManager.getInstance().setActivityContext(this);
        VCWindowManager.removeFloatButton(getApplicationContext());
        muteAudio(true);
        muteVideo(true);
        if (RTCManager.isIsShitongPlatform()) {
            if (mediaShiTongFragment.isShareScreen) {
                mediaShiTongFragment.rlShareScreen.setVisibility(View.VISIBLE);
            } else {
                mediaShiTongFragment.rlShareScreen.setVisibility(View.GONE);
            }
        } else {
            if (prefs.isSimulcast()) {
                if (mediaSimulcastFragment.isShareScreen) {
                    mediaSimulcastFragment.rlShareScreen.setVisibility(View.VISIBLE);
                } else {
                    mediaSimulcastFragment.rlShareScreen.setVisibility(View.GONE);
                }
            } else {
                if (mediaFragment.isShareScreen) {
                    mediaFragment.rlShareScreen.setVisibility(View.VISIBLE);
                } else {
                    mediaFragment.rlShareScreen.setVisibility(View.GONE);
                }
            }
        }
        homeListener.startListen();
        phoneListener.startListen();
        networkListener.startListen();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isForeground = false;
        homeListener.stopListen();
        boolean shareScreen;
        if (RTCManager.isIsShitongPlatform()) {
            shareScreen = mediaShiTongFragment.isShareScreen;
        } else {
            if (prefs.isSimulcast()) {
                shareScreen = mediaSimulcastFragment.isShareScreen;
            } else {
                shareScreen = mediaFragment.isShareScreen;
            }
        }
        if (shareScreen && audioManager != null) {
            if (VCUtil.checkFloatPermission(this)) {
                //开启悬浮按钮
                VCWindowManager.createFloatButton(getApplicationContext(), ZJConferenceActivity.class, false);

                vcrtc.setVideoEnable(false, true);
            } else {
                showToast(getString(R.string.no_system_alert_window_permission), Toast.LENGTH_SHORT);
            }
        }
    }

    /**
     * 退出会议，释放监听
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (homeListener != null) {
            homeListener = null;
        }
        if (screenListener != null) {
            screenListener.stopListen();
            screenListener = null;
        }
        if (phoneListener != null) {
            phoneListener.stopListen();
            phoneListener = null;
        }
        if (networkListener != null) {
            networkListener.stopListen();
            networkListener = null;
        }
    }

    /**
     * 监听返回键，弹出提示dialog
     *
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fl_content);
            if (fragment instanceof MediaShiTongFragment) {
                mediaShiTongFragment.showDisconnectDialog();
                return true;
            } else if (fragment instanceof MediaSimulcastFragment) {
                mediaSimulcastFragment.showDisconnectDialog();
                return true;
            } else if (fragment instanceof MediaFragment) {
                mediaFragment.showDisconnectDialog();
                return true;
            } else if (fragment instanceof ParticipantFragment){
                getSupportFragmentManager().popBackStack();
                return true;
            }else {
                return super.onKeyUp(keyCode, event);
            }
        } else {
            return super.onKeyUp(keyCode, event);
        }
    }

    public void showToast(String message, int length) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.layout_toast, null);

        TextView tvMessage = layout.findViewById(R.id.tv_toast_message);
        tvMessage.setText(message);

        final Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setDuration(length);
        toast.setView(layout);
        toast.show();
    }

    private void showBeDisconnectedToast(String reason) {
        try {
            showToast(reason, 5000);
            disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showErrorDialog(String title, String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.disconnect_sure, (dialog, which) -> {
                    disconnect();
                    dialog.dismiss();
                }).create();

        if (!isFinishing()) {
            alertDialog.show();
        }
    }

    public void disconnect() {
        vcrtc.disconnect();

        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }

//        finish();
        Process.killProcess(Process.myPid());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (RTCManager.isIsShitongPlatform()) {
                mediaShiTongFragment.onActivityResult(requestCode, resultCode, data);
            } else {
                if (prefs.isSimulcast()) {
                    mediaSimulcastFragment.onActivityResult(requestCode, resultCode, data);
                } else {
                    mediaFragment.onActivityResult(requestCode, resultCode, data);
                }
            }
        }
    }

    public void showParticipant(){
        Fragment participantFragment = ParticipantFragment.newInstance();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fl_content, participantFragment).addToBackStack(null);

        transaction.commit();
    }

    /**
     * 信息回调监听，具体可参照文档
     */
    private VCRTCListener listener = new VCRTCListenerImpl() {


        @Override
        public void onConnected(String uuid) {
            super.onConnected(uuid);
            if (mediaCallBack != null) {
                mediaCallBack.onConnect();
            }
        }

        @Override
        public void onCallConnected() {
            if (mediaCallBack != null) {
                mediaCallBack.onCallConnect();
            }
        }

        @Override
        public void onLocalVideo(String uuid, VCRTCView view) {
            myUUID = uuid;
            if (mediaCallBack != null) {
                mediaCallBack.onLocalVideo(uuid, view);
            }
        }

        @Override
        public void onRemoteVideo(String uuid, VCRTCView view) {
            if (mediaCallBack != null) {
                mediaCallBack.onRemoteVideo(uuid, view);
            }
        }


        @Override
        public void onLocalStream(String uuid, String streamURL, String streamType) {
            myUUID = uuid;
            if (mediaCallBack != null && "camera".equalsIgnoreCase(streamType)) {
                mediaCallBack.onLocalStream(uuid, streamURL);
            }
        }

        @Override
        public void onAddView(String uuid, VCRTCView view, String viewType) {
            if (mediaCallBack != null) {
                mediaCallBack.onAddView(uuid, view, viewType);
            }
        }

        @Override
        public void onRemoveView(String uuid, VCRTCView view) {
            if (mediaCallBack != null) {
                mediaCallBack.onRemoveView(uuid, view);
            }
        }

        @Override
        public void onRemoteStream(String uuid, String streamURL, String streamType) {
            if (mediaCallBack != null) {
                mediaCallBack.onRemoteStream(uuid, streamURL, streamType);
            }
        }

        @Override
        public void onAddParticipant(Participant participant) {
            if (mediaCallBack != null) {
                mediaCallBack.onAddParticipant(participant);
            }
            if (conferenceCallBack != null) {
                conferenceCallBack.onAddParticipant(participant);
            }
            if (!participants.containsKey(participant.getUuid())) {
                participants.put(participant.getUuid(), participant);
            }
        }

        @Override
        public void onUpdateParticipant(Participant participant) {
            if (mediaCallBack != null) {
                mediaCallBack.onUpdateParticipant(participant);
            }
            if (conferenceCallBack != null) {
                conferenceCallBack.onUpdateParticipant(participant);
            }
            participants.put(participant.getUuid(), participant);
        }

        @Override
        public void onRemoveParticipant(String uuid) {
            if (mediaCallBack != null) {
                mediaCallBack.onRemoveParticipant(uuid);
            }
            if (conferenceCallBack != null) {
                conferenceCallBack.onRemoveParticipant(uuid);
            }
            if (participants.containsKey(uuid)) {
                participants.remove(uuid);
            }
        }

        @Override
        public void onLayoutUpdate(String layout, String hostLayout, String guestLayout) {
            if (mediaCallBack != null) {
                mediaCallBack.onLayoutUpdate(layout, hostLayout, guestLayout);
            }
        }

        @Override
        public void onLayoutUpdateParticipants(List<String> participants) {
            if (mediaCallBack != null) {
                mediaCallBack.onLayoutUpdateParticipants(participants);
            }
        }

        @Override
        public void onRoleUpdate(String role) {
            isHost = role.equals("HOST");
            call.setHost(isHost);
            if (mediaCallBack != null) {
                mediaCallBack.onRoleUpdate(role);
            }
            if (conferenceCallBack != null) {
                conferenceCallBack.onRoleUpdate(role);
            }
        }

        @Override
        public void onPresentation(boolean isActive, String uuid) {
            if (isActive) {
                if (!isForeground) {
                    if (VCWindowManager.getFloatButton() != null) {
                        VCWindowManager.getFloatButton().openMeetingRoom(true);
                    }
                    VCWindowManager.removeFloatButton(getApplicationContext());
                }
            }
            if (mediaCallBack != null) {
                mediaCallBack.onPresentation(isActive, uuid);
            }
        }

        @Override
        public void onPresentationReload(String url) {
            if (mediaCallBack != null) {
                mediaCallBack.onPresentationReload(url);
            }
        }

        @Override
        public void onScreenShareState(boolean isActive) {
            if (mediaCallBack != null) {
                mediaCallBack.onScreenShareState(isActive);
            }
        }

        @Override
        public void onRecordState(boolean isActive) {
            if (mediaCallBack != null) {
                mediaCallBack.onRecordState(isActive);
            }
        }

        @Override
        public void onLiveState(boolean isActive) {
            if (mediaCallBack != null) {
                mediaCallBack.onLiveState(isActive);
            }
        }

        @Override
        public void onStageVoice(List<Stage> stages) {
            vadStage = stages;
            if (conferenceCallBack != null) {
                conferenceCallBack.onStageVoice(stages);
            }
        }

        @Override
        public void onConferenceUpdate(ConferenceStatus status) {
            isAllMute = status.isGuests_muted();
            isLock = status.isLocked();
            if (conferenceCallBack != null) {
                conferenceCallBack.onConferenceUpdate(status);
            }
        }

        @Override
        public void onDisconnect(String reason) {
            if (isForeground) {
                if (Locale.getDefault().getLanguage().equals("zh")) {
                    Map<String, String> tipsMap = VCUtil.getTipsMessageMap();
                    reason = tipsMap.get(reason);
                }
                showBeDisconnectedToast(reason);
            }
        }

        @Override
        public void onError(ErrorCode error, String description) {
            if (error.equals(ErrorCode.noCameraFound)) {
                runOnUiThread(() -> {
                    if (isForeground)
                        showErrorDialog(getString(R.string.no_camera_found), getString(R.string.check_camera));
                });
            } else if (error.equals(ErrorCode.joinConferenceFailed)) {
                if (Locale.getDefault().getLanguage().equals("zh")) {
                    Map<String, String> tipsMap = VCUtil.getTipsMessageMap();
                    if (!TextUtils.isEmpty(tipsMap.get(description)))
                        description = tipsMap.get(description);
                }
                String reason = description;
                runOnUiThread(() -> showBeDisconnectedToast(reason));
            }
        }


        @Override
        public void onPresentationShareState(boolean isSuccess, String reason) {
            super.onPresentationShareState(isSuccess, reason);
            runOnUiThread(() -> {
                if (isSuccess){
                    mediaCallBack.onPresentationSuccess();
                }else {
                    vcrtc.stopPresentation();
                    if (mediaSimulcastFragment != null) {
                        mediaSimulcastFragment.toggleShare();
                    }
                    if (mediaFragment != null) {
                        mediaFragment.toggleShare();
                    }
                    if (mediaShiTongFragment != null) {
                        mediaShiTongFragment.toggleShare();
                    }
                }
            });
        }
        private String whiteUUID = "";
        @Override
        public void onWhiteboardStart(String uuid, boolean isMark) {
            super.onWhiteboardStart(uuid, isMark);
            if (!whiteUUID.equals(uuid)) {
                whiteUUID = uuid;
                mediaCallBack.onWhiteboardStart(uuid, isMark);
            }
            isShowWhite = true;
        }

        @Override
        public void onWhiteboardImageUpdate(Bitmap bitmap) {
            super.onWhiteboardImageUpdate(bitmap);
            mediaCallBack.onWhiteboardImageUpdate(bitmap);
        }

        @Override
        public void onWhiteboardStop() {
            super.onWhiteboardStop();
            whiteUUID = "";
            mediaCallBack.onWhiteboardStop();
            isShowWhite = false;
        }

        @Override
        public void onWhiteboardAddPayload(int cmdid, WhiteboardPayload payload) {
            super.onWhiteboardAddPayload(cmdid, payload);
            mediaCallBack.onWhiteboardAddPayload(cmdid, payload);
        }

        @Override
        public void onWhiteboardClearPayload() {
            super.onWhiteboardClearPayload();
            mediaCallBack.onWhiteboardClearPayload();
        }

        @Override
        public void onWhiteboardDeletePayload(int cmdid) {
            super.onWhiteboardDeletePayload(cmdid);
            mediaCallBack.onWhiteboardDeletePayload(cmdid);
        }
    };
    private MediaCallBack mediaCallBack;
    private ConferenceCallBack conferenceCallBack;

    public void setMediaCallBack(MediaCallBack mediaCallBack) {
        this.mediaCallBack = mediaCallBack;
    }

    public void setConferenceCallBack(ConferenceCallBack conferenceCallBack) {
        this.conferenceCallBack = conferenceCallBack;
    }
    public interface MediaCallBack {
        void onLocalVideo(String uuid, VCRTCView view);

        void onRemoteVideo(String uuid, VCRTCView view);

        void onLocalStream(String uuid, String streamURL);

        void onAddView(String uuid, VCRTCView view, String viewType);

        void onRemoveView(String uuid, VCRTCView view);

        void onRemoteStream(String uuid, String streamURL, String streamType);

        void onAddParticipant(Participant participant);

        void onUpdateParticipant(Participant participant);

        void onRemoveParticipant(String uuid);

        void onLayoutUpdate(String layout, String hostLayout, String guestLayout);

        void onLayoutUpdateParticipants(List<String> participants);

        void onPresentation(boolean isActive, String uuid);

        void onPresentationReload(String url);

        void onScreenShareState(boolean isActive);

        void onRecordState(boolean isActive);

        void onLiveState(boolean isActive);

        void onRoleUpdate(String role);

        void onConnect();

        void onCallConnect();

        void onPresentationSuccess();

        void onWhiteboardAddPayload(int cmdid, WhiteboardPayload payload);

        void onWhiteboardClearPayload();

        void onWhiteboardDeletePayload(int cmdid);

        void onWhiteboardImageUpdate(Bitmap bitmap);

        void onWhiteboardStart(String uuid, boolean isMark);

        void onWhiteboardStop();
    }

    public interface ConferenceCallBack {
        void onAddParticipant(Participant participant);

        void onUpdateParticipant(Participant participant);

        void onRemoveParticipant(String uuid);

        void onStageVoice(List<Stage> stages);

        void onConferenceUpdate(ConferenceStatus status);

        void onRoleUpdate(String role);
    }


    /**
     * 加入白板
     */
    public void joinMark() {
        if (mediaSimulcastFragment != null) {
            mediaSimulcastFragment.joinMark();
        }
//        if (mediaFragment != null) {
//            mediaFragment.joinMark();
//        }
//
        if (mediaShiTongFragment != null) {
            mediaShiTongFragment.joinMark();
        }
    }

    /**
     * 发起标注
     */
    public void startMark() {
        if (mediaSimulcastFragment != null) {
            mediaSimulcastFragment.startMark();
        }
//        if (mediaFragment != null) {
//            mediaFragment.startMark();
//        }
        if (mediaShiTongFragment != null) {
            mediaShiTongFragment.startMark();
        }
    }

    public void updateMark() {
        if (mediaSimulcastFragment != null) {
            mediaSimulcastFragment.updateMark();
        }
//        if (mediaFragment != null) {
//            mediaFragment.updateMark();
//        }
        if (mediaShiTongFragment != null) {
            mediaShiTongFragment.updateMark();
        }
    }


    public void switchMark() {
        if (mediaSimulcastFragment != null) {
            mediaSimulcastFragment.switchMark();
        }
//        if (mediaFragment != null) {
//            mediaFragment.switchMark();
//        }
        if (mediaShiTongFragment != null) {
            mediaShiTongFragment.switchMark();
        }
    }


    /**
     * 加入白板
     */
    public void joinBoard() {
        if (mediaSimulcastFragment != null) {
            mediaSimulcastFragment.joinBoard();
        }
//        if (mediaFragment != null) {
//            mediaFragment.joinBroad();
//        }
        if (mediaShiTongFragment != null) {
            mediaShiTongFragment.joinBoard();
        }
    }

    public void handleDisplay(boolean isShow){
        if (mediaShiTongFragment != null) {
            mediaShiTongFragment.handleDisplay(isShow);
        }
        if (mediaSimulcastFragment != null) {
            mediaSimulcastFragment.handleDisplay(isShow);
        }
    }
}
