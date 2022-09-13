package com.example.alan.sdkdemo.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.alan.sdkdemo.R;
import com.example.alan.sdkdemo.util.GlideEngine;
import com.example.alan.sdkdemo.util.WhiteBoardUtil;
import com.example.alan.sdkdemo.widget.ZoomFrameLayout;
import com.example.alan.sdkdemo.widget.ZoomViewPager;
import com.huantansheng.easyphotos.EasyPhotos;
import com.huantansheng.easyphotos.models.album.entity.Photo;
import com.qw.soul.permission.SoulPermission;
import com.qw.soul.permission.bean.Permission;
import com.qw.soul.permission.callbcak.CheckRequestPermissionListener;
import com.vcrtc.VCRTC;
import com.vcrtc.VCRTCView;
import com.vcrtc.adapters.StatsAdapter;
import com.vcrtc.entities.Call;
import com.vcrtc.entities.MediaStats;
import com.vcrtc.entities.Participant;
import com.vcrtc.entities.People;
import com.vcrtc.entities.StatsItemBean;
import com.vcrtc.entities.WhiteboardPayload;
import com.vcrtc.listeners.DoubleClickListener;
import com.vcrtc.utils.BitmapUtil;
import com.vcrtc.utils.PDFUtil;
import com.vcrtc.utils.VCUtil;
import com.vcrtc.webrtc.RTCManager;
import com.vcrtc.widget.WhiteBoardView;

import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.VideoFrame;
import org.webrtc.YuvHelper;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

public class MediaShiTongFragment extends Fragment implements View.OnClickListener {

    private final int PDF_PICKER_REQUEST = 46709;
    private final int HIDE_BAR = 1;
    private final int REFRESH_TIME = 2;
    private final int REFRESH_STATS = 3;
    private final int START_SCREEN_SHARE = 4;

    private View rootView;
    private LinearLayout llLiving, llRecording;
    public RelativeLayout rlTopBar, rlToolBar, rlLoading, rlUnStick, rlShareScreen, rlCallOut;
    private ZoomFrameLayout flBigVideo;
    private LinearLayout llSmallVideo, llBigName;
    private TextView tvTime, tvChanel, tvBigName, tvPeopleNum, tvCallName;
    private LinearLayout llStats;
    private ImageView ivMuteAudio, ivMuteVideo, ivSwitchCamera, ivAudioMedol, ivShare, ivParticipants, ivMore, ivHangup, ivCircle, ivSignal, ivCloseVideo, ivAudioImg, ivCloseVideoBig, ivBigMute, ivCancel;
    private ZoomViewPager vpShare;
    private PopupWindow popupWindowShare, popupWindowMore, popupWindowStats;

    private FrameLayout.LayoutParams bigLayoutParams;
    private FrameLayout.LayoutParams videoLayoutParams;
    private LinearLayout.LayoutParams smallLayoutParams;

    private VCRTC vcrtc;
    private VCRTCView localView, bigView;
    private Call call;

    private Timer hideBarTimer;
    private Timer durationTimer;
    private Timer getStatsTimer;
    private TimerTask hideBarTimerTask;
    private TimerTask getStatsTimerTask;
    private int time;
    public boolean isShowBar, isHideSmallView, isMuteAudio, isMuteVideo, isAudioModel, isFront, isShare, isShareScreen, isPresentation;
    private boolean isRecord, isLive;
    /**
     * 参会人列表，不包括自己
     **/
    private Map<String, People> peoples;
    /**
     * 参会人列表，所有人
     **/
    private Map<String, People> showPeoples;
    private List<String> participantsSort;
    private People me;
    private String presentationStreamURL;
    private String bigUUID;
    private String stickUUID;
    private boolean isStick;
    private boolean makeMeBig;
    private List<String> imagePathList;
    private int pictureIndex;
    private int positionPixels = 0;
    private List<StatsItemBean> itemBeanList;
    private StatsAdapter adapter;
    private long clickCount = 0;
    private BitmapUtil bitmapUtil;
    private Activity mActivity;
    VCRTCView testView;
    private FrameLayout testFrameLayout, flMarkBackground;

    private WhiteBoardUtil whiteBoardUtil;
    String whiteBoardUUID = "";

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_media_shitong, container, false);

        initView();
        initData();
        showBar();
        showLoading();
        initMediaStatsWindow();
        return rootView;
    }

    private void initView() {
        rlCallOut = rootView.findViewById(R.id.rl_call_out);
        rlLoading = rootView.findViewById(R.id.layout_loading);
        rlTopBar = rootView.findViewById(R.id.rl_top_bar);
        rlToolBar = rootView.findViewById(R.id.rl_tool_bar);
        llLiving = rootView.findViewById(R.id.ll_living);
        llRecording = rootView.findViewById(R.id.ll_recording);
        rlUnStick = rootView.findViewById(R.id.rl_unstick);
        rlShareScreen = rootView.findViewById(R.id.rl_share_screen);
        flBigVideo = rootView.findViewById(R.id.fl_big_video);
        llSmallVideo = rootView.findViewById(R.id.ll_small_video);
        llBigName = rootView.findViewById(R.id.ll_name_big);
        tvCallName = rootView.findViewById(R.id.tv_call_name);
        tvTime = rootView.findViewById(R.id.tv_time);
        tvChanel = rootView.findViewById(R.id.tv_room_num);
        tvBigName = rootView.findViewById(R.id.tv_name_big);
        tvPeopleNum = rootView.findViewById(R.id.tv_people_num);
        llStats = rootView.findViewById(R.id.ll_stats);
        ivMuteAudio = rootView.findViewById(R.id.iv_mute_audio);
        ivMuteVideo = rootView.findViewById(R.id.iv_mute_video);
        ivSwitchCamera = rootView.findViewById(R.id.iv_switch_camera);
        ivAudioMedol = rootView.findViewById(R.id.iv_audio_model);
        ivShare = rootView.findViewById(R.id.iv_share);
        ivParticipants = rootView.findViewById(R.id.iv_participants);
        flMarkBackground = rootView.findViewById(R.id.fl_mark_background);
        ivMore = rootView.findViewById(R.id.iv_more);
        ivHangup = rootView.findViewById(R.id.iv_hangup);
        ivCircle = rootView.findViewById(R.id.iv_circle);
        ivSignal = rootView.findViewById(R.id.iv_signal);
        ivAudioImg = rootView.findViewById(R.id.iv_audio_img);
        ivCloseVideoBig = rootView.findViewById(R.id.iv_close_video_img);
        ivBigMute = rootView.findViewById(R.id.iv_mute_big);
        ivCancel = rootView.findViewById(R.id.iv_call_cancel);
        vpShare = rootView.findViewById(R.id.vp_share);

        ivAudioMedol.setVisibility(View.VISIBLE);
        rlUnStick.setOnClickListener(this);
        llStats.setOnClickListener(this);
        ivMuteAudio.setOnClickListener(this);
        ivMuteVideo.setOnClickListener(this);
        ivSwitchCamera.setOnClickListener(this);
        ivAudioMedol.setOnClickListener(this);
        ivShare.setOnClickListener(this);
        ivParticipants.setOnClickListener(this);
        ivMore.setOnClickListener(this);
        ivHangup.setOnClickListener(this);
        ivCancel.setOnClickListener(this);
        testFrameLayout = rootView.findViewById(R.id.test_frame);

        flBigVideo.setOnClickListener(() -> {
            clickCount++;
            mHandler.postDelayed(() -> {
                if (clickCount == 1) {
                    if (isShowBar) {
                        hideBar();
                    } else {
                        showBar();
                    }
                } else if (clickCount == 2) {
                    if (!isPresentation && !isShareScreen && !isAudioModel) {
                        if (isStick) {
                            setUnStick();
                            showToast(getString(R.string.main_screen_unlock));
                        } else {
                            setStick(bigUUID);
                        }
                    }
                }
                mHandler.removeCallbacksAndMessages(null);
                clickCount = 0;
            }, 400);
        });

        /**
         * 图片分享viewpager滑动监听
         */
        vpShare.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (positionOffsetPixels > 0) {
                    positionPixels = positionOffsetPixels;
                }
            }

            @Override
            public void onPageSelected(int position) {
                pictureIndex = position;
                if (isPDF) {
                    pdfBitmap = pdfUtil.openPage(pictureIndex);
                }

            }

            @Override
            public void onPageScrollStateChanged(int state) {
                switch (state) {
                    case ViewPager.SCROLL_STATE_IDLE:
                        //无动作、初始状态
                        sendSharePicture();
                        if (positionPixels == 0) {
                            showToast(getString(R.string.toast_slippery));
                        }
                        positionPixels = 0;
                        break;
                    case ViewPager.SCROLL_STATE_DRAGGING:
                        //点击、滑屏
                        positionPixels = 0;
                        break;
                    case ViewPager.SCROLL_STATE_SETTLING:
                        //释放
                        break;
                    default:
                }
                mHandler.removeCallbacksAndMessages(null);
                clickCount = 0;
            }
        });

    }

    private void handleVideoFrame(VideoFrame videoFrame) {
        videoFrame.retain();
        VideoFrame.Buffer videoFrameBuffer = videoFrame.getBuffer();
        int width = videoFrameBuffer.getWidth();
        int height = videoFrameBuffer.getHeight();
        int rotation = videoFrame.getRotation();
        long timestampNs = videoFrame.getTimestampNs();

        int bufferSize = width * height * 3 / 2;
        ByteBuffer dstBuffer = ByteBuffer.allocateDirect(bufferSize);

        fillBuffer(dstBuffer, videoFrameBuffer);
        byte[] i420 = buffer2byte(dstBuffer);
        videoFrame.release();
        Log.d("i420_test", "handleVideoFrame: " + i420.length);
    }

    private static void fillBuffer(ByteBuffer dstBuffer, VideoFrame.Buffer srcBuffer) {
        VideoFrame.I420Buffer i420 = srcBuffer.toI420();
        YuvHelper.I420Copy(i420.getDataY(), i420.getStrideY(), i420.getDataU(), i420.getStrideU(),
                i420.getDataV(), i420.getStrideV(), dstBuffer, i420.getWidth(), i420.getHeight());
        i420.release();
    }

    public static byte[] buffer2byte(ByteBuffer byteBuffer) {
        byteBuffer.position(0);
        int len = byteBuffer.limit() - byteBuffer.position();
        byte[] bytes = new byte[len];
        if (byteBuffer.isReadOnly()) {
            return null;
        } else {
            byteBuffer.get(bytes);
        }
        return bytes;
    }

    //        testView = new VCRTCView(getActivity());
