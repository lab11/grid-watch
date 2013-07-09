//
//  LocationManager.h
//
//  Copyright (c) 2012 Symbiotic Software LLC. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <CoreLocation/CoreLocation.h>

#define NOTIFICATION_LOCATION_UPDATE @"NOTIFICATION_LOCATION_UPDATE"

// A simpler singleton class that issues notifications
@interface LocationManager : NSObject <CLLocationManagerDelegate>

+ (LocationManager *)sharedManager;
- (void)updateCurrentLocation;
- (CLLocation *)currentLocation;

@end
