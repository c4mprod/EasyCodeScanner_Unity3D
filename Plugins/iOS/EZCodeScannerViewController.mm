#import "EZCodeScannerViewController.h"
#import "ZBarImageScanner.h"
#import <QuartzCore/QuartzCore.h>
#import <AudioToolbox/AudioServices.h>
//#import "UINavigationController-Oriented.h"

@implementation EZCodeScannerViewController

#define SYSTEM_VERSION_GREATER_THAN(v)              ([[[UIDevice currentDevice] systemVersion] compare:v options:NSNumericSearch] == NSOrderedDescending)
#define SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(v)  ([[[UIDevice currentDevice] systemVersion] compare:v options:NSNumericSearch] != NSOrderedAscending)

//Grab the Unity3D ViewController (UnityGetGLViewController())
#ifdef UNITY_4_0
	//Unity4
	#import "iPhone_View.h"
#else
	//Unity3.5
	extern UIViewController* UnityGetGLViewController();
#endif


- (id) initWithUI:(BOOL)_showUI withText:(char*)_text withSymbol:(int)_symbols withLandscape:(BOOL)isLandscape
{
	self = [super init];
	mCodeFound = NO;
    mShowUI = _showUI;
    mDefaultText = _text;
    mSymbols = _symbols;
    mForceLandscape = isLandscape;
    
    //always portait whenever device orientation
    float h = self.view.bounds.size.height;
    float w = self.view.bounds.size.width;
    
    //init camera
    UIInterfaceOrientation interfaceOrientation = [[UIApplication sharedApplication] statusBarOrientation];
    //NSLog(@"initWithUI interfaceOrientation=%d", interfaceOrientation);
    [self init_camera];
    
    //init screen
    if (UIInterfaceOrientationIsLandscape(interfaceOrientation)) {
    	[mReader willRotateToInterfaceOrientation:interfaceOrientation duration:0.1];
    	screen_height = w;
    	screen_width = h;
    } else {
    	screen_height = h;
   	 	screen_width = w;
    }
    //NSLog(@"initWithUI w=%f h=%f ", screen_width, screen_height);
    [self init_screen];
        
    navController = [[UINavigationController alloc] initWithRootViewController:self];
    //[navController setForceLandscape:mForceLandscape];
    navController.navigationBar.barStyle=UIBarStyleBlackTranslucent;
    UIBarButtonItem *cancelButton = [[UIBarButtonItem alloc] initWithTitle:@"Cancel" style:UIBarButtonItemStylePlain target:self action:@selector(onBackClick)];
    self.navigationItem.leftBarButtonItem = cancelButton;
    [cancelButton release];
    
    [UnityGetGLViewController() presentModalViewController:navController animated:NO];
    UnitySendMessage("CodeScannerBridge", "onScannerEvent", "EVENT_OPENED");
    return self;
}

- (void) onBackClick {

	UnitySendMessage("CodeScannerBridge", "onScannerEvent", "EVENT_CLOSED");
	[self quit];
}

- (void) quit
{
    if (mReader != nil)
	{
		[mReader stop];
    	[mReader release];
    	mReader = nil;
	}
	if (mScanner != nil)
	{
    	[mScanner release];
    	mScanner = nil;
	}
	
	[UnityGetGLViewController()  dismissModalViewControllerAnimated:NO];
}

- (void) readerView:(ZBarReaderView *)readerView didReadSymbols: (ZBarSymbolSet *)symbols fromImage:(UIImage *)image
{
	if (mCodeFound) return;

    AudioServicesPlayAlertSound(kSystemSoundID_Vibrate);
    
    ZBarSymbol * s = nil;
    NSData *pixelData = [[NSData alloc] initWithData:UIImagePNGRepresentation(image)];
    //NSData* pixelData = (NSData*) CGDataProviderCopyData(CGImageGetDataProvider(image.CGImage));
    
    instance->mPixelData = (unsigned char *)[pixelData bytes];
    instance->mPixelSize = [pixelData length];
    //NSLog(@"pixelData length = %d", [pixelData length]);
    
    for (s in symbols)
    {
    	mCodeFound = true;
        UnitySendMessage("CodeScannerBridge", "onScannerMessage", [s.data cStringUsingEncoding:NSUTF8StringEncoding]);
        break;
        //UIImageView* view = (UIImageView*)[self.view viewWithTag:2];
        //view.image = [UIImage imageWithData:pixelData];
        //view.hidden = NO;
        //mLabel.text = s.data;
    }
    
    [NSObject cancelPreviousPerformRequestsWithTarget:self selector:@selector(quit) object:nil];
    [self performSelector:@selector(quit) withObject:nil afterDelay:0.5];
    
}

