package edu.umich.eecs.gridwatch;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.widget.TextView;

public class GridWatch extends Activity {
	TextView mStatus;
	TextView mPendingCount;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_grid_watch);
		
		mStatus = (TextView) findViewById(R.id.status_text);
		mPendingCount = (TextView) findViewById(R.id.pending_count);
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
			mPendingCount.setText(Integer.toString(intent.getIntExtra("pending", 0)));
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
}