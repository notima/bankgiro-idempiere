package org.notima.bankgiro.adempiere;


import java.beans.PropertyChangeSupport;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;

import org.compiere.minigrid.IMiniTable;
import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.notima.bankgiro.adempiere.model.MLBSettings;
import org.notima.bg.BgUtil;

/**
 * Common ancestor class to GUI management form.
 * 
 * @author Daniel Tamm
 *
 */
public class LBManagement {
	
	public static CLogger log = CLogger.getCLogger(LBManagement.class);

	private SortedMap<String, MLBSettings>	 lbSettings;		// Store LB-settings for user
	
	public LBManagement() {
		
		lbSettings = new TreeMap<String, MLBSettings>();
		MLBSettings.putSettings(lbSettings);
		
	}

	public SortedMap<String, MLBSettings> getLbSettings() {
		return lbSettings;
	}

	public void setLbSettings(SortedMap<String, MLBSettings> lbSettings) {
		this.lbSettings = lbSettings;
	}
	
	
	/**
	 * Reads bank accounts that can be used for LB
	 * 
	 * @return
	 */
	public List<BankInfo> readBankAccounts() {
		
		//  Bank Account Info
		String sql = 
			"SELECT DISTINCT C_BankAccount.C_BankAccount_ID,"                       //  1
			+ "C_Bank.Name || ' ' || AccountNo AS Name,"          //  2
			+ "C_BankAccount.C_Currency_ID, C_Currency.ISO_Code,"                   //  3..4
			+ "CurrentBalance "                              //  5
			+ "FROM C_BankAccount "
			+ "LEFT JOIN C_Bank ON C_Bank.C_Bank_ID=C_BankAccount.C_Bank_ID "
			+ "LEFT JOIN C_Currency ON C_BankAccount.C_Currency_ID=C_Currency.C_Currency_ID "
			+ "WHERE C_BankAccount.isActive='Y' AND C_BankAccount.AD_Client_ID=? "
			+ "AND C_BankAccount.C_BankAccount_ID IN (SELECT C_BankAccount_ID FROM C_BankAccountDoc WHERE XC_LBPlugin_ID>0) "
			+ "ORDER BY 2";

		List<BankInfo> result = new ArrayList<BankInfo>();
		
		try
		{
			PreparedStatement pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, Env.getAD_Client_ID(Env.getCtx()));
			ResultSet rs = pstmt.executeQuery();
			while (rs.next())
			{
				boolean transfers = false;
				BankInfo bi = new BankInfo (rs.getInt(1), rs.getInt(3),
					rs.getString(2), rs.getString(4),
					rs.getBigDecimal(5), transfers);
				result.add(bi);
			}
			rs.close();
			pstmt.close();
		}
		catch (SQLException e)
		{
			LBManagement.log.log(Level.SEVERE, "VLBManagementPanel.readBankAccounts - BA", e);
		}
		
