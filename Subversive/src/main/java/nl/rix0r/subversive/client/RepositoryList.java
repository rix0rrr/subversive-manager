
package nl.rix0r.subversive.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import java.util.List;

/**
 * @author rix0rrr
 */
public class RepositoryList extends Composite {
    interface MyUiBinder extends UiBinder<Widget, RepositoryList> { };
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    @UiField VerticalPanel repoList;
    @UiField Button refreshButton;

    public RepositoryList(List<String> repositories) {
        initWidget(uiBinder.createAndBindUi(this));

        repoList.setStyleName("subversive-RepoGrid");
        setStyleName("subversive-RepositoryList");

        setRepositories(repositories);
    }

    public void setRepositories(List<String> repositories) {
        repoList.clear();

        for (String repository: repositories) {
            Hyperlink h = new Hyperlink(repository, repository);
            repoList.add(h);
        }

        if (repositories.isEmpty()) {
            repoList.add(new Label("No repositories for you to manage."));
        }
    }

    @UiHandler("refreshButton")
    void refreshClick(ClickEvent e) {
        History.fireCurrentHistoryState();
    }
}
