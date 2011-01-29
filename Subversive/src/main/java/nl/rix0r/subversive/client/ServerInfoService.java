
package nl.rix0r.subversive.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * Service for getting access to server settings
 *
 * @author rix0rrr
 */
@RemoteServiceRelativePath("configeditor")
public interface ServerInfoService extends RemoteService {

    /**
     * Return the URL and link to the branding image
     */
    public String[] getBrandingImage();
}
