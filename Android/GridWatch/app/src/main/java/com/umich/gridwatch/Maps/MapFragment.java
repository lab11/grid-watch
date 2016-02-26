package com.umich.gridwatch.Maps;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polygon;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.layers.CustomLayer;
import com.mapbox.mapboxsdk.views.MapView;
import com.umich.gridwatch.R;
import com.umich.gridwatch.Utils.GridWatchLogger;
import com.umich.gridwatch.Utils.Private;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MapFragment extends Fragment {
    private String onInitMapTag = "MapFragmet:InitMap";
    private String onDrawPolygonTag = "MapFragmet:DrawPolygon";
    private String onPurgePolygonsTag = "MapFragmet:onPurgePolygons";

    private long poly_index = 0;

    private long TIME_THRESH = 10000;
    private long PURGE_THRESH = 100;
    private long MOST_RECENT_PURGE = System.currentTimeMillis();

    private LocationManager mLocationManager;
    private GridWatchLogger mGWLogger;
    private final static int num_markers = 20; //go back in the log this many to place markers

    private ArrayList<GWPolygon> current_polygons;


    private MapView mv = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.activity_map, container, false);
        mv = (MapView) view.findViewById(R.id.mapview);
        mv.setAccessToken(Private.map_access);

        current_polygons = new ArrayList<>();

        initMap(mv, savedInstanceState);
        //load_log(mv);
        //load_world(mv);
        return view;

    }

    private static class GWPolygon {
        private long created_time;
        private Polygon poly;

        private GWPolygon(long created_time, Polygon poly) {
            this.created_time = created_time;
            this.poly = poly;
        }

        public long getCreated_time() {
            return created_time;
        }

        public Polygon getPoly() {
            return poly;
        }
    }

    private void purgePolygons() {
        Log.e(onPurgePolygonsTag, "hit");
        Log.e(onPurgePolygonsTag + ":firstIF", String.valueOf(MOST_RECENT_PURGE - System.currentTimeMillis()));

        if ((System.currentTimeMillis() - MOST_RECENT_PURGE) > PURGE_THRESH) { //Don't want to purge each message
            Log.e(onPurgePolygonsTag, "purging... normal op");
            for (int i = 0; i < current_polygons.size(); i++) {
                GWPolygon cur = current_polygons.get(i);
                if ((System.currentTimeMillis() - cur.getCreated_time()) > TIME_THRESH) { //Only want to throw away old things
                    Log.e(onPurgePolygonsTag, "removing polygon:" + String.valueOf(cur.getPoly().getId()));

                    current_polygons.remove(i);
                    mv.removeAnnotation(cur.getPoly());
                }
            }
        }
    }

    public void drawPolygon(ArrayList<LatLng> to_draw) {
        PolygonOptions p = new PolygonOptions()
                .addAll(to_draw)
                .strokeColor(Color.parseColor("#3bb2d0"))
                .fillColor(Color.parseColor("#3bb2d0"));
        Polygon poly = p.getPolygon();
        purgePolygons();
        current_polygons.add(new GWPolygon(System.currentTimeMillis(), poly));
        mv.addPolygon(p);
    }

    private void initMap(MapView mv, Bundle savedInstanceState) {

        mv.setStyleUrl(Style.EMERALD);
        LocationManager service = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = service.getBestProvider(criteria, false);
        Location location = service.getLastKnownLocation(provider);
        LatLng cur_loc = null;
        if (location != null) {
            cur_loc = new LatLng(location.getLatitude(), location.getLongitude());
        }
        if (cur_loc != null) {
            Log.e(onInitMapTag, cur_loc.toString());
            mv.setLatLng(cur_loc);
            mv.setZoom(10);
        } else {
            Log.e(onInitMapTag, "cur loc null");
            cur_loc = new LatLng(46.2000,6.1500); //set to geneva
            mv.setLatLng(cur_loc);
            mv.setZoom(5);
        }
        mv.setZoomControlsEnabled(true);
        mv.onCreate(savedInstanceState);

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

        Context context = getActivity().getApplicationContext();
        IconFactory mIconFactory = IconFactory.getInstance(context);
        Drawable d = ContextCompat.getDrawable(context, R.drawable.power_out_icon);
        if (type.equals("plugged")) {
            d = ContextCompat.getDrawable(context, R.drawable.power_on_icon);
        }
        Icon markerIcon = mIconFactory.fromDrawable(d);
        String titleStr = "Log Number " + String.valueOf(count);
        String infoStr = "This marker represents a " + type + " report that was " + String.valueOf(count) + " from the most current report.";
        mapView.addMarker(new MarkerOptions()
                .position(latLng)
                .title(titleStr)
                .snippet(infoStr)
                .icon(markerIcon));

    }

    @Override
    public void onStart() {
        super.onStart();
        mv.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mv.onResume();
    }

    @Override
    public void onPause()  {
        super.onPause();
        mv.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mv.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mv.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mv.onSaveInstanceState(outState);
    }


}


