package com.umich.gridwatch;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.cocoahero.android.geojson.GeoJSON;
import com.cocoahero.android.geojson.GeoJSONObject;
import com.dd.processbutton.iml.ActionProcessButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.umich.gridwatch.Dialogs.AboutDialog;
import com.umich.gridwatch.Dialogs.AskDialog;
import com.umich.gridwatch.Dialogs.AskMapUpdateDialog;
import com.umich.gridwatch.Dialogs.ConsentDialog;
import com.umich.gridwatch.Dialogs.ReportDialog;
import com.umich.gridwatch.GCM.GCMPreferences;
import com.umich.gridwatch.GCM.GCMRegistrationIntentService;
import com.umich.gridwatch.Intro.TutorialActivity;
import com.umich.gridwatch.Maps.MapFragment;
import com.umich.gridwatch.Maps.UnconnectedMapFragment;
import com.umich.gridwatch.Utils.IntentConfig;
import com.umich.gridwatch.Utils.SensorConfig;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/*
This needs some rather major refactoring

This is the main UI class. It will one day be the only place
where dialogs are spawned. This would require changes in SettingsPerferenceActivity
and in other places.

Currently, updating the map, gcm, and getting the consent are done in this class...
They should be moved to individual sensors in the sensors folder
 */

public class HomeActivity extends ActionBarActivity implements ReportDialog.ReportDialogListener, AskDialog.AskDialogListener, AskMapUpdateDialog.AskMapUpdateDialogListener, ConsentDialog.ConsentDialogListener {
    private final static String onCreateTag = "homeActivity:onCreate";
    private final static String onCreateOptionsTag = "homeActivity:onCreateOptions";
    private final static String onOptionsItemSelectedTag = "homeActivity:onOptionsItemSelected";
    private final static String onResumeTag = "homeActivity:onResume";
    private final static String onReturnValueTag = "homeActivity:onReturnValue";
    private final static String setupManualReportButtonTag = "homeActivity:setupManualReportButton";
    private final static String startGWServiceTag = "homeActivity:startGWService";
    private final static String setupMapTag = "homeActivity:setupMap";
    private final static String initGCMTag = "homeActivity:initGCM";
    private final static String checkPlayServicesTag = "homeActivity:checkPlayServices";
    private final static String broadcastReceiverTag = "homeActivity:broadcastReceiver";
    private final static String onAskMapUpdateReturnValueTag = "homeActivity:onAskMapUpdateReturnValue";

    private ProgressDialog mProgressDialog;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static int cur_event_index;

    private boolean display_on = true; //display download progress or not. off had an odd UI block... push to its own thread and it would be ok...

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TODO delete before shipping... remove this and its permission
        Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(300);

