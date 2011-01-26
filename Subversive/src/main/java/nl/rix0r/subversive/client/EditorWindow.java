
package nl.rix0r.subversive.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.Widget;
import java.util.List;
import nl.rix0r.subversive.client.DirectoryTree.DirectoryDecorator;
import nl.rix0r.subversive.client.GroupList.GroupDecorator;
import nl.rix0r.subversive.client.PermissionsList.PrincipalAccess;
import nl.rix0r.subversive.subversion.Access;
import nl.rix0r.subversive.subversion.Anonymous;
import nl.rix0r.subversive.subversion.Directory;
import nl.rix0r.subversive.subversion.EditSession;
import nl.rix0r.subversive.subversion.EditSession.GroupModifications;
import nl.rix0r.subversive.subversion.GrantPermission;
import nl.rix0r.subversive.subversion.Group;
import nl.rix0r.subversive.subversion.Permission;
import nl.rix0r.subversive.subversion.Principal;
import nl.rix0r.subversive.subversion.RemoveGroup;
import nl.rix0r.subversive.subversion.RevokePermission;
import nl.rix0r.subversive.subversion.User;

/**
 * @author rix0rrr
 */
public class EditorWindow extends Composite implements HasCloseHandlers<EditSession> {
    interface MyUiBinder extends UiBinder<Widget, EditorWindow> { };
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    @UiField Button saveButton;
    @UiField Button undoButton;
    @UiField PermissionsList permissions;
    @UiField Label repoTitle;
    @UiField DirectoryTree directoryTree;
    @UiField Label selectedDirectory;
    @UiField Button assignButton;
    @UiField Button removeButton;
    @UiField Button anonymousButton;
    @UiField Button newGroupButton;
    @UiField Button editGroupButton;
    @UiField Button deleteGroupButton;
    @UiField GroupList groups;
    @UiField UserList users;
    @UiField TabLayoutPanel tabpanel;
    @UiField Image alertImage;

    private EditSession editSession;
    private UserRetrievalServiceAsync userRetrieval;

    public EditorWindow(EditSession editSession, UserRetrievalServiceAsync userRetrieval) {
        initWidget(uiBinder.createAndBindUi(this));
        wireUp();
        this.userRetrieval = userRetrieval;
        users.setUserRetrievalService(userRetrieval);
        groups.setDecorator(groupDecorator);
        directoryTree.setDecorator(directoryDecorator);

        setEditSession(editSession);
    }

    public void setEditSession(EditSession editSession) {
        this.editSession = editSession;
        directoryTree.setRepository(editSession.repository());
        refresh();
    }

    public void refresh() {
        List<Directory> configuredDirectories = editSession.configuredDirectories();
        directoryTree.add(configuredDirectories);
        directoryTree.makeVisible(configuredDirectories);
        repoTitle.setText(editSession.repository());
        groups.setGroups(editSession.availableGroups());
        refreshPermissions();
        refreshButtonStates();
    }

    /**
     * Refresh button states (enabled or disabled)
     *
     * Call this if you know nothing else has changed.
     */
    private void refreshButtonStates() {
        if (editSession == null) return;

        undoButton.setEnabled(editSession.canUndo());
        assignButton.setEnabled(
                (tabpanel.getSelectedIndex() == 0 && users.selected() != null && !permissions.containsPrincipal(users.selected()))
                || (groups.selected() != null && !permissions.containsPrincipal(groups.selected())));
        anonymousButton.setEnabled(!permissions.containsPrincipal(new Anonymous()));
        removeButton.setEnabled(permissions.selected() != null);
        editGroupButton.setEnabled(groups.selected() != null && userCanEditGroup(groups.selected()));
        deleteGroupButton.setEnabled(editGroupButton.isEnabled());
    }

    /**
     * Refresh the permissions list
     *
     * Call this if you know it's the only thing that needs updating.
     */
    public void refreshPermissions() {
        Directory directory = directoryTree.selected();
        permissions.clear();

        if (directory == null) return;
        selectedDirectory.setText(directory.path());
        permissions.add(editSession.permissions(directory));

        // Doesn't really have anything to do with that but
        // always seems to go hand-in-hand.
        refreshButtonStates();
    }

    private void wireUp() {
        directoryTree.addSelectionHandler(new SelectionHandler<Directory>() {
            public void onSelection(SelectionEvent<Directory> event) {
                refresh();
            }
        });
    }

    public HandlerRegistration addCloseHandler(CloseHandler<EditSession> handler) {
        return addHandler(handler, CloseEvent.getType());
    }

    public void directoryRetrievalFailed(String errorMessage) {
        alertImage.setTitle(errorMessage);
        alertImage.setVisible(true);
    }

