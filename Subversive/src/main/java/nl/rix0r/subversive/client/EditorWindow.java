
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
import com.google.gwt.event.logical.shared.ValueChangeHandler;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
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

    private enum Tab {
        Users, Groups
    }

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
    private CachingUserRetrieval userRetrieval;
    private GroupEditor currentGroupEditor;

    public EditorWindow(EditSession editSession, CachingUserRetrieval userRetrieval) {
        initWidget(uiBinder.createAndBindUi(this));
        wireUp();
        this.userRetrieval = userRetrieval;
        users.setUserRetrieval(userRetrieval);
        groups.setDecorator(groupDecorator);
        directoryTree.setDecorator(directoryDecorator);

        setEditSession(editSession);
    }

    public void setEditSession(EditSession editSession) {
        this.editSession = editSession;
        directoryTree.setRepository(editSession.repository());
        refresh();
    }

    private Set<Directory> lastConfiguredDirectories;

    public void refresh() {
        Set<Directory> configuredDirectories = editSession.configuredDirectories();
        directoryTree.add(configuredDirectories);
        directoryTree.makeVisible(configuredDirectories);

        if (lastConfiguredDirectories == null
                || !lastConfiguredDirectories.equals(configuredDirectories)) {
            directoryTree.refresh();
            lastConfiguredDirectories = configuredDirectories;
        }

        repoTitle.setText(editSession.repository());
        groups.setGroups(editSession.availableGroups());
        refreshPermissions();
        refreshButtonStates();
    }

    private Tab selectedTab() {
        int ix = tabpanel.getSelectedIndex();
        if (tabpanel.getTabWidget(ix).getTitle().toLowerCase().startsWith("group"))
            return Tab.Groups;
        else
            return Tab.Users;
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
                (selectedTab() == Tab.Users && users.selected() != null && !permissions.containsPrincipal(users.selected()))
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
        permissions.add(expandUserInfo(editSession.permissions(directory)));

        // Doesn't really have anything to do with that but
        // always seems to go hand-in-hand.
        refreshButtonStates();
    }

    /**
     * Return a list of permissions with user info expanded
     */
    private List<Permission> expandUserInfo(Collection<Permission> permissions) {
        List<Permission> ret = new ArrayList<Permission>();
        for (Permission perm: permissions) {
            if (perm.principal() instanceof User)
                ret.add(new Permission(
                        perm.directory(),
                        userRetrieval.expandUser((User)perm.principal()),
                        perm.access()));
            else
                ret.add(perm);
        }
        userRetrieval.finished(doRefresh);

        return ret;
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

    @UiHandler("backButton")
    void cancelClick(ClickEvent e) {
        Window.Location.replace("#"); // Go back to the overview without leaving a history token
    }

    @UiHandler("removeButton")
    void handleRemove(ClickEvent e) {
        PrincipalAccess pa = permissions.getSelected();
        if (pa != null) revokePermissions(pa.principal);
    }

    @UiHandler("assignButton")
    void handleAssignClick(ClickEvent e) {
        if (selectedTab() == Tab.Users)
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
        // Double-click should enter edit -- it's usually what you want to do with groups
        // and it's consistent with the behaviour in the permissions list.
        editGroup(e.getTarget());
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
    void permissionsDoubleClicked(OpenEvent<PrincipalAccess> e) {
        if (!(e.getTarget().principal instanceof Group)) return;
        editGroup((Group)e.getTarget().principal);
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
        editGroup(groups.selected());
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
        currentGroupEditor = new GroupEditor(userRetrieval);
        currentGroupEditor.newGroup(editSession.repository());
        currentGroupEditor.addCloseHandler(new CloseHandler<Boolean>() {
            public void onClose(CloseEvent<Boolean> event) {
                if (event.getTarget()) {
                    editSession.add(currentGroupEditor.modification());
                    refresh();
                }
                Subversive.display(EditorWindow.this);
                currentGroupEditor = null;
            }
        });
        Subversive.display(currentGroupEditor);
    }

    private void editGroup(Group g) {
        if (g == null || g.global()) return;

        final GroupEditor ge = new GroupEditor(userRetrieval);
        ge.load(editSession.configuration(), g);
        ge.addCloseHandler(new CloseHandler<Boolean>() {
            public void onClose(CloseEvent<Boolean> event) {
                if (event.getTarget()) {
                    editSession.add(ge.modification());
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

    private Runnable doRefresh = new Runnable() {
        public void run() {
            refresh();
        }
    };
}