-(void) init_screen 
{
	for (id v in [self.view subviews]) {
        if (![v isKindOfClass:[ZBarReaderView class]]) {
            [v removeFromSuperview];
        }
    }
        
    if (mShowUI)
    	[self init_ui];
    
    if(mDefaultText)
        [self init_label:mDefaultText];
}

- (void) init_camera
{
    if (mReader!=nil) {
    	[mReader release];
    	mReader = nil;
    }
    if (mScanner!=nil) {
    	[mScanner release];
    	mScanner = nil;
    }
    mReader = [[ZBarReaderView alloc] init];
    mScanner = [[ZBarImageScanner alloc] init];
    
    if (mSymbols>=0){
        [mScanner setSymbology:ZBAR_NONE config:ZBAR_CFG_ENABLE to:0];
        [mScanner setSymbology:(zbar_symbol_type_t)mSymbols config:ZBAR_CFG_ENABLE to:1];
    }
    
    [mReader initWithImageScanner:mScanner];
    mReader.readerDelegate = self;
    
    const float h = self.view.bounds.size.height;
    const float w = self.view.bounds.size.width;
    CGRect reader_rect = CGRectMake(0, 0, w, h);
    mReader.frame = reader_rect;
    mReader.backgroundColor = [UIColor blackColor];
    [mReader start];
    
    [self.view addSubview: mReader];
}

- (void) init_ui
{
	//mask
    UIView* mask1 = [[UIView alloc] initWithFrame:CGRectMake(0, 0, screen_width, screen_height/5)];
    mask1.backgroundColor = [UIColor colorWithRed:0 green:0 blue:0 alpha:0.6];
    [self.view addSubview:mask1];
    [mask1 release];
    UIView* mask2 = [[UIView alloc] initWithFrame:CGRectMake(0, screen_height/5, screen_width/10, screen_height-screen_height*2/5)];
    mask2.backgroundColor = [UIColor colorWithRed:0 green:0 blue:0 alpha:0.6];
    [self.view addSubview:mask2];
    [mask2 release];
    UIView* mask3 = [[UIView alloc] initWithFrame:CGRectMake(screen_width-screen_width/10, screen_height/5, screen_width/10, screen_height-screen_height*2/5)];
    mask3.backgroundColor = [UIColor colorWithRed:0 green:0 blue:0 alpha:0.6];
    [self.view addSubview:mask3];
    [mask3 release];
    UIView* mask4 = [[UIView alloc] initWithFrame:CGRectMake(0, screen_height-screen_height/5, screen_width, screen_height/5)];
    mask4.backgroundColor = [UIColor colorWithRed:0 green:0 blue:0 alpha:0.6];
    [self.view addSubview:mask4];
    [mask4 release];
    
    //border
    UIView* border = [[UIView alloc] initWithFrame:CGRectMake(screen_width/10, screen_height/5, screen_width-screen_width/5, screen_height-screen_height*2/5)];
    border.layer.borderColor = [UIColor blackColor].CGColor;
    border.layer.borderWidth = 2.0f;
    [self.view addSubview:border];
    [border release];
    
    //laser
    UIView* laser = [[UIView alloc] initWithFrame:CGRectMake(screen_width/10, screen_height/2, screen_width-screen_width/5, 2)];
    laser.backgroundColor = [UIColor colorWithRed:255 green:0 blue:0 alpha:0.7];
    [self.view addSubview:laser];
    [laser release];
    
    //Test Image
    //UIImageView* testImg = [[UIImageView alloc] initWithFrame:CGRectMake(w-105, 40, 100, 100)];
    //testImg.hidden = YES;
    //testImg.tag = 2;
    //[self.view addSubview:testImg];
    //[testImg release];

    
}

- (void) init_label:(char*)_text
{
    if (!mLabel)
    {
        mLabel = [[UILabel alloc] initWithFrame:CGRectMake(0, screen_height-screen_height/10, screen_width, screen_height/10)];
        mLabel.backgroundColor = [UIColor clearColor];
        mLabel.text = [[NSString alloc] initWithUTF8String:_text];
        mLabel.textAlignment = UITextAlignmentCenter;
        mLabel.textColor = [UIColor whiteColor];
        mLabel.numberOfLines = 2;
        mLabel.minimumFontSize = 10;
        mLabel.baselineAdjustment = UIBaselineAdjustmentAlignCenters;
    }
    
    mLabel.frame = CGRectMake(0, screen_height-screen_height/10, screen_width, screen_height/10);
    [self.view addSubview:mLabel];
}

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
	mReader.torchMode = 1;
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
    mReader.torchMode = 0;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Release any cached data, images, etc that aren't in use.
}

