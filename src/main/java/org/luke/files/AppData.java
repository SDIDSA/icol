package org.luke.files;

import java.io.File;
import java.util.Random;

public class AppData {
    private static final File root = new File(System.getenv("appData") + "\\icol");

    static {
        if(!root.exists()) {
            if(!root.mkdir()) {
                throw new RuntimeException("initialisation failure...");
            }
        }
    }

    private static final Random r = new Random();
    public static File usableIcon(File tempIcon) {
        String preName = tempIcon.getName().replace(".ico", "");
        File res = new File(root, preName + "__" + r.nextInt(999) + ".ico");
        File[] files = res.getParentFile().listFiles();
        assert files != null;
        for(File f : files) {
            if(f.getName().toLowerCase().split("__")[0].equals(preName.toLowerCase())) {
                f.deleteOnExit();
            }
        }
        if(res.exists() && !res.delete()) {
            throw new RuntimeException("failed to create icon file");
        }
        if(tempIcon.renameTo(res)) {
            return res;
        }
        throw new RuntimeException("failed to create icon file");
    }
}
