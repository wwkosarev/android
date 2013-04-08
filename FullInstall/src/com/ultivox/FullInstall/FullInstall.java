package com.ultivox.FullInstall;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

public class FullInstall extends Activity {
    /**
     * Called when the activity is first created.
     */

    private InstallMainTask mt;
    private Activity context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("ReInstall", "started");
        context = this;
        mt = new InstallMainTask();
        mt.execute();

    }

    class InstallMainTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d("ReInstall", "Begin task");
        }

        @Override
        protected Integer doInBackground(Void... params) {
            Process proc1,proc2,proc3;

            String[] command1 = new String[] { "su", "-c",
                    "busybox install /mnt/extsd/Install/UVoxPlayer.apk /system/app/" };
            Log.d("ReInstall", command1[0] + " " + command1[1] + " " + command1[2]);
            String[] command2 = new String[] { "su", "-c",
                    "busybox install /mnt/extsd/Install/ReBootApp.apk /system/app/" };
            Log.d("ReInstall", command2[0] + " " + command2[1] + " " + command2[2]);
            String[] command3 = new String[] { "su", "-c",
                    "busybox install /mnt/extsd/Install/ReInstall.apk /system/app/" };
            Log.d("ReInstall", command3[0] + " " + command3[1] + " " + command3[2]);
            try {
                proc1 = Runtime.getRuntime().exec(command1);
                Integer res1 = proc1.waitFor();
                Log.d("ReInstall", "Result of reinstall : " + res1);
                proc2 = Runtime.getRuntime().exec(command2);
                Integer res2 = proc2.waitFor();
                Log.d("ReInstall", "Result of reinstall : " + res2);
                proc3 = Runtime.getRuntime().exec(command3);
                Integer res3 = proc3.waitFor();
                Log.d("ReInstall", "Result of reinstall : " + res3);
                if ((res1 != null) && (res2 != null) && (res3 != null))
                    return 1;
                else
                    return null;
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
            Log.d("ReInstall", "End");
        }
    }
}

