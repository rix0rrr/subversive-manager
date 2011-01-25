
package nl.rix0r.subversive.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import nl.rix0r.subversive.subversion.Directory;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang.StringUtils;

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
            if (!dir.startsWith(".") && repositoryDir(dir).isDirectory() && new File(repositoryDir(dir), "format").exists())
                ret.add(dir);

        return ret;
    }

    private File repositoryDir(String repositoryName) {
        return new File(directory, repositoryName);
    }

    /**
     * Return a list of all directories in the given repository
     *
     * Uses the 'svnlook' command to obtain this list.
     */
    public List<Directory> listDirectories(final String repository) {
        List<Directory> ret = new ArrayList<Directory>();

        try {
            CommandLine cl = CommandLine.parse("svnlook tree --full-paths ${repo}", new HashMap() {{
                put("repo", repositoryDir(repository));
            }});

            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            DefaultExecutor executor = new DefaultExecutor();
            executor.setStreamHandler(new PumpStreamHandler(stdout, stderr));
            int exitCode = executor.execute(cl);

            if (exitCode != 0)
                throw new RuntimeException("Failed to retrieve directory listing for repository: " + repository + ": svnlook gave an error: " + stderr.toString());

            String[] lines = StringUtils.split(stdout.toString(), "\r\n");
            for (String line: lines) {
                line = line.trim();
                if (line.endsWith("/")) /* Directory */
                    ret.add(new Directory(repository, line.substring(0, line.length() - 1)));
            }

            return ret;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to retrieve directory listing for repository: " + repository + ": " + ex.getMessage(), ex);
        }
    }
}