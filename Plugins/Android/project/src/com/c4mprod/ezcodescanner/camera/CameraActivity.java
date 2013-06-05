package com.c4mprod.ezcodescanner.camera;

import java.lang.reflect.Method;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.c4mprod.ezcodescanner.RootActivity;
import com.c4mprod.ezcodescanner.views.CameraUI;
import com.unity3d.player.UnityPlayer;

@SuppressLint({ "NewApi", "NewApi" })
public class CameraActivity extends Activity {

    // Public
    public static final String  EVENT_OPENED      = "EVENT_OPENED";
    public static final String  EVENT_CLOSED      = "EVENT_CLOSED";

    // Camera
    private Camera              mCamera;
    private CameraPreview       mPreview;
    private Handler             mAutoFocusHandler;
    private String              mDataStr          = "";

    // Views
    private FrameLayout         mPreviewLayout;
    private TextView            mScanTextView;
    private ImageScanner        mImageScanner;

    // States
    private boolean             mIsBarcodeScanned = false;
    private boolean             mIsPreviewing     = true;

    // Config
    protected static final long TIME_BWIN_2_SCANS = 2000;
    private boolean             mShowUI;
    private String              mDefaultText;
    private int                 mSymbolMask;
    private boolean             mForceLandscape;
    private CameraUI            mCameraUI;
    private RelativeLayout      mMainLayout;
    private ImageView           mTestImage;
    private static int          mCamFacing;
    private static float        pixelDensity      = 1;

    private final Handler       mHandler          = new Handler();

    // Load Native Libary
    static {
        System.loadLibrary("iconv");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            mShowUI = extra.getBoolean(RootActivity.EXTRA_BOOLEAN_UI, true);
            mDefaultText = extra.getString(RootActivity.EXTRA_STRING_TXT);
            mSymbolMask = extra.getInt(RootActivity.EXTRA_INT_SYMBOLS, -1);
            mForceLandscape = extra.getBoolean(RootActivity.EXTRA_BOOLEAN_LANDSCAPE, false);
        }

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        pixelDensity = metrics.density;

        // --- Create the activity layout through the code since Unity3d does not support XML layout
        mMainLayout = new RelativeLayout(this);
        android.widget.RelativeLayout.LayoutParams params_main = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mMainLayout.setLayoutParams(params_main);

        // full screen, can be smaller
        mPreviewLayout = new FrameLayout(this);
        android.widget.RelativeLayout.LayoutParams params_cam = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        params_cam.addRule(RelativeLayout.CENTER_IN_PARENT, -1);
        mMainLayout.addView(mPreviewLayout, params_cam);

