package com.sprd.gallery3d.refocusimage;

import android.graphics.Point;
import android.util.Log;

import com.sprd.gallery3d.refocusimage.arcrealtime.ArcRealBokeh;
import com.sprd.gallery3d.refocusimage.arcrealtime.ArcRealBokehData;
import com.sprd.gallery3d.refocusimage.arcrealtime.ArcRealBokehJpegData;

public abstract class CommonRefocus {
    private static final String TAG = "CommonRefocus";
    private static final String BOKEH_FLAG = "BOKE";
    private static final String BLUR_FLAG = "BLUR";
    protected long mNativeContext; // accessed by native methods
    protected long mNativeDstBuffer; // accessed by native methods
    protected static boolean ARC_ENABLE = false;
    protected static boolean SBS_SR_ENABLE = false;
    protected static boolean NEW_JPEG_DATA = false;
    protected int mType;
    protected RefocusData mData;
    protected boolean mNeedSaveSr = false;

    public CommonRefocus() {
    }

    public CommonRefocus(RefocusData data) {
        mData = data;
        mType = data.getType();
        Log.d(TAG, "mType = " + mType);
    }

    public static CommonRefocus getInstance(byte[] content) {
        ARC_ENABLE = true;
        SBS_SR_ENABLE = false;
        // NEW_JPEG_DATA = GalleryUtils.isNewJpegData();
        NEW_JPEG_DATA = false;
        Log.d(TAG, "ARC_ENABLE = " + ARC_ENABLE + ", SBS_SR_ENABLE = " + SBS_SR_ENABLE + ", NEW_JPEG_DATA = " + NEW_JPEG_DATA);
        return initRefocusType(content);
    }

    private static CommonRefocus initRefocusType(byte[] content) {
        Log.d(TAG, "initRefocusType start");
        CommonRefocus instance = null;
        String typeString = RefocusUtils.getStringValue(content, 1);

        if (BOKEH_FLAG.equalsIgnoreCase(typeString)) {
            if (ARC_ENABLE) {
                if (NEW_JPEG_DATA) {
                    ArcRealBokehJpegData arcJpegData = new ArcRealBokehJpegData(content);
                    Log.d(TAG, "new arc data is : " + arcJpegData.toString());
                    instance = new ArcRealBokeh(arcJpegData);
                } else {
                    ArcRealBokehData arcData = new ArcRealBokehData(content);
                    Log.d(TAG, "arc data is : " + arcData.toString());
                    instance = new ArcRealBokeh(arcData);
                }
            }
        }
        return instance;
    }

    // Debug, dump yuv, depth, params.
    public void dumpData(String filePath) {
        String dumpPath = RefocusUtils.getDumpPath(filePath);
        Log.d(TAG, "debug dumpData, dumpPath " + dumpPath);
        RefocusUtils.writeByteData(mData.getMainYuv(), "mainYuv.yuv", dumpPath);
        RefocusUtils.writeByteData(mData.getDepthData(), "depth.data", dumpPath);
        RefocusUtils.writeByteData(mData.toString().getBytes(), "Params.txt", dumpPath);
    }

    public int getType() {
        return mType;
    }

    public boolean isNeedSaveSr() {
        return mNeedSaveSr;
    }

    public boolean isNewJpegData() {
        return NEW_JPEG_DATA;
    }

    public void setMainYuv(byte[] mainYuv) {
        mData.setMainYuv(mainYuv);
    }

    public void setDepth(byte[] depth) {
        mData.setDepthData(depth);
    }

    public RefocusData getRefocusData() {
        return mData;
    }

    // init lib
    public abstract void initLib();

    // unInit lib
    public abstract void unInitLib();

    // calculate distance
    public abstract int distance();

    // do refocus
    public abstract byte[] doRefocus(byte[] editYuv, Point point, int blurIntensity);

    // some refocus no depth,need use two yuv to calculate depth
    public abstract void calDepth();

    // calculate progress by Original blur intensity
    public abstract int getProgress();

    // calculate blur intensity by progress
    public abstract int calBlurIntensity(int progress);

    // depth Rotate
    public abstract int doDepthRotate(byte depth[], int width, int height, int angle);
}
