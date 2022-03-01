package org.notima.bankgiro.adempiere;

import org.compiere.model.*;
import org.notima.bg.bgmax.BgMaxReceipt;


/**
 * Bean class to use for reporting (Jasper Reports)
 * 
 * @author Daniel Tamm
 *
 */
public class ReceivablesBgPayment extends PaymentExtendedRecord {

	private BgMaxReceipt	m_bgPayment;
	
	public BgMaxReceipt getBgPayment() {
		return m_bgPayment;
	}
	public void setBgPayment(BgMaxReceipt payment) {
		m_bgPayment = payment;
	}
	
}