//        testView.setVideoCallBackListener(videoFrame -> {
//            //使用VideoFrame时，如果不是复制i420，应先retain()增加引用计数，使用完后再release()，异步处理，不要形成堵塞
//            Log.d("i420_test", "setVideoCallBackListener: " + videoFrame + "  main: " + (Looper.getMainLooper() == Looper.myLooper()));
//            handleVideoFrame(videoFrame);
//            return videoFrame;
//        }, VCRTCView.EXTERNAL_HANDLE);
//        testFrameLayout.addView(testView);
    private void initData() {
        call = ((ZJConferenceActivity) getActivity()).call;
        vcrtc = ((ZJConferenceActivity) getActivity()).vcrtc;
        whiteBoardUtil = new WhiteBoardUtil(vcrtc, (ZJConferenceActivity) mActivity);
        whiteBoardUtil.initWhiteBoardView(rootView);
        pdfUtil = new PDFUtil(getActivity(), true, 1920, 1080);
        tvChanel.setText(call.getChannel());

        if (ZJConferenceActivity.joinMuteAudio) {
            ivMuteAudio.setSelected(true);
        }

        int bigVideoWidth;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int screenHeight = dm.heightPixels;
        int screenWidth = dm.widthPixels;
        if (screenWidth * 9 / 16 >= screenHeight) {
            bigVideoWidth = screenHeight * 16 / 9;
        } else {
            bigVideoWidth = screenWidth;
        }

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) llSmallVideo.getLayoutParams();
        params.setMarginStart((screenWidth - bigVideoWidth) / 2);

        bigLayoutParams = new FrameLayout.LayoutParams(bigVideoWidth, ViewGroup.LayoutParams.MATCH_PARENT);
        bigLayoutParams.gravity = Gravity.CENTER;

        videoLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        smallLayoutParams = new LinearLayout.LayoutParams(bigVideoWidth / 5 - VCUtil.dp2px(getActivity().getApplicationContext(), 1),
                (bigVideoWidth / 5 - VCUtil.dp2px(getActivity().getApplicationContext(), 1)) * 9 / 16);
        smallLayoutParams.setMargins(0, 0, VCUtil.dp2px(getActivity().getApplicationContext(), 1), 0);

        isMuteAudio = false;
        isMuteVideo = false;
        isAudioModel = false;
        isFront = true;
        isShare = false;
        isShareScreen = false;
        peoples = new LinkedHashMap<>();
        showPeoples = new LinkedHashMap<>();
        imagePathList = new ArrayList<>();
        itemBeanList = new ArrayList<>();

        bigView = new VCRTCView(getActivity());
        bigView.setZOrder(0);
        bigView.setObjectFit("contain");
        if (ZJConferenceActivity.allowCamera)
            bigView.setMirror(true);

        flBigVideo.addView(bigView, bigLayoutParams);

        ((ZJConferenceActivity) getActivity()).setMediaCallBack(callBack);
        bitmapUtil = new BitmapUtil(getActivity());
    }

    public void refreshAudioVideoIcon() {
        if (!ZJConferenceActivity.allowRecordAudio) {
            ivMuteAudio.setSelected(true);
        }
        if (!ZJConferenceActivity.allowCamera) {
            ivMuteVideo.setSelected(true);
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.iv_mute_audio) {
            toggleMuteAudio();
            showBar();
        } else if (i == R.id.iv_mute_video) {
            if (!isAudioModel) {
                toggleMuteVideo();
            }
            showBar();
        } else if (i == R.id.iv_switch_camera) {
            switchCamera();
            showBar();
        } else if (i == R.id.iv_audio_model) {
            if (!isMuteVideo) {
                toggleAudioModel();
            }
            showBar();
        } else if (i == R.id.iv_share) {
            toggleShare();
        } else if (i == R.id.iv_more) {
            showMoreWindow();
            showBar();
        } else if (i == R.id.iv_hangup) {
            showDisconnectDialog();
            hideBar();
        } else if (i == R.id.ll_stats) {
            startGetStats();
            showMediaStatsWindow();
        } else if (i == R.id.iv_close) {
            stopGetStats();
            popupWindowStats.dismiss();
        } else if (i == R.id.rl_unstick) {
            setUnStick();
            showToast(getString(R.string.main_screen_unlock));
        } else if (i == R.id.iv_call_cancel) {
            disconnect();
        } else if (i == R.id.iv_participants) {
            ((ZJConferenceActivity) getActivity()).showParticipant();
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HIDE_BAR:
                    hideBar();
                    break;
                case REFRESH_TIME:
                    refreshTime();
                    break;
                case REFRESH_STATS:
                    refreshStats((double) msg.obj);
                    break;
                case START_SCREEN_SHARE:
                    startScreenShare();
                    break;
                default:
            }
        }
    };

    /**
     * 获取会中通话状态信息
     */
    private void startGetStats() {
        if (getStatsTimer == null) {
            getStatsTimer = new Timer();
        }
        if (getStatsTimerTask == null) {
            getStatsTimerTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        itemBeanList.clear();
                        double fractionLost = 0;
                        List<MediaStats> stats = vcrtc.getMediaStatistics();

                        for (MediaStats mediaStats : stats) {
                            if (mediaStats.getMediaType().equals("audio") && mediaStats.getDirection().equals("out")) {
                                StatsItemBean bean = new StatsItemBean();
                                bean.setTerminal(getString(R.string.stats_local));
                                bean.setChanel(getString(R.string.stats_audio_send));
                                bean.setCodec(mediaStats.getCodec());
                                bean.setResolution("--");
                                bean.setFrameRate("--");
                                bean.setBitRate(mediaStats.getBitrate() + "");
                                bean.setJitter(mediaStats.getJitter() + "ms");
                                bean.setFractionLost(mediaStats.getFractionLost());
                                itemBeanList.add(bean);
                                break;
                            }
                        }
                        for (MediaStats mediaStats : stats) {
                            if (mediaStats.getMediaType().equals("audio") && mediaStats.getDirection().equals("in")) {
                                StatsItemBean bean = new StatsItemBean();
                                bean.setTerminal(getString(R.string.stats_remote));
                                bean.setChanel(getString(R.string.stats_audio_recv));
                                bean.setCodec(mediaStats.getCodec());
                                bean.setResolution("--");
                                bean.setFrameRate("--");
                                bean.setBitRate(mediaStats.getBitrate() + "");
                                bean.setJitter(mediaStats.getJitter() + "ms");
                                bean.setFractionLost(mediaStats.getFractionLost());
                                itemBeanList.add(bean);
                                break;
                            }
                        }
                        for (MediaStats mediaStats : stats) {
                            if (mediaStats.getMediaType().equals("video") && mediaStats.getDirection().equals("out")) {
                                StatsItemBean bean = new StatsItemBean();
                                bean.setTerminal(getString(R.string.stats_local));
                                bean.setChanel(getString(R.string.stats_video_send));
                                bean.setCodec(mediaStats.getCodec());
                                bean.setResolution(mediaStats.getResolution());
                                bean.setFrameRate(mediaStats.getFrameRate() + "");
                                bean.setBitRate(mediaStats.getBitrate() + "");
                                bean.setJitter(mediaStats.getJitter() + "ms");
                                bean.setFractionLost(mediaStats.getFractionLost());
                                itemBeanList.add(bean);
                            }
                        }
                        for (MediaStats mediaStats : stats) {
                            if (mediaStats.getMediaType().equals("video") && mediaStats.getDirection().equals("in")) {
                                StatsItemBean bean = new StatsItemBean();
                                for (String uuid : peoples.keySet()) {
                                    if (mediaStats.getUuid().equals(uuid)) {
                                        bean.setTerminal(peoples.get(uuid).getName());
                                    }
                                }
                                bean.setChanel(getString(R.string.stats_video_recv));
                                bean.setCodec(mediaStats.getCodec());
                                bean.setResolution(mediaStats.getResolution());
                                bean.setFrameRate(mediaStats.getFrameRate() + "");
                                bean.setBitRate(mediaStats.getBitrate() + "");
                                bean.setJitter(mediaStats.getJitter() + "ms");
                                bean.setFractionLost(mediaStats.getFractionLost());
                                itemBeanList.add(bean);
                            }
                        }

                        for (MediaStats mediaStats : stats) {
                            if ("presentation".equals(mediaStats.getMediaType()) && "out".equals(mediaStats.getDirection())) {
                                StatsItemBean bean = new StatsItemBean();
                                bean.setCodec(mediaStats.getCodec());
                                bean.setResolution(mediaStats.getResolution());
                                bean.setFrameRate(mediaStats.getFrameRate() + "");
                                bean.setBitRate(mediaStats.getBitrate() + "");
                                bean.setJitter(mediaStats.getJitter() + "ms");
                                bean.setPacketsLost(String.valueOf(mediaStats.getPacketsLost()));
                                bean.setFractionLost(mediaStats.getFractionLost());
                                Log.d("media_presentation", "分辨率: " + bean.getResolution() + " 帧率： " + bean.getFrameRate());
                            }
                        }
                        for (MediaStats mediaStats : stats) {
                            fractionLost += mediaStats.getFractionLost();
                        }
                        Message msg = Message.obtain();
                        msg.what = REFRESH_STATS;
                        msg.obj = fractionLost;
                        mHandler.sendMessage(msg);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
        }
        if (getStatsTimer != null && getStatsTimerTask != null) {
            try {
                getStatsTimer.schedule(getStatsTimerTask, 0, 2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void stopGetStats() {
        if (getStatsTimer != null) {
            getStatsTimer.cancel();
            getStatsTimer = null;
        }
        if (getStatsTimerTask != null) {
            getStatsTimerTask.cancel();
            getStatsTimerTask = null;
        }
    }

    private void refreshStats(double ractionLost) {
        if (ractionLost == 0) {
            ivSignal.setImageResource(R.mipmap.icon_sign_5);
        } else if (ractionLost > 0 && ractionLost <= 1) {
            ivSignal.setImageResource(R.mipmap.icon_sign_4);
        } else if (ractionLost > 1 && ractionLost <= 2) {
            ivSignal.setImageResource(R.mipmap.icon_sign_3);
        } else if (ractionLost > 2 && ractionLost <= 5) {
            ivSignal.setImageResource(R.mipmap.icon_sign_2);
        } else if (ractionLost > 5 && ractionLost <= 10) {
            ivSignal.setImageResource(R.mipmap.icon_sign_1);
        } else {
            ivSignal.setImageResource(R.mipmap.icon_sign_0);
        }
        adapter.notifyDataSetChanged();
    }

    private void startTheTime() {
        time = 0;

        if (durationTimer == null) {
            durationTimer = new Timer();
        }

        durationTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(REFRESH_TIME);
            }
        }, 0, 1000);
    }

    private void refreshTime() {
        time++;
        int hour = time / 3600;
        int minute = time % 3600 / 60;
        int second = time % 60;
        String timeString = String.format(Locale.CHINA, "%02d:%02d:%02d", hour, minute, second);
        tvTime.setText(timeString);
    }

    private void showLoading() {
        Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.anim_loading_circle);
        LinearInterpolator interpolator = new LinearInterpolator();
        animation.setInterpolator(interpolator);
        rlLoading.setVisibility(View.VISIBLE);
        ivCircle.startAnimation(animation);
    }

    private void stopLoading() {
        ivCircle.clearAnimation();
        rlLoading.setVisibility(View.GONE);
    }

    private void showBar() {
        isShowBar = true;
        rlTopBar.setVisibility(View.VISIBLE);
        rlToolBar.setVisibility(View.VISIBLE);

        releaseBarTimer();

        if (hideBarTimer == null) {
            hideBarTimer = new Timer();
        }

        if (hideBarTimerTask == null) {
            hideBarTimerTask = new TimerTask() {
                @Override
                public void run() {
                    mHandler.sendEmptyMessage(HIDE_BAR);
                }
            };
        }

        if (hideBarTimer != null && hideBarTimerTask != null) {
            hideBarTimer.schedule(hideBarTimerTask, 5000);
        }
    }

    private void hideBar() {
        isShowBar = false;

        rlTopBar.setVisibility(View.INVISIBLE);
        rlToolBar.setVisibility(View.INVISIBLE);

        if (popupWindowMore != null && popupWindowMore.isShowing())
            popupWindowMore.dismiss();
        if (popupWindowShare != null && popupWindowShare.isShowing())
            popupWindowShare.dismiss();

        releaseBarTimer();
    }

    private void releaseBarTimer() {
        if (hideBarTimer != null) {
            hideBarTimer.cancel();
            hideBarTimer = null;
        }

        if (hideBarTimerTask != null) {
            hideBarTimerTask.cancel();
            hideBarTimerTask = null;
        }
    }

    private void releaseDurationTimer() {
        if (durationTimer != null) {
            durationTimer.cancel();
            durationTimer = null;
        }
    }

    private void releaseStatsTimer() {
        if (getStatsTimer != null) {
            getStatsTimer.cancel();
            getStatsTimer = null;
        }
    }

    private void hideShowSmallView() {
        llSmallVideo.setVisibility(isHideSmallView ? View.VISIBLE : View.GONE);
        if (!isHideSmallView) {
            isHideSmallView = true;
            llSmallVideo.removeAllViews();
            tvViewInView.setText(R.string.more_show_small_view);
        } else {
            isHideSmallView = false;
            sortPeopels();
            tvViewInView.setText(R.string.more_hide_small_view);
        }
    }

    /**
     * 锁定屏幕
     *
     * @param uuid
     */
    public void setStick(String uuid) {
        if (uuid != null && !"".equals(uuid)) {
            if (uuid.equals(me.getUuid())) {
                makeMeBig = true;
                sortPeopels();
            } else {
                makeMeBig = false;
                vcrtc.setParticipantStick(uuid, true);
            }
            stickUUID = uuid;
            rlUnStick.setVisibility(View.VISIBLE);
            showToast(getString(R.string.main_screen_lock_cancel));
            isStick = true;
        }
    }

    /**
     * 解锁屏幕
     */
    private void setUnStick() {
        if (isStick) {
            makeMeBig = false;
            if (stickUUID != null) {
                if (stickUUID.equals(me.getUuid())) {
                    sortPeopels();
                } else {
                    vcrtc.setParticipantStick(stickUUID, false);
                }
                rlUnStick.setVisibility(View.INVISIBLE);
            }
            isStick = false;
        }
    }

    private void toggleMuteAudio() {
        if (!ZJConferenceActivity.allowRecordAudio) {
            checkRecordAudioPermission();
            return;
        }
        vcrtc.setAudioEnable(isMuteAudio);
        ivMuteAudio.setSelected(!isMuteAudio);
        isMuteAudio = !isMuteAudio;
    }

    private void checkRecordAudioPermission() {
        SoulPermission.getInstance().checkAndRequestPermission(Manifest.permission.RECORD_AUDIO,
                new CheckRequestPermissionListener() {
                    @Override
                    public void onPermissionOk(Permission permission) {
                        ZJConferenceActivity.allowRecordAudio = true;
                        ivMuteAudio.setSelected(false);
                        if (ZJConferenceActivity.allowCamera) {
                            vcrtc.setCallType(VCRTC.CallType.video);
                        } else {
                            vcrtc.setCallType(VCRTC.CallType.recvAndSendAudioBitmap);
                        }
                        vcrtc.reconnectOnlyMediaCall();
                    }

                    @Override
                    public void onPermissionDenied(Permission permission) {
                        if (!permission.shouldRationale()) {
                            new AlertDialog.Builder(getActivity())
                                    .setTitle(R.string.permission_tips)
                                    .setMessage(String.format(getString(R.string.permission_message_no_record_audio), getString(R.string.app_name)))
                                    .setPositiveButton(R.string.permission_go, (dialog, which) -> {
                                        SoulPermission.getInstance().goApplicationSettings();
                                        dialog.dismiss();
                                    })
                                    .setNegativeButton(R.string.permission_cancel, (dialog, which) -> {
                                        dialog.dismiss();
                                    })
                                    .create()
                                    .show();
                        }
                    }
                });
    }

    Handler handlerVideo = new Handler();
    Runnable runnableVideo = new Runnable() {
        @Override
        public void run() {
            ivCloseVideo.setVisibility(View.GONE);
            if (me.isBig() || peoples.size() == 0) {
                ivCloseVideoBig.setVisibility(View.GONE);
            }
        }
    };

    private void toggleMuteVideo() {
        if (!ZJConferenceActivity.allowCamera) {
            checkCameraPermission();
            return;
        }
        vcrtc.updateVideoImage(((ZJConferenceActivity) getActivity()).closeVideoBitmap);
        vcrtc.setVideoEnable(isMuteVideo, true);
        ivMuteVideo.setSelected(!isMuteVideo);
        isMuteVideo = !isMuteVideo;
        if (isMuteVideo) {
            handlerVideo.removeCallbacks(runnableVideo);
            ivCloseVideo.setVisibility(View.VISIBLE);
            if (me.isBig() || peoples.size() == 0) {
                ivCloseVideoBig.setVisibility(View.VISIBLE);
            }
        } else {
            handlerVideo.postDelayed(runnableVideo, 700);
        }
    }

    private void checkCameraPermission() {
        SoulPermission.getInstance().checkAndRequestPermission(Manifest.permission.CAMERA,
                new CheckRequestPermissionListener() {
                    @Override
                    public void onPermissionOk(Permission permission) {
                        ZJConferenceActivity.allowCamera = true;
                        ivMuteVideo.setSelected(false);
                        if (ZJConferenceActivity.allowRecordAudio) {
                            vcrtc.setCallType(VCRTC.CallType.video);
                        } else {
                            vcrtc.setCallType(VCRTC.CallType.recvAndSendVideo);
                        }
                        vcrtc.reconnectOnlyMediaCall();
                    }

                    @Override
                    public void onPermissionDenied(Permission permission) {
                        if (!permission.shouldRationale()) {
                            new AlertDialog.Builder(getActivity())
                                    .setTitle(R.string.permission_tips)
                                    .setMessage(String.format(getString(R.string.permission_message_no_camera), getString(R.string.app_name)))
                                    .setPositiveButton(R.string.permission_go, (dialog, which) -> {
                                        SoulPermission.getInstance().goApplicationSettings();
                                        dialog.dismiss();
                                    })
                                    .setNegativeButton(R.string.permission_cancel, (dialog, which) -> {
                                        dialog.dismiss();
                                    })
                                    .create()
                                    .show();
                        }
                    }
                });
    }

    /**
     * 切换前后摄像头
     */
    private void switchCamera() {
        if (localView != null) {
            if (isFront) {
                vcrtc.switchCamera(checkCamera(false));
            } else {
                vcrtc.switchCamera(checkCamera(true));
            }

//            vcrtc.switchCamera();
            isFront = !isFront;
            localView.setMirror(isFront);
            if ((stickUUID != null && stickUUID.equals(me.getUuid())) || peoples.size() <= 0) {
                bigView.setMirror(isFront);
            }
        }
    }

    private String checkCamera(boolean selectFront) {
        CameraEnumerator cameraEnumerator;
        if (Camera2Enumerator.isSupported(mActivity)) {
            cameraEnumerator = new Camera2Enumerator(mActivity);
        } else {
            cameraEnumerator = new Camera1Enumerator();
        }

        String[] devicesName = cameraEnumerator.getDeviceNames();
        Log.d("checkCamera", "checkCamera: ");
        for (int i = 0; i < devicesName.length; i++) {
            if (selectFront && cameraEnumerator.isFrontFacing(devicesName[i])) {
                return devicesName[i];
            }
            if (!selectFront && cameraEnumerator.isBackFacing(devicesName[i])) {
                return devicesName[i];
            }

        }
        return "";
    }

    private void toggleAudioModel() {
        vcrtc.updateVideoImage(((ZJConferenceActivity) getActivity()).audioModelBitmap);
        vcrtc.setVideoEnable(isAudioModel, true);
        ivAudioMedol.setSelected(!isAudioModel);
        isAudioModel = !isAudioModel;
        vcrtc.setAudioModelEnable(isAudioModel);
        if (isAudioModel) {
            ivAudioImg.setVisibility(View.VISIBLE);
        } else {
            ivAudioImg.setVisibility(View.GONE);
        }
    }

    /**
     * 分享按钮打开/关闭
     */
    public void toggleShare() {
        if (isShare) {
            llSmallVideo.setVisibility(View.VISIBLE);
            imagePathList.clear();
            stopWhiteBoard();
            stopPresentation();
            ivShare.setSelected(false);
            isShareScreen = false;
            if (!isMuteVideo) {
                vcrtc.setVideoEnable(true, true);
            }
        } else {
            if (!isAudioModel) {
                showShareWindow();
            }
        }
    }

    private void stopWhiteBoard() {
        if (mActivity != null) {
            ((ZJConferenceActivity) mActivity).isShowWhite = false;
            ivShare.setSelected(false);
            if (!whiteBoardUtil.isJoin()) {
                vcrtc.stopTwoWayWhiteboard();
            }
            whiteBoardUtil.makeFloatVisible(false);
            if (whiteBoardUtil != null && whiteBoardUtil.getWhiteBoardview() != null) {
                whiteBoardUtil.resetPosition();
                whiteBoardUtil.releaseView();
                whiteBoardUtil.releaseWhiteView();
            }
            flMarkBackground.setVisibility(View.GONE);
        }
    }

    /**
     * 显示分享pop
     */
    private void showShareWindow() {
        View view = getActivity().getLayoutInflater().inflate(R.layout.layout_share, null);
        popupWindowShare = new PopupWindow(view, VCUtil.dp2px(getActivity(), 120), RelativeLayout.LayoutParams.WRAP_CONTENT, true);
        popupWindowShare.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindowShare.setOutsideTouchable(true);
        int[] location = new int[2];
        ivShare.getLocationOnScreen(location);
        popupWindowShare.showAtLocation(ivShare, Gravity.NO_GRAVITY, location[0] - VCUtil.dp2px(getActivity(), 40), location[1] - VCUtil.dp2px(getActivity(), 170));

        TextView tvSharePicture = view.findViewById(R.id.tv_share_picture);
        TextView tvShareFile = view.findViewById(R.id.tv_share_file);
        TextView tvShareScreen = view.findViewById(R.id.tv_share_screen);
        TextView tvShareWhite = view.findViewById(R.id.tv_share_white_board);
        tvSharePicture.setOnClickListener(v -> {
            GlideEngine.getInstance().getPicture(this);
            popupWindowShare.dismiss();
        });

        tvShareFile.setOnClickListener(v -> {
            Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
            galleryIntent.setType("application/pdf");
            galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);
            galleryIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
            Intent chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.share_choose_file));

            mActivity.startActivityForResult(chooserIntent, PDF_PICKER_REQUEST);
            popupWindowShare.dismiss();
        });

        tvShareScreen.setOnClickListener(v -> {
            RTCManager.getInstance().setActivityContext(getActivity());
            vcrtc.sendPresentationScreen();
            popupWindowShare.dismiss();
        });
        tvShareWhite.setOnClickListener(v -> {
            startWhiteBoard();
            popupWindowShare.dismiss();
        });
    }

    /**
     * 打开白板
     */
    float whiteHeight;
    float whiteWidth;

    private void startWhiteBoard() {
        vpShare.setVisibility(View.GONE);
        rlShareScreen.setVisibility(View.GONE);
        ivShare.setSelected(true);
        whiteBoardUtil.setMark(false);
        ((ZJConferenceActivity) mActivity).isShowWhite = true;
        isShare = true;

        setUnStick();
        vcrtc.updateClayout("0:1");
        calculateSize();
        Bitmap backgroundBitmap = Bitmap.createBitmap((int) whiteWidth, (int) whiteHeight, Bitmap.Config.RGB_565);
        backgroundBitmap.eraseColor(Color.WHITE);
        vcrtc.sendPresentationBitmap(backgroundBitmap, true);
    }

    private void calculateSize() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        whiteHeight = dm.heightPixels;
        whiteWidth = whiteHeight * (16 * 1.0f / 9 * 1.0f);
        float temp = 0;
        if (whiteHeight > whiteWidth) {
            temp = whiteHeight;
            whiteHeight = whiteWidth;
            whiteWidth = temp;
        }
    }

    private TextView tvRecord, tvLive, tvViewInView, tvAdditional;
    private View line1, line2, line3;
    private int y;

    /**
     * 显示"更多"  pop
     */
    private void showMoreWindow() {
        View view = getActivity().getLayoutInflater().inflate(R.layout.layout_more, null);
        popupWindowMore = new PopupWindow(view, VCUtil.dp2px(getActivity(), 120), RelativeLayout.LayoutParams.WRAP_CONTENT, true);
        popupWindowMore.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindowMore.setOutsideTouchable(true);

        tvRecord = view.findViewById(R.id.tv_record);
        tvLive = view.findViewById(R.id.tv_live);
        tvViewInView = view.findViewById(R.id.tv_view_in_view);
        tvAdditional = view.findViewById(R.id.tv_additional);

        line1 = view.findViewById(R.id.line1);
        line2 = view.findViewById(R.id.line2);
        line3 = view.findViewById(R.id.line3);

        tvRecord.setOnClickListener(v -> {
            vcrtc.switchRecorder(!isRecord);
            popupWindowMore.dismiss();
        });

        tvLive.setOnClickListener(v -> {
            vcrtc.switchLiving(!isLive);
            popupWindowMore.dismiss();
        });

        tvViewInView.setOnClickListener(v -> {
            hideShowSmallView();
            popupWindowMore.dismiss();
        });

        refreshMoreWindow();

        int[] location = new int[2];
        ivMore.getLocationOnScreen(location);

        popupWindowMore.showAtLocation(ivMore, Gravity.NO_GRAVITY, location[0] - VCUtil.dp2px(getActivity(), 40), location[1] - y);
    }

    /**
     * 刷新"更多"pop内容
     */
    private void refreshMoreWindow() {
        y = VCUtil.dp2px(getActivity(), 130);

        if (vcrtc.canRecord() && call.isHost()) {
            tvRecord.setVisibility(View.VISIBLE);
        } else {
            tvRecord.setVisibility(View.GONE);
            line1.setVisibility(View.GONE);
            y -= VCUtil.dp2px(getActivity(), 40);
        }

        if (vcrtc.canLive() && call.isHost()) {
            tvLive.setVisibility(View.VISIBLE);
        } else {
            tvLive.setVisibility(View.GONE);
            line2.setVisibility(View.GONE);
            y -= VCUtil.dp2px(getActivity(), 40);
        }

        tvRecord.setText(isRecord ? R.string.more_close_record : R.string.more_open_record);
        tvLive.setText(isLive ? R.string.more_close_live : R.string.more_open_live);
        tvViewInView.setText(isHideSmallView ? R.string.more_show_small_view : R.string.more_hide_small_view);
    }

    private void showToast(String message) {
        ((ZJConferenceActivity) getActivity()).showToast(message, Toast.LENGTH_SHORT);
    }

    private void disconnect() {

        releaseBarTimer();
        releaseDurationTimer();
        releaseStatsTimer();

        llSmallVideo.removeAllViews();
        flBigVideo.removeAllViews();

        ((ZJConferenceActivity) mActivity).disconnect();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseBarTimer();
        releaseDurationTimer();
        releaseStatsTimer();
        timerHandler.removeCallbacksAndMessages(null);
    }

    /**
     * 开启屏幕共享并跳转到桌面
     */
    private void startScreenShare() {
        ivShare.setSelected(true);
        //切到桌面
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        startActivity(home);
    }

    /**
     * 开启双流
     */
    private void startPresentation() {
        setUnStick();
        rlShareScreen.setVisibility(View.GONE);
        if (isShare) {
            vcrtc.updateClayout("0:1");
            ivShare.setSelected(true);
            vpShare.setVisibility(View.VISIBLE);
            if (isPicture) {
                ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getActivity(), imagePathList);
                viewPagerAdapter.setOnItemImageListener(new ViewPagerAdapter.OnItemImageListener() {
                    @Override
                    public void onClick() {
                        if (isShowBar) {
                            hideBar();
                        } else {
                            showBar();
                        }
                    }

                    @Override
                    public void onCutBitmap(Bitmap bitmap) {
                        vcrtc.sendPresentation(bitmap);
                    }
                });
                vpShare.setAdapter(viewPagerAdapter);
            } else if (isPDF) {
                PDFAdapter viewPagerAdapter = new PDFAdapter(mActivity, pdfUtil.getPageCount(), pdfUtil, pdfBitmap);
                viewPagerAdapter.setOnItemImageListener(new PDFAdapter.OnItemImageListener() {
                    @Override
                    public void onClick() {
                        if (isShowBar) {
                            hideBar();
                        } else {
                            showBar();
                        }
                    }

                    @Override
                    public void onCutBitmap(Bitmap bitmap) {

                    }

                    @Override
                    public void onGetImageView(ImageView imageView) {

                    }
                });
                vpShare.setAdapter(viewPagerAdapter);
            } else {
                setUnStick();
                rlShareScreen.setVisibility(View.GONE);
                // 收到其他人的双流
                ivShare.setSelected(false);
                vpShare.setVisibility(View.GONE);
            }
        } else {
            ivShare.setSelected(false);
            vpShare.setVisibility(View.GONE);
        }
    }

    /**
     * 发送分享的图片
     */
    private Map<Integer, Bitmap> pictureMap = new HashMap<>();
    private void sendSharePicture() {
        if (isPicture) {
            Bitmap bitmap = pictureMap.get(pictureIndex);
            if (bitmap == null) {
                pictureMap.put(pictureIndex, com.example.alan.sdkdemo.util.BitmapUtil.getImage(imagePathList.get(pictureIndex)));
                bitmap = pictureMap.get(pictureIndex);
            }
            whiteBoardUtil.sendWhiteBoardBitmap(vcrtc);
            if (!whiteBoardUtil.isJoin() && whiteBoardUtil.getWhiteBoardview() != null) {
                vcrtc.updateWhiteboardImage(com.example.alan.sdkdemo.util.BitmapUtil.formatBitmap16_9_No_recycle(bitmap, 1920, 1080));
            }
        } else if (isPDF) {
            whiteBoardUtil.sendWhiteBoardBitmap(vcrtc);
            vcrtc.updateWhiteboardImage(com.example.alan.sdkdemo.util.BitmapUtil.formatBitmap16_9_No_recycle(pdfBitmap, 1920, 1080));

        }
    }

    /**
     * 停止双流
     */
    private void stopPresentation() {
        isPicture = false;
        isPDF = false;
        try {
            vcrtc.stopPresentation();
        } catch (Exception e) {
        }
        vcrtc.updateClayout("1:4");
        vpShare.setVisibility(View.GONE);
        rlShareScreen.setVisibility(View.GONE);
        releaseMap();
        isShare = false;
    }

    private void releaseMap() {
        for (Bitmap value : pictureMap.values()) {
            if (value != null && !value.isRecycled()) {
                value.recycle();
            }
        }
        pictureMap.clear();
        System.gc();
    }

    /**
     * 分享选中后返回的回调
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    private Bitmap pdfBitmap;
    private boolean isPDF = false, isPicture = false;
    private PDFUtil pdfUtil;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PDF_PICKER_REQUEST) {
                // 从文件中选择pdf并返回
                Uri uri = data.getData();
                if (uri != null) {
                    try {
                        pdfBitmap = pdfUtil.openFile(uri);
                        isPicture = false;
                        isPDF = true;
                        pictureIndex = 0;
                        isShare = true;
                        startPresentation();
                        sendSharePicture();
                    } catch (Exception e) {
                        showToast("共享失败，请检测文件格式是否正确");
                        e.printStackTrace();
                    }
                }
            } else if (requestCode == GlideEngine.REQUEST_CODE) {
                // 从文件中选择图片并返回

                ArrayList<Photo> selectList = data.getParcelableArrayListExtra(EasyPhotos.RESULT_PHOTOS);
                if (selectList != null && selectList.size() > 0) {
                    isPicture = true;
                    isPDF = false;
                    for (Photo media : selectList) {
                        imagePathList.add(media.path);
                    }
                    pictureIndex = 0;
                    isShare = true;
                    startPresentation();
                    sendSharePicture();
                }
            }
        }
    }

    /**
     * 初始化会议状态信息的画面
     */
    private void initMediaStatsWindow() {
        View view = getActivity().getLayoutInflater().inflate(R.layout.popup_stats, null);
        popupWindowStats = new PopupWindow(view, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, false);
        popupWindowStats.setOutsideTouchable(false);

        ImageView ivClose = view.findViewById(R.id.iv_close);
        ListView lvStats = view.findViewById(R.id.lv_stats);

        ivClose.setOnClickListener(this);

        adapter = new StatsAdapter(getActivity(), itemBeanList, false);
        lvStats.setAdapter(adapter);
    }

    private void showMediaStatsWindow() {
        popupWindowStats.showAtLocation(rootView, Gravity.CENTER, 0, 0);
    }

    public void showDisconnectDialog() {
        if (popupWindowStats != null && popupWindowStats.isShowing()) {
            popupWindowStats.dismiss();
            return;
        }
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.disconnect_title)
                .setMessage(R.string.disconnect_message)
                .setPositiveButton(R.string.disconnect_sure, (dialog, which) -> {
                    disconnect();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.disconnect_cancel, (dialog, which) -> {
                    dialog.dismiss();
                }).create();

        alertDialog.show();
    }

    /**
     * 排序结束，刷新整个视频画面
     */
    private void refreshUI() {
        tvPeopleNum.setText(String.valueOf(peoples.size() + 1));
        if (showPeoples.size() > 1 || isPresentation) {
            for (String uuid : peoples.keySet()) {
                if (!showPeoples.containsKey(uuid) && peoples.get(uuid).getPeopleView() != null && peoples.get(uuid).getPeopleView().getParent() != null) {
                    llSmallVideo.removeView(peoples.get(uuid).getPeopleView());
                }
            }
            if (isPresentation) {
                llSmallVideo.removeView(me.getPeopleView());
                llBigName.setVisibility(View.INVISIBLE);
                bigView.setMirror(false);
                bigView.setStreamURL(presentationStreamURL);
                for (String uuid : showPeoples.keySet()) {
                    if (showPeoples.get(uuid).getPeopleView() != null && showPeoples.get(uuid).getPeopleView().getParent() == null) {
                        llSmallVideo.addView(showPeoples.get(uuid).getPeopleView(), smallLayoutParams);
                    }
                }
            } else {
                for (String uuid : showPeoples.keySet()) {
                    if (showPeoples.get(uuid).isBig()) {
                        bigView.setStreamURL(showPeoples.get(uuid).getStreamURL());
                        bigView.setMirror(false);
                        if (uuid.equals(me.getUuid()) && !isPresentation) {
                            bigView.setMirror(isFront && ZJConferenceActivity.allowCamera);
                            if (isMuteVideo) {
                                ivCloseVideoBig.setVisibility(View.VISIBLE);
                            }
                        } else {
                            ivCloseVideoBig.setVisibility(View.GONE);
                        }
                        ivBigMute.setVisibility(showPeoples.get(uuid).isMute() ? View.VISIBLE : View.GONE);
                        tvBigName.setText(showPeoples.get(uuid).getName());
                        llBigName.setVisibility(View.VISIBLE);
                        if (showPeoples.get(uuid).getPeopleView() != null && showPeoples.get(uuid).getPeopleView().getParent() != null) {
                            llSmallVideo.removeView(showPeoples.get(uuid).getPeopleView());
                        }
                    } else {
                        if (!isHideSmallView) {
                            if (showPeoples.get(uuid).getPeopleView() != null && showPeoples.get(uuid).getPeopleView().getParent() == null) {
                                if (uuid != null && uuid.equals(me.getUuid())) {
                                    llSmallVideo.addView(showPeoples.get(uuid).getPeopleView(), 0, smallLayoutParams);
                                } else {
                                    if (llSmallVideo.getChildAt(0) != null) {
                                        llSmallVideo.addView(showPeoples.get(uuid).getPeopleView(), 1, smallLayoutParams);
                                    } else {
                                        llSmallVideo.addView(showPeoples.get(uuid).getPeopleView(), smallLayoutParams);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (me != null && me.getStreamURL() != null) {
                bigView.setStreamURL(me.getStreamURL());
                bigView.setMirror(isFront && ZJConferenceActivity.allowCamera);
                llBigName.setVisibility(View.INVISIBLE);
                llSmallVideo.removeAllViews();
            }
        }
    }

    /**
     * 参会人排序，判断一下谁是大画面，谁是小画面
     */
    private void sortPeopels() {
        showPeoples.clear();
        if (isPresentation) {
            if (me != null) me.setBig(false);
            if (participantsSort != null && peoples.size() > 0) {
                for (int i = 0; i < participantsSort.size(); i++) {
                    for (String uuid : peoples.keySet()) {
                        if (uuid.equals(participantsSort.get(i))) {
                            peoples.get(uuid).setBig(false);
                            showPeoples.put(uuid, peoples.get(uuid));
                        }
                    }
                }
            }
        } else {
            if (makeMeBig) {
                if (me != null) {
                    me.setBig(true);
                    bigUUID = me.getUuid();
                }
                if (participantsSort != null && peoples.size() > 0) {
                    for (int i = 0; i < participantsSort.size(); i++) {
                        for (String uuid : peoples.keySet()) {
                            if (uuid.equals(participantsSort.get(i))) {
                                peoples.get(uuid).setBig(false);
                                showPeoples.put(uuid, peoples.get(uuid));
                            }
                        }
                    }
                }
            } else {
                if (me != null) me.setBig(false);
                if (participantsSort != null && peoples.size() > 0) {
                    for (int i = 0; i < participantsSort.size(); i++) {
                        for (String uuid : peoples.keySet()) {
                            if (uuid.equals(participantsSort.get(i))) {
                                if (i == 0) {
                                    peoples.get(uuid).setBig(true);
                                    bigUUID = uuid;
                                } else {
                                    peoples.get(uuid).setBig(false);
                                }
                                showPeoples.put(uuid, peoples.get(uuid));
                            }
                        }
                    }
                }
            }
            if (me != null) showPeoples.put(me.getUuid(), me);
        }
        refreshUI();
    }

    public void startMarkTools(boolean alreadySuccess) {
        Bitmap backgroundBitmap = null;
        calculateSize();
        if (isPicture) {
            if (imagePathList.size() == 0) {
                return;
            }
            backgroundBitmap = com.example.alan.sdkdemo.util.BitmapUtil.formatBitmap16_9(com.example.alan.sdkdemo.util.BitmapUtil.getImage(imagePathList.get(pictureIndex)), 1920, 1080);
        } else if (isPDF) {
            if (pdfBitmap == null) {
                return;
            }
            backgroundBitmap = com.example.alan.sdkdemo.util.BitmapUtil.formatBitmap16_9(pdfBitmap, 1920, 1080);
        }
        if (alreadySuccess){
            WhiteBoardView whiteBoardView = new WhiteBoardView(mActivity, (int) whiteWidth, (int) whiteHeight, Color.BLACK, 5);
            whiteBoardView.setOnClickListener(v -> {
                if (isShowBar) {
                    hideBar();
                } else {
                    showBar();
                }
            });
            whiteBoardUtil.joinMarkView(whiteBoardView);
            whiteBoardUtil.addMarkView((int) whiteWidth, (int) whiteHeight, false);
        }else {
            vcrtc.sendPresentationBitmap(backgroundBitmap, true);
            vcrtc.startTwoWayWhiteboard(backgroundBitmap, true, true);
        }
    }

    public void joinBoard() {
        if (whiteBoardUtil.getWhiteBoardview() != null) {
            whiteBoardUtil.showWhiteView();
            handleDisplay(false);
        } else {
            joinWhitBoardTools();
        }
    }

    public void joinWhitBoardTools() {
        if (isAudioModel) {
            return;
        }
        calculateSize();
        WhiteBoardView whiteBoardView = new WhiteBoardView(mActivity, (int) whiteWidth, (int) whiteHeight, Color.BLACK, 5);
        whiteBoardUtil.joinWhiteBoardView(whiteBoardView);
        whiteBoardView.setOnClickListener(v -> {
            if (isShowBar) {
                hideBar();
            } else {
                showBar();
            }
        });
        vcrtc.joinWhiteboard(objects -> {
            whiteBoardView.initByPayloads((Map<Integer, WhiteboardPayload>) objects[0]);
            whiteBoardUtil.resetPosition();
            whiteBoardUtil.addWhiteBoardView((int) whiteWidth, (int) whiteHeight);
            handleDisplay(false);
        });
    }

    public void startWhiteBoardTools(boolean alreadySuccess) {
        if (isAudioModel) {
            return;
        }
        calculateSize();
        if (alreadySuccess) {
            WhiteBoardView whiteBoardView = new WhiteBoardView(mActivity, (int) whiteWidth, (int) whiteHeight, Color.BLACK, 5);
            whiteBoardView.setOnClickListener(v -> {
                if (isShowBar) {
                    hideBar();
                } else {
                    showBar();
                }
            });
            whiteBoardUtil.setWhiteBoardView(whiteBoardView);
            whiteBoardUtil.resetPosition();
            whiteBoardUtil.addWhiteBoardView(whiteWidth, whiteHeight);
            handleDisplay(false);
        } else {
            Bitmap backgroundBitmap = Bitmap.createBitmap((int) whiteWidth, (int) whiteHeight, Bitmap.Config.RGB_565);
            backgroundBitmap.eraseColor(Color.WHITE);
            vcrtc.startTwoWayWhiteboard(backgroundBitmap, false, true);
        }
    }

    public void handleDisplay(boolean isShow) {
//        if (!isShow){
//            llSmallVideo.removeAllViews();
//        }
        llSmallVideo.setVisibility(isShow ? View.VISIBLE : View.GONE);
        flMarkBackground.setVisibility(!isShow ? View.VISIBLE : View.GONE);

    }


    Handler sendHandler = new Handler(Looper.getMainLooper());

    Runnable sendRunnable = () -> {
        whiteBoardUtil.sendWhiteBoardBitmap(vcrtc);
    };

    private void sendHandle() {
        sendHandler.removeCallbacks(sendRunnable);
        sendHandler.postDelayed(sendRunnable, 300);
    }

    ZJConferenceActivity.MediaCallBack callBack = new ZJConferenceActivity.MediaCallBack() {

        @Override
        public void onConnect() {
            if (ZJConferenceActivity.isTurnOn) {
                timerHandler.postDelayed(runnable, 45000);
            }
        }

        @Override
        public void onCallConnect() {
            if (getActivity() != null && ZJConferenceActivity.joinMuteAudio) {
                toggleMuteAudio();
                ZJConferenceActivity.joinMuteAudio = false;
            }
        }

        @Override
        public void onPresentationSuccess() {
            if (isPicture || isPDF) {
                startMarkTools(false);
            } else {
                if (!isShareScreen) {
                    startWhiteBoardTools(false);
                }
            }
        }

        @Override
        public void onWhiteboardAddPayload(int cmdid, WhiteboardPayload payload) {
            WhiteBoardView view = whiteBoardUtil.getWhiteBoardview();
            if (view != null) {
                view.updateByPaload(cmdid, payload);
                if (whiteBoardUtil.isMark() && !whiteBoardUtil.isJoin()) {
                    sendHandle();
                }
            }
        }

        @Override
        public void onWhiteboardClearPayload() {
            WhiteBoardView view = whiteBoardUtil.getWhiteBoardview();
            if (view != null) {
                view.clear();
                if (whiteBoardUtil.isMark() && !whiteBoardUtil.isJoin()) {
                    sendHandle();
                }
            }
        }

        @Override
        public void onWhiteboardDeletePayload(int cmdid) {
            WhiteBoardView view = whiteBoardUtil.getWhiteBoardview();
            if (view != null) {
                view.deleteByCmdid(cmdid);
                if (whiteBoardUtil.isMark() && !whiteBoardUtil.isJoin()) {
                    sendHandle();
                }
            }
        }

        @Override
        public void onWhiteboardImageUpdate(Bitmap bitmap) {
            View view = whiteBoardUtil.getWhiteBoardview();
            if (view != null) {
                whiteBoardUtil.setMarkBackground(true, bitmap);
            }
            whiteBoardUtil.setCurrentMarkBitmapD(bitmap);
        }

        @Override
        public void onWhiteboardStart(String uuid, boolean isMark) {
            // 清空上一次的白板
            if (whiteBoardUtil.getWhiteBoardview() != null) {
                stopWhiteBoard();
            }
            whiteBoardUtil.setMark(isMark);
            if (getActivity() != null) {
                whiteBoardUtil.makeFloatVisible(true);
                if (!((ZJConferenceActivity) getActivity()).myUUID.equals(uuid)) {
                    // 如果当前共享白板的不是我
                    whiteBoardUtil.setCurrentStatus(true);
                } else {
                    whiteBoardUtil.setCurrentStatus(false);
                    if (!isMark) {
                        startWhiteBoardTools(true);
                        whiteBoardUtil.initStartBroad();
                    } else {
                        startMarkTools(true);
                        whiteBoardUtil.initStartMark();
                    }
                }
            }


        }

        @Override
        public void onWhiteboardStop() {
            ((ZJConferenceActivity) mActivity).isShowWhite = false;
            if (whiteBoardUtil != null) {
                whiteBoardUtil.makeFloatVisible(false);
                if (whiteBoardUtil.getWhiteBoardview() != null) {
                    vcrtc.stopTwoWayWhiteboard();
                    handleDisplay(true);
                    whiteBoardUtil.releaseWhiteView();
                    whiteBoardUtil.releaseView();
                }
            }
            whiteBoardUUID = "";
        }

        @Override
        public void onLocalVideo(String uuid, VCRTCView view) {
        }

        @Override
        public void onRemoteVideo(String uuid, VCRTCView view) {

        }

        @Override
        public void onLocalStream(String uuid, String streamURL) {
            stopLoading();
            if (localView == null) {
                bigUUID = uuid;
                bigView.setStreamURL(streamURL);

                localView = new VCRTCView(getActivity());
                localView.setZOrder(1);
                if (ZJConferenceActivity.allowCamera)
                    localView.setMirror(true);
                localView.setObjectFit("contain");
                localView.setStreamURL(streamURL);

                View myView = getActivity().getLayoutInflater().inflate(R.layout.video_item, null);

                ImageView ivVideoLoading = myView.findViewById(R.id.iv_video_loading);
                AnimationDrawable animationDrawable = (AnimationDrawable) ivVideoLoading.getDrawable();
                animationDrawable.start();

                FrameLayout flVideo = myView.findViewById(R.id.fl_video);
                ivCloseVideo = myView.findViewById(R.id.iv_close_video);
                TextView tvName = myView.findViewById(R.id.tv_name);
                tvName.setText(R.string.me);

                flVideo.addView(localView, videoLayoutParams);

                flVideo.setOnClickListener(new DoubleClickListener() {

                    @Override
                    public void onSingleClick(View v) {

                    }

                    @Override
                    public void onDoubleClick(View v) {
                        if (!isPresentation && !isShareScreen) {
                            if (isStick && !stickUUID.equals(uuid)) {
                                vcrtc.setParticipantStick(stickUUID, false);
                            }
                            setStick(uuid);
                        }
                    }

                });

                me = new People(uuid, getString(R.string.me), streamURL, myView, false);

                startTheTime();
            } else {
                if (ZJConferenceActivity.allowCamera)
                    localView.setMirror(true);
                localView.setStreamURL(streamURL);
                me.setStreamURL(streamURL);
            }
        }

        @Override
        public void onAddView(String uuid, VCRTCView view, String viewType) {
            if (viewType.equals("video") && peoples.containsKey(uuid)) {

                view.setZOrder(1);
                view.setObjectFit("contain");
                View peopleView = peoples.get(uuid).getPeopleView();
                FrameLayout flVideo = peopleView.findViewById(R.id.fl_video);
                flVideo.removeAllViews();
                flVideo.addView(view, videoLayoutParams);

                flVideo.setOnClickListener(new DoubleClickListener() {

                    @Override
                    public void onSingleClick(View v) {

                    }

                    @Override
                    public void onDoubleClick(View v) {
                        if (!isPresentation && !isShareScreen) {
                            setStick(uuid);
                        }
                    }

                });
                sortPeopels();
            }
        }

        @Override
        public void onRemoveView(String uuid, VCRTCView view) {
            if (peoples.containsKey(uuid)) {
                View peopleView = peoples.get(uuid).getPeopleView();
                FrameLayout flVideo = peopleView.findViewById(R.id.fl_video);
                flVideo.removeAllViews();
            }
        }

        @Override
        public void onRemoteStream(String uuid, String streamURL, String streamType) {
            if (streamType.equals("video") && peoples.containsKey(uuid)) {
                peoples.get(uuid).setStreamURL(streamURL);
            } else if (streamType.equals("presentation")) {
                isPresentation = true;
                presentationStreamURL = streamURL;
            }
            sortPeopels();
        }

        @Override
        public void onAddParticipant(Participant participant) {
            if (!peoples.containsKey(participant.getUuid()) && (me == null || !participant.getUuid().equals(me.getUuid()))) {
                View peopleView = getActivity().getLayoutInflater().inflate(R.layout.video_item, null);

                ImageView ivVideoLoading = peopleView.findViewById(R.id.iv_video_loading);
                AnimationDrawable animationDrawable = (AnimationDrawable) ivVideoLoading.getDrawable();
                animationDrawable.start();

                ImageView ivMute = peopleView.findViewById(R.id.iv_mute);
                TextView tvName = peopleView.findViewById(R.id.tv_name);
                ivMute.setVisibility(participant.getIs_muted().equals("YES") ? View.VISIBLE : View.GONE);
                tvName.setText(participant.getOverlay_text());
                People people = new People(participant.getUuid(), participant.getOverlay_text(), null, peopleView, false);
                people.setMute(participant.getIs_muted().equals("YES"));
                peoples.put(participant.getUuid(), people);

            }
        }

        @Override
        public void onUpdateParticipant(Participant participant) {
            if (peoples.containsKey(participant.getUuid())) {
                peoples.get(participant.getUuid()).setName(participant.getOverlay_text());
                peoples.get(participant.getUuid()).setMute(participant.getIs_muted().equals("YES"));
                if (peoples.get(participant.getUuid()).getPeopleView() != null) {
                    ImageView ivMute = peoples.get(participant.getUuid()).getPeopleView().findViewById(R.id.iv_mute);
                    TextView tvName = peoples.get(participant.getUuid()).getPeopleView().findViewById(R.id.tv_name);
                    ivMute.setVisibility(participant.getIs_muted().equals("YES") ? View.VISIBLE : View.GONE);
                    tvName.setText(participant.getOverlay_text());
                }
            } else {
                if (me == null || !participant.getUuid().equals(me.getUuid())) {
                    View peopleView = getActivity().getLayoutInflater().inflate(R.layout.video_item, null);

                    ImageView ivVideoLoading = peopleView.findViewById(R.id.iv_video_loading);
                    AnimationDrawable animationDrawable = (AnimationDrawable) ivVideoLoading.getDrawable();
                    animationDrawable.start();

                    ImageView ivMute = peoples.get(participant.getUuid()).getPeopleView().findViewById(R.id.iv_mute);
                    TextView tvName = peopleView.findViewById(R.id.tv_name);
                    ivMute.setVisibility(participant.getIs_muted().equals("YES") ? View.VISIBLE : View.GONE);
                    tvName.setText(participant.getOverlay_text());
                    People people = new People(participant.getUuid(), participant.getOverlay_text(), null, peopleView, false);
                    people.setMute(participant.getIs_muted().equals("YES"));
                    peoples.put(participant.getUuid(), people);
                }
            }

            if (me != null && participant.getUuid().equals(me.getUuid())) {
                ImageView ivMute = me.getPeopleView().findViewById(R.id.iv_mute);
                ivMute.setVisibility(participant.getIs_muted().equals("YES") ? View.VISIBLE : View.GONE);
            }
            refreshUI();
        }

        @Override
        public void onRemoveParticipant(String uuid) {
            if (peoples.containsKey(uuid)) {
                if (peoples.get(uuid).getPeopleView() != null && peoples.get(uuid).getPeopleView().getParent() != null) {
                    llSmallVideo.removeView(peoples.get(uuid).getPeopleView());
                }
                peoples.remove(uuid);
                sortPeopels();
            }
            if (uuid.equals(stickUUID)) {
                setUnStick();
            }
        }

        @Override
        public void onLayoutUpdate(String layout, String hostLayout, String guestLayout) {

        }

        @Override
        public void onRoleUpdate(String role) {
            call.setHost(role.equals("HOST") ? true : false);
        }

        @Override
        public void onLayoutUpdateParticipants(List<String> participants) {
            participantsSort = participants;
            sortPeopels();
        }

        @Override
        public void onPresentation(boolean isActive, String uuid) {
            isPresentation = isActive;
            sortPeopels();
            if (isActive) {
//                startPresentation();
                if (isShare && !uuid.equals(me.getUuid())) {
                    showToast(getString(R.string.sharing_screen_interrupted));
                    imagePathList.clear();
                    isShare = false;
                    isShareScreen = false;
                    rlShareScreen.setVisibility(View.GONE);
                    vpShare.setVisibility(View.GONE);
                    ivShare.setSelected(false);
                    vcrtc.stopPresentation();
                }
            } else {
                if (!isShare) {
                    stopPresentation();
                    vcrtc.stopReceivePresentation();
                }
            }
        }

        @Override
        public void onPresentationReload(String url) {

        }

        @Override
        public void onScreenShareState(boolean isActive) {
            if (isActive) {
                isShare = true;
                isShareScreen = true;
                mHandler.sendEmptyMessage(START_SCREEN_SHARE);
            }
        }

        @Override
        public void onRecordState(boolean isActive) {
            isRecord = isActive;
            llRecording.setVisibility(isRecord && call.isHost() ? View.VISIBLE : View.GONE);
            if (popupWindowMore != null && popupWindowMore.isShowing()) {
                refreshMoreWindow();
            }
        }

        @Override
        public void onLiveState(boolean isActive) {
            isLive = isActive;
            llLiving.setVisibility(isLive && call.isHost() ? View.VISIBLE : View.GONE);
            if (popupWindowMore != null && popupWindowMore.isShowing()) {
                refreshMoreWindow();
            }
        }

    };

    private boolean isStop = false;
    Handler timerHandler = new Handler();
    Runnable runnable = () -> {
        if (!isStop) {
            disconnect();
            showToast(getString(R.string.disconnect_timeout));
        }
    };

    public void switchMark() {
        handleDisplay(true);
        WhiteBoardView view = whiteBoardUtil.getWhiteBoardview();
        if (view != null) {
            view.setBackgroundColor(Color.parseColor("#00000000"));
            whiteBoardUtil.setMarkBackground(false, null);
            flMarkBackground.setVisibility(View.GONE);
        }
        bigView.setVisibility(View.VISIBLE);
    }

    public void updateMark() {
        handleDisplay(false);
        Bitmap backgroundBitmap = null;
        if (isPicture) {
            backgroundBitmap = com.example.alan.sdkdemo.util.BitmapUtil.formatBitmap16_9(com.example.alan.sdkdemo.util.BitmapUtil.getImage(imagePathList.get(pictureIndex)), 1920, 1080);
            ;
        } else if (isPDF) {
            Log.d("startWhiteBroad", "startOrJoinMark: isPDF");
            backgroundBitmap = com.example.alan.sdkdemo.util.BitmapUtil.formatBitmap16_9(pdfBitmap, 1920, 1080);
        }
        vcrtc.updateWhiteboardImage(backgroundBitmap);
        Log.d("startWhiteBroad", "update_bitmap:");
        WhiteBoardView view = whiteBoardUtil.getWhiteBoardview();
        if (view != null) {
            whiteBoardUtil.setMarkBackground(true, backgroundBitmap);
            flMarkBackground.setVisibility(View.VISIBLE);
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        }
    }

    public void startMark() {
        startMarkTools(false);
    }

    public void joinMark() {
        if (whiteBoardUtil.getWhiteBoardview() != null) {
            whiteBoardUtil.showWhiteView();

            whiteBoardUtil.setMarkBackground(true, whiteBoardUtil.getCurrentMarkBitmap());
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            handleDisplay(false);
            return;
        }
        calculateSize();
        vcrtc.joinWhiteboard(objects -> {
            WhiteBoardView whiteBoardView = new WhiteBoardView(mActivity, (int) whiteWidth, (int) whiteHeight, Color.BLACK, 5);
            whiteBoardUtil.joinMarkView(whiteBoardView);
            whiteBoardView.setOnClickListener(v -> {
                if (isShowBar) {
                    hideBar();
                } else {
                    showBar();
                }
            });
            Log.d("white_join", "justJoinMark: ");
            flMarkBackground.setVisibility(View.VISIBLE);
            whiteBoardUtil.setMarkBackground(true, whiteBoardUtil.getCurrentMarkBitmap());
            whiteBoardView.initByPayloads((Map<Integer, WhiteboardPayload>) objects[0]);
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            whiteBoardUtil.addMarkView((int) whiteWidth, (int) whiteHeight, false);
            handleDisplay(false);
        });
//        bigView.setVisibility(View.GONE);
    }
}
