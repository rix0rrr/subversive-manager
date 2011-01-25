
package nl.rix0r.subversive.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ClientBundle.Source;
import com.google.gwt.resources.client.ImageResource;

/**
 * @author rix0rrr
 */
public interface Resources extends ClientBundle {
    public static final Resources The =  GWT.create(Resources.class);

    @Source("folder.png")
    public ImageResource folderImage();

    @Source("folderlight.png")
    public ImageResource noFolderImage();

    @Source("user.png")
    public ImageResource userImage();

    @Source("group.png")
    public ImageResource groupImage();

    @Source("anonymous.png")
    public ImageResource anonymousImage();
}
