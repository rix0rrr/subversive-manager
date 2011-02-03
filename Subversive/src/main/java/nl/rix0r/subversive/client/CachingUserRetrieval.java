
package nl.rix0r.subversive.client;

import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.rpc.AsyncCallback;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import nl.rix0r.subversive.client.generic.AsyncFilter;
import nl.rix0r.subversive.subversion.User;

/**
 * A user retrieval object that caches information returned by the server to
 * respond to queries faster.
 *
 * @author rix0rrr
 */
public class CachingUserRetrieval implements HasValueChangeHandlers<Void> {
    private UserRetrievalServiceAsync remote;
    private HandlerManager handlerManager = new HandlerManager(this);

    // Users that we've already seen
    private Map<User, User> seenUsers = new HashMap<User, User>();

    // Bookkeeping for expandUsers
    private Set<User>  requestedUsers = new HashSet<User>();
    private Set<User>    pendingUsers = new HashSet<User>();

    private String username;
    private String password;

    public CachingUserRetrieval(UserRetrievalServiceAsync remote) {
        this.remote = remote;
    }

    public void setLogin(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Remember that the given set of users has passed through the cache
     */
    private void remember(Collection<User> users) {
        for (User user: users)
            seenUsers.put(user, user);
    }

    /**
     * Return all user objects from the cache that match the search term
     */
    private Collection<User> quickFind(String query) {
        Set<User> ret = new HashSet<User>();
        for (User user: seenUsers.keySet())
            if (user.matches(query))
                ret.add(user);
        return ret;
    }

    /**
     * Find users with names matching the given query strings
     *
     * Beware that the callback may be fired multiple times: once for the
     * information that can be retrieved quickly and once for the information
     * that is fetched from the remote server.
     *
     * Returns true if this call has started a new request. In that case, the
     * current database should be cleared.
     */
    public void findUsers(String like, final AsyncCallback<Collection<User>> whenFound) {
        latestQuery = like;
        if (loading) return;

        final Collection<User> quickResponse = quickFind(like);
        if (!quickResponse.isEmpty()) whenFound.onSuccess(quickResponse);

        remote.findUsers(like, username, password, new AsyncFilter<Collection<User>>(whenFound) {
            @Override
            protected Collection<User> filter(Collection<User> result) {
                remember(result);
                // Return only what wasn't yet in the quick response
                result.removeAll(quickResponse);
                loading = false;

                if (!fetchStartedQuery.equals(latestQuery))
                    findUsers(latestQuery, whenFound);

                return result;
            }
        });
    }

    public boolean willStartNewSearch() {
        return !loading;
    }

    public void initialUserSet(AsyncCallback<Collection<User>> callback) {
        remote.initialUserSet(new AsyncFilter<Collection<User>>(callback) {
            @Override
            protected Collection<User> filter(Collection<User> result) {
                remember(result);
                return result;
            }
        });
    }

    /**
     * Expand info on the given user
     *
     * Returns user objects from the cache if available, or the requested
     * user objects if the object is not in the cache. Designed to be called
     * in a loop, call finished() when the loop is finished.
     *
     * If it is found that some users are not known, their information will
     * then be retrieved, and an event will be fired as soon as information is
     * available (at which point the information can be re-queried).
     */
    public User expandUser(User user) {
        if (seenUsers.containsKey(user))
            return seenUsers.get(user);

        if (requestedUsers.contains(user))
            // Either request pending or no info available
            return user;

        requestedUsers.add(user);
        pendingUsers.add(user);
        return user;
    }

    /**
     * Signal that a loop of expandUser calls is finished, and retrieval
     * can start
     */
    public void finished() {
        if (!pendingUsers.isEmpty()) {
            remote.expandInfo(pendingUsers, username, password, usersRetrieved);
            pendingUsers.clear();
        }
    }

    private AsyncCallback<Collection<User>> usersRetrieved = new AsyncCallback<Collection<User>>() {
        public void onFailure(Throwable caught) {
        }

        public void onSuccess(Collection<User> result) {
            remember(result);
            ValueChangeEvent.fire(CachingUserRetrieval.this, null);
        }
    };

    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Void> handler) {
        return handlerManager.addHandler(ValueChangeEvent.getType(), handler);
    }

    public void fireEvent(GwtEvent<?> event) {
        handlerManager.fireEvent(event);
    }
}
