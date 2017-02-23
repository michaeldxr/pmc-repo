package oracle.apps.votuning;

import java.io.File;
import java.io.FilenameFilter;

public class JprFileFilter  implements FilenameFilter {
    public JprFileFilter() {
        super();
    }

    public boolean accept(File dir, String name) {
        String absPath = dir.getAbsolutePath();
        
        if(absPath.contains(".ade_path"))
            return false;
        return name.endsWith(".jpr");

    }
}
