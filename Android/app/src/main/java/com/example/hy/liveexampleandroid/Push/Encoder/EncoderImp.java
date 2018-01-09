package com.example.hy.liveexampleandroid.Push.Encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Size;

import com.example.hy.liveexampleandroid.Push.PusherImp;
import com.example.hy.liveexampleandroid.Push.Queue.QueueManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by UPC on 2018/1/7.
 */

public class EncoderImp implements Encoder {
    private static final String TAG = "EncoderImp";
    private Thread mEncodeThread;
    private String mEncodeType = null;
    private Size mEncodeSize = null;
    private MediaCodec mMediaCodec = null;
    private MediaFormat mMediaFormat = null;
    private boolean isRuning;
    private FileOutputStream fileOutputStream;
    private byte[] configureByte;  //the configure info before the key frame
    private int TIMEOUT_USEC = 12000;
    private int frame_rate = 30;
    private byte[] yuv420;

    public EncoderImp() {
        try {
            fileOutputStream = new FileOutputStream(new File(Environment.getExternalStorageDirectory().getPath()
                    + "/testH264.h264"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        mEncodeThread = new Thread(() -> {
            isRuning = true;
            byte[] input = null;
            long pts = 0;
            long generateIndex = 0;
            yuv420 = new byte[mEncodeSize.getHeight() * mEncodeSize.getWidth()*3/2];
            while (isRuning) {
                if (QueueManager.getYUVQueueSize() > 0) {
                    input = QueueManager.pollDataFromYUVQueue();
                    swapYV12toNV12(input, yuv420, mEncodeSize.getWidth(), mEncodeSize.getHeight());
                }
                if (yuv420 != null) {
                    long startMs = System.currentTimeMillis();
                    int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
                    if (inputBufferIndex >= 0) {
                        pts = computePresentationTime(generateIndex);
                        ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
                        inputBuffer.clear();
                        inputBuffer.put(yuv420);
                        mMediaCodec.queueInputBuffer(inputBufferIndex, 0, yuv420.length, pts, 0);
                        generateIndex += 1;
                    }
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    int outPutBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                    while (outPutBufferIndex >= 0) {
                        ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outPutBufferIndex);
                        byte[] outData = new byte[bufferInfo.size];
                        outputBuffer.get(outData);
                        if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                            configureByte = new byte[bufferInfo.size];
                            configureByte = outData;

                        } else if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                            byte[] keyFrame = new byte[bufferInfo.size + configureByte.length];
                            System.arraycopy(configureByte, 0, keyFrame, 0, configureByte.length);
                            System.arraycopy(outData, 0, keyFrame, configureByte.length, outData.length);
                            try {
                                fileOutputStream.write(keyFrame, 0, keyFrame.length);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                fileOutputStream.write(outData, 0, outData.length);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                        mMediaCodec.releaseOutputBuffer(outPutBufferIndex, false);
                        outPutBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                    }
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        });
    }

    @Override
    public void initial() {
        if (mEncodeType == null)
            mEncodeType = MediaFormat.MIMETYPE_VIDEO_AVC;
        if (mEncodeSize == null)
            mEncodeSize = PusherImp.supportSize[0];

        mMediaFormat = MediaFormat.createVideoFormat(mEncodeType, mEncodeSize.getWidth(), mEncodeSize.getHeight());
        mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mEncodeSize.getWidth() * mEncodeSize.getHeight() * 5);
        mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frame_rate);
        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        try {
            mMediaCodec = MediaCodec.createEncoderByType(mEncodeType);
        } catch (IOException e) {
            //ToastUtil.toast();
            e.printStackTrace();
            return;
        }
        mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        //   mMediaCodec.start();
    }

    @Override
    public void startEncoder() {
        mMediaCodec.start();
        mEncodeThread.start();
    }

    @Override
    public void stopEncoder() {
        isRuning = false;
        // mMediaCodec.stop();
        // mMediaCodec.release();
    }

    @Override
    public void onDestroy() {

    }

    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / frame_rate;
    }

    private void swapYV12toI420(byte[] yv12bytes, byte[] i420bytes, int width, int height) {
        System.arraycopy(yv12bytes, 0, i420bytes, 0, width * height);
        System.arraycopy(yv12bytes, width * height + width * height / 4, i420bytes, width * height, width * height / 4);
        System.arraycopy(yv12bytes, width * height, i420bytes, width * height + width * height / 4, width * height / 4);
    }

    //this is work on my phone,it's probably cause the color problem on different device
    //switch YV12
    private void swapYV12toNV12(byte[] yv12bytes, byte[] nv12bytes, int width, int height) {
        int nLenY = width * height;
        int nLenU = nLenY / 4;

        System.arraycopy(yv12bytes, 0, nv12bytes, 0, width * height);
        for (int i = 0; i < nLenU; i++) {
            nv12bytes[nLenY + 2 * i + 1] = yv12bytes[nLenY + i+nLenU];//u    odd number is u
            nv12bytes[nLenY + 2 * i] = yv12bytes[nLenY  + i];//y             even number is y
        }
    }
}