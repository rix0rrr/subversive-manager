
package nl.rix0r.subversive.subversion;

/**
 * Remove group from a repository
 *
 * @author rix0rrr
 */
public class RemoveGroup implements Modification {
    private Group group;

    public RemoveGroup() { }

    public RemoveGroup(Group group) {
        this.group = group;
    }

    public void apply(Configuration configuration) {
        configuration.removeGroup(group);
    }

    public String repository() {
        return group.repository();
    }

    @Override
    public String toString() {
        return "Remove group " + group;
    }
}
