
package nl.rix0r.subversive.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import nl.rix0r.subversive.client.ConfigEditorService;
import nl.rix0r.subversive.client.ServiceException;
import nl.rix0r.subversive.client.UserRetrievalService;
import nl.rix0r.subversive.subversion.EditSession;
import nl.rix0r.subversive.subversion.Modification;
import nl.rix0r.subversive.subversion.User;

/**
 * @author rix0rrr
 */
public class ConfigEditorServlet extends RemoteServiceServlet implements ConfigEditorService, UserRetrievalService {
    private final static String propertiesFile = "subversive.properties";

    private Properties properties;
    private File configFile;
    private CredentialsAuthority userAuthority;
    private RepositoryLister repositoryLister;

    public synchronized List<String> apply(List<Modification> modifications, String username, String password) throws ServiceException {
        initialize();
        authenticate(username, password);

        FileChannel channel = null;
        FileLock    lock = null;

        List<String> errors = new ArrayList<String>();
        try {
            // Acquire lock on the file
            //channel = new RandomAccessFile(configFile, "rw").getChannel();
            //lock    = channel.lock();

            DiskConfiguration config = new DiskConfiguration(configFile);
            config.load();

            for (Modification mod: modifications) {
                try {
                    verifyCanManage(username, mod.repository());
                    mod.apply(config);
                } catch (Exception ex) {
                    errors.add(ex.getMessage());
                }
            }

            //config.save();

            return errors;
        } catch (IOException ex) {
            throw new ServiceException(ex);
        } finally {
            if (lock != null) try { lock.release(); } catch (IOException ex) { }
            if (channel != null) try { channel.close(); } catch (IOException ex) { }
        }
    }

    public synchronized EditSession begin(String repository, String username, String password) throws ServiceException {
        try {
            initialize();
            authenticate(username, password);
            verifyCanManage(username, repository);

            DiskConfiguration config = new DiskConfiguration(configFile);
            config.load();

            return new EditSession(repository, config.subset(repository));
        } catch (Exception ex) {
            throw new ServiceException(ex);
        }
    }


    public List<String> myRepositories(String username, String password) throws ServiceException {
        initialize();
        authenticate(username, password);

        List<String> ret = new ArrayList<String>();
        for (String repo: repositoryLister.allRepositories())
            if (canManage(username, repo))
                ret.add(repo);

        return ret;
    }

    public List<User> findUsers(String like) throws ServiceException {
        initialize();
        return userAuthority.findUsers(like);
    }

    public List<User> initialUserSet() throws ServiceException {
        initialize();
        return userAuthority.initialSet();
    }

    private void initialize() throws ServiceException {
        initConfiguration();
        initUserAuthority();
        initRepositoryLister();
    }

    /**
     * Read the properties file
     */
    private void loadProperties() throws ServiceException {
        if (properties != null) return;

        InputStream is = getClass().getClassLoader().getResourceAsStream(propertiesFile);
        if (is == null) throw new ServiceException("Properties file not found: " + propertiesFile);
        try {
            Properties p = new Properties();
            p.load(is);
            properties = p;
        } catch (IOException ex) {
            throw new ServiceException("Error loading properties file: " + propertiesFile + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Authenticate the given credentials
     *
     * Throws an exception on failure.
     */
    private void authenticate(String username, String password) throws ServiceException {
        if (!userAuthority.authenticate(username, password))
            throw new ServiceException("Invalid username or password for user: " + username);
    }

    /**
     * Return whether the user is allowed to manage the repository
     *
     * Throws an exception if not.
     */
    private void verifyCanManage(String username, String repository) throws ServiceException {
        if (!canManage(username, repository))
            throw new ServiceException(username + " is not allowed to manage " + repository + ". Sorry.");
    }

    private boolean canManage(String username, String repository) {
        return true;
    }

    /**
     * Load the configuration
     */
    private void initConfiguration() throws ServiceException {
        if (configFile != null) return;

        loadProperties();
        String fileName = properties.getProperty("subversion.access");
        if (fileName == null) throw new ServiceException("Property file setting not found: subversion.access");
        File file = new File(fileName);
        if (!file.exists()) throw new ServiceException("Configuration file not found: " + file);
        if (!file.canWrite()) throw new ServiceException("Configuration file not writable: " + file);
        configFile = file;
    }

    private void initUserAuthority() throws ServiceException {
        try {
            if (userAuthority != null) return;

            loadProperties();
            String htPasswdFile = properties.getProperty("auth.htpasswd");
            if (htPasswdFile != null) {
                userAuthority = new HtPasswdAuthority(new File(htPasswdFile));
                return;
            }

            throw new ServiceException("Property file: must set a user authority, either htpasswd file or LDAP server.");
        } catch (Exception ex) {
            throw new ServiceException(ex);
        }
    }

    private void initRepositoryLister() throws ServiceException {
        try {
            if (repositoryLister != null) return;

            String repoDir = properties.getProperty("subversion.repodir");
            if (repoDir == null) throw new ServiceException("Property file: subversion.repodir not set.");

            repositoryLister = new RepositoryLister(new File(repoDir));
        } catch (Exception ex) {
            throw new ServiceException(ex);
        }
    }
}
