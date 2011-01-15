
package nl.rix0r.subversive.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import java.util.List;
import nl.rix0r.subversive.subversion.User;

/**
 *
 * @author rix0rrr
 */
@RemoteServiceRelativePath("configeditor")
public interface UserRetrievalService extends RemoteService {
    /**
     * Find available users with a name like the given string
     */
    public List<User> findUsers(String like) throws ServiceException;

    /**
     * Return the initial set of users
     *
     * The user database back-end can decide whether it makes sense to display
     * an initial set or not: from a static, cheaply queried database, maybe
     * so. From LDAP, not so much.
     */
    public List<User> initialUserSet() throws ServiceException;

}
