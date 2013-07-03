//
//  chargingViewController.m
//  charging
//
//  Created by Noah Klugman on 6/19/13.
//  Copyright (c) 2013 Noah Klugman. All rights reserved.
//

#import "chargingViewController.h"
#import "LocationManager.h"
#import <CoreMotion/CoreMotion.h>
#import <SystemConfiguration/SystemConfiguration.h>
#import <ifaddrs.h>
#import <arpa/inet.h>
#import <AdSupport/AdSupport.h>


@interface chargingViewController ()

- (void)startAccelerometer;
- (void)stopAccelerometer;

@end

@implementation chargingViewController
@synthesize chargingLabel;
@synthesize accelLabel;
@synthesize serverLabel;
@synthesize IPLabel;
@synthesize sentLabel;
@synthesize GPSLabel;
@synthesize manager;
@synthesize timeLabel;


BOOL accelIsGoing;
float accelCount;
float firstAccelTime;

- (void)viewDidLoad
{

    [[UIDevice currentDevice] setBatteryMonitoringEnabled:YES];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(batteryStatus) name:UIDeviceBatteryStateDidChangeNotification object:nil];
      [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(GPSUpdate) name:NOTIFICATION_LOCATION_UPDATE object:nil];

    manager = [LocationManager sharedManager];
    [manager updateCurrentLocation];

    [self serverStatus];
    [self getIPAddress];
    [self batteryStatus];
    [self getSysTime];
    
    accelIsGoing = false;

    [self send];
    [super viewDidLoad];

}


-(NSString *)GPSUpdate
{
    NSLog(@"GPS Update");
    NSString * gps = [self getGPSString];
    GPSLabel.text = gps;
    return gps;
}

-(NSString *)getGPSString
{
    CLLocation * loc = manager.currentLocation;
    
    NSString * lng = ([NSString stringWithFormat:@"%.3f", loc.coordinate.longitude]);
    NSString * lat = ([NSString stringWithFormat:@"%.3f", loc.coordinate.latitude]);
    NSString * locGPSString = @"&lng=";
    locGPSString = [locGPSString stringByAppendingString:lng];
    locGPSString = [locGPSString stringByAppendingString:@"&lat="];
    locGPSString = [locGPSString stringByAppendingString:lat];
    return locGPSString;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
}

- (void)getIPAddress {
    NSString *address = @"error";
    struct ifaddrs *interfaces = NULL;
    struct ifaddrs *temp_addr = NULL;
    int success = 0;
    success = getifaddrs(&interfaces);
    if (success == 0) {
        temp_addr = interfaces;
        while(temp_addr != NULL) {
            if(temp_addr->ifa_addr->sa_family == AF_INET) {
                if([[NSString stringWithUTF8String:temp_addr->ifa_name] isEqualToString:@"en0"]) {
                    address = [NSString stringWithUTF8String:inet_ntoa(((struct sockaddr_in *)temp_addr->ifa_addr)->sin_addr)];
                }
            }
            temp_addr = temp_addr->ifa_next;
        }
    }
    freeifaddrs(interfaces);
    IPLabel.text = address;
}

- (bool)serverStatus
{
    bool success = false;
    const char *host_name = [@"www.google.com" cStringUsingEncoding:NSASCIIStringEncoding];
    SCNetworkReachabilityRef reachability = SCNetworkReachabilityCreateWithName(NULL, host_name);
    if (reachability) {
        SCNetworkReachabilityFlags flags;
        success = SCNetworkReachabilityGetFlags(reachability, &flags);
        bool isAvailable = success && (flags & kSCNetworkFlagsReachable) &&
        !(flags & kSCNetworkFlagsConnectionRequired);
        if (isAvailable) {
            serverLabel.text = @"Available";
            return true;
        }else{
            serverLabel.text = @"Down";
            return false;
        }
        CFRelease(reachability);
    }
    return true;
}


