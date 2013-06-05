#import <UIKit/UIKit.h>
#import "ZBarSDK.h"

@interface EZCodeScannerViewController : UIViewController <ZBarReaderViewDelegate>
{
    UINavigationController *navController;
    ZBarReaderView * mReader;
    ZBarImageScanner * mScanner;
    UILabel* mLabel;
    
    bool mCodeFound;
    bool mShowUI;
    char* mDefaultText;
    int mSymbols;
    bool mForceLandscape;
    unsigned char* mPixelData;
    int mPixelSize;
    float screen_width;
    float screen_height;
}

#ifdef __cplusplus
extern "C" {
#endif
	
    void launchScannerImpl(struct ConfigStruct *confStruct);
    bool getScannedImageImpl(unsigned char** imageData, int* imageDataLength);
    void decodeImageImpl(int symbols, const char* pixelBytes, int64_t length);
    
#ifdef __cplusplus
}
#endif


@end
