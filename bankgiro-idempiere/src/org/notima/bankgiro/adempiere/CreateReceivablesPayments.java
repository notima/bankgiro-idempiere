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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.compiere.model.MBPartner;
import org.compiere.model.MBankAccount;
import org.compiere.model.MCurrency;
import org.compiere.model.MInvoice;
import org.compiere.model.MPayment;
import org.compiere.process.DocAction;
import org.compiere.util.CPreparedStatement;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.notima.bankgiro.adempiere.model.MLBSettings;
import org.notima.bg.BgFile;
import org.notima.bg.BgSet;
import org.notima.bg.BgUtil;
import org.notima.bg.Transaction;
import org.notima.bg.bgmax.BgMaxReceipt;
import org.notima.bg.bgmax.BgMaxReference;


/**
 * Process to create payments from a receivables file such as BGMax.
 * 
 * @author Daniel Tamm
 *
 */
public class CreateReceivablesPayments extends PaymentFactory {

	private	BgFile      m_bgFile;

	private static Pattern	documentNoPattern = Pattern.compile("^.*?(\\d+).*?$");	

	public CreateReceivablesPayments(SortedMap<String, MLBSettings> lbSettings, Properties ctx, MBankAccount ourBa, int unknownBPartnerId) {
		super(lbSettings, ctx, ourBa);
	}
	
	/**
	 * Returns the file written. Null if no file has been written yet.
	 * 
	 * @return
	 */
	public java.io.File getFile() {
		return(m_bgFile.getFile());
	}
	
	public void setBgFile(BgFile file) {
		m_bgFile = file;
	}
	
	public BgFile getBgFile() {
		return(m_bgFile);
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

		m_payments = new Vector<PaymentExtendedRecord>();
		String 		trxName = Trx.createTrxName();
		Trx trx = Trx.get(trxName, true);
		BgSet		currentSet;
		Transaction	currentPayment;
        BgMaxReceipt    bgMaxPayment;
		Properties 	ctx = Env.getCtx();
		MCurrency	currency;
		Vector<PaymentExtendedRecord>	paymentsToProcess = new Vector<PaymentExtendedRecord>();
		MPayment payment;
		int			ourBankAccountID = 0;
		
		int			currencyId;
		// Maps to quickly map a bg/pg/bankaccount to a customer (BPartnerID)
		SortedMap<String, Integer>	bgToCustomerMap = new TreeMap<String, Integer>();
		SortedMap<String, Integer>	pgToCustomerMap = new TreeMap<String, Integer>();
		SortedMap<String, Integer>  accountToCustomerMap = new TreeMap<String, Integer>();
		String bg, pg, routing, account;
		Integer bPartnerId;
		
		trx.start();
		
		// Load maps
		String sql = "select C_BPartner_ID, RoutingNo, AccountNo, BP_Bankgiro, BP_Plusgiro from " +
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
			if (routing!=null && account!=null) {
				accountToCustomerMap.put(BgUtil.toDigitsOnly(routing) + "-" + BgUtil.toDigitsOnly(account), bPartnerId);
			}
			if (bg!=null && bg.trim().length()>0) {
				bgToCustomerMap.put(BgUtil.toDigitsOnly(bg), bPartnerId);
			}
			if (pg!=null && pg.trim().length()>0) {
				pgToCustomerMap.put(BgUtil.toDigitsOnly(pg), bPartnerId);
			}
		}
		rs.close();
		ps.close();
		// End of load maps
		
		// Create payments from records in file
		// Iterate through the sets
		for (Iterator<BgSet> setIterator = m_bgFile.getRecords().iterator(); setIterator.hasNext();) {

			currentSet = setIterator.next();
			currency = MCurrency.get(ctx, currentSet.getCurrency());
			currencyId = currency.get_ID();
			if (m_ourBa!=null) {
				ourBankAccountID = m_ourBa.get_ID();
			} else {
				throw new Exception("A bank account must be selected");
			}

			ReceivablesBgPayment rPayment;
			// Iterate through each payment in the set
			for (Iterator<Transaction> payIterator = currentSet.getRecords().iterator(); payIterator.hasNext();) {
				currentPayment = payIterator.next();
                if (currentPayment instanceof BgMaxReceipt) {
                    bgMaxPayment = (BgMaxReceipt)currentPayment;
                    // Add payment to list of payments to process
                    rPayment = createPayment(ctx, bgMaxPayment, trxName, ourBankAccountID, currencyId, currentSet);
                    // Only process the payments if we have a business partner associated with the payment,
                    // the business partner is identified and the invoice is attached and the amount is the same.
                    if (rPayment.getAdempierePayment().getC_BPartner_ID()>0 &&
                    	rPayment.getAdempierePayment().getC_BPartner_ID()!=m_unknownBPartnerId &&
                    	rPayment.getAdempierePayment().getC_Invoice_ID()>0 &&
                    	!rPayment.getAdempierePayment().isOverUnderPayment()
                    	) {
                    	
                    	paymentsToProcess.add(rPayment);
                    	rPayment.setBPartnerIdentified(true);
                    	m_payments.add(0, rPayment); // Add identified first
                    } else {
                    	m_notProcessedPayments.add(rPayment);
                    	rPayment.setBPartnerIdentified(false);
                    	m_payments.add(rPayment); // Add non-identified last
                    }
                    
                }
				
			}
		}
		// Commit changes
		if (!m_dryRun) {
			trx.commit();
		} else {
			trx.rollback();
		}
		
