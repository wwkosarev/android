package com.ultivox.uvoxplayer;

import java.util.HashMap;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;

public class PIDHashMap extends HashMap<String, Integer> {
	
	private static final long serialVersionUID = 19640807L;

	public PIDHashMap(Context con) {
		
		ActivityManager am = (ActivityManager) con.getSystemService( Context.ACTIVITY_SERVICE );
		List<RunningAppProcessInfo> procInfos = am.getRunningAppProcesses();
		this.clear();
		for (int i=0; i<procInfos.size(); i++) {
			this.put(procInfos.get(i).processName,procInfos.get(i).pid);
		}
	}
	
	public int getPID(String appName) {
		return this.get(appName);
	}
	
	public String getAppName(int pid) {
		String name = "";
		if (this.containsValue(pid)) {
			for (Entry<String, Integer> entry : this.entrySet()) {
				if (entry.getValue()==pid) {
					name = entry.getKey();
				}
			}
		}
		return name;
	}

}
