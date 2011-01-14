package nl.rix0r.subversive.subversion;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * An in-memory representation of a Subversion configuration
 *
 * For external clients, a Configuration is read-only. It can only
 * be modified by applying Modifications to it.
 *
 * @author rix0rrr
 */
public class Configuration implements Serializable {
    private Map<Group, GroupDefinition> definitions = new HashMap<Group, GroupDefinition>();
    private HashSet<Permission>         permissions = new HashSet<Permission>();

    public GroupDefinition group(Group group) {
        return definitions.get(group);
    }

    protected void clear() {
        definitions.clear();
        permissions.clear();
    }

    /**
     * Add a definition for the given group if one doesn't yet exist
     */
    protected GroupDefinition addGroup(Group group) {
        if (!definitions.containsKey(group)) definitions.put(group, new GroupDefinition(group));
        return group(group);
    }

    protected void addUserToGroup(Group group, User user) {
        addGroup(group).addUser(user);
    }

    protected void removeGroup(Group group) {
        definitions.remove(group);
    }

    /**
     * Add or overwrite the given permission record
     */
    protected void addPermission(Permission permission) {
        // Deleting first removes an old one that classifies as the same from
        // the set.
        permissions.remove(permission);
        permissions.add(permission);
    }

    protected void removePermission(Permission permission) {
        permissions.remove(permission);
    }

    private void addGroupDefinition(GroupDefinition groupDef) {
        definitions.put(groupDef.group(), groupDef);
    }

    public Collection<GroupDefinition> groupDefinitions() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    /**
     * Return a subset of Configuration restricted to the given repository
     *
     * Global groups are also returned. If repository is null, no
     * filtering is done.
     */
    public Configuration subset(String repository) {
        Configuration ret = new Configuration();

        for (GroupDefinition def: definitions.values())
            if (repository == null || def.appliesToRepository(repository))
                ret.addGroupDefinition(new GroupDefinition(def));

        for (Permission perm: permissions)
            if (repository == null || perm.appliesToRepository(repository))
                ret.addPermission(new Permission(perm));

        return ret;
    }

    /**
     * Returns a copy of the configuration
     */
    public Configuration copy() {
        return subset(null);
    }

    /**
     * Return a subset of all permissions
     *
     * Filter either by directory, or principal, or both (set values to
     * null to not filter on that attribute).
     */
    public Set<Permission> permissions(Directory directory, Principal principal) {
        Set<Permission> ret = new TreeSet<Permission>();

        for (Permission p: permissions)
            if ((directory == null || p.directory().equals(directory))
                &&
                (principal == null || p.principal().equals(principal)))
                ret.add(p);

        return ret;
    }

    /**
     * Whether this is a valid principal
     *
     * The only principal that is invalid, is a Group for which no definition
     * exists.
     */
    public boolean validPrincipal(Principal principal) {
        // princial is group => group exists

        return !(principal instanceof Group) || (group((Group)principal) != null);
    }
}