package nl.rix0r.subversive.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
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

    public DirectoryTree() {
        initWidget(uiBinder.createAndBindUi(this));
        tree.setAnimationEnabled(true);
        tree.addSelectionHandler(new SelectionHandler<TreeItem>() {
            public void onSelection(SelectionEvent<TreeItem> event) {
                fireSelected();
            }
        });
    }

    public HandlerRegistration addSelectionHandler(SelectionHandler<Directory> handler) {
        return addHandler(handler, SelectionEvent.getType());
    }

    public Directory selected() {
        TreeItem selected = tree.getSelectedItem();
        return selected == null ? null : (Directory)selected.getUserObject();
    }

    /**
     * Fire the currently selected directory as an event
     */
    private void fireSelected() {
        SelectionEvent.fire(this, selected());
    }

    /**
     * Load this tree from the given list of directories, inferring
     * directories that are not actually in the given set
     */
    public void load(List<Directory> directories) {
        tree.clear();

        DirectoryStructure ds = new DirectoryStructure();
        ds.add(directories);
        ds.walk(new DirWalker<TreeItem>() {
            public TreeItem walk(TreeItem parent, Dir child) {
                String name = child.directory().lastSegment();
                if (name.equals("/")) name = "root (/)";
                if (child.real()) name += " (r)";

                TreeItem ti = parent == null ? tree.addItem(name) : parent.addItem(name);
                ti.setUserObject(child.directory());

                return ti;
            }
        });

        expandAll();

        // Select the root
        if (tree.getItemCount() > 0) {
            tree.setSelectedItem(tree.getItem(0));
        }
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
}
