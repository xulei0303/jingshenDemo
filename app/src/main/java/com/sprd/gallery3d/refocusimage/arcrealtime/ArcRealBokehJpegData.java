package com.sprd.gallery3d.refocusimage.arcrealtime;

import com.sprd.gallery3d.refocusimage.RefocusData;
import com.sprd.gallery3d.refocusimage.RefocusUtils;

    /*
    1.mJpegImageData：jpeg1
    2.mMainJpegImageData：jpeg ori
    3.mDepthYuvImageData：depth
    4.MainWidthData
    5.MainHeightData
    6.DepthSizeData
    7.i32BlurIntensity
    8. InPositionX
    9. InPositionY
    10. rotation
    11. mJpegImageData size ->jpeg1 size
    12.mMainJpegImageData—size -> ori jpeg size
    13.BOKE
    */

public class ArcRealBokehJpegData extends RefocusData {

    private String bokehFlag;
    private int oriJpegSize;

    public ArcRealBokehJpegData(byte[] content) {
        super(TYPE_BOKEH_ARC);
        initData(content);
    }

    public void initData(byte[] content) {
        bokehFlag = RefocusUtils.getStringValue(content, 1); // 1 bokeh Flag -> BOKE
        oriJpegSize = RefocusUtils.getIntValue(content, 2);
        // int jpegSize = RefocusUtils.getIntValue(content, 3); // no use, just check.
        rotation = RefocusUtils.getIntValue(content, 4);
        sel_y = RefocusUtils.getIntValue(content, 5);
        sel_x = RefocusUtils.getIntValue(content, 6);
        blurIntensity = RefocusUtils.getIntValue(content, 7);//[0,60]
        depthSize = RefocusUtils.getIntValue(content, 8);
        yuvHeight = RefocusUtils.getIntValue(content, 9);
        yuvWidth = RefocusUtils.getIntValue(content, 10);
        jpegSize = content.length - 10 * 4 - depthSize - oriJpegSize; ////eq jpegSize, check

        oriJpeg = new byte[oriJpegSize];
        depthData = new byte[depthSize];
        System.arraycopy(content, jpegSize, oriJpeg, 0, oriJpegSize);
        System.arraycopy(content, jpegSize + oriJpegSize, depthData, 0, depthSize);

    }

    public String getBokehFlag() {
        return bokehFlag;
    }

    public void setBokehFlag(String bokehFlag) {
        this.bokehFlag = bokehFlag;
    }

    public int getOriJpegSize() {
        return oriJpegSize;
    }

    public void setOriJpegSize(int oriJpegSize) {
        this.oriJpegSize = oriJpegSize;
    }

    @Override
    public String toString() {
        return "ArcRealBokehJpegData{" +
                "\nyuvWidth=" + yuvWidth +
                ", \nyuvHeight=" + yuvHeight +
                ", \ndepthSize=" + depthSize +
                ", \nblurIntensity=" + blurIntensity +
                ", \nsel_x=" + sel_x +
                ", \nsel_y=" + sel_y +
                ", \nrotation=" + rotation +
                ", \njpegSize=" + jpegSize +
                ", \noriJpegSize=" + oriJpegSize +
                ", \nbokehFlag='" + bokehFlag + '\'' +
                '}';
    }


}
