
package nl.rix0r.subversive.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import java.util.List;

/**
 * @author rix0rrr
 */
public class ResultDialog extends Composite implements HasCloseHandlers<Void> {
    interface MyUiBinder extends UiBinder<Widget, ResultDialog> { };
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    @UiField Label summary;
    @UiField VerticalPanel messages;
    @UiField Button okButton;

    public ResultDialog() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    public HandlerRegistration addCloseHandler(CloseHandler<Void> handler) {
        return addHandler(handler, CloseEvent.getType());
    }

    public void setMessages(List<String> msgs) {
        summary.setText(msgs.isEmpty()
                ? "Your modifications have been succesfully applied."
                : "There were problems applying (some of) your modifications. See below for details.");

        messages.clear();
        for (String msg: msgs)
            messages.add(new Label(msg));
    }

    @UiHandler("okButton")
    void okClicked(ClickEvent e) {
        CloseEvent.fire(this, null);
    }
}
