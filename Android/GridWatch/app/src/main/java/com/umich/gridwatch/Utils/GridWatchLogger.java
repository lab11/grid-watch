package com.umich.gridwatch.Utils;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

public class GridWatchLogger {

	private final static String LOG_NAME = "gridwatch.log";
	private File mLogFile;
    private Context mContext;

	public GridWatchLogger(Context context) {
        mContext = context;
        if (android.os.Build.VERSION.SDK_INT>=19) {
            ArrayList<String> arrMyMounts = new ArrayList<String>();
            File[] possible_kitkat_mounts = mContext.getExternalFilesDirs(null);
            for (int x = 0; x < possible_kitkat_mounts.length; x++) {
                if (possible_kitkat_mounts[x] != null){
                    String logFilePath = possible_kitkat_mounts[x].toString();
                    mLogFile = new File(logFilePath, LOG_NAME);
                }
            }
        } else {
            mLogFile = new File(Environment.getExternalStorageDirectory(), LOG_NAME);
        }
	}

    //TODO put in check to round robin the log
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

	public static HashSet<String> getExternalMounts() {
		final HashSet<String> out = new HashSet<String>();
		String reg = "(?i).*vold.*(vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*";
		String s = "";
		try {
			final Process process = new ProcessBuilder().command("mount")
					.redirectErrorStream(true).start();
			process.waitFor();
			final InputStream is = process.getInputStream();
			final byte[] buffer = new byte[1024];
			while (is.read(buffer) != -1) {
				s = s + new String(buffer);
			}
			is.close();
		} catch (final Exception e) {
			e.printStackTrace();
		}

		// parse output
		final String[] lines = s.split("\n");
		for (String line : lines) {
			if (!line.toLowerCase(Locale.US).contains("asec")) {
				if (line.matches(reg)) {
					String[] parts = line.split(" ");
					for (String part : parts) {
						if (part.startsWith("/"))
							if (!part.toLowerCase(Locale.US).contains("vold"))
								out.add(part);
					}
				}
			}
		}
		return out;
	}

}
