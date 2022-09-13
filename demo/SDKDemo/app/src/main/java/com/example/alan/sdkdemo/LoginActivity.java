package com.example.alan.sdkdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.alan.sdkdemo.contact.SPUtil;
import com.example.alan.sdkdemo.databinding.ActivityLoginBinding;
import com.vcrtc.registration.VCRegistrationUtil;
import com.vcrtc.registration.VCService;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.appcompat.app.AppCompatActivity;

import static com.vcrtc.registration.VCService.VC_ACTION;

/**
 * A login screen that offers login via username/password.
 */
public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    ActivityLoginBinding binding;
    LoginReceiver receiver = new LoginReceiver();
    AudioManager am;

    // UI references.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        IntentFilter filter = new IntentFilter(VC_ACTION);
        registerReceiver(receiver, filter);

        binding.btnLogin.setOnClickListener(this);
        binding.btnLogout.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_login:
                VCRegistrationUtil.logout(this);
                SPUtil.Companion.instance(getApplicationContext()).setLogin(false);
                SPUtil.Companion.instance(getApplicationContext()).setModel("");
                SPUtil.Companion.instance(getApplicationContext()).setSessionId("");
                SPUtil.Companion.instance(getApplicationContext()).setCompanyId("");
                VCRegistrationUtil.login(this,"", binding.email.getText().toString(), binding.password.getText().toString());
                break;
            case R.id.btn_logout:
                VCRegistrationUtil.logout(this);
                break;
            default:
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public class LoginReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(VCService.MSG);
            switch (message) {
                case VCService.MSG_LOGIN_SUCCESS:
                    Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                case VCService.MSG_LOGIN_FAILED:
                    Toast.makeText(LoginActivity.this, "登录失败", Toast.LENGTH_SHORT).show();
                    break;
                case VCService.MSG_USER_INFO:
                    //用户信息
                    String userJson = intent.getStringExtra(VCService.DATA_BROADCAST);
                    SPUtil.Companion.instance(LoginActivity.this).setLogin(true);
                    Log.d("user_info", "onReceive: " + userJson);
                    try {
                        JSONObject jsonObject = new JSONObject(userJson);
                        SPUtil.Companion.instance(LoginActivity.this).setSessionId(jsonObject.optString("session_id"));
                        String results = jsonObject.optString("results");
                        if (!TextUtils.isEmpty(results)){
                            JSONObject responseR = new JSONObject(results);
                            SPUtil.Companion.instance(LoginActivity.this).setCompanyId(responseR.optString("companyId"));
                            boolean isPublishModel = responseR.optBoolean("IsPublicModel");
                            SPUtil.Companion.instance(LoginActivity.this).setModel(isPublishModel?"cloud_conference": "virtual_mcu");
                        }else {
                            SPUtil.Companion.instance(LoginActivity.this).setCompanyId(jsonObject.optString("companyId"));
                            boolean isPublishModel = jsonObject.optBoolean("IsPublicModel");
                            SPUtil.Companion.instance(LoginActivity.this).setModel(isPublishModel?"cloud_conference": "virtual_mcu");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

}

