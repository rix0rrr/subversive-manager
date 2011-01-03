
package nl.rix0r.subversive.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import java.util.List;
import nl.rix0r.subversive.client.ConfigEditorService;
import nl.rix0r.subversive.subversion.EditSession;
import nl.rix0r.subversive.subversion.Modification;
import nl.rix0r.subversive.subversion.User;

/**
 *
 * @author rix0rrr
 */
public class ConfigEditorServlet extends RemoteServiceServlet implements ConfigEditorService {

    public List<String> apply(List<Modification> modifications, String username, String password) {
        return null;
    }

    public EditSession begin(String repository, String username, String password) {
        return null;
    }

    public List<User> findUsers(String like) {
        return null;
    }

    public List<String> myRepositories(String username, String password) {
        return null;
    }
}
