package com.c4mprod.ezcodescanner.camera;

import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private final SurfaceHolder     mHolder;
    private final Camera            mCamera;
    private final PreviewCallback   previewCallback;
    private final AutoFocusCallback autoFocusCallback;
    private final boolean           isLandscape;

    public CameraPreview(Context context, Camera camera, PreviewCallback previewCb, AutoFocusCallback autoFocusCb, boolean land) {
        super(context);
        mCamera = camera;
        previewCallback = previewCb;
        autoFocusCallback = autoFocusCb;
        isLandscape = land;

        /*
         * Set camera to continuous focus if supported, otherwise use
         * software auto-focus. Only works for API level >=9.
         */

        // Camera.Parameters parameters = camera.getParameters();
        // for (String f : parameters.getSupportedFocusModes()) {
        // if (f == Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) {
        // mCamera.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        // autoFocusCallback = null;
        // break;
        // }
        // }

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);

        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (Exception e) {
            Log.d("DBG", "Error setting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Camera preview released in activity
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        /*
         * If your preview can change or rotate, take care of those events here.
         * Make sure to stop the preview before resizing or reformatting it.
         */
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        try {
            // Hard code camera surface rotation 90 degs to match Activity view in portrait
            if (!isLandscape) {
                mCamera.setDisplayOrientation(90);
            }
            mCamera.setPreviewDisplay(mHolder);
            mCamera.setPreviewCallback(previewCallback);

            if (isLandscape) {
                Size size = getLargestSupportedPreviewSize(mCamera);
                if (size != null) {
                    Camera.Parameters parameters = mCamera.getParameters();
                    parameters.setPreviewSize(size.width, size.height);
                    mCamera.setParameters(parameters);
                }
            }
            mCamera.startPreview();
            mCamera.autoFocus(autoFocusCallback);
        } catch (Exception e) {
            Log.d("DBG", "Error starting camera preview: " + e.getMessage());
        }
    }

    public Size getLargestSupportedPreviewSize(Camera camera) {
        Camera.Parameters parameters = mCamera.getParameters();
        List<Size> sizes = parameters.getSupportedPreviewSizes();
        int maxvalue = 0;
        Size size = null;
        for (Size s : sizes) {
            // Log.d("DEBUG", "available size=" + s.width + "x" + s.height);
            if (s.width > maxvalue) {
                maxvalue = s.width;
                size = s;
            }
        }
        // Log.d("DEBUG", "=>Choosen size=" + size.width + "x" + size.height);
        return size;
    }
}
