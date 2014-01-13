package edu.umich.eecs.gridwatch;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GridWatch extends Activity {
	private final static String INTENT_NAME = "GridWatch-update-event";
	private final static String INTENT_EXTRA_EVENT_TYPE = "event_type";
	private final static String INTENT_EXTRA_EVENT_INFO = "event_info";
	//private final static String INTENT_EXTRA_EVENT_TIME = "event_time";

	// This is the main page view
	View mMainView = null;

	// This is the entire log view that we create so we can add log
	// messages to it. Whenever the log needs to be displayed, call
	// setContentView(mLogView);
	View mLogView = null;

	// Tool for getting a pretty date
	DateFormat mDateFormat = DateFormat.getDateTimeInstance();

	private GridWatchLogger mGWLogger;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// Display the main homepage view and hide the back button
		mMainView = inflater.inflate(R.layout.activity_grid_watch, null);
		setContentView(mMainView);
		getActionBar().setDisplayHomeAsUpEnabled(false);

		// "Inflate" the log view so that we can append
		// log messages to it. Also save a reference to it
		// so we can display this copy of the log.
		mLogView = inflater.inflate(R.layout.log, null);

		// Register that we want to receive notices from the service.
	//	LocalBroadcastManager.getInstance(this).registerReceiver(mServiceMessageReceiver,
	//			new IntentFilter(INTENT_NAME));

		mGWLogger = new GridWatchLogger();

		Button log_refresh_btn = (Button) mLogView.findViewById(R.id.log_refresh);
		log_refresh_btn.setOnClickListener(refreshLog);

	}

	@Override
	protected void onResume() {
		super.onResume();

		// Make sure the service is running
		Intent intent = new Intent(this, GridWatchService.class);
		startService(intent);

	//	LocalBroadcastManager.getInstance(this).registerReceiver(mServiceMessageReceiver,
	//			new IntentFilter(INTENT_NAME));
		registerReceiver(mServiceMessageReceiver, new IntentFilter(INTENT_NAME));
	}

	@Override
	protected void onPause() {
		super.onPause();

		//LocalBroadcastManager.getInstance(this).unregisterReceiver(mServiceMessageReceiver);
		unregisterReceiver(mServiceMessageReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.grid_watch, menu);
		return true;
	}

	// This handles the callbacks from the service class.
	private BroadcastReceiver mServiceMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//mPendingCount.setText(" "+Integer.toString(intent.getIntExtra("pending_queue_len", 0)));

			// Update the front display
			if (intent.getStringExtra(INTENT_EXTRA_EVENT_TYPE).equals("event_post")) {

				Hashtable<String, String> result = new Hashtable<String, String>();

				// Create hashtable from the result string
				String event_info = intent.getStringExtra(INTENT_EXTRA_EVENT_INFO);
				String[] info_items = event_info.split("\\,\\ ");
				for (String item : info_items) {
					String[] kv = item.split("\\=");
					if (kv.length == 2) {
						result.put(kv[0], kv[1]);
					}
				}

				String display = result.get("event_type") + " at ";
				display += mDateFormat.format(new Date(Long.valueOf(result.get("time")))) + "\n";
				if (result.get("event_type").equals("unplugged")) {
					display += "movement: " + result.get("moved") + "\n";
					//display += "60 hz: " + result.get("sixty_hz") + "\n";
					display += "60 hz: not implemented" + "\n";
				}

				((TextView) mMainView.findViewById(R.id.txt_status)).setText(display);
			}

		}
	};

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			// Handle when the log item is selected from the menu
			case R.id.action_log:
				// Display the log layout and put the back button in the header
				setContentView(mLogView);
				getActionBar().setDisplayHomeAsUpEnabled(true);
				return true;

			// Handle when the back button is pressed
			case android.R.id.home:
				// Display the homepage and removed the back icon in the header
				setContentView(mMainView);
				getActionBar().setDisplayHomeAsUpEnabled(false);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	// Remove all text elements in the log linear items view
	private void clearLog () {
		LinearLayout log_linear_layout = (LinearLayout) mLogView.findViewById(R.id.log_linear);
		LinearLayout log_linear_items_layout = (LinearLayout) log_linear_layout.findViewById(R.id.log_linear_items);
		log_linear_items_layout.removeAllViews();
	}

	// Add a log line to the log view
	private void addLogItem (String time, String event_type, String info) {
		Context context = getApplicationContext();

		LinearLayout log_linear_layout = (LinearLayout) mLogView.findViewById(R.id.log_linear_items);
		View ruler = new View(context);
		ruler.setBackgroundColor(Color.DKGRAY);
		log_linear_layout.addView(ruler, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
		// Add the date timestamp
		TextView text_log_time = new TextView(context);
		text_log_time.setBackgroundColor(Color.WHITE);
		text_log_time.setTextColor(Color.BLACK);
		text_log_time.setText(time);
		log_linear_layout.addView(text_log_time, 1);
		// Add any information about what happened
		TextView text_log_entry = new TextView(context);
		text_log_entry.setBackgroundColor(Color.WHITE);
		text_log_entry.setTextColor(Color.BLACK);
		String log_entry = event_type;
		if (info != null) {
			log_entry += " - " + info;
		}
		text_log_entry.setText(log_entry);
		log_linear_layout.addView(text_log_entry, 2);
	}

	// Read in the log file and write it to the log view
	private View.OnClickListener refreshLog = new View.OnClickListener() {
		@Override
		public void onClick (View v) {
			clearLog();

			ArrayList<String> log = mGWLogger.read();

			for (String line : log) {
				String[] log_fields = line.split("\\|");
				if (log_fields.length > 1) {
					String time = log_fields[0];
					String event_type = log_fields[1];
					String info;
					if (log_fields.length > 2) {
						info = log_fields[2];
					} else {
						info = null;
					}
					addLogItem(time, event_type, info);
				}
			}
		}
	};

}