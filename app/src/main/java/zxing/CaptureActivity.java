package zxing;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ResultParser;
import com.wtree.qrscandemo.R;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import listener.IZXingActivity;
import opencv.ImagePreProcess;
import view.CameraZoomTouchListener;
import zxing.camera.CameraManager;
import zxing.decode.CameraDecodeThread;
import zxing.decode.DecodeCore;
import zxing.decode.DecodeFormatManager;
import zxing.utils.BeepManager;
import zxing.utils.CaptureActivityHandler;
import zxing.utils.InactivityTimer;



/**
 * 扫码类
 */
public class CaptureActivity extends AppCompatActivity implements Callback, IZXingActivity {
    public final static int REQUEST_CODE_PERMISSION = 100;
    private static final String TAG = CaptureActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSION_CAMERA = 0x2;
    private static final int REQUEST_IMAGE = 1;

    private static final int REQUEST_CODE_SCAN_GALLERY = 100;

    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;

    private SurfaceView scanPreview = null;

    //    private ImageView flashBtnIv = null;//闪光灯
    private boolean isHasSurface = false;




    private int mType;


    public static final int TYPE_OTHER = 333;

    private String photo_path;
    private Bitmap scanBitmap;


    public static void launch(Context context) {

        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setClass(context, CaptureActivity.class);

        context.startActivity(intent);
    }

    public static void launch(Context context, int type) {
        Intent intent = new Intent();
        intent.putExtra("_type", type);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setClass(context, CaptureActivity.class);

        context.startActivity(intent);
    }


    String mPlatName;

    String mActionStr="";

    boolean fromClound=false;

    protected int getLayoutId() {
        return R.layout.zxing_main;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(getLayoutId());
        inactivityTimer = new InactivityTimer(this);
        beepManager = new BeepManager(this);
        initView();

    }

    protected void initView() {


        findViewById(R.id.whole_layout).setOnTouchListener(new CameraZoomTouchListener());
        scanPreview = (SurfaceView) findViewById(R.id.preview_view);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION:
                boolean isAllGranted = true;
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        isAllGranted = false;
                    }
                }
                if (isAllGranted) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        initCamera(scanPreview.getHolder());
                    }
                } else {
                    //进入应用必要权限的判断，现仅需外部存储
                    Toast.makeText(this,"没有权限",Toast.LENGTH_SHORT).show();;
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        setOnResume();
    }

    private void setOnResume() {
        // CameraManager must be initialized here, not in onCreate(). This is
        // necessary because we don't
        // want to open the camera driver and measure the screen size if we're
        // going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the
        // wrong size and partially
        // off screen.
        cameraManager = new CameraManager(getApplication());
        Log.e("camera", "instance~~~~~~~~");

        handler = null;

        if (isHasSurface) {
            // The activity was paused but not stopped, so the surface still
            // exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(scanPreview.getHolder());
            Log.e("camera", "2~~~~~~~~");
        } else {
            // Install the callback and wait for surfaceCreated() to init the
            // camera.
            scanPreview.getHolder().addCallback(this);
        }

        inactivityTimer.onResume();
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        beepManager.close();
        cameraManager.closeDriver();
        if (!isHasSurface) {
            scanPreview.getHolder().removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!isHasSurface) {
            isHasSurface = true;
            initCamera(holder);
            Log.e("camera", "3~~~~~~~~");
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isHasSurface = false;
    }

    @Override
    public void handleDecode(Result result) {
        inactivityTimer.onActivity();
        beepManager.playBeepSoundAndVibrate();

        formatResult(result);

//        if(!TextUtils.isEmpty(result.getText())){
//            ToastManager.showLongToast(result.getText());
//        }
    }

    @Override
    public CameraManager getCameraManager() {
        return CameraManager.get();
    }

    @Override
    public Rect getCropRect() {
        return null;
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public Handler getHandler() {

        return handler;
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION_CAMERA);
            return;
        }

        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a
            // RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, cameraManager, CameraDecodeThread.ALL_MODE);
            }
//            initCrop();
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }


    private void displayFrameworkBugMessageAndExit() {
        // camera error
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage("Camera error");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }

        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        builder.show();
    }



    // 对扫描的二维码图片进行解码
    public void formatResult(Result obj) {
        ParsedResult result = parseResult(obj);

        boolean isOk = false;
        String json = result.toString();



        //TODO 解析结果
        AlertDialog alertDialog=new AlertDialog.Builder(this)
                .setMessage(json).setPositiveButton("确定",null)
                .show();



    }


    public static ParsedResult parseResult(Result rawResult) {
        return ResultParser.parseResult(rawResult);
    }


    private static final long VIBRATE_DURATION = 200L;





    private byte[] bitmap2Bytes(Bitmap bitmap) {
        int picw = bitmap.getWidth(), pich = bitmap.getHeight();
        int[] pix = new int[picw * pich];
        bitmap.getPixels(pix, 0, picw, 0, 0, picw, pich);

        int tempH = pich - (pich % 6);
        int tempW = picw - (picw % 6);
        byte[] result = new byte[tempW * tempH * 4];

        for (int y = 0; y < tempH; y++) {
            for (int x = 0; x < tempW; x++) {
                int dstIndex = y * tempW + x;
                int srcIndex = y * picw + x;
                result[dstIndex * 4] = (byte) ((pix[srcIndex] >> 16) & 0xff);     //bitwise shifting
                result[dstIndex * 4 + 1] = (byte) ((pix[srcIndex] >> 8) & 0xff);
                result[dstIndex * 4 + 2] = (byte) (pix[srcIndex] & 0xff);
                result[dstIndex * 4 + 3] = (byte) 0xff;
            }
        }
        return result;
    }

}