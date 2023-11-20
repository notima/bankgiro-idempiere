/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.notima.bankgiro.adempiere;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.Query;
import org.compiere.util.Env;

/**
 * Representation of accounts payable payment
 *
 * @author Daniel Tamm
 */
@XmlRootElement
public class LbPaymentRow {

    public static final int PAYSTATUS_ANY = 0;
    public static final int PAYSTATUS_NOTPAID = 1;
    public static final int PAYSTATUS_INTRANSIT = 2;
    public static final int PAYSTATUS_PAID = 3;
    public static final int PAYSTATUS_NOT_APPROVED = 4;

    public boolean      selected;
    public int          payStatus;
    public int			invoice_ID;
    @XmlTransient
    private MInvoice     invoice;
    public int			bpartner_ID;
    @XmlTransient
    private MBPartner    bpartner;
    public String		dstAcct;
    public java.sql.Timestamp   payDate;
    public java.sql.Timestamp   dueDate;
    public double       payAmount;
    public double		amountDue;
    public String		currency;
    public boolean		arCreditMemo = false;
    private MBPBankAccount	dstAccount;

    /**
     * Sets paystatus "manually". Can't change from Paid to any other state. If
     * you want to change from paid you need to cancel the payment first.
     *
     * @param status
     */
    public void setPayStatus(int status) {
        if (payStatus==status) return;
        if (!invoice.isSOTrx() || invoice.getC_DocType().getDocBaseType().equals(MDocType.DOCBASETYPE_ARCreditMemo)) {
            if (status==LbPaymentRow.PAYSTATUS_NOTPAID && payStatus==LbPaymentRow.PAYSTATUS_INTRANSIT) {
                invoice.set_ValueOfColumn("LbStatus", Integer.valueOf(0));
                invoice.saveEx();
                payStatus = status;
            }
            if (status==LbPaymentRow.PAYSTATUS_INTRANSIT && payStatus==LbPaymentRow.PAYSTATUS_NOTPAID) {
                invoice.set_ValueOfColumn("LbStatus", Integer.valueOf(1));
                invoice.saveEx();
                payStatus = status;
            }
        } else {
            // Sales invoice
        }

    }
    
    /**
     * Return the account that will be used for sending payment 
     * 
     * @param bpartnerId
     * @return
     * @throws	Exception if there are no or more than one bank account.
     */
    @XmlTransient
    public static MBPBankAccount getDestinationAccount(int bpartnerId) throws Exception {

        List<MBPBankAccount> accts = new Query(
                Env.getCtx(), MBPBankAccount.Table_Name,
                "C_BPartner_ID=? AND islbrutin='Y'", null)
                .setParameters(new Object[]{bpartnerId})
                .setOnlyActiveRecords(true)
                .list();
        
        MBPBankAccount receiverBankAccount = null;
        if (accts.size()>1) {
        	throw new Exception("More than one possible bank account. Please make sure only one is active.");
        }
        if (accts.size()==1)
        	receiverBankAccount = accts.get(0);
        if (receiverBankAccount==null) {
        	throw new Exception("No active bank account for LB-rutin");
        }
    	
    	return(receiverBankAccount);
    	
    }

	public MInvoice getInvoice() {
		if (invoice==null && invoice_ID>0) {
			invoice = new MInvoice(Env.getCtx(), invoice_ID, null);
		}
		return invoice;
	}

	public void setInvoice(MInvoice invoice) {
		this.invoice = invoice;
	}

	public MBPartner getBpartner() {
		if (bpartner==null && bpartner_ID>0) {
			bpartner = new MBPartner(Env.getCtx(), bpartner_ID, null);
		}
		return bpartner;
	}

	public void setBpartner(MBPartner bpartner) {
		this.bpartner = bpartner;
	}

	/**
	 * Destination account. This field is set manually.
	 * 
	 * Use getDestinationAccount to lookup a destination account.
	 * 
	 * @return
	 */
	public MBPBankAccount getDstAccount() {
		return dstAccount;
	}

	public void setDstAccount(MBPBankAccount dstAccount) {
		this.dstAccount = dstAccount;
	}

	

}
