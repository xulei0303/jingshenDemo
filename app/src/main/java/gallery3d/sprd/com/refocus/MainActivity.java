package gallery3d.sprd.com.refocus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Choreographer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.sprd.gallery3d.refocusimage.RefocusEditActivity;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    // Used to load the 'native-lib' library on application startup.
//    static {
//        System.loadLibrary("native-lib");
//    }
    ImageView tv;
    Button button;
    TextView textview;
    static {
        System.loadLibrary("jni_arcsoft_real_bokeh");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("xulei","MainActivity onCreate  ");
        setContentView(R.layout.activity_main);
        tv = (ImageView) findViewById(R.id.sample_text);
        try {
//            tv.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("cz.jpg")));
            tv.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("refocus_image.jpg")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        button  = (Button) findViewById(R.id.button);
        textview  = (TextView) findViewById(R.id.path);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                startImageBlendingActivity();
                startRefocusActivity();
            }
        });
        if (PackageManager.PERMISSION_DENIED == checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
            button.setEnabled(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("cz", "onResume: "  + getSupportActionBar());
        Log.d("cz", "onResume: " + getActionBar());
        Uri external = MediaStore.Files.getContentUri("external");
//        String[] select= {
//            MediaStore.Images.Media.BUCKET_ID,
//            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
//            MediaStore.Files.FileColumns.MEDIA_TYPE,
//            MediaStore.Files.FileColumns.DATA
//        };
//        Cursor query = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, select, "1) GROUP BY 1,(2", null, "MAX(datetaken) DESC");
//        while (query.moveToNext()){
//            int anInt = query.getInt(query.getColumnIndex(MediaStore.Images.Media.BUCKET_ID));
//            String string = query.getString(query.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME));
//            String string1 = query.getString(query.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE));
//            String string2 = query.getString(query.getColumnIndex(MediaStore.Files.FileColumns.DATA));
//            Log.d("cz", "onResume: " + anInt + "----"+string+"----" + string1 + "----" + string2);
//        }
//        query.close();
        Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long l) {
                Log.d(TAG, "doFrame: " + l);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            button.setEnabled(true);
        }else {
            finish();
            Toast.makeText(MainActivity.this, "请同意权限申请,以继续操作", Toast.LENGTH_SHORT).show();
        }
    }

//    public void startImageBlendingActivity() {
//        Intent intent = new Intent(this, ReplaceActivity.class);
//        startActivityForResult(intent, 1);
////        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this, tv, "cz").toBundle());
//    }

    public void startRefocusActivity() {
        Intent intent = new Intent(this, RefocusEditActivity.class);
//        startActivity(intent);
        startActivityForResult(intent, 1);
//        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this, tv, "cz").toBundle());
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode ==1 && data != null && data.getStringExtra("path")!= null){
//            final String paths= data.getStringExtra("path");
//            new Thread(){
//                @Override
//                public void run() {
//                    super.run();
//                    final Bitmap bitmap = BitmapFactory.decodeFile(paths);
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            tv.setImageBitmap(bitmap);
//                            textview.setText(paths);
//                        }
//                    });
//                }
//            }.start();
//        }
//    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        Log.d("xulei", "requestCode = " + requestCode + ", resultCode = " + resultCode + ", data = " + data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            new Thread() {
                @Override
                public void run() {
                    final String path = data.getExtras().getString("path");
                    final Bitmap bitmap = BitmapFactory.decodeFile(path);
                    if (bitmap != null) runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv.setImageBitmap(bitmap);
                            textview.setText(path);
                        }
                    });

                }
            }.start();


        }

    }
}