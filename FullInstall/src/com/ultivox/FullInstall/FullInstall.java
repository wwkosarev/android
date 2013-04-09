package com.ultivox.FullInstall;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;

public class FullInstall extends Activity {

    private static final String BROADCAST_INFO = "com.ultivox.FullInstall.info";
    private static final String EXT_TEXT = "text";

    /**
     * Called when the activity is first created.
     */

    private InstallMainTask mt;
    private Activity context;
    private TextView textInfo;
    private BroadcastReceiver brInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("ReInstall", "started");
        context = this;
        setContentView(R.layout.main);
        textInfo = (TextView) findViewById(R.id.textInfo);
        textInfo.setText("Start \n");
        brInfo = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                String info = intent.getStringExtra(EXT_TEXT);
                textInfo.append( info+"\n");
            }
        };

        IntentFilter intInfo = new IntentFilter(BROADCAST_INFO);
        registerReceiver(brInfo, intInfo);
    }

    public void startInstall(View v) {
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
            Process proc1,proc2,proc3,proc4;

            String[] command1 = new String[] { "su", "-c",
                    "busybox install /mnt/extsd/Install/UVoxPlayer.apk /system/app/" };
            Log.d("ReInstall", command1[0] + " " + command1[1] + " " + command1[2]);
            String[] command2 = new String[] { "su", "-c",
                    "busybox install /mnt/extsd/Install/ReBootApp.apk /system/app/" };
            Log.d("ReInstall", command2[0] + " " + command2[1] + " " + command2[2]);
            String[] command3 = new String[] { "su", "-c",
                    "busybox install /mnt/extsd/Install/ReInstall.apk /system/app/" };
            Log.d("ReInstall", command3[0] + " " + command3[1] + " " + command3[2]);
            String[] command4 = new String[] { "su", "-c",
                    "busybox cp /mnt/extsd/Install/UVoxPref.xml /data/data/com.ultivox.uvoxplayer/shared_prefs/" };
            Log.d("ReInstall", command4[0] + " " + command4[1] + " " + command4[2]);
            String message =  command1[0] + " " + command1[1] + " " + command1[2] +  "\n";
            try {
                proc1 = Runtime.getRuntime().exec(command1);
                Integer res1 = proc1.waitFor();
                Log.d("ReInstall", "Result of reinstall : " + res1);
                message += "Result of reinstall : " + res1 + "\n" +command2[0] + " " + command2[1] + " " + command2[2] + "\n";
                proc2 = Runtime.getRuntime().exec(command2);
                Integer res2 = proc2.waitFor();
                Log.d("ReInstall", "Result of reinstall : " + res2);
                message += "Result of reinstall : " + res2 + "\n" +command3[0] + " " + command3[1] + " " + command3[2] + "\n";
                proc3 = Runtime.getRuntime().exec(command3);
                Integer res3 = proc3.waitFor();
                Log.d("ReInstall", "Result of reinstall : " + res3);
                message += "Result of reinstall : " + res3 + "\n" +command4[0] + " " + command4[1] + " " + command4[2] + "\n";
                proc4 = Runtime.getRuntime().exec(command4);
                Integer res4 = proc4.waitFor();
                Log.d("ReInstall", "Result of copy: " + res4);
                message += "Result of reinstall : " + res4  + "\n";
                Intent mesInfo = new Intent(BROADCAST_INFO);
                mesInfo.putExtra(EXT_TEXT, message);
                sendBroadcast(mesInfo);
                if ((res1 != null) && (res2 != null) && (res3 != null) && (res4 != null))
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

