
package nl.rix0r.subversive.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import java.util.List;
import nl.rix0r.subversive.client.LoginDialog.LoginHandler;

/**
 *
 * @author rix0rrr
 */
public class Subversive implements EntryPoint, LoginHandler {
    private ConfigEditorServiceAsync configEditor;

    private LoginDialog loginDialog = new LoginDialog(this);
    private String username;
    private String password;

    private ApplicationScreen currentScreen;

    public Subversive() {
        configEditor = new StubConfigEditor();
    }

    public void onModuleLoad() {
        RootPanel.get("app").getElement().setInnerHTML("");

        display(loginDialog);
    }

    public void display(ApplicationScreen screen) {
        RootPanel.get("app").clear();
        RootPanel.get("app").add(screen);
        this.currentScreen = screen;
    }

    public void tryLogin(String username, String password) {
        this.username = username;
        this.password = password;
        showRepositories();
    }

    private void showRepositories() {
        configEditor.myRepositories(username, password, new Callback<List<String>>() {
            public void onSuccess(List<String> result) {
                display(new RepositoryList(result));
            }
        });
    }

    abstract private class Callback<T> implements AsyncCallback<T> {
        public void onFailure(Throwable caught) {
            if (currentScreen == null) display(loginDialog);
            currentScreen.displayError(caught.getMessage());
        }
    }
}
