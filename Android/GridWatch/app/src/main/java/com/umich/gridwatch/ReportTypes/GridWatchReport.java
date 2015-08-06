package com.umich.gridwatch.ReportTypes;

import android.content.Context;

import com.umich.gridwatch.Utils.NetworkAndPhoneUtils;

import org.apache.http.NameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nklugman on 5/30/15.
 */
public class GridWatchReport {
    private String id;
    private String phone_type;
    private String user_name;
    private String os_name;
    private String os_version;
    private String app_version;
    private String beta_name;
    private String gps_latitude;
    private String gps_longitude;
    private String gps_accuracy;
    private String gps_time;
    private String gps_altitude;
    private String gps_speed;
    private String network_latitude;
    private String network_longitude;
    private String network_accuracy;
    private String network_time;
    private String network_altitude;
    private String network_speed;
    private String connection_type;
    private String time;
    private String event_type;
    private String failed;

    private GridWatchEvent mEvent;
    private static NetworkAndPhoneUtils util;
    private Context mContext;

    public GridWatchReport(GridWatchEvent gwEvent, Context context) {
        mEvent = gwEvent;
        mContext = context;
        util = new NetworkAndPhoneUtils(mContext);
    }

    public void set_id(String ID) {
        id = ID;
    }
    public void set_phone_type(String Phone_type) {
        phone_type = Phone_type;
    }
    public void set_user_name(String User_name) {
        user_name = User_name;
    }
    public void set_os_name(String Os_name) { os_name = Os_name; }
    public void set_os_version(String Os_version) { os_version = Os_version; }
    public void set_app_version(String Os_name) { os_name = Os_name; }
    public void set_beta_name(String Beta_name) { beta_name = Beta_name; }

    public void set_gps_latitude(String Gps_latitude) { gps_latitude = Gps_latitude; }
    public void set_gps_longitude(String Gps_longitude) { gps_longitude = Gps_longitude; }
    public void set_gps_accuracy(String Gps_accuracy) { gps_accuracy = Gps_accuracy; }
    public void set_gps_time(String Gps_time) { gps_time = Gps_time; }
    public void set_gps_altitude(String Gps_altitude) { gps_altitude = Gps_altitude; }
    public void set_gps_speed(String Gps_speed) { gps_speed = Gps_speed; }
    public void set_network_latitude(String Network_latitude) { network_latitude = Network_latitude; }
    public void set_network_longitude(String Network_longitude) { network_longitude = Network_longitude; }
    public void set_network_accuracy(String Network_accuracy) { network_accuracy = Network_accuracy; }
    public void set_network_time(String Network_time) { network_time = Network_time; }
    public void set_network_altitude(String Network_altitude) { network_altitude = Network_altitude; }
    public void set_network_speed(String Network_speed) { network_speed = Network_speed; }

    public void set_connection_type(String Connection_type) { connection_type = Connection_type; }
    public void set_time(String Time) { time = Time; }
    public void set_event_type(String Event_type) { event_type = Event_type; }
    public void set_failed(String Failed) { failed = Failed; }


    public List<NameValuePair> bundle_report() {
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(30);
        return nameValuePairs;
    }

    public void generate_report() {


        set_event_type(mEvent.getEventType());
        set_time(mEvent.getTime());
        set_failed(mEvent.getFailureMessage());
    }

    public void build_basics() {

    }



}
