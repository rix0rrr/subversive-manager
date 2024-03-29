
package nl.rix0r.subversive.subversion;

/**
 * Add a new group to a repository
 *
 * @author rix0rrr
 */
public class NewGroup implements Modification {
    private Group group;

    public NewGroup() { }

    public NewGroup(Group group) {
        this.group = group;
    }

    public void apply(Configuration configuration) {
        configuration.addGroup(group);
    }

    public String repository() {
        return group.repository();
    }

    @Override
    public String toString() {
        return "Create group " + group;
    }
}
