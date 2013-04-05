package com.ultivox.uvoxplayer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import android.content.Context;
import android.os.Environment;

public class TimeLogInfo {

	private PIDHashMap pidMap;
	private String logFile;

	// конструктор класса - формирует текущий PIDHashMap а также определяет path текущего logcat файла и перенаправляет вывод логов в новый файл.
	public TimeLogInfo(Context con) {
		this.pidMap = new PIDHashMap(con);
		String logDirPath = Environment.getExternalStorageDirectory()
				.toString() + UVoxPlayer.LOGCAT_DIR;
		File logDir = new File(logDirPath);
		if (!logDir.exists()) {
			logDir.mkdir();
		}
		;
		String[] logFiles = logDir.list();
		String logName = android.text.format.DateFormat.format(
				"logyyyyMMddhhmmss.txt", new java.util.Date()).toString();
		String comandStop = "logcat -c";
		String comandStart = "logcat -v long  -f "
				+ Environment.getExternalStorageDirectory().toString()
				+ UVoxPlayer.LOGCAT_DIR + File.separator + logName;
		try {
			Runtime.getRuntime().exec(comandStop);  // очищаем буфер вывода
			Runtime.getRuntime().exec(comandStart); // направляем вывод логов в новый файл
		} catch (IOException e) {
			e.printStackTrace();
			logFile = null;
		}
		if (logFiles.length > 0) {     // удаляем все файлы кроме последнего
			Arrays.sort(logFiles);
			logFile = logDirPath + File.separator + logFiles[0];
			for (int k = 1; k < logFiles.length; k++) {
				File f = new File(logDirPath + File.separator + logFiles[k]);
				if (f.exists()) {
					f.delete();
				}
			}
		}

	}
	
	public String getLogFile () {
		return this.logFile;
	}
	
	public PIDHashMap getPIDMap () {
		return this.pidMap;
	}

}
