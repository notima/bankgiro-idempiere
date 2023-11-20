/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.notima.bankgiro.idempiere.ui.swing;

import java.awt.Component;
import javax.swing.*;
import javax.swing.table.*;
import org.compiere.grid.ed.VDate;

/**
 *
 * @author daniel
 */
public class DateCellEditor extends AbstractCellEditor implements TableCellEditor {

    private VDate       dateValue = new VDate();

    @Override
    public Object getCellEditorValue() {
        return dateValue.getValue();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        dateValue.setValue(value);
        return(dateValue);
    }

}
