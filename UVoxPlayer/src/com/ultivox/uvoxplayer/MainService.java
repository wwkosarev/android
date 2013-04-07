package com.ultivox.uvoxplayer;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainService extends Service {

	final String LOG_TAG = "MainService";
	public final static String BROADCAST_ACT_SCH = "com.ultivox.uvoxplayer.main";
	public final static String BROADCAST_ACT_SERVER = "com.ultivox.uvoxplayer.server";

	public final static int STATUS_MESSAGE = 100;
	public final static int STATUS_RELAUNCH = 200;
	public final static int STATUS_MESSAGE_STOP = 300;
	public final static int STATUS_PLAY_STOP = 400;
	public final static int STATUS_FADEOUT = 500;
	public final static int STATUS_FADEIN = 600;
	public final static int STATUS_SCHEDUL_END = 700;
	public final static int STATUS_PLAYLIST_EMPTY = 800;

	public final static int SERVER_START = 1000;
	public final static int SERVER_CONTINUE = 2000;
	public final static int SERVER_STOP = 3000;
	public final static int SERVER_ERROR = 4000;
	public final static int SERVER_RELOAD = 5000;

	public final static String PARAM_RESULT = "result";
	public final static String PARAM_NAME = "name";

	PlayMusicService playService;
	ServiceConnection sConn;
	boolean boundPlay = false;
	boolean fade = false;
	boolean playlistEmpty = false;
	boolean playDelay = false;
	Intent intentPlay;
	Intent intentSchedule;
	Intent intentMessage;
	Service self;

	public static long lastConnection = 0;
	private static boolean isCreated = false;
	private static boolean isStarted = false;
	private static int servId = 0;
	BroadcastReceiver brMain, brServer;
	int mess_name = 0;
	Queue<AsyncTask<String, String, String>> taskQueue = new LinkedList<AsyncTask<String, String, String>>();
	TimeLogInfo taskLogCat = null;
	protected boolean isConnectionSession = false;
	protected boolean reLoad = false;

	@Override
	public void onCreate() {
		super.onCreate();
		if (isCreated) {
			return;
		}
		self = this;
		Log.d(LOG_TAG, "onCreate");
		isCreated = true;
		intentPlay = new Intent(this, PlayMusicService.class);
		intentSchedule = new Intent(this, SchedulerService.class);
		intentMessage = new Intent(this, PlayMessageService.class);
		// ������� ����������� � �������� PlayMusicService
		sConn = new ServiceConnection() {

			public void onServiceConnected(ComponentName name, IBinder binder) {
				Log.d(LOG_TAG, "MainService onServiceConnected");
				playService = ((PlayMusicService.PlayBinder) binder)
						.getService();
				boundPlay = true;
			}

			public void onServiceDisconnected(ComponentName name) {
				Log.d(LOG_TAG, "MainService onServiceDisconnected");
				boundPlay = false;
			}
		};
		// ������� BroadcastReceiver
		brMain = new BroadcastReceiver() {
			// �������� ��� ��������� ���������

			@Override
			public void onReceive(Context context, Intent intent) {

				// Analyze extras of broadcast intend received

				int result = intent.getIntExtra(PARAM_RESULT, 0);
				switch (result) {
				case STATUS_MESSAGE: {
					// 1st - make fade-out, if message file exists.
					mess_name = intent.getIntExtra(PARAM_NAME, 0);
					pushAlltoQueue(mess_name);
					if (!fade) {
						fade = true;
						Log.d(LOG_TAG, "It's time for message. Id of message:"
								+ mess_name);
						playService.fadeOut();
					}
					break;
				}
				case STATUS_FADEOUT: {
					// play message
					Log.d(LOG_TAG, "FadeOut made. File of message:" + mess_name);
					startService(new Intent(context, PlayMessageService.class)
							.putExtra(PARAM_NAME, mess_name));
					break;
				}
				case STATUS_FADEIN: {
					// the end of fade-out fade-in procedure
					if (playDelay) { // Reload PlayMusicService if playDelay
										// triggered on.
						startService(intentPlay);
						bindService(intentPlay, sConn, 0);
						playDelay = false;
					}
					fade = false;
					break;
				}
				case STATUS_RELAUNCH: {
					// re-launch Scheduler Message Service
					Log.d(LOG_TAG, "Now reloading Message Scheduler");
					startService(intentSchedule);
					if (playlistEmpty) {
						Log.d(LOG_TAG, "Reload PlayMisicService");
						startService(intentPlay);
						bindService(intentPlay, sConn, 0);
						playlistEmpty = false;
					}
					break;
				}
				case STATUS_PLAYLIST_EMPTY: {
					// re-launch Scheduler Message Service
					Log.d(LOG_TAG, "There's no playlist for current hour");
					playlistEmpty = true;
					if (boundPlay) {
						unbindService(sConn);
						boundPlay = false;
					}
					stopService(intentPlay);
					break;
				}
				case STATUS_MESSAGE_STOP: {
					// message ended, make fade-in
					Log.d(LOG_TAG, "Message ended");
					playService.fadeIn();
					break;
				}
				case STATUS_PLAY_STOP: {
					// message ended, let's play music again
					Log.d(LOG_TAG,
							"Play new track"
									+ String.format("   boundPlay=%b",
											boundPlay));
					if (boundPlay) {
						unbindService(sConn);
						boundPlay = false;
					}
					stopService(intentPlay);
					if (fade) { // end of track in time with fade-out fade-in
								// procedure - stop delayPlayMusicService, but
								// delay it restart until this procedure will
								// ended
						playDelay = true;
					} else {
						startService(intentPlay);
						bindService(intentPlay, sConn, 0);
					}
					break;
				}
				case STATUS_SCHEDUL_END: {
					stopService(intentSchedule);
					break;
				}
				}
			}

		};
		// ������� ������ ��� BroadcastReceiver
		IntentFilter intFilt = new IntentFilter(BROADCAST_ACT_SCH);
		// ������������ (��������) BroadcastReceiver
		registerReceiver(brMain, intFilt);

		brServer = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {

				int result = intent.getIntExtra(PARAM_RESULT, 0);
				Log.d(LOG_TAG,
						String.format("Connect intent recieved #%d", result));
				AlarmManager alarmConnectServer = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
				switch (result) {
				case SERVER_START: {
					if (UVoxPlayer.LOGCAT_ON.equals("on")) {
						taskLogCat = new TimeLogInfo(context);
						taskQueue.offer(new LogCatTask(context));
					} else {
                        String logDirPath = Environment.getExternalStorageDirectory()
                                .toString() + UVoxPlayer.LOGCAT_DIR;
                        File logDir = new File(logDirPath);
                        if (logDir.exists()) {
                            String[] logFiles = logDir.list();
                            for (int i=0; i<logFiles.length; i++ ) {
                                    File f = new File(logDirPath + File.separator + logFiles[i]);
                                    if (f.exists()) {
                                        f.delete();
                                    }
                            }
                        }
                    }
					if (UVoxPlayer.LOGPLAY_ON.equals("on")) {
						taskQueue.offer(new LogPlayTask(context));
					}
					if (UVoxPlayer.UPGRADE_ON.equals("on")) {
						taskQueue.offer(new UpgradeTask(context));
					}
					if (UVoxPlayer.DOWNLOAD_ON.equals("on")) {
						taskQueue.offer(new DownloadTask(context));
					}
					taskQueue.offer(new SettingsTask(context));
					isConnectionSession = true;
                    LogPlay.write("system", "Server connection", "start");
                    SysInfo sys = new SysInfo();
                    LogPlay.write("system", sys.getMemFree(), "info");
                    LogPlay.write("system", sys.getIntMemFree(), "info");
                    Intent mesServ = new Intent(BROADCAST_ACT_SERVER);
					mesServ.putExtra(PARAM_RESULT, SERVER_CONTINUE);
					sendBroadcast(mesServ);
					break;
				}
				case SERVER_CONTINUE: {
					if (!isConnectionSession) {
						Intent mesServ = new Intent(BROADCAST_ACT_SERVER);
						mesServ.putExtra(PARAM_RESULT, SERVER_STOP);
						sendBroadcast(mesServ);
						break;
					}
					AsyncTask<String, String, String> currentTask = taskQueue
							.poll();
					if (currentTask != null) {
						if (taskLogCat != null) {
							currentTask.execute(taskLogCat.getLogFile());
						} else {
							currentTask.execute();
						}
					} else {
						Intent mesServ = new Intent(BROADCAST_ACT_SERVER);
						mesServ.putExtra(PARAM_RESULT, SERVER_STOP);
						sendBroadcast(mesServ);
					}
					break;
				}
				case SERVER_STOP: {
					if (reLoad) {
						Log.d(LOG_TAG, "Now process will be reloded!");
						// NOTsilent!!!
						// Intent intent = new Intent(Intent.ACTION_VIEW);
						// intent.setDataAndType(Uri.fromFile(new
						// File("/mnt/sdcard/Download/UVoxPlayer.apk")),
						// "application/vnd.android.package-archive");
						// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //
						// without this flag android returned a intent error!
						// this.startActivity(intent);
						// Silent
						Intent mesFinish = new Intent(UVoxPlayer.BROADCAST_FINISH);
						sendBroadcast(mesFinish);
						stopSelf();
						reLoad = false;
						return;
					}
                    LogPlay.write("system", "Server connection", "stop");
					Intent intStartServer = new Intent(BROADCAST_ACT_SERVER);
					intStartServer.putExtra(PARAM_RESULT, SERVER_START);
					PendingIntent pendIntentStartServer = PendingIntent
							.getBroadcast(context, 0, intStartServer, 0);
					long nextTimeConnect = 0;
					if (UVoxPlayer.INTERVAL_CONNECTION > 0) {
						nextTimeConnect = Calendar.getInstance()
								.getTimeInMillis()
								+ UVoxPlayer.INTERVAL_CONNECTION;
					} else if (!UVoxPlayer.TIME_CONNECTION.equals("")) {
						try {
							nextTimeConnect = dateParseTimeRegExp(UVoxPlayer.TIME_CONNECTION);
						} catch (IllegalArgumentException e) {
							nextTimeConnect = Calendar.getInstance()
									.getTimeInMillis()
									+ UVoxPlayer.INTERVAL_CONNECTION_STAT;
							e.printStackTrace();
						}
					} else {
						nextTimeConnect = Calendar.getInstance()
								.getTimeInMillis()
								+ UVoxPlayer.INTERVAL_CONNECTION_STAT;
					}
					alarmConnectServer.set(AlarmManager.RTC_WAKEUP,
							nextTimeConnect, pendIntentStartServer);
					isConnectionSession = false;
					lastConnection = Calendar.getInstance().getTimeInMillis();
					Log.d(LOG_TAG,
							String.format(
									"Set alarm to next server connection on %d sec delay",
									(nextTimeConnect - Calendar.getInstance()
											.getTimeInMillis()) / 1000));
					break;
				}
				case SERVER_ERROR: {
                    LogPlay.write("system", "Server connection", "error");
                    if (!isConnectionSession) {
						break;
					}
					Intent intStartServer = new Intent(BROADCAST_ACT_SERVER);
					intStartServer.putExtra(PARAM_RESULT, SERVER_START);
					PendingIntent pendIntentStartServer = PendingIntent
							.getBroadcast(context, 0, intStartServer, 0);
					long nextTimeConnect = 0;
					nextTimeConnect = Calendar.getInstance().getTimeInMillis()
							+ UVoxPlayer.INTERVAL_CONNECTION_STAT;
					alarmConnectServer.set(AlarmManager.RTC_WAKEUP,
							nextTimeConnect, pendIntentStartServer);
					isConnectionSession = false;
					Log.d(LOG_TAG,
							String.format(
									"Task queue ERROR! Set alarm to next server connection on %d (hour delay)",
									(nextTimeConnect - Calendar.getInstance()
											.getTimeInMillis()) / 1000));
					break;
				}
				case SERVER_RELOAD: {
					reLoad = true;
					Intent mesServ = new Intent(BROADCAST_ACT_SERVER);
					mesServ.putExtra(PARAM_RESULT, SERVER_CONTINUE);
					sendBroadcast(mesServ);
					break;
				}
				}

			}
		};
		// Set filter for server BroadcastReceiver
		IntentFilter intFiltServer = new IntentFilter(BROADCAST_ACT_SERVER);
		// turn-on server BroadcastReceiver
		registerReceiver(brServer, intFiltServer);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		servId = startId;
		if (isStarted) {
			return START_NOT_STICKY;
		}
		isStarted = true;
		startForeground(5555, new Notification());
		Log.d(LOG_TAG, "onStartCommand");
		startService(intentSchedule);
		startService(intentPlay);
		bindService(intentPlay, sConn, 0);
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		isStarted = false;
		isCreated = false;
		unregisterReceiver(brMain);
		unregisterReceiver(brServer);
		if (boundPlay) {
			unbindService(sConn);
			boundPlay = false;
		}
		stopService(intentSchedule);
		stopService(intentPlay);
		stopService(intentMessage);
		Log.d(LOG_TAG, "onDestroy");
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	private void pushAlltoQueue(int messSetId) {

		final String TAG = "pushAlltoQueue";
		String[] columns = new String[] { NetDbHelper.MESS_SETS_MESS_ID,
				NetDbHelper.MESS_SETS_FILE, NetDbHelper.MESS_SETS_ORDER,
				NetDbHelper.MESS_SETS_DURATION };
		String selection = NetDbHelper.MESS_SETS_MESS_ID + " = ? ";
		String[] selectionArgs = null;
		String orderBy = NetDbHelper.MESS_SETS_ORDER + " ASC";

		Cursor curMessage;

		Log.d(TAG, String.format("Recieve intent %d", messSetId));
		selectionArgs = new String[] { String.format("%d", messSetId) };
		curMessage = UVoxPlayer.currDb.query(NetDbHelper.TABLE_MESS_SETS,
				columns, selection, selectionArgs, null, null, orderBy);
		if (curMessage.moveToFirst()) {
			Log.d(TAG, String.format("Files number: %d", curMessage.getCount()));
			do {
				PlayMessageService.messQueue.add(curMessage
						.getString(curMessage
								.getColumnIndex(NetDbHelper.MESS_SETS_FILE)));
				Log.d(TAG, String.format("File %s order %d", curMessage
						.getString(curMessage
								.getColumnIndex(NetDbHelper.MESS_SETS_FILE)),
						curMessage.getInt(curMessage
								.getColumnIndex(NetDbHelper.MESS_SETS_ORDER))));
			} while (curMessage.moveToNext());
		}
	}

	public static long dateParseTimeRegExp(String period) {

		Pattern pattern = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})");
		Calendar m = Calendar.getInstance(); // midnight
		m.set(Calendar.HOUR_OF_DAY, 0);
		m.set(Calendar.MINUTE, 0);
		m.set(Calendar.SECOND, 0);
		m.set(Calendar.MILLISECOND, 0);
		long now = Calendar.getInstance().getTimeInMillis();
		long tillMidnight = now - m.getTimeInMillis();
		long event = 0L;
		Matcher matcher = pattern.matcher(period);
		if (matcher.matches()) {
			event = Long.parseLong(matcher.group(1)) * 3600000L
					+ Long.parseLong(matcher.group(2)) * 60000
					+ Long.parseLong(matcher.group(3)) * 1000;
		} else {
			throw new IllegalArgumentException("Invalid format " + period);
		}
		if (event > tillMidnight) {
			return m.getTimeInMillis() + event;
		} else {
			m.add(Calendar.DAY_OF_MONTH, 1);
			return m.getTimeInMillis() + event;
		}
	}
}