#pragma mark - View lifecycle

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    if ( SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(@"5.0") ) {
    	double delayInSeconds = 0.1f;
		dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, delayInSeconds * NSEC_PER_SEC);
		dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
    		[UIViewController attemptRotationToDeviceOrientation];
		});
		
		//UIInterfaceOrientation interfaceOrientation = [[UIApplication sharedApplication] statusBarOrientation];
    	//[self willRotateToInterfaceOrientation:interfaceOrientation duration:0.2f];
	}
     
}

- (void)viewDidUnload
{
    [super viewDidUnload];
    // Release any retained subviews of the main view.
    // e.g. self.myOutlet = nil;
}

- (void)viewWillAppear:(BOOL)animated
{
    [super viewWillAppear:animated];
}

- (void)viewDidAppear:(BOOL)animated
{
    [super viewDidAppear:animated];
}

- (void)viewWillDisappear:(BOOL)animated
{
	[super viewWillDisappear:animated];
}

- (void)viewDidDisappear:(BOOL)animated
{
	[super viewDidDisappear:animated];
}

/*
- (NSUInteger)supportedInterfaceOrientations
{
	if (mForceLandscape) {
		return UIInterfaceOrientationMaskLandscape;
	} else {
		return UIInterfaceOrientationMaskPortrait;
	}
}

- (BOOL)shouldAutorotate
{
	return NO;
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
{
	if (mForceLandscape && UIInterfaceOrientationIsPortrait(interfaceOrientation)){
        return NO;
    } else if (!mForceLandscape && UIInterfaceOrientationIsLandscape(interfaceOrientation)){
        return NO;
    }
    return YES;
}
*/

-(void)willRotateToInterfaceOrientation: (UIInterfaceOrientation)orientation duration:(NSTimeInterval)duration {

	UIInterfaceOrientation interfaceOrientation = [[UIApplication sharedApplication] statusBarOrientation];
	//NSLog(@"willRotateToInterfaceOrientation current=%d interfaceOrientation=%d", interfaceOrientation, orientation);

	//only id 90°
	if ((UIInterfaceOrientationIsPortrait(interfaceOrientation) && UIInterfaceOrientationIsLandscape(orientation)) || (UIInterfaceOrientationIsPortrait(orientation) && UIInterfaceOrientationIsLandscape(interfaceOrientation)))
	{
		//NSLog(@"willRotateToInterfaceOrientation rotate 90");
		float f = screen_height;
		screen_height = screen_width;
		screen_width = f;
		//NSLog(@"willRotateToInterfaceOrientation h=%f w=%f", screen_height, screen_width);
		[self init_screen];
	}
	
    [mReader willRotateToInterfaceOrientation:orientation duration:duration];
}

- (void)dealloc {
    
    [navController release];
    [mReader release];
    [mLabel release];
    [super dealloc];
}

EZCodeScannerViewController* instance;

struct ConfigStruct {
    bool showUI;
    char* defaultText;
    int symbols;
    bool forceLandscape;
};

void launchScannerImpl(struct ConfigStruct *confStruct) {
        
    instance = [[EZCodeScannerViewController alloc] initWithUI:confStruct->showUI withText:confStruct->defaultText withSymbol:confStruct->symbols withLandscape:confStruct->forceLandscape];
}

bool getScannedImageImpl(unsigned char** imageData, int* imageDataLength) {

	if (instance->mPixelData != nil) {
		*imageData = instance->mPixelData;
   		*imageDataLength = instance->mPixelSize;
   		return true;
	}

   return false;
}

void decodeImageImpl(int symbols, const char* pixelBytes, int64_t length) {

	if (pixelBytes != nil && length > 0) {
        
        NSData* pixelData = [NSData dataWithBytes:pixelBytes length:length];
        UIImage *uiimage=[UIImage imageWithData:pixelData];
        
		ZBarImage* image = [[ZBarImage alloc] initWithCGImage:uiimage.CGImage];
		
		ZBarImageScanner * scanner = [[ZBarImageScanner alloc] init];
		if (symbols>=0){
			[scanner setSymbology:ZBAR_NONE config:ZBAR_CFG_ENABLE to:0]; 
			[scanner setSymbology:(zbar_symbol_type_t)symbols config:ZBAR_CFG_ENABLE to:1];
		}
		
		NSInteger result = [scanner scanImage:image];
		[image release];
		
		NSString* data = nil;
		if (result > 0) {
			ZBarSymbolSet * set = scanner.results;
			ZBarSymbol* s = nil;
			for (s in set)
			{
				data = s.data;
			}
		}
		[scanner release];
		
		UnitySendMessage("CodeScannerBridge", "onDecoderMessage", [data cStringUsingEncoding:NSUTF8StringEncoding]);
	}
} 

@end
