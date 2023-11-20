package org.notima.bankgiro.adempiere;

import java.io.File;
import java.util.List;

import org.compiere.model.MBankAccount;

/**
 * Basic Payment File factory. Used mainly for testing purposes.
 * 
 * @author daniel.tamm
 *
 */
public class XmlFileFactory implements PaymentFileFactory {

	public static String KEY = "XML_BASIC_FILE";
	
	@Override
	public File createPaymentFile(MBankAccount srcAccount,
			List<LbPaymentRow> suggestedPayments, File outDir, String trxName) {
		
		
		return null;
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getName() {
		return "XML Basic File Factory";
	}

	@Override
	public PaymentValidator getPaymentValidator() {
		// TODO Auto-generated method stub
		return null;
	}

}
