package org.notima.bankgiro.adempiere;

import java.io.File;
import java.util.List;

import org.compiere.model.MBankAccount;

/**
 * <p>Interface for creating outbound payment files.</p>
 * 
 * <p>The class implementing the interface receives the source bank account, 
 * a list of suggested payments, directory where to place the outbound file
 * and the current transaction (if any).</p>
 * 
 * <p>With this information the implementing class is responsible to translate the
 * payments into a specific file format.</p>
 * 
 * <p>Normally a specific PaymentFileFactory is invoked from the class PaymentFileWorker.</p>
 * 
 * <p>PaymentFileFactories are tracked by the PluginRegistry.</p>
 * 
 * <p>The PaymentFileWorker itself is normally invoked from a payment plugin.</p>
 * 
 * <p>NOTE: For inbound payment files, @see PaymentFactory</p>
 * 
 * @author Daniel Tamm
 * @see		PaymentFileWorker
 * @see		PluginRegistry		
 *
 */
public interface PaymentFileFactory {

	/**
	 * Creates a payment file
	 * 
	 * @param	srcAccount			Source bank account
	 * @param	suggestedPayments	List of suggested payments
	 * @param	outDir				Where the out file should be placed
	 * @param	trxName				Transaction
	 * 
	 * @return		A file
	 */
	public File createPaymentFile(MBankAccount srcAccount, List<LbPaymentRow> suggestedPayments, File outDir, String trxName);

	/**
	 * Returns the validator used to validate payments for this file factory
	 * 
	 * @return
	 */
	public PaymentValidator getPaymentValidator();
	
	/**
	 * Key by which this payment file factory is referred by
	 * 
	 * @return
	 */
	public String getKey();
	
	
	/**
	 * Full name of this payment file factory
	 * 
	 * @return
	 */
	public String getName();
	
	
}
