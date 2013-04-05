package com.ultivox.reinstall;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MainActivity extends Activity {

	private MyTask mt;
	private Activity context;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("ReInstall", "started");
		context = this;
		mt = new MyTask();
		mt.execute();

	}

	class MyTask extends AsyncTask<Void, Void, Integer> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			Log.d("ReInstall", "Begin");
		}

		@Override
		protected Integer doInBackground(Void... params) {
			String[] command = new String[] { "su", "-c",
					"busybox install /mnt/sdcard/Download/UVoxPlayer.apk /system/app/" };
			Log.d("ReInstall", command[0] + " " + command[1] + " " + command[2]);
			Process reInsProc;
			try {
				reInsProc = Runtime.getRuntime().exec(command);
				int res = reInsProc.waitFor();
				Log.d("ReInstall", "Result of reinstall : " + res);
				return res;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			if (result == null) {
				return;
			}
			Intent reboot = new Intent();
			reboot.setClassName("com.ultivox.rebootapp", "com.ultivox.rebootapp.MainActivity");
			reboot.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(reboot);
			Log.d("ReInstall", "End");
		}
	}

}
