package com.ultivox.uvoxplayer;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class DownloadTask extends AsyncTask<String, String, String> implements
		BackgroundTask {

	public final static String RECORD_TAG = "record";
	final String LOG_TAG = "DownloadTaskLogs";
	private String statusLine = "";
	private static boolean isConnected = false;
	private Context tContext;
	private String dlInstruction = null;
	private String dlComInstruction = null;
	private String downloadURL = null;
	private String downloadTo = null;
	private String rqstString = null;
	private String rqstStringCom = null;
	private String playDir = null;
	private String messDir = null;

	String tmp = "";
//	private int countSongs;
	private String colName;
	// private String dbName;
	private String currDbName;
	private HttpResponse response;
	ContentValues row = new ContentValues();
	private String group;
	private String root;
	private String action;
	private String subj;
	private URL url;

	DownloadTask(Context context) {
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

		Log.d(LOG_TAG, "Background job started");

		rqstString = UVoxPlayer.CONFIG_URL + UVoxPlayer.DBCONNECT
				+ UVoxPlayer.UMS_NB;
		rqstStringCom = UVoxPlayer.CONFIG_URL + UVoxPlayer.COMCONNECT
				+ UVoxPlayer.UMS_NB;
		downloadURL = UVoxPlayer.HOME_URL;
		downloadTo = Environment.getExternalStorageDirectory().toString()
				+ UVoxPlayer.STORAGE + File.separator;
		playDir = UVoxPlayer.PLAYLISTS;
		messDir = UVoxPlayer.MESSAGES;
		Log.d(LOG_TAG,
				String.format(
						"DB req: %s COM req: %s Download base: %s Reciever base: %s Playlist dir: %s Message dir %s",
						rqstString, rqstStringCom, downloadURL, downloadTo,
						playDir, messDir));
		publishProgress(statusLine);
		if (isConnected) {
			HttpClient httpClient = CustomHttpClient.getHttpClient();
			try { // Updating database
				Log.d(LOG_TAG, rqstString);
				HttpGet request = new HttpGet(rqstString);
				HttpParams params = new BasicHttpParams();
				HttpConnectionParams.setSoTimeout(params, 10000); // 10 sec
				request.setParams(params);
				response = httpClient.execute(request);
				if (HttpStatus.SC_OK == response.getStatusLine()
						.getStatusCode()) {
					dlInstruction = convertStreamToString(response.getEntity()
							.getContent());
					Log.d(LOG_TAG, "Get XML instructions");
					publishProgress("Connection OK");
				} else {
					publishProgress("Wrong server response");
				}
				TimeUnit.SECONDS.sleep(1);
				statusLine = "Connected";
				publishProgress(statusLine);
				try {
					XmlPullParserFactory factory = XmlPullParserFactory
							.newInstance();
					XmlPullParser xpp = factory.newPullParser();
					xpp.setInput(new StringReader(dlInstruction));

					while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
						switch (xpp.getEventType()) {
						case XmlPullParser.START_DOCUMENT:
							break;
						case XmlPullParser.START_TAG:
							tmp = "";
							for (int i = 0; i < xpp.getAttributeCount(); i++) {
								tmp = tmp + xpp.getAttributeName(i) + " = "
										+ xpp.getAttributeValue(i) + ", ";
							}
							switch (xpp.getDepth()) {
							case 1:
								break;
							case 2:
								currDbName = xpp.getName();
								UVoxPlayer.currDb
										.delete(currDbName, null, null);
								// TODO change to var
								publishProgress(String.format(
										"Updating  /%s/  table", currDbName));
								break;
							// open database

							case 3:
								// start to form new row of db
								row.clear();
								break;
							case 4:
								// save column name
								colName = xpp.getName();
								break;
							}
							break;
						case XmlPullParser.END_TAG:
							switch (xpp.getDepth()) {
							case 1:
								// NetPlayer.currDb.close();
								break;
							case 2:
								break;
							case 3:
								// insert row
								UVoxPlayer.currDb.insert(currDbName, null, row);
								// TODO change to var
								break;
							case 4:
								break;
							}
							break;
						case XmlPullParser.TEXT:
							switch (xpp.getDepth()) {
							case 2:
								break;
							case 3:
								break;
							case 4:
								// append one column to row
								row.put(colName, xpp.getText());
								break;
							}
							break;

						default:
							break;
						}
						xpp.next();
					}
					Log.d(LOG_TAG, "End Playlist connection");
					publishProgress("");

				} catch (XmlPullParserException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

			} catch (IOException e) {
				publishProgress("Server not connected");
				e.printStackTrace();
			} catch (InterruptedException e) {
				publishProgress("Server not connected");
				e.printStackTrace();
			}
			try { // Updating file system
				Log.d(LOG_TAG, rqstStringCom);
				HttpGet request = new HttpGet(rqstStringCom);
				HttpParams params = new BasicHttpParams();
				HttpConnectionParams.setSoTimeout(params, 5000); // 10 sec
				request.setParams(params);
				response = httpClient.execute(request);
				if (HttpStatus.SC_OK == response.getStatusLine()
						.getStatusCode()) {
					dlComInstruction = convertStreamToString(response
							.getEntity().getContent());
					Log.d(LOG_TAG, "Get XML instructions");
					publishProgress("Connection OK");
				} else {
					publishProgress("Wrong server response");
				}
				TimeUnit.SECONDS.sleep(1);
				publishProgress("");
				try {
					XmlPullParserFactory factory = XmlPullParserFactory
							.newInstance();
					XmlPullParser xpp = factory.newPullParser();
					xpp.setInput(new StringReader(dlComInstruction));

					while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
						switch (xpp.getEventType()) {
						case XmlPullParser.START_DOCUMENT:
							break;
						case XmlPullParser.START_TAG:
							switch (xpp.getDepth()) {
							case 1:
								root = xpp.getName();
								break;
							case 2:
								group = xpp.getName();
								break;
							case 3:
								action = xpp.getName();
								break;
							case 4:
								subj = xpp.getName();
								break;
							}
							break;
						case XmlPullParser.END_TAG:
							switch (xpp.getDepth()) {
							case 1:
								break;
							case 2:
								break;
							case 3:
								break;
							case 4:
								break;
							}
							break;
						case XmlPullParser.TEXT:
							if (xpp.getDepth() == 4) {
								if (group.equals("playlists")) {
									if (action.equals("create")) {
										if (subj.equals("dir")) {
											File cDir = new File(downloadTo
													+ group + File.separator
													+ xpp.getText());
											if (!cDir.exists()
													&& !cDir.isDirectory()) {
												cDir.mkdirs();
											}
										}
									}
									if (action.equals("download")) {
										if (subj.equals("file")) {
											File cFile = new File(downloadTo
													+ group + File.separator
													+ xpp.getText());
											if (!cFile.isDirectory()) {
												donwloadFileBinary(downloadURL
														+ group, downloadTo
														+ group, File.separator
														+ xpp.getText());
											}
										}
									}
									if (action.equals("delete")) {
										if (subj.equals("dir")) {
											File cDir = new File(downloadTo
													+ group + File.separator
													+ xpp.getText());
											if (cDir.exists()
													&& cDir.isDirectory()) {
												String[] files = cDir
														.list(null);
												if ((files.length == 0)
														|| (files == null)) {
													cDir.delete();
												}
											}
										}
										if (subj.equals("file")) {
											File cFile = new File(downloadTo
													+ group + File.separator
													+ xpp.getText());
											if (cFile.exists()
													&& !cFile.isDirectory()) {
												cFile.delete();
											}
										}
									}
								}
								if (group.equals("message")) {
									if (action.equals("download")) {
										if (subj.equals("file")) {
											File cFile = new File(downloadTo
													+ group + File.separator
													+ xpp.getText());
											if (!cFile.isDirectory()) {
												donwloadFileBinary(downloadURL
														+ group, downloadTo
														+ group, File.separator
														+ xpp.getText());
											}
										}
									}
									if (action.equals("delete")) {
										if (subj.equals("file")) {
											File cFile = new File(downloadTo
													+ group + File.separator
													+ xpp.getText());
											if (cFile.exists()
													&& !cFile.isDirectory()) {
												cFile.delete();
											}
										}
									}
								}
							}
							break;

						default:
							break;
						}
						xpp.next();
					}
					Log.d(LOG_TAG, "End Message connection");
				} catch (XmlPullParserException e) {
					e.printStackTrace();
					return null;
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}

			} catch (IOException e) {
				publishProgress("Server not connected");
				e.printStackTrace();
				return null;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		} else {
			publishProgress("Playlists can't be downloaded, connection not established");
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return null;
		}
		return response.getEntity().toString();
	}

	@Override
	protected void onPostExecute(String result) {

		super.onPostExecute(result);
		Intent mesServ = new Intent(UVoxPlayer.BROADCAST_ACT_SERVER);
		if (result != null) {
			mesServ.putExtra(UVoxPlayer.PARAM_RESULT, UVoxPlayer.SERVER_CONTINUE);
			publishProgress("");
		} else {
			mesServ.putExtra(UVoxPlayer.PARAM_RESULT, UVoxPlayer.SERVER_ERROR);
			publishProgress("Connecting problem. Please try again later.");
		}
		tContext.sendBroadcast(mesServ);
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

	private boolean donwloadFileBinary(String sourcedir, String targetdir,
			String targetfile) {
		try {
			publishProgress(String.format("%s --- try to download %s file.",
					statusLine, targetfile));
			int i = targetfile.lastIndexOf(File.separator);
			if (i < 0) {
				url = new URL(sourcedir + File.separator
						+ URLParamEncoder.encode(targetfile));
			} else {
				String dir = targetfile.substring(0, i);
				String file = targetfile.substring(i + 1);
				url = new URL(sourcedir + dir + File.separator
						+ URLParamEncoder.encode(file));
			}
			File outputFile = new File(targetdir + targetfile);
			if (outputFile.exists()) {
				return true;
			}
			HttpURLConnection c = (HttpURLConnection) url.openConnection();
			c.setRequestMethod("GET");
			c.setDoOutput(true);
			c.connect();
			FileOutputStream fos = new FileOutputStream(outputFile);
			InputStream is = c.getInputStream();
			byte[] buffer = new byte[1024 * 16];
			int len1 = 0;
			int count = 0;
			while ((len1 = is.read(buffer)) != -1) {
				fos.write(buffer, 0, len1);
				count++;
				publishProgress(String.format(
						"%s --- %d Kb of %s file downloaded.", statusLine,
						count, targetfile));
			}
			fos.close();
			is.close();
			c.disconnect();
		} catch (Exception e) {
			Log.e(LOG_TAG, "Download error! " + e.getMessage());
			File outputFile = new File(targetdir + targetfile);
			outputFile.delete();
			return false;
		}
		return true;
	}

	private int getFileSize(URL url) {
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("HEAD");
			conn.getInputStream();
			return conn.getContentLength();
		} catch (IOException e) {
			return -1;
		} finally {
			conn.disconnect();
		}
	}

	@Override
	public String runTask(String file) {

		this.execute();
		return null;
	}

	@Override
	public String getTaskName() {

		return "DownloadTask";
	}
}
