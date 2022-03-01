/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.notima.bankgiro.adempiere;

import javax.swing.table.*;
import org.compiere.util.*;

/**
 *
 * @author Daniel Tamm
 */
public class LbPayStatusCellRenderer extends DefaultTableCellRenderer {

    /**
	 * 
	 */
	private static final long serialVersionUID = 5985383768346272946L;
    public String[] m_payStatus = new String[] {
        Msg.translate(Env.getCtx(), "All"),
        Msg.translate(Env.getCtx(), "Not.Paid"),
        Msg.translate(Env.getCtx(), "In.Transit"),
        Msg.translate(Env.getCtx(), "Paid"),
        Msg.translate(Env.getCtx(), "Not.Approved")
    };

    public LbPayStatusCellRenderer() {
        super();
    }

    public String getPayStatusName(int payStatus) {
        return(m_payStatus[payStatus]);
    }

    @Override
	public void setValue(Object value) {
        if (value instanceof Integer) {
            int i = ((Integer)value).intValue();
            if (i>=0 && i<m_payStatus.length) {
                this.setText(m_payStatus[i]);
            } else {
                this.setText(value.toString());
            }
        } else {
            this.setText("");
        }
	}


}
