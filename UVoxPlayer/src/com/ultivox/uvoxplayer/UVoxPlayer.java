package com.ultivox.uvoxplayer;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.ultivox.uvoxplayer.visualizer.VisualizerView;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

public class UVoxPlayer extends Activity {

    public static final String PARAM_PLAYER = "vizual";
    public static Activity activity;

	public final static String INIT_UMS_NB = "UMS000003";
	public final static String INIT_HOME_URL = "http://www.thinkinghouse.ru/netplayer/";
	public final static String INIT_CONFIG_URL = "http://www.thinkinghouse.ru/interface/";
	public final static String INIT_TEST_CONN_URL = "test000001/setup.txt";
	public final static String INIT_APK_FILE = "UVoxPlayer.apk";
	public final static String INIT_STORAGE = "/Music/netplayer";
	public final static String INIT_PLAYLISTS = "/playlists";
	public final static String INIT_MESSAGES = "/message";
	public final static String INIT_DBCONNECT = "rqstplayer.php?player=";
	public final static String INIT_COMCONNECT = "rqstplayer_com.php?player=";
	public final static String INIT_LOG_RECIEVER = "php/file_reciever.php?player=";
	public final static String INIT_LOGCAT_DIR = "/logcatmonitor";
	public final static String INIT_TIME_CONNECTION = "";
	public final static long INIT_INTERVAL_CONNECTION = 0;
	public final static long INTERVAL_CONNECTION_STAT = 60*60*1000; // 60*60*1000
	public static final String INIT_LOGCAT_SERVER = "http://www.thinkinghouse.ru/interface/php/logcat_reciever.php";
	public static final String INIT_LOGPLAY_SERVER = "http://www.thinkinghouse.ru/interface/php/logplay_reciever.php";
	public static final String INIT_SETTINGS_SERVER = "http://www.thinkinghouse.ru/interface/php/settings_reciever.php";
	public final static String INIT_LOGCAT_ON = "off";
	public final static String INIT_LOGPLAY_ON = "off";
	public final static String INIT_UPGRADE_ON = "off";
	public final static String INIT_DOWNLOAD_ON = "off";
	public final static String MESS_EXT = ".mp3";

	public static boolean PREF_EXIST = false;
	public static boolean VOL_EXIST = false;
	public static String UMS_NB;
	public static String HOME_URL;
	public static String CONFIG_URL;
	public static String TEST_CONN_URL;
	public static String APK_FILE;
	public static String STORAGE;
	public static String PLAYLISTS;
	public static String MESSAGES;
	public static String DBCONNECT;
	public static String COMCONNECT;
	public static String LOG_RECIEVER;
	public static String LOGCAT_DIR;
	public static String TIME_CONNECTION = null;
	public static long INTERVAL_CONNECTION = 0;
	public static String LOGCAT_SERVER;
	public static String LOGPLAY_SERVER;
	public static String SETTINGS_SERVER;
	public static String LOGCAT_ON;
	public static String LOGPLAY_ON;
	public static String UPGRADE_ON;
	public static String DOWNLOAD_ON;
	public static String ID;

	public final static String MAIN_PREF = "UVoxPref";
	public final static String VOL_PREF = "VolumeUVoxPref";
	public final static String LOG_COMMAND_MUSIC = "music";
	public final static String LOG_COMMAND_MESS = "mess";
	public final static String BROADCAST_PLAYINFO = "com.ultivox.uvoxplayer.play";
	public final static String BROADCAST_FINISH = "com.ultivox.uvoxplayer.finish";
	public final static String BROADCAST_MUSICINFO = "com.ultivox.uvoxplayer.music";
    public final static String BROADCAST_MESSINFO = "com.ultivox.uvoxplayer.message";
    public final static String BROADCAST_VOLUME = "com.ultivox.uvoxplayer.volume";

	public final static String BROADCAST_SHOW = "com.ultivox.uvoxplayer.show";
	public final static String PARAM_TEXT = "text";
	private BroadcastReceiver brStatus;