		// If commit works fine, continue to process matched payments (so we don't have to process them
		// manually)
		if (!m_dryRun) {
			trx.start();
			PaymentExtendedRecord pmt;
			for (Iterator<PaymentExtendedRecord> it = paymentsToProcess.iterator(); it.hasNext();) {
				pmt = it.next();
				payment = pmt.getAdempierePayment();
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
		
		return(m_payments);
		
	}

	/**
	 * Creates an MPayment from the currentPayment. A payment is always created regardless of if it is 
	 * matched or not. 
	 * 
	 * If the payment is matched can be checked by seeing if the payment has a bPartnerId set.
	 * 
	 * @param ctx
	 * @param currentPayment
	 * @param trxName
	 * @param ourBankAccountID
	 * @param currencyId
	 * @param currentSet
	 * @return
	 * @throws Exception
	 */
    private ReceivablesBgPayment createPayment(Properties ctx, BgMaxReceipt currentPayment, String trxName, int ourBankAccountID, int currencyId, BgSet currentSet) throws Exception {
		MInvoice	invoice = null;
        // Try to find the invoice paid using our invoice no
        // Get document no
        String documentNo = currentPayment.getReference();
        String ocr = null;
        // If the reference type is OCR, we are pretty safe and the invoice no is the 
        // OCR-number minus the last digit.
        if (currentPayment.getReferenceType()==BgMaxReference.REFTYPE_OCR) {
            ocr = documentNo;
            invoice = lookupInvoiceOCR(ctx, ocr);
        } else {
        	Matcher m;
        	m = documentNoPattern.matcher(documentNo);
        	if (m.matches()) {
        		documentNo = m.group(1);
        		invoice = lookupInvoice(ctx, documentNo);
        	}
        }
        // If we don't have a documentNo at this point, try to find it in other fields
        if (invoice==null && documentNo==null || documentNo.trim().length()==0) {
        	Vector<String> infoRecords = currentPayment.getInfoRecords();
        	Matcher m;
        	for (Iterator<String> it = infoRecords.iterator(); it.hasNext();) {
        		// Look for numbers in the info records
        		m = documentNoPattern.matcher(it.next());
        		if (m.matches()) {
        			documentNo = m.group(1);
        			invoice = lookupInvoice(ctx, documentNo);
        			if (invoice!=null) break;
        		}
        	}
        }
        if (invoice==null) {
        	invoice = lookupInvoice(ctx, documentNo);
        }

        ReceivablesBgPayment rPayment = new ReceivablesBgPayment();
        MPayment payment = new MPayment(ctx, 0, trxName);
        payment.set_ValueOfColumn("AD_Client_ID", Integer.valueOf(m_clientId));
        payment.setAD_Org_ID(m_orgId);
        payment.setC_DocType_ID(true); // AR Receipt
        payment.setIsReceipt(true);
        payment.setC_BankAccount_ID(ourBankAccountID);
        if (invoice!=null) {
        	payment.setC_BPartner_ID(invoice.getC_BPartner_ID());
        } else {
        	payment.setC_BPartner_ID(m_unknownBPartnerId);
        }
        payment.setC_Currency_ID(currencyId);
        payment.setTenderType(BGPaymentModelValidator.TENDERTYPE_Bankgiro);
        payment.setDescription(
        		currentPayment.getReference() + 
        		(currentPayment.getName1()!=null ? " : " + currentPayment.getName1() : "")
        		);
        payment.setDateAcct(new Timestamp(currentPayment.getTransactionDate().getTime()));
        payment.setDateTrx(new Timestamp(currentPayment.getTransactionDate().getTime()));
        payment.setAmount(currencyId, new BigDecimal(currentPayment.getAmount()));
        if (ocr!=null)
            payment.set_CustomColumn("OCR", ocr);
        if (invoice!=null) {
	        payment.setC_Invoice_ID(invoice.get_ID());
	        payment.set_CustomColumn("IsOCR", invoice.get_Value("IsOCR"));
	        double openAmount = invoice.getOpenAmt().doubleValue();
	        if (currentPayment.getAmount()!=openAmount) {
	            payment.setOverUnderAmt(new BigDecimal(new Double(openAmount-currentPayment.getAmount()).toString()));
	        }
	        payment.setIsApproved(true);
	        rPayment.setInvoice(invoice);
        }
        if (payment.getC_BPartner_ID()>0) {
        	rPayment.setBPartner(new MBPartner(ctx, payment.getC_BPartner_ID(), trxName));
        } else {
        	throw new Exception("A payment must have a business partner. Have you defined the unknown payer BPartner?");
        }
        payment.saveEx(trxName);
        rPayment.setAdempierePayment(payment);
        rPayment.setBgPayment(currentPayment);
        return(rPayment);
    }



}
