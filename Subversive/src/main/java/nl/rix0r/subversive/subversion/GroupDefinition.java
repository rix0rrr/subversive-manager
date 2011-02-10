
package nl.rix0r.subversive.subversion;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Specification of the members of a group
 */
public class GroupDefinition implements Serializable, Comparable {
    private Group group;
    private Set<User> users = new HashSet<User>();

    public GroupDefinition() { }

    public GroupDefinition(Group group) {
        this.group = group;
    }

    public GroupDefinition(GroupDefinition proto) {
        this.group = proto.group();
        for (User u: proto.users())
            addUser(u);
    }

    public Group group() {
        return group;
    }

    public Set<User> users() {
        return Collections.unmodifiableSet(users);
    }

    protected void addUser(User user) {
        users.add(user);
    }

    protected void removeUser(User user) {
        users.remove(user);
    }

    public boolean contains(User user) {
        return users.contains(user);
    }

    public boolean appliesToRepository(String repository) {
        return group.appliesToRepository(repository);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GroupDefinition other = (GroupDefinition) obj;
        if (this.group != other.group && (this.group == null || !this.group.equals(other.group))) {
            return false;
        }
        if (this.users != other.users && (this.users == null || !this.users.equals(other.users))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 47 * hash + (this.group != null ? this.group.hashCode() : 0);
        hash = 47 * hash + (this.users != null ? this.users.hashCode() : 0);
        return hash;
    }

    public int compareTo(Object o) {
        return this.group().compareTo(((GroupDefinition)o).group());
    }
}
