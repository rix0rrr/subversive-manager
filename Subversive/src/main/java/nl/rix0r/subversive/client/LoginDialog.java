package nl.rix0r.subversive.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 *
 * @author rix0rrr
 */
public class LoginDialog extends Composite implements ApplicationScreen {
    interface MyUiBinder extends UiBinder<Widget, LoginDialog> { };
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    private LoginHandler loginHandler;

    @UiField TextBox username;
    @UiField TextBox password;
    @UiField Button loginButton;
    @UiField Label errorLabel;

    public LoginDialog(LoginHandler loginHandler) {
        initWidget(uiBinder.createAndBindUi(this));

        this.loginHandler = loginHandler;
    }

    @UiHandler("loginButton")
    void handleClick(ClickEvent e) {
        doLogin();
    }

    @UiHandler("username")
    void handleUserKey(KeyUpEvent e) {
        handleKey(e);
    }

    @UiHandler("password")
    void handlePasswordKey(KeyUpEvent e) {
        handleKey(e);
    }

    void handleKey(KeyUpEvent e) {
        displayError("");
        if (e.getNativeKeyCode() == KeyCodes.KEY_ENTER /* enter */)
            doLogin();
    }

    private void doLogin() {
        loginHandler.tryLogin(username.getText(), password.getText());
    }

    public void displayError(String message) {
        errorLabel.setText(message);
    }

    public interface LoginHandler {
        public void tryLogin(String username, String password);
    }
}