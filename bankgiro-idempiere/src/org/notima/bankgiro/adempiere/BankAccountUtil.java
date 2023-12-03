package org.notima.bankgiro.adempiere;

import org.compiere.model.MBPBankAccount;
import org.notima.bg.BgUtil;

public class BankAccountUtil {

	public enum BankAccountType {
		BANKGIRO,
		PLUSGIRO,
		DOMESTIC_BANKACCT,
		IBAN,
		SEPA,
		NONE		
	}
	
	private MBPBankAccount	bpacct;
	private String	bankgiro;
	private String	plusgiro;
	private String	clearing;
	private String	accountNo;
	private String	swift;
	private String	iban;
	private String	dstAcct;
	private BankAccountType	accountType;
	
	public static BankAccountUtil buildFromMBPBankAccount(MBPBankAccount bpacct) {
		
		BankAccountUtil that = new BankAccountUtil();
		that.bpacct = bpacct;
		that.init();
		return that;
	}
	
	private void init() {

		bankgiro = bpacct.get_ValueAsString("BP_Bankgiro");
		plusgiro = bpacct.get_ValueAsString("BP_Plusgiro");
		clearing = bpacct.getRoutingNo();
		accountNo = bpacct.getAccountNo();
		swift = bpacct.get_ValueAsString("swiftcode");
		iban = bpacct.get_ValueAsString("iban");

		if (BgUtil.validateBankgiro(bankgiro)) {
			dstAcct = bankgiro;
			accountType = BankAccountType.BANKGIRO;
		} else if (BgUtil.validateBankgiro(plusgiro)) {
			dstAcct = plusgiro;
			accountType = BankAccountType.PLUSGIRO;
		} else if (accountNo!=null && accountNo.trim().length()>0) {
			dstAcct = (clearing!=null ? (clearing + " ") : "") + accountNo;
			accountType = BankAccountType.DOMESTIC_BANKACCT;
		} else if (BgUtil.validateIban(swift, iban)) {
			dstAcct = iban + " : " + swift;
			accountType = BankAccountType.IBAN;
		} else {
			dstAcct = "";
			accountType = BankAccountType.NONE;
		}
		
	}
	
	public String getDstAcct() {
		return dstAcct;
	}

	/**
	 * Return a string representation of the recipient account no
	 * 
	 * @param receiverBankAccount
	 * @return
	 * @throws Exception
	 */
	public static String getAccountString(MBPBankAccount receiverBankAccount) throws Exception {
		
		BankAccountUtil bpa = BankAccountUtil.buildFromMBPBankAccount(receiverBankAccount);
		
		return bpa.getDstAcct();
	}
	
	public String getClearing() {
		return clearing;
	}

	public void setClearing(String clearing) {
		this.clearing = clearing;
	}

	public String getAccountNo() {
		return accountNo;
	}

	public void setAccountNo(String accountNo) {
		this.accountNo = accountNo;
	}

	public String getSwift() {
		return swift;
	}

	public void setSwift(String swift) {
		this.swift = swift;
	}

	public String getIban() {
		return iban;
	}

	public void setIban(String iban) {
		this.iban = iban;
	}

	public BankAccountType getAccountType() {
		return accountType;
	}
	
}
