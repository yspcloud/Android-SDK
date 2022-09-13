package com.example.alan.sdkdemo.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.alan.sdkdemo.R;
import com.example.alan.sdkdemo.databinding.ActivitySettingBinding;
import com.vcrtc.VCRTCPreferences;
import com.vcrtc.callbacks.CallBack;

import androidx.appcompat.app.AppCompatActivity;

public class SettingActivity extends AppCompatActivity implements View.OnClickListener {

    ActivitySettingBinding binding;

    VCRTCPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferences = new VCRTCPreferences(this);
        initData();
    }

    private void initData() {
        binding.etApiServer.setText(preferences.getApiServer());
        binding.etCaptureW.setText(String.valueOf(preferences.getVideoWidthCapture()));
        binding.etCaptureH.setText(String.valueOf(preferences.getVideoHeightCapture()));
        binding.etCaptureF.setText(String.valueOf(preferences.getFpsCapture()));
        binding.etUpW.setText(String.valueOf(preferences.getVideoWidthUp()));
        binding.etUpH.setText(String.valueOf(preferences.getVideoHeightUP()));
        binding.etUpF.setText(String.valueOf(preferences.getFpsUp()));
        binding.etDownW.setText(String.valueOf(preferences.getVideoWidthDown()));
        binding.etDownH.setText(String.valueOf(preferences.getVideoHeightDown()));
        binding.etDownF.setText(String.valueOf(preferences.getFpsDown()));
        binding.etSmallW.setText(String.valueOf(preferences.getVideoWidthSmall()));
        binding.etSmallH.setText(String.valueOf(preferences.getVideoHeightSmall()));
        binding.etSmallF.setText(String.valueOf(preferences.getFpsSmall()));
        binding.etUpBw.setText(String.valueOf(preferences.getBandwidthUp()));
        binding.etMaxF.setText(String.valueOf(preferences.getFpsMax()));
        binding.etDownBw.setText(String.valueOf(preferences.getBandwidthDown()));
        binding.etSmallBw.setText(String.valueOf(preferences.getBandwidthSmall()));
        binding.etPresentationCaptureW.setText(String.valueOf(preferences.getVideoPresentationWidthCapture()));
        binding.etPresentationCaptureH.setText(String.valueOf(preferences.getVideoPresentationHeightCapture()));
        binding.etPresentationCaptureF.setText(String.valueOf(preferences.getFpsPresentationCapture()));
        binding.etPresentationUpW.setText(String.valueOf(preferences.getVideoPresentationWidthUp()));
        binding.etPresentationUpH.setText(String.valueOf(preferences.getVideoPresentationHeightUp()));
        binding.etPresentationUpF.setText(String.valueOf(preferences.getFpsPresentationUp()));
        binding.etPresentationBw.setText(String.valueOf(preferences.getBandwidthPresentation()));
        binding.etPresentationMaxF.setText(String.valueOf(preferences.getFpsPresentationMax()));
        binding.sRecvStream.setChecked(preferences.isSimulcast());
        binding.sSendStream.setChecked(preferences.isMultistream());
        binding.sEnableH264Encoder.setChecked(preferences.isEnableH264HardwareEncoder());
        binding.sDisableH264Decoder.setChecked(preferences.isDisableH264HardwareDecoder());
//        binding.sDisableCameraEncoder.setChecked(preferences.isDisableCameraEncoder());
        binding.sPrintLogs.setChecked(preferences.isPrintLogs());
        switch (preferences.getSpeakerphone()) {
            case "auto":
                binding.rbAuto.setChecked(true);
                break;
            case "true":
                binding.rbEnabled.setChecked(true);
                break;
            case "false":
                binding.rbDisabled.setChecked(true);
                break;
                default:
        }
        binding.btnCheck.setOnClickListener(this);
        binding.btnSubmit.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_check:
                check(view);
                break;
            case R.id.btn_submit:
                save(view);
                break;
                default:
        }
    }

    public void save(View view) {
        preferences.setApiServer(binding.etApiServer.getText().toString());

        preferences.setCaptureVideoSize(
                Integer.parseInt(binding.etCaptureW.getText().toString()),
                Integer.parseInt(binding.etCaptureH.getText().toString()));

        preferences.setCaptureVideoFps(Integer.parseInt(binding.etCaptureF.getText().toString()));

        preferences.setVideoSize(
                Integer.parseInt(binding.etUpW.getText().toString()),
                Integer.parseInt(binding.etUpH.getText().toString()),
                Integer.parseInt(binding.etDownW.getText().toString()),
                Integer.parseInt(binding.etDownH.getText().toString()));

        preferences.setVideoFps(
                Integer.parseInt(binding.etUpF.getText().toString()),
                Integer.parseInt(binding.etDownF.getText().toString()));

        preferences.setSmallVideoSize(
                Integer.parseInt(binding.etSmallW.getText().toString()),
                Integer.parseInt(binding.etSmallH.getText().toString()));

        preferences.setSmallVideoFps(Integer.parseInt(binding.etSmallF.getText().toString()));

        preferences.setBandwidth(
                Integer.parseInt(binding.etUpBw.getText().toString()),
                Integer.parseInt(binding.etDownBw.getText().toString()));

        preferences.setMaxVideoFps(Integer.parseInt(binding.etMaxF.getText().toString()));

        preferences.setBandwidthSmall(Integer.parseInt(binding.etSmallBw.getText().toString()));

        preferences.setCapturePresentationVideoSize(
                Integer.parseInt(binding.etPresentationCaptureW.getText().toString()),
                Integer.parseInt(binding.etPresentationCaptureH.getText().toString()));

        preferences.setCapturePresentationVideoFps(Integer.parseInt(binding.etPresentationCaptureF.getText().toString()));

        preferences.setPresentationVideoSize(
                Integer.parseInt(binding.etPresentationUpW.getText().toString()),
                Integer.parseInt(binding.etPresentationUpH.getText().toString()));

        preferences.setPresentationVideoFps(Integer.parseInt(binding.etPresentationUpF.getText().toString()));

        preferences.setBandwidthPresentation(Integer.parseInt(binding.etPresentationBw.getText().toString()));

        preferences.setPresentationMaxVideoFps(Integer.parseInt(binding.etPresentationMaxF.getText().toString()));

        preferences.setSimulcast(binding.sRecvStream.isChecked());
        preferences.setMultistream(binding.sSendStream.isChecked());
        preferences.setEnableH264HardwareEncoder(binding.sEnableH264Encoder.isChecked());
        preferences.setDisableH264HardwareDecoder(binding.sDisableH264Decoder.isChecked());
//        preferences.setDisableCameraEncoder(binding.sDisableCameraEncoder.isChecked());
        preferences.setPrintLogs(binding.sPrintLogs.isChecked());

        String speakerphone = "auto";
        if (binding.rbAuto.isChecked()) {
            speakerphone = "auto";
        } else if (binding.rbEnabled.isChecked()) {
            speakerphone = "true";
        } else if (binding.rbDisabled.isChecked()) {
            speakerphone = "false";
        }
        preferences.setSpeakerphone(speakerphone);

        Toast.makeText(this,"保存成功！",Toast.LENGTH_SHORT).show();

        finish();
    }

    public void check(View view) {
        String url = binding.etApiServer.getText().toString().split(":")[0];
        preferences.setServerAddress(url, "443", new CallBack() {
            @Override
            public void success(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(SettingActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void failure(String reason) {
                runOnUiThread(() -> {
                    Toast.makeText(SettingActivity.this, reason, Toast.LENGTH_SHORT).show();
                });
            }
        });
//        preferences.setServerAddress(etApiServer.getText().toString(), );
    }
}
