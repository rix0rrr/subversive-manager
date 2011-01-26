
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
import nl.rix0r.subversive.subversion.Group;
import nl.rix0r.subversive.subversion.GroupDefinition;
import nl.rix0r.subversive.subversion.Modification;
import nl.rix0r.subversive.subversion.NewGroup;
import nl.rix0r.subversive.subversion.RemoveGroup;
import nl.rix0r.subversive.subversion.RemoveUserFromGroup;
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

    public GroupEditor(CachingUserRetrieval retrieval) {
        initWidget(uiBinder.createAndBindUi(this));
        allUsers.setUserRetrieval(retrieval);
    }

    public void newGroup(String repository) {
        baseGroup = new Group(repository, "");
    }

    public void load(GroupDefinition gd) {
        baseGroup = gd.group();
        groupName.setText(baseGroup.name());
        baseUsers = new HashSet<User>(gd.users());
        added     = new HashSet<User>();
        removed   = new HashSet<User>();
        refresh();
    }

    private void refresh() {
        groupUsers.setUsers(effectiveUsers());
    }

    private Collection<User> effectiveUsers() {
        Set<User> effective = new HashSet<User>(baseUsers);
        for (User u: added) effective.add(u);
        for (User u: removed) effective.remove(u);
        return effective;
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

    public List<Modification> modifications() {
        List<Modification> ret = new ArrayList<Modification>();

        if (baseGroup != null && !baseGroup.name().equals(groupName()))
            renamedModifications(ret);
        else
            inplaceModifications(ret);

        return ret;
    }

    /**
     * List of modifications when the group is renamed or a new group is created
     *
     * In that case: remove the old group, entirely add the group with the
     * new name
     */
    private void renamedModifications(List<Modification> into) {
        if (!baseGroup.name().equals(""))
            into.add(new RemoveGroup(baseGroup));

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
}