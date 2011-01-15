
package nl.rix0r.subversive.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import java.util.ArrayList;
import java.util.List;
import nl.rix0r.subversive.subversion.Access;
import nl.rix0r.subversive.subversion.Configuration;
import nl.rix0r.subversive.subversion.EditSession;
import nl.rix0r.subversive.subversion.Group;
import nl.rix0r.subversive.subversion.Modification;
import nl.rix0r.subversive.subversion.TestConfigurationBuilder;
import nl.rix0r.subversive.subversion.User;

/**
 *
 * @author rix0rrr
 */
public class StubConfigEditor implements ConfigEditorServiceAsync, UserRetrievalServiceAsync {
    private List<User> allUsers = new ArrayList<User>() {{
        add(new User("alice",  "Alice Krieger"));
        add(new User("bob",    "Uncle Bob"));
        add(new User("carol",  "Christmas Carol"));
        add(new User("dave",   "Dangerous Dave"));
        add(new User("eve",    "Eve of Destruction"));
        add(new User("fred",   "Fred F."));
        add(new User("george", "George Weasley"));
        add(new User("paul",   "Paul \"The Party Animal\" Parker"));
    }};

    private List<String> repositories = new ArrayList<String>() {{
        add("Foo");
        add("Bar");
        add("Baz");
    }};

    private Configuration configuration = new TestConfigurationBuilder()
            .group(new Group("Foo", "Editors"), new User("alice"), new User("bob"))
            .group(new Group("Foo", "Readers"), new User("eve"), new User("paul"))
            .group(new Group("Admins"), new User("fred"), new User("george"))
            .group(new Group("Bar", "RW"), new User("carol"))
            .group(new Group("Baz", "Empty"))
            .directory("Foo", "/")
                .permission(new Group("Foo", "Editors"), Access.ReadWrite)
                .permission(new Group("Foo", "Readers"), Access.Read)
            .directory("Bar", "/")
                .permission(new Group("Bar", "RW"), Access.ReadWrite)
                .permission(new User("carol"), Access.Read)
            .directory("Bar", "/some/sub/path")
                .permission(new User("dave"), Access.Read)
            .directory("Baz", "/")
                .permission(new User("mallory"), Access.Read)
            .build();

    public void apply(List<Modification> modifications, String username, String password, AsyncCallback<List<String>> callback) {
        if (!verifyPassword(username, password, callback)) return;
    }

    public void begin(String repository, String username, String password, AsyncCallback<EditSession> callback) {
        if (!verifyPassword(username, password, callback)) return;

        callback.onSuccess(new EditSession(repository, configuration.subset(repository)));
    }

    public void initialUserSet(AsyncCallback<List<User>> callback) {
        callback.onSuccess(new ArrayList<User>(allUsers));
    }

    public void findUsers(String like, AsyncCallback<List<User>> callback) {
        List<User> ret = new ArrayList<User>();
        for (User user: allUsers)
            if (user.matches(like))
                ret.add(user);

        callback.onSuccess(ret);
    }

    public void myRepositories(String username, String password, AsyncCallback<List<String>> callback) {
        if (!verifyPassword(username, password, callback)) return;

        List<String> ret = new ArrayList<String>();

        if (username.equals("test")) {
            ret.addAll(repositories);
        }

        callback.onSuccess(ret);
    }

    private boolean verifyPassword(String username, String password, AsyncCallback onFail) {
        username = username != null ? username : "";
        password = password != null ? password : "";

        boolean ok = (username.equals("foo") || username.equals("test"))
                && (username.equals(password));

        if (!ok) onFail.onFailure(new ServiceException("Invalid username or password. Try again."));
        return ok;
    }

}