		return result;
		
		
	}

	/**
	 * Creates LBPaymentRows from a list of invoices.
	 * 
	 * @param invoices
	 * @param progressMsg
	 */
    private void addBPartners(List<MInvoice> invoices, PropertyChangeSupport progressMsg, List<LbPaymentRow> targetList) {
        LbPaymentRow row;
        if (progressMsg!=null)
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
			String BP_Bankgiro = receiverBankAccount.get_ValueAsString("BP_Bankgiro");
			String BP_Plusgiro = receiverBankAccount.get_ValueAsString("BP_Plusgiro");
			String clearing = receiverBankAccount.getRoutingNo();
			String accountNo = receiverBankAccount.getAccountNo();
			String swift = receiverBankAccount.get_ValueAsString("swiftcode");
			String iban = receiverBankAccount.get_ValueAsString("iban");
			if (BgUtil.validateBankgiro(BP_Bankgiro)) {
				row.dstAcct = BP_Bankgiro;
			} else if (BgUtil.validateBankgiro(BP_Plusgiro)) {
				row.dstAcct = BP_Plusgiro;
			} else if (accountNo!=null && accountNo.trim().length()>0) {
				row.dstAcct = (clearing!=null ? (clearing + " ") : "") + accountNo;
			} else if (BgUtil.validateIban(swift, iban)) {
				row.dstAcct = iban + " : " + swift;
			} else {
				row.dstAcct = "";
			}
          } catch (Exception eee) {
        	  row.dstAcct = "";
          }
          java.sql.Timestamp ts = (java.sql.Timestamp)row.getInvoice().get_Value("PayDateBG");
          if (ts==null) ts = new java.sql.Timestamp(0);
          row.payDate = new java.sql.Timestamp(Math.max(Calendar.getInstance().getTimeInMillis(), ts.getTime()));
          // Set paystatus.
          String psStr = (String)row.getInvoice().get_Value("LbStatus");
          Integer ps = null;
          if (psStr!=null) {
        	  ps = Integer.parseInt(psStr);
          }
          if (row.getInvoice().isPaid()) {
              row.payStatus = LbPaymentRow.PAYSTATUS_PAID;    // Paid
          } else if (ps==null) {
              row.payStatus = LbPaymentRow.PAYSTATUS_NOTPAID;    // Not paid
          } else if (ps.intValue()==1) {
              row.payStatus = LbPaymentRow.PAYSTATUS_INTRANSIT; // In transit
          } else {
              row.payStatus = LbPaymentRow.PAYSTATUS_NOTPAID;    // Not paid
          }
          
          // Check if approved/completed
          if (!"CO".equals(row.getInvoice().getDocStatus())) {
        	  row.payStatus = LbPaymentRow.PAYSTATUS_NOT_APPROVED; // TODO: Use constants instead
          }
          
          targetList.add(row);
          if (progressMsg!=null)
        	  progressMsg.firePropertyChange("invoiceCount", invoiceCount, ++invoiceCount);
          
        }
    }

    /**
     * Loads selection into paymentRows
     * 
     * @param paymentRule
     * @param bPartnerId
     * @param startDate
     * @param endDate
     * @param status   1 = Not paid, 2 = In transit, 3 = paid
     * @param progressMsg
     * @param targetList	List to be populated
     */
    public void loadSelection(String paymentRule, int bPartnerId, Date startDate, Date endDate, int status, PropertyChangeSupport progressMsg, List<LbPaymentRow> targetList) {

        StringBuffer query = new StringBuffer("PaymentRule=? AND (DocStatus='CO' OR DocStatus='CL' OR DocStatus='IP')");
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
            case 1: query.append(" AND IsPaid<>'Y' AND (LbStatus='0' or LbStatus is null)"); break;
            case 2: query.append(" AND IsPaid<>'Y' AND LbStatus='1'"); break;
            case 3: query.append(" AND IsPaid='Y'"); break;
        }

        if (progressMsg!=null) 
        	progressMsg.firePropertyChange("text", "Reading invoices", "Reading invoices");
        List<MInvoice> invoices = new Query(Env.getCtx(), MInvoice.Table_Name,
                query.toString(), null)
        		.setParameters(params.toArray())
                .setApplyAccessFilter(true)
                .setOrderBy("PayDateBG")
                .list();
        
        addBPartners(invoices, progressMsg, targetList);
    }
	
    private void addBPartnersReceivables(List<MInvoice> invoices, Map<Integer, java.sql.Date> dueDates, PropertyChangeSupport progressMsg, List<LbPaymentRow> targetList) {
        LbPaymentRow row;
        if (progressMsg!=null)
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
          targetList.add(row);
          if (progressMsg!=null)
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
    public void loadSelectionReceivables(int bPartnerId, java.util.Date startDate, java.util.Date endDate, int status, PropertyChangeSupport progressMsg, List<LbPaymentRow> targetList) throws SQLException {

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
        addBPartnersReceivables(invoices, dueDates, progressMsg, targetList);
    }
    
    
    
	public IMiniTable fillPayablesTable(IMiniTable payablesTable, int lbStatus, java.sql.Date fromDate, java.sql.Date toDate, int bPartnerId) {
		
		
		return payablesTable;
	}
	
	
	
}
