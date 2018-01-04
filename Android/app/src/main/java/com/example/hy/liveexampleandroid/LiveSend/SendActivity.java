package com.example.hy.liveexampleandroid.LiveSend;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.hy.liveexampleandroid.R;
import com.example.hy.liveexampleandroid.ToastUtil;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Hamik Young on 2017/12/29.
 */

public class SendActivity extends AppCompatActivity implements
        SendView, View.OnClickListener {

    private Button mSendBtn;
    private TextureView mTextureView;
    private EditText mEditText;

    private SendPresenter presenter;


    private static final String TAG = "SendActivity";

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);
        mEditText = findViewById(R.id.push_IP);
        mSendBtn = findViewById(R.id.send_live_btn);
        mTextureView = findViewById(R.id.video_texture);
        mSendBtn.setOnClickListener(this);
        findViewById(R.id.take_pic_btn).setOnClickListener(this);
        presenter = new SendPresenterImp(this, new SendInteractorImp());
        presenter.initialPusher();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.setting_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.setting_item:
                showSettingPopWindow();
                break;
            case R.id.switch_camera:
                presenter.switchCamera();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void toastMessage(String message) {
        ToastUtil.toast(this, message, Toast.LENGTH_SHORT);
    }

    @Override
    public void showSettingPopWindow() {

    }

    @Override
    public void IpIsEmpty() {
        mEditText.setError(getString(R.string.Ip_Empty_Error));
    }

    @Override
    public void IpError() {
        mEditText.setError(getString(R.string.Ip_Error));
    }

    @Override
    public void btnTextChangeToStart() {
        mSendBtn.setText(R.string.startLive);
        mEditText.setEnabled(true);
    }

    @Override
    public void btnTextChangeToStop() {
        mSendBtn.setText(R.string.stopLive);
        mEditText.setEnabled(false);
    }

    @Override
    public TextureView supplyTextureView() {
        return mTextureView;
    }

    @Override
    public CameraManager supplyCameraManager() {
        return (CameraManager) getSystemService(CAMERA_SERVICE);
    }

    @Override
    public String getPushIp() {
        return mEditText.getText().toString();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.send_live_btn:
                if (mSendBtn.getText().toString().equals(getResources().getString(R.string.startLive))) {
                    presenter.startPushVideo(mEditText.getText().toString());
                } else {
                    presenter.stopPushVideo();
                }
                break;
            case R.id.take_pic_btn:
                presenter.takePic();
                break;
            default:
                break;
        }
    }
}