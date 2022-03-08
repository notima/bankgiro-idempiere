/******************************************************************************
 * The contents of this file are subject to the   Compiere License  Version 1.1
 * ("License"); You may not use this file except in compliance with the License
 * You may obtain a copy of the License at http://www.compiere.org/license.html
 * Software distributed under the License is distributed on an  "AS IS"  basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * The Original Code is                  Compiere  ERP & CRM  Business Solution
 * The Initial Developer of the Original Code is Jorg Janke  and ComPiere, Inc.
 * Portions created by Jorg Janke are Copyright (C) 1999-2003 Jorg Janke, parts
 * created by ComPiere are Copyright (C) ComPiere, Inc.;   All Rights Reserved.
 * Contributor(s): ______________________________________.
 *****************************************************************************/
package org.notima.bankgiro.adempiere.model;

import java.util.List;
import java.util.Properties;

import org.compiere.model.CalloutEngine;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MPaymentTerm;
import org.compiere.model.Query;
import org.notima.bankgiro.adempiere.BankAccountUtil;
import org.notima.bg.BgUtil;


/**
 *	Invoice Callouts
 *	
 *  @version $Id: CalloutInvoice.java,v 1.2 2004/12/15 09:55:17 johc Exp $
 */
public class XX_CalloutInvoice extends CalloutEngine
{

	/**
	 *	Invoice Header - DocType.
	 *		- PaymentRule
	 *		- temporary Document
	 *  Context:
	 *  	- DocSubTypeSO
	 *		- HasCharges
	 *	- (re-sets Business Partner info of required)
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Model Tab
	 *  @param mField   Model Field
	 *  @param value    The new value
	 */
	public String docType (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		Integer C_DocType_ID = (Integer)value;
		if (C_DocType_ID == null || C_DocType_ID.intValue() == 0)
			return "";

        MDocType docType = new MDocType(ctx, C_DocType_ID, null);
        String s = docType.getDocBaseType();
        if (s.startsWith("AP"))
            mTab.setValue("PaymentRule", "Z");    //  anba 041109 changed to Bankgiro from S - Check
        else if (s.endsWith("C"))
            mTab.setValue("PaymentRule", "P");    //  OnCredit
		return "";
	}	//	docType

	/**
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Model Tab
	 *  @param mField   Model Field
	 *  @param value    The new value
	 */
	public String setPayDate (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
        String paymentRule = (String)mTab.getValue("PaymentRule");
        if ("Z".equalsIgnoreCase(paymentRule)) {
            Integer C_PaymentTerm_ID = (Integer)mTab.getValue("C_PaymentTerm_ID");
            MPaymentTerm term = new MPaymentTerm(ctx, C_PaymentTerm_ID, null);
            int daysToAdd = term.getNetDays(); // TODO: Handle all cases of payment term
            java.sql.Timestamp di =(java.sql.Timestamp)mTab.getValue("DateInvoiced");
            java.util.Calendar cal = new java.util.GregorianCalendar();
            cal.setTimeInMillis(di.getTime());
            cal.add(java.util.Calendar.DAY_OF_YEAR, daysToAdd);
            java.sql.Timestamp dp = new java.sql.Timestamp(cal.getTimeInMillis());
            mTab.setValue("PayDateBG", dp);
            mTab.setValue("DueDate", dp);
        }

        return("");
	}	//	docType
	
	/**
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Model Tab
	 *  @param mField   Model Field
	 *  @param value    The new value
	 */
	public String setPayDateFromDueDate (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
        String paymentRule = (String)mTab.getValue("PaymentRule");
        if ("Z".equalsIgnoreCase(paymentRule)) {
        	java.sql.Timestamp dueDate = (java.sql.Timestamp)mTab.getValue("DueDate");
        	if (dueDate!=null) {
        		mTab.setValue("PayDateBG", dueDate);
        	}
        }

        return("");
	}	//	docType
	

	/**
	 *	Invoice Header- BPartner.
	 *		- M_PriceList_ID (+ Context)
	 *		- C_BPartner_Location_ID
	 *		- AD_User_ID
	 *		- POReference
	 *		- SO_Description
	 *		- IsDiscountPrinted
	 *		- PaymentRule
	 *		- C_PaymentTerm_ID
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Model Tab
	 *  @param mField   Model Field
	 *  @param value    The new value
	 */
	public String bPartner (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		Integer C_BPartner_ID = (Integer)value;
		if (C_BPartner_ID == null || C_BPartner_ID.intValue() == 0)
			return "";

        // Get invoice
        int invoiceId = mTab.getRecord_ID();
        if (invoiceId==0) return null;
        int windowId = mTab.getAD_Window_ID();
        
        if (windowId==183) { // Vendor Invoice
        
	        // Check if LB-rutin
	        MBPartner bp = new MBPartner(ctx, C_BPartner_ID, null);
	        
	        if ("Z".equals(bp.getPaymentRulePO())) {
	
	        	// Find target bank account
	        	List<MBPBankAccount> dstBas = new Query(ctx, MBPBankAccount.Table_Name, "C_BPartner_ID=? AND IsLbRutin='Y' AND IsActive='Y'", null)
	        			.setParameters(new Object[]{C_BPartner_ID})
	        			.list();
	        	
	        	if (dstBas!=null && dstBas.size()>1) {
	        		return "More than one active destination account for LB-rutin";
	        	}
	        	if (dstBas==null || dstBas.size()==0) {
	        		return "No destination account defined for LB-rutin";
	        	}
	        	
	        	MBPBankAccount dstBa = dstBas.get(0);
	        	boolean isOCR = dstBa.get_ValueAsBoolean("IsOCR");
	        	
	        	mTab.setValue("IsOCR", Boolean.valueOf(isOCR));
	        	try {
	        		mTab.setValue("DstBankAccount", BankAccountUtil.getAccountString(dstBa));
	        	} catch (Exception e) {
	        		e.printStackTrace();
	        	}
	        	
	        }
        }
        
		return "";
	}	//	bPartner
	
	
	/**
	 * 
	 * 
	 * @param ctx
	 * @param WindowNo
	 * @param mTab
	 * @param mField		should be the isOCR check
	 * @param value
	 * @return
	 */
	public String checkOcr(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value) {
		
		if (!"IsOCR".equalsIgnoreCase(mField.getColumnName()) || value==null)
			return "";
		
		Boolean v = Boolean.FALSE;
		
		if (value instanceof Boolean) {
				v = (Boolean)value;
		} 
		if (value instanceof String) {
			v = "Y".equalsIgnoreCase(value.toString());
		}
		
		GridField f = mTab.getField("BPDocumentNo");
		if (f==null) return ""; 
		
		GridField isSo = mTab.getField("IsSOTrx");
		String bpDocumentNo = f.getValue()!=null ? f.getValue().toString() : null;
		GridField f2 = mTab.getField("OCR");
		boolean soTrx = isSo.getValue()!=null ? "Y".equals(isSo.toString()) : false;
		String ocr = f2.getValue()!=null ? f2.getValue().toString() : null;
		if (soTrx) {
		} else {
			if (v.booleanValue() && bpDocumentNo!=null && (ocr==null || ocr.trim().length()==0)) {
				if (BgUtil.isValidOCRNumber(bpDocumentNo)) {
					mTab.setValue("OCR", bpDocumentNo);
				}
			}
		}
		
		return "";
		// check OCR
	}	
	
}	//	CalloutInvoice
