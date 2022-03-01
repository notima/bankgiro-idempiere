/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.notima.bankgiro.adempiere;

import java.beans.PropertyChangeSupport;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;

import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.Query;
import org.compiere.util.Env;


/**
 * Table model for displaying payments
 *
 * @author Daniel Tamm
 */
public class LbPaymentTableModel extends AbstractTableModel {

    public static final int COL_SELECT = 0;
    public static final int COL_PAYSTATUS = 1;
    public static final int COL_BPARTNER = 2;
    public static final int COL_INVOICEID = 3;
    public static final int COL_OCRREF = 4;
    public static final int	COL_DSTACCT = 5;
    public static final int COL_INVDATE = 6;
    public static final int COL_DUEDATE = 7;
    public static final int COL_PAYDATE = 8;
    public static final int COL_AMOUNTDUE = 9;
    public static final int COL_PAYAMOUNT = 10;
    public static final int	COL_CURRENCY = 11;
    public static final int COL_PRODDATE = 12;

    private SumUpdate   m_sumUpdate;

    public String[] m_colNames = new String[]{
        "",
        "Pay status",
        "Business Partner",
        "Invoice ID",
        "OCR / Ref",
        "Dst Acct",
        "Invoice Date",
        "Due Date",
        "Pay Date",
        "Amount Due",
        "Payment Amount",
        "Currency",
        "Prod Date"
    };


    private Vector<LbPaymentRow>  m_paymentRows;

