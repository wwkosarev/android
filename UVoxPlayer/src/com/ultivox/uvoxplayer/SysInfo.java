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

    public String getMainMemFree() {

        StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
        long freeMb  = (statFs.getAvailableBlocks() *  (long) statFs.getBlockSize()) / 1048576;
        long fullMb  = (statFs.getBlockCount() *  (long) statFs.getBlockSize()) / 1048576;
        return String.format("Free main memory %d MB of %d", freeMb, fullMb);
    }

    public long getMainMemFreeLong() {

        StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
        long freeMb  = (statFs.getAvailableBlocks() *  (long) statFs.getBlockSize()) / 1048576;
        return freeMb;
    }

    public long getMainMemFullLong() {

        StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
        long fullMb  = (statFs.getBlockCount() *  (long) statFs.getBlockSize()) / 1048576;
        return fullMb;
    }



    public String getIntMemFree() {

        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long freeMb  = (statFs.getAvailableBlocks() *  (long) statFs.getBlockSize()) / 1048576;
            long fullMb  = (statFs.getBlockCount() *  (long) statFs.getBlockSize()) / 1048576;
            return String.format("Free internal memory %d MB of %d", freeMb, fullMb);
        }
        return "SD not mounted";
    }

    public long getIntMemFullLong() {

        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long fullMb  = (statFs.getBlockCount() *  (long) statFs.getBlockSize()) / 1048576;
        return fullMb;
    }


    public String getExtMemFree() {

        String state = Environment.getExternalStorageState();       //TODO wrong!!!
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            StatFs statFs = new StatFs("/mnt/extsd");
            long freeMb  = (statFs.getAvailableBlocks() *  (long) statFs.getBlockSize()) / 1048576;
            long fullMb  = (statFs.getBlockCount() *  (long) statFs.getBlockSize()) / 1048576;
            return String.format("Free internal memory %d MB of %d", freeMb, fullMb);
        }
        return "SD not mounted";
    }

    public long getExtMemFullLong() {

        StatFs statFs = new StatFs("/mnt/extsd");
        long fullMb  = (statFs.getBlockCount() *  (long) statFs.getBlockSize()) / 1048576;
        return fullMb;
    }
}
