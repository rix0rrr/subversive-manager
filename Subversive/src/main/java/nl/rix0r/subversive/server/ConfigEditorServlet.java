
package nl.rix0r.subversive.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import nl.rix0r.subversive.client.ConfigEditorService;
import nl.rix0r.subversive.client.ServerInfoService;
import nl.rix0r.subversive.client.ServiceException;
import nl.rix0r.subversive.client.UserRetrievalService;
import nl.rix0r.subversive.subversion.Configuration;
import nl.rix0r.subversive.subversion.Directory;
import nl.rix0r.subversive.subversion.EditSession;
import nl.rix0r.subversive.subversion.Group;
import nl.rix0r.subversive.subversion.GroupDefinition;
import nl.rix0r.subversive.subversion.Modification;
import nl.rix0r.subversive.subversion.User;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.log4j.Logger;

/**
 * @author rix0rrr
 */
public class ConfigEditorServlet extends RemoteServiceServlet implements
        ConfigEditorService, UserRetrievalService, ServerInfoService {

    private final static int invalidPasswordSleep = 2000;
    private final static Logger log = Logger.getLogger(ConfigEditorServlet.class);

    private PropertiesConfiguration properties;
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
            channel = new RandomAccessFile(configFile, "rw").getChannel();
            lock    = channel.lock();

            DiskConfiguration config = new DiskConfiguration(configFile);
            config.load();

            // We don't do permission validation live: otherwise we could
            // never rename (remove and re-add) the Owners group. Instead,
            // determine these things beforehand.
            modifications = filterAllowedModifications(modifications, username, config, errors);

            for (Modification mod: modifications) {
                try {
                    mod.apply(config);
                } catch (Exception ex) {
                    errors.add(ex.getMessage());
                }
            }

            config.save();

            return errors;
        } catch (IOException ex) {
            throw new ServiceException(ex);
        } finally {
            if (lock != null) try { lock.release(); } catch (IOException ex) { }
            if (channel != null) try { channel.close(); } catch (IOException ex) { }
        }
    }

    /**
     * Remove the illegal modifications from the list, adding a notice to errors if so
     */
    private List<Modification> filterAllowedModifications(List<Modification> modifications, String username, Configuration config, List<String> errors) {
        List<Modification> ret = new ArrayList<Modification>(modifications.size());
        for (Modification mod: modifications) {
            try {
                verifyCanManage(username, mod.repository(), config);
                ret.add(mod);
            } catch (Exception ex) {
                errors.add(ex.getMessage());
            }
        }
        return ret;
    }

    public synchronized EditSession begin(String repository, String username, String password) throws ServiceException {
        try {
            initialize();
            authenticate(username, password);

            DiskConfiguration config = new DiskConfiguration(configFile);
            config.load();

            verifyCanManage(username, repository, config);

            return new EditSession(repository, config.subset(repository));
        } catch (Exception ex) {
            throw new ServiceException(ex);
        }
    }

    public List<Directory> listDirectories(String repository, String username, String password) throws ServiceException {
        try {
            initialize();
            authenticate(username, password);

            DiskConfiguration config = new DiskConfiguration(configFile);
            config.load();

            verifyCanManage(username, repository, config);

            return repositoryLister.listDirectories(repository);
        } catch (Exception ex) {
            throw new ServiceException(ex);
        }
    }


    public List<String> myRepositories(String username, String password) throws ServiceException {
        initialize();
        authenticate(username, password);

        try {
            DiskConfiguration config = new DiskConfiguration(configFile);
            config.load();

            List<String> ret = new ArrayList<String>();
            for (String repo: repositoryLister.allRepositories())
                if (canManage(username, repo, config))
                    ret.add(repo);

            return ret;
        } catch (IOException ex) {
            throw new ServiceException(ex);
        }
    }

    public Collection<User> findUsers(String like) throws ServiceException {
        try {
            initialize();
            if (userAuthority == null) return new ArrayList<User>(); // None
            return userAuthority.findUsers(like);
        } catch (Exception ex) {
            log.warn(ex.getMessage(), ex);
            throw new ServiceException(ex);
        }
    }

    public Collection<User> initialUserSet() throws ServiceException {
        try {
            initialize();
            if (userAuthority == null) return new ArrayList<User>(); // None
            return userAuthority.initialSet();
        } catch (Exception ex) {
            log.warn(ex.getMessage(), ex);
            return new ArrayList<User>();
        }
    }

    public Collection<User> expandInfo(Collection<User> input) throws ServiceException {
        try {
            initialize();
            if (userAuthority == null) return new ArrayList<User>(); // None
            return userAuthority.expandInfo(input);
        } catch (Exception ex) {
            log.warn(ex.getMessage(), ex);
            return input;
        }
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

        if (properties == null) properties = tryFile("/etc/subversive.conf"); // System-wide
        if (properties == null) properties = tryFile(".subversive.conf");     // Expected in home dir
        if (properties == null) properties = tryFile("subversive.conf");      // Expected in current directory
        if (properties == null)
            throw new ServiceException("Error loading configuration (/etc/subversive.conf or .subversive.conf)");
        else
            log.info("Configuration loaded from: " + properties.getFile());
    }

    private PropertiesConfiguration tryFile(String filename) {
        try {
            PropertiesConfiguration props = new PropertiesConfiguration(filename);
            FileChangedReloadingStrategy reloading = new FileChangedReloadingStrategy();
            reloading.setRefreshDelay(1000); // At most once every second
            props.setReloadingStrategy(reloading);
            return props;
        } catch (ConfigurationException ex) {
            log.warn("Can't load configuration file: " + filename);
            return null;
        }
    }

    /**
     * Authenticate the given credentials
     *
     * Throws an exception on failure. Additionally, sleep for a bit to avoid
     * clients brute-forcing the password. Of course, this allows for a DoS
     * but that's better than the alternative.
     */
    private void authenticate(String username, String password) throws ServiceException {
        if (userAuthority == null)
            throw new ServiceException("No authentication mechanism specified, and no credentials passed by proxy. Check the configuration.");

        if (!userAuthority.authenticate(username, password)) {
            try {
                Thread.sleep(invalidPasswordSleep);
            } catch (InterruptedException ex) { }
            throw new ServiceException("Invalid username or password for user: " + username);
        }
    }

    /**
     * Return whether the user is allowed to manage the repository
     *
     * Throws an exception if not.
     */
    private void verifyCanManage(String username, String repository, Configuration config) throws ServiceException {
        if (!canManage(username, repository, config))
            throw new ServiceException("'" + username + "' is not allowed to manage the '" + repository + "' repository. Sorry.");
    }

    private boolean canManage(String username, String repository, Configuration config) throws ServiceException {
        return isAdmin(username, config) || isOwner(username, repository, config);
    }

    /**
     * Returns whether the user is admin
     *
     * I.e., if there is a group marked as admin group, whether the user is
     * a member of this group.
     */
    private boolean isAdmin(String username, Configuration config) throws ServiceException {
        loadProperties();

        String adminGroupName = properties.getString("subversive.admingroup", "admins");

        return memberOf(username, new Group(adminGroupName), config);
    }

    private boolean isOwner(String username, String repository, Configuration config) throws ServiceException {
        loadProperties();

        String ownerGroupName = properties.getString("subversive.ownergroup", "owners");

        return memberOf(username, new Group(repository, ownerGroupName), config);
    }

    private boolean memberOf(String username, Group group, Configuration config) {
        GroupDefinition gd = config.group(group);
        return gd != null && gd.users().contains(new User(username));
    }

    /**
     * Load the configuration
     */
    private void initConfiguration() throws ServiceException {
        loadProperties();
        String fileName = properties.getString("subversion.accessfile");

        if (fileName == null) throw new ServiceException("Property file setting not found: subversion.accessfile");
        File file = new File(fileName);
        if (!file.exists()) throw new ServiceException("Subversion configuration file not found: " + file);
        if (!file.canWrite()) throw new ServiceException("Subversion configuration file not writable: " + file);
        configFile = file;
    }

    private void initUserAuthority() throws ServiceException {
        try {
            loadProperties();
            String htPasswdFile = properties.getString("auth.htpasswd");
            if (htPasswdFile != null) {
                userAuthority = new HtPasswdAuthority(new File(htPasswdFile));
                return;
            }

            String ldapUrl = properties.getString("auth.ldap.url");
            if (ldapUrl != null) {
                LdapAuthority ldap = new LdapAuthority(ldapUrl);

                String searchUser = properties.getString("auth.ldap.searchuserdn");
                String searchPass = properties.getString("auth.ldap.searchpassword");
                if (searchUser != null && !searchUser.equals(""))
                    ldap.setSearchLogin(searchUser, searchPass);

                String usernameField = properties.getString("auth.ldap.usernamefield");
                if (usernameField != null && !usernameField.equals(""))
                    ldap.setUsernameField(usernameField);

                String fullNameField = properties.getString("auth.ldap.fullnamefield");
                if (fullNameField != null && !fullNameField.equals(""))
                    ldap.setFullNameFields(fullNameField);

                userAuthority = ldap;
                return;
            }

            userAuthority = null;
        } catch (Exception ex) {
            throw new ServiceException(ex);
        }
    }

    private void initRepositoryLister() throws ServiceException {
        try {
            String repoDir = properties.getString("subversion.repodir");
            if (repoDir == null) throw new ServiceException("Property file: subversion.repodir not set.");

            repositoryLister = new RepositoryLister(new File(repoDir));
        } catch (Exception ex) {
            throw new ServiceException(ex);
        }
    }

    /**
     * Return the branding image from the config
     */
    public String[] getBrandingImage() {
        try {
            initialize();
            return new String[] {
                properties.getString("subversive.brandingimage", ""),
                properties.getString("subversive.brandinglink", "")
            };
        } catch (Exception ex) {
            log.warn("Error retrieving branding image", ex);
            return new String[0];
        }
    }
}
