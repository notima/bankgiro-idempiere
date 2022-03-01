
package org.notima.bankgiro.adempiere.model;
import org.compiere.model.*;

import java.sql.*;
import java.util.*;

import org.compiere.util.*;

/**
 * Payment Bankgiro Callout
 * 
 * @author anba
 * @version $Id: XX_CalloutBGPayment.java,v 1.3 2005/02/24 13:34:57 johc Exp $
 *
 */
public class XX_CalloutBGPayment extends CalloutEngine
{

	/**
	 *  Payment_Bankgiro
	 *  When businesspartner selected
	 *  - get BP_Bankgiro from businesspartner
	 * 
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Model Tab
	 *  @param mField   Model Field
	 *  @param value    The new value
	 *  @return Error message or ""
	 */
	public String bankgiroBPartner (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		Integer C_BPartner_ID = (Integer)value;
		if (isCalloutActive()	//	assuming it is resetting value
			|| C_BPartner_ID == null || C_BPartner_ID.intValue() == 0)
			return "";

        MBPBankAccount bAccount = new Query(ctx, MBPBankAccount.Table_Name, "C_BPartner_ID=?",null)
                .setParameters(new Object[]{C_BPartner_ID})
                .first();

        String bankgiro = "";
        String plusgiro = "";
        if (bAccount!=null) {
            bankgiro = (String)bAccount.get_Value("BP_Bankgiro");
            plusgiro = (String)bAccount.get_Value("BP_Plusgiro");
        }
        mTab.setValue("BP_Bankgiro", bankgiro);
        mTab.setValue("BP_Plusgiro", plusgiro);

		return "";
	}	
	
	/**
	 *  Payment_Bankgiro.
	 *  When invoice selected
	 *  - get BP_Bankgiro from businesspartner
	 * 
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Model Tab
	 *  @param mField   Model Field
	 *  @param value    The new value
	 *  @return Error message or ""
	 */
	public String bankgiroBPInvoice (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		Integer C_Invoice_ID = (Integer)value;
		if (isCalloutActive()	//	assuming it is resetting value
			|| C_Invoice_ID == null || C_Invoice_ID.intValue() == 0)
			return "";

        MInvoice invoice = new Query(ctx, MInvoice.Table_Name, "C_Invoice_ID=?",null)
                .setParameters(new Object[]{C_Invoice_ID})
                .first();

        MBPBankAccount bAccount = new Query(ctx, MBPBankAccount.Table_Name, "C_BPartner_ID=?", null)
                .setParameters(new Object[]{invoice.getC_BPartner_ID()})
                .first();

        String bankgiro = "";
        String plusgiro = "";
        if (bAccount!=null) {
            bankgiro = (String)bAccount.get_Value("BP_Bankgiro");
            plusgiro = (String)bAccount.get_Value("BP_Plusgiro");
        }
        mTab.setValue("BP_Bankgiro", bankgiro);
        mTab.setValue("BP_Plusgiro", plusgiro);
        int C_Currency_ID = invoice.getC_Currency_ID();
        mTab.setValue("PayDateBG", invoice.get_Value("PayDateBG"));
        MCurrency currency = MCurrency.get(ctx, C_Currency_ID);
        mTab.setValue("CurrencyISO", currency.getISO_Code());
		return "";
	}	

	/**
	 *  Payment_Bankgiro.
	 *  When new payment selected
	 *  - get AD_Bankgiro from organisation
	 * 
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Model Tab
	 *  @param mField   Model Field
	 *  @param value    The new value
	 *  @return Error message or ""
	 */
	public String bankgiroAD (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		Integer C_BankAccount_ID = (Integer)value;
		if (isCalloutActive()		//	assuming it is resetting value
			|| C_BankAccount_ID == null || C_BankAccount_ID.intValue() == 0)
			return "";
        MBankAccount bAccount = new Query(ctx, MBankAccount.Table_Name, "C_BankAccount_ID=?", null)
                .setParameters(new Object[]{C_BankAccount_ID})
                .first();
        mTab.setValue("AD_Bankgiro", bAccount.get_Value("AD_Bankgiro"));
		return "";
	}

	/**
	 *  Payment_Bankgiro.
	 *  When invoice selected
	 *  - get POReference from Invoice
	 * 
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Model Tab
	 *  @param mField   Model Field
	 *  @param value    The new value
	 *  @return Error message or ""
	 */
	public String poReference (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		Integer C_Invoice_ID = (Integer)value;
		if (isCalloutActive()	//	assuming it is resetting value
			|| C_Invoice_ID == null || C_Invoice_ID.intValue() == 0)
			return "";
		setCalloutActive(true);
				
		String sql1 = "SELECT POReference, IsOCR, OCR, documentno "
			+ "FROM C_Invoice WHERE C_Invoice_ID=?";
		
		try
		{
			PreparedStatement pstmt = DB.prepareStatement(sql1);
			pstmt.setInt(1, C_Invoice_ID.intValue());
			ResultSet rs = pstmt.executeQuery();
			Integer C_BPartner_ID = null;
			if (rs.next())
			{
				if (rs.getString(1) != null)
					mTab.setValue("POReference", new String(rs.getString(1)));
				else
					mTab.setValue("POReference", new String(""));
				if (rs.getString(2) != null)
				{
				   String s = rs.getString(2);
				   if (s.charAt(0) == 'Y')
				      mTab.setValue("IsOCR", new Boolean(true));
				   else
				      mTab.setValue("IsOCR", new Boolean(false));
				}
				if (rs.getString(3) != null)
					mTab.setValue("OCR", new String(rs.getString(3)));
				else
					mTab.setValue("OCR", new String(""));
				if (rs.getString(4) != null)
				   mTab.setValue("DocumentNoInvoice", new String(rs.getString(4)));
				else
				   mTab.setValue("DocumentNoInvoice", new String(""));
			}	
			rs.close();
			pstmt.close();
			
		}
		catch (SQLException e)
		{
			log.severe("POReference: " + e.getMessage());
			setCalloutActive(false);
			return e.getLocalizedMessage();
		}

		setCalloutActive(false);
		return "";
	}	
	
	/**
	 *  Payment_Bankgiro.
	 *  Sets PayDateBG to todays date + one day
	 * 
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Model Tab
	 *  @param mField   Model Field
	 *  @param value    The new value
	 *  @return Error message or ""
	 */
	public String payDateBG (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		Integer AD_Org_ID = (Integer)value;
		if (isCalloutActive()		//	assuming it is resetting value
			|| AD_Org_ID == null || AD_Org_ID.intValue() == 0)
			return "";
		
		Calendar cal = GregorianCalendar.getInstance();
		cal.roll(Calendar.DATE, 1);
		Timestamp ts = new Timestamp(cal.getTimeInMillis());
		mTab.setValue("PayDateBG", ts);
		return "";
	}

	
}
