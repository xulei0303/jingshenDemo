package com.sprd.gallery3d.refocusimage;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.sprd.gallery3d.refocusimage.RefocusImageView.RefocusViewCallback;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

public class RefocusEditActivity extends Activity implements Handler.Callback, RefocusViewCallback {
    private static final String TAG = "RefocusEditActivity";

    private int mType;
    public static final String SRC_PATH = "refocus_path";
    public static final String SRC_WIDTH = "refocus_width";
    public static final String SRC_HEIGHT = "refocus_height";
    private Handler mUiHandler, mThreadHandler;
    private HandlerThread mHandlerThread = null;
    private MenuItem mUndoItem, mRedoItem, mSaveItem;
    private Uri mUri;
    private String mFilePath;
    private String[] fNumList = new String[]{"F0.95", "F1.4", "F2.0", "F2.8", "F4.0", "F5.6", "F8.0", "F11.0", "F13.0", "F16.0"};
    private static final String REF_START = "F8.0";
    private static final String REF_END = "F2.0";
    private RefocusImageView mRefocusView;
    private ProgressBar mProgressBar;
    private static final int MSG_DECODE_SRC = 1 << 0;
    private static final int MSG_DISPLAY_SRC = 1 << 1;
    private static final int MSG_INIT_REFOCUS = 1 << 2;
    private static final int MSG_INIT_UI = 1 << 3;
    private static final int MSG_UPDATE_MENU = 1 << 4;
    private static final int MSG_HIDE_CIRCLE = 1 << 5;
    private static final int DELAY_HIDE_TIME = 1000;
    private Bitmap mSrcBitmap;
    private Bitmap mEditBitmap;
    private ContentResolver mResolver;
    private int mSrcWidth, mSrcHeight;
    private int mScale;
    private SeekBar mSeekBar;
    private TextView mStartValue, mEndValue, mCurValue;
    private byte[] mEditYuv;
    private int mYuvW, mYuvH;
    private int mRotate;
    private Point mYuvP = new Point();
    private Point mSrcP = new Point();
    private CommonRefocus mComRefocus;
    private boolean mPaused = true;

    public static boolean DEBUG = false;
    private int mOrgProgress, mCurProgress, mOldProgress;
    private boolean mDoReset;
    private RefocusTask mRefocusTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("xulei", "onCreate.");
        super.onCreate(savedInstanceState);
        initWidgets();
        initActionBar();
        initBase();
        processIntent();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume.");
        mPaused = false;
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause.");
        mPaused = true;
        if (mRefocusTask != null && !mRefocusTask.isCancelled()
                && mRefocusTask.getStatus() == AsyncTask.Status.RUNNING) {
            Log.d(TAG, "onPause, RefocusTask running,and cancel mRefocusTask!");
            mRefocusTask.cancel(true);
            mRefocusTask = null;
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy.");
        super.onDestroy();
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
        if (mThreadHandler != null) {
            mThreadHandler.removeCallbacksAndMessages(null);
        }
        if (mUiHandler != null) {
            mUiHandler.removeCallbacksAndMessages(null);
        }
        if (mComRefocus != null) {
            mComRefocus.unInitLib();
        }
        RefocusUtils.recycleBitmap(mSrcBitmap);
        RefocusUtils.recycleBitmap(mEditBitmap);
        mEditYuv = null;
        System.gc();
    }

