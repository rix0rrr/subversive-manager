
package nl.rix0r.subversive.subversion;

/**
 *
 * @author rix0rrr
 */
public class GrantPermission implements Modification {
    private Permission permission;

    public GrantPermission(Permission permission) {
        this.permission = permission;
    }

    public void apply(Configuration configuration) {
        if (!configuration.validPrincipal(permission.principal()))
            throw new ModificationException("Unable to grant " + permission + ": group is gone.");

        configuration.addPermission(permission);
    }

    public String repository() {
        return permission.directory().repository();
    }

    @Override
    public String toString() {
        return "Grant " + permission;
    }
}
