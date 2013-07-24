package com.ultivox.uvoxplayer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class PlayMusicService extends Service {

	private static final String TAG = "PlayMusicService";
	private static final String TABLE_PLAY = "music";
	private MediaPlayer mPlayer;
	private String currentPlayList;
	private static final String COL_ID = "_id";
	private static final String COL_DAY = "day";
	private static final String COL_HOUR = "hour";
	private static final String COL_TRACK = "track";
	private Cursor curPlay = null;
	private static boolean playlistNotEmpty = false;
	
	private static boolean isCreated = false;
	private static boolean isStarted = false;

	BroadcastReceiver brMusic;
	String currFileToPlay = null;

	PlayBinder binder = new PlayBinder();
	protected boolean musicEnded = false;
	static int prev = 0;
    private static PlayMusicService pms;

    @Override
	public void onCreate() {
		if (isCreated) {
			return;
		}
		isCreated = true;
		super.onCreate();
		// esPlayMusic = Executors.newFixedThreadPool(1);
		mPlayer = MediaPlayer.create(this, R.raw.everybody);
		mPlayer.setLooping(false);
		mPlayer.reset();
		brMusic = new BroadcastReceiver() {
			// действи€ при получении сообщений

			@Override
			public void onReceive(Context context, Intent intent) {
				if (mPlayer.isPlaying()) {
					mPlayer.setVolume(UVoxPlayer.volumeMusic,
							UVoxPlayer.volumeMusic);
				}
			}
		};
		IntentFilter volFilt = new IntentFilter(UVoxPlayer.BROADCAST_MUSICINFO);
		// регистрируем (включаем) BroadcastReceiver
		registerReceiver(brMusic, volFilt);
		Log.d(TAG, "Create service");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {


		pms = this;
		if (isStarted) {
			return START_NOT_STICKY;
		}
		isStarted = true;
		Log.d(TAG, "Start service");
		mPlayer.setOnCompletionListener(new OnCompletionListener() {
			public void onCompletion(MediaPlayer mp) {
				Log.d(TAG, "Song ended.");
				LogPlay.write(UVoxPlayer.LOG_COMMAND_MUSIC, currFileToPlay,
						LogPlay.STAT_END);
				mPlayer.stop();
				mPlayer.release();
				Intent mesint = new Intent(UVoxPlayer.BROADCAST_ACT_SCH);
				mesint.putExtra(UVoxPlayer.PARAM_RESULT,
                        UVoxPlayer.STATUS_PLAY_STOP);
				sendBroadcast(mesint);
			}
		});
		currentPlayList = getCurrentPlayList();
		String pathS = UVoxEnvironment.getExternalStorageDirectory().toString()
				+ UVoxPlayer.STORAGE + UVoxPlayer.PLAYLISTS + File.separator
				+ currentPlayList;
        File f = new File(pathS);
		if ((currentPlayList != null) && f.isDirectory()) {
			String filesPlayList[] = f.list();
			int nb = filesPlayList.length;
			if (nb < 4) {
                Log.d(TAG, "Less then 3 tracks to play, exit!");
                setAlarmToNextHour();
				return START_NOT_STICKY;
			}
			Random generator = new Random();
			do {
				nb = generator.nextInt(filesPlayList.length);
				Log.d(TAG, String.format("random nb: - %d", nb));
			} while (prev == nb); // not previous song
			prev = nb;
			currFileToPlay = UVoxEnvironment.getExternalStorageDirectory()
					.toString()
					+ UVoxPlayer.STORAGE
					+ UVoxPlayer.PLAYLISTS
					+ File.separator
					+ currentPlayList
					+ "/"
					+ filesPlayList[nb];
			MediaMetadataRetriever mmr = new MediaMetadataRetriever();
			mmr.setDataSource(currFileToPlay);
			// form array to broadcast song TAG
			String idTag[] = {
					mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
					mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
					mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
					mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE),
					mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION),
					mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE) };
			Intent mesPlay = new Intent(UVoxPlayer.BROADCAST_PLAYINFO);
			mesPlay.putExtra(UVoxPlayer.PLAYINFO, idTag);
			sendBroadcast(mesPlay);
			playlistNotEmpty = true;
			Log.d(TAG, "Play file: " + currFileToPlay);
			try {
				mPlayer.setDataSource(currFileToPlay);
			} catch (IllegalArgumentException e) {
				logError();
				e.printStackTrace();
			} catch (SecurityException e) {
				logError();
				e.printStackTrace();
			} catch (IllegalStateException e) {
				logError();
				e.printStackTrace();
			} catch (IOException e) {
				logError();
				e.printStackTrace();
			}
			try {
				mPlayer.prepare();
				mPlayer.setVolume(UVoxPlayer.volumeMusic,
						UVoxPlayer.volumeMusic);
				mPlayer.start();
                Intent mesConnVisual = new Intent(UVoxPlayer.BROADCAST_VOLUME);
                mesConnVisual.putExtra(UVoxPlayer.PARAM_PLAYER, mPlayer.getAudioSessionId());
                sendBroadcast(mesConnVisual);
				LogPlay.write(UVoxPlayer.LOG_COMMAND_MUSIC, currFileToPlay,
						LogPlay.STAT_BEGIN);
			} catch (IllegalStateException e) {
				logError();
				e.printStackTrace();
			} catch (IOException e) {
				logError();
				e.printStackTrace();
			}
		} else {
            Log.e(TAG, "No such playlist or playlist not schedeled");
            setAlarmToNextHour();
		}
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mPlayer.release();
		String idTag[] = {"","","","","",""};
		Intent mesPlay = new Intent(UVoxPlayer.BROADCAST_PLAYINFO);
		mesPlay.putExtra(UVoxPlayer.PLAYINFO, idTag);
		sendBroadcast(mesPlay);
        Intent mesConnVisual = new Intent(UVoxPlayer.BROADCAST_VOLUME);
        mesConnVisual.putExtra(UVoxPlayer.PARAM_PLAYER, 0);
        sendBroadcast(mesConnVisual);
		unregisterReceiver(brMusic);
		isStarted = false;
		isCreated = false;
		Log.d(TAG, "Service onDestroy");
	}

	@Override
	public IBinder onBind(Intent arg0) {
		Log.d(TAG, "Service onBind");
		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.d(TAG, "Service onUnbind");
		return true;
	}

	void fadeOut() {
		Log.d(TAG, "Fade Out");
		new Thread(new Runnable() {

			float vol = UVoxPlayer.volumeMusic;

			public void run() {
				if (playlistNotEmpty) {
					try {
						for (int i = 1; i <= 10; i++) {
							if (mPlayer.isPlaying()) {
								mPlayer.setVolume(vol * (10 - i) / 10, vol
										* (10 - i) / 10);
							} else {
								break;
							}
							try {
								TimeUnit.MILLISECONDS.sleep(200);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						if (mPlayer.isPlaying()) {
							mPlayer.pause();
						} else {
							musicEnded = true;
						}
					} catch (IllegalStateException e) {
						Log.d(TAG, "Song ended in FadeOut");
						musicEnded = true;
						e.printStackTrace();
					}
				}
				Intent mesint = new Intent(UVoxPlayer.BROADCAST_ACT_SCH);
				mesint.putExtra(UVoxPlayer.PARAM_RESULT,
                        UVoxPlayer.STATUS_FADEOUT);
				sendBroadcast(mesint);
				Log.d(TAG, "FadeOut intent sended");
			}
		}).start();
	}

	void fadeIn() {
		Log.d(TAG, "Fade In");
		new Thread(new Runnable() {

			float vol = UVoxPlayer.volumeMusic;

			public void run() {
				if (playlistNotEmpty && !musicEnded) {
					try {
						mPlayer.start();
						for (int i = 1; i <= 10; i++) {
							if (mPlayer.isPlaying()) {
								mPlayer.setVolume(vol * i / 10, vol * i / 10);
							} else {
								break;
							}
							try {
								TimeUnit.MILLISECONDS.sleep(200);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					} catch (IllegalStateException e) {
						Log.d(TAG, "Probably song ended in FadeIn");
						e.printStackTrace();
					}
				}
				Intent mesint = new Intent(UVoxPlayer.BROADCAST_ACT_SCH);
				mesint.putExtra(UVoxPlayer.PARAM_RESULT,
                        UVoxPlayer.STATUS_FADEIN);
				sendBroadcast(mesint);
				musicEnded = false;
				Log.d(TAG, "FadeIn intent sended");
			}
		}).start();
	}

	public String getCurrentPlayList() {
		String curPlayList = null;
		;
		// переменные дл€ query
		String[] columns = null;
		String selection = null;
		String[] selectionArgs = null;

		Calendar upTime; // “екущее врем€
		Calendar playTime; // ¬рем€ текущего плей-листа

		upTime = Calendar.getInstance();
		playTime = Calendar.getInstance();

		int currentDayWeek = upTime.get(Calendar.DAY_OF_WEEK);
		int currentHour = upTime.get(Calendar.HOUR_OF_DAY);
		Log.d(TAG, String.format("day - %d, hour - %d", currentDayWeek,
				currentHour));

		playTime.setTimeInMillis(0);
		playTime.setFirstDayOfWeek(currentDayWeek);
		playTime.set(Calendar.HOUR_OF_DAY, currentHour);
		columns = new String[] { COL_ID, COL_DAY, COL_HOUR, COL_TRACK };
		selection = COL_DAY + " = ? AND " + COL_HOUR + " = ?";
		selectionArgs = new String[] { String.format("%d", currentDayWeek),
				String.format("%d", currentHour) };
		curPlay = UVoxPlayer.currDb.query(TABLE_PLAY, columns, selection,
				selectionArgs, null, null, null, null);
		if (curPlay.moveToFirst()) {
			curPlayList = curPlay.getString(curPlay.getColumnIndex(COL_TRACK));
			Log.d(TAG, String.format("day - %d, hour - %d, track - %s ",
					curPlay.getInt(curPlay.getColumnIndex(COL_DAY)),
					curPlay.getInt(curPlay.getColumnIndex(COL_HOUR)),
					curPlayList));
		} else {
			curPlayList = null;
			Log.d(TAG, "Have no data to play");
		}
		curPlay.close();
		return curPlayList;
	}

	class PlayBinder extends Binder {
		PlayMusicService getService() {
			return pms;
		}
	}

	void logError() {
		LogPlay.write(UVoxPlayer.LOG_COMMAND_MUSIC, currFileToPlay,
				LogPlay.STAT_ERROR);
	}

    void setAlarmToNextHour () {
        playlistNotEmpty = false;
        Intent mesint = new Intent(UVoxPlayer.BROADCAST_ACT_SCH);
        mesint.putExtra(UVoxPlayer.PARAM_RESULT,
                UVoxPlayer.STATUS_PLAYLIST_EMPTY);
        sendBroadcast(mesint);
    }

}
