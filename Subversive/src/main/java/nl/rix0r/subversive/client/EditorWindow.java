
package nl.rix0r.subversive.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import nl.rix0r.subversive.subversion.Directory;
import nl.rix0r.subversive.subversion.EditSession;

/**
 * @author rix0rrr
 */
public class EditorWindow extends Composite {
    interface MyUiBinder extends UiBinder<Widget, EditorWindow> { };
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    @UiField Button saveButton;
    @UiField PermissionsList permissions;
    @UiField Label repoTitle;
    @UiField DirectoryTree directoryTree;
    @UiField Label selectedDirectory;

    private EditSession editSession;

    public EditorWindow(EditSession editSession) {
        initWidget(uiBinder.createAndBindUi(this));
        this.editSession = editSession;

        wireUp();

        repoTitle.setText(editSession.repository());
        directoryTree.load(editSession.configuredDirectories());
    }

    private void wireUp() {
        directoryTree.addSelectionHandler(new SelectionHandler<Directory>() {
            public void onSelection(SelectionEvent<Directory> event) {
                showDirectory(event.getSelectedItem());
            }
        });
    }

    /**
     * Load the permissions for the given directory
     */
    private void showDirectory(Directory directory) {
        if (directory == null) return;

        selectedDirectory.setText(directory.path());
        permissions.removeAllRows();
        permissions.add(editSession.permissions(directory));
    }
}
