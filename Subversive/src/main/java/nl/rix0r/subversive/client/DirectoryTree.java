package nl.rix0r.subversive.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
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
import java.util.List;
import nl.rix0r.subversive.client.DirectoryStructure.Dir;
import nl.rix0r.subversive.client.DirectoryStructure.DirWalker;
import nl.rix0r.subversive.subversion.Directory;

/**
 * @author rix0rrr
 */
public class DirectoryTree extends Composite implements HasSelectionHandlers<Directory> {
    interface MyUiBinder extends UiBinder<Widget, DirectoryTree> { };
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    @UiField Tree tree;
    @UiField TextBox customDirField;

    private DirectoryStructure directoryStructure = new DirectoryStructure();
    private DirectoryDecorator decorator;
    private Directory selectedDirectory;
    private String repository;

    public DirectoryTree() {
        initWidget(uiBinder.createAndBindUi(this));
        tree.setAnimationEnabled(true);
        tree.addSelectionHandler(new SelectionHandler<TreeItem>() {
            public void onSelection(SelectionEvent<TreeItem> event) {
                selectionChanged(directoryFromTreeItem(event.getSelectedItem()));
            }
        });
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
    public void add(List<Directory> directories) {
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
        treeItemToSelect = null;
        directoryStructure.walk(new DirWalker<TreeItem>() {
            public TreeItem walk(TreeItem parent, Dir child) {
                String name = child.directory().lastSegment();
                if (name.equals("/")) name = "root (/)";

                TreeItem ti = parent == null ? tree.addItem(name) : parent.addItem(name);
                ti.setUserObject(child.directory());

                if (decorator != null) decorator.decorateDirectoryNode(child.directory(), ti);
                if (child.directory().equals(selectedDirectory)) treeItemToSelect = ti;
                return ti;
            }
        });

        expandAll();

        // Either select the root (without firing an event)
        // or reselect the previously selected directory.
        if (treeItemToSelect == null && tree.getItemCount() > 0) {
            treeItemToSelect = tree.getItem(0);
            selectedDirectory = directoryFromTreeItem(treeItemToSelect);
        }

        if (treeItemToSelect != null)
            tree.setSelectedItem(treeItemToSelect, false);
    }

    public void expandAll() {
        for (int i = 0; i < tree.getItemCount(); i++)
            expand(tree.getItem(i));
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
