package org.notima.bankgiro.adempiere;

import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

import org.compiere.model.MBankAccount;
import org.compiere.model.MInvoice;
import org.compiere.util.DB;
import org.compiere.util.Trx;

/**
 * <p>Convenience class to create an outbound payment file.</p>
 * 
 * <p>The payment file factory is normally retrieved using the PluginRegistry.</p>
 * 
 * @author Daniel Tamm
 * 
 * @see		PluginRegistry
 * @see		PaymentFileFactory
 *
 */
public class PaymentFileWorker {

	/**
	 * 
	 * @param factory
	 * @param senderAcct
	 * @param payments
	 * @param dstDir
	 * @return		Null if successful. An error message if not successful.
	 */
	public static String createFile(PaymentFileFactory factory, MBankAccount senderAcct, List<LbPaymentRow> payments, File dstDir) {
		
        // Validate payments
        String msg;
        PaymentValidator pv = factory.getPaymentValidator();
        if (pv!=null) {
	        for (LbPaymentRow pmt : payments) {
	        	msg = pv.validatePayment(senderAcct, pmt);
	        	if (msg!=null) {
	        		msg += " : Invoice " + pmt.getInvoice().getDocumentNo() + " : BPartner " + pmt.getBpartner().getName();
	        		return msg;
	        	} 
	        }
        }

        String trxName = Trx.createTrxName();
        Trx trx = Trx.get(trxName, true);
        
        trx.start();

        File result = null;
        
        try {
        
	        result = factory.createPaymentFile(senderAcct, payments, dstDir, trxName);
	        
	        if (result!=null) {
	        	// Update status of payments
	        	MInvoice invoice;
	        	Timestamp prodDate = new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis());
	
		        	for (LbPaymentRow pmt : payments) {
		
		        		BigDecimal PayAmt = BigDecimal.valueOf(pmt.payAmount);
		        		invoice = pmt.getInvoice();
		                // Add invoice to vector of processed invoices
		                invoice.set_CustomColumn("LbStatus", Integer.valueOf(1)); // Sent
		                invoice.set_CustomColumn("LbAmount", pmt.arCreditMemo ? PayAmt.negate() : PayAmt); // Amount sent
		                invoice.set_CustomColumn("PayDateBG", pmt.payDate);
		                invoice.set_CustomColumn("LBProdDate", prodDate);
		                invoice.set_CustomColumn("LBProdSrcBankAccount_id", senderAcct.get_ID());
		                invoice.saveEx(trxName);
		                
		            }
		            DB.commit(true, trxName);
	        } else {
	        	DB.rollback(false, trxName);
	        	return "File creation failed.";
	        }
        
        } catch (Exception e) {
        	// Rollback transaction
        	try {
				DB.rollback(true, trxName);
			} catch (SQLException e1) {
				trx.close();
				e1.printStackTrace();
				return e1.getMessage();
			}
        }

        trx.close();
        
        MessageCenter.info(result.getAbsolutePath() + " was created.");
		
		return null;
	}
	
}
