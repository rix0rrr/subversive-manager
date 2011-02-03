
package nl.rix0r.subversive.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import java.util.ArrayList;
import java.util.Collection;
import nl.rix0r.subversive.subversion.User;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author rix0rrr
 */
public class CachingUserRetrievalTest {
    int fetchCount = 0;
    String latestSearch;
    AsyncCallback<Collection<User>> findCallback;
    boolean finished = false;

    private CachingUserRetrieval userRetrieval = new CachingUserRetrieval(new UserRetrievalServiceAsync() {
        public void findUsers(String like, AsyncCallback<Collection<User>> callback) {
            fetchCount++;
            latestSearch = like;
            findCallback = callback;
        }

        public void initialUserSet(AsyncCallback<Collection<User>> callback) {
        }

        public void expandInfo(Collection<User> input, AsyncCallback<Collection<User>> callback) {
        }
    });

    AsyncCallback<Collection<User>> allDone = new AsyncCallback<Collection<User>>() {
        public void onFailure(Throwable caught) {
        }

        public void onSuccess(Collection<User> result) {
            finished = true;
        }
    };

    /**
     * Verify that no two "find user" server fetches are in progress at the same time
     */
    @Test
    public void noTwoFetches() {
        userRetrieval.findUsers("a", allDone);
        Assert.assertFalse(finished);
        Assert.assertEquals(1, fetchCount);

        userRetrieval.findUsers("b", allDone);
        Assert.assertFalse(finished);
        Assert.assertEquals(1, fetchCount);

        // Finish it off for good measure
        findCallback.onSuccess(new ArrayList<User>());
        Assert.assertTrue(finished);
    }

    @Test
    public void restartWhenQueryChanged() {
        userRetrieval.findUsers("c", allDone);
        userRetrieval.findUsers("d", allDone);
        Assert.assertEquals(1, fetchCount);
        Assert.assertFalse(finished);
        Assert.assertEquals("c", latestSearch);

        // Finish off the first call
        findCallback.onSuccess(new ArrayList<User>());
        // Should immediately trigger a fetch for "b"
        Assert.assertEquals(2, fetchCount);
        Assert.assertEquals("d", latestSearch);
    }
}