        initGCM(); //set up GCM callbacks
        initIntentReceiver(); //the homeactivity should do all UI. needs to get callbacks
        initDownloader(); //setup the map downloader
        setupMap(); //load the map UI
        setupManualReportButton();
        //updateMap(); //download and parse the map code TODO implement fully
        doConsent(); //check for consent and force it
    }

    // Ask for consent if needed
    public void doConsent() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean consent_val = sp.getBoolean(SensorConfig.consent, false);
        if (!consent_val) {
            ConsentDialog dialog = new ConsentDialog();
            dialog.show(getSupportFragmentManager(), "AboutDialogFragment");
        }
    }


    // Get the value of the consent dialog if displayed
    @Override
    public void onConsentReturnValue(Boolean foo) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(SensorConfig.consent, foo);
        editor.apply();

        if (foo != true) {
            System.exit(0); //no consent. no app.
        } else {
            startGWService(); //get the background service going
        }
    }

    // Spawn the manual report event
    @Override
    public void onDialogReturnValue(Boolean foo) {
        if (foo == true) {
            Intent intent = new Intent(HomeActivity.this, GridWatchService.class);
            intent.putExtra(IntentConfig.INTENT_MANUAL_KEY, IntentConfig.INTENT_EXTRA_EVENT_MANUAL_OFF);
            startService(intent);
        }
    }

    // Download the map from the GW servers...
    public void updateMap() {
        downloadMap();
        try {
            GeoJSONObject geoJSON = GeoJSON.parse(readFromFile());
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void initDownloader() {
            mProgressDialog = new ProgressDialog(HomeActivity.this);
            mProgressDialog.setMessage("Updating Map");
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(true);
    }

    public void downloadMap() {
        final DownloadTask downloadTask = new DownloadTask(HomeActivity.this);
        downloadTask.execute("http://grid.watch:8088/cur_reports.json");
        if (display_on) {
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    downloadTask.cancel(true);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void initIntentReceiver() {
        registerReceiver(mBroadcastReceiver, new IntentFilter(IntentConfig.INTENT_NAME));
    }

    @Override
    protected void onStop()
    {
        unregisterReceiver(mBroadcastReceiver);
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_about) {
            DialogFragment dialog = new AboutDialog();
            dialog.show(getSupportFragmentManager(), "AboutDialogFragment");
            return true;
        }

        //TODO change this into seperate login/tutorial models
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsPreferenceActivity.class);
            this.startActivity(intent);
            return true;
        }

        if (id == R.id.action_view_log) {
            startActivity(new Intent(HomeActivity.this, LogViewActivity.class));
        }

        if (id == R.id.action_refresh) {
            setupAskMapUpdate();
        }

        if (id == R.id.action_tutorial) {
            startActivity(new Intent(HomeActivity.this, TutorialActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    // This will be used to catch the result of the manual dialog and spawn another dialog reporting success
    @Override
    public void onAskReturnValue(Boolean foo) {
        Intent intent = new Intent(HomeActivity.this, GridWatchService.class);
        intent.putExtra(IntentConfig.INTENT_MANUAL_KEY, IntentConfig.INTENT_EXTRA_EVENT_GCM_ASK_RESULT);
        intent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_GCM_ASK_RESULT, foo);
        intent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_GCM_ASK_INDEX, cur_event_index);
        startService(intent);
    }

    // Set the auto updating map preferences
    @Override
    public void onAskMapUpdateReturnValue(String foo) {
        //Intent intent = new Intent(HomeActivity.this, GridWatchService.class);
        Log.d(onAskMapUpdateReturnValueTag, String.valueOf(foo));
        if (String.valueOf(foo).equals(SensorConfig.yes) || String.valueOf(foo).equals(SensorConfig.always)) {
            display_on = true;
            if (foo.equals(SensorConfig.always)) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("map_update_pref", SensorConfig.always);
                Log.d(onAskMapUpdateReturnValueTag + " setting always", sp.getString("map_update_title", ""));
                editor.putString("map_update_values", "Auto");
                editor.apply();
            }
            updateMap();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBroadcastReceiver, new IntentFilter(IntentConfig.INTENT_NAME));
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(GCMPreferences.REGISTRATION_COMPLETE));
        startGWService();
    }

    @Override
    protected void onPause() {
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    // The app needs google play to do the GCM stuff
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.d(checkPlayServicesTag, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    // Init the GCM code
    private void initGCM() {
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(GCMPreferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    Log.d(initGCMTag, getString(R.string.gcm_send_message));
                } else {
                    Log.d(initGCMTag, getString(R.string.token_error_message));
                }
            }
        };
        if (checkPlayServices()) {
            startService(new Intent(this, GCMRegistrationIntentService.class));
        }
    }

    // Get the background process running... This little call is pretty important
    private void startGWService() {
        Intent intent = new Intent(this, GridWatchService.class);
        startService(intent);
    }

    // Shows the main map
    public void setupMap() {
        //TODO add in offline error message here... maybe cache last map if possible?... this doesnt seem to work yet
        if (!isConnected()) {
            Log.d(setupMapTag, "not connected");
            //DialogFragment dialog = new NoConnectionDialog();
            //dialog.show(getSupportFragmentManager(), "NoConnectionDialogFragment");
            Fragment fragment = new UnconnectedMapFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .commit();
        } else {
            Fragment fragment = new MapFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .commit();
        }
    }

    // The button to manually report a power outage
    public void setupManualReportButton() {
        final ActionProcessButton btnReport = (ActionProcessButton) findViewById(R.id.report_btn);
        btnReport.setMode(ActionProcessButton.Mode.PROGRESS);
        btnReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(setupManualReportButtonTag, "REPORT CLICKED");
                DialogFragment dialog = new ReportDialog();
                dialog.show(getSupportFragmentManager(), "ReportDialogFragment");
            }
        });
    }

    // Sets up the map update preference dialog box
    public void setupAskMapUpdate() {
        SharedPreferences sp =  PreferenceManager.getDefaultSharedPreferences(this);
        Map<String,?> keys = sp.getAll();
        String map_update_pref = (String) keys.get("map_update_pref"); //hack for now... should be Integer, but this was crashing all the time... not important enough to debug yet
        if (map_update_pref != null) {
            if (!map_update_pref.equals(SensorConfig.always)) {
                AskMapUpdateDialog dialog = new AskMapUpdateDialog();
                dialog.show(getSupportFragmentManager(), "AskMapUpdateDialogFragment");
            } else {
                updateMap();
            }
        }
    }

    // Generate the GCM ask UI
    public void setupAskReport(int i) {
        cur_event_index = i;
        Log.d(setupManualReportButtonTag, "ASK GENERATED");
        AskDialog dialog = new AskDialog();
        dialog.show(getSupportFragmentManager(), "AskDialogFragment");
    }

    // Get all the intents. This is scafolding to handle UI events from around the app
    public BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra(IntentConfig.INTENT_TO_HOME) == null) return; //should be changed so that this isn't broadcast
            Log.d(broadcastReceiverTag, "Event Received in HomeActivity");
            if (intent.getStringExtra(IntentConfig.INTENT_TO_HOME).equals(IntentConfig.INTENT_EXTRA_EVENT_GCM_ASK)) {
                setupAskReport(intent.getIntExtra(IntentConfig.INTENT_EXTRA_EVENT_GCM_ASK_INDEX, 0));
                Log.d(broadcastReceiverTag, "confirm gcm ask");
            }
            /*
            else if (intent.getStringExtra(IntentConfig.INTENT_TO_HOME).equals(IntentConfig.INTENT_EXTRA_EVENT_CONFIRM_DELETE)) {
                Log.w(broadcastReceiverTag, "confirm delete");
                setupConfirmDeleteDialog();
            }
            */
        }
    };

    // TODO move to NetworkAndPhoneUtils
    public boolean isConnected() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // Class for downloading and parsing map data from GW servers. This is formated as a GEOJSON.
    //TODO finish implementing and add in password protection
    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                int fileLength = connection.getContentLength();
                input = connection.getInputStream();
                output = new FileOutputStream("/sdcard/map.extension");

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }
                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            if (display_on) {
                mProgressDialog.show();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            if (display_on) {
                mProgressDialog.setIndeterminate(false);
                mProgressDialog.setMax(100);
                mProgressDialog.setProgress(progress[0]);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            if (display_on) {
                mProgressDialog.dismiss();
            }
            if (result != null)
                Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();
            else {
             if (display_on) {
                 Toast.makeText(context, "File downloaded", Toast.LENGTH_SHORT).show();
             }
            }
        }
    }

    // Read in the local log for the map
    private String readFromFile() {
        File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(sdcard,"map.extension");
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
        }
        return text.toString();
    }
}
