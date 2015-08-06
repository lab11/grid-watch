package com.umich.gridwatch.Maps;

import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.umich.gridwatch.R;


public class UnconnectedMapFragment extends Fragment {

    private LocationManager mLocationManager;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.activity_unconnected_map, container, false);
        return view;
    }


}


