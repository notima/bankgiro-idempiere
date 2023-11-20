package org.notima.bankgiro.adempiere.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.notima.bankgiro.adempiere.model.XX_CalloutInvoice;

public class CalloutBgInvoiceSetPayDate extends XX_CalloutInvoice implements
		IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		
		return this.setPayDate(ctx, WindowNo, mTab, mField, value);
				
	}

}
