
package nl.rix0r.subversive.subversion;

/**
 * @author rix0rrr
 */
public class AddUserToGroup implements Modification {
    private Group group;
    private User  user;

    public AddUserToGroup(User user, Group group) {
        this.group = group;
        this.user  = user;
    }

    public void apply(Configuration configuration) {
        GroupDefinition g = configuration.group(group);
        if (g == null)
            throw new ModificationException("Unable to add " + user + " to group " + group + ": group is gone.");

        g.addUser(user);
    }

    public Group group() {
        return group;
    }

    public String repository() {
        return group.repository();
    }

    @Override
    public String toString() {
        return "Add " + user + " to group " + group;
    }
}
