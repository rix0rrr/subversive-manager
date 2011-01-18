
package nl.rix0r.subversive.subversion;

/**
 * Collection of users
 *
 * Groups can be global or scoped to a repository. If they are scoped to the
 * repository that is being edited, the group can be edited as well.
 *
 * @author rix0rrr
 */
public class Group implements Principal {
    private String repository;
    private String name;

    public Group() { }

    public Group(String name) {
        this.repository = "";
        this.name       = name;
    }

    public Group(String repository, String name) {
        this.repository = repository;
        this.name       = name;
    }

    public String name() {
        return name;
    }

    public String repository() {
        return repository;
    }

    public boolean global() {
        return repository.equals("");
    }

    public boolean appliesToRepository(String repository) {
        return global() || repository().equals(repository);
    }

    public boolean matches(String text) {
        return text == null || name.toLowerCase().contains(text.toLowerCase());
    }

    @Override
    public String toString() {
        return name + (global() ? " (global)" : "");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Group other = (Group) obj;
        if ((this.repository == null) ? (other.repository != null) : !this.repository.equals(other.repository)) {
            return false;
        }
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (this.repository != null ? this.repository.hashCode() : 0);
        hash = 97 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }
}
