package com.c4mprod.ezcodescanner.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

public final class CameraUI extends View {

    private final Paint        paint;
    private static final int[] SCANNER_ALPHA  = { 0, 64, 128, 192, 255, 192, 128, 64 };
    private final int          laserColor;
    private int                scannerAlpha;
    private final int          frameColor;
    private final int          maskColor;
    private Rect               mFrame;
    int                        mWidth;
    int                        mHeight;

    private static final int   PERCENT_LEFT   = 20;
    private static final int   PERCENT_TOP    = 30;
    private static final int   PERCENT_RIGHT  = 20;
    private static final int   PERCENT_BOTTOM = 30;

    public CameraUI(Context context) {
        super(context);

        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint();
        laserColor = 0xFFFF0000;
        frameColor = 0xCC000000;
        maskColor = 0x90000000;

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

        if (changed) {
            mWidth = right - left;
            mHeight = bottom - top;

            if (mWidth < mHeight) {
                // portrait
                mFrame = new Rect((mWidth * PERCENT_LEFT) / 100, (mHeight * PERCENT_TOP) / 100, mWidth - (mWidth * PERCENT_RIGHT) / 100,
                                  mHeight - (mHeight * PERCENT_BOTTOM) / 100);
            } else {
                // landscape
                mFrame = new Rect((mWidth * PERCENT_TOP) / 100, (mHeight * PERCENT_RIGHT) / 100, mWidth - (mWidth * PERCENT_BOTTOM) / 100,
                                  mHeight - (mHeight * PERCENT_LEFT) / 100);
            }
        }

        super.onLayout(changed, left, top, right, bottom);
    }

    public Rect getCaptureFrame() {
        return mFrame;
    }

    @Override
    public void onDraw(Canvas canvas) {

        if (mFrame != null) {
            int width = canvas.getWidth();
            int height = canvas.getHeight();

            // Draw the exterior (i.e. outside the framing rect) darkened
            paint.setColor(maskColor);
            canvas.drawRect(0, 0, width, mFrame.top, paint);
            canvas.drawRect(0, mFrame.top, mFrame.left, mFrame.bottom + 1, paint);
            canvas.drawRect(mFrame.right + 1, mFrame.top, width, mFrame.bottom + 1, paint);
            canvas.drawRect(0, mFrame.bottom + 1, width, height, paint);

            // Draw a two pixel solid black border inside the framing rect
            paint.setColor(frameColor);
            canvas.drawRect(mFrame.left, mFrame.top, mFrame.right + 1, mFrame.top + 2, paint);
            canvas.drawRect(mFrame.left, mFrame.top + 2, mFrame.left + 2, mFrame.bottom - 1, paint);
            canvas.drawRect(mFrame.right - 1, mFrame.top, mFrame.right + 1, mFrame.bottom - 1, paint);
            canvas.drawRect(mFrame.left, mFrame.bottom - 1, mFrame.right + 1, mFrame.bottom + 1, paint);

            // Draw a red "laser scanner" line through the middle to show decoding
            // is active
            paint.setColor(laserColor);
            paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
            scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
            int middle = mFrame.height() / 2 + mFrame.top;
            canvas.drawRect(mFrame.left + 2, middle - 1, mFrame.right - 1, middle + 2, paint);

            postInvalidate();
        }
    }

}
