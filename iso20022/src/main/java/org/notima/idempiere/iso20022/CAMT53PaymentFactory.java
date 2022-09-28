package org.notima.idempiere.iso20022;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MBankAccount;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MPayment;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.jfree.util.Log;
import org.notima.bankgiro.adempiere.PaymentExtendedRecord;
import org.notima.bankgiro.adempiere.PluginRegistry;
import org.notima.bankgiro.adempiere.model.MLBSettings;
import org.notima.bg.BgUtil;
import org.notima.idempiere.iso20022.entity.camt.AccountIdentification4Choice;
import org.notima.idempiere.iso20022.entity.camt.AccountSchemeName1Choice;
import org.notima.idempiere.iso20022.entity.camt.AccountStatement2;
import org.notima.idempiere.iso20022.entity.camt.ActiveOrHistoricCurrencyAndAmount;
import org.notima.idempiere.iso20022.entity.camt.AmountAndCurrencyExchange3;
import org.notima.idempiere.iso20022.entity.camt.AmountAndCurrencyExchangeDetails3;
import org.notima.idempiere.iso20022.entity.camt.BankToCustomerStatementV02;
import org.notima.idempiere.iso20022.entity.camt.CashAccount16;
import org.notima.idempiere.iso20022.entity.camt.CashAccount20;
import org.notima.idempiere.iso20022.entity.camt.CreditDebitCode;
import org.notima.idempiere.iso20022.entity.camt.CreditorReferenceInformation2;
import org.notima.idempiere.iso20022.entity.camt.DateAndDateTimeChoice;
import org.notima.idempiere.iso20022.entity.camt.DocumentCAMT53;
import org.notima.idempiere.iso20022.entity.camt.EntryDetails1;
import org.notima.idempiere.iso20022.entity.camt.EntryTransaction2;
import org.notima.idempiere.iso20022.entity.camt.GenericAccountIdentification1;
import org.notima.idempiere.iso20022.entity.camt.PartyIdentification32;
import org.notima.idempiere.iso20022.entity.camt.ProprietaryReference1;
import org.notima.idempiere.iso20022.entity.camt.RemittanceAmount1;
import org.notima.idempiere.iso20022.entity.camt.RemittanceInformation5;
import org.notima.idempiere.iso20022.entity.camt.ReportEntry2;
import org.notima.idempiere.iso20022.entity.camt.StructuredRemittanceInformation7;
import org.notima.idempiere.iso20022.entity.camt.TransactionParty2;
import org.notima.idempiere.iso20022.entity.camt.TransactionReferences2;

public class CAMT53PaymentFactory {

	
	private Iso20022PaymentFactory				paymentFactory;
	private List<PaymentExtendedRecord> result = new ArrayList<PaymentExtendedRecord>();
	boolean receivablesOnly = false;
	private SortedMap<String, MLBSettings> lbSettings;
	private MLBSettings msgPrefix;
	private String	msgPrefixStr;
	private DocumentCAMT53 document = new DocumentCAMT53();
	
	private File f;

	public CAMT53PaymentFactory(Iso20022PaymentFactory pf) {
		paymentFactory = pf;
	}

