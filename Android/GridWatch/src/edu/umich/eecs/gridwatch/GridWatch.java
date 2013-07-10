package edu.umich.eecs.gridwatch;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class GridWatch extends Activity {
	TextView mStatus;
	TextView mPendingCount;
	EditText mAlertServerEditText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_grid_watch);
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		String alertServer = settings.getString("alert_server",
				getString(R.string.default_alert_server));
		
		mStatus = (TextView) findViewById(R.id.status_text);
		mPendingCount = (TextView) findViewById(R.id.pending_count);
		mAlertServerEditText = (EditText) findViewById(R.id.alert_server);
		mAlertServerEditText.setHint(alertServer);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		Intent intent = new Intent(this, GridWatchService.class);
		startService(intent);
		
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, 
				new IntentFilter("GridWatch-update-event"));
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.grid_watch, menu);
		return true;
	}
	
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mPendingCount.setText(" "+Integer.toString(intent.getIntExtra("pending", 0)));
			if (intent.hasExtra("id")) {
				mStatus.setText("Most recent report:\n"
						+ "id: " + intent.getStringExtra("id") + "\n"
						+ "time: " + intent.getStringExtra("time") + "\n"
						+ "lat: " + intent.getStringExtra("lat") + "\n"
						+ "lon: " + intent.getStringExtra("lon") + "\n"
						+ "\n"
						+ "resp: " + intent.getStringExtra("resp")
						);
			} else {
				mStatus.setText("No incidents reported since startup");
			}
		}
	};
	
	public void setAlertServer(View view) {
		String alertServer = mAlertServerEditText.getText().toString();
		if (!URLUtil.isValidUrl(alertServer)) {
			Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show();
			return;
		}
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("alert_server", alertServer);
		editor.apply();
		
		mAlertServerEditText.setText("");
		mAlertServerEditText.setHint(alertServer);
		Toast.makeText(this, "Alert Server Updated", Toast.LENGTH_SHORT).show();
	}
	
	public void resetAlertServer(View view) {
		String alertServer = getString(R.string.default_alert_server);
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("alert_server", alertServer);
		editor.apply();
		
		mAlertServerEditText.setText("");
		mAlertServerEditText.setHint(alertServer);
		Toast.makeText(this, "Alert Server Updated", Toast.LENGTH_SHORT).show();
	}
}