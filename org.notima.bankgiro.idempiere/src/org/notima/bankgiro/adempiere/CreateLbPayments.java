package org.notima.bankgiro.adempiere;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JOptionPane;

import org.compiere.model.MBPartner;
import org.compiere.model.MBankAccount;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MPayment;
import org.compiere.process.DocAction;
import org.compiere.util.CPreparedStatement;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.notima.bankgiro.adempiere.model.MLBSettings;
import org.notima.bg.BgSet;
import org.notima.bg.BgUtil;
import org.notima.bg.LbFile;
import org.notima.bg.Transaction;
import org.notima.bg.lb.LbPayment;
import org.notima.bg.lb.LbSet;


/**
 * Process to create payments from an LB-file (reconciliation file).
 * @author Daniel Tamm
 *
 */
public class CreateLbPayments extends PaymentFactory {

	private	LbFile		m_lbFile;
	
	public CreateLbPayments(SortedMap<String, MLBSettings> lbSettings, Properties ctx, MBankAccount ourBa, int unknownBPartnerId, LbFile file) {
		super(lbSettings, ctx, ourBa);
		m_lbFile = file;
	}

	/**
	 * Returns the file written. Null if no file has been written yet.
	 * 
	 * @return
	 */
	public java.io.File getFile() {
		return(m_lbFile.getFile());
	}
	
	public LbFile getLbFile() {
		return(m_lbFile);
	}

