
package nl.rix0r.subversive.client.generic;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author rix0rrr
 */
abstract public class SelectableTable<T> extends FlexTable {
    private int selectedRow       = -1;
    private ListModel<T> model    = new ListModel<T>();
    private Map<T, Row> rows = new HashMap<T, Row>();

    public SelectableTable() {
        setCellSpacing(0);

        addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                Cell c = getCellForEvent(event);
                if (c != null) setSelectedRow(c.getRowIndex());
            }
        });

        model.addValueChangeHandler(new ValueChangeHandler() {
            public void onValueChange(ValueChangeEvent event) {
                update();
            }
        });
    }

    public ListModel<T> getModel() {
        return model;
    }

    private void updateCellStyles() {
        for (int i = 0; i < getRowCount(); i++) {
            getRowFormatter().setStyleName(i, i == selectedRow ? "selected" : "");
        }
    }

    public void setSelectedRow(int rowIndex, boolean fireEvent) {
        if (selectedRow < 0) selectedRow = -1;
        if (selectedRow >= getRowCount()) selectedRow = getRowCount() - 1;
        this.selectedRow = rowIndex;
        updateCellStyles();
    }

    public void setSelectedRow(int rowIndex) {
        setSelectedRow(rowIndex, true);
    }

    public int getSelectedRow() {
        return selectedRow;
    }

    public void update() {
        Set<T> removedRows = new HashSet<T>(rows.keySet());

        for (int i = 0; i < model.size(); i++) {
            T el = model.get(i);
            renderRow(i, el, rowFor(el));
            removedRows.remove(el);
        }

        // Items remaining in removedRows are gone
        for (T el: removedRows)
            rows.remove(el);

        // Remove remaining rows in the model
        while (getRowCount() > model.size())
            removeRow(getRowCount() - 1);

        if (selectedRow >= getRowCount()) selectedRow = getRowCount() - 1;
        updateCellStyles();
    }

    private Row rowFor(T element) {
        if (rows.containsKey(element)) return rows.get(element);
        Row r = new Row();
        initializeRow(element, r);
        rows.put(element, r);
        return r;
    }

    abstract protected void initializeRow(T element, Row row);
    abstract protected void renderRow(int i, T element, Row row);

    /**
     * An object that descendant classes can use to store persistent
     * row information in, such as widgets and user objects
     */
    public class Row {
        public final Map<String, Widget> widgets = new HashMap<String, Widget>();
        public Object userObject;
    }
}