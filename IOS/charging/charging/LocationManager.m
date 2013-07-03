//
//  LocationManager.m
//
//  Copyright (c) 2012 Symbiotic Software LLC. All rights reserved.
//

#import "LocationManager.h"

static id sharedInstance;

@interface LocationManager ()
{
	CLLocation *currentLocation;
}

@property (nonatomic, retain) CLLocationManager *locationManager;

- (void)applicationWillTerminate:(NSNotification *)notification;

@end

@implementation LocationManager

@synthesize locationManager;


+ (void)initialize
{
	if(sharedInstance == nil)
	{
		sharedInstance = [[LocationManager alloc] init];
		[[NSNotificationCenter defaultCenter] addObserver:sharedInstance selector:@selector(applicationWillTerminate:) name:UIApplicationWillTerminateNotification object:nil];
	}
}

- (void)applicationWillTerminate:(NSNotification *)notification
{
	[[NSNotificationCenter defaultCenter] removeObserver:self];
}

+ (LocationManager *)sharedManager
{
	return (LocationManager *)sharedInstance;
}

- (void)locationManager:(CLLocationManager *)manager didFailWithError:(NSError *)error
{
    NSLog(@"didFailWithError: %@", error);
    UIAlertView *errorAlert = [[UIAlertView alloc]
                               initWithTitle:@"Error" message:@"Failed to Get Your Location" delegate:nil cancelButtonTitle:@"OK" otherButtonTitles:nil];
    [errorAlert show];
}

- (void)updateCurrentLocation
{
	if(self.locationManager == nil)
		self.locationManager = [[CLLocationManager alloc] init];
    self.locationManager.desiredAccuracy = kCLLocationAccuracyThreeKilometers;
    //kCLLocationAccuracyHundredMeters;
	self.locationManager.delegate = self;
	[self.locationManager startUpdatingLocation];
}

- (CLLocation *)currentLocation
{
	return currentLocation;
}

#pragma mark - Internal Methods

- (void)dealloc
{
	[[NSNotificationCenter defaultCenter] removeObserver:self];
	[self.locationManager setDelegate:nil];
	self.locationManager = nil;
}

#pragma mark - CLLocationManagerDelegate methods
- (void)locationManager:(CLLocationManager *)manager didUpdateToLocation:(CLLocation *)newLocation fromLocation:(CLLocation *)oldLocation
{	
	NSTimeInterval interval;
	BOOL update = YES;
	

	if(self.currentLocation != nil)
	{
        NSLog(@"Found LOCATION");
		// Don't update if this was from the same startUpdatingLocation request
		interval = [newLocation.timestamp timeIntervalSinceDate:self.currentLocation.timestamp];
		if(interval < 3.0)
			update = NO;
	}
	
	[self.locationManager stopUpdatingLocation];
	
	if(update)
	{
        currentLocation = newLocation;

		[[NSNotificationCenter defaultCenter] postNotification:[NSNotification notificationWithName:NOTIFICATION_LOCATION_UPDATE object:self]];
	}
}

/*
- (void)locationManager:(CLLocationManager *)manager didUpdateToLocation:(CLLocation *)newLocation fromLocation:(CLLocation *)oldLocation
{
    NSLog(@"didUpdateToLocation: %@", newLocation);
    CLLocation *currentLocation = newLocation;
    
    if (currentLocation != nil) {
        NSLog(@"GPS");
        NSLog([NSString stringWithFormat:@"%.8f", currentLocation.coordinate.longitude]);
        NSLog([NSString stringWithFormat:@"%.8f", currentLocation.coordinate.latitude]);
    }
}
*/


@end
