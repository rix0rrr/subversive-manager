
package nl.rix0r.subversive.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import java.util.List;

/**
 *
 * @author rix0rrr
 */
public class RepositoryList extends Composite implements ApplicationScreen {
    interface MyUiBinder extends UiBinder<Widget, RepositoryList> { };
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    @UiField Label errorLabel;
    @UiField VerticalPanel repoList;

    public RepositoryList(List<String> repositories) {
        initWidget(uiBinder.createAndBindUi(this));

        setRepositories(repositories);
    }

    public void setRepositories(List<String> repositories) {
        repoList.clear();

        for (String repository: repositories) {
            Hyperlink h = new Hyperlink(repository, repository);
            repoList.add(h);
        }
    }

    public void displayError(String message) {
        Window.alert(message);
    }
}
