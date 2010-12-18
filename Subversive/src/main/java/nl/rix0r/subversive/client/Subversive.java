
package nl.rix0r.subversive.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

/**
 *
 * @author rix0rrr
 */
public class Subversive implements EntryPoint {

    public void onModuleLoad() {
        RootPanel.get().add(new Label("Hoi"));
    }
}
