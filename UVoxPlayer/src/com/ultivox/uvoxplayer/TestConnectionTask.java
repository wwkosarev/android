package com.ultivox.uvoxplayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

public class TestConnectionTask extends AsyncTask<String, String, String> implements BackgroundTask {

	final String LOG_TAG = "TestConnectionTaskLogs";
	private String statusLine = "";
	static boolean isConnected = false;
	private Context tContext;

	TestConnectionTask(Context context) {
		tContext = context;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		Log.d(LOG_TAG, "Start connection task");
		ConnectivityManager cm = (ConnectivityManager) tContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfoEth = cm.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
		NetworkInfo netInfoWifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if ((netInfoEth != null || netInfoWifi != null)
				&& (netInfoEth.isConnected() || netInfoWifi.isConnected())) {
			isConnected = true;
			statusLine = "Connecting.....";
		} else {
			statusLine = "";
			if (netInfoEth == null) {
				Log.d(LOG_TAG, "Ethernet switched off");
				statusLine = statusLine + "Ethernet switched off/";
			}
			if (netInfoWifi == null) {
				Log.d(LOG_TAG, "WiFi switched off");
				statusLine = statusLine + "WiFi switched off/";
			}
			if (!netInfoEth.isConnected()) {
				Log.d(LOG_TAG, "Ethernet not connected");
				statusLine = statusLine + "Ethernet not connected/";
			}
			if (!netInfoWifi.isConnected()) {
				Log.d(LOG_TAG, "WiFi not connected");
				statusLine = statusLine + "WiFi not connected/";
			}
		}
	}

	@Override
	protected void onProgressUpdate(String... progress) {

		Intent mesService = new Intent(UVoxPlayer.BROADCAST_SHOW);
		mesService.putExtra(UVoxPlayer.PARAM_TEXT, progress[0]);
		tContext.sendBroadcast(mesService);
	}

	@Override
	protected String doInBackground(String... u) {
		
		String url = UVoxPlayer.HOME_URL + UVoxPlayer.TEST_CONN_URL;
		Log.d(LOG_TAG, "Background job started");
		publishProgress(statusLine);
		if (isConnected) {
			HttpClient httpClient = CustomHttpClient.getHttpClient();
			try {
				HttpGet request = new HttpGet(url);
				HttpParams params = new BasicHttpParams();
				HttpConnectionParams.setSoTimeout(params, 10000); // 30 sec
				request.setParams(params);
				HttpResponse response = httpClient.execute(request);
				if (HttpStatus.SC_OK == response.getStatusLine()
						.getStatusCode()) {
					Log.d(LOG_TAG, convertStreamToString(response.getEntity()
							.getContent()));
					publishProgress("Connection OK");
				} else {
					publishProgress("Wrong server response");
				}
				TimeUnit.SECONDS.sleep(2);
				publishProgress("");
				return response.getEntity().toString();
			} catch (IOException e) {
				publishProgress("Server not connected");
				e.printStackTrace();
			} catch (InterruptedException e) {
				publishProgress("Server not connected");
				e.printStackTrace();
			}
		} else {

		}
		return null;
	}

	@Override
	protected void onPostExecute(String result) {
		if (result != null) {

		} else {
			Intent mesService = new Intent(UVoxPlayer.BROADCAST_SHOW);
			mesService.putExtra(UVoxPlayer.PARAM_TEXT, "Connecting problem. Please try again later.");
			tContext.sendBroadcast(mesService);
		}
	}

	public static String convertStreamToString(InputStream is) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	@Override
	public String runTask(String file) {
		
		this.execute();
		return null;
	}
	
	@Override
	public String getTaskName() {

		return "TestConnectionTask";
	}

}
