package com.umich.gridwatch;

import android.os.Environment;

import java.io.*;
import java.util.ArrayList;

public class GridWatchSync {

	private final static String LOG_NAME = "gridwatch_state.log";

	private File mLogFile;

	public GridWatchSync () {
		File root = Environment.getExternalStorageDirectory();
		mLogFile = new File(root, LOG_NAME);
	}

	public void log (String time, String event_type, String info) {
		String l = time + "|" + event_type;
		if (info != null) {
			l += "|" + info;
		}
		try {
			FileWriter logFW = null;
			logFW = new FileWriter(mLogFile.getAbsolutePath(), true);
			logFW.write(l + "\n");
			logFW.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ArrayList<String> read () {
		ArrayList<String> ret = new ArrayList<String>(200);

		try {
			BufferedReader logBR = new BufferedReader(new InputStreamReader(new FileInputStream(mLogFile.getAbsolutePath())));

			int line_num = 0;
			String line;

			while ((line = logBR.readLine()) != null) {
				ret.add(line);
				if (line_num >= 100) {
					break;
				}
			}

			logBR.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ret;

	}
	
	public String get_last_value () {
		ArrayList<String> log = read();
		if (!log.isEmpty()) {
			String last = log.get(log.size() - 1);
			if (last != null) {
				String[] last_fields = last.split("\\|");
				if (last_fields.length > 1) {
					return last_fields[1];
				}
			}
		}
		return "-1"; 
	}

}
