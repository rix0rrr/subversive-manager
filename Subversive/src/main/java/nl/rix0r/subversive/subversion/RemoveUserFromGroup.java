
package nl.rix0r.subversive.subversion;

/**
 * @author rix0rrr
 */
public class RemoveUserFromGroup implements Modification {
    private Group group;
    private User  user;

    public RemoveUserFromGroup(User user, Group group) {
        this.group = group;
        this.user  = user;
    }

    public void apply(Configuration configuration) {
        GroupDefinition g = configuration.group(group);
        if (g == null)
            throw new ModificationException("Unable to remove " + user + " from group " + group + ": group is gone.");

        g.removeUser(user);
    }

    @Override
    public String toString() {
        return "Remove " + user + " from group " + group;
    }
}
