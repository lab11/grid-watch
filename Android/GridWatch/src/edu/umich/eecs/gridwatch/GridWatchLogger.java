package edu.umich.eecs.gridwatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.os.Environment;

public class GridWatchLogger {

	private final static String LOG_NAME = "gridwatch.log";

	private File mLogFile;

	public GridWatchLogger () {
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

}
