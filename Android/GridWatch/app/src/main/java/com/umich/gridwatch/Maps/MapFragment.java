package com.umich.gridwatch.Maps;

import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Icon;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.umich.gridwatch.R;
import com.umich.gridwatch.Utils.GridWatchLogger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;


public class MapFragment extends Fragment {

    private LocationManager mLocationManager;
    private GridWatchLogger mGWLogger;
    private final static int num_markers = 20; //go back in the log this many to place markers

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.activity_map, container, false);
        MapView mv = (MapView) view.findViewById(R.id.mapview);

        init_map(mv);
        load_log(mv);
        load_world(mv);
        return view;

    }

    private void init_map(MapView mv) {
        mv.setUserLocationEnabled(true);
        mv.setUserLocationTrackingMode(UserLocationOverlay.TrackingMode.NONE);
        mv.setCenter(mv.getUserLocation());

        mv.setZoom(10);
        mv.setUserLocationRequiredZoom(10);

    }

    private void load_log(MapView map) {
        mGWLogger = new GridWatchLogger(getActivity());
        int num_markers_yet = 0;
        ArrayList<String> log = mGWLogger.read();
        int count = num_markers;
        for (int i = log.size() - 1; i >= 0; i--) {
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
                        Log.w("test_a", key_val[0]);
                        if (key_val[0].trim().equals("type")) {
                            Log.w("type_a", key_val[1]);
                            if (key_val[1].trim().equals("plugged") ||
                                    key_val[1].trim().equals("usr_plugged")) {
                                type = "plugged";

                            } else if (key_val[1].trim().equals("unplugged") ||
                                    key_val[1].trim().equals("usr_unplugged")) {
                                type = "unplugged";
                            }
                        }
                        if (key_val[0].trim().equals("lat")) {
                            lat = Double.parseDouble(key_val[1]);
                            Log.w("lat", String.valueOf(lat));
                        }
                        if (key_val[0].trim().equals("lng")) {
                            lng = Double.parseDouble(key_val[1]);
                            Log.w("lng", String.valueOf(lng));
                        }
                        if (lat != -1 && lng != -1) {
                            Log.w("redo", type);
                            LatLng latLng = new LatLng(lat, lng);
                            if (!type.equals("")) {
                                draw_marker(map, latLng, type, count);
                                num_markers_yet++;
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
    }

    private void load_world(MapView mv) {
        try {
            FileInputStream in = new FileInputStream("/sdcard/map.extension");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line = reader.readLine();
            while(line != null){
                Log.w("line", line);
                line = reader.readLine();
            }

        } catch (java.io.FileNotFoundException e){
            Log.e("MapFragment:load_world", "File Not Found");
        } catch (IOException e) {
            Log.e("MapFragment:load_world", "IOException");
        }
    }


    private void draw_marker(MapView mapView, LatLng latLng, String type, int count) {

        Marker marker = new Marker(mapView, "Log Number " + String.valueOf(count), "This marker represents a " + type + " report that was " + String.valueOf(count) + " from the most current report.", latLng);
        Drawable d = getResources().getDrawable(R.drawable.power_out_icon);
        Log.w("draw_marker", type);
        if (type.equals("plugged")) {
            d = getResources().getDrawable(R.drawable.power_on_icon);
        }
        Icon marker_img = new Icon(d);
        marker.setIcon(marker_img);
        mapView.addMarker(marker);
        mapView.setCenter(latLng);
    }


}