	/**
	 * Initializes the file reading. If the file format is wrong, false is returned.
	 * 
	 * @return
	 */
	public boolean initFileReading() throws Exception {

		f = paymentFactory.getFile();
		
		paymentFactory.getLogger().fine("Reading file " + f.getAbsolutePath());
		

		// Get message id prefix
		lbSettings = PluginRegistry.registry
				.getLbSettings();
		msgPrefix = lbSettings
				.get(Iso20022FileFactory.ISO20022_MSGPREFIX);
		if (msgPrefix == null)
			throw new Exception(
					"No ISO Message prefix configured. Check LB-settings");
		msgPrefixStr = msgPrefix.getName();

		// Correct for Handelsbanken sending files in non UTF-8 format
		Charset cs = Charset.forName("ISO-8859-15");
		InputStreamReader reader = new InputStreamReader(
				new FileInputStream(f), cs);

		try {
			JAXBContext contextObj = JAXBContext.newInstance(DocumentCAMT53.class);
			Unmarshaller marshallerObj = contextObj.createUnmarshaller();
			document = (DocumentCAMT53) marshallerObj.unmarshal(reader);
			reader.close();
		} catch (javax.xml.bind.UnmarshalException eu) {
			reader.close();
			return false;
		} 
		
		return true;
		
	}
	
	
	public List<PaymentExtendedRecord> getSourcePayments(Properties props)
			throws Exception {
		
		if (props!=null && props.containsKey(Iso20022PaymentFactory.PROP_RECEIVABLES_ONLY) && props.get(Iso20022PaymentFactory.PROP_RECEIVABLES_ONLY)!=null) {
			receivablesOnly = true;
		}

		PaymentExtendedRecord rec;

		// Get BankToCustomerStatement (can be many accounts)
		BankToCustomerStatementV02 cct = document.getBkToCstmrStmt();

		// Get statement entries (one per bank account to be reconciled)
		List<AccountStatement2> statements = cct.getStmt();

		MBankAccount ba = null;
		String ourRef;

		String trxName = null;

		for (AccountStatement2 s : statements) {

			// Get bank account
			CashAccount20 acct = s.getAcct();
			ba = lookupBankAccount(acct);
			paymentFactory.setBankAccount(ba);
			boolean finnishCod = false;

			// Get a list of entries
			List<ReportEntry2> entries = s.getNtry();
			for (ReportEntry2 e : entries) {

				// Get indicator (debit = outgoing payments) / credit = incoming
				// payments
				CreditDebitCode crDbCode = e.getCdtDbtInd();

				// Get currency and total
				ActiveOrHistoricCurrencyAndAmount amt = e.getAmt();

				// Get book date
				DateAndDateTimeChoice dte = e.getBookgDt();

				// Get entry details
				List<EntryDetails1> lst = e.getNtryDtls();

				if (CreditDebitCode.CRDT.equals(crDbCode)) {

					// Process incoming payments
					for (EntryDetails1 det : lst) {

						// Reset finnish C.O.D.
						finnishCod = false;
						
						List<EntryTransaction2> entList = det.getTxDtls();

						for (EntryTransaction2 ee : entList) {

							// Get related parties (debtor)
							TransactionParty2 parties = ee.getRltdPties();
							if (parties!=null) {
								PartyIdentification32 dbtr = parties.getDbtr();
								if (dbtr!=null && "MATKAHUOLTO OY AB".equals(dbtr.getNm())) {
									// Finnish Cash-On-Delivery
									finnishCod = true;
								}
							}
							
							RemittanceInformation5 rmtInf = ee.getRmtInf();
							if (rmtInf!=null) {
								List<StructuredRemittanceInformation7> rmtInfList = rmtInf
										.getStrd();
								// Get remittance information (information about
								// incoming payments)
								for (StructuredRemittanceInformation7 rr : rmtInfList) {
	
									rec = new PaymentExtendedRecord();
									rec.setBankAccountPtr(ba);
									
									CreditorReferenceInformation2 reference = rr
											.getCdtrRefInf();
									ourRef = reference.getRef();
	
									RemittanceAmount1 ramt = rr.getRfrdDocAmt();
									if (ramt!=null) {
										amt = ramt.getRmtdAmt();
									} else {
										amt = ee.getAmtDtls().getTxAmt().getAmt();
									}
	
									// Remove last digit since its a KID
									ourRef = ourRef.substring(0, ourRef.length()-1);
									if (finnishCod) {
										// Remove prefixing zeroes
										ourRef = BgUtil.trimLeadingZeros(ourRef);
									}
	
									rec.setPaymentReference(ourRef);
									rec.setCurrency(amt.getCcy());
									rec.setOrderSum(amt.getValue().doubleValue());
									rec.setTrxDate(dte.getDt().toGregorianCalendar()
										.getTime());
	
									
									// Check invoice
									MInvoice invoice = new Query(
											Env.getCtx(),
											MInvoice.Table_Name,
											"AD_Client_ID=? AND DocumentNo=? AND IsSOTrx='Y'",
											trxName).setParameters(
											new Object[] {
													Env.getAD_Client_ID(Env.getCtx()),
													ourRef }).firstOnly();
	
									if (invoice != null) {
										rec.setInvoice(invoice);
										rec.setInvoiceNo(ourRef);
										rec.setBPartner(new MBPartner(Env.getCtx(),
												invoice.getC_BPartner_ID(), trxName));
										
										result.add(rec);
										
									} else {
										Log.warn("Can't match invoice " + ourRef);
									}
									
									// If no payment is created here, it's created later
									// createPayment(ba, invoice, rec.getTrxDate(), true, rec.getOrderSum(), amt.getCcy(), trxName);
								} // End of structured information
	
								// Check unstructured information
								List<String> references = rmtInf.getUstrd();
								
								if ((rmtInfList==null || rmtInfList.isEmpty()) && (references!=null && !references.isEmpty())) {
									
									rec = new PaymentExtendedRecord();
									rec.setBankAccountPtr(ba);
									
									if (ee.getAmtDtls()!=null) {
										amt = ee.getAmtDtls().getTxAmt().getAmt();
									} else {
										amt = e.getAmt();
									}
	
									// Take first
									ourRef = references.get(0).trim();
									
									rec.setPaymentReference(ourRef);
									rec.setCurrency(amt.getCcy());
									rec.setOrderSum(amt.getValue().doubleValue());
									rec.setTrxDate(dte.getDt().toGregorianCalendar()
										.getTime());
	
									
									// Check invoice
									MInvoice invoice = new Query(
											Env.getCtx(),
											MInvoice.Table_Name,
											"AD_Client_ID=? AND DocumentNo=? AND IsSOTrx='Y'",
											trxName).setParameters(
											new Object[] {
													Env.getAD_Client_ID(Env.getCtx()),
													ourRef }).setOrderBy("dateacct desc").firstOnly();
	
									if (invoice != null) {
										rec.setInvoice(invoice);
										rec.setInvoiceNo(ourRef);
										rec.setBPartner(new MBPartner(Env.getCtx(),
												invoice.getC_BPartner_ID(), trxName));
										
										result.add(rec);
										
									} else {
										Log.warn("Can't match invoice " + ourRef);
									}
									
									
									
								}
								
							} // End of remittance information !=null
						} // End of transaction loop

					}

				} else if (!receivablesOnly) {

					// Process outgoing payments

					for (EntryDetails1 det : lst) {

						List<EntryTransaction2> entList = det.getTxDtls();

						for (EntryTransaction2 ee : entList) {

							rec = new PaymentExtendedRecord();
							rec.setBankAccountPtr(ba);
							
							TransactionReferences2 tr2 = ee.getRefs();
							// Make sure we have initiated this transaction
							String msgId = tr2.getMsgId();
							if (msgId == null
									|| !msgId.startsWith(msgPrefixStr)) {
								// Not a transaction initiated by us
								continue;
							}

							// Get our invoice no
							ourRef = tr2.getEndToEndId();
							ProprietaryReference1 theirRef1 = tr2.getPrtry();
							String theirRef = theirRef1.getRef();
							rec.setBpInvoiceNo(theirRef);

							// Check invoice
							MInvoice invoice = new Query(
									Env.getCtx(),
									MInvoice.Table_Name,
									"AD_Client_ID=? AND DocumentNo=? AND PaymentRule='Z'",
									trxName).setParameters(
									new Object[] {
											Env.getAD_Client_ID(Env.getCtx()),
											ourRef }).firstOnly();

							if (invoice != null) {
								rec.setInvoice(invoice);
								rec.setBPartner(new MBPartner(Env.getCtx(),
										invoice.getC_BPartner_ID(), trxName));
							} else {
								throw new Exception("Can't match invoice "
										+ ourRef);
							}

							AmountAndCurrencyExchange3 aace = ee.getAmtDtls();
							AmountAndCurrencyExchangeDetails3 aaced = aace
									.getTxAmt();
							ActiveOrHistoricCurrencyAndAmount amount = aaced
									.getAmt();

							rec.setInvoiceNo(ourRef);
							rec.setCurrency(amount.getCcy());
							rec.setOrderSum(amount.getValue().doubleValue());
							rec.setTrxDate(dte.getDt().toGregorianCalendar()
									.getTime());
							// The last trx date will be trx date for the whole
							// file.
							paymentFactory.setTrxDate(rec.getTrxDate());

							TransactionParty2 trp = ee.getRltdPties();
							PartyIdentification32 pid = trp.getCdtr();
							rec.setName(pid.getNm());

							CashAccount16 rcptAcct = trp.getCdtrAcct();
							AccountIdentification4Choice acctId = rcptAcct
									.getId();
							String rcptAcctStr;
							String iban = acctId.getIBAN();
							if (iban == null) {

								GenericAccountIdentification1 ai = acctId
										.getOthr();
								rcptAcctStr = ai.getId();

							} else {
								rcptAcctStr = iban;
							}
							rec.setBankAccount(rcptAcctStr);

							boolean arCreditMemo = false;
							// Check document type
							if (invoice != null) {
								arCreditMemo = MDocType.DOCBASETYPE_ARCreditMemo
										.equalsIgnoreCase(invoice
												.getC_DocType()
												.getDocBaseType());
								if (arCreditMemo) {
									rec.setOrderSum(-rec.getOrderSum());
								}
							}

							// Create payment
							if (!arCreditMemo) {
								try {
									MPayment pmt = paymentFactory.createPayment(ba, invoice,
											rec.getTrxDate(), false, amount
													.getValue().doubleValue(),
											amount.getCcy(), trxName);
		
									rec.setAdempierePayment(pmt);
								} catch (AdempiereException ae) {
									Log.warn(ae.getMessage() + " invoice " + invoice.getDocumentNo());
									rec.setInvoice(invoice);
								}
							} else {
								// Create the payment later if credit memo
								rec.setInvoice(invoice);
							}

							result.add(rec);
						}
					}

				} // End of processing outgoing payments

			}
		}

		return result;
	}

