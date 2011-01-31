
package nl.rix0r.subversive.client.generic;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.user.client.ui.TextBox;

/**
 * @author rix0rrr
 */
public class HintTextBox extends TextBox {
    private String hint = "";
    private boolean empty = true;
    private boolean focused = false;

    public HintTextBox() {
        addFocusHandler(new FocusHandler() {
            public void onFocus(FocusEvent event) {
                enter();
            }
        });

        addBlurHandler(new BlurHandler() {
            public void onBlur(BlurEvent event) {
                leave();
            }
        });

        leave();
    }

    public void setHint(String hint) {
        this.hint = hint;
        leave();
    }

    public String getHint() {
        return hint;
    }

    @Override
    public String getText() {
        if (empty) return "";
        return super.getText();
    }

    @Override
    public void setText(String text) {
        if (!focused && (text == null || text.equals(""))) {
            empty = true;
            text  = hint;
        }
        super.setText(text);
        applyEmptyStyles();
    }

    private void enter() {
        focused = true;
        if (empty) setValue("", false); // If the thing is supposed to be empty, make sure it is
        empty = false; // empty conflicts with getText

        applyEmptyStyles();
    }

    private void leave() {
        empty = super.getText() == null || super.getText().equals("");
        if (empty) setValue(hint, false);
        focused = false;

        applyEmptyStyles();
    }

    private void applyEmptyStyles() {
        setStyleDependentName("hint", empty && !focused);
    }
}
