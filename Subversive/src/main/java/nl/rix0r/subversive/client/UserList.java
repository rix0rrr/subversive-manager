
package nl.rix0r.subversive.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import java.util.List;
import nl.rix0r.subversive.client.generic.SelectableTable;
import nl.rix0r.subversive.client.generic.SelectableTable.Row;
import nl.rix0r.subversive.subversion.User;

/**
 *
 * @author rix0rrr
 */
public class UserList extends Composite {
    interface MyUiBinder extends UiBinder<Widget, UserList> { };
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    @UiField(provided=true) UserSelectList users;
    @UiField TextBox searchField;

    private UserRetrievalServiceAsync service;
    private boolean loading = false;

    public UserList() {
        users = new UserSelectList();
        initWidget(uiBinder.createAndBindUi(this));
    }

    /**
     * Set the service that can be used to retrieve information about users
     */
    public void setUserRetrievalService(UserRetrievalServiceAsync service) {
        if (this.service != service) {
            this.service = service;
            retrieveInitialSet();
        }
    }

    public UserRetrievalServiceAsync getUserRetrievalService() {
        return service;
    }

    private void retrieveInitialSet() {
        loading = true;
        service.initialUserSet(fillUsers);
    }

    private void retrieveBasedOnFilter() {
        if (loading) return;
        loading = true;
        service.findUsers(searchField.getValue(), fillUsers);
    }

    AsyncCallback<List<User>> fillUsers = new Subversive.Callback<List<User>>() {
        public void onSuccess(List<User> result) {
            loading = false;
            users.getModel().replace(result);
            users.setSelectedRow(0);
        }
    };

    @UiHandler("searchField")
    void onKeyUp(KeyUpEvent e) {
        retrieveBasedOnFilter();
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
