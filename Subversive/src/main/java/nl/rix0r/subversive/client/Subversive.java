package nl.rix0r.subversive.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import java.util.ArrayList;
import java.util.List;
import nl.rix0r.subversive.client.LoginDialog.LoginHandler;
import nl.rix0r.subversive.subversion.Directory;
import nl.rix0r.subversive.subversion.EditSession;
import nl.rix0r.subversive.subversion.Modification;

/**
 *
 * @author rix0rrr
 */
public class Subversive implements EntryPoint, LoginHandler, HistoryListener {
    private ConfigEditorServiceAsync configEditor;
    private UserRetrievalServiceAsync userService;
    private ServerInfoServiceAsync serverInfo;
    private CachingUserRetrieval userRetrieval;

    private LoginDialog loginDialog = new LoginDialog(this);
    private String username;
    private String password;
    private static Label errorLabel = new Label("");

    public Subversive() {
        configEditor = GWT.create(ConfigEditorService.class);
        userService  = GWT.create(UserRetrievalService.class);
        serverInfo   = GWT.create(ServerInfoService.class);
        //StubConfigEditor stub = new StubConfigEditor();
        //configEditor = stub;
        //userService = stub;

        // Add a caching layer to the user retrieval service
        userRetrieval = new CachingUserRetrieval(userService);

        errorLabel.setStyleName("gwt-errorMessage");
        errorLabel.setVisible(false);
    }

    public void onModuleLoad() {
        RootPanel.get("app").getElement().setInnerHTML("");

        History.addHistoryListener(this);
        retrieveBrandingImage();
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

    private void openEditor(final String repository) {
        setError("");
        configEditor.begin(repository, username, password, new Callback<EditSession>() {
            public void onSuccess(EditSession result) {
                final EditorWindow ew = new EditorWindow(result, userRetrieval);
                ew.addCloseHandler(new CloseHandler<EditSession>() {
                    public void onClose(CloseEvent<EditSession> event) {
                        commitSession(event.getTarget());
                    }
                });

                display(ew);

                // Start loading directories, forward to editor when retrieved
                configEditor.listDirectories(repository, username, password, new AsyncCallback<List<Directory>>() {
                    public void onFailure(Throwable caught) {
                        ew.directoryRetrievalFailed(caught.getMessage());
                    }

                    public void onSuccess(List<Directory> result) {
                        ew.directoriesRetrieved(result);
                    }
                });
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

        // Have to make a copy: the UnmodifiableList is not serializable
        List<Modification> mods = new ArrayList<Modification>(session.modifications());
        configEditor.apply(mods, username, password, new Callback<List<String>>() {
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
        errorLabel.setVisible(message != null && !message.equals(""));
    }

    private void retrieveBrandingImage() {
        serverInfo.getBrandingImage(new AsyncCallback<String[]>() {
            public void onFailure(Throwable caught) {
                // Too bad
            }

            public void onSuccess(String[] result) {
                String image = result.length > 0 ? result[0] : "";
                String link  = result.length > 1 ? result[1] : "";

                if (image != null && !image.equals("")) {
                    String imageTag = "<img src=\"" + image + "\" class=\"brandingImage\">";

                    if (link != null && !link.equals("")) {
                        imageTag = "<a href=\"" + link + "\">" + imageTag + "</a>";
                    }

                    RootPanel.get("header").add(new HTML(imageTag));
                }
            }
        });
    }

    abstract public static class Callback<T> implements AsyncCallback<T> {
        public void onFailure(Throwable caught) {
            GWT.log(caught.getMessage(), caught);
            setError(caught.getMessage());
        }
    }
}
