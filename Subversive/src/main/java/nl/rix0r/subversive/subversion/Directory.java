
package nl.rix0r.subversive.subversion;

import java.io.Serializable;
import java.util.List;

/**
 * A directory in a repository
 *
 * @author rix0rrr
 */
public class Directory implements Comparable, Serializable {
    private String repository;
    private String path;

    public Directory(String repository, String path) {
        this.repository = repository;
        this.path       = makeAbsolute(noTrailingSlash(path));
    }

    public String repository() {
        return repository;
    }

    public String path() {
        return path;
    }

    /**
     * Return the last segment (name) of the path, or "/"
     * if this directory is the root
     */
    public String lastSegment() {
        if (root()) return "/";
        int ix = path.lastIndexOf("/");
        return path.substring(ix + 1);
    }

    /**
     * Add the root path to the current directory to the given list
     */
    public void addRootPath(List<Directory> into) {
        if (!root()) parent().addRootPath(into);
        into.add(this);
    }

    /**
     * Returns the parent directory of this directory
     *
     * Returns the root if this directory is already the root
     */
    public Directory parent() {
        if (root()) return this;
        int ix = path.lastIndexOf("/");
        return new Directory(repository, path.substring(0, ix));
    }

    /**
     * Returns true iff this is the root path (/)
     */
    public boolean root() {
        return path.equals("/");
    }

    private String makeAbsolute(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    private String noTrailingSlash(String path) {
        if (path.equals("")) return path;
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    public boolean appliesToRepository(String repository) {
        return repository().equals(repository);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Directory other = (Directory) obj;
        if ((this.repository == null) ? (other.repository != null) : !this.repository.equals(other.repository)) {
            return false;
        }
        if ((this.path == null) ? (other.path != null) : !this.path.equals(other.path)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.repository != null ? this.repository.hashCode() : 0);
        hash = 37 * hash + (this.path != null ? this.path.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return path;
    }

    public int compareTo(Object t) {
        Directory that = (Directory)t;

        int comp = 0;
        if (comp == 0) comp = this.repository.compareTo(that.repository);
        if (comp == 0) comp = this.path.compareTo(that.path);

        return comp;
    }
}