	// public final static String Download_ID = "DOWNLOAD_ID";
	public final static String PLAYINFO = "info";
	public static float volumeMusic = 1;
	public static float volumeMessage = 1;
	public static String[] playInfo;
	public static TextView textInfo;
	public static SharedPreferences settings;
	public static SharedPreferences volumes;
	private SeekBar musicSeekBar = null;
	private SeekBar messageSeekBar = null;
	TextView serviceInfo;
	final static String LOG_TAG = "UVoxPlayerLogs";
	String playTitle[] = { "Album:", "Artist:", "Song title:", "Genre:",
			"Duration:", "Bitrate:" };
	public static NetDbHelper netDbHelper;
	public static SQLiteDatabase currDb;

	private LogCatTask taskLogCat = null;
	private LogPlayTask taskLogPlay = null;
	private SettingsTask taskSettings = null;
	private TestConnectionTask tcTask = null;
	private DownloadTask dlTask = null;
	private UpgradeTask ugTask = null;

	private BroadcastReceiver br;
    private BroadcastReceiver brFinish;
    private BroadcastReceiver brVolume;

	private Intent intentMainService;

	private static ContentResolver contRes;

    private VisualizerView mVisualizerView;
    private static int playSessionId = 0;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(LOG_TAG, "onCreate");
		activity = this;
        String mountState = Environment.getExternalStorageState();
        int tries = 60;
        do {
            if (!mountState.equals(Environment.MEDIA_MOUNTED)) {
                try {
                    Thread.sleep(1000); // sleep for a second
                } catch (InterruptedException e) {
                    Log.w(LOG_TAG, "Interrupted!");
                    break;
                }
                mountState = Environment.getExternalStorageState();
            } else {
                Log.i(LOG_TAG, "External media mounted");
                break;
            }
        } while (--tries > 0);
        if (tries == 0) {
            Log.d(LOG_TAG, "Storage not mounted ---- System shutdown!");
            System.exit(0);
        }
		contRes = getContentResolver();
		setContentView(R.layout.activity_uvox_player);
		textInfo = (TextView) findViewById(R.id.textInfo);
		serviceInfo = (TextView) findViewById(R.id.textService);
		settings = getSharedPreferences(MAIN_PREF, MODE_PRIVATE);
		initGlobals(settings);
		volumes = getSharedPreferences(VOL_PREF, MODE_PRIVATE);
		initVolumes(volumes);
		createAppDirs();
		brStatus = new BroadcastReceiver() {
			// �������� ��� ��������� ���������

			@Override
			public void onReceive(Context context, Intent intent) {

				String infoService = intent.getStringExtra(PARAM_TEXT);
				serviceInfo.setText(infoService);
			}
		};
		IntentFilter intFStatus = new IntentFilter(BROADCAST_SHOW);
		registerReceiver(brStatus, intFStatus);

