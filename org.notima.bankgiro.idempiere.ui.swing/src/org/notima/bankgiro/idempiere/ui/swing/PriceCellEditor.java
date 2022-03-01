package org.notima.bankgiro.idempiere.ui.swing;

import java.awt.Component;

import javax.swing.*;
import javax.swing.table.*;

public class PriceCellEditor extends AbstractCellEditor implements TableCellEditor {

	private JTextField	m_field = new JTextField();
	
	public PriceCellEditor() {
		super();
	}

	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {
		
		String text = "";
		m_field.setText(text);
		return(m_field);
	}

	public Object getCellEditorValue() {
		return m_field.getText();
	}
	
	

}