        mPreviewLayout.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (mCamera != null) {
                    Parameters params = mCamera.getParameters();
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        params.setFlashMode(Parameters.FLASH_MODE_TORCH);
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        params.setFlashMode(Parameters.FLASH_MODE_OFF);
                    }
                    try {
                        mCamera.setParameters(params);
                    } catch (Exception e) {
                        // In case not supported
                    }
                }
                return false;
            }
        });

        if (mShowUI) {
            mCameraUI = new CameraUI(this);
            android.widget.RelativeLayout.LayoutParams params_ui = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            mCameraUI.setLayoutParams(params_ui);
            mMainLayout.addView(mCameraUI, params_ui);
        } else {
            mCameraUI = null;
        }

        // Barcode string, optional
        if (mDefaultText != null) {
            mScanTextView = new TextView(this);
            android.widget.RelativeLayout.LayoutParams params_text = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            params_text.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, -1);
            mScanTextView.setText(mDefaultText);
            mScanTextView.setTextSize(14);
            mScanTextView.setTextColor(0xCCFFFFFF);
            mScanTextView.setGravity(Gravity.CENTER);
            mScanTextView.setMaxLines(2);
            params_text.setMargins(20, 20, 20, 20);
            mMainLayout.addView(mScanTextView, params_text);
        }

        if (!mShowUI || mForceLandscape) requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(mMainLayout);

        if (mForceLandscape)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mAutoFocusHandler = new Handler();
    }

    @Override
    protected void onStart() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    // because UnitySendMessage is static
                    Class<UnityPlayer> c = com.unity3d.player.UnityPlayer.class;
                    Method method = c.getMethod("UnitySendMessage", new Class[] { String.class, String.class, String.class });
                    method.invoke(null, "CodeScannerBridge", "onScannerEvent", EVENT_OPENED); // reveiver null
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        });

        super.onStart();
    }

    @Override
    protected void onResume() {

        super.onResume();
        try {
            // Camera Instance
            mCamera = getCameraInstance();
            mIsPreviewing = true;

            // Instance barcode scanner
            mImageScanner = new ImageScanner();
            if (mSymbolMask >= 0) {
                mImageScanner.setConfig(Symbol.NONE, Config.ENABLE, 0);
                mImageScanner.setConfig(mSymbolMask, Config.ENABLE, 1);
            }
            mImageScanner.setConfig(0, Config.X_DENSITY, 3);
            mImageScanner.setConfig(0, Config.Y_DENSITY, 3);

            // mCamera.getParameters().setPreviewFormat(ImageFormat.RGB_565);
            mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB, mForceLandscape);
            mPreviewLayout.addView(mPreview);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        try {
            if (mCamera != null) {
                mIsPreviewing = false;
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    // because UnitySendMessage is static
                    Class<UnityPlayer> c = com.unity3d.player.UnityPlayer.class;
                    Method method = c.getMethod("UnitySendMessage", new Class[] { String.class, String.class, String.class });
                    method.invoke(null, "CodeScannerBridge", "onScannerEvent", EVENT_CLOSED); // reveiver null
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        });
        super.onBackPressed();
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance() {
        Camera c = null;

        if (Build.VERSION.SDK_INT < 9) {
            try {
                c = Camera.open();
            } catch (Exception e) {
            }
        } else {
            int cameraCount = 0;
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            cameraCount = Camera.getNumberOfCameras();
            for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
                Camera.getCameraInfo(camIdx, cameraInfo);

                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    mCamFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
                    try {
                        c = Camera.open(camIdx);
                    } catch (RuntimeException e) {
                        Log.e("DEBUG", "Camera failed to open: " + e.getLocalizedMessage());
                    }
                    break;
                }

                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    mCamFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
                    try {
                        c = Camera.open(camIdx);
                    } catch (RuntimeException e) {
                        Log.e("DEBUG", "Camera failed to open: " + e.getLocalizedMessage());
                    }
                }
            }
        }
        return c;
    }

    public void initScanner() {

        if (mIsBarcodeScanned) {
            mIsBarcodeScanned = false;
            if (mScanTextView != null) mScanTextView.setText(mDefaultText);
            mCamera.setPreviewCallback(previewCb);
            mCamera.startPreview();
            mIsPreviewing = true;
            mCamera.autoFocus(autoFocusCB);
        }
    }

    public Rect getFramingRectInPreview(int previewWidth, int previewHeight, Rect cropFrame) {

        Rect rect = new Rect();

        Point cameraResolution = new Point(previewWidth, previewHeight);

        WindowManager manager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point screenResolution = new Point(display.getWidth(), display.getHeight());

        // Log.d("DEBUG", "cameraResolution.x=" + cameraResolution.x + " screenResolution.x=" + screenResolution.x);
        // Log.d("DEBUG", "cameraResolution.y=" + cameraResolution.y + " screenResolution.y=" + screenResolution.y);
        // Log.d("DEBUG", "cropFrame.left=" + cropFrame.left + " cropFrame.right=" + cropFrame.right + " cropFrame.top=" + cropFrame.top + " cropFrame.bottom="
        // + cropFrame.bottom);

        if (mForceLandscape) {
            rect.left = cropFrame.left * cameraResolution.x / screenResolution.x;
            rect.right = cropFrame.right * cameraResolution.x / screenResolution.x;
            rect.top = cropFrame.top * cameraResolution.y / screenResolution.y;
            rect.bottom = cropFrame.bottom * cameraResolution.y / screenResolution.y;
        } else {
            rect.left = cropFrame.top * cameraResolution.x / screenResolution.y;
            rect.right = cropFrame.bottom * cameraResolution.x / screenResolution.y;
            rect.top = (screenResolution.x - cropFrame.right) * cameraResolution.y / screenResolution.x;
            rect.bottom = (screenResolution.x - cropFrame.left) * cameraResolution.y / screenResolution.x;
        }
        return rect;
    }

    public int cropAndScanImage(byte[] data, int dataWidth, int dataHeight, Rect cropFrame, boolean reverseHorizontal) {

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        int marginLeft = 0;
        int marginTop = 0;
        float frameWidth = dataWidth;
        float frameHeight = dataHeight;

        // Log.d("DEBUG", "---");
        // Log.d("DEBUG", "cropFrame:" + cropFrame.toString());

        // adjust the frame according to the ratio between the real preview size and the screen size
        Rect adjustedframe = getFramingRectInPreview(dataWidth, dataHeight, cropFrame);

        // Log.d("DEBUG", "adjustedframe:" + adjustedframe.toString());

        frameWidth = adjustedframe.width(); // * pixelDensity;
        frameHeight = adjustedframe.height(); // * pixelDensity;

        marginLeft = (int) ((dataWidth - frameWidth) / 2);
        marginTop = (int) ((dataHeight - frameHeight) / 2);

        // Log.d("DEBUG", "data:" + data.length + " dataWidth:" + dataWidth + " dataHeight:" + dataHeight + " marginLeft=" + marginLeft + " marginTop=" +
        // marginTop);

        // --- METHOD 1 no filter (working) ---
        int width = (int) frameWidth;
        int height = (int) frameHeight;
        int area = width * height;
        byte[] matrix = new byte[area];
        int inputOffset = marginTop * dataWidth + marginLeft;
        if (width == dataWidth) {
            System.arraycopy(data, inputOffset, matrix, 0, area);
        } else {
            // Otherwise copy one cropped row at a time.
            byte[] yuv = data;
            for (int y = 0; y < height; y++) {
                int outputOffset = y * width;
                System.arraycopy(yuv, inputOffset, matrix, outputOffset, width);
                inputOffset += dataWidth;
            }
        }
        Image barcode = new Image(width, height, "Y800");
        barcode.setData(matrix);

        return mImageScanner.scanImage(barcode);

        // --- METHOD 2 Black & white filter (working) ---
        // int inputOffset = marginTop * dataWidth + marginLeft;
        // int[] pixels = new int[width * height];
        // byte[] yuv = data;
        // try {
        // for (int y = 0; y < height; y++) {
        // int outputOffset = y * width;
        // for (int x = 0; x < width; x++) {
        // int grey = yuv[inputOffset + x] & 0xff;
        // pixels[outputOffset + x] = 0xFF000000 | (grey * 0x00010101);
        // }
        // inputOffset += dataWidth;
        // }
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
        //
        // // Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        // // bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        // // mTestImage.setImageBitmap(bitmap);
        // Image barcode = new Image(width, height, "RGB4");
        // barcode.setData(pixels);
        // return mImageScanner.scanImage(barcode.convert("Y800"));

        // --- METHOD 3 (crash lib) ---
        // ByteArrayOutputStream out = new ByteArrayOutputStream();
        // YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, dataWidth, dataHeight, null);
        // Rect cropRect = new Rect(marginLeft, marginTop, marginLeft + width, marginTop + height);
        // yuvImage.compressToJpeg(cropRect, 50, out);
        // byte[] imageBytes = out.toByteArray();
        // // Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        // // mTestImage.setImageBitmap(image);
        // Image barcode = new Image(width, height, "JPEG");
        // barcode.setData(imageBytes);
        // return mImageScanner.scanImage(barcode.convert("Y800"));

    }

    private void reverseHorizontal(byte[] yuvData, int width, int height) {
        for (int y = 0, rowStart = 0; y < height; y++, rowStart += width) {
            int middle = rowStart + width / 2;
            for (int x1 = rowStart, x2 = rowStart + width - 1; x1 < middle; x1++, x2--) {
                byte temp = yuvData[x1];
                yuvData[x1] = yuvData[x2];
                yuvData[x2] = temp;
            }
        }
    }

    private final Runnable doAutoFocus     = new Runnable() {
                                               @Override
                                               public void run() {

                                                   // Log.d("DEBUG", "doAutoFocus mIsPreviewing=" + mIsPreviewing);
                                                   if (mIsPreviewing) mCamera.autoFocus(autoFocusCB);
                                               }
                                           };

    PreviewCallback        previewCb       = new PreviewCallback() {

                                               @Override
                                               public void onPreviewFrame(final byte[] data, Camera camera) {
                                                   Camera.Parameters parameters = camera.getParameters();
                                                   final Size size = parameters.getPreviewSize();

                                                   mPreviewHandler.post(new Runnable() {

                                                       @Override
                                                       public void run() {

                                                           int result = 0;
                                                           if (mCameraUI != null) {
                                                               Rect percentFramingRect = mCameraUI.getCaptureFrame();
                                                               result = cropAndScanImage(data, size.width, size.height, percentFramingRect,
                                                                                         (mCamFacing == Camera.CameraInfo.CAMERA_FACING_FRONT));
                                                           } else {
                                                               Image barcode = new Image(size.width, size.height, "Y800");
                                                               barcode.setData(data);

                                                               result = mImageScanner.scanImage(barcode);
                                                           }

                                                           Message msg = new Message();
                                                           msg.arg1 = result;
                                                           mPreviewHandler.sendMessage(msg);
                                                       }
                                                   });
                                               }
                                           };

    // Mimic continuous auto-focusing (autofocus available since API lvl 9)
    AutoFocusCallback      autoFocusCB     = new AutoFocusCallback() {
                                               @Override
                                               public void onAutoFocus(boolean success, Camera camera) {

                                                   // if (Build.VERSION.SDK_INT <= 9) {
                                                   mAutoFocusHandler.postDelayed(doAutoFocus, TIME_BWIN_2_SCANS);
                                                   // }
                                               }
                                           };

    private final Handler  mUnityHandler   = new Handler() {
                                               @Override
                                               public void handleMessage(Message msg) {
                                                   try {
                                                       Class<UnityPlayer> c = com.unity3d.player.UnityPlayer.class;
                                                       Method method = c.getMethod("UnitySendMessage", new Class[] { String.class, String.class, String.class });
                                                       method.invoke(null, "CodeScannerBridge", "onScannerMessage", mDataStr); // reveiver null
                                                       // because
                                                       // UnitySendMessage
                                                       // is
                                                       // static
                                                   } catch (NoSuchMethodException e) {
                                                       e.printStackTrace();
                                                       return;
                                                   } catch (Exception e) {
                                                       e.printStackTrace();
                                                       return;
                                                   }

                                                   finish();
                                               }
                                           };

    private final Handler  mPreviewHandler = new Handler() {
                                               @Override
                                               public void handleMessage(Message msg) {

                                                   int result = msg.arg1;

                                                   if (result != 0 && mIsBarcodeScanned == false) {
                                                       mIsBarcodeScanned = true;
                                                       mIsPreviewing = false;
                                                       mCamera.setPreviewCallback(null);
                                                       mCamera.stopPreview();

                                                       SymbolSet syms = mImageScanner.getResults();
                                                       for (Symbol sym : syms) {
                                                           if (mScanTextView != null) mScanTextView.setText(sym.getData());

                                                           mDataStr = sym.getData();
                                                       }

                                                       if (syms.size() > 0) {

                                                           Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                                           v.vibrate(300);

                                                           final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
                                                           tg.startTone(ToneGenerator.TONE_PROP_BEEP);

                                                           mUnityHandler.sendEmptyMessageDelayed(0, TIME_BWIN_2_SCANS / 2);
                                                       }
                                                   }

                                               }
                                           };
}
