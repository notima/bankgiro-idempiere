/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.notima.bankgiro.adempiere;

import java.beans.PropertyChangeSupport;
import java.sql.*;
import javax.swing.table.*;
import java.util.*;
import org.compiere.model.*;
import org.compiere.util.*;

/**
 * Table model for displaying receivables
 *
 * @author Daniel Tamm
 */
public class ReceivablesTableModel extends AbstractTableModel {

    /**
	 * 
	 */
	private static final long serialVersionUID = -832614464614389842L;
	public static final int COL_SELECT = 0;
    public static final int COL_PAYSTATUS = 1;
    public static final int COL_BPARTNER = 2;
    public static final int COL_INVOICEID = 3;
    public static final int COL_OCRREF = 4;
    public static final int COL_INVDATE = 5;
    public static final int COL_DUEDATE = 6;
    public static final int COL_PAYDATE = 7;
    public static final int COL_AMOUNTDUE = 8;
    public static final int COL_PAYAMOUNT = 9;

    private SumUpdate   m_sumUpdate;

    public String[] m_colNames = new String[]{
        "",
        "Pay status",
        "Business Partner",
        "Invoice ID",
        "OCR / Ref",
        "Invoice Date",
        "Due Date",
        "Pay Date",
        "Amount Due",
        "Payment Amount"
    };


    private Vector<LbPaymentRow>  m_paymentRows;

    public ReceivablesTableModel() {
        m_paymentRows = new Vector<LbPaymentRow>();
    }

    public void clear() {
    	m_paymentRows.clear();
    	this.fireTableDataChanged();
    }
    
    public Vector<LbPaymentRow> getSelected() {
        Vector<LbPaymentRow> result = new Vector<LbPaymentRow>();
        LbPaymentRow row;
        for (Iterator<LbPaymentRow> it = m_paymentRows.iterator(); it.hasNext();) {
            row = it.next();
            if (row.selected) {
                result.add(row);
            }
        }
        return(result);
    }
    
    /**
     * If a sum updater is set, the sum updater method is called when the selected
     * sum changes.
     *
     * @param s
     */
    public void setSumUpdater(SumUpdate s) {
        m_sumUpdate = s;
    }

    public double getSelectedSum() {
        double selectedAmount = 0.0;
        int selectedCount = 0;
        double allAmount = 0.0;
        LbPaymentRow row;
        for (Iterator<LbPaymentRow> it = m_paymentRows.iterator(); it.hasNext();) {
            row = it.next();
            if (row.selected) {
                selectedAmount += row.payAmount;
                selectedCount ++;
            }
            allAmount += row.payAmount;
        }
        if (m_sumUpdate!=null) {
        	selectedAmount = Math.round(selectedAmount * 100)/100.0;
        	allAmount = Math.round(allAmount * 100)/100.0;
            m_sumUpdate.updateSums(selectedAmount, selectedCount, allAmount, m_paymentRows.size());
        }
        return(selectedAmount);
    }

    private void addBPartners(List<MInvoice> invoices, Map<Integer, java.sql.Date> dueDates, PropertyChangeSupport progressMsg) {
        m_paymentRows = new Vector<LbPaymentRow>();
        LbPaymentRow row;
        progressMsg.firePropertyChange("invoiceMax", 0, invoices.size());
        int invoiceCount=0;
        for (Iterator<MInvoice> it = invoices.iterator(); it.hasNext();) {
          row = new LbPaymentRow();
          row.setInvoice(it.next());
          row.selected = false;
          row.setBpartner(new MBPartner(Env.getCtx(), row.getInvoice().getC_BPartner_ID(), null));
          row.payAmount = row.getInvoice().getOpenAmt().doubleValue();
          java.sql.Date ts = dueDates.get(row.getInvoice().get_ID());
          if (ts==null) ts = new java.sql.Date(0);
          row.dueDate = new java.sql.Timestamp(ts.getTime());
          row.payDate = new java.sql.Timestamp(Math.max(Calendar.getInstance().getTimeInMillis(), ts.getTime()));

          // Set paystatus.
          if (row.getInvoice().isPaid()) {
              row.payStatus = LbPaymentRow.PAYSTATUS_PAID;    // Paid
          } else {
              row.payStatus = LbPaymentRow.PAYSTATUS_NOTPAID;    // Not paid
          }
          m_paymentRows.add(row);
          progressMsg.firePropertyChange("invoiceCount", invoiceCount, ++invoiceCount);
        }
    }

