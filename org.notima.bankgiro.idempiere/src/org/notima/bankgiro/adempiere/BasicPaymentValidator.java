package org.notima.bankgiro.adempiere;

import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MBankAccount;
import org.compiere.model.MInvoice;
import org.compiere.util.Env;
import org.notima.bg.BgUtil;

/**
 * Validates that the payment can be sent, before the actual file creation starts.
 * 
 * @author daniel.tamm
 *
 */
public class BasicPaymentValidator implements PaymentValidator {

	/**
	 * Does basic check such as making sure there are valid references to send to the payee
	 * and that a destination bank account is defined.
	 * 
	 * If the destination bank account is found, the dstAccount field of pmt is set.
	 */
	@Override
	public String validatePayment(MBankAccount srcAccount, LbPaymentRow pmt) {

		MInvoice invoice = pmt.getInvoice();
        // Set information from the invoice
        int C_BPartner_ID = invoice.getC_BPartner_ID();
        boolean isOCR = invoice.get_Value("isOCR")!=null && "Y".equalsIgnoreCase(invoice.get_ValueAsString("isOCR"));
        String OCR = (String)invoice.get_Value("OCR"); 				    // OCR reference
        String BPInvoiceNo = (String)invoice.get_Value("BPDocumentNo");		// Invoice number if not OCR
        
        // Check for reference to payee
        if (isOCR && !BgUtil.isValidOCRNumber(OCR)) {
        	return "Invoice does't have a valid OCR-reference";
        }

        // Check for invoice number on vendor payments
        if (!invoice.isSOTrx()) {
	        if (!isOCR && (BPInvoiceNo==null || BPInvoiceNo.trim().length()==0)) {
	        	return "Invoice is missing BP Invoice No";
	        }
        }

        // Try getting receiver bank account
    	MBPartner bp = new MBPartner(Env.getCtx(), C_BPartner_ID, null);
        MBPBankAccount receiverBankAccount = null;
        
        try {
        	receiverBankAccount = LbPaymentRow.getDestinationAccount(C_BPartner_ID);        
        } catch (Exception eee) {
        	eee.printStackTrace();
        	return(bp.getName() + ": " + eee.getMessage());
        }

        if (receiverBankAccount==null) {
        	return bp.getName() + " has no bank account definied";
        } else 
        	pmt.setDstAccount(receiverBankAccount);
		
		return null;
	}

}
