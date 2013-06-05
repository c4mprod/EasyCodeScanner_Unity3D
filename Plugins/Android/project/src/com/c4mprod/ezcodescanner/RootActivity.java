package com.c4mprod.ezcodescanner;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.c4mprod.ezcodescanner.camera.CameraActivity;
import com.unity3d.player.UnityPlayerActivity;

public class RootActivity extends UnityPlayerActivity {

    public static final String EXTRA_BOOLEAN_UI        = "show_ui";
    public static final String EXTRA_STRING_TXT        = "default_text";
    public static final String EXTRA_INT_SYMBOLS       = "symbols_mask";
    public static final String EXTRA_BOOLEAN_LANDSCAPE = "force_landscape";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Launch the scanner view with specific config if root activity is the MAIN activity (no other plugins)
     * 
     * @param showUI
     *            display the ui overlay
     * @param defaultText
     *            The default text to show while scanning
     * @param symbols
     *            Mask with values in Symbol class, for instance Symbol.QRCODE or (Symbol.QRCODE|Symbol.EAN13). -1 for all symbols.
     */
    public void launchScannerImpl(boolean showUI, String defaultText, int symbols, boolean forceLandscape) {

        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra(EXTRA_BOOLEAN_UI, showUI);
        intent.putExtra(EXTRA_STRING_TXT, defaultText);
        intent.putExtra(EXTRA_INT_SYMBOLS, symbols);
        intent.putExtra(EXTRA_BOOLEAN_LANDSCAPE, forceLandscape);
        startActivity(intent);
    }

    /**
     * Launch the scanner statically from an existing activity view with specific config (Other activity than RootActivity is MAIN - other plugin).
     * 
     * @param showUI
     *            display the ui overlay
     * @param defaultText
     *            The default text to show while scanning
     * @param symbols
     *            Mask with values in Symbol class, for instance Symbol.QRCODE or (Symbol.QRCODE|Symbol.EAN13). -1 for all symbols.
     */
    public static void launchScannerImpl(Activity root, boolean showUI, String defaultText, int symbols, boolean forceLandscape) {

        Intent intent = new Intent(root, CameraActivity.class);
        intent.putExtra(RootActivity.EXTRA_BOOLEAN_UI, showUI);
        intent.putExtra(RootActivity.EXTRA_STRING_TXT, defaultText);
        intent.putExtra(RootActivity.EXTRA_INT_SYMBOLS, symbols);
        intent.putExtra(RootActivity.EXTRA_BOOLEAN_LANDSCAPE, forceLandscape);
        root.startActivity(intent);
    }

}