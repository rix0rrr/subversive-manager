
package nl.rix0r.subversive.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import java.util.ArrayList;
import java.util.List;
import nl.rix0r.subversive.subversion.EditSession;
import nl.rix0r.subversive.subversion.Modification;
import nl.rix0r.subversive.subversion.User;

/**
 *
 * @author rix0rrr
 */
public class StubConfigEditor implements ConfigEditorServiceAsync {
    public void apply(List<Modification> modifications, String username, String password, AsyncCallback<List<String>> callback) {
        if (!verifyPassword(username, password, callback)) return;
    }

    public void begin(String repository, String username, String password, AsyncCallback<EditSession> callback) {
        if (!verifyPassword(username, password, callback)) return;
    }

    public void findUsers(String like, AsyncCallback<List<User>> callback) {
    }

    public void myRepositories(String username, String password, AsyncCallback<List<String>> callback) {
        if (!verifyPassword(username, password, callback)) return;

        List<String> ret = new ArrayList<String>();
        ret.add("Foo");
        ret.add("Bar");
        ret.add("Baz");

        callback.onSuccess(ret);
    }

    private boolean verifyPassword(String username, String password, AsyncCallback onFail) {
        username = username != null ? username : "";
        password = password != null ? password : "";

        if (!username.equals("test") || !password.equals("test")) {
            onFail.onFailure(new ServiceException("Invalid username or password. Try again."));
            return false;
        }

        return true;
    }
}