- (void)batteryStatus
{
    if ([[UIDevice currentDevice] batteryState] == UIDeviceBatteryStateUnknown)
    {
        chargingLabel.text = @"Unknown";
        accelLabel.text = @"Off";
        sentLabel.text = @"No";
        GPSLabel.text = @"Waiting";
    }
    if ([[UIDevice currentDevice] batteryState] == UIDeviceBatteryStateCharging)
    {
        //When charging, we don't care about acceleration
        chargingLabel.text = @"Charging";
        accelLabel.text = @"Off";
        sentLabel.text = @"No";
        GPSLabel.text = @"Waiting";
        [self stopAccelerometer];
    }
    if ([[UIDevice currentDevice] batteryState] == UIDeviceBatteryStateUnplugged)
    {
        //Just unplugged, now we measure all the things!
        chargingLabel.text = @"Unplugged";
        [self startAccelerometer];
    }
}

- (NSString *)getSysTime
{
    NSDate *today = [NSDate date];
    NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
    NSLocale* formatterLocale = [[NSLocale alloc] initWithLocaleIdentifier:@"en_GB"];
    [dateFormatter setLocale:formatterLocale];
    [dateFormatter setTimeStyle:NSDateFormatterShortStyle];
    NSString * dateWithColon = [dateFormatter stringFromDate:today];
    //NSLog(dateWithColon);
    timeLabel.text = dateWithColon;
    NSLocale* formatterLocale2 = [[NSLocale alloc] initWithLocaleIdentifier:@"en_US"]; //this is a hack to fix the iphone getting forced into GB when phone closes... TODO look up how to fix this
    return dateWithColon;
}


- (void)send
{
    
    //tack the time on the post
    NSString * dateWithColon = [self getSysTime];
    NSString *stringWithoutSpaces = [dateWithColon stringByReplacingOccurrencesOfString:@":" withString:@""];
    NSString *urlString = @"time=";
    urlString = [urlString stringByAppendingString:stringWithoutSpaces];
    
    //tack the id on the post
    NSString *udid;
    udid = [UIDevice currentDevice].identifierForVendor.UUIDString;
    urlString = [urlString stringByAppendingString:@"&id="];
    urlString = [urlString stringByAppendingString:udid];
    
    //tack the gps location on the post
    [manager updateCurrentLocation];
    
    
    //build and send the post
    NSString *myRequestString = urlString;
    NSData *myRequestData = [ NSData dataWithBytes: [ myRequestString UTF8String ] length: [ myRequestString length ] ];
    NSMutableURLRequest *request = [ [ NSMutableURLRequest alloc ] initWithURL: [ NSURL URLWithString: @"http://requestb.in/13kstae1" ] ];
    [ request setValue:@"application/x-www-form-urlencoded" forHTTPHeaderField:@"content-type"];
    [ request setHTTPMethod: @"POST" ];
    [ request setHTTPBody: myRequestData ];
    
    
    //get the response
    NSURLResponse *response;
    NSError *err;
    NSData *returnData = [ NSURLConnection sendSynchronousRequest: request returningResponse:&response error:&err];
    NSString *content = [NSString stringWithUTF8String:[returnData bytes]];
    sentLabel.text = @"Yup";
     
}

- (void)accelerometer:(UIAccelerometer *)accelerometer
        didAccelerate:(UIAcceleration *)acceleration
{
    NSNumber * accel = [NSNumber numberWithDouble: sqrt(acceleration.x*acceleration.x*acceleration.y*acceleration.y*acceleration.z*acceleration.z)];
    
    if (!accelIsGoing) //just want to grab the first timestamp
    {
        accelIsGoing = true;
        firstAccelTime = acceleration.timestamp;
    }
    else if (accelIsGoing) 
    {
        accelCount = accelCount + accel.unsignedLongValue;
        if (acceleration.timestamp - firstAccelTime > 5)
        {
            accelLabel.text = @"Off";
            [self stopAccelerometer];
            accelIsGoing = false; //turn off data gathering
            if (accelCount < 5)
            {
                [manager updateCurrentLocation];
                //sleep(30);
                [self send];
            }
            
        }
    }
    NSString *msg = [[NSString alloc] initWithFormat: @"%g", accel.doubleValue];
    accelLabel.text = msg;
}

- (void)startAccelerometer {
    UIAccelerometer *accelerometer = [UIAccelerometer sharedAccelerometer];
    accelerometer.delegate = self;
    accelerometer.updateInterval = 0.25;
}

- (void)stopAccelerometer {
    UIAccelerometer *accelerometer = [UIAccelerometer sharedAccelerometer];
    accelerometer.delegate = nil;
}


@end
