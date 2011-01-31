
package nl.rix0r.subversive.client;

import com.google.gwt.user.client.ui.ListBox;
import nl.rix0r.subversive.subversion.Access;

/**
 * @author rix0rrr
 */
public class AccessDropdown extends ListBox {
    public AccessDropdown() {
        super(false);

        addItem("Read",       Access.Read.name());
        addItem("Read/Write", Access.ReadWrite.name());
        addItem("Revoke",     Access.Revoke.name());
    }

    public AccessDropdown(Access selected) {
        this();

        setAccess(selected);
    }

    public void setAccess(Access access) {
        for (int i = 0; i < getItemCount(); i++)
            if (getValue(i).equals(access.name())) {
                setSelectedIndex(i);
                return;
            }
    }

    public Access getAccess() {
        return Access.valueOf(getValue(getSelectedIndex()));
    }
}