	/**
	 * This method is not used since createPayments(Properties) is overridden.
	 */
	@Override
	protected List<PaymentExtendedRecord> getSourcePayments(Properties props)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	protected List<PaymentExtendedRecord> createPayments(Properties props) throws Exception {

		String 		trxName = Trx.createTrxName();
		Trx trx = Trx.get(trxName, true);
		LbSet		currentSet;
		LbPayment	currentPayment;
		MInvoice	invoice;
		Properties 	ctx = Env.getCtx();
		MCurrency	currency;
		MBankAccount	ourBa;
		Vector<MPayment>	paymentsToProcess = new Vector<MPayment>();
		MPayment payment;
		int			ourBankAccountID = 0;
		boolean		unknownFlag = false;
		m_payments = new Vector<PaymentExtendedRecord>();
		PaymentExtendedRecord	rPayment;
		
		int			currencyId;
		// Maps to quickly map a bg/pg/bankaccount/iban to a customer (BPartnerID)
		SortedMap<String, Integer>	bgToCustomerMap = new TreeMap<String, Integer>();
		SortedMap<String, Integer>	pgToCustomerMap = new TreeMap<String, Integer>();
		SortedMap<String, Integer>  accountToCustomerMap = new TreeMap<String, Integer>();
		SortedMap<String, Integer>  ibanToCustomerMap = new TreeMap<String, Integer>();
		String bg, pg, routing, account, iban, swift;
		Integer bPartnerId;
		
		trx.start();
		// Load maps
		String sql = "select C_BPartner_ID, RoutingNo, AccountNo, BP_Bankgiro, BP_Plusgiro, IBAN, SwiftCode from " +
					 org.compiere.model.MBPBankAccount.Table_Name + 
					 "  where AD_Client_ID=? and (AD_Org_ID=0 or AD_Org_ID=?) and IsLbRutin='Y'";
		CPreparedStatement ps = DB.prepareStatement(sql, trxName);
		int c = 1;
		ps.setInt(c++, m_clientId);
		ps.setInt(c++, m_orgId);
		ResultSet rs = ps.executeQuery();
		while(rs.next()) {
			bPartnerId = rs.getInt(1);
			routing = rs.getString(2);
			account = rs.getString(3);
			bg = rs.getString(4);
			pg = rs.getString(5);
			iban = rs.getString(6);
			swift = rs.getString(7);
			if (routing!=null && account!=null) {
				accountToCustomerMap.put(BgUtil.toDigitsOnly(routing) + "-" + BgUtil.toDigitsOnly(account), bPartnerId);
			}
			if (bg!=null && bg.trim().length()>0) {
				bgToCustomerMap.put(BgUtil.toDigitsOnly(bg), bPartnerId);
			}
			if (pg!=null && pg.trim().length()>0) {
				pgToCustomerMap.put(BgUtil.toDigitsOnly(pg), bPartnerId);
			}
			if (iban!=null) {
				iban = iban.trim().toUpperCase();
				ibanToCustomerMap.put(iban, bPartnerId);
			}
			if (swift!=null) {
				swift = swift.trim().toUpperCase();
			}
		}
		rs.close();
		ps.close();
		// End of load maps

		// Create payments from records in file

		for (Iterator<BgSet> setIterator = m_lbFile.getRecords().iterator(); setIterator.hasNext();) {
			currentSet = (LbSet)setIterator.next();
			currency = MCurrency.get(ctx, currentSet.getCurrency());
			currencyId = currency.get_ID();
			// Lookup our bank account
			ourBa = m_ourBa;
			if (ourBa!=null) {
				ourBankAccountID = ourBa.get_ID();
				// Compare account no with account in file.
				String accountNo = BgUtil.toDigitsOnly(ourBa.getAccountNo());
				boolean isBankgiro = (Boolean)ourBa.get_Value("IsBankgiro");
				String AD_Bankgiro = BgUtil.toDigitsOnly(ourBa.get_ValueAsString("AD_Bankgiro"));
				String compare = isBankgiro ? accountNo : AD_Bankgiro;
				if (!compare.equals(currentSet.getSenderBankAccount())) {
					throw new Exception("The file's BG account (" + currentSet.getSenderBankAccount() + 
							") and the selected account (" + compare + ") doesn't match");
				}
			} else {
				throw new Exception("We have no bank account with BG: " + currentSet.getSenderBankAccount());
			}
			
			for (Iterator<Transaction> payIterator = currentSet.getRecords().iterator(); payIterator.hasNext();) {
				bPartnerId = null;
				unknownFlag = false;
				currentPayment = (LbPayment)payIterator.next();
				rPayment = new PaymentExtendedRecord();
				
				// Try to find the invoice paid using our reference
				invoice = lookupInvoice(ctx, currentPayment.getOurRef());
				if (invoice!=null) {
					bPartnerId = invoice.getC_BPartner_ID();
					invoice.set_ValueOfColumn("LbStatus", LbPaymentRow.PAYSTATUS_PAID);
					invoice.saveEx(trxName);
					rPayment.setInvoice(invoice);
				} else {
					
					// Try to find business partner using payment
					if (currentPayment.isBgPayment()) {
						bPartnerId = bgToCustomerMap.get(currentPayment.getDstBg()); 
					}
					if (currentPayment.isPgPayment()) {
						bPartnerId = pgToCustomerMap.get(currentPayment.getDstPg());
					}
					if (currentPayment.isAccountPayment()) {
						bPartnerId = accountToCustomerMap.get(BgUtil.toDigitsOnly(currentPayment.getDstAccount()));
					}
					
					if (bPartnerId==null) {
						bPartnerId = m_unknownBPartnerId;
						unknownFlag = true;
					}
				}

				boolean arCreditMemo = false;
				// Check document type
				if (invoice!=null) {
					arCreditMemo = MDocType.DOCBASETYPE_ARCreditMemo.equalsIgnoreCase(invoice.getC_DocType().getDocBaseType());
					if (arCreditMemo) {
						currentPayment.setAmount(-currentPayment.getAmount());
					}
				}
				
				payment = new MPayment(ctx, 0, trxName);
				payment.set_ValueOfColumn("AD_Client_ID", Integer.valueOf(m_clientId));
				payment.setAD_Org_ID(m_orgId);
				payment.setC_DocType_ID(arCreditMemo); // AP Payment
				payment.setC_BankAccount_ID(ourBankAccountID);
				payment.setC_BPartner_ID(bPartnerId);
				payment.setC_Currency_ID(currencyId);
				payment.set_CustomColumn("AD_Bankgiro", currentSet.getSenderBankAccount());
				payment.set_CustomColumn("OCR", currentPayment.getOcr());
				payment.set_CustomColumn("BP_Bankgiro", currentPayment.getDstBg());
				payment.set_CustomColumn("BP_Plusgiro", currentPayment.getDstPg());
				payment.setTenderType(BGPaymentModelValidator.TENDERTYPE_Bankgiro);
				payment.setDescription((currentPayment.getDstName()!=null ? (currentPayment.getDstName() + " : ") : "") + currentPayment.getOurRef());
				payment.setDateAcct(new Timestamp(currentPayment.getTransactionDate().getTime()));
				payment.setDateTrx(new Timestamp(currentPayment.getTransactionDate().getTime()));
				// Make sure amount is rounded
				currentPayment.setAmount(Math.round(currentPayment.getAmount()*100)/100.0);
				payment.setAmount(currencyId, new BigDecimal(Double.toString(currentPayment.getAmount())));
				if (invoice!=null) {
					payment.setC_Invoice_ID(invoice.get_ID());
					payment.set_CustomColumn("IsOCR", invoice.get_Value("IsOCR"));
					double openAmount = invoice.getOpenAmt().doubleValue();
					double pmtDiff = openAmount - currentPayment.getAmount();
					if (pmtDiff!=0) {
						if (Math.abs(pmtDiff)>m_amountThreshold) {
							payment.setOverUnderAmt(new BigDecimal(new Double(pmtDiff).toString()));
						} else {
							payment.setDiscountAmt(new BigDecimal(new Double(pmtDiff).toString()));
						}
					}
				} else {  // TODO: Remove temporary charge when done 
					payment.setC_Charge_ID(1000008);
				}
				payment.setIsApproved(true);
				payment.saveEx(trxName);
				rPayment.setAdempierePayment(payment);
				rPayment.setBPartner(new MBPartner(ctx, bPartnerId, trxName));
				rPayment.setBPartnerIdentified(!unknownFlag);
				rPayment.setTransaction(currentPayment);
				m_payments.add(rPayment);
				// Add payment to list of payments to process
				// Only process payments connected to an invoice
				if (m_autoComplete && !unknownFlag && invoice!=null && !payment.isOverUnderPayment()) paymentsToProcess.add(payment);
			}
		}
		
		// Commit changes
		if (!m_dryRun) {
			trx.commit();
		} else {
			trx.rollback();
		}
		
		// If commit works fine, continue to process all sure payments (so we don't have to process them
		// manually)
		if (!m_dryRun) {
			trx.start();
			for (Iterator<MPayment> it = paymentsToProcess.iterator(); it.hasNext();) {
				payment = it.next();
				try {
					boolean success = payment.processIt(DocAction.ACTION_Complete);
					if (success) {
						payment.saveEx(trxName);
					}
				} catch (Exception ee) {
					System.err.println("Could not complete payment " + payment.getDocumentNo() + " : " + ee.getMessage());
				}
			}
			trx.commit();
		}
		
		trx.close();
		int diff = m_payments.size() - paymentsToProcess.size();
		if (diff!=0) {
			if (diff==1) {
				JOptionPane.showMessageDialog(null, diff + " payment was not completed automatically.");
			} else {
				JOptionPane.showMessageDialog(null, diff + " payments were not completed automatically.");
			}
		}
		return(m_payments);
	}

	

}
