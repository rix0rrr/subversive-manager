
package nl.rix0r.subversive.client;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import nl.rix0r.subversive.client.PermissionsList.PrincipalAccess;
import nl.rix0r.subversive.client.generic.SelectableTable;
import nl.rix0r.subversive.client.generic.SelectableTable.Row;
import nl.rix0r.subversive.subversion.Access;
import nl.rix0r.subversive.subversion.Permission;
import nl.rix0r.subversive.subversion.Principal;

/**
 * @author rix0rrr
 */
public class PermissionsList extends SelectableTable<PrincipalAccess> {
    public PermissionsList() {
        getColumnFormatter().setWidth(0, "16px");
        getColumnFormatter().setWidth(2, "100px");
    }

    /**
     * Add all permissions in the given list to the table
     *
     * Directories are not checked.
     */
    public void add(Iterable<Permission> permissions) {
        for (Permission permission: permissions)
            addPrincipal(permission.principal(), permission.access());
    }

    public void addPrincipal(Principal principal, Access access) {
        getModel().add(new PrincipalAccess(principal, access));
    }

    public boolean containsPrincipal(Principal principal) {
        for (PrincipalAccess pa: getModel().all())
            if (pa.principal.equals(principal))
                return true;
        return false;
    }

    public void clear() {
        getModel().clear();
    }

    @Override
    protected void initializeRow(final PrincipalAccess element, Row row) {
        final AccessDropdown dd = new AccessDropdown(element.access);

        // Upon changing the dropdown, save back to the model record
        dd.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                element.access = dd.getAccess();
            }
        });

        row.widgets.put("image", IconProvider.principal(element.principal));
        row.widgets.put("dropdown", dd);
    }

    public PrincipalAccess getSelected() {
        return getModel().get(getSelectedRow());
    }

    public void remove(PrincipalAccess pa) {
        getModel().remove(pa);
    }

    @Override
    protected void renderRow(int i, PrincipalAccess element, Row row) {
        setWidget(i, 0, row.widgets.get("image"));
        setText(i, 1, element.principal.toString());
        setWidget(i, 2, row.widgets.get("dropdown"));
    }

    public static class PrincipalAccess {
        public final Principal principal;
        public Access access;

        public PrincipalAccess(Principal principal, Access access) {
            this.principal = principal;
            this.access    = access;
        }
    }

}