    private void initBase() {
        mHandlerThread = new HandlerThread("RefocusEditActivity-Handler");
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper(), this); // this is "Handler.Callback"
        mUiHandler = new MainHandler(this);
    }

    private static class MainHandler extends Handler {
        private final WeakReference<RefocusEditActivity> mRefocusWeak;

        public MainHandler(RefocusEditActivity RefocusEditActivity) {
            mRefocusWeak = new WeakReference<>(RefocusEditActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            RefocusEditActivity RefocusEditActivity = mRefocusWeak.get();
            if (RefocusEditActivity != null) {
                RefocusEditActivity.handleMainMsg(msg);
            }
        }
    }

    /**
     * UiHandler's handleMessage. excute in main thread
     *
     * @param msg
     */
    private void handleMainMsg(Message msg) {
        switch (msg.what) {
            case MSG_DISPLAY_SRC:
                mRefocusView.setSrcBitmap(mSrcBitmap);
                break;
            case MSG_INIT_UI:
                Log.d(TAG, "init finished, update ui.");
                mProgressBar.setVisibility(View.GONE);
                mSeekBar.setEnabled(true);
                switch (mType) {
                    case RefocusData.TYPE_REFOCUS:
                        // refocus F8-F2  progress is 0-255
                        mSeekBar.setMax(255);
                        mSeekBar.setProgress(mOrgProgress);
                        mStartValue.setText(REF_START);
                        mEndValue.setText(REF_END);
                        String curValue = RefocusUtils.getRefCurValue(mOrgProgress);
                        mCurValue.setText(curValue);
                        break;
                    case RefocusData.TYPE_BOKEH_ARC:
                    case RefocusData.TYPE_BOKEH_SPRD:
                    case RefocusData.TYPE_BOKEH_SBS:
                    case RefocusData.TYPE_BLUR_GAUSS:
                    case RefocusData.TYPE_BLUR_FACE:
                    case RefocusData.TYPE_BLUR_TF:
                        // refocus F0.95-F16.0  progress is 0-9
                        mSeekBar.setMax(9);
                        mSeekBar.setProgress(mOrgProgress);
                        mStartValue.setText(fNumList[0]);
                        mEndValue.setText(fNumList[9]);
                        mCurValue.setText(fNumList[mOrgProgress]);
                        break;
                    default:
                        break;
                }
                Point srcPoint = RefocusUtils.yuvToSrcPoint(mYuvP, mSrcWidth, mSrcHeight, mRotate);
                mRefocusView.initCircle(srcPoint, RefocusImageView.CIRCLE_DEF);
                mRefocusView.setCanTouch(true);
                mUiHandler.sendEmptyMessageDelayed(MSG_HIDE_CIRCLE, DELAY_HIDE_TIME);
                break;
            case MSG_UPDATE_MENU:
                updateMenu();
                break;
            case MSG_HIDE_CIRCLE:
                mRefocusView.hideCircle();
            default:
                break;
        }
    }

    /**
     * ThreadHandler's handleMessage, excute in child thread
     *
     * @param msg
     * @return
     */
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_DECODE_SRC:
                deocdeBitmap();
                break;
            case MSG_INIT_REFOCUS:
                initRefocusData();
                switch (mType) {
                    case RefocusData.TYPE_BOKEH_ARC:
                    case RefocusData.TYPE_BOKEH_SPRD:
                    case RefocusData.TYPE_BOKEH_SBS:
                    case RefocusData.TYPE_BLUR_FACE:
                    case RefocusData.TYPE_BLUR_GAUSS:
                        mComRefocus.initLib();
                        break;
                    case RefocusData.TYPE_REFOCUS:
                    case RefocusData.TYPE_BLUR_TF:
                        // need calculate depth
                        mComRefocus.initLib();
                        mComRefocus.calDepth();
                        break;
                    default:
                        break;
                }
                if(DEBUG) {
                    mComRefocus.dumpData(mFilePath);
                }
                mUiHandler.sendEmptyMessage(MSG_INIT_UI);
                break;
            default:
                break;

        }
        return true;
    }

    private void deocdeBitmap() {
        try {
            Log.d(TAG, "deocdeBitmap start.");
            Options options = new Options();
            options.inSampleSize = getScaleRatio();
//        mSrcBitmap = RefocusUtils.loadBitmap(RefocusEditActivity.this, mUri, options);
            InputStream open = getResources().getAssets().open("refocus_image.jpg");
            mSrcBitmap = BitmapFactory.decodeStream(open,null,options);
//            Log.d(TAG, "deocdeBitmap end. scale bitmap w " + mSrcBitmap.getWidth() + " h " + mSrcBitmap.getHeight());
            mUiHandler.sendEmptyMessage(MSG_DISPLAY_SRC);
            open.close();
        } catch (IOException e) {

        }
    }

    private void initRefocusData() {
        Log.d(TAG, "initRefocusData start");
        InputStream inStream = null;
        try {
            inStream = getResources().getAssets().open("refocus_image.jpg");
            byte[] content = RefocusUtils.streamToByte(inStream);
            if (content == null) {
                Log.i(TAG, "read stream by uri fail!");
                return;
            }
            mComRefocus = CommonRefocus.getInstance(content);
            mType = mComRefocus.getType();
            RefocusData data = mComRefocus.getRefocusData();
            mEditYuv = data.getMainYuv();
            mRotate = data.getRotation();
            mYuvW = data.getYuvWidth();
            mYuvH = data.getYuvHeight();
            mYuvP = new Point(data.getSel_x(), data.getSel_y());
            mOrgProgress = mComRefocus.getProgress();
            mCurProgress = mOrgProgress;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                inStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "initRefocusData end");
    }

    private void initWidgets() {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_refocus_edit);
        mRefocusView = (RefocusImageView) findViewById(R.id.refocus_view);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mSeekBar = (SeekBar) findViewById(R.id.refocus_seekbar);
        mStartValue = (TextView) findViewById(R.id.start_value);
        mEndValue = (TextView) findViewById(R.id.end_value);
        mCurValue = (TextView) findViewById(R.id.current_value);
        mRefocusView.setCanTouch(false);
        mRefocusView.setRefocusViewCallback(this);
        mProgressBar.setVisibility(View.VISIBLE);
        mSeekBar.setOnSeekBarChangeListener(new ChangeListenerImp());
        mSeekBar.setEnabled(false);
    }

    private void processIntent() {
        Intent intent = getIntent();
        mFilePath = intent.getStringExtra(SRC_PATH);
        Log.d(TAG, "processIntent mFilePath = " + mFilePath);

        mSrcWidth = 1944;
        mSrcHeight = 2592;
        mRefocusView.setSrcRectF(mSrcWidth, mSrcHeight);
        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        int screenWidth = display.getWidth();
        int screenHeight = display.getHeight();
        Log.d(TAG, "Screen w: " + screenWidth + ", Screen h: " + screenHeight);

        float wScale = (float) mSrcWidth / (float) screenWidth;
        float hScale = (float) mSrcHeight / (float) screenHeight;
        float scale = Math.max(wScale, hScale) + 0.5f;
        mScale = (int) ((scale < 1.0f) ? 1 : scale);
        Log.d(TAG, "processIntent scale = " + scale + ", mScale = " + mScale);
        showSrcBitmap();
        initRefocus();
    }

    private void showSrcBitmap() {
        mThreadHandler.sendEmptyMessage(MSG_DECODE_SRC);
    }

    private void initRefocus() {
        mThreadHandler.sendEmptyMessage(MSG_INIT_REFOCUS);
    }

    private int getScaleRatio() {
        return mScale;
    }

    private void initActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.photo_toolbar_background));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_undo_redo_save, menu);
        mUndoItem = menu.findItem(R.id.refocus_edit_undo);
        mRedoItem = menu.findItem(R.id.refocus_edit_redo);
        mSaveItem = menu.findItem(R.id.refocus_edit_save);
        mRedoItem.setIcon(R.drawable.ic_refocus_redo);
        mUndoItem.setIcon(R.drawable.ic_refocus_undo_select);
        mSaveItem.setIcon(R.drawable.ic_refocus_storage);
        mUndoItem.setEnabled(false);
        mRedoItem.setEnabled(false);
        mSaveItem.setEnabled(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.refocus_edit_undo:
                doReset();
                break;
            case R.id.refocus_edit_redo:
                doRedo();
                break;
            case R.id.refocus_edit_save:
                doSave();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mSaveItem != null && mSaveItem.isEnabled()) {
            showSaveDialog();
        } else {
            super.onBackPressed();
        }
    }

    private void doSave() {
        SaveBitmapTask saveBitmapTask = new SaveBitmapTask();
        saveBitmapTask.execute(mEditYuv);
    }

    private void doReset() {
        mDoReset = true;
        mRefocusView.reset();
        mOldProgress = mCurProgress;
        mSeekBar.setProgress(mOrgProgress);
        updateMenu();
    }

    private void doRedo() {
        mDoReset = false;
        mRefocusView.redo();
        mSeekBar.setProgress(mOldProgress);
        updateMenu();
    }

    private void updateMenu() {
        if (mDoReset) {
            mUndoItem.setEnabled(false);
            mRedoItem.setEnabled(true);
            mSaveItem.setEnabled(false);
        } else {
            mUndoItem.setEnabled(true);
            mRedoItem.setEnabled(false);
            mSaveItem.setEnabled(true);
        }
    }

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.refocus_save_change);
        builder.setMessage(R.string.refocus_confirm_save);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setNeutralButton(R.string.refocus_quit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                doSave();
            }
        });
        builder.show();
    }

    File saveFile = null;

    private class SaveBitmapTask extends AsyncTask<byte[], Void, Boolean> {
        @Override
        protected void onPreExecute() {
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(byte[]... params) {
//            boolean saveResult = RefocusUtils.saveJpegByYuv(RefocusEditActivity.this,
//                    params[0], mYuvW, mYuvH, mRotate, mUri);
            File save = RefocusUtils.saveJpegByYuv(RefocusEditActivity.this,
                    params[0], mYuvW, mYuvH, mRotate);
            Log.d("xulei","   save  = "+save);
            if (save != null) {
                Log.d("xulei","   save  = "+save);
                saveFile = save;
                return true;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean save) {
            if (!save) {
                Toast.makeText(RefocusEditActivity.this, R.string.refocus_save_fail, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(RefocusEditActivity.this, R.string.refocus_save_success, Toast.LENGTH_SHORT).show();
                if (saveFile != null) {
                    setResult(RESULT_OK, new Intent().putExtra("path", saveFile.toString()));
                } else {
                    // do nothing.
                }
            }
            mProgressBar.setVisibility(View.GONE);
            finish();
        }
    }

    private class ChangeListenerImp implements SeekBar.OnSeekBarChangeListener {

        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mSeekBar.setProgress(progress);
            mCurProgress = progress;
            switch (mType) {
                case RefocusData.TYPE_BOKEH_ARC:
                case RefocusData.TYPE_BOKEH_SPRD:
                case RefocusData.TYPE_BOKEH_SBS:
                case RefocusData.TYPE_BLUR_TF:
                case RefocusData.TYPE_BLUR_FACE:
                case RefocusData.TYPE_BLUR_GAUSS:
                    // [F0.95 - F16], seekBar progress [0,9]
                    mCurValue.setText(fNumList[progress]);
                    break;
                case RefocusData.TYPE_REFOCUS:
                    // [F8 - F2], seekBar progress [0,255]
                    String curValue = RefocusUtils.getRefCurValue(progress);
                    mCurValue.setText(curValue);
                    break;
                default:
                    break;
            }
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
            //do nothing!
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
            Log.d(TAG, "SeekBar on Stop Touch ");
            doRefocus();
        }
    }

    @Override
    public void onUpdatePoint(Point srcPoint) {
        mSrcP = srcPoint;
        mYuvP = RefocusUtils.srcToYuvPoint(mSrcP, mSrcWidth, mSrcHeight, mRotate);
        Log.d(TAG, "onUpdatePoint mSrcP = " + mSrcP + ", mYuvP" + mYuvP);
    }

    @Override
    public void touchValid() {
        if (mUiHandler.hasMessages(MSG_HIDE_CIRCLE)) {
            mUiHandler.removeMessages(MSG_HIDE_CIRCLE);
        }
    }

    @Override
    public void doRefocus() {
        Log.d(TAG, "doRefocus.");
        if (RefocusEditActivity.this.isFinishing()) {
            return;
        }
        if (mRefocusTask != null && !mRefocusTask.isCancelled()
                && mRefocusTask.getStatus() == AsyncTask.Status.RUNNING) {
            Log.d(TAG, "AsyncTask running ,and cancel AsyncTask! ");
            mRefocusTask.cancel(true);
            mRefocusTask = null;
        }
        mRefocusTask = new RefocusTask();
        mRefocusTask.execute();
    }

    private class RefocusTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            int blurIntensity = mComRefocus.calBlurIntensity(mCurProgress);
            Log.d(TAG, "RefocusTask do refocus start. blurIntensity = " + blurIntensity + ", mYuvP" + mYuvP);
            mEditYuv = mComRefocus.doRefocus(mEditYuv, mYuvP, blurIntensity);
            Log.d(TAG, "RefocusTask do refocus end.");
            Options options = new Options();
            options.inSampleSize = getScaleRatio();
            Bitmap bitmap = RefocusUtils.getPicFromBytes(mEditYuv, mYuvW, mYuvH, options);
            Log.d(TAG, "RefocusTask yuv to bitmap finished.");
            mEditBitmap = RefocusUtils.rotateBitmap(bitmap, (float) mRotate);
            Log.d(TAG, "RefocusTask rotate bitmap finished, do refocus end.");
            return true;
        }

        @Override
        protected void onPostExecute(Boolean succeed) {
            if (!succeed) {
                Log.d(TAG, "do refocus fail.");
                return;
            }
            mRefocusView.setBitmap(mEditBitmap);
            mDoReset = false;
            updateMenu();
        }
    }

}


