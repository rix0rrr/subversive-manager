
package nl.rix0r.subversive.subversion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author rix0rrr
 */
public class EditSession implements Serializable {
    private List<Modification> modifications = new ArrayList<Modification>();
    private String repository;
    private Configuration baseConfiguration;

    // The following fields are only for in-memory bookkeeping
    transient private Configuration currentConfiguration;
    transient private Map<Group, GroupModifications> modificationTable = new HashMap<Group, GroupModifications>();
    transient private Set<Directory> assignedDirectories = new HashSet<Directory>();

    public EditSession(String repository, Configuration baseConfiguration) {
        this.repository        = repository;
        this.baseConfiguration = baseConfiguration;
    }

    /**
     * Empty constructor for the benefit of GWT serialization
     */
    public EditSession() {
    }

    /**
     * Returns the name of the repository currently being edited
     */
    public String repository() {
        return repository;
    }

    /**
     * Return the current version of the configuration
     *
     * This configuration has all modifications applied
     */
    public Configuration configuration() {
        if (currentConfiguration == null) updateCurrentConfiguration(); // First time
        return currentConfiguration;
    }

    /**
     * Updates the current configuration
     *
     * Found by applying all modifications to the base configuration.
     * Also calculates some stats that we show in the EditorWindow.
     */
    private void updateCurrentConfiguration() {
        modificationTable.clear();

        Configuration latest = baseConfiguration.copy();
        for (Modification m: modifications) {
            countGroupModification(m);
            m.apply(latest);
        }
        currentConfiguration = latest;

        detectAssignedDirectories();
    }

    /**
     * Add all directories that have permissions to a set for visualization purposes
     */
    private void detectAssignedDirectories() {
        assignedDirectories.clear();
        for (Permission p: currentConfiguration.permissions(null, null))
            assignedDirectories.add(p.directory());
    }

    public boolean directoryAssigned(Directory directory) {
        return assignedDirectories.contains(directory);
    }

    /**
     * Count the modification in the group table if it is a group modification
     */
    private void countGroupModification(Modification m) {
        Group group = null;
        boolean add = false;
        if (m instanceof AddUserToGroup) {
            group = ((AddUserToGroup)m).group();
            add = true;
        }
        if (m instanceof RemoveUserFromGroup)
            group = ((RemoveUserFromGroup)m).group();

        if (group != null) {
            if (!modificationTable.containsKey(group)) modificationTable.put(group, new GroupModifications());
            if (add)
                modificationTable.get(group).addition();
            else
                modificationTable.get(group).removal();
        }
    }

    public GroupModifications groupModifications(Group group) {
        if (!modificationTable.containsKey(group)) return new GroupModifications();
        return modificationTable.get(group);
    }

    /**
     * Returns all directories that have configured permissions
     */
    public Set<Directory> configuredDirectories() {
        Set<Directory> ret = new HashSet<Directory>();
        for (Permission p: configuration().permissions(null, null))
            ret.add(p.directory());
        return ret;
    }

    /**
     * Return all groups to which permissions can be assigned
     *
     * Returns both global and local groups.
     */
    public List<Group> availableGroups() {
        List<Group> ret = new ArrayList<Group>();
        for (GroupDefinition gd: configuration().groupDefinitions())
            ret.add(gd.group());
        return ret;
    }

    /**
     * Return the group definition for the given group
     */
    public GroupDefinition groupDefinition(Group group) {
        return configuration().group(group);
    }

    /**
     * Return all permissions that apply to the given directory
     */
    public Collection<Permission> permissions(Directory directory) {
        return configuration().permissions(directory, null);
    }

    /**
     * Add a new modification
     */
    public void add(Modification modification) {
        modifications.add(modification);
        updateCurrentConfiguration();
    }

    public void addAll(List<Modification> modifications) {
        this.modifications.addAll(modifications);
        updateCurrentConfiguration();
    }

    /**
     * Remove the last modification
     */
    public void undo() {
        if (modifications.size() > 0) {
            modifications.remove(modifications.size() - 1);
            updateCurrentConfiguration();
        }
    }

    /**
     * Returns whether undo makes sense
     */
    public boolean canUndo() {
        return modifications.size() > 0;
    }

    /**
     * Return a list of all current modifications
     */
    public List<Modification> modifications() {
        return Collections.unmodifiableList(modifications);
    }

    /**
     * Class that represents the modifications to a single group
     */
    public static class GroupModifications implements Serializable {
        private int additions = 0;
        private int removals  = 0;

        public void addition() {
            additions++;
        }

        public void removal() {
            removals++;
        }

        public int additions() {
            return additions;
        }

        public int removals() {
            return removals;
        }
    }
}
