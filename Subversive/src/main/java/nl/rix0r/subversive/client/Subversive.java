
package nl.rix0r.subversive.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import java.util.List;
import nl.rix0r.subversive.client.LoginDialog.LoginHandler;
import nl.rix0r.subversive.subversion.EditSession;

/**
 *
 * @author rix0rrr
 */
public class Subversive implements EntryPoint, LoginHandler, HistoryListener {
    private ConfigEditorServiceAsync configEditor;
    private UserRetrievalServiceAsync userService;

    private LoginDialog loginDialog = new LoginDialog(this);
    private String username;
    private String password;
    private static Label errorLabel = new Label("");

    public Subversive() {
        StubConfigEditor stub = new StubConfigEditor();
        configEditor = stub;
        userService = stub;

        errorLabel.setStyleName("gwt-errorMessage");
    }

    public void onModuleLoad() {
        RootPanel.get("app").getElement().setInnerHTML("");

        History.addHistoryListener(this);
        showLoginDialog();
    }

    public static void display(Widget screen) {
        RootPanel.get("app").clear();
        RootPanel.get("app").add(errorLabel);
        RootPanel.get("app").add(screen);
    }

    public void tryLogin(String username, String password) {
        this.username = username;
        this.password = password;

        dispatchOnToken(History.getToken());
    }

    private void showLoginDialog() {
        setError("");
        display(loginDialog);
    }

    private void showRepositories() {
        setError("");
        configEditor.myRepositories(username, password, new Callback<List<String>>() {
            public void onSuccess(List<String> result) {
                display(new RepositoryList(result));
            }
        });
    }

    private void openEditor(String repository) {
        setError("");
        configEditor.begin(repository, username, password, new Callback<EditSession>() {
            public void onSuccess(EditSession result) {
                EditorWindow ew = new EditorWindow(result, userService);
                ew.addCloseHandler(new CloseHandler<EditSession>() {
                    public void onClose(CloseEvent<EditSession> event) {
                        commitSession(event.getTarget());
                    }
                });

                display(ew);
            }
        });
    }

    /**
     * Save an edit session back to the server and display the results
     */
    private void commitSession(EditSession session) {
        setError("");
        if (session.modifications().isEmpty()) {
            // Nothing to do, just go back to listing
            History.newItem("");
            return;
        }

        configEditor.apply(session.modifications(), username, password, new Callback<List<String>>() {
            public void onSuccess(List<String> result) {
                ResultDialog rd = new ResultDialog();
                rd.setMessages(result);
                rd.addCloseHandler(new CloseHandler<Void>() {
                    public void onClose(CloseEvent<Void> event) {
                        History.newItem("");
                    }
                });
                display(rd);
            }
        });
    }

    private void dispatchOnToken(String historyToken) {
        if (historyToken == null) historyToken = "";
        if (historyToken.startsWith("#")) historyToken = historyToken.substring(1);

        if (historyToken.equals("")) {
            showRepositories();
            return;
        }

        openEditor(historyToken);
    }

    public void onHistoryChanged(String historyToken) {
        dispatchOnToken(historyToken);
    }

    public static void setError(String message) {
        errorLabel.setText(message);
    }

    abstract public static class Callback<T> implements AsyncCallback<T> {
        public void onFailure(Throwable caught) {
            setError(caught.getMessage());
        }
    }
}
