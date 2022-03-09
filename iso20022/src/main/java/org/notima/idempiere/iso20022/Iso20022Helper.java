package org.notima.idempiere.iso20022;

import org.notima.bankgiro.adempiere.BankAccountUtil;
import org.notima.idempiere.iso20022.entity.pain.BranchAndFinancialInstitutionIdentification4;
import org.notima.idempiere.iso20022.entity.pain.ClearingSystemIdentification2Choice;
import org.notima.idempiere.iso20022.entity.pain.ClearingSystemMemberIdentification2;
import org.notima.idempiere.iso20022.entity.pain.FinancialInstitutionIdentification7;

public class Iso20022Helper {

	
	public static BranchAndFinancialInstitutionIdentification4 getCreditorAgent(BankAccountUtil bau) {
		
		BranchAndFinancialInstitutionIdentification4 bafiit = new BranchAndFinancialInstitutionIdentification4();
		FinancialInstitutionIdentification7 fii = new FinancialInstitutionIdentification7();

		ClearingSystemMemberIdentification2 clrSys = getClearingSystemFor(bau);
		if (clrSys!=null) {
			fii.setClrSysMmbId(clrSys);
		}
		if (bau.getAccountType().equals(BankAccountUtil.BankAccountType.IBAN)) {
			fii.setBIC(bau.getSwift().toUpperCase());
		}
		bafiit.setFinInstnId(fii);
		
		return bafiit;
		
	}

	public static ClearingSystemMemberIdentification2 getClearingSystemFor(BankAccountUtil bau) {

		switch (bau.getAccountType()) {
			case BANKGIRO: 
				return getClearingSystemBankgirot();
			case PLUSGIRO:
				return getClearingSystemPlusgirot();
			case DOMESTIC_BANKACCT:
				return getClearingSystemForDomesticBank(bau.getClearing());
			default:
				break;
		}

		return null;
		
	}
	
	
	public static ClearingSystemMemberIdentification2 getClearingSystemForDomesticBank(String clearing) {
		return getClearingSystemDomesticBankTransfer(clearing);
	}
	
	
	
	private static ClearingSystemMemberIdentification2 getClearingSystemBankgirot() {
		return getClearingSystemDomesticBankTransfer("9900");
	}
	
	private static ClearingSystemMemberIdentification2 getClearingSystemPlusgirot() {
		return getClearingSystemDomesticBankTransfer("9500");
	}

	public static ClearingSystemMemberIdentification2 getClearingSystemDomesticBankTransfer(String clearing) {
		return getClearingSystem("SESBA", clearing);
	}
	
	private static ClearingSystemMemberIdentification2 getClearingSystem(String choice, String mmbId) {
		ClearingSystemMemberIdentification2 clrSys = new ClearingSystemMemberIdentification2(); 
		ClearingSystemIdentification2Choice clrChoice = new ClearingSystemIdentification2Choice();
		clrChoice.setCd(choice);
		clrSys.setClrSysId(clrChoice);
		clrSys.setMmbId(mmbId);
		return clrSys;
	}
	
}
