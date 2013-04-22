package com.ultivox.uvoxplayer;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: wwk
 * Date: 11.04.13
 * Time: 21:58
 * To change this template use File | Settings | File Templates.
 */
public class RemoveLauncherTask extends AsyncTask<String, String, String> implements BackgroundTask {

    final static String LOG_TAG = "RemoveLauncherTask";
    private final Context uContext;

    RemoveLauncherTask(Context context) {
        uContext = context;
    }

    @Override
    protected void onProgressUpdate(String... progress) {

    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... params) {

        String[] command = null;
        boolean com = false;
        String output = null;
        if (params[0].equals("remove")) {
            command = new String[]{"su", "-c",
                    "busybox mv /system/app/" + UVoxPlayer.reMoveFile + " /mnt/sdcard/Download/"};
            output = "Result of removing Launcher: ";
            com = true;
        }
        if (params[0].equals("install")) {
            command = new String[]{"su", "-c",
                    "busybox install /mnt/sdcard/Download/" + UVoxPlayer.reMoveFile + " /system/app/"};
            output = "Result of reinstall : ";
            com = true;
        }
        if (com) {
            Log.d(LOG_TAG, command[0] + " " + command[1] + " " + command[2]);
            Process reInsProc;
            try {
                reInsProc = Runtime.getRuntime().exec(command);
                int res = reInsProc.waitFor();
                Log.d(LOG_TAG, output + res);
                if (res == 0) {
                    return "Ok";
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {

        super.onPostExecute(result);
     }

    @Override
    public String runTask(String com) {

        this.execute(com);
        return null;
    }

    @Override
    public String getTaskName() {

        return "RemoveLauncherTask";
    }
}
