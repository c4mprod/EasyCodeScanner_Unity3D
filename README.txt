# Easy Code Scanner Unity 3D plugin for Android and iOS
Ready to use plugin to capture barcode and associated data in an Unity3D project.

# Usage

## Installation


### In a new Test Project

* File > New Project > select a folder
* Assets > import package > Custom Package 
* Select the EasyCodeScanner_Unity3D_android_ios_v1.x.unitypackage file then import
* re-open Unity3D with the EasyCodeScanner.unity file located in the folder you created
* File > Build settings
* Select iOS or Android

On iOS
* Build the plugin in Unity3D (switch to iOS and 'Build')
* In the generated XCode project in 'Build Settings'
    - set 'Enable C++ Exception' to YES
    - set Architecture to 'Standard armv7'
* Build and Run with XCode 

On Android
* Build and Run and select a destination APK file name.


### In an existing Project

* Import the package in Unity or copy the content of the package in the 'Assets' folder of your Unity project (Asset > Import Package > Custom Package...).
* Select your platform in Unity3d (File > Build Setting) and switch to the desired platform (android or iOS)

On iOS
* Build the plugin in Unity3D (switch to iOS and 'Build')
* In the generated XCode project in 'Build Settings'
    - set 'Enable C++ Exception' to YES
    - set Architecture to 'Standard armv7'
* Build and Run with XCode 

On Android
* Put the 2 plugin activities (RootActivity & CameraActivity) in your own Manifest.xml file, otherwise make sure the bundle identifier in the player settings is the one declared in Assets/Plugins/Android/AndroidManifest.xml in the player settings (com.c4mprod.ezcodescanner).
  In your Manifest, RootActivity does not need to be MAIN if an other android plugin is already MAIN.
* Build and Run and select a destination APK file name.




## Compilation (Optional)

The plugins are already compiled and ready to be used but you may want to modify it to change the behavior of the plugin.
Here are the instructions:

### Android

* First import the /Assets/Plugins/Android/project folder in Eclipse (right-click and Import)
* Make your modifications if needed.
* Export as a JAR file to Assets/Plugins/Android/EZCodeScanner.jar (Right-click > Export, then select JAR and ONLY export 'src' and 'gen').
* Save and replace
* Build and Run on the Unity3D side.

### iOS
* You can modify the EZCodeScannerViewController.h and EZCodeScannerViewController.mm file in Assets/Plugins/iOS


### Modify the auto-generated GameObject name

if you need to change the name of the object generated then you have to change the following lines and recompile the plugin:
* line 254 in CamaraActivity.java for Android
* line 56 in EZCodeScannerViewController.mm for iPhone
* line 31 in EasyCodeScanner.cs



## Q&A

Q : I got the following error, how to solve it?
Undefined symbols for architecture armv7:
"_launchScannerImpl", referenced from:
RegisterMonoModules() in RegisterMonoModules.o
ld: symbol(s) not found for architecture armv7
clang: error: linker command failed with exit code 1 (use -v to see invocation)

A : Check the following points :
1.The package file are most certainly not at the right location, make sur the "Plugin" folder is right under the "Asset" folder:
+Assets
 -CHANGES.txt
 -EasyCodeScanner.cs
 -EasyCodeScanner.unity
 -EasyCodeScannerExample.cs
 +Plugins
  +Android
  +iOS
 -README.txt
2. In the generated iPhone project you have the EZCodeScannerViewController.mm file under the libraries folder
3. The EZCodeScannerViewController.mm file has the Unity-iPhone Target Membership checked (on the right panel) - very important !


Q : On Android it crashes just after pushing the scan button, what is the problem?
A : Make sure you have declared the 2 activities of the plugin in your Manifest and also the set the permissions (CAMERA & VIBRATE).


Q : Do I Need to manually copy files in the Xcode project?
A : No everything should be automatically copied to the Xcode project by unity3D if the plugin files are at the right location.


Q: Can I decode a provided image with the plugin?
A: Yes on iOS devices only, you can provide a Texture2D or a byte array and a call back will give you back the decoded message.

Q: How can I change the Scan UI?
A: You can change the scan message in the launchScanner method. For graphical modification you need to use 2 different tricks
   - convert your images in int arrays and load them statically in your classes
   - Use Resource.Load() and then access them natively via the external data folder (http://stackoverflow.com/questions/9129086/load-local-asset-bundle-through-www)

Q: As I need a bar code reader, and not a QR Code is there anything that can be done ? 
A: As for Barcode, EasyCodeScanner can read a wide variety of codes. You just have to specify in the type of code 

ZBAR_NONE = 0, < no symbol decoded 
ZBAR_PARTIAL = 1, < intermediate status 
ZBAR_EAN8 = 8, < EAN-8 /
ZBAR_UPCE = 9, < UPC-E 
ZBAR_ISBN10 = 10, < ISBN-10 (from EAN-13). @since 0.4 
ZBAR_UPCA = 12, < UPC-A 
ZBAR_EAN13 = 13, < EAN-13 
ZBAR_ISBN13 = 14, < ISBN-13 (from EAN-13). @since 0.4 
ZBAR_I25 = 25, < Interleaved 2 of 5. @since 0.4 
ZBAR_CODE39 = 39, < Code 39. @since 0.4 
ZBAR_PDF417 = 57, < PDF417. @since 0.6 
ZBAR_QRCODE = 64, < QR Code. @since 0.10 
ZBAR_CODE128 = 128, < Code 128 
ZBAR_SYMBOL = 0x00ff, < mask for base symbol type 
ZBAR_ADDON2 = 0x0200, < 2-digit add-on flag 
ZBAR_ADDON5 = 0x0500, < 5-digit add-on flag 
ZBAR_ADDON = 0x0700, < add-on flag mask 
} zbar_symbol_type_t;

for instance, only Barcode
launchScanner( true, "Scanning...", 13, true);