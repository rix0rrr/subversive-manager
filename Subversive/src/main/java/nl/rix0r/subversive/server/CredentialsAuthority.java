
package nl.rix0r.subversive.server;

import java.util.Collection;
import nl.rix0r.subversive.subversion.User;

/**
 * Authority for users
 *
 * Verifies credentials and retrieves users.
 *
 * @author rix0rrr
 */
public interface CredentialsAuthority {

    /**
     * Try to authenticate the user
     *
     * Returns true if successful, false if not.
     */
    public boolean authenticate(String username, String password);

    /**
     * Return a list of users from the database matching the given query
     * string.
     */
    public Collection<User> findUsers(String like);

    /**
     * Return an initial set of users that are shown without searching.
     *
     * The back-end may decide to honor this request or not.
     */
    public Collection<User> initialSet();

    /**
     * Expand information on the given set of users
     *
     * Adds full names if all you've got is user names.
     */
    public Collection<User> expandInfo(Collection<User> input);
}
