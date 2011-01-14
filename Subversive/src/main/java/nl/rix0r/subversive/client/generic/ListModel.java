
package nl.rix0r.subversive.client.generic;

import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A collection of classes to be rendered in a table
 *
 * It is basically a List with events for add and remove.
 */
public class ListModel<T> implements HasValueChangeHandlers<Object> {
    private List<T> contents = new ArrayList<T>();
    private HandlerManager events = new HandlerManager(this);

    public void add(T element) {
        contents.add(element);
        changed();
    }

    public void addAll(Collection<T> elements) {
        contents.addAll(elements);
        changed();
    }

    public void remove(T element) {
        contents.remove(element);
        changed();
    }

    public void removeAll(List<T> elements) {
        contents.removeAll(contents);
        changed();
    }

    public void clear() {
        contents.clear();
        changed();
    }

    public T get(int i) {
        if (i < 0 || i >= size()) return null; // Explosion-safe get
        return contents.get(i);
    }

    public List<T> all() {
        return Collections.unmodifiableList(contents);
    }

    public void replace(Collection<? extends T> what) {
        contents.clear();
        contents.addAll(what);
        changed();
    }

    public int size() {
        return contents.size();
    }

    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Object> handler) {
        return events.addHandler(ValueChangeEvent.getType(), handler);
    }

    public void fireEvent(GwtEvent<?> event) {
        events.fireEvent(event);
    }

    protected void changed() {
        ValueChangeEvent.fire(this, null);
    }
}
