package org.notima.bankgiro.adempiere.callout;

import java.util.Properties;

import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.notima.bankgiro.adempiere.model.XX_CalloutBGPayment;

/**
 * Gets bank account information from the BPartner and sets it on the invoice.
 * 
 * @author Daniel Tamm
 *
 */
public class CalloutBankgiroBPartner extends XX_CalloutBGPayment implements org.adempiere.base.IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {

		return bankgiroBPartner(ctx, WindowNo, mTab, mField, value);
	}

}
