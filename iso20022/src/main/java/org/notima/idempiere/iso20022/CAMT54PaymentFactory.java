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
import org.notima.bg.lb.LbPayment;

import iso.std.iso._20022.tech.xsd.camt_054_001.AccountIdentification4Choice;
import iso.std.iso._20022.tech.xsd.camt_054_001.AccountNotification2;
import iso.std.iso._20022.tech.xsd.camt_054_001.AccountSchemeName1Choice;
import iso.std.iso._20022.tech.xsd.camt_054_001.ActiveOrHistoricCurrencyAndAmount;
import iso.std.iso._20022.tech.xsd.camt_054_001.AmountAndCurrencyExchange3;
import iso.std.iso._20022.tech.xsd.camt_054_001.AmountAndCurrencyExchangeDetails3;
import iso.std.iso._20022.tech.xsd.camt_054_001.BankToCustomerDebitCreditNotificationV02;
import iso.std.iso._20022.tech.xsd.camt_054_001.CashAccount16;
import iso.std.iso._20022.tech.xsd.camt_054_001.CashAccount20;
import iso.std.iso._20022.tech.xsd.camt_054_001.CreditDebitCode;
import iso.std.iso._20022.tech.xsd.camt_054_001.DateAndDateTimeChoice;
import iso.std.iso._20022.tech.xsd.camt_054_001.DocumentCAMT54;
import iso.std.iso._20022.tech.xsd.camt_054_001.EntryDetails1;
import iso.std.iso._20022.tech.xsd.camt_054_001.EntryTransaction2;
import iso.std.iso._20022.tech.xsd.camt_054_001.GenericAccountIdentification1;
import iso.std.iso._20022.tech.xsd.camt_054_001.PartyIdentification32;
import iso.std.iso._20022.tech.xsd.camt_054_001.ProprietaryReference1;
import iso.std.iso._20022.tech.xsd.camt_054_001.ReportEntry2;
import iso.std.iso._20022.tech.xsd.camt_054_001.TransactionParty2;
import iso.std.iso._20022.tech.xsd.camt_054_001.TransactionReferences2;

public class CAMT54PaymentFactory {
	
	private Iso20022PaymentFactory				paymentFactory;
	private List<PaymentExtendedRecord> result = new ArrayList<PaymentExtendedRecord>();
	boolean receivablesOnly = false;
	private SortedMap<String, MLBSettings> lbSettings;
	private MLBSettings msgPrefix;
	private MBankAccount ba = null;
	private String	msgPrefixStr;
	private String	trxName;
	private DocumentCAMT54 document = new DocumentCAMT54();
	private CreditDebitCode crDbCode;
	private DateAndDateTimeChoice dte;
	
	private File f;
	
	public CAMT54PaymentFactory(Iso20022PaymentFactory pf) {
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
		Charset cs = Charset.forName("UTF-8");
		InputStreamReader reader = new InputStreamReader(
				new FileInputStream(f), cs);

		try {
			JAXBContext contextObj = JAXBContext.newInstance(DocumentCAMT54.class);
			Unmarshaller marshallerObj = contextObj.createUnmarshaller();
			document = (DocumentCAMT54) marshallerObj.unmarshal(reader);
			reader.close();
		} catch (javax.xml.bind.UnmarshalException eu) {
			paymentFactory.getLogger().info(eu.getMessage());
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


		// Get BankToCustomerStatement (can be many accounts)
		BankToCustomerDebitCreditNotificationV02 cct = document.getBkToCstmrDbtCdtNtfctn();

		// Get statement entries (one per bank account to be reconciled)
		List<AccountNotification2> statements = cct.getNtfctn();

		for (AccountNotification2 s : statements) {

			// Get bank account
			CashAccount20 acct = s.getAcct();
			ba = lookupBankAccount(acct);
			paymentFactory.setBankAccount(ba);

			// Entry count
			int entryCount = 0;
			
			// Get a list of entries
			List<ReportEntry2> entries = s.getNtry();
			for (ReportEntry2 e : entries) {
				entryCount++;
				try {
					processReportEntry(e);
				} catch (Exception ee) {
					Log.warn("Can't process entry " + entryCount + " : " + ee.getMessage());
					ee.printStackTrace();
				}
				
			}
		}

		return result;
	}

	private void processReportEntry(ReportEntry2 e) {

		
		// Get indicator (debit = outgoing payments) / credit = incoming
		// payments
		crDbCode = e.getCdtDbtInd();

		// Get book date
		dte = e.getBookgDt();

		// Get entry details
		List<EntryDetails1> lst = e.getNtryDtls();

		// Process outgoing payments

		for (EntryDetails1 det : lst) {

			List<EntryTransaction2> entList = det.getTxDtls();

			for (EntryTransaction2 ee : entList) {
				
				processEntryTransaction(ee);

			}

		} // End of processing outgoing payments
		
	}
	
	
	private void processEntryTransaction(EntryTransaction2 ee) {
		
		String ourRef;

		// This record is used to comply with previous jasper reports.
		LbPayment lbPayment = new LbPayment();
		
		PaymentExtendedRecord rec = new PaymentExtendedRecord();
		rec.setBankAccountPtr(ba);
		
		TransactionReferences2 tr2 = ee.getRefs();

		// Get our invoice no
		ourRef = tr2.getEndToEndId();
		ProprietaryReference1 theirRef1 = tr2.getPrtry();
		if (theirRef1!=null) {
			String theirRef = theirRef1.getRef();
			rec.setBpInvoiceNo(theirRef);
		}

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
			Log.warn("Can't match invoice " + ourRef);
		}

		AmountAndCurrencyExchange3 aace = ee.getAmtDtls();
		AmountAndCurrencyExchangeDetails3 aaced = aace
				.getTxAmt();
		ActiveOrHistoricCurrencyAndAmount amount = aaced
				.getAmt();

		rec.setInvoiceNo(ourRef);
		rec.setDescription(ourRef);
		rec.setCurrency(amount.getCcy());
		rec.setOrderSum(amount.getValue().doubleValue());
		rec.setTransaction(lbPayment);
		lbPayment.setAmount(rec.getOrderSum());
		rec.setTrxDate(dte.getDt().toGregorianCalendar()
				.getTime());
		// The last trx date will be trx date for the whole
		// file.
		paymentFactory.setTrxDate(rec.getTrxDate());

		TransactionParty2 trp = ee.getRltdPties();
		PartyIdentification32 pid = trp.getCdtr();
		if (pid!=null) {
			rec.setName(pid.getNm());
			lbPayment.setDstName(pid.getNm());
		}

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
		}
		
		// Create payment
		if (!arCreditMemo && !CreditDebitCode.CRDT.equals(crDbCode)) {
			try {
				MPayment pmt = paymentFactory.createPayment(ba, invoice,
						rec.getTrxDate(), false, rec.getOrderSum(),
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
