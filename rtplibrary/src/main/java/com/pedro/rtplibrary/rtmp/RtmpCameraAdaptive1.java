package com.pedro.rtplibrary.rtmp;

/*
* Extends RtmpCamera with simple adaptive bitrate algorithm with callbacks
*
*
* */

import android.content.Context;
import android.media.MediaCodec;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;

import com.pedro.rtplibrary.base.Camera1Base;
import com.pedro.rtplibrary.view.LightOpenGlView;
import com.pedro.rtplibrary.view.OpenGlView;

import net.ossrs.rtmp.ConnectCheckerRtmp;
import net.ossrs.rtmp.SrsFlvMuxer;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

public class RtmpCameraAdaptive1 extends RtmpCamera1 {


    private static final String TAG = "RtmpCameraAdaptive1";
    private Timer adaptiveStreamingTimer = null;
    private Boolean adaptiveStreamingEnabled = true;
    private int initialVideoBitrate=0;

    public interface AdaptiveStreamingCallback {
        void onStreamingStat(int bitrate, int frameCount);
    }

    private AdaptiveStreamingCallback ascallback = null;
    public void setAdaptiveStreamingCallback(AdaptiveStreamingCallback cb) {
        ascallback=cb;
    }

    public RtmpCameraAdaptive1(SurfaceView surfaceView, ConnectCheckerRtmp connectChecker) {
        super(surfaceView, connectChecker);
    }

    public RtmpCameraAdaptive1(TextureView textureView, ConnectCheckerRtmp connectChecker) {
        super(textureView, connectChecker);

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public RtmpCameraAdaptive1(OpenGlView openGlView, ConnectCheckerRtmp connectChecker) {
        super(openGlView, connectChecker);

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public RtmpCameraAdaptive1(LightOpenGlView lightOpenGlView, ConnectCheckerRtmp connectChecker) {
        super(lightOpenGlView, connectChecker);

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public RtmpCameraAdaptive1(Context context, ConnectCheckerRtmp connectChecker) {
        super(context, connectChecker);

    }


    @Override
    protected void startStreamRtp(String url) {
        super.startStreamRtp(url);
        startAdaptiveTuner();
    }

    @Override
    protected void stopStreamRtp() {
        stopAdaptiveTuner();
        super.stopStreamRtp();
    }

    public boolean prepareVideo(int width, int height, int fps, int bitrate, boolean hardwareRotation,
                                int iFrameInterval, int rotation) {
        initialVideoBitrate=bitrate;
        return super.prepareVideo(width, height, fps, bitrate, hardwareRotation, iFrameInterval, rotation);
    }

    public boolean prepareVideo(int width, int height, int fps, int bitrate, boolean hardwareRotation,
                                int rotation) {
        initialVideoBitrate=bitrate;
        return super.prepareVideo(width, height, fps, bitrate, hardwareRotation, rotation);
    }


    private void startAdaptiveTuner(){

        if (adaptiveStreamingEnabled) {
            adaptiveStreamingTimer = new Timer();
            adaptiveStreamingTimer.schedule(new TimerTask() {
                public int previousFrameCount;
                public int frameQueueIncreased;

                @Override
                public void run() {

                    int frameCountInQueue = srsFlvMuxer.getFrameCountInQueue();
                    Log.d(TAG, "audiovideo frameCountInQueue : " + frameCountInQueue);
                    if (frameCountInQueue > previousFrameCount || frameCountInQueue==srsFlvMuxer.QCAPACITY) {
                        frameQueueIncreased++;
                    }
                    else {
                        frameQueueIncreased--;
                    }
                    previousFrameCount = frameCountInQueue;

                    if (frameQueueIncreased > 5) {
                        //decrease bitrate

                        Log.d(TAG, "try to decrease bitrate");
                        decreaseBitrate(false);
                        frameQueueIncreased = 0;
                    }

                    if (frameQueueIncreased < -10) {
                        //increase bitrate
                        Log.d(TAG, "try to increase bitrate");
                        increaseBitrate(false);

                        frameQueueIncreased = 0;
                    }
                    if (ascallback!=null)
                        ascallback.onStreamingStat(videoEncoder.getBitRate(), frameCountInQueue);
                }
            }, 0, 500);
        }
    }

    private void stopAdaptiveTuner(){
        if (adaptiveStreamingTimer != null) {
            adaptiveStreamingTimer.cancel();
            adaptiveStreamingTimer = null;
        }
    }

    public void increaseBitrate(boolean force) {
        int delta = initialVideoBitrate/10;
        int b = videoEncoder.getBitRate()+delta;

        if (b<initialVideoBitrate||force) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                setVideoBitrateOnFly(b);
                Log.i(TAG, "increaseBitrate=" + b);
            }
        }
    }

    public void decreaseBitrate(boolean force) {
        int delta = initialVideoBitrate/5;
        int b = videoEncoder.getBitRate()-delta;
        if (b>initialVideoBitrate/10 || force) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                super.setVideoBitrateOnFly(b);
                Log.i(TAG, "decreaseBitrate=" + b);
            }
        }
    }
}
