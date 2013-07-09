//
//  chargingViewController.h
//  charging
//
//  Created by Noah Klugman on 6/19/13.
//  Copyright (c) 2013 Noah Klugman. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "LocationManager.h"


@interface chargingViewController : UIViewController <UIAccelerometerDelegate>
{
}

@property (weak, nonatomic) IBOutlet UILabel *serverLabel;
@property (weak, nonatomic) IBOutlet UILabel *IPLabel;
@property (weak, nonatomic) IBOutlet UILabel *sentLabel;
@property (weak, nonatomic) IBOutlet UILabel *chargingLabel;
@property (weak, nonatomic) IBOutlet UILabel *accelLabel;
@property (weak, nonatomic) IBOutlet UILabel *GPSLabel;
@property (weak, nonatomic) LocationManager *manager;
@property (weak, nonatomic) IBOutlet UILabel *timeLabel;

@end
