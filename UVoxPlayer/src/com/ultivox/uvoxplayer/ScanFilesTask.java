package com.ultivox.uvoxplayer;

import java.io.File;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.TextView;

public class ScanFilesTask extends AsyncTask<String, String, String> {
	
	final String LOG_TAG = "ScanFilesTaskLogs";
	private String statusLine = "";
	private Context jContext;
	private String filesArray[][];
	private int nb[];
	String currFile;

	ScanFilesTask(Context context) {
		jContext = context;
	}
	
	@Override
	protected void onProgressUpdate(String... progress) {
		TextView statusText = (TextView) ((Activity) jContext)
				.findViewById(R.id.textService);
		statusText.setText(progress[0]);
	}
	
	@Override
	protected String doInBackground(String... path) {
		
		filesArray[0] = new File(path[0]).list();
		int i = 0;
		nb[i] = filesArray[0].length;
		int k=0;
		currFile = path[0]+"/"+filesArray[i][k];
		publishProgress(statusLine);

		return null;
	}
	
	@Override
	protected void onPostExecute(String result) {
		statusLine = "";
		publishProgress(statusLine);
	}

}
