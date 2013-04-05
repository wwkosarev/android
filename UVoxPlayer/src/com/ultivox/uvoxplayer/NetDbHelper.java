package com.ultivox.uvoxplayer;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class NetDbHelper extends SQLiteOpenHelper {
	
    private static final String DB_NAME = "netdb";

    private static final int DB_VERSION = 1;

    public static final String TABLE_MUSIC = "music";
    public static final String MUSIC_ID = "_id";
    public static final String MUSIC_DAY = "day";
    public static final String MUSIC_HOUR = "hour";
    public static final String MUSIC_TRACK = "track";
    
    public static final String TABLE_MESSAGES = "messages";
    public static final String MESSAGES_ID = "_id";
    public static final String MESSAGES_MESS_ID = "mess_id";
    public static final String MESSAGES_DATE_ON = "date_on";
    public static final String MESSAGES_DATE_OFF = "date_off";
    public static final String MESSAGES_TIME_ON = "time_on";
    public static final String MESSAGES_TIME_OFF = "time_off";
    public static final String MESSAGES_TIME_INT = "time_interval";
    public static final String MESSAGES_TIME_SHIFT = "time_shift";
    public static final String MESSAGES_RECURR = "recurrence";
    public static final String MESSAGES_DURATION = "duration";
    
    public static final String TABLE_MESS_SETS = "mess_sets";
    public static final String MESS_SETS_ID = "_id";
    public static final String MESS_SETS_MESS_ID = "mess_id";
    public static final String MESS_SETS_FILE = "file";
    public static final String MESS_SETS_ORDER = "ord";
    public static final String MESS_SETS_DURATION = "duration";
    
    public static final String TABLE_LOG = "log_play";
    public static final String LOG_ID = "_id";
    public static final String LOG_DATE = "date";
    public static final String LOG_COMAND = "comand";
    public static final String LOG_FILE = "file";
    public static final String LOG_STATUS = "stat";
    
    private Context cont;
    
    private static final String CREATE_TABLE_MUSIC = "CREATE TABLE " + TABLE_MUSIC + "("
    		+ MUSIC_ID + " integer NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE, "
    		+ MUSIC_DAY + " integer, "
    		+ MUSIC_HOUR + " integer, "
    		+ MUSIC_TRACK + " text);";
    
    private static final String CREATE_TABLE_MESSAGES = "CREATE TABLE " + TABLE_MESSAGES + "("
    		+ MESSAGES_ID + " integer NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE, "
    		+ MESSAGES_MESS_ID + " integer, "
    		+ MESSAGES_DATE_ON + " text, "
    		+ MESSAGES_DATE_OFF + " text, "
    		+ MESSAGES_TIME_ON + " integer, "
    		+ MESSAGES_TIME_OFF + " integer, "
    		+ MESSAGES_TIME_INT + " integer, "
    		+ MESSAGES_TIME_SHIFT + " integer, "
    		+ MESSAGES_RECURR + " integer, "
    		+ MESSAGES_DURATION + " integer);";
    
    private static final String CREATE_TABLE_MESS_SETS = "CREATE TABLE " + TABLE_MESS_SETS + "("
    		+ MESS_SETS_ID + " integer NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE, "
    		+ MESS_SETS_MESS_ID + " integer, "
    		+ MESS_SETS_FILE + " text, "
    	    + MESS_SETS_ORDER +" integer, "
    	    + MESS_SETS_DURATION + " real);";
    
    private static final String CREATE_TABLE_LOG = "CREATE TABLE " + TABLE_LOG + "("
    		+ LOG_ID + " integer NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE, "
    		+ LOG_DATE + " integer, "
    		+ LOG_COMAND + " text, "
    		+ LOG_STATUS + " text, "
    	    + LOG_FILE + " text);";


	public NetDbHelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, DB_NAME, factory, version);
		cont=context;
	}
	
	public NetDbHelper(Context context, String name, CursorFactory factory,
			int version, DatabaseErrorHandler errorHandler) {
		super(context, DB_NAME, factory, version, errorHandler);
		cont=context;
	}


	@Override
	public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_MUSIC);
        db.execSQL(CREATE_TABLE_MESSAGES);
        db.execSQL(CREATE_TABLE_MESS_SETS);
        db.execSQL(CREATE_TABLE_LOG);

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}

}
