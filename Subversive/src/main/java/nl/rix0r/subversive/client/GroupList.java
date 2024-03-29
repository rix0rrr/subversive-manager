
package nl.rix0r.subversive.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.HasOpenHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import nl.rix0r.subversive.client.generic.SelectableTable;
import nl.rix0r.subversive.client.generic.SelectableTable.Row;
import nl.rix0r.subversive.subversion.Group;

/**
 *
 * @author rix0rrr
 */
public class GroupList extends Composite implements
        HasSelectionHandlers<Group>, HasOpenHandlers<Group> {

    interface MyUiBinder extends UiBinder<Widget, GroupList> { };
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    @UiField(provided=true) GroupSelectList groups;
    @UiField TextBox searchField;

    private List<Group> baseGroups = new ArrayList<Group>();
    private GroupDecorator decorator;

    public GroupList() {
        groups = new GroupSelectList();
        initWidget(uiBinder.createAndBindUi(this));

        groups.addDoubleClickHandler(new DoubleClickHandler() {
            public void onDoubleClick(DoubleClickEvent event) {
                fireOpenEvent();
                event.preventDefault();
                event.stopPropagation();
            }
        });

        groups.getModel().setComparator(new Comparator<Group>() {
            public int compare(Group o1, Group o2) {
                // Sort local before global
                if (o1.global() != o2.global())
                    return o1.global() ? 1 : -1;

                // Otherwise by name as per uuush
                return o1.compareTo(o2);
            }
        });
    }

    public void setGroups(List<Group> groups) {
        this.baseGroups = new ArrayList<Group>(groups);
        updateModelOnFilter();
    }

    @UiHandler("searchField")
    void keyUp(KeyUpEvent e) {
        if (e.getNativeKeyCode() == 13)
            fireOpenEvent();
        else
            updateModelOnFilter();
    }

    public Group selected() {
        return groups.selected();
    }

    /**
     * Make sure all rows that match the filter remain in the model
     */
    private void updateModelOnFilter() {
        groups.getModel().replace(matchingGroups(searchField.getValue()));
        groups.setSelectedRow(0);
    }

    private List<Group> matchingGroups(String filter) {
        List<Group> ret = new ArrayList<Group>(baseGroups.size());
        for (Group g: baseGroups)
            if (g.matches(filter))
                ret.add(g);
        return ret;
    }

    public HandlerRegistration addSelectionHandler(SelectionHandler<Group> handler) {
        return groups.addSelectionHandler(handler);
    }

    public HandlerRegistration addOpenHandler(OpenHandler<Group> handler) {
        return addHandler(handler, OpenEvent.getType());
    }

    public GroupDecorator getDecorator() {
        return decorator;
    }

    public void setDecorator(GroupDecorator decorator) {
        this.decorator = decorator;
    }

    /**
     * Fire a selection event on the selected element
     */
    private void fireOpenEvent() {
        if (selected() != null)
            OpenEvent.fire(this, selected());
    }

    public class GroupSelectList extends SelectableTable<Group> {
        public GroupSelectList() {
            getColumnFormatter().setWidth(0, "16px");
        }

        @Override
        protected void initializeRow(Group element, Row row) {
            row.widgets.put("image", IconProvider.principal(element));
        }

        @Override
        protected void renderRow(int i, Group element, Row row) {
            setWidget(i, 0, row.widgets.get("image"));
            setText(i, 1, element.toString());
            if (decorator != null)
                setText(i, 2, decorator.getModificationSummary(element));
        }
    }

    /**
     * Can be used to show additional information in the group list
     */
    public interface GroupDecorator {
        public String getModificationSummary(Group group);
    }
}
