package com.ultivox.uvoxplayer;

import android.os.Environment;
import java.io.File;

/**
 * Created by wwk on 03.07.13.
 */
public class UVoxEnvironment {


    public static File getExternalStorageDirectory() {

        SysInfo sys = new SysInfo();
        File card = new File("/mnt/extsd");
        if (sys.getExtMemFullLong()<sys.getIntMemFullLong()) {
            return Environment.getExternalStorageDirectory();
        } else {
            return card;
        }

    }
}
