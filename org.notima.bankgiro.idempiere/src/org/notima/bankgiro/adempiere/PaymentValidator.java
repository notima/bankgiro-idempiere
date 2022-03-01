package org.notima.bankgiro.adempiere;

import org.compiere.model.MBankAccount;

public interface PaymentValidator {
	
	/**
	 * Returns null if the payment passes the validation. 
	 * If validation isn't passed a reason why is returned.
	 *
	 * @param srcAccount
	 * @param pmt
	 * @return
	 */
	public String validatePayment(MBankAccount srcAccount, LbPaymentRow pmt);
	
}
