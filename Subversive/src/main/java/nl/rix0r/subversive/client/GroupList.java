
package nl.rix0r.subversive.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import java.util.ArrayList;
import java.util.List;
import nl.rix0r.subversive.client.generic.SelectableTable;
import nl.rix0r.subversive.client.generic.SelectableTable.Row;
import nl.rix0r.subversive.subversion.Group;

/**
 *
 * @author rix0rrr
 */
public class GroupList extends Composite {
    interface MyUiBinder extends UiBinder<Widget, GroupList> { };
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    @UiField(provided=true) GroupSelectList groups;
    @UiField TextBox searchField;

    private List<Group> baseGroups = new ArrayList<Group>();

    public GroupList() {
        groups = new GroupSelectList();
        initWidget(uiBinder.createAndBindUi(this));
    }

    public void setGroups(List<Group> groups) {
        this.baseGroups = new ArrayList<Group>(groups);
        updateModelOnFilter();
    }

    @UiHandler("searchField")
    void keyUp(KeyUpEvent e) {
        updateModelOnFilter();
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

    public static class GroupSelectList extends SelectableTable<Group> {
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
        }
    }

}
