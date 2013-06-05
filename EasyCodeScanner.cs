using UnityEngine;
using System.Collections;
using System.Runtime.InteropServices;
using System;

public class EasyCodeScanner : MonoBehaviour{
	
	//Action
	public static event Action<string> OnScannerMessage;
	public static event Action<string> OnScannerEvent;
	public static event Action<string> OnDecoderMessage;
	
	#if UNITY_IPHONE
		public struct ConfigStruct
		{
			public bool showUI;
	    	public string defaultText;
			public int symbols;
	    	public bool forceLandscape;
		}
		[DllImport ("__Internal")] private static extern void launchScannerImpl(ref ConfigStruct conf);
		[DllImport ("__Internal")] private static extern bool getScannedImageImpl(ref IntPtr pixelBytes, ref int imageDataLength);
		[DllImport ("__Internal")] private static extern void decodeImageImpl(int symbols, byte[] pixelBytes, long length);
	#endif
	
	static EasyCodeScanner instance;
	public static void Initialize()
	{
		if (instance == null)
		{
			GameObject newGameObject = new GameObject("CodeScannerBridge");
			newGameObject.AddComponent<EasyCodeScanner>();
			instance = newGameObject.GetComponent<EasyCodeScanner>();
		}
	}
	
	void Start()
	{
		
	}
	
	//Callback from Java or Objective-C which returns the data (barcode, url, ...)
	void onScannerMessage(string data){
		//Debug.Log("EasyCodeScanner - onScannerMessage data=:"+data);
		if (OnScannerMessage != null)
        {
            OnScannerMessage(data);
        }
	}
	
	//Callback from Java or Objective-C which notifies an event
	//param : "EVENT_OPENED", "EVENT_CLOSED"
	void onScannerEvent(string eventStr){
		//Debug.Log("EasyCodeScanner - onScannerEvent eventStr=:"+eventStr);
		if (OnScannerEvent != null)
        {
            OnScannerEvent(eventStr);
        }
	}
	
	void onDecoderMessage(string code){
		//Debug.Log("EasyCodeScanner - onDecoderMessage code=:"+code);
		if (OnDecoderMessage != null)
        {
            OnDecoderMessage(code);
        }
	}
	
	public static void launchScanner(bool showUI, string defaultTxt, int symbol, bool forceLandscape) {
		
		if (instance==null) {
			Debug.LogError("EasyCodeScanner - launchScanner error : scanner must be initialized before.");
			return;
		}
		#if UNITY_IPHONE	
			//IPHONE - Display the UIViewController
			ConfigStruct conf = new ConfigStruct();
			conf.showUI = showUI;
			conf.defaultText = defaultTxt;
			conf.symbols = symbol;
			conf.forceLandscape = forceLandscape;
			launchScannerImpl(ref conf);
		#endif
		
		#if UNITY_ANDROID
			//ANDROID - Launch the Activity with an Intent
			AndroidJavaClass ajc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
			AndroidJavaObject ajo = ajc.GetStatic<AndroidJavaObject>("currentActivity");
		
			//In case RootActivity is the MAIN in the manifest
			//ajo.Call("launchScannerImpl", showUI, defaultTxt, symbol, forceLandscape);
		
			//In case RootActivity is not the MAIN activity in the manifest (multi-plugin)
			var jc = new AndroidJavaClass("com.c4mprod.ezcodescanner.RootActivity");
    		jc.CallStatic("launchScannerImpl", ajo, showUI, defaultTxt, symbol, forceLandscape);
		#endif
	
	}
	
	public static byte[] getScannerImage() {
		
		if (instance==null) {
			Debug.LogError("EasyCodeScanner - launchScanner error : scanner must be initialized before.");
			return null;
		}
		#if UNITY_IPHONE
			//Get the image from the scanner
			byte[] pixelBytes = null;
			IntPtr ptrImageData = IntPtr.Zero;
	        int imageDataLength = 0;
	        bool success = getScannedImageImpl(ref ptrImageData, ref imageDataLength);
	        if (success)
	        	{
	            	// Load the results into a managed array.
	            	pixelBytes = new byte[imageDataLength];
	            	Marshal.Copy(ptrImageData
	                	, pixelBytes
	                	, 0
	                	, imageDataLength);
	        	}
			return pixelBytes;
		#elif UNITY_ANDROID
			//Not yet implemented
			return null;
		#else
			return null;
		#endif
	}
	
	public static Texture2D getScannerImage(int texWidth, int texHeigh) {
		
		if (instance==null) {
			Debug.LogError("EasyCodeScanner - launchScanner error : scanner must be initialized before.");
			return null;
		}
		#if UNITY_IPHONE
			byte[] pixelBytes = getScannerImage();
			//Convert the image to a texture
			Texture2D tex = new Texture2D(texWidth, texHeigh);
        	tex.LoadImage(pixelBytes);
			return tex;
		#elif UNITY_ANDROID
			//Not yet implemented
			return null;
		#else
			return null;
		#endif
	}
	
	//Calls OnDecoderMessage in return
	public static void decodeImage(int symbols, byte[] image) {
		
		if (instance==null) {
			Debug.LogError("EasyCodeScanner - launchScanner error : scanner must be initialized before.");
			return;
		}
		#if UNITY_IPHONE
			decodeImageImpl(symbols, image, image.Length);
		#elif UNITY_ANDROID
			//Not yet implemented
			return;
		#else
			return;
		#endif
	}
	
	//Calls OnDecoderMessage in return
	public static void decodeImage(int symbols, Texture2D texture) {
		
		if (instance==null) {
			Debug.LogError("EasyCodeScanner - launchScanner error : scanner must be initialized before.");
			return;
		}
		#if UNITY_IPHONE
			byte[] pixelBytes = texture.EncodeToPNG();
			decodeImageImpl(symbols, pixelBytes, pixelBytes.Length);
		#elif UNITY_ANDROID
			//Not yet implemented
			return;
		#else
			return;
		#endif
	}
	
}