package com.sprd.gallery3d.refocusimage.arcrealtime;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.util.Log;

import com.sprd.gallery3d.refocusimage.CommonRefocus;
import com.sprd.gallery3d.refocusimage.RefocusUtils;

public class ArcRealBokeh extends CommonRefocus {
    private static final String TAG = "ArcRealBokeh";
    private int depthSize;

    static {
        System.loadLibrary("jni_arcsoft_real_bokeh");
    }

    public ArcRealBokeh(ArcRealBokehData data) {
        super(data);
        initData(data);
    }

    private void initData(ArcRealBokehData data) {
        if (data == null) return;
        depthSize = data.getDepthSize();
    }

    public ArcRealBokeh(ArcRealBokehJpegData data) {
        super(data);
        initData(data);
    }

    private void initData(ArcRealBokehJpegData data) {
        if (data == null) return;
        depthSize = data.getDepthSize();
        byte[] oriJpeg = data.getOriJpeg();
        Log.d(TAG, "decoder oriJpegBitmap start");
        Bitmap oriJpegBitmap = BitmapFactory.decodeByteArray(oriJpeg, 0, oriJpeg.length, null);
        Log.d(TAG, "decoder oriJpegBitmap end");
        setMainYuv(RefocusUtils.bitmap2yuv(oriJpegBitmap));
    }

    @Override
    public void initLib() {
        Log.d(TAG, "initLib");
        mNeedSaveSr = false;
        init();
    }

    @Override
    public void unInitLib() {
        Log.d(TAG, "unInitLib");
        unInit();
    }

    @Override
    public int distance() {
        // arc no distance feature, return 0.
        return 0;
    }

    @Override
    public byte[] doRefocus(byte[] editYuv, Point point, int blurIntensity) {
        Log.d(TAG, "doRefocus");
        editYuv = refocus(editYuv, point.x, point.y, blurIntensity);
        return editYuv;
    }

    @Override
    public void calDepth() {
        // arc has depth, don't need calculate
    }

    @Override
    public int getProgress() {
        // hal to gallery BlurIntensity: [6,60]-> [1,10]*6
        // arc progress -> 0-9
        int orgProgress = 10 - (mData.getBlurIntensity() * 10 / 60);  // [0,9]
        return orgProgress;
    }

    @Override
    public int calBlurIntensity(int currentProgress) {
        // progress: 0-9, libF: 0-60, hal to gallery:[6-60]
        int blurIntensity = 60 - (currentProgress * 60 / 10); //[60-6], 60 is max blur
        return blurIntensity;
    }

    @Override
    public int doDepthRotate(byte[] depth, int width, int height, int angle) {
        return 0;
    }

    public byte[] refocus(byte[] out_array, int x, int y, int blurIntensity) {
        return refocus(mData.getYuvWidth(), mData.getYuvHeight(), depthSize, mData.getMainYuv(), out_array, mData.getDepthData(), x, y, blurIntensity);
    }

    public native int init();

    public native int unInit();

    public native byte[] refocus(int w, int h, int disparitySize, byte[] in_array, byte[] out_array, byte[] depth, int x, int y, int blurIntensity);


}