    public void directoriesRetrieved(List<Directory> directories) {
        directoryTree.add(directories);
        alertImage.setVisible(false);
    }

    private void editingDone() {
        CloseEvent.fire(this, editSession);
    }

    private Directory currentDirectory() {
        return directoryTree.selected();
    }

    @UiHandler("saveButton")
    void saveButtonClicked(ClickEvent e) {
        editingDone();
    }

    @UiHandler("undoButton")
    void undoButtonClicked(ClickEvent e) {
        editSession.undo();
        refresh();
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
    void handleUserOpen(OpenEvent<User> e) {
        grantNewPermissions(e.getTarget());
    }

    @UiHandler("users")
    void userSelectionChanged(SelectionEvent<User> e) {
        refreshButtonStates();
    }

    @UiHandler("groups")
    void handleGroupOpen(OpenEvent<Group> e) {
        grantNewPermissions(e.getTarget());
    }

    @UiHandler("groups")
    void groupSelectionChanged(SelectionEvent<Group> e) {
        refreshButtonStates();
    }

    @UiHandler("permissions")
    void permissionsSelectionChanged(SelectionEvent<PrincipalAccess> e) {
        refreshButtonStates();
    }

    @UiHandler("permissions")
    void permissionChanged(ValueChangeEvent<PrincipalAccess> e) {
        grantChangedPermissions(e.getValue().principal, e.getValue().access);
    }

    @UiHandler("newGroupButton")
    void newGroupClicked(ClickEvent e) {
        newGroup();
    }

    @UiHandler("editGroupButton")
    void editGroupClicked(ClickEvent e) {
        Group selected = groups.selected();
        if (selected == null || selected.global()) return;
        editGroup(selected);
    }

    @UiHandler("deleteGroupButton")
    void deleteGroupClicked(ClickEvent e) {
        if (userCanEditGroup(groups.selected()))
            deleteGroup(groups.selected());
    }

    /**
     * Returns whether the current user is allowed to edit
     * the given group
     *
     * Currently, global groups cannot be edited from the
     * editor.
     */
    private boolean userCanEditGroup(Group group) {
        return !group.global();
    }

    /**
     * Add the given principal to the currently selected directory
     */
    private void grantNewPermissions(Principal principal) {
        if (principal == null || permissions.containsPrincipal(principal)) return;
        editSession.add(new GrantPermission(new Permission(currentDirectory(), principal, Access.Read)));
        refresh();
    }

    private void grantChangedPermissions(Principal principal, Access access) {
        if (principal == null) return;
        editSession.add(new GrantPermission(new Permission(currentDirectory(), principal, access)));
        refresh();
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
        refresh();
    }

    private void deleteGroup(Group group) {
        if (group == null) return;
        editSession.add(new RemoveGroup(group));
        refresh();
    }

    private void newGroup() {
        final GroupEditor ge = new GroupEditor(userRetrieval);
        ge.newGroup(editSession.repository());
        ge.addCloseHandler(new CloseHandler<Boolean>() {
            public void onClose(CloseEvent<Boolean> event) {
                if (event.getTarget()) {
                    editSession.addAll(ge.modifications());
                    refresh();
                }
                Subversive.display(EditorWindow.this);
            }
        });
        Subversive.display(ge);
    }

    private void editGroup(Group g) {
        if (g == null) return;

        final GroupEditor ge = new GroupEditor(userRetrieval);
        ge.load(editSession.groupDefinition(g));
        ge.addCloseHandler(new CloseHandler<Boolean>() {
            public void onClose(CloseEvent<Boolean> event) {
                if (event.getTarget()) {
                    editSession.addAll(ge.modifications());
                    refresh();
                }
                Subversive.display(EditorWindow.this);
            }
        });

        Subversive.display(ge);
    }

    private GroupDecorator groupDecorator = new GroupDecorator() {
        public String getModificationSummary(Group group) {
            if (group == null) return "";
            GroupModifications gm = editSession.groupModifications(group);

            StringBuilder sb = new StringBuilder();
            if (gm.additions() > 0) sb.append("+").append(gm.additions());
            if (gm.removals() > 0) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("-").append(gm.removals());
            }
            return sb.toString();
        }
    };

    private DirectoryDecorator directoryDecorator = new DirectoryDecorator() {
        public void decorateDirectoryNode(Directory directory, TreeItem ti) {
            Panel p = new FlowPanel();
            p.setStyleName("gwt-treeRow");
            if (editSession.directoryAssigned(directory))
                p.add(new Image(Resources.The.folderImage()));
            else
                p.add(new Image(Resources.The.noFolderImage()));
            p.add(new InlineLabel(ti.getText()));
            ti.setWidget(p);
        }
    };
}