	/**
	 * Matches account information to the account in Adempiere.
	 * 
	 * @param acct
	 * @return
	 */
	private MBankAccount lookupBankAccount(CashAccount20 acct) throws Exception {

		MBankAccount ba = null;

		AccountIdentification4Choice acctId = acct.getId();
		String iban = acctId.getIBAN();
		String id = null;
		// Check for iban match
		if (iban != null) {
			ba = new Query(Env.getCtx(), MBankAccount.Table_Name,
					"IBAN=? AND AD_Client_ID=?", null).setParameters(
					new Object[] { iban, Env.getAD_Client_ID(Env.getCtx()) })
					.first();
		}

		if (ba == null) {
			GenericAccountIdentification1 ai = acctId.getOthr();
			id = ai.getId();
			AccountSchemeName1Choice asc = ai.getSchmeNm();
			String schemeName = asc.getCd();

			if ("BBAN".equalsIgnoreCase(schemeName)) {
				ba = new Query(
						Env.getCtx(),
						MBankAccount.Table_Name,
						"translate(concat(bban, accountno), '-., ','')=? AND AD_Client_ID=?",
						null).setParameters(
						new Object[] { id, Env.getAD_Client_ID(Env.getCtx()) })
						.first();
			}

		}

		if (ba == null) {
			throw new Exception(
					"Can't find account " + iban != null ? ("IBAN: " + iban)
							: ("BBAN: " + id));
		}
		return ba;
	}
	
}
