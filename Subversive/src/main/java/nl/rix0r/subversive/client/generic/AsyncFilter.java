
package nl.rix0r.subversive.client.generic;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 *
 * @author rix0rrr
 */
abstract public class AsyncFilter<T> implements AsyncCallback<T> {
    private AsyncCallback<T> original;

    public AsyncFilter(AsyncCallback<T> original) {
        this.original = original;
    }

    public void onFailure(Throwable caught) {
        original.onFailure(caught);
    }

    public void onSuccess(T result) {
        original.onSuccess(filter(result));
    }

    abstract protected T filter(T result);
}
