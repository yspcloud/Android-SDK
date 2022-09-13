package com.example.alan.sdkdemo;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.example.alan.sdkdemo.contact.ContactActivity;
import com.example.alan.sdkdemo.contact.SPUtil;
import com.example.alan.sdkdemo.databinding.ActivityMainBinding;
import com.example.alan.sdkdemo.ui.SettingActivity;
import com.example.alan.sdkdemo.ui.ZJConferenceActivity;
import com.example.alan.sdkdemo.util.CheckUtil;
import com.vcrtc.VCRTCPreferences;
import com.vcrtc.callbacks.CallBack;
import com.vcrtc.entities.Call;
import com.vcrtc.utils.SystemUtil;
import com.vcrtc.utils.VCUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    ActivityMainBinding binding;
    private final int REQUEST_PERMISSION = 1000;
    private final int OVERLAY_PERMISSION_REQ_CODE = 1001;
    private VCRTCPreferences vcPrefs;
    private final int REQUEST_SUCCESS = 0;
    private final int REQUEST_FAILED = 1;
    private MainHandler mainHandler;
    Call call;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        vcPrefs = new VCRTCPreferences(getApplicationContext());
        call = new Call();
        mainHandler = new MainHandler();
        new Handler().postDelayed(this::checkPermission, 1000);

        binding.btnSetting.setOnClickListener(this);
        binding.btnLogin.setOnClickListener(this);
        binding.btnConnect.setOnClickListener(this);
        binding.btnContact.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void goToConference() {

        call.setNickname(binding.etNickname.getText().toString());
        call.setChannel(binding.etMeetNum.getText().toString());
        call.setPassword(binding.etPassword.getText().toString());
        call.setCheckDup(VCUtil.MD5(SystemUtil.getMac(this) + call.getNickname()));
        call.setHideMe(false);
        Intent intent = new Intent(this, ZJConferenceActivity.class);
        intent.putExtra("call", call);
        startActivity(intent);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_connect:
                checkUrl(binding.tvAddress.getText().toString());
                break;
            case R.id.btn_setting:
                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_login:
                Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(loginIntent);
                break;
            case R.id.btn_contact:
                if (SPUtil.Companion.instance(this).isLogin()){
                    Intent ContactIntent = new Intent(MainActivity.this, ContactActivity.class);
                    startActivity(ContactIntent);
                }else {
                    Toast.makeText(MainActivity.this, "先登录", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermission() {
        PackageManager pm = getPackageManager();
        String pkgName = this.getPackageName();
        boolean permission = (PackageManager.PERMISSION_GRANTED == pm.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, pkgName)
                && PackageManager.PERMISSION_GRANTED == pm.checkPermission(Manifest.permission.READ_PHONE_STATE, pkgName)
                && PackageManager.PERMISSION_GRANTED == pm.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, pkgName));
        if (!permission) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.main_tips);
                builder.setMessage(getString(R.string.main_request_system_alert_window));
                //设置确定按钮
                builder.setPositiveButton(R.string.main_go_setting, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + pkgName));
                    startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
                    dialog.dismiss(); //关闭dialog
                });
                builder.setCancelable(false);

                builder.create();
                builder.show();
            }
        }
    }

    /**
     * 检测会议室号码或密码是否正确
     *
     * @param num
     * @param apiServer
     */
    private void loadInfoAndCall(String num, String apiServer) {
        CheckUtil.checkConference(num, apiServer, new CheckUtil.CheckConferenceListener() {
            @Override
            public void onSuccess(String s) {
                Log.d("loadInfoAndCall", "onSuccess: " + s);
                Message msg = new Message();
                msg.what = REQUEST_SUCCESS;
                Bundle bundle = new Bundle();
                bundle.putString("rep", s);
                msg.setData(bundle);
                mainHandler.sendMessage(msg);
            }

            @Override
            public void onFail(Exception e) {
                Message msg = new Message();
                msg.what = REQUEST_FAILED;
                mainHandler.sendMessage(msg);
            }
        });
    }


    class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == REQUEST_SUCCESS) {
                String rep = msg.getData().getString("rep");
                try {
                    JSONObject jsonObject = new JSONObject(rep);
                    int statusCode = jsonObject.optInt("code");
                    if (statusCode == 200) {
                        String hostPwd = jsonObject.optString("hostpwd");
                        String guestPwd = jsonObject.optString("guestpwd");
                        if (hostPwd.equals(binding.etPassword.getText().toString()) || guestPwd.equals(binding.etPassword.getText().toString())) {
                            goToConference();
                        } else {
                            displayMessage("会议号码或密码错误");
                        }
                    } else {
                        displayMessage("会议号码或密码错误");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (msg.what == REQUEST_FAILED) {
                displayMessage("请求失败");
            }
            super.handleMessage(msg);
        }
    }

    private void checkUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            displayMessage("fail");
            return;
        }

        vcPrefs.setServerAddress(url, "443", new CallBack() {
            @Override
            public void success(String s) {
                mainHandler.post(() -> {
                    loadInfoAndCall(binding.etMeetNum.getText().toString(), binding.tvAddress.getText().toString());
                });
            }

            @Override
            public void failure(String s) {
                displayMessage(s);
            }
        });
    }

    private void displayMessage(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show());

    }

}
