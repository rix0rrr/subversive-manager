
package nl.rix0r.subversive.server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Class responsible accessing repositories on disk
 *
 * Lists the available repositories and possibly looks inside them
 * for directory information.
 */
public class RepositoryLister {
    private final File directory;

    public RepositoryLister(File directory) {
        if (!directory.exists()) throw new RuntimeException("No such repository directory: " + directory);
        if (!directory.isDirectory()) throw new RuntimeException("No such repository directory: " + directory);
        if (!directory.canRead()) throw new RuntimeException("Repository directory not readable: " + directory);
        this.directory = directory;
    }

    public List<String> allRepositories() {
        String[] dirs = directory.list();
        List<String> ret = new ArrayList<String>(dirs.length);

        for (String dir: dirs)
            if (!dir.startsWith(".") && new File(directory, dir).isDirectory() && new File(new File(directory, dir), "format").exists())
                ret.add(dir);

        return ret;
    }

}
