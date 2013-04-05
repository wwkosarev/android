package com.ultivox.rebootapp;

import java.util.concurrent.TimeUnit;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.app.Activity;
import android.content.Context;
import android.util.Log;

public class MainActivity extends Activity {

	private MyTask mt;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("ReBootApp", "started");
	    mt = new MyTask();
	    mt.execute();

	}
	
	class MyTask extends AsyncTask<Void, Void, Void> {

	    @Override
	    protected void onPreExecute() {
	      super.onPreExecute();
	      Log.d("ReBootApp", "Begin");
	    }

	    @Override
	    protected Void doInBackground(Void... params) {
	      try {
	        TimeUnit.SECONDS.sleep(10);
	      } catch (InterruptedException e) {
	        e.printStackTrace();
	      }
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			pm.reboot(null);
	      return null;
	    }

	    @Override
	    protected void onPostExecute(Void result) {
	      super.onPostExecute(result);
	      Log.d("ReBootApp", "End");
	    }
	}

}
