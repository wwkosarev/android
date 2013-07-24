package com.ultivox.uvoxplayer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.util.Log;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SchedulerService extends Service {

	final String LOG_TAG = "SchedulerService";

	int stepInterval = 5*60*1000; //in mills
	int stepShift = 30*1000; //in mills
	int stepHour = 60*60*1000; //in mills
	int delta = 1000; //in mills
	Calendar timeNow;
	Calendar timeVar;
	Calendar timeDayBegin;	// ���� ������ ���
	Calendar timeScheduler; // ����+��� ������� ������������ ��� ����������������
							// ���������� ������� ������ ������.
	
	private ExecutorService esMessageScheduler;

	@Override
	public void onCreate() {
		super.onCreate();
		esMessageScheduler = Executors.newFixedThreadPool(1);
		Log.d(LOG_TAG, "onCreate");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		Log.d(LOG_TAG, "onStartCommand");
		ScheduleMessages sm = new ScheduleMessages(this);
		esMessageScheduler.execute(sm);

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(LOG_TAG, "onDestroy");
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(LOG_TAG, "onBind");
		return null;
	}

	class ScheduleMessages implements Runnable {

		// ���������� ��� query
		String[] columns = null;
		String selection = null;
		String[] selectionArgs = null;
		String groupBy = null;
		String having = null;
		String orderBy = null;
		private Context upContext;

		// ������
		Cursor curMessage = null;
		AlarmManager alarmMgr;

		public ScheduleMessages(Context context) {

			upContext = context;

			timeNow = Calendar.getInstance();
			timeVar = Calendar.getInstance();
			timeDayBegin = Calendar.getInstance();
			timeScheduler = Calendar.getInstance();  // Moment of start next hour

		}

		public void run() {

			// to trigger the alarm
			int currentYear = timeNow.get(Calendar.YEAR);
			int currentMonth = timeNow.get(Calendar.MONTH);
			int currentDay = timeNow.get(Calendar.DAY_OF_MONTH);
			int currentHour = timeNow.get(Calendar.HOUR_OF_DAY);
			String cuttentDate = String.format("%04d-%02d-%02d", currentYear, currentMonth+1, currentDay);
			Log.d(LOG_TAG,String.format("Date: %s hour: %d", cuttentDate,currentHour));
			timeScheduler.setTimeInMillis(0);
			timeScheduler.set(currentYear, currentMonth, currentDay, currentHour+1, 0, 0);
			timeDayBegin.setTimeInMillis(0);
			timeDayBegin.set(currentYear, currentMonth, currentDay, 0, 0, 0);

			columns = new String[] {
					NetDbHelper.MESSAGES_MESS_ID,
					NetDbHelper.MESSAGES_DATE_ON,
					NetDbHelper.MESSAGES_DATE_OFF,
					NetDbHelper.MESSAGES_TIME_ON, 
					NetDbHelper.MESSAGES_TIME_OFF,
					NetDbHelper.MESSAGES_TIME_INT,
					NetDbHelper.MESSAGES_TIME_SHIFT,
					NetDbHelper.MESSAGES_RECURR,
					NetDbHelper.MESSAGES_DURATION };
			selection ="((" + NetDbHelper.MESSAGES_DATE_ON + " <= ? AND " + NetDbHelper.MESSAGES_DATE_OFF + " > ? ) " +
					"AND (" + NetDbHelper.MESSAGES_TIME_ON + "<= ? AND " + NetDbHelper.MESSAGES_TIME_OFF + " > ? ))";
			selectionArgs = new String[] {cuttentDate, cuttentDate,String.format("%d", currentHour),String.format("%d", currentHour)};
			curMessage = UVoxPlayer.currDb.query(NetDbHelper.TABLE_MESSAGES, columns, selection,	selectionArgs, null, null, null);
			Log.d(LOG_TAG, "Select exec!");
			alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
			if (curMessage.moveToFirst() != false) {
				int i = 1;
				do {
					int k = columns.length;
					String row ="";
					for (int j=0; j<k; j++) {
						if ((j==1)||(j==2)) {
							row = row + columns[j]+ ": " +curMessage.getString(j) + "  ";
						} else {
							row = row + columns[j]+ ": " + curMessage.getLong(j) + "  ";
						}
					}
					Log.d(LOG_TAG, row);
					long start = timeDayBegin.getTimeInMillis() + 
							stepHour*curMessage.getLong(curMessage.getColumnIndex(NetDbHelper.MESSAGES_TIME_ON)) +
							stepShift*curMessage.getLong(curMessage.getColumnIndex(NetDbHelper.MESSAGES_TIME_SHIFT));
					long varTime = start+delta;
					long recurrence = curMessage.getLong(curMessage.getColumnIndex(NetDbHelper.MESSAGES_RECURR));
					long interval = stepInterval*curMessage.getLong(curMessage.getColumnIndex(NetDbHelper.MESSAGES_TIME_INT));
					long now = timeNow.getTimeInMillis();
					long end = timeScheduler.getTimeInMillis();
					Log.d(LOG_TAG,String.format("Start: %d End: %d varTime: %d reccur: %d interval: %d now: %d", start,end,varTime,recurrence,interval,now));
					do {
						if (varTime>=now) {
							Log.d(LOG_TAG, String.format("Time to AlarmManager %d", varTime));
							timeVar.setTimeInMillis(varTime);
//							Log.d(LOG_TAG,timeVar.toString());
							Intent mesint = new Intent(UVoxPlayer.BROADCAST_ACT_SCH);
							mesint.putExtra(UVoxPlayer.PARAM_RESULT,
                                    UVoxPlayer.STATUS_MESSAGE);
							mesint.putExtra(UVoxPlayer.PARAM_NAME, curMessage.getInt(curMessage.getColumnIndex(NetDbHelper.MESSAGES_MESS_ID)));
							PendingIntent pendingIntent = PendingIntent.getBroadcast(
									upContext, i, mesint, 0);
							alarmMgr.cancel(pendingIntent);
							Log.d(LOG_TAG,mesint.toString());
							alarmMgr.set(AlarmManager.RTC_WAKEUP, varTime, pendingIntent);
							i++;
						}
						varTime =  varTime + interval;
					} while ((recurrence>0)&&(varTime<end));
				} while (curMessage.moveToNext());
			} else {
				Log.d(LOG_TAG, "Have no data to schedule messeges");
			}
			// if last connection to server out off limit
			long nowMoment = Calendar.getInstance().getTimeInMillis();
			if ((nowMoment - UVoxPlayer.lastConnection - UVoxPlayer.INTERVAL_CONNECTION) >= 0) {
				Log.d(LOG_TAG,"It's too many time afte last server connection");
				// Set 5 sec alarm to server connection
				AlarmManager alarmConnectServer = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
				Intent intStartServer = new Intent(UVoxPlayer.BROADCAST_ACT_SERVER);
				intStartServer.putExtra(UVoxPlayer.PARAM_RESULT, UVoxPlayer.SERVER_START);
				PendingIntent pendIntentStartServer = PendingIntent.getBroadcast(upContext, 0, intStartServer, 0);
				alarmConnectServer.set(AlarmManager.RTC_WAKEUP, nowMoment+5000, pendIntentStartServer);
			}
			stop();
		}

		void stop() {
			Intent mesint = new Intent(UVoxPlayer.BROADCAST_ACT_SCH);
			mesint.putExtra(UVoxPlayer.PARAM_RESULT,
                    UVoxPlayer.STATUS_RELAUNCH);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(upContext,
					0, mesint, 0);
			alarmMgr.set(AlarmManager.RTC_WAKEUP,
					timeScheduler.getTimeInMillis(), pendingIntent);
//			curMessage.close();
			Log.d(LOG_TAG, "Stop triggering message scheduler");
			Intent mesEndInt = new Intent(UVoxPlayer.BROADCAST_ACT_SCH);
			mesEndInt.putExtra(UVoxPlayer.PARAM_RESULT,
                    UVoxPlayer.STATUS_SCHEDUL_END);
			sendBroadcast(mesEndInt);
		}

	}

}