		// Setup music volume control
		musicSeekBar = (SeekBar) findViewById(R.id.seekBarMusic);
		musicSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				volumeMusic = (float) progress / 100;
				serviceInfo.setText(String.format("Volume level:%d %%",
						progress));
				Intent mesMusic = new Intent(BROADCAST_MUSICINFO);
				sendBroadcast(mesMusic);

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				SharedPreferences.Editor editor = volumes.edit();
				editor.putFloat("MUSIC", volumeMusic);
				editor.commit();
				serviceInfo.setText("");

			}
		});

		// Setup message volume control
		messageSeekBar = (SeekBar) findViewById(R.id.seekBarMess);
		messageSeekBar
				.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						volumeMessage = (float) progress / 100;
						serviceInfo.setText(String.format(
								"Message level:%d %%", progress));
						Intent mesMess = new Intent(BROADCAST_MESSINFO);
						sendBroadcast(mesMess);
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {

					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						SharedPreferences.Editor editor = volumes.edit();
						editor.putFloat("MESSAGE", volumeMessage);
						editor.commit();
						serviceInfo.setText("");

					}
				});
		br = new BroadcastReceiver() {
			// �������� ��� ��������� ���������

			@Override
			public void onReceive(Context context, Intent intent) {

				playInfo = intent.getStringArrayExtra(PLAYINFO);
				textInfo.setText("");
				for (int i = 0; i < playTitle.length; i++) {
					textInfo.append(playTitle[i] + playInfo[i] + "\n");
				}
			}
		};
		// ������� ������ ��� BroadcastReceiver
		IntentFilter intFilt = new IntentFilter(BROADCAST_PLAYINFO);
		// ������������ (��������) BroadcastReceiver
		registerReceiver(br, intFilt);

		brFinish = new BroadcastReceiver() {
			// �������� ��� ��������� ���������

			@Override
			public void onReceive(Context context, Intent intent) {

				Log.d(LOG_TAG, "Finish Activity");
				Log.d(LOG_TAG, "REBOOOT!");
				Intent inst = new Intent();
				inst.setClassName("com.ultivox.reinstall", "com.ultivox.reinstall.MainActivity");
				inst.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(inst);  
				finish();
			}
		};

		IntentFilter intFinish = new IntentFilter(BROADCAST_FINISH);
		registerReceiver(brFinish, intFinish);

        brVolume = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                int result = intent.getIntExtra(PARAM_PLAYER, 0);
                Log.d(LOG_TAG, String.format("Receive visulizer brodcast with %d extra",result));
                try {
                    if ((result==0)&&(playSessionId!=0)) {
                        // song start to play
                        playSessionId = result;
                        mVisualizerView.clearRenderers();
                        mVisualizerView.setEnabled(false);

                    }
                    if ((result!=0)&&(playSessionId==0)) {
                        // song stop to play
                        playSessionId = result;
                        mVisualizerView.setEnabled(true);
                        mVisualizerView.link(playSessionId);
                    } else {
                        playSessionId = result;
                    }
                } catch (IllegalStateException	 e) {
                    e.printStackTrace();
                }

            }
        };
        IntentFilter intVolume = new IntentFilter(BROADCAST_VOLUME);
        registerReceiver(brVolume, intVolume);

		Intent intStartServer = new Intent(MainService.BROADCAST_ACT_SERVER);
		intStartServer.putExtra(MainService.PARAM_RESULT,
				MainService.SERVER_START);
		PendingIntent pendIntentStartServer = PendingIntent.getBroadcast(this,
				0, intStartServer, 0);
		long nextTimeConnect = 0;
		AlarmManager alarmConnectServer = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		if (INTERVAL_CONNECTION > 0) {
			nextTimeConnect = Calendar.getInstance().getTimeInMillis()
					+ INTERVAL_CONNECTION;
		} else if (!TIME_CONNECTION.equals("")) {
			try {
				nextTimeConnect = MainService
						.dateParseTimeRegExp(TIME_CONNECTION);
			} catch (IllegalArgumentException e) {
				nextTimeConnect = Calendar.getInstance().getTimeInMillis()
						+ INTERVAL_CONNECTION_STAT;
				e.printStackTrace();
			}
		} else {
			nextTimeConnect = Calendar.getInstance().getTimeInMillis()
					+ UVoxPlayer.INTERVAL_CONNECTION_STAT;
		}
		alarmConnectServer.set(AlarmManager.RTC_WAKEUP, nextTimeConnect,
				pendIntentStartServer);
		Log.d(LOG_TAG, String
				.format("Set alarm to next server connection after %d sec",
						(nextTimeConnect - Calendar.getInstance()
								.getTimeInMillis()) / 1000));
		intentMainService = new Intent(this, MainService.class);
		startService(intentMainService);
	}

	@Override
	protected void onStart() {
		super.onStart();
		netDbHelper = new NetDbHelper(this, null, null, 1);
		currDb = netDbHelper.getWritableDatabase();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(LOG_TAG, "onResume");
		textInfo.setText("");
		if (playInfo != null)
			for (int i = 0; i < playInfo.length; i++) {
				textInfo.append(playTitle[i] + playInfo[i] + "\n");
			}
		musicSeekBar.setProgress((int) (volumeMusic * 100));
		messageSeekBar.setProgress((int) (volumeMessage * 100));
        mVisualizerView = (VisualizerView) findViewById(R.id.viewVolume);
        serviceInfo.setText("");
	}

	@Override
	public void onBackPressed() {
		return;
	}

	public void onClickStart(View v) {
		LogPlay.write("button", "Start", "press");
		startService(new Intent(this, MainService.class));
	}

	public void onClickStop(View v) {
        try {
            mVisualizerView.clearRenderers();
            mVisualizerView.setEnabled(false);
        } catch (IllegalStateException	 e) {
            e.printStackTrace();
        }
        LogPlay.write("button", "Stop", "press");
		if (taskLogCat != null
				&& taskLogCat.getStatus() != AsyncTask.Status.FINISHED) {
			taskLogCat.cancel(true);
			taskLogCat = null;
		}
		if (taskLogPlay != null
				&& taskLogPlay.getStatus() != AsyncTask.Status.FINISHED) {
			taskLogPlay.cancel(true);
			taskLogPlay = null;
		}
		if (taskSettings != null
				&& taskSettings.getStatus() != AsyncTask.Status.FINISHED) {
			taskSettings.cancel(true);
			taskSettings = null;
		}
		if (tcTask != null && tcTask.getStatus() != AsyncTask.Status.FINISHED) {
			tcTask.cancel(true);
			tcTask = null;
		}
		if (dlTask != null && dlTask.getStatus() != AsyncTask.Status.FINISHED) {
			dlTask.cancel(true);
			dlTask = null;
		}
		if (ugTask != null && ugTask.getStatus() != AsyncTask.Status.FINISHED) {
			ugTask.cancel(true);
			ugTask = null;
		}
		stopService(new Intent(this, MainService.class));
	}

	public void onClickNetset(View v) {
		LogPlay.write("button", "Net setting", "press");
		Intent i = new Intent(
				android.provider.Settings.ACTION_WIRELESS_SETTINGS);
		i.putExtra(":android:show_fragment",
				"com.android.settings.WirelessSettings");
		i.putExtra(":android:no_headers", true);
		startActivityForResult(i, 0);
	}

	public void onClickTimeset(View v) {
		LogPlay.write("button", "Time setting", "press");
		Intent i = new Intent(android.provider.Settings.ACTION_DATE_SETTINGS);
		i.putExtra(":android:show_fragment",
				"com.android.settings.DateTimeSettings");
		i.putExtra(":android:no_headers", true);
		startActivityForResult(i, 0);
	}

	public void onClickTest(View v) {
		LogPlay.write("button", "Test connection", "press");
		if (tcTask != null) {
			AsyncTask.Status tcStatus = tcTask.getStatus();
			if (tcStatus != AsyncTask.Status.FINISHED) {
				Log.v(LOG_TAG, "... no need to start new test connection task");
				return;
			}
		}
		tcTask = new TestConnectionTask(this);
		tcTask.runTask(null);
	}

	public void onClickDownload(View v) {
		LogPlay.write("button", "Download", "press");
		if (dlTask != null) {
			AsyncTask.Status dlStatus = dlTask.getStatus();
			if (dlStatus != AsyncTask.Status.FINISHED) {
				Log.v(LOG_TAG, "... no need to start new download task");
				return;
			}
		}
		dlTask = new DownloadTask(this);
		dlTask.runTask(null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.uvox_player, menu);
		return true;
	}

	// Menu handlers

	public void onUpgrade(MenuItem item) {
		LogPlay.write("button", "Upgrade software", "press");
		if (ugTask != null) {
			AsyncTask.Status ugStatus = ugTask.getStatus();
			if (ugStatus != AsyncTask.Status.FINISHED) {
				Log.v(LOG_TAG, "... no need to start new task");
				return;
			}
		}
		ugTask = new UpgradeTask(this);
		ugTask.runTask(null);
	}

	public void onTerminal(MenuItem item) {
		LogPlay.write("button", "Terminal option", "press");
		PackageManager pm = getPackageManager();
		try {
			pm.getPackageInfo("jackpal.androidterm",
					PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			Log.v(LOG_TAG, "jackpal.androidterm package not installed");
			return;
		}
		Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage(
				"jackpal.androidterm");
		startActivity(LaunchIntent);
	}

	public void onLogCat(MenuItem item) {

		LogPlay.write("button", "LogCat option", "press");
		TimeLogInfo newLog = new TimeLogInfo(this);
		if (newLog.getLogFile() != null) {
			if (taskLogCat != null) {
				AsyncTask.Status dlStatus = taskLogCat.getStatus();
				if (dlStatus != AsyncTask.Status.FINISHED) {
					Log.v(LOG_TAG, " client-server task allready run");
					return;
				}
			}
			taskLogCat = new LogCatTask(this);
			taskLogCat.runTask(newLog.getLogFile());
		}
	}

	public void onLogPlay(MenuItem item) {

		LogPlay.write("button", "LogPlay option", "press");
		if (taskLogPlay != null) {
			AsyncTask.Status dlStatus = taskLogPlay.getStatus();
			if (dlStatus != AsyncTask.Status.FINISHED) {
				Log.v(LOG_TAG, " client-server task allready run");
				return;
			}
		}
		taskLogPlay = new LogPlayTask(this);
		taskLogPlay.runTask(null);
	}

	public void onSettings(MenuItem item) {

		LogPlay.write("button", "Settings option", "press");
		if (taskSettings != null) {
			AsyncTask.Status dlStatus = taskSettings.getStatus();
			if (dlStatus != AsyncTask.Status.FINISHED) {
				Log.v(LOG_TAG, " client-server task allready run");
				return;
			}
		}
		taskSettings = new SettingsTask(this);
		taskSettings.runTask(null);
	}

	public void onAbout(MenuItem item) throws ClassNotFoundException,
			NoSuchMethodException {
		LogPlay.write("button", "About option", "press");
		System.exit(0);
	}

	public void onRestore(MenuItem item) {

		LogPlay.write("button", "About option", "press");
		String[] command = new String[] {"su", "-c", "busybox install /mnt/sdcard/Download/Launcher2.apk /system/app/"};
		Log.d(LOG_TAG,command[0]+" "+command[1]+" "+command[2]);
		Process reInsProc;
		try {
			reInsProc = Runtime.getRuntime().exec(command);
			int res = reInsProc.waitFor();
			Log.d(LOG_TAG,"Result of reinstall : "+res);
			if (res==0) {
				
			}
		}catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
//		currDb.close();
		Log.d(LOG_TAG, "onPause");
	}

	@Override
	public void onDestroy() {

		super.onDestroy();
		stopService(intentMainService);
		currDb.close();
		unregisterReceiver(br);
		unregisterReceiver(brFinish);
		unregisterReceiver(brStatus);
		Log.d(LOG_TAG, "Player stoped");
	}

	public static void initGlobals(SharedPreferences set) {

		Log.d(LOG_TAG, "Init Prepererences");
		if (set.getBoolean("PREF_EXIST", false)) {
			PREF_EXIST = set.getBoolean("PREF_EXIST", PREF_EXIST);
			UMS_NB = set.getString("UMS_NB", UMS_NB);
			HOME_URL = set.getString("HOME_URL", HOME_URL);
			CONFIG_URL = set.getString("CONFIG_URL", CONFIG_URL);
			TEST_CONN_URL = set.getString("TEST_CONN_URL", TEST_CONN_URL);
			APK_FILE = set.getString("APK_FILE", APK_FILE);
			STORAGE = set.getString("STORAGE", STORAGE);
			PLAYLISTS = set.getString("PLAYLISTS", PLAYLISTS);
			MESSAGES = set.getString("MESSAGES", MESSAGES);
			DBCONNECT = set.getString("DBCONNECT", DBCONNECT);
			COMCONNECT = set.getString("COMCONNECT", COMCONNECT);
			LOG_RECIEVER = set.getString("LOG_RECIEVER", LOG_RECIEVER);
			LOGCAT_DIR = set.getString("LOGCAT_DIR", LOGCAT_DIR);
			TIME_CONNECTION = set.getString("TIME_CONNECTION", TIME_CONNECTION);
			INTERVAL_CONNECTION = set.getLong("INTERVAL_CONNECTION",
					INTERVAL_CONNECTION);
			LOGCAT_SERVER = set.getString("LOGCAT_SERVER", LOGCAT_SERVER);
			LOGPLAY_SERVER = set.getString("LOGPLAY_SERVER", LOGPLAY_SERVER);
			SETTINGS_SERVER = set.getString("SETTINGS_SERVER", SETTINGS_SERVER);
			LOGCAT_ON = set.getString("LOGCAT_ON", LOGCAT_ON);
			LOGPLAY_ON = set.getString("LOGPLAY_ON", LOGPLAY_ON);
			UPGRADE_ON = set.getString("UPGRADE_ON", UPGRADE_ON);
			DOWNLOAD_ON = set.getString("DOWNLOAD_ON", DOWNLOAD_ON);
			ID = set.getString("ID", ID);

		} else {
			SharedPreferences.Editor editor = set.edit();
			PREF_EXIST = true;
			editor.putBoolean("PREF_EXIST", PREF_EXIST);
			UMS_NB = INIT_UMS_NB;
			editor.putString("UMS_NB", UMS_NB);
			HOME_URL = INIT_HOME_URL;
			editor.putString("HOME_URL", HOME_URL);
			CONFIG_URL = INIT_CONFIG_URL;
			editor.putString("CONFIG_URL", CONFIG_URL);
			TEST_CONN_URL = INIT_TEST_CONN_URL;
			editor.putString("TEST_CONN_URL", TEST_CONN_URL);
			APK_FILE = INIT_APK_FILE;
			editor.putString("APK_FILE", APK_FILE);
			STORAGE = INIT_STORAGE;
			editor.putString("STORAGE", STORAGE);
			PLAYLISTS = INIT_PLAYLISTS;
			editor.putString("PLAYLISTS", PLAYLISTS);
			MESSAGES = INIT_MESSAGES;
			editor.putString("MESSAGES", MESSAGES);
			DBCONNECT = INIT_DBCONNECT;
			editor.putString("DBCONNECT", DBCONNECT);
			COMCONNECT = INIT_COMCONNECT;
			editor.putString("COMCONNECT", COMCONNECT);
			LOG_RECIEVER = INIT_LOG_RECIEVER;
			editor.putString("LOG_RECIEVER", LOG_RECIEVER);
			LOGCAT_DIR = INIT_LOGCAT_DIR;
			editor.putString("LOGCAT_DIR", LOGCAT_DIR);
			TIME_CONNECTION = INIT_TIME_CONNECTION;
			editor.putString("TIME_CONNECTION", TIME_CONNECTION);
			INTERVAL_CONNECTION = INIT_INTERVAL_CONNECTION;
			editor.putLong("INTERVAL_CONNECTION", INTERVAL_CONNECTION);
			LOGCAT_SERVER = INIT_LOGCAT_SERVER;
			editor.putString("LOGCAT_SERVER", LOGCAT_SERVER);
			LOGPLAY_SERVER = INIT_LOGPLAY_SERVER;
			editor.putString("LOGPLAY_SERVER", LOGPLAY_SERVER);
			SETTINGS_SERVER = INIT_SETTINGS_SERVER;
			editor.putString("SETTINGS_SERVER", SETTINGS_SERVER);
			LOGCAT_ON = INIT_LOGCAT_ON;
			editor.putString("LOGCAT_ON", LOGCAT_ON);
			LOGPLAY_ON = INIT_LOGPLAY_ON;
			editor.putString("LOGPLAY_ON", LOGPLAY_ON);
			UPGRADE_ON = INIT_UPGRADE_ON;
			editor.putString("UPGRADE_ON", UPGRADE_ON);
			DOWNLOAD_ON = INIT_DOWNLOAD_ON;
			editor.putString("DOWNLOAD_ON", DOWNLOAD_ON);

			ID = Settings.Secure.getString(contRes, Settings.Secure.ANDROID_ID);
			editor.putString("ID", ID);
			editor.commit();
		}
	}

	public void initVolumes(SharedPreferences set) {

		Log.d(LOG_TAG, "Adjust volumes");
		if (set.getBoolean("VOL_EXIST", false)) {
			VOL_EXIST = set.getBoolean("VOL_EXIST", VOL_EXIST);
			volumeMusic = set.getFloat("MUSIC", 1);
			volumeMessage = set.getFloat("MESSAGE", 1);
		} else {
			SharedPreferences.Editor editor = set.edit();
			VOL_EXIST = true;
			editor.putBoolean("VOL_EXIST", VOL_EXIST);
			editor.putFloat("MUSIC", 1);
			editor.putFloat("MESSAGE", 1);
			editor.commit();
		}
	}

	private void createAppDirs() {

		String base = Environment.getExternalStorageDirectory().toString()
				+ INIT_STORAGE;
		Log.d(LOG_TAG, "Restore filesystem"+base);
		File dir = new File(base);
		if (!dir.exists()) {
			Log.d(LOG_TAG,base);
			dir.mkdir();
		}
		dir = new File(base + PLAYLISTS);
		if (!dir.exists()) {
			Log.d(LOG_TAG,base + PLAYLISTS);
			dir.mkdir();
		}
		dir = new File(base + MESSAGES);
		if (!dir.exists()) {
			Log.d(LOG_TAG,base + MESSAGES);
			dir.mkdir();
		}
	}
}
