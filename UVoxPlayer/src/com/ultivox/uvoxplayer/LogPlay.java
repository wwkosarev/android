package com.ultivox.uvoxplayer;

import android.content.ContentValues;
import android.util.Log;

public class LogPlay {
	
	final static String LOG_TAG = "LogPlay";
	public final static String STAT_BEGIN = "begin";
	public final static String STAT_END = "end";
	public final static String STAT_ERROR = "error";
	
	public static boolean write(String command, String file, String status) {
		
		ContentValues cv = new ContentValues();
		long dtMili = System.currentTimeMillis();
		cv.put(NetDbHelper.LOG_DATE, dtMili);
		cv.put(NetDbHelper.LOG_COMAND, command);
		cv.put(NetDbHelper.LOG_FILE, file);
		cv.put(NetDbHelper.LOG_STATUS, status);
		long addRows = UVoxPlayer.currDb.insert(NetDbHelper.TABLE_LOG,null, cv);
		if (addRows>0) {
			Log.d(LOG_TAG, String.format("Row #%d inserted", addRows));
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean clearAll() {
		
		int delRows = UVoxPlayer.currDb.delete(NetDbHelper.TABLE_LOG,null, null);
		if (delRows>0) {
			Log.d(LOG_TAG, String.format("%d rows deleted", delRows));
			return true;
		} else {
			return false;
		}
	}

}
