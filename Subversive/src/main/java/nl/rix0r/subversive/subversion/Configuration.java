package nl.rix0r.subversive.subversion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * An in-memory representation of a Subversion configuration
 *
 * For external clients, a Configuration is read-only. It can only
 * be modified by applying Modifications to it.
 *
 * @author rix0rrr
 */
public class Configuration {
    private Map<Group, GroupDefinition> definitions = new HashMap<Group, GroupDefinition>();
    private HashSet<Permission>         permissions = new HashSet<Permission>();

    protected GroupDefinition group(Group group) {
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

    /**
     * Return a subset of Configuration restricted to the given repository
     *
     * Global groups are also returned.
     */
    public Configuration subset(String repository) {
        Configuration ret = new Configuration();

        for (GroupDefinition def: definitions.values())
            if (def.appliesToRepository(repository))
                addGroupDefinition(new GroupDefinition(def));

        for (Permission perm: permissions)
            if (perm.appliesToRepository(repository))
                ret.addPermission(new Permission(perm));

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