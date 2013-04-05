package com.ultivox.uvoxplayer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import org.xmlpull.v1.XmlSerializer;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

public class LogPlayTask extends AsyncTask<String, String, String> implements
		BackgroundTask {

	private static final String lineEnd = "\r\n";
	private static final String twoHyphens = "--";
	private static final String boundary = "***UMSDATA***";
	final String LOG_TAG = "LogPlayTask";
	private Context tContext;
	int bytesRead, bytesAvailable, bufferSize;
	byte[] buffer;
	int maxBufferSize = 1 * 1024 * 1024;

	HttpURLConnection connection = null;
	DataOutputStream outputStream = null;
	DataInputStream inputStream = null;
	private String statusLine = "";
	String serverResponse;
	String serverMessage;
	String[] columns = null;
	Cursor curLogPlay = null;
	private boolean isConnected = false;;

	LogPlayTask(Context context) {
		tContext = context;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		Log.d(LOG_TAG, "Start connection task");
		ConnectivityManager cm = (ConnectivityManager) tContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfoEth = cm
				.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
		NetworkInfo netInfoWifi = cm
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if ((netInfoEth != null || netInfoWifi != null)
				&& (netInfoEth.isConnected() || netInfoWifi.isConnected())) {
			statusLine = "Connecting.....";
			isConnected = true;
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
	protected String doInBackground(String... params) {

		serverResponse = null;
		publishProgress(statusLine);
		if (!isConnected) {
			return null;
		}
		try {
			URL url = new URL(UVoxPlayer.LOGPLAY_SERVER);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Connection", "Keep-Alive");
			connection.setRequestProperty("Content-Type",
					"multipart/form-data; boundary=" + boundary);

			outputStream = new DataOutputStream(connection.getOutputStream());
			outputStream.writeBytes(twoHyphens + boundary + lineEnd);
			outputStream
					.writeBytes("Content-Disposition: form-data; name=\"xmldata\""
							+ lineEnd);
			outputStream.writeBytes(lineEnd);

			XmlSerializer sXML = Xml.newSerializer();
			sXML.setOutput(outputStream, "UTF-8");
			sXML.startDocument("UTF-8", true);
			sXML.startTag("", "logplayinfo");
			sXML.attribute(
					"",
					"data",
					android.text.format.DateFormat.format(
							"yyyy-MM-dd hh:mm:ss", new java.util.Date())
							.toString());
			sXML.startTag("", "player");
			sXML.text(UVoxPlayer.UMS_NB);
			sXML.endTag("", "player");
			columns = new String[] { NetDbHelper.LOG_ID, NetDbHelper.LOG_DATE,
					NetDbHelper.LOG_COMAND, NetDbHelper.LOG_FILE,
					NetDbHelper.LOG_STATUS };
			curLogPlay = UVoxPlayer.currDb.query(NetDbHelper.TABLE_LOG,
					columns, null, null, null, null, null);
			if (curLogPlay.moveToFirst()) {
				int idDate = curLogPlay.getColumnIndex(NetDbHelper.LOG_DATE);
				int idCommand = curLogPlay
						.getColumnIndex(NetDbHelper.LOG_COMAND);
				int idFile = curLogPlay.getColumnIndex(NetDbHelper.LOG_FILE);
				int idStat = curLogPlay.getColumnIndex(NetDbHelper.LOG_STATUS);
				do {

					sXML.startTag("", "event");
					sXML.startTag("", "date");
					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(curLogPlay.getLong(idDate));
					Date date = cal.getTime();
					SimpleDateFormat format = new SimpleDateFormat(
							"yyyy-MM-dd HH:mm:ss");
					sXML.text(format.format(date));
					sXML.endTag("", "date");
					sXML.startTag("", "command");
					sXML.text(curLogPlay.getString(idCommand));
					sXML.endTag("", "command");
					sXML.startTag("", "file");
					sXML.text(curLogPlay.getString(idFile));
					sXML.endTag("", "file");
					sXML.startTag("", "stat");
					sXML.text(curLogPlay.getString(idStat));
					sXML.endTag("", "stat");
					sXML.endTag("", "event");
				} while (curLogPlay.moveToNext());
			}
			sXML.endTag("", "logplayinfo");
			sXML.endDocument();
			outputStream.writeBytes(lineEnd);
			outputStream.writeBytes(twoHyphens + boundary + twoHyphens
					+ lineEnd);
			outputStream.writeBytes(lineEnd);
			outputStream.flush();
			outputStream.close();

			// и получаем ответ от сервера

			BufferedReader in = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));

			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = in.readLine()) != null) {
				sb.append(line + '\n');
			}

			int serverResponseCode = connection.getResponseCode();
			serverResponse = connection.getResponseMessage();
			serverMessage = sb.toString();
			in.close();
			sb = null;
			if (serverResponseCode == 200) {
				UVoxPlayer.currDb.delete(NetDbHelper.TABLE_LOG, null, null);
			}
			Log.d(LOG_TAG, String.format("LogPLay server response %d --- %s",
					serverResponseCode, serverMessage));
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (Exception e1) {
			e1.printStackTrace();
		} finally {
			connection.disconnect();
		}
		return serverResponse;
	}

	@Override
	protected void onPostExecute(String result) {

		super.onPostExecute(result);
		Intent mesServ = new Intent(MainService.BROADCAST_ACT_SERVER);
		if (result != null) {
			mesServ.putExtra(MainService.PARAM_RESULT,
					MainService.SERVER_CONTINUE);
			publishProgress("");
		} else {
			mesServ.putExtra(MainService.PARAM_RESULT, MainService.SERVER_ERROR);
			publishProgress("Connecting problem. Please try again later.");
		}
		tContext.sendBroadcast(mesServ);
		Log.d(LOG_TAG, "End. Result = " + result);
		publishProgress("");
	}

	@Override
	public String runTask(String file) {

		this.execute();
		return null;
	}

	@Override
	public String getTaskName() {

		return "LogPlayTask";
	}
}
