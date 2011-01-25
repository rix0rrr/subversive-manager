
package nl.rix0r.subversive.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import java.util.List;
import nl.rix0r.subversive.subversion.Directory;
import nl.rix0r.subversive.subversion.EditSession;
import nl.rix0r.subversive.subversion.Modification;

/**
 * The editor interface provided by the server
 *
 * @author rix0rrr
 */
@RemoteServiceRelativePath("configeditor")
public interface ConfigEditorService extends RemoteService {

    /**
     * Apply the given list of modifications to the configuration
     *
     * Returns a list of error message of changes that couldn't be applied.
     */
    public List<String> apply(List<Modification> modifications, String username, String password) throws ServiceException;

    /**
     * Start editing the given repository
     */
    public EditSession begin(String repository, String username, String password) throws ServiceException;

    /**
     * Find all repository that can be managed by the given user
     */
    public List<String> myRepositories(String username, String password) throws ServiceException;

    /**
     * Return a list of all directories in the given repository
     */
    public List<Directory> listDirectories(String repository, String username, String password) throws ServiceException;
}
