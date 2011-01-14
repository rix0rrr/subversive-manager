
package nl.rix0r.subversive.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
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

    public GroupList() {
        groups = new GroupSelectList();
        initWidget(uiBinder.createAndBindUi(this));
    }

    public void setGroups(List<Group> groups) {
        this.groups.getModel().replace(groups);
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
            System.out.println("renderRow");
            setWidget(i, 0, row.widgets.get("image"));
            setText(i, 1, element.toString());
        }
    }

}
