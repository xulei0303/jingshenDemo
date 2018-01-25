package com.sprd.gallery3d.refocusimage.arcrealtime;

import com.sprd.gallery3d.refocusimage.RefocusData;
import com.sprd.gallery3d.refocusimage.RefocusUtils;

public class ArcRealBokehData extends RefocusData {

    private String bokehFlag;

    public ArcRealBokehData(byte[] content) {
        super(TYPE_BOKEH_ARC);
        initData(content);
    }

    public void initData(byte[] content) {
        bokehFlag = RefocusUtils.getStringValue(content, 1); // 1 bokeh Flag -> BOKE
        rotation = RefocusUtils.getIntValue(content, 2); //2 rotation
        sel_y = RefocusUtils.getIntValue(content, 3); //3 inPositionY
        sel_x = RefocusUtils.getIntValue(content, 4); //4 inPositionX
        blurIntensity = RefocusUtils.getIntValue(content, 5); // 5 i32BlurIntensity -> [0,60]
        depthSize = RefocusUtils.getIntValue(content, 6); // 6 DepthSizeData  depth size
        mainYuvSize = RefocusUtils.getIntValue(content, 7); // 7 MainSizeData mainyuv size
        yuvHeight = RefocusUtils.getIntValue(content, 8);// 8 MainHeightData mainyuv Height
        yuvWidth = RefocusUtils.getIntValue(content, 9); // 9 MainWidethData mianyuv Wideth
        jpegSize = content.length - 9 * 4 - depthSize - mainYuvSize;
        mainYuv = new byte[mainYuvSize];
        depthData = new byte[depthSize];
        int mainYuvIndex = jpegSize;
        int depthIndex = mainYuvIndex + mainYuvSize;
        System.arraycopy(content, mainYuvIndex, mainYuv, 0, mainYuvSize);
        System.arraycopy(content, depthIndex, depthData, 0, depthSize);
    }

    public byte[] getDepthData() {
        return depthData;
    }

    public void setDepthData(byte[] depthData) {
        this.depthData = depthData;
    }

    public int getDepthSize() {
        return depthSize;
    }

    public void setDepthSize(int depthSize) {
        this.depthSize = depthSize;
    }

    public String getBokehFlag() {
        return bokehFlag;
    }

    public void setBokehFlag(String bokehFlag) {
        this.bokehFlag = bokehFlag;
    }

    @Override
    public String toString() {
        return "ArcRealBokehData{" +
                "\nyuvWidth=" + yuvWidth +
                ", \nyuvHeight=" + yuvHeight +
                ", \nmainYuvSize=" + mainYuvSize +
                ", \ndepthSize=" + depthSize +
                ", \nblurIntensity=" + blurIntensity +
                ", \nsel_x=" + sel_x +
                ", \nsel_y=" + sel_y +
                ", \nrotation=" + rotation +
                ", \nbokehFlag='" + bokehFlag + '\'' +
                '}';
    }


}
