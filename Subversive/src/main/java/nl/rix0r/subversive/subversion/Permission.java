
package nl.rix0r.subversive.subversion;

import java.io.Serializable;

/**
 * Permissions granted to a user for a directory
 * 
 * It is a combination of a Directory, Principal and Rights, and directly
 * corresponds to a line in the svn_access.conf file.
 *
 * @author rix0rrr
 */
public class Permission implements Comparable, Serializable {
    private Principal principal;
    private Access    access;
    private Directory directory;

    public Permission(Directory directory, Principal principal, Access access) {
        this.directory = directory;
        this.principal = principal;
        this.access    = access;
    }

    public Permission(Permission proto) {
        this(proto.directory(), proto.principal(), proto.access());
    }

    public Access access() {
        return access;
    }

    public Directory directory() {
        return directory;
    }

    public Principal principal() {
        return principal;
    }

    public boolean appliesToRepository(String repository) {
        return directory.appliesToRepository(repository);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Permission other = (Permission) obj;
        if (this.principal != other.principal && (this.principal == null || !this.principal.equals(other.principal))) {
            return false;
        }
        if (this.directory != other.directory && (this.directory == null || !this.directory.equals(other.directory))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + (this.principal != null ? this.principal.hashCode() : 0);
        hash = 73 * hash + (this.directory != null ? this.directory.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return access + " to " + principal + " at " + directory;
    }

    public int compareTo(Object t) {
        Permission that = (Permission)t;

        int comp = 0;
        if (comp == 0) comp = this.directory.compareTo(that.directory);
        if (comp == 0) comp = this.principal.toString().compareTo(that.principal.toString());

        return comp;
    }
}
