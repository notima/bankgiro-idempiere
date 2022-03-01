package org.notima.bankgiro.adempiere;

import org.compiere.model.MBPBankAccount;
import org.notima.bg.BgUtil;

public class BankAccountUtil {

	/**
	 * Return a string representation of the recipient account no
	 * 
	 * @param receiverBankAccount
	 * @return
	 * @throws Exception
	 */
	public static String getAccountString(MBPBankAccount receiverBankAccount) throws Exception {
		String BP_Bankgiro = receiverBankAccount.get_ValueAsString("BP_Bankgiro");
		String BP_Plusgiro = receiverBankAccount.get_ValueAsString("BP_Plusgiro");
		String clearing = receiverBankAccount.getRoutingNo();
		String accountNo = receiverBankAccount.getAccountNo();
		String swift = receiverBankAccount.get_ValueAsString("swiftcode");
		String iban = receiverBankAccount.get_ValueAsString("iban");
		String dstAcct;
		if (BgUtil.validateBankgiro(BP_Bankgiro)) {
			dstAcct = BP_Bankgiro;
		} else if (BgUtil.validateBankgiro(BP_Plusgiro)) {
			dstAcct = BP_Plusgiro;
		} else if (accountNo!=null && accountNo.trim().length()>0) {
			dstAcct = (clearing!=null ? (clearing + " ") : "") + accountNo;
		} else if (BgUtil.validateIban(swift, iban)) {
			dstAcct = iban + " : " + swift;
		} else {
			dstAcct = "";
		}
		return dstAcct;
	}
	
	
}
