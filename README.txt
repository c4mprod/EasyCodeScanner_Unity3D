# Easy Code Scanner Unity 3D plugin for Android and iOS
Ready to use plugin to capture barcode and associated data in an Unity3D project.

# Usage

## Installation

* Import the module in Unity or copy the 'Assets' folder content to your Unity project (Asset > Import Package > Custom Package...).
* Select your platform in Unity3d (File > Build Setting) and switch to the desired platform (android or iOS)
* On Android only : If you import the plugin in an existing project, put the 2 plugin activities (RootActivity & CameraActivity) in your own Manifest.xml file, otherwise make sure the bundle identifier in the player settings is the one declared in Assets/Plugins/Android/AndroidManifest.xml in the player settings (com.c4mprod.ezcodescanner).
  In your Manifest, RootActivity does not need to be MAIN if an other android plugin is already MAIN.
* Build and Run

## Compilation

The plugins are already compiled and ready to be used but you may want to modify it to change the behavior of the plugin.
Here are the instructions:

### Android

* First import the /Assets/Plugins/Android/project folder in Eclipse (right-click and Import)
* Make your modifications if needed.
* Export as a JAR file to Assets/Plugins/Android/EZCodeScanner.jar (Right-click > Export, then select JAR and ONLY export 'src' and 'gen').
* Save and replace
* Build and Run on the Unity3D side.

### iOS

* Build the plugin in Unity3D (switch to iOS and 'Build')
* In the generated XCode project in 'Build Settings'
    - set 'Enable C++ Exception' to YES
    - set Architecture to 'Standard armv7'
* Build and Run with XCode 

### Modify the auto-generated GameObject name

if you need to change the name of the object generated then you have to change the following lines and recompile the plugin:
* line 254 in CamaraActivity.java for Android
* line 56 in EZCodeScannerViewController.mm for iPhone
* line 31 in EasyCodeScanner.cs
