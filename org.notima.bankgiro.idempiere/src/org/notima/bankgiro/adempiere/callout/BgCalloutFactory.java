package org.notima.bankgiro.adempiere.callout;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.IColumnCalloutFactory;
import org.compiere.model.MInvoice;

public class BgCalloutFactory implements IColumnCalloutFactory {

	public static CalloutBankgiroBPartner calloutBankgiroBPartner = new CalloutBankgiroBPartner();
	public static CalloutBgInvoiceCheckOCR calloutBgInvoiceCheckOCR = new CalloutBgInvoiceCheckOCR();
	public static CalloutBgInvoiceSetPayDate calloutBgInvoiceSetPayDate = new CalloutBgInvoiceSetPayDate();
	
	@Override
	public IColumnCallout[] getColumnCallouts(String tableName,
			String columnName) {
		
		// Invoice
		if (tableName.equalsIgnoreCase(MInvoice.Table_Name)) {
			
			if (columnName.equalsIgnoreCase(MInvoice.COLUMNNAME_C_BPartner_ID)) {
				return new IColumnCallout[]{calloutBankgiroBPartner, calloutBgInvoiceSetPayDate};
			}
			
			// Updates duedate and pay date
			if (columnName.equalsIgnoreCase(MInvoice.COLUMNNAME_DateInvoiced)) {
				return new IColumnCallout[]{calloutBgInvoiceSetPayDate};
			}
			
		}
		
		return null;
	}

}
