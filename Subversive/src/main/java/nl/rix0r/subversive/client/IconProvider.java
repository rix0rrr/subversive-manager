
package nl.rix0r.subversive.client;

import com.google.gwt.user.client.ui.Image;
import nl.rix0r.subversive.subversion.Group;
import nl.rix0r.subversive.subversion.Principal;
import nl.rix0r.subversive.subversion.User;

/**
 *
 * @author rix0rrr
 */
public class IconProvider {
    public static Image principal(Principal principal) {
        if (principal instanceof User) return new Image(Resources.The.userImage());
        if (principal instanceof Group) return new Image(Resources.The.groupImage());
        return new Image(Resources.The.anonymousImage());
    }
}
