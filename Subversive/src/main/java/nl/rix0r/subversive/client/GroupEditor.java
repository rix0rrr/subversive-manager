
package nl.rix0r.subversive.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import nl.rix0r.subversive.subversion.AddUserToGroup;
import nl.rix0r.subversive.subversion.Configuration;
import nl.rix0r.subversive.subversion.GrantPermission;
import nl.rix0r.subversive.subversion.Group;
import nl.rix0r.subversive.subversion.Modification;
import nl.rix0r.subversive.subversion.NewGroup;
import nl.rix0r.subversive.subversion.Permission;
import nl.rix0r.subversive.subversion.RemoveGroup;
import nl.rix0r.subversive.subversion.RemoveUserFromGroup;
import nl.rix0r.subversive.subversion.RevokePermission;
import nl.rix0r.subversive.subversion.SingleModification;
import nl.rix0r.subversive.subversion.User;

/**
 * Group Editor
 *
 * Throws a CloseEvent with true or false depending on whether the dialog
 * is closed with Save or Cancel.
 *
 * @author rix0rrr
 */
public class GroupEditor extends Composite implements HasCloseHandlers<Boolean> {
    interface MyUiBinder extends UiBinder<Widget, GroupEditor> { };
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    @UiField TextBox groupName;
    @UiField UserList groupUsers;
    @UiField UserList allUsers;
    @UiField Button assignButton;
    @UiField Button removeButton;

    private Group baseGroup;
    private Set<User> baseUsers = Collections.emptySet();
    private Set<User> added   = new HashSet<User>();
    private Set<User> removed = new HashSet<User>();
    private CachingUserRetrieval userRetrieval;

    private Configuration configuration; // For when the group is renamed

    public GroupEditor(CachingUserRetrieval retrieval) {
        userRetrieval = retrieval;
        initWidget(uiBinder.createAndBindUi(this));
        allUsers.setUserRetrieval(retrieval);

        setStyleName("subversive-GroupEditor");
    }

    public void newGroup(String repository) {
        baseGroup = new Group(repository, "");
    }

    public void load(Configuration configuration, Group group) {
        this.configuration = configuration;
        baseGroup = group;
        groupName.setText(baseGroup.name());
        baseUsers = new HashSet<User>(configuration.group(group).users());
        added     = new HashSet<User>();
        removed   = new HashSet<User>();
        refresh();
    }

    public void refresh() {
        groupUsers.setUsers(expandUsers(effectiveUsers()));
    }

    private Collection<User> effectiveUsers() {
        Set<User> effective = new HashSet<User>(baseUsers);
        for (User u: added) effective.add(u);
        for (User u: removed) effective.remove(u);
        return effective;
    }

    private Collection<User> expandUsers(Collection<User> users) {
        Set<User> expanded = new HashSet<User>();
        for (User user: users)
            expanded.add(userRetrieval.expandUser(user));

        userRetrieval.finished(doRefresh);

        return expanded;
    }

    private void updateButtonStates() {
        assignButton.setEnabled(allUsers.selected() != null);
        removeButton.setEnabled(groupUsers.selected() != null);
    }

    @UiHandler("groupUsers")
    void groupSelection(SelectionEvent<User> e) {
        updateButtonStates();
    }

    @UiHandler("allUsers")
    void allSelection(SelectionEvent<User> e) {
        updateButtonStates();
    }

    @UiHandler("allUsers")
    void allOpen(OpenEvent<User> e) {
        assignSelected();
    }

    @UiHandler("assignButton")
    void assignClicked(ClickEvent e) {
        assignSelected();
    }

    @UiHandler("removeButton")
    void removeClicked(ClickEvent e) {
        removeSelected();
    }

    private void assignSelected() {
        User u = allUsers.selected();
        if (u == null) return;

        if (removed.contains(u))
            removed.remove(u);
        else
            added.add(u);
        refresh();
    }

    private void removeSelected() {
        User u = groupUsers.selected();
        if (u == null) return;

        if (added.contains(u))
            added.remove(u);
        else
            removed.add(u);
        refresh();
    }

    @UiHandler("saveButton")
    void saveClick(ClickEvent e) {
        if (groupName().equals("")) {
            Subversive.setError("Enter a group name.");
            return;
        }

        CloseEvent.fire(this, true);
    }

    @UiHandler("cancelButton")
    void cancelClick(ClickEvent e) {
        CloseEvent.fire(this, false);
    }

    public HandlerRegistration addCloseHandler(CloseHandler<Boolean> handler) {
        return addHandler(handler, CloseEvent.getType());
    }

    private String groupName() {
        String t = groupName.getText();
        if (t == null) return "";
        return t.trim();
    }

    public Modification modification() {
        return new SingleModification(modifications());
    }

    private List<Modification> modifications() {
        List<Modification> ret = new ArrayList<Modification>();

        if (baseGroup.name().equals(""))
            newModifications(ret);
        else if (!baseGroup.name().equals(groupName()))
            renameModifications(ret);
        else
            inplaceModifications(ret);

        return ret;
    }

    /**
     * List of modifications when the group is renamed
     *
     * In that case: remove the old group, entirely add the group with the
     * new name. Also, replace all permissions for the old group with
     * permissions for the new group.
     */
    private void renameModifications(List<Modification> into) {
        // Remove all permissions and the old group
        for (Permission perm: configuration.permissions(null, baseGroup))
            into.add(new RevokePermission(perm));
        into.add(new RemoveGroup(baseGroup));

        // Add the new group and the new permissions
        Group newGroup = new Group(baseGroup.repository(), groupName());
        into.add(new NewGroup(newGroup));
        for (Permission perm: configuration.permissions(null, baseGroup)) 
            into.add(new GrantPermission(perm.change(newGroup)));

        // Add the users to the new group
        for (User u: effectiveUsers())
            into.add(new AddUserToGroup(u, newGroup));

    }

    /**
     * Modifications for when this is for creating a new group
     */
    private void newModifications(List<Modification> into) {
        Group newGroup = new Group(baseGroup.repository(), groupName());
        into.add(new NewGroup(newGroup));

        for (User u: effectiveUsers())
            into.add(new AddUserToGroup(u, newGroup));
    }

    /**
     * Return the set of additions and removals
     */
    private void inplaceModifications(List<Modification> into) {
        for (User u: removed)
            into.add(new RemoveUserFromGroup(u, baseGroup));

        for (User u: added)
            into.add(new AddUserToGroup(u, baseGroup));
    }

    private Runnable doRefresh = new Runnable() {
        public void run() {
            refresh();
        }
    };
}