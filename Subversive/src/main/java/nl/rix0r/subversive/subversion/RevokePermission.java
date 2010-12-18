
package nl.rix0r.subversive.subversion;

/**
 *
 * @author rix0rrr
 */
public class RevokePermission implements Modification {
    private Permission permission;

    public RevokePermission(Permission permission) {
        this.permission = permission;
    }

    public void apply(Configuration configuration) {
        if (!configuration.validPrincipal(permission.principal()))
            throw new ModificationException("Unable to revoke " + permission + ": group is gone.");

        configuration.removePermission(permission);
    }

    @Override
    public String toString() {
        return "Revoke " + permission;
    }
}
