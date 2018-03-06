package com.sprd.gallery3d.refocusimage;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore.Images.ImageColumns;
import android.text.TextUtils;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RefocusUtils {
    private static final String TAG = "RefocusUtils";
    public static final String DEBUG_ROOT_PATH = "sdcard/refocus";
    public static byte[] intToBytes(int value) {
        byte[] src = new byte[4];
        src[3] = (byte) ((value >> 24) & 0xFF);
        src[2] = (byte) ((value >> 16) & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }

    public static int bytesToInt(byte[] src, int offset) {
        int value;
        value = ((src[offset] & 0xFF)
                | ((src[offset + 1] & 0xFF) << 8)
                | ((src[offset + 2] & 0xFF) << 16)
                | ((src[offset + 3] & 0xFF) << 24));
        return value;
    }

    public static String bytesToString(byte[] bytes, int offset) {
        char a = (char) (bytes[offset] & 0xFF);
        char b = (char) (bytes[offset + 1] & 0xFF);
        char c = (char) (bytes[offset + 2] & 0xFF);
        char d = (char) (bytes[offset + 3] & 0xFF);
        String s = new String(new char[]{a, b, c, d});
        return s;
    }

    public static int getIntValue(byte[] content, int position) {
        int intValue = bytesToInt(content, content.length - position * 4);
        return intValue;
    }

    public static String getStringValue(byte[] content, int position) {
        String stringValue = bytesToString(content, content.length - position * 4);
        return stringValue;
    }

    public static byte[] streamToByte(InputStream inStream) {
        byte[] data = null;
        try {
            Log.d(TAG, "streamToByte start.");
            data = new byte[inStream.available()];
            inStream.read(data);
            Log.d(TAG, "streamToByte end.");
        } catch (Exception e) {
            Log.e(TAG, "streamToByte Exception ", e);
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (Throwable t) {
                    // do nothing
                }
            }
        }
        return data;
    }

    // yuv rotate 90 Degree
    public static byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        // Rotate the Y
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i--;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
                i--;
            }
        }
        return yuv;
    }

    // yuv rotate 180 Degree
    public static byte[] rotateYUV420Degree180(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];

        int i = 0;
        int count = 0;
        for (i = imageWidth * imageHeight - 1; i >= 0; i--) {
            yuv[count] = data[i];
            count++;
        }
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (i = imageWidth * imageHeight * 3 / 2 - 1; i >= imageWidth
                * imageHeight; i -= 2) {
            yuv[count++] = data[i - 1];
            yuv[count++] = data[i];
        }
        return yuv;
    }

    // yuv rotate 270 Degree
    public static byte[] rotateYUV420Degree270(byte[] data, int imageWidth, int imageHeight) {

        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        // Rotate the Y luma
        int i = 0;
        for (int x = imageWidth - 1; x >= 0; x--) {
            for (int y = 0; y < imageHeight; y++) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth * imageHeight;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
                i++;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i++;
            }
        }
        return yuv;
    }

    public static Bitmap loadBitmap(Context context, Uri uri, Options o) {
        if (uri == null || context == null) {
            throw new IllegalArgumentException("bad argument to loadBitmap");
        }
        InputStream is = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            Bitmap result = BitmapFactory.decodeStream(is, null, o);
            is.close();
            return result;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Memory it too low, load bitmap failed." + e);
            System.gc();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException for " + uri, e);
        } catch (Exception e) {
            Log.e(TAG, "SecurityException for " + uri, e);
        } finally {
        }
        return null;
    }

    public static Bitmap rotateBitmap(Bitmap origin, float orientation) {
        if (orientation == 0.0f) {
            Log.d(TAG, "not need rotate !");
            return origin;
        }
        Log.d(TAG, "rotateBitmap start.");
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.setRotate(orientation);
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        origin.recycle();
        Log.d(TAG, "rotateBitmap end.");
        return newBM;
    }

    public static File saveJpegByYuv(Context context, byte[] bytes, int yuvW, int yuvH, int rotate) {

        OutputStream output = null;
        File file = null;
        try {
            byte[] jdata = getPicByteByYuv(bytes, yuvW, yuvH, rotate);
            Bitmap bitmap = BitmapFactory.decodeByteArray(jdata, 0, jdata.length, null);
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
            String name = df.format(new Date());
            String savePath = Environment.getExternalStorageDirectory().toString();
            file = writeToLocalFile(bitmap, "new_refocus_" + name + ".jpg", savePath);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (null != output) {
                try {
                    output.close();
                } catch (Exception t) {
                    t.printStackTrace();
                }
            }
        }
        return file;
    }

    public static File writeToLocalFile(Bitmap bitmap, String name, String path) {
        if (TextUtils.isEmpty(name) || bitmap == null) {
            return null;
        }
        FileOutputStream fileOutputStream;
        File filepath = new File(path);
        if (!filepath.exists()) {
            filepath.mkdirs();
        }
        File file = new File(filepath, name);
        if (file.exists()) {
            file.delete();
        }
        try {
            boolean newFile = file.createNewFile();
            if (newFile) {
                fileOutputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
                fileOutputStream.flush();
                fileOutputStream.close();
                Log.d(TAG, "write new file:" + name + " to " + path);
                fileOutputStream.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    public static Point srcToYuvPoint(Point srcPoint, int SrcW, int SrcH, int rotate) {
        int yuvX = 0;
        int yuvY = 0;
        switch (rotate) {
            case 0:
                yuvX = srcPoint.x;
                yuvY = srcPoint.y;
                break;
            case 90:
                yuvX = srcPoint.y;
                yuvY = SrcW - srcPoint.x;
                break;
            case 180:
                yuvX = SrcW - srcPoint.x;
                yuvY = SrcH - srcPoint.y;
                break;
            case 270:
                yuvX = SrcH - srcPoint.y;
                yuvY = srcPoint.x;
                break;
            default:
                break;
        }
        Point yuvPoint = new Point(yuvX, yuvY);
        return yuvPoint;
    }

    public static Point yuvToSrcPoint(Point yuvPoint, int SrcW, int SrcH, int rotate) {
        int srcX = 0;
        int srcY = 0;
        switch (rotate) {
            case 0:
                srcX = yuvPoint.x;
                srcY = yuvPoint.y;
                break;
            case 90:
                srcX = SrcW - yuvPoint.y;
                srcY = yuvPoint.x;
                break;
            case 180:
                srcX = SrcW - yuvPoint.x;
                srcY = SrcH - yuvPoint.y;
                break;
            case 270:
                srcX = yuvPoint.y;
                srcY = SrcH - yuvPoint.x;
                break;
            default:
                break;
        }
        Point srcPoint = new Point(srcX, srcY);
        return srcPoint;
    }

    /**
     * @param bytes yuv byte
     * @param w     yuv w
     * @param h     yuv h
     * @param opts
     * @return
     */
    public static Bitmap getPicFromBytes(byte[] bytes, int w, int h, Options opts) {
        if (bytes == null) {
            Log.e(TAG, "getPicFromBytes bytes is null !");
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Bitmap bitmap = null;
        try {
            Log.d(TAG, "getPicFromBytes  compressToJpeg  start . ");
            YuvImage yuvimage = new YuvImage(bytes, ImageFormat.NV21, w, h, null);
            yuvimage.compressToJpeg(new Rect(0, 0, w, h), 75, out);
            Log.d(TAG, "getPicFromBytes  compressToJpeg  end . ");
            byte[] jdata = out.toByteArray();
            bitmap = BitmapFactory.decodeByteArray(jdata, 0, jdata.length, opts);
            Log.d(TAG, "getPicFromBytes decodeByteArray success .");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (Exception e) {
                // do nothing
            }
        }
        return bitmap;
    }


    public static byte[] getPicByteByYuv(byte[] bytes, int yuvW, int yuvH, int rotation) {
        if (bytes == null) {
            Log.e(TAG, "getPicFromBytes bytes is null !");
            return null;
        }
        Log.d(TAG, "getPicByteByYuv compressToJpeg start . ");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuvimage;
        switch (rotation) {
            case 0:
                yuvimage = new YuvImage(bytes, ImageFormat.NV21, yuvW, yuvH, null);
                yuvimage.compressToJpeg(new Rect(0, 0, yuvW, yuvH), 90, out);
                break;
            case 90:
                yuvimage = new YuvImage(rotateYUV420Degree90(bytes, yuvW, yuvH), ImageFormat.NV21, yuvH, yuvW, null);
                yuvimage.compressToJpeg(new Rect(0, 0, yuvH, yuvW), 90, out);
                break;
            case 180:
                yuvimage = new YuvImage(rotateYUV420Degree180(bytes, yuvW, yuvH), ImageFormat.NV21, yuvW, yuvH, null);
                yuvimage.compressToJpeg(new Rect(0, 0, yuvW, yuvH), 90, out);
                break;
            case 270:
                yuvimage = new YuvImage(rotateYUV420Degree270(bytes, yuvW, yuvH), ImageFormat.NV21, yuvH, yuvW, null);
                yuvimage.compressToJpeg(new Rect(0, 0, yuvH, yuvW), 90, out);
                break;
        }
        Log.d(TAG, "getPicByteByYuv compressToJpeg end . ");
        byte[] picBytes = out.toByteArray();
        try {
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return picBytes;
    }

/*
    public static void saveByPath(Context context, byte[] picBytes, String filePath) {
        Log.d(TAG, "saveByPath filePath = " + filePath);
        ByteArrayOutputStream outputPicBytes = null;
        RandomAccessFile raf = null;
        try {
            outputPicBytes = new ByteArrayOutputStream();
            ExifInterface exif = new ExifInterface();
            exif.readExif(filePath);
            // change jepg's exif tag value.
            exif.setTagValue(ExifInterface.TAG_CAMERATYPE_IFD, new Integer(BokehSaveManager.TYPE_MODE_BOKEH));
            exif.writeExif(picBytes, outputPicBytes);
            byte[] exifPicBytes = outputPicBytes.toByteArray();
            raf = new RandomAccessFile(filePath, "rw");
            raf.seek(0);
            raf.write(exifPicBytes);
            MediaScannerConnection.scanFile(context, new String[]{filePath}, null, null);
            Log.d(TAG, "saveByPath end.");
        } catch (IOException e) {
            Log.e(TAG, "Exception writing jpeg file", e);
        } finally {
            try {
                if (outputPicBytes != null) {
                    outputPicBytes.close();
                }
                if (raf != null) {
                    raf.close();
                }
            } catch (Throwable t) {
                // do nothing
            }
        }
    }
    */

/*
    public static void saveSrByPath(Context context, byte[] picBytes, byte[] srYuv, int srYuvIndex, int srFlagIndex, String filePath) {
        Log.d(TAG, "saveByPath filePath = " + filePath);
        ByteArrayOutputStream outputPicBytes = null;
        RandomAccessFile raf = null;
        try {
            outputPicBytes = new ByteArrayOutputStream();
            ExifInterface exif = new ExifInterface();
            exif.readExif(filePath);
            // change jepg's exif tag value.
            exif.setTagValue(ExifInterface.TAG_CAMERATYPE_IFD, new Integer(BokehSaveManager.TYPE_MODE_BOKEH));
            exif.writeExif(picBytes, outputPicBytes);
            byte[] exifPicBytes = outputPicBytes.toByteArray();
            raf = new RandomAccessFile(filePath, "rw");
            raf.seek(0);
            raf.write(exifPicBytes); //save jpeg
            raf.seek(srYuvIndex);
            raf.write(srYuv);  //save after sr yuv
            raf.seek(srFlagIndex);
            byte[] srSaveFlag = intToBytes(1);
            raf.write(srSaveFlag); //save SrSaveFlag
            MediaScannerConnection.scanFile(context, new String[]{filePath}, null, null);
            Log.d(TAG, "saveByPath end.");
        } catch (IOException e) {
            Log.e(TAG, "Exception writing jpeg file", e);
        } finally {
            try {
                if (outputPicBytes != null) {
                    outputPicBytes.close();
                }
                if (raf != null) {
                    raf.close();
                }
            } catch (Throwable t) {
                // do nothing
            }
        }
    }
    */

    public static void saveNewJpeg(Context context, byte[] data, Uri uri, String path) {
        OutputStream outputStream = null;
        try {
            outputStream = context.getContentResolver().openOutputStream(uri);
            outputStream.write(data);
            if (!TextUtils.isEmpty(path)) {
                Log.d(TAG, "save new jpeg,and scanFile !");
                MediaScannerConnection.scanFile(context,
                        new String[]{path}, null, null);
            }
        } catch (IOException e) {
            Log.e(TAG, "Exception while writing debug jpeg file", e);
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Throwable t) {
                // do nothing
            }
        }
    }

    /*
    public static byte[] jpegAddExif(byte[] picBytes, String path) {
        byte[] exifPicBytes = null;
        try {
            ByteArrayOutputStream outputPicBytes = new ByteArrayOutputStream();
            ExifInterface exif = new ExifInterface();
            exif.readExif(path);
            exif.setTagValue(ExifInterface.TAG_CAMERATYPE_IFD, new Integer(BokehSaveManager.TYPE_MODE_BOKEH));
            exif.writeExif(picBytes, outputPicBytes);
            exifPicBytes = outputPicBytes.toByteArray();
            outputPicBytes.close();
        } catch (Exception e) {
            Log.d(TAG, "jpegAddExif fail! ", e);
        }
        return exifPicBytes;
    }

    public static void saveSr(Context context, String filePath, byte[] afterSrYuv){
        ByteArrayOutputStream outputPicBytes = null;
        RandomAccessFile raf = null;
        try {
            outputPicBytes = new ByteArrayOutputStream();
            byte[] exifPicBytes = outputPicBytes.toByteArray();
            raf = new RandomAccessFile(filePath, "rw");
            raf.seek(0);
            raf.write(exifPicBytes);
            MediaScannerConnection.scanFile(context, new String[]{filePath}, null, null);
            Log.d(TAG, "saveByPath end.");
        } catch (IOException e) {
            Log.e(TAG, "Exception writing jpeg file", e);
        } finally {
            try {
                if (outputPicBytes != null) {
                    outputPicBytes.close();
                }
                if (raf != null) {
                    raf.close();
                }
            } catch (Throwable t) {
                // do nothing
            }
        }
    }
    */


    public static void refSaveDepth(Context context, Uri uri, byte[] depth) {
        RandomAccessFile raf = null;
        Cursor cursor = null;
        String filePath = "";
        try {
            cursor = context.getContentResolver().query(uri,
                    new String[]{ImageColumns.DATA}, null, null, null);
            if (cursor.moveToFirst()) {
                filePath = cursor.getString(0); // query _data
                Log.d(TAG, "refSaveDepth filePath is = " + filePath);
            }
            byte[] newHasDepthflag = RefocusUtils.intToBytes(1); // new hasDepth flag is 1
            byte[] depthLength = RefocusUtils.intToBytes(depth.length);

            raf = new RandomAccessFile(filePath, "rw");
            long fileLength = raf.length();
            raf.seek(fileLength);
            raf.write(depth);
            raf.seek(fileLength + depth.length);
            raf.write(depthLength);
            raf.seek(fileLength + depth.length + depthLength.length);
            raf.write(newHasDepthflag);
            MediaScannerConnection.scanFile(context, new String[]{filePath}, null, null);
            Log.d(TAG, "saveByPath end.");
        } catch (IOException e) {
            Log.e(TAG, "Exception writing jpeg file", e);
        } finally {
            try {
                if (cursor != null) {
                    cursor.close();
                }
                if (raf != null) {
                    raf.close();
                }
            } catch (Throwable t) {
                // do nothing
            }
        }
    }

    public static void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    // use progress to calculate refocus image current value.
    public static String getRefCurValue(int progress) {
        float percent = progress / 255f;
        float blur = 8f - (percent * 6f);
        DecimalFormat df = new DecimalFormat(".0");
        return "F" + df.format(blur);
    }


    public static void writeByteData(byte[] bytes, String name, String path) {
        if (bytes == null || bytes.length == 0) return;
        try {
            File filepath = new File(path);
            if (!filepath.exists()) {
                filepath.mkdirs();
            }
            File data = new File(filepath, name);
            if (data.exists()){
                data.delete();
            }
            data.createNewFile();
            FileOutputStream depth = new FileOutputStream(data);
            depth.write(bytes);
            Log.d(TAG, "write Byte Data " + name + " to -->:" + data);
            depth.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "writeByteData: FileNotFoundException ");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "writeByteData: IOException");
            e.printStackTrace();
        }
    }

    public static String getDumpPath(String filePath) {
        String dirName = filePath.substring(filePath.lastIndexOf("/"), filePath.lastIndexOf("."));
        Log.d(TAG, "dirName = " + dirName);
        return DEBUG_ROOT_PATH + dirName;
    }

    public static byte[] bitmap2yuv(Bitmap bitmap) {
        Log.d(TAG, "bitmap2yuv");
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] buffer = new int[width * height];
        bitmap.getPixels(buffer, 0, width, 0, 0, width, height);
        return rgb2yuv(buffer, width, height);
    }

    public static byte[] rgb2yuv(int[] argb, int width, int height) {
        Log.d(TAG, "rgb2yuv start.");
        int len = width * height;
        byte[] yuv = new byte[len * 3 / 2];
        int yIndex = 0;
        int uvIndex = len;
        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                // a = (argb[index] & 0xff000000) >> 24;// no use
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;
                Y = (77 * R + 150 * G + 29 * B) >> 8;
                yuv[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    U = ((-43 * R - 85 * G + 128 * B) >> 8) + 128;
                    V = ((128 * R - 107 * G - 21 * B) >> 8) + 128;
                    yuv[uvIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                    yuv[uvIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                }
                index++;
            }
        }
        Log.d(TAG, "return yuv, rgb2yuv end .");
        return yuv;
    }

}
