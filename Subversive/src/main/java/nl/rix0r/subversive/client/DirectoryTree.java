package nl.rix0r.subversive.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.Widget;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import nl.rix0r.subversive.client.DirectoryStructure.Dir;
import nl.rix0r.subversive.client.DirectoryStructure.DirWalker;
import nl.rix0r.subversive.subversion.Directory;

/**
 * @author rix0rrr
 */
public class DirectoryTree extends Composite implements
        HasSelectionHandlers<Directory>, OpenHandler<TreeItem>, CloseHandler<TreeItem> {

    interface MyUiBinder extends UiBinder<Widget, DirectoryTree> { };
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    @UiField Tree tree;
    @UiField TextBox customDirField;

    private DirectoryStructure directoryStructure = new DirectoryStructure();
    private DirectoryDecorator decorator;
    private Directory selectedDirectory;
    private String repository;

    /**
     * Directories of nodes that should be expanded
     * 
     * Persistent between refreshes.
     */
    private Set<Directory> expandedDirs = new HashSet<Directory>();

    /**
     * A mapping of directories to the nodes that visualize them
     */
    private Map<Directory, TreeItem> whichNode = new HashMap<Directory, TreeItem>();

    public DirectoryTree() {
        initWidget(uiBinder.createAndBindUi(this));
        tree.setAnimationEnabled(true);

        // Fire an event when the user selects a directory
        tree.addSelectionHandler(new SelectionHandler<TreeItem>() {
            public void onSelection(SelectionEvent<TreeItem> event) {
                selectionChanged(directoryFromTreeItem(event.getSelectedItem()));
            }
        });

        // Remember when the user expands/collapses a node, to restore the
        // state after a refresh.
        tree.addOpenHandler(this);
        tree.addCloseHandler(this);
    }

    public void setRepository(String repository) {
        directoryStructure.clear();
        this.repository = repository;
        directoryStructure.add(new Directory(repository, "/"));
    }

    private Directory directoryFromTreeItem(TreeItem ti) {
        if (ti == null) return null;
        return (Directory)ti.getUserObject();
    }

    public DirectoryDecorator getDecorator() {
        return decorator;
    }

    public void setDecorator(DirectoryDecorator decorator) {
        this.decorator = decorator;
    }

    public HandlerRegistration addSelectionHandler(SelectionHandler<Directory> handler) {
        return addHandler(handler, SelectionEvent.getType());
    }

    public void onOpen(OpenEvent<TreeItem> event) {
        expandedDirs.add(directoryFromTreeItem(event.getTarget()));
    }

    public void onClose(CloseEvent<TreeItem> event) {
        expandedDirs.remove(directoryFromTreeItem(event.getTarget()));
    }

    public Directory selected() {
        return selectedDirectory;
    }

    /**
     * Fire the currently selected directory as an event
     */
    private void selectionChanged(Directory newSelected) {
        selectedDirectory = newSelected;
        SelectionEvent.fire(this, selectedDirectory);
    }

    /**
     * Load this tree from the given list of directories, inferring
     * directories that are not actually in the given set
     */
    public void add(Collection<Directory> directories) {
        if (directoryStructure.add(directories))
            refresh();
    }

    public void add(Directory directory) {
        if (directoryStructure.add(directory))
            refresh();
    }

    private TreeItem treeItemToSelect;

    public void refresh() {
        tree.clear();
        whichNode.clear();

        treeItemToSelect = null;
        directoryStructure.walk(new DirWalker<TreeItem>() {
            public TreeItem walk(TreeItem parent, Dir child) {
                String name = child.directory().lastSegment();
                if (name.equals("/")) name = "root (/)";

                TreeItem ti = parent == null ? tree.addItem(name) : parent.addItem(name);
                ti.setUserObject(child.directory());
                whichNode.put(child.directory(), ti); // Register the directory->node mapping

                if (decorator != null) decorator.decorateDirectoryNode(child.directory(), ti);
                if (child.directory().equals(selectedDirectory)) treeItemToSelect = ti;
                return ti;
            }
        });

        expandNodes();

        // Either select the root (without firing an event)
        // or reselect the previously selected directory.
        if (treeItemToSelect == null && tree.getItemCount() > 0) {
            treeItemToSelect = tree.getItem(0);
            selectedDirectory = directoryFromTreeItem(treeItemToSelect);
        }

        if (treeItemToSelect != null)
            tree.setSelectedItem(treeItemToSelect, false);
    }

    /**
     * Add the given directory and all of its parents to the list of
     * expanded nodes.
     */
    public void makeVisible(Directory directory) {
        if (_makeVisible(directory))
            expandNodes();
    }

    public void makeVisible(Collection<Directory> directories) {
        boolean change = false;
        for (Directory directory: directories)
            change |= _makeVisible(directory);

        if (change)
            expandNodes();
    }

    private boolean _makeVisible(Directory directory) {
        boolean change = false;
        if (!directory.root()) {
            change |= expandedDirs.add(directory.parent());
            change |= _makeVisible(directory.parent());
        }
        return change;
    }

    /**
     * For all directory nodes that should be expanded, expand the
     * appropriate TreeItem.
     */
    private void expandNodes() {
        for (Directory dir: expandedDirs) {
            TreeItem ti = whichNode.get(dir);
            if (ti != null)
                ti.setState(true, false);
        }
    }

    public void expand(TreeItem ti) {
        ti.setState(true);
        for (int i = 0; i < ti.getChildCount(); i++)
            expand(ti.getChild(i));
    }

    private void injectDirectory(String path) {
        Directory base = directoryStructure.root();
        if (base == null) return;

        add(new Directory(base.repository(), path));
    }

    @UiHandler("customDirField")
    void onCustomDirKey(KeyUpEvent e) {
        if (e.getNativeKeyCode() == 13) {
            injectDirectory(sanitizeDirectory(customDirField.getText()));
            customDirField.setText("");
        }
    }

    private String sanitizeDirectory(String dir) {
        return dir.trim().replace("\\", "/");
    }

    public interface DirectoryDecorator {
        public void decorateDirectoryNode(Directory directory, TreeItem ti);
    }
}
