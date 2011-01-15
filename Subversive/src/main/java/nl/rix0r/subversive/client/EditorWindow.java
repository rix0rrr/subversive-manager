
package nl.rix0r.subversive.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import nl.rix0r.subversive.client.PermissionsList.PrincipalAccess;
import nl.rix0r.subversive.subversion.Access;
import nl.rix0r.subversive.subversion.Anonymous;
import nl.rix0r.subversive.subversion.Directory;
import nl.rix0r.subversive.subversion.EditSession;
import nl.rix0r.subversive.subversion.GrantPermission;
import nl.rix0r.subversive.subversion.Group;
import nl.rix0r.subversive.subversion.Permission;
import nl.rix0r.subversive.subversion.Principal;
import nl.rix0r.subversive.subversion.RevokePermission;
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

    public void refreshPermissions() {
        Directory directory = directoryTree.selected();
        permissions.clear();

        if (directory == null) return;
        selectedDirectory.setText(directory.path());
        permissions.add(editSession.permissions(directory));
    }

    private void wireUp() {
        directoryTree.addSelectionHandler(new SelectionHandler<Directory>() {
            public void onSelection(SelectionEvent<Directory> event) {
                refreshPermissions();
            }
        });
    }

    private Directory currentDirectory() {
        return directoryTree.selected();
    }

    @UiHandler("removeButton")
    void handleRemove(ClickEvent e) {
        PrincipalAccess pa = permissions.getSelected();
        if (pa != null) revokePermissions(pa.principal);
    }

    @UiHandler("assignButton")
    void handleAssignClick(ClickEvent e) {
        if (tabpanel.getSelectedIndex() == 0)
            grantNewPermissions(users.selected());
        else
            grantNewPermissions(groups.selected());
    }

    @UiHandler("anonymousButton")
    void handleAnonymousClick(ClickEvent e) {
        grantNewPermissions(new Anonymous());
    }

    @UiHandler("users")
    void handleUserSelection(SelectionEvent<User> e) {
        grantNewPermissions(e.getSelectedItem());
    }

    @UiHandler("groups")
    void handleGroupSelection(SelectionEvent<Group> e) {
        grantNewPermissions(e.getSelectedItem());
    }

    @UiHandler("permissions")
    void permissionChanged(ValueChangeEvent<PrincipalAccess> e) {
        grantChangedPermissions(e.getValue().principal, e.getValue().access);
    }

    /**
     * Add the given principal to the currently selected directory
     */
    private void grantNewPermissions(Principal principal) {
        if (principal == null || permissions.containsPrincipal(principal)) return;
        editSession.add(new GrantPermission(new Permission(currentDirectory(), principal, Access.Read)));
        refreshPermissions();
    }

    private void grantChangedPermissions(Principal principal, Access access) {
        if (principal == null) return;
        editSession.add(new GrantPermission(new Permission(currentDirectory(), principal, access)));
        refreshPermissions();
    }

    /**
     * Remove permissions for the given principal from the currently
     * selected directory
     *
     * The value of Access actually doesn't matter: Permissions are
     * compared based on directory and principal.
     */
    private void revokePermissions(Principal principal) {
        if (principal == null || !permissions.containsPrincipal(principal)) return;
        editSession.add(new RevokePermission(new Permission(currentDirectory(), principal, Access.Read)));
        refreshPermissions();
    }
}
