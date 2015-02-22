package com.umich.gridwatch;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Icon;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.tileprovider.tilesource.MapboxTileLayer;
import com.mapbox.mapboxsdk.views.MapView;

import java.util.ArrayList;

/**
 * Created by nklugman on 2/20/15.
 */
public class GridWatchMapView extends Activity {
    private GridWatchLogger mGWLogger;
    private final static int num_markers = 1; //go back in the log this many to place markers

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapView mapView = new MapView(this);
        mapView.setAccessToken(Private.api_key);
        mapView.setTileSource(new MapboxTileLayer(Private.map_id));
        mapView.setZoom(20);
        mapView.clear();
        mGWLogger = new GridWatchLogger(this.getApplicationContext());

        int num_markers_yet = 0;
        ArrayList<String> log = mGWLogger.read();
        int count = num_markers;
        for (int i = log.size()-1; i >= 0; i--) {
            String report = log.get(i);
            String[] report_split = report.split(",");
            double lat = -1;
            double lng = -1;
            String type = "";
            for (int j = 0; j < report_split.length; j++) {
                Log.w("split", report_split[j]);
                String[] key_val = report_split[j].split("=");
                if (key_val != null) {
                    if (key_val.length >= 1) {
                        if (key_val[0].trim().equals("event_type")) {
                            Log.w("type", key_val[1]);
                            if (key_val[1].trim().equals("plugged") ||
                                    key_val[1].trim().equals("usr_plugged")) {
                                type = "plugged";
                                num_markers_yet++;

                            } else if (key_val[1].trim().equals("unplugged") ||
                                    key_val[1].trim().equals("usr_unplugged")) {
                                type = "unplugged";
                                num_markers_yet++;
                            }
                        }
                        if (key_val[0].trim().equals("gps_latitude")) {
                                lat = Double.parseDouble(key_val[1]);
                        }
                        if (key_val[0].trim().equals("gps_longitude")) {
                                lng = Double.parseDouble(key_val[1]);
                        }
                        if (lat != -1 && lng != -1) {
                            LatLng latLng = new LatLng(lat, lng);
                            if (!type.equals("")) {
                                draw_marker(mapView, latLng, type, count);
                            }
                        }
                    }
                }
            }
            if (num_markers_yet == num_markers) {
                break;
            }
            count--;
        }
        setContentView(mapView);
    }

    private void draw_marker(MapView mapView, LatLng latLng, String type, int count) {
        Marker marker = new Marker(mapView, "Log Number " + String.valueOf(count), "This marker represents a " + type + " report that was " + String.valueOf(count) + " from the most current report.", latLng);
        Drawable d = getResources().getDrawable(R.drawable.power_out_icon);
        if (type.equals("plugged")) {
            d = getResources().getDrawable(R.drawable.power_on_icon);
        }
        Icon marker_img = new Icon(d);
        marker.setIcon(marker_img);
        mapView.addMarker(marker);
        mapView.setCenter(latLng);
    }


}