    /**
     * Loads invoice selection
     * 
     * @param bPartnerId
     * @param startDate
     * @param endDate
     * @param status 1 = Not paid, 3 = paid
     * @throws SQLException
     */
    public void loadSelection(int bPartnerId, java.util.Date startDate, java.util.Date endDate, int status, PropertyChangeSupport progressMsg) throws SQLException {

        StringBuffer query = new StringBuffer("IsSOTrx='Y'");
        int i_bpartnerId = 0, i_startDate = 0, i_endDate = 0;
        int c = 1;
        Vector<Object> params = new Vector<Object>();
        if (bPartnerId>0) {
            query.append(" AND C_BPartner_ID=?");
            params.add(bPartnerId);
            i_bpartnerId = c++;
        }
        if (startDate!=null) {
            query.append(" AND DateInvoiced>=?");
            params.add(startDate);
            i_startDate = c++;
        }
        if (endDate!=null) {
            query.append(" AND DateInvoiced<=?");
            params.add(endDate);
            i_endDate = c++;
        }
        switch(status) {
            case LbPaymentRow.PAYSTATUS_NOTPAID: query.append(" AND IsPaid<>'Y'"); break;
            case LbPaymentRow.PAYSTATUS_PAID: query.append(" AND IsPaid='Y'"); break;
        }

        progressMsg.firePropertyChange("text", "Reading invoices", "Reading invoices");
        List<MInvoice> invoices = new Query(Env.getCtx(), MInvoice.Table_Name,
                query.toString(),
                null)
                .setApplyAccessFilter(true)
                .setOrderBy("DateInvoiced")
                .setParameters(params.toArray())
                .list();
        // Get due dates
        String query2 =
                "select c_invoice_id, paymenttermduedate(c_paymentterm_id, dateinvoiced::timestamp with time zone) AS duedate " +
                "from c_invoice where " + query.toString();
        Map<Integer, java.sql.Date> dueDates = new TreeMap<Integer, java.sql.Date>();
        PreparedStatement ps = DB.prepareStatement(query2, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, null);
        if (i_bpartnerId>0) ps.setInt(i_bpartnerId, bPartnerId);
        if (i_startDate>0) ps.setDate(i_startDate, new java.sql.Date(startDate.getTime()));
        if (i_endDate>0) ps.setDate(i_endDate, new java.sql.Date(endDate.getTime()));
        ResultSet rs = ps.executeQuery();
        while(rs.next()) {
            dueDates.put(rs.getInt(1), rs.getDate(2));
        }
        rs.close();
        ps.close();
        addBPartners(invoices, dueDates, progressMsg);
        // Update sums
        getSelectedSum();
    }

    @Override
    public String getColumnName(int col) {
        return(m_colNames[col]);
    }

    @Override
    public int getRowCount() {
        return(m_paymentRows.size());
    }

    @Override
    public int getColumnCount() {
        return(m_colNames.length);
    }

    @Override
    public Class getColumnClass(int col) {
        switch(col) {
            case COL_SELECT: return(Boolean.class);
            case COL_PAYSTATUS: return(Integer.class);
            case COL_BPARTNER: return(String.class);
            case COL_INVOICEID: return(String.class);
            case COL_OCRREF: return(String.class);
            case COL_INVDATE: return(java.sql.Timestamp.class);
            case COL_DUEDATE: return(java.sql.Timestamp.class);
            case COL_PAYDATE: return(java.sql.Timestamp.class);
            case COL_AMOUNTDUE: return(Double.class);
            case COL_PAYAMOUNT: return(Double.class);
        }
        return(Object.class);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        switch(columnIndex) {
            case COL_SELECT: return(true);
            case COL_PAYAMOUNT: return(true);
            case COL_PAYDATE: return(true);
        }
        return(false);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        LbPaymentRow row = m_paymentRows.get(rowIndex);
        switch(columnIndex) {
            case COL_SELECT:    return(row.selected);
            case COL_PAYSTATUS: return(row.payStatus);
            case COL_BPARTNER:  return(row.getBpartner().getName());
            case COL_INVOICEID: return(row.getInvoice().getDocumentNo());
            case COL_OCRREF:    return(row.getInvoice().get_ValueAsString("OCR"));
            case COL_INVDATE:   return(row.getInvoice().getDateInvoiced());
            case COL_DUEDATE:   return(row.dueDate);
            case COL_PAYDATE:   return(row.payDate);
            case COL_AMOUNTDUE: return(row.getInvoice().getOpenAmt());
            case COL_PAYAMOUNT: return(row.payAmount);
        }
        return(null);
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        LbPaymentRow row = m_paymentRows.get(rowIndex);
        switch (columnIndex) {
            case COL_SELECT:    row.selected = ((Boolean)value).booleanValue(); 
                                getSelectedSum();
                                break;
            case COL_PAYSTATUS: row.setPayStatus(((Integer)value).intValue()); break;
            case COL_PAYDATE:   row.payDate = ((java.sql.Timestamp)value); break;
            case COL_PAYAMOUNT: row.payAmount = ((Double)value).doubleValue(); break;
        }
    }

}
