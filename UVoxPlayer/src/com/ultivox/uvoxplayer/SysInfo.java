package com.ultivox.uvoxplayer;

import android.os.Environment;
import android.os.StatFs;
import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: wwk
 * Date: 06.04.13
 * Time: 9:41
 */
public class SysInfo {

    public String getMemFree() {

        File path = Environment.getDataDirectory();
        StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
        long freeKb  = (statFs.getAvailableBlocks() *  (long) statFs.getBlockSize()) / 1048576;
        return String.format("Free main memory %d MB", freeKb);
    }

    public String getIntMemFree() {

        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long freeKb  = (statFs.getAvailableBlocks() *  (long) statFs.getBlockSize()) / 1048576;
            return String.format("Free  internal memory %d MB", freeKb);
        }
        return "SD not mounted";
    }
}
