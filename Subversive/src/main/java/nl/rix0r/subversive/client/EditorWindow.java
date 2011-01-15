
package nl.rix0r.subversive.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import nl.rix0r.subversive.subversion.Access;
import nl.rix0r.subversive.subversion.Anonymous;
import nl.rix0r.subversive.subversion.Directory;
import nl.rix0r.subversive.subversion.EditSession;
import nl.rix0r.subversive.subversion.Group;
import nl.rix0r.subversive.subversion.Principal;
import nl.rix0r.subversive.subversion.User;

/**
 * @author rix0rrr
 */
public class EditorWindow extends Composite {
    interface MyUiBinder extends UiBinder<Widget, EditorWindow> { };
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    @UiField Button saveButton;
    @UiField PermissionsList permissions;
    @UiField Label repoTitle;
    @UiField DirectoryTree directoryTree;
    @UiField Label selectedDirectory;
    @UiField Button assignButton;
    @UiField Button removeButton;
    @UiField GroupList groups;
    @UiField UserList users;
    @UiField TabLayoutPanel tabpanel;

    private EditSession editSession;

    public EditorWindow(EditSession editSession, UserRetrievalServiceAsync userRetrieval) {
        initWidget(uiBinder.createAndBindUi(this));
        wireUp();
        setEditSession(editSession);
        users.setUserRetrievalService(userRetrieval);
    }

    public void setEditSession(EditSession editSession) {
        this.editSession = editSession;
        refresh();
    }

    public void refresh() {
        repoTitle.setText(editSession.repository());
        directoryTree.load(editSession.configuredDirectories());
        groups.setGroups(editSession.availableGroups());
    }

    private void wireUp() {
        directoryTree.addSelectionHandler(new SelectionHandler<Directory>() {
            public void onSelection(SelectionEvent<Directory> event) {
                showDirectory(event.getSelectedItem());
            }
        });
    }

    /**
     * Load the permissions for the given directory
     */
    private void showDirectory(Directory directory) {
        if (directory == null) return;

        selectedDirectory.setText(directory.path());
        permissions.clear();
        permissions.add(editSession.permissions(directory));
    }

    @UiHandler("removeButton")
    void handleRemove(ClickEvent e) {
        permissions.remove(permissions.getSelected());
    }

    @UiHandler("assignButton")
    void handleAssignClick(ClickEvent e) {
        if (tabpanel.getSelectedIndex() == 0)
            addPrincipal(users.selected());
        else
            addPrincipal(groups.selected());
    }

    @UiHandler("anonymousButton")
    void handleAnonymousClick(ClickEvent e) {
        addPrincipal(new Anonymous());
    }

    @UiHandler("users")
    void handleUserSelection(SelectionEvent<User> e) {
        addPrincipal(e.getSelectedItem());
    }

    @UiHandler("groups")
    void handleGroupSelection(SelectionEvent<Group> e) {
        addPrincipal(e.getSelectedItem());
    }

    private void addPrincipal(Principal principal) {
        if (principal == null || permissions.containsPrincipal(principal)) return;
        permissions.addPrincipal(principal, Access.Read);
    }
}
