package com.umich.gridwatch.Sensors;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.umich.gridwatch.Utils.IntentConfig;
import com.umich.gridwatch.Utils.SensorConfig;

import java.util.ArrayList;

/**
 * Created by nklugman on 5/29/15.
 *
 * Not currently used. GPS is getting grabbed at GWService... this should change soon
 *
 */
public class GPSService extends IntentService {
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location currentBestLocation;
    ResultReceiver mResultReceiver;

    public GPSService() {
        super("GPSService");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        Log.d("onHandleIntent", "HIT");
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mResultReceiver = workIntent.getParcelableExtra(IntentConfig.RECEIVER_KEY);
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                checkLocation(location);
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
            public void onProviderEnabled(String provider) {
            }
            public void onProviderDisabled(String provider) {
            }
        };
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

        String locationProvider = LocationManager.GPS_PROVIDER;
        Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
        checkLocation(lastKnownLocation);
    }

    private void checkLocation(Location location) {
        if (isBetterLocation(location, currentBestLocation)) {
            currentBestLocation = location;
            Bundle bundle = new Bundle();
            bundle.putStringArrayList(IntentConfig.RESULT_KEY, turnLocToArray(currentBestLocation));
            mResultReceiver.send(IntentConfig.GPS, bundle);
            locationManager.removeUpdates(locationListener);
        }
    }

    private ArrayList<String> turnLocToArray(Location a) {
        ArrayList<String> loc = new ArrayList<String>();
        loc.add(String.valueOf(a.getLatitude()));
        loc.add(String.valueOf(a.getLongitude()));
        loc.add(String.valueOf(a.getAccuracy()));
        loc.add(String.valueOf(a.getProvider()));
        loc.add(String.valueOf(a.getSpeed()));
        loc.add(String.valueOf(a.getTime()));
        return loc;
    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > SensorConfig.GPS_CURRENT_THRESH;
        boolean isSignificantlyOlder = timeDelta < -SensorConfig.GPS_CURRENT_THRESH;
        boolean isNewer = timeDelta > 0;

        // If it's been more than GPS_CURRENT_THRESH minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > SensorConfig.ACCURACY_CHANGED_SIGNIFICANCE;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}
