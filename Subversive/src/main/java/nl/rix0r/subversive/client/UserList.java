
package nl.rix0r.subversive.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.HasOpenHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import nl.rix0r.subversive.client.generic.SelectableTable;
import nl.rix0r.subversive.client.generic.SelectableTable.Row;
import nl.rix0r.subversive.subversion.User;

/**
 *
 * @author rix0rrr
 */
public class UserList extends Composite implements
        HasSelectionHandlers<User>, HasOpenHandlers<User> {

    interface MyUiBinder extends UiBinder<Widget, UserList> { };
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    @UiField(provided=true) UserSelectList users;
    @UiField TextBox searchField;
    @UiField Label notice;

    private CachingUserRetrieval service;
    private Collection<User> baseUsers;

    public UserList() {
        users = new UserSelectList();
        initWidget(uiBinder.createAndBindUi(this));

        users.addDoubleClickHandler(new DoubleClickHandler() {
            public void onDoubleClick(DoubleClickEvent event) {
                fireOpenEvent();
                event.preventDefault();
                event.stopPropagation();
            }
        });

        users.getModel().setComparator(new Comparator<User>() {
            public int compare(User o1, User o2) {
                // Otherwise by name as per uuush
                return o1.compareTo(o2);
            }
        });
    }

    public void setNotice(String notice) {
        if (notice == null) notice = "";
        this.notice.setText(notice);
        this.notice.setVisible(!notice.equals(""));
    }

    /**
     * Set the service that can be used to retrieve information about users
     */
    public void setUserRetrieval(CachingUserRetrieval service) {
        this.baseUsers = null;
        if (this.service != service) {
            this.service = service;
            retrieveInitialSet();
        }
    }

    public void setUsers(Collection<User> users) {
        setNotice("");
        this.baseUsers = new ArrayList<User>(users);
        this.service = null;
        showFromBaseSet();
    }

    private void showFromBaseSet() {
        this.users.getModel().replace(matchingUsers(searchField.getText()));
    }

    private Collection<User> matchingUsers(String what) {
        if (what == null || what.equals("")) return baseUsers;
        List<User> ret = new ArrayList<User>();
        for (User user: baseUsers)
            if (user.matches(what))
                ret.add(user);
        return ret;
    }

    public CachingUserRetrieval getUserRetrieval() {
        return service;
    }

    private void retrieveInitialSet() {
        service.initialUserSet(initialFillUsers);
    }

    private void retrieveBasedOnFilter() {
        // Beware: can fire the callback twice if a partial response comes from the cache
        if (service.willStartNewSearch()) users.getModel().clear();
        service.findUsers(searchField.getValue(), fillUsers);
    }

    AsyncCallback<Collection<User>> initialFillUsers = new Subversive.Callback<Collection<User>>() {
        public void onSuccess(Collection<User> result) {
            if (result.isEmpty())
                setNotice("Start typing to search for a user.");
            else
                fillUsers.onSuccess(result);
        }
    };

    AsyncCallback<Collection<User>> fillUsers = new Subversive.Callback<Collection<User>>() {
        public void onSuccess(Collection<User> result) {
            setNotice("");
            users.getModel().addAll(result);
            if (users.getSelectedRow() == -1) users.setSelectedRow(0);
        }
    };

    public User selected() {
        return users.selected();
    }

    public HandlerRegistration addSelectionHandler(SelectionHandler<User> handler) {
        return users.addSelectionHandler(handler);
    }

    public HandlerRegistration addOpenHandler(OpenHandler<User> handler) {
        return addHandler(handler, OpenEvent.getType());
    }

    @UiHandler("searchField")
    void onKeyUp(KeyUpEvent e) {
        if (e.getNativeKeyCode() == 13)
            fireOpenEvent();
        else
            updateFilter();
    }

    private void updateFilter() {
        if (baseUsers != null) showFromBaseSet();
        if (service != null) retrieveBasedOnFilter();
    }

    /**
     * Fire a selection event on the selected element
     */
    private void fireOpenEvent() {
        if (selected() != null)
            OpenEvent.fire(this, selected());
    }

    public static class UserSelectList extends SelectableTable<User> {
        public UserSelectList() {
            getColumnFormatter().setWidth(0, "16px");
        }

        @Override
        protected void initializeRow(User element, Row row) {
            row.widgets.put("image", IconProvider.principal(element));
        }

        @Override
        protected void renderRow(int i, User element, Row row) {
            setWidget(i, 0, row.widgets.get("image"));
            setText(i, 1, element.toString());
        }
    }

}
