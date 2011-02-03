
package nl.rix0r.subversive.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import nl.rix0r.subversive.subversion.Access;
import nl.rix0r.subversive.subversion.Configuration;
import nl.rix0r.subversive.subversion.Directory;
import nl.rix0r.subversive.subversion.EditSession;
import nl.rix0r.subversive.subversion.Group;
import nl.rix0r.subversive.subversion.Modification;
import nl.rix0r.subversive.subversion.ModificationException;
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

        List<String> messages = new ArrayList<String>();
        for (Modification mod: modifications) {
            String message = applyModification(username, mod);
            if (message != null) messages.add(message);
        }
        if (modifications.isEmpty())
            messages.add("No changes made.");

        callback.onSuccess(messages);
    }

    private String applyModification(String username, Modification mod) {
        if (!canManageRepository(username, mod.repository()))
            return "You're not allowed to manage repository " + mod.repository() + ". Sorry.";

        try {
            mod.apply(configuration);
            return null;
        } catch (ModificationException ex) {
            return ex.getMessage();
        }
    }

    public void begin(String repository, String username, String password, AsyncCallback<EditSession> callback) {
        if (!verifyPassword(username, password, callback)) return;
        if (!verifyManageRepository(username, repository, callback)) return;

        callback.onSuccess(new EditSession(repository, configuration.subset(repository)));
    }

    public void initialUserSet(AsyncCallback<Collection<User>> callback) {
        callback.onSuccess(new ArrayList<User>(allUsers));
    }

    public void findUsers(String like, String u, String p, AsyncCallback<Collection<User>> callback) {
        List<User> ret = new ArrayList<User>();
        for (User user: allUsers)
            if (user.matches(like))
                ret.add(user);

        callback.onSuccess(ret);
    }

    public void expandInfo(Collection<User> p0, String u, String p, AsyncCallback<Collection<User>> callback) {
        callback.onSuccess(p0);
    }

    public void myRepositories(String username, String password, AsyncCallback<List<String>> callback) {
        if (!verifyPassword(username, password, callback)) return;

        List<String> ret = new ArrayList<String>();

        for (String repository: repositories)
            if (canManageRepository(username, repository))
                ret.add(repository);

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

    private boolean canManageRepository(String username, String repository) {
        return username.equals("test");
    }

    private boolean verifyManageRepository(String username, String repository, AsyncCallback onFail) {
        if (canManageRepository(username, repository)) return true;
        onFail.onFailure(new ServiceException("You're not allowed to manage repository " + username + ". Sorry."));
        return false;
    }

    public void listDirectories(String repository, String username, String password, AsyncCallback<List<Directory>> asyncCallback) {
        asyncCallback.onSuccess(new ArrayList<Directory>());
    }
}
