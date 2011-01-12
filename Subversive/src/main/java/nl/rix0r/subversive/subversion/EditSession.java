
package nl.rix0r.subversive.subversion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author rix0rrr
 */
public class EditSession implements Serializable {
    private List<Modification> modifications = new ArrayList<Modification>();

    private String repository;
    private Configuration baseConfiguration;
    private Configuration currentConfiguration;

    public EditSession(String repository, Configuration baseConfiguration) {
        this.repository = repository;
        this.baseConfiguration = baseConfiguration;
        this.currentConfiguration = baseConfiguration;
    }

    /**
     * Empty constructor for the benefit of GWT serialization
     */
    protected EditSession() {
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
        return currentConfiguration;
    }

    /**
     * Updates the current configuration
     *
     * Found by applying all modifications to the base configuration.
     */
    private void updateCurrentConfiguration() {
        Configuration latest = baseConfiguration.copy();
        for (Modification m: modifications)
            m.apply(latest);
        currentConfiguration = latest;
    }

    /**
     * Returns all directories that have configured permissions
     */
    public List<Directory> configuredDirectories() {
        List<Directory> ret = new ArrayList<Directory>();
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
}
