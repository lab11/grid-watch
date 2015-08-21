package com.umich.gridwatch;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.umich.gridwatch.Utils.GridWatchLogger;

import java.util.ArrayList;

/**
 * Created by nklugman on 8/20/15.
 */
public class LogViewActivity extends ActionBarActivity {

    private static final String setupRefreshButton = "LogViewActivity:setupRefreshButton";
    private GridWatchLogger mGWLogger;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_view);
        mGWLogger = new GridWatchLogger(this.getApplicationContext());
        setupRefreshButton();
        refresh_log();
    }

    // Remove all text elements in the log linear items view
    private void clearLog () {
        LinearLayout log_linear_layout = (LinearLayout) findViewById(R.id.log_linear);
        LinearLayout log_linear_items_layout = (LinearLayout) log_linear_layout.findViewById(R.id.log_linear_items);
        log_linear_items_layout.removeAllViews();
    }

    // Add a log line to the log view
    private void addLogItem (String time, String event_type, String info) {
        Context context = getApplicationContext();

        LinearLayout log_linear_layout = (LinearLayout) findViewById(R.id.log_linear_items);
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

    private void refresh_log() {
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

    public void setupRefreshButton() {
        final Button btnRefresh = (Button) findViewById(R.id.log_refresh);
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               refresh_log();
            }
        });
    }

}