    public LbPaymentTableModel() {
        m_paymentRows = new Vector<LbPaymentRow>();
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

    public void clear() {
    	m_paymentRows.clear();
    	this.fireTableDataChanged();
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
                selectedCount++;
            }
            allAmount += row.payAmount;
        }
        if (m_sumUpdate!=null) {
        	// Round
        	selectedAmount = Math.round(selectedAmount * 100)/100.0;
        	allAmount = Math.round(allAmount * 100)/100.0;
            m_sumUpdate.updateSums(selectedAmount, selectedCount, allAmount, m_paymentRows.size());
        }
        return(selectedAmount);
    }

    private void addBPartners(List<MInvoice> invoices, PropertyChangeSupport progressMsg) {
        m_paymentRows = new Vector<LbPaymentRow>();
        LbPaymentRow row;
        progressMsg.firePropertyChange("invoiceMax", 0, invoices.size());
        int invoiceCount = 0;
        for (Iterator<MInvoice> it = invoices.iterator(); it.hasNext();) {
          row = new LbPaymentRow();
          row.setInvoice(it.next());
          row.selected = false;
          row.setBpartner(new MBPartner(Env.getCtx(), row.getInvoice().getC_BPartner_ID(), null));
          row.payAmount = row.getInvoice().getOpenAmt().doubleValue();
          row.arCreditMemo = row.getInvoice().getC_DocType().getDocBaseType().equals(MDocType.DOCBASETYPE_ARCreditMemo);
          if (row.arCreditMemo || (row.getInvoice().isSOTrx() && !row.arCreditMemo && row.payAmount<0)) {
        	  row.payAmount = -row.payAmount;
          }
          row.amountDue = row.payAmount;

          row.currency = MCurrency.getISO_Code(Env.getCtx(), row.getInvoice().getC_Currency_ID());
          // Get destination account

          MBPBankAccount receiverBankAccount = null;
          
          try {
			receiverBankAccount = LbPaymentRow.getDestinationAccount(row.getBpartner().get_ID());
			row.dstAcct = BankAccountUtil.getAccountString(receiverBankAccount);
          } catch (Exception eee) {
        	  row.dstAcct = "";
          }
          java.sql.Timestamp ts = (java.sql.Timestamp)row.getInvoice().get_Value("PayDateBG");
          if (ts==null) ts = new java.sql.Timestamp(0);
          row.payDate = new java.sql.Timestamp(Math.max(Calendar.getInstance().getTimeInMillis(), ts.getTime()));
          // Set paystatus.
          Integer ps = (Integer)row.getInvoice().get_Value("LbStatus");
          if (row.getInvoice().isPaid()) {
              row.payStatus = LbPaymentRow.PAYSTATUS_PAID;    // Paid
          } else if (ps==null) {
              row.payStatus = LbPaymentRow.PAYSTATUS_NOTPAID;    // Not paid
          } else if (ps.intValue()==1) {
              row.payStatus = LbPaymentRow.PAYSTATUS_INTRANSIT; // In transit
          } else {
              row.payStatus = LbPaymentRow.PAYSTATUS_NOTPAID;    // Not paid
          }
          m_paymentRows.add(row);
          progressMsg.firePropertyChange("invoiceCount", invoiceCount, ++invoiceCount);
          
        }
    }

    /**
     * 
     * @param paymentRule
     * @param bPartnerId
     * @param startDate
     * @param endDate
     * @param status   1 = Not paid, 2 = In transit, 3 = paid
     * @param progressMsg
     */
    public void loadSelection(String paymentRule, int bPartnerId, Date startDate, Date endDate, int status, PropertyChangeSupport progressMsg) {

        StringBuffer query = new StringBuffer("PaymentRule=? AND (DocStatus='CO' OR DocStatus='CL')");
        List<Object> params = new Vector<Object>();
        params.add(paymentRule);
        if (bPartnerId>0) {
            query.append(" AND C_BPartner_ID=?");
            params.add(bPartnerId);
        }
        if (startDate!=null) {
            query.append(" AND DateInvoiced>=?");
            params.add(startDate);
        }
        if (endDate!=null) {
            query.append(" AND DateInvoiced<=?");
            params.add(endDate);
        }
        switch(status) {
            case 1: query.append(" AND IsPaid<>'Y' AND (LbStatus=0 or LbStatus is null)"); break;
            case 2: query.append(" AND IsPaid<>'Y' AND LbStatus=1"); break;
            case 3: query.append(" AND IsPaid='Y'"); break;
        }

        progressMsg.firePropertyChange("text", "Reading invoices", "Reading invoices");
        List<MInvoice> invoices = new Query(Env.getCtx(), MInvoice.Table_Name,
                query.toString(), null)
        		.setParameters(params.toArray())
                .setApplyAccessFilter(true)
                .setOrderBy("PayDateBG")
                .list();
        addBPartners(invoices, progressMsg);
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
            case COL_DSTACCT: return(String.class);
            case COL_INVDATE: return(java.sql.Timestamp.class);
            case COL_DUEDATE: return(java.sql.Timestamp.class);
            case COL_AMOUNTDUE: return(Double.class);
            case COL_PAYAMOUNT: return(Double.class);
            case COL_CURRENCY: return(String.class);
            case COL_PRODDATE: return(java.sql.Timestamp.class);
        }
        return(Object.class);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        switch(columnIndex) {
            case COL_SELECT: return(true);
            case COL_PAYDATE: return(true);
            case COL_PAYAMOUNT: return(true);
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
            case COL_OCRREF:    if (row.getInvoice().get_Value("IsOCR")!=null && ((Boolean)row.getInvoice().get_Value("IsOCR")).booleanValue()) {
            						return(row.getInvoice().get_ValueAsString("OCR"));
            					} else {
            						return(row.getInvoice().get_ValueAsString("BPDocumentNo"));
            					}
            case COL_DSTACCT:	return(row.dstAcct);
            case COL_INVDATE:   return(row.getInvoice().getDateInvoiced());
            case COL_DUEDATE:   return(row.getInvoice().get_Value("PayDateBG"));
            case COL_PAYDATE:   return(row.payDate);
            case COL_AMOUNTDUE: return(row.amountDue);
            case COL_PAYAMOUNT: return(row.payAmount);
            case COL_CURRENCY : return(row.currency);
            case COL_PRODDATE : return(row.getInvoice().get_Value("LBProdDate"));
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
            case COL_PAYAMOUNT:
            	try {
            		row.payAmount = Double.parseDouble(value.toString()); break;
            	} catch (Exception e) {
            		JOptionPane.showMessageDialog(null, value.toString() + " is not a number.");
            	}
        }
    }

}
