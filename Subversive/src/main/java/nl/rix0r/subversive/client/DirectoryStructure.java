
package nl.rix0r.subversive.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import nl.rix0r.subversive.subversion.Directory;

/**
 * Class that infers directory structures
 *
 * @author rix0rrr
 */
public class DirectoryStructure {
    private Dir root;

    /**
     * Add the given list of directories into the structure
     *
     * All directories must be in the same repository.
     *
     * Returns whether a new directory was effectively added.
     */
    public boolean add(Collection<Directory> directories) {
        boolean change = false;
        for (Directory directory: directories)
            change |= add(directory);
        return change;
    }

    /**
     * Add the given directory into the structure
     *
     * All directories must be in the same repository.
     *
     * Returns whether a new directory was effectively added.
     */
    public boolean add(Directory directory) {
        boolean change = ensureRoot(directory);

        List<Directory> rootPath = new ArrayList<Directory>();
        directory.addRootPath(rootPath);

        change |= root.add(rootPath, 1);
        return change;
    }

    public void clear() {
        root = null;
    }

    /**
     * Ensure that we have a root node, create one if necessary
     *
     * Also verifies whether the repositories match
     */
    private boolean ensureRoot(Directory directory) {
        boolean change = root == null;
        if (root == null) root = new Dir(new Directory(directory.repository(), "/")).real(true);
        if (!directory.repository().equals(root.directory.repository()))
                throw new RuntimeException("All directories must be in the same repository. Expected " + root.directory.repository() + ", got " + directory.repository());
        return change;
    }

    public Directory root() {
        if (root == null) return null;
        return root.directory();
    }

    public void walk(DirWalker<?> walker) {
        if (root != null)
            root.visit(null, walker);
    }

    /**
     * A node in the directory tree
     */
    public class Dir {
        private final String name;
        private final Directory directory;
        private final Map<Directory, Dir> children = new TreeMap<Directory, Dir>();
        private boolean real;

        public Dir(Directory directory) {
            this.directory = directory;
            this.name      = directory.lastSegment();
        }

        public boolean real() {
            return real;
        }

        public Dir real(boolean real) {
            this.real = real;
            return this;
        }

        public Directory directory() {
            return directory;
        }

        /**
         * Add the given directory as a child and process the rest of the stack
         *
         * Directory is indicated by an index into the list. If the directory
         * already exists, processing is continued. The directory that is finally
         * inserted is returned.
         */
        public boolean add(List<Directory> directories, int i) {
            if (i == directories.size()) return false;

            boolean change = false;
            Directory child = directories.get(i);
            if (!children.containsKey(child)) {
                children.put(child, new Dir(child));
                change = true;
            }

            change |= children.get(child).add(directories, i + 1);
            return change;
        }

        public <T> void visit(T parentValue, DirWalker<T> walker) {
            T value = walker.walk(parentValue, this);
            for (Dir child: children.values())
                child.visit(value, walker);
        }
    }

    /**
     * Walk over the director tree
     *
     * At every level, the function can return a value which will be used
     * to call the lower levels.
     */
    public interface DirWalker<T> {
        public T walk(T parent, Dir child);
    }
}
