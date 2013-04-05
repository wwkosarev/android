package com.ultivox.uvoxplayer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

public class SettingsTask extends AsyncTask<String, String, String> implements BackgroundTask {

	private static final String lineEnd = "\r\n";
	private static final String twoHyphens = "--";
	private static final String boundary = "***UMSDATA***";
	final String LOG_TAG = "SettingsTask";
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
	private boolean isConnected = false;

	SettingsTask(Context context) {
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
			isConnected   = true;
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
		serverMessage = null;
		publishProgress(statusLine);
		if (!isConnected) {
			return null;
		}
		try {
			URL url = new URL(UVoxPlayer.SETTINGS_SERVER);
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
			sXML.startTag("", "settings");
			sXML.attribute(
					"",
					"data",
					android.text.format.DateFormat.format(
							"yyyy-MM-dd hh:mm:ss", new java.util.Date())
							.toString());
			sXML.startTag("", "player");
			sXML.text(UVoxPlayer.UMS_NB);
			sXML.endTag("", "player");

			Map<String, ?> mapSettings = UVoxPlayer.settings.getAll();
			for (String key : mapSettings.keySet()) {
				sXML.startTag("", "data");
				String currValue = String.valueOf(mapSettings.get(key));
				sXML.startTag("", "key");
				sXML.text(key);
				sXML.endTag("", "key");
				sXML.startTag("", "value");
				sXML.text(currValue);
				sXML.endTag("", "value");
				sXML.endTag("", "data");
			}
			sXML.endTag("", "settings");
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
			Log.d(LOG_TAG, String.format("LogPLay server response %d --- %s",
					serverResponseCode, serverMessage));
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (Exception e1) {
			throw new RuntimeException(e1);
		} finally {
			connection.disconnect();
		}
		return serverMessage;
	}

	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		Log.d(LOG_TAG, "End. Result = " + result);
		publishProgress("");
		Intent mesServ = new Intent(MainService.BROADCAST_ACT_SERVER);
		if (result == null) {
			mesServ.putExtra(MainService.PARAM_RESULT, MainService.SERVER_ERROR);
			publishProgress("Connecting problem. Please try again later.");
			return;
		}
		XmlPullParserFactory factory;
		try {
			factory = XmlPullParserFactory.newInstance();
			XmlPullParser xpp = factory.newPullParser();
			xpp.setInput(new StringReader(result));
			String kv = "";
			String xkey = "";
			String xvalue = "";
			while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
				switch (xpp.getEventType()) {
				case XmlPullParser.START_DOCUMENT:
					break;
				case XmlPullParser.START_TAG:
					if (xpp.getDepth() == 3) {
						kv = xpp.getName();
					}
					break;
				case XmlPullParser.END_TAG:
					if (xpp.getDepth() == 3) {
						kv = "";
					}
					if (xpp.getDepth() == 2) {
						SharedPreferences.Editor editor = UVoxPlayer.settings.edit();
						if (xkey.equals("PREF_EXIST")) {
							editor.putBoolean(xkey, Boolean.valueOf(xvalue));
						}
						if (xkey.equals("INTERVAL_CONNECTION")) {
							editor.putLong(xkey, Long.valueOf(xvalue));
						} else  {
							editor.putString(xkey, xvalue);
						}
						editor.commit();
					}
					break;
				case XmlPullParser.TEXT:
					if (kv.equals("key")) {
						xkey = xpp.getText();
					}
					if (kv.equals("value")) {
						xvalue = xpp.getText();
					}
					break;

				default:
					break;
				}
				xpp.next();
			}
			SharedPreferences settings = tContext.getSharedPreferences(UVoxPlayer.MAIN_PREF, UVoxPlayer.MODE_PRIVATE);
			UVoxPlayer.initGlobals(settings);
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mesServ.putExtra(MainService.PARAM_RESULT, MainService.SERVER_CONTINUE);
		tContext.sendBroadcast(mesServ);

	}

	@Override
	public String runTask(String file) {

		this.execute();
		return null;
	}

	@Override
	public String getTaskName() {

		return "SettingsTask";
	}
}
