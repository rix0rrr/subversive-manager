
package nl.rix0r.subversive.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import nl.rix0r.subversive.server.generic.LDAP;
import nl.rix0r.subversive.subversion.User;
import org.apache.log4j.Logger;

/**
 * A user authority for connecting to LDAP
 *
 * Parameters:
 * - LDAP url: base of the LDAP directory to search. Should include server and
 *   root DN. Of the form: ldap://server:389/ou=root,o=dn. (Required)
 * - Search credentials: used to locate the user object to authenticate based
 *   on just a username. Will try anonymous searching for login user, if not
 *   available, will try logged-in user credentials to search for attributes
 *   (Default: anonymous)
 * - Username field: attribute in directory containing username. Typically
 *   'uid' or 'cn'. (Default: 'uid')
 * - Fullname field: a comma-separated list of fields that are tried (in order)
 *   Can't use spaces in this.
 *   to obtain the user's full name. (Default: 'fullname, cn')
 *
 * @author rix0rrr
 */
public class LdapAuthority implements CredentialsAuthority {
    private static final Logger log = Logger.getLogger(LdapAuthority.class);

    private String url;
    private String searchDn;
    private String searchPass;
    private String usernameField  = "uid";
    private String fullNameFields = "fullname, cn";

    public LdapAuthority(String url) {
        this.url = url;
    }

    public void setSearchLogin(String searchDn, String password) {
        this.searchDn = searchDn;
        this.searchPass = password;
    }

    public void otherSearchLogin(String username, String password) {
        if ((searchDn == null || searchDn.equals(""))
        && !(username == null || username.equals(""))) {
            searchDn   = findDn(username);
            searchPass = password;
            log.debug("Using " + searchDn + " for search.");
        }
    }

    public void setUsernameField(String usernameField) {
        this.usernameField = usernameField;
    }

    public void setFullNameFields(String fullNameFields) {
        this.fullNameFields = fullNameFields;
    }

    private String findDn(String username) {
        LDAP ldap = new LDAP(url, searchDn, searchPass);
        try {
            return ldap.findUserName(username, usernameField, true);
        } finally {
            ldap.close();
        }
    }

    /**
     * Authenticate the user, return true or false
     *
     * Log LDAP failures to the log
     */
    public boolean authenticate(String username, String password) {
        try {
            LDAP.validateCredentials(url, usernameField, username, password, searchDn, searchPass);
            return true;
        } catch (RuntimeException ex) {
            log.warn(ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Return a list of fields that should be
     * @return
     */
    String[] interestingFields() {
        ArrayList<String> ret = new ArrayList<String>();
        for (String fullNameField: fullNameFields()) ret.add(fullNameField);
        ret.add(usernameField);
        return asArray(ret);
    }

    private String[] asArray(List<String> xs) {
        String[] arr = new String[xs.size()];
        int i = 0;
        for (String s: xs) arr[i++] = s;
        return arr;
    }

    private String[] fullNameFields() {
        String[] fields = fullNameFields.split(",");
        for (int i = 0; i < fields.length; i++)
            fields[i] = fields[i].trim();
        return fields;
    }

    /**
     * Create a user object from the given map
     */
    private User userFromMap(Map<String, String> properties) {
        String username = properties.get(usernameField);
        String fullName = "";
        for (String fullNameField: fullNameFields()) {
            fullName = properties.get(fullNameField);
            if (!fullName.equals("")) break;
        }
        return new User(username, fullName);
    }

    public Collection<User> findUsers(String like) {
        List<User> ret = new ArrayList<User>();

        LDAP ldap = new LDAP(url, searchDn, searchPass);
        try {
            List<Map<String, String>> results = ldap.search(like, interestingFields(), interestingFields());

            for (Map<String, String> record: results)
                ret.add(userFromMap(record));

            return ret;
        } finally {
            ldap.close();
        }

    }

    /**
     * Initial set of users
     *
     * No initial set for LDAP.
     */
    public Collection<User> initialSet() {
        return new ArrayList<User>();
    }

    public Collection<User> expandInfo(Collection<User> input) {
        List<User> ret = new ArrayList<User>();

        LDAP ldap = new LDAP(url, searchDn, searchPass);
        try {
            for (User user: input) {
                String dn = ldap.findUserName(user.username(), usernameField, false);
                if (!dn.equals(""))
                    ret.add(userFromMap(ldap.getProperties(dn, interestingFields())));
            }

            return ret;
        } finally {
            ldap.close();
        }
    }
}
