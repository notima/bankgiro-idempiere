package org.notima.bankgiro.adempiere;

import java.util.Properties;

import org.compiere.model.*;
import java.util.*;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.compiere.util.AdempiereSystemError;
import org.compiere.util.Env;

/**
 * Callout used by vendor invoices
 * @author daniel.tamm
 *
 */
public class BGInvoiceCallout extends CalloutEngine {

	/**
	 * Checks BP Document no to ensure there are no duplicates. If there are duplicates a warning is displayed.
	 * This callout can be called from any field in the vendor invoice windows since it doesn't care about mField
	 * and value.
	 * 
	 * @param ctx
	 * @param windowNo
	 * @param mTab
	 * @param mField
	 * @param value
	 * @return
	 * @throws AdempiereSystemError
	 */
	public String checkBPDocumentNo(Properties ctx, int windowNo, GridTab mTab, GridField mField, Object value) throws AdempiereSystemError {

		GridField bpDocumentNo = mTab.getField("BPDocumentNo");
		if (bpDocumentNo==null || bpDocumentNo.getValue()==null) return("");
		String v = (String)bpDocumentNo.getValue().toString();
		if (v.trim().length()==0) return("");
		
		// Get current record ID (a vendor invoice)
		int id = mTab.getRecord_ID();
		MInvoice invoice = new MInvoice(ctx, id, null);
		if (invoice.getC_BPartner_ID()==0) return("");	// Can't do check without the bp set.
		
		// The value is supposed to be the vendor's invoice document no
		List<MInvoice> invoices = new Query(ctx, MInvoice.Table_Name, "c_bPartner_ID=? and BPDocumentNo=?", null)
									.setParameters(new Object[]{invoice.getC_BPartner_ID(), v})
									.list();

		if (invoices.size()==0) return("");
		if (invoices.size()==1) {
			if (invoice.get_ID()==0) {
				// This invoice is not saved yet, so we must have a duplicate.
				return(showDuplicatesDialog(windowNo, id, invoices));
			}
			// Check if the invoice is this invoice
			if (invoices.get(0).get_ID()==invoice.get_ID()) {
				return(""); // It's this invoice
			}
		}
		if (invoices.size()>1) {
			// There are more than one duplicate
			return(showDuplicatesDialog(windowNo, id, invoices));
		}
		
		return("");
	}
	
	private String showDuplicatesDialog(int windowNo, int recordId, List<MInvoice> invoices) {
		// Make a list of document number with duplicates
		Vector<String> duplicates = new Vector<String>();
		MInvoice invoice;
		for (Iterator<MInvoice> it = invoices.iterator(); it.hasNext(); ) {
			invoice = it.next();
			if (invoice.get_ID()!=recordId) {
				duplicates.add(invoice.getDocumentNo());
			}
		}
		// Add result to string buffer
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<duplicates.size(); i++) {
			buf.append(duplicates.get(i));
			if (i<(duplicates.size()-1)) buf.append(", ");
		}
		String bpDocumentNo = invoices.get(0).get_ValueAsString("BPDocumentNo");
		String msgString = "The vendor already has ";
		if (duplicates.size()>1) {
			msgString += "invoices";
		} else {
			msgString += "an invoice";
		}
		msgString += " with document no " + bpDocumentNo + ".\n";
		msgString += "The invoices are: " + buf.toString();
		// TODO: Show message
		// Alert.info(windowNo, null, msgString);
		return(msgString);
	}
	
}
