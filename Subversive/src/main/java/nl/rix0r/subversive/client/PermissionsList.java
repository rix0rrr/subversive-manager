
package nl.rix0r.subversive.client;

import com.google.gwt.user.client.ui.FlexTable;
import java.util.List;
import nl.rix0r.subversive.subversion.Access;
import nl.rix0r.subversive.subversion.Anonymous;
import nl.rix0r.subversive.subversion.Group;
import nl.rix0r.subversive.subversion.Permission;
import nl.rix0r.subversive.subversion.Principal;
import nl.rix0r.subversive.subversion.User;

/**
 * @author rix0rrr
 */
public class PermissionsList extends FlexTable {

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
        int row = getRowCount();

        setText(row, 0, principalImage(principal));
        setText(row, 1, principal.toString());
        setWidget(row, 2, new AccessDropdown(access));
    }

    private String principalImage(Principal principal) {
        if (principal instanceof User) return "user";
        if (principal instanceof Group) return "group";
        if (principal instanceof Anonymous) return "anon";
        return "?";
    }
}
