package org.notima.idempiere.iso20022;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;

import javax.xml.bind.JAXB;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MBankAccount;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrg;
import org.compiere.util.Env;
import org.notima.bankgiro.adempiere.BankAccountUtil;
import org.notima.bankgiro.adempiere.LbPaymentRow;
import org.notima.bankgiro.adempiere.MessageCenter;
import org.notima.bankgiro.adempiere.PaymentFileFactory;
import org.notima.bankgiro.adempiere.PaymentValidator;
import org.notima.bankgiro.adempiere.PluginRegistry;
import org.notima.bankgiro.adempiere.model.MLBSettings;
import org.notima.bg.BgUtil;
import org.notima.idempiere.iso20022.entity.pain.AccountIdentification4Choice;
import org.notima.idempiere.iso20022.entity.pain.AccountSchemeName1Choice;
import org.notima.idempiere.iso20022.entity.pain.ActiveOrHistoricCurrencyAndAmount;
import org.notima.idempiere.iso20022.entity.pain.AmountType3Choice;
import org.notima.idempiere.iso20022.entity.pain.BranchAndFinancialInstitutionIdentification4;
import org.notima.idempiere.iso20022.entity.pain.CashAccount16;
import org.notima.idempiere.iso20022.entity.pain.CategoryPurpose1Choice;
import org.notima.idempiere.iso20022.entity.pain.CreditTransferTransactionInformation10;
import org.notima.idempiere.iso20022.entity.pain.CreditorReferenceInformation2;
import org.notima.idempiere.iso20022.entity.pain.CreditorReferenceType1Choice;
import org.notima.idempiere.iso20022.entity.pain.CreditorReferenceType2;
import org.notima.idempiere.iso20022.entity.pain.CustomerCreditTransferInitiationV03;
import org.notima.idempiere.iso20022.entity.pain.Document;
import org.notima.idempiere.iso20022.entity.pain.DocumentType3Code;
import org.notima.idempiere.iso20022.entity.pain.FinancialInstitutionIdentification7;
import org.notima.idempiere.iso20022.entity.pain.GenericAccountIdentification1;
import org.notima.idempiere.iso20022.entity.pain.GroupHeader32;
import org.notima.idempiere.iso20022.entity.pain.PartyIdentification32;
import org.notima.idempiere.iso20022.entity.pain.PaymentIdentification1;
import org.notima.idempiere.iso20022.entity.pain.PaymentInstructionInformation3;
import org.notima.idempiere.iso20022.entity.pain.PaymentMethod3Code;
import org.notima.idempiere.iso20022.entity.pain.PaymentTypeInformation19;
import org.notima.idempiere.iso20022.entity.pain.Priority2Code;
import org.notima.idempiere.iso20022.entity.pain.RemittanceInformation5;
import org.notima.idempiere.iso20022.entity.pain.ServiceLevel8Choice;
import org.notima.idempiere.iso20022.entity.pain.StructuredRemittanceInformation7;

/**
 * File factory that creates outbound PAIN XML files
 * 
 * Parameters used in LB_Settings
 * 
 * ISO20022_MSGPREFIX
 * ISO20022_MSGID
 * 
 * @author Daniel Tamm
 *
 */
public class Iso20022FileFactory implements PaymentFileFactory {

	public static String KEY = "ISO20022-PAIN-01";
	
	public static String ISO20022_MSGPREFIX = "ISO20022_MSGPREFIX";
	public static String ISO20022_MSGID = "IS020022_MSGID";
	
	public static DateFormat	df = new SimpleDateFormat("yyyy-MM-dd");

	private MBankAccount 		ba;
	private List<LbPaymentRow>	payments;
	private SortedMap<String, MLBSettings> lbSettings;
	
	private MLBSettings 		msgPrefix;
	private MLBSettings 		msgId;
	private int					currentFileId;
	private String				currentMsgId;
	private int					currentPaymentId;
	private static DatatypeFactory 	dataTypeFactory;
	
	static {
		try {
			dataTypeFactory = javax.xml.datatype.DatatypeFactory.newInstance();
		} catch (Exception e) {
			MessageCenter.error(e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Read message prefix and message ID from LB Settings
	 */
	public Iso20022FileFactory() {
		lbSettings = PluginRegistry.registry.getLbSettings();
		msgPrefix = lbSettings.get(ISO20022_MSGPREFIX);
		if (msgPrefix==null) {
			msgPrefix = MLBSettings.setSystemSetting(ISO20022_MSGPREFIX, "iso20022_");
		}
		
		msgId = lbSettings.get(ISO20022_MSGID);
		if (msgId==null) {
			msgId = MLBSettings.setSystemSetting(ISO20022_MSGID, "1");
		}
		
	}

	/**
	 * Used by JAXB to get correct date format in XML-file
	 * 
	 * @param d
	 * @return
	 */
	public static XMLGregorianCalendar parseISODate(String d) {
		if (d==null || d.trim().length()==0) return null;
		GregorianCalendar cal = new GregorianCalendar();
		try {
			Date date = df.parse(d);
			cal.setTime(date);
			return dataTypeFactory.newXMLGregorianCalendar(cal);
		} catch (Exception e) {
			MessageCenter.error(e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Used by JAXB to get correct date format in XML-file
	 * 
	 * @param c
	 * @return
	 */
	public static String printISODate(XMLGregorianCalendar c) {
		if (c==null) return null;
		return df.format(c.toGregorianCalendar().getTime());
	}
	
	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getName() {
		return "ISO20022 Pain.01 File Creator";
	}
	
	/**
	 * Returns message ID
	 * @return
	 */
	private String getNextMessageId(String trxName) throws Exception {

		// Refresh msg prefix
		msgPrefix.load(trxName);
		// Refresh msg id
		msgId.load(trxName);

		currentFileId = Integer.parseInt(msgId.getName());
		currentFileId++;
		
		currentMsgId = msgPrefix.getName() + currentFileId;
		
		return currentMsgId;
	}
	
	@Override
	public File createPaymentFile(MBankAccount srcAccount,
			List<LbPaymentRow> suggestedPayments, File outDir, String trxName) {
		
		ba = srcAccount;
		payments = suggestedPayments;
		Properties ctx = Env.getCtx();

		Document doc = new Document();
		CustomerCreditTransferInitiationV03 init = new CustomerCreditTransferInitiationV03();
		doc.setCstmrCdtTrfInitn(init);
		GroupHeader32 gh = new GroupHeader32();
		init.setGrpHdr(gh);
		// Message ID
		// MsgId
		try {
			gh.setMsgId(getNextMessageId(trxName));
		} catch (Exception e) {
			e.printStackTrace();
			MessageCenter.error("Can't get message ID for next file. Please check system settings");
			return null;
		}

		// Date of file
		// CreDtTm
		XMLGregorianCalendar xcal;
		xcal = dataTypeFactory.newXMLGregorianCalendar((GregorianCalendar)Calendar.getInstance());

		// Add payment info
		List<PaymentInstructionInformation3> paymentList = init.getPmtInf();
		PaymentInstructionInformation3 pmt;
		
		gh.setCreDtTm(xcal);
		
		// Number of transactions
		// NbOfTxs
		gh.setNbOfTxs(Integer.toString(payments.size()));
		
		// Initiator name
		// InitgPty / Nm
		MOrg info = MOrg.get(ctx, ba.getAD_Org_ID());
		PartyIdentification32 sender = new PartyIdentification32();
		sender.setNm(info.getName());
		gh.setInitgPty(sender);
		
		// Forward agent
		BranchAndFinancialInstitutionIdentification4 bafii = new BranchAndFinancialInstitutionIdentification4();
		FinancialInstitutionIdentification7 fii = new FinancialInstitutionIdentification7();
        String bic = ba.getBank().getSwiftCode();
        if (bic==null || bic.trim().length()==0) {
        	MessageCenter.error("Sender account must have BIC/SWIFT (defined on bank)");
        	return null;
        }
		fii.setBIC(bic.trim().toUpperCase());
		bafii.setFinInstnId(fii);
		
		gh.setFwdgAgt(bafii);
		
        String countryCodeSrcAccount = ba.getIBAN().substring(0, 2).toUpperCase();
		
		currentPaymentId = 1;
		
		try {
			for (LbPaymentRow lpr : payments) {
	
				pmt = convert(bafii, sender, countryCodeSrcAccount, lpr);
				paymentList.add(pmt);
				currentPaymentId++;
				
			}
		} catch (Exception e) {
			MessageCenter.error(e.getMessage());
			e.printStackTrace();
			return null;
		}
		
		File outFile = null;
		try {
			// Save to file

			String fileName = outDir.getCanonicalPath() + File.separator + currentMsgId;
	        MLBSettings addTime = PluginRegistry.registry.getLbSettings().get(MLBSettings.LB_ADD_TS);
	        if (addTime!=null && "Y".equalsIgnoreCase(addTime.getName())) {
	            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyMMdd.HHmm");
	            java.util.Date now = Calendar.getInstance().getTime();
	            fileName += "_" + format.format(now);
	        }
	        fileName += ".xml";
	        
			outFile = new File(fileName);
			FileOutputStream fsOut = new FileOutputStream(outFile);
			JAXB.marshal(doc, fsOut);
			fsOut.close();
		} catch (Exception e) {
			MessageCenter.error(e.getMessage());
			e.printStackTrace();
			return null;
		}

		// File has been created, persist the increased payment id
		msgId.setName(Integer.toString(currentFileId));
		msgId.saveEx(trxName);
		
		return outFile;
	}

	private PaymentInstructionInformation3 convert(
			BranchAndFinancialInstitutionIdentification4 bafii,
			PartyIdentification32 debtor, 
			String countryCodeSrcAccount,
			LbPaymentRow lpr) throws Exception {

		PaymentInstructionInformation3 pmt = new PaymentInstructionInformation3();
		pmt.setPmtInfId(Integer.toString(currentPaymentId));
		
		// Payment Method - PmtMtd - Always TRF
		pmt.setPmtMtd(PaymentMethod3Code.TRF);
		
		// Payment Tp Inf
		PaymentTypeInformation19 pmtTpInf = new PaymentTypeInformation19();
		pmtTpInf.setInstrPrty(Priority2Code.NORM); // Always Normal
		// Service level
		ServiceLevel8Choice svcLvl = new ServiceLevel8Choice();
		svcLvl.setCd("NURG"); // Non-urgent
		pmtTpInf.setSvcLvl(svcLvl);
		// Category Purpose
		CategoryPurpose1Choice ctgyPurp = new CategoryPurpose1Choice();
		ctgyPurp.setCd("SUPP"); // Always supplier payment
		pmtTpInf.setCtgyPurp(ctgyPurp);
		
		pmt.setPmtTpInf(pmtTpInf);
		
		// Dbtr - Debtor
		pmt.setDbtr(debtor);
		
		// DbtrAcct - Debtor account
		CashAccount16 ca = new CashAccount16();
		ca.setCcy(ba.getC_Currency().getISO_Code());
		ca.setId(convert(ba));
		pmt.setDbtrAcct(ca);
		
		// DbtrAgt - Debtor agent
		pmt.setDbtrAgt(bafii);
		
		XMLGregorianCalendar xcal;
		GregorianCalendar cal = (GregorianCalendar)Calendar.getInstance();
		cal.setTimeInMillis(lpr.payDate.getTime());
		xcal = dataTypeFactory.newXMLGregorianCalendar(cal);
		pmt.setReqdExctnDt(xcal);
		
		// CdtTrfTxInf
		// CreditTransferTransactionInformation10
		CreditTransferTransactionInformation10 trx;
		
		trx = new CreditTransferTransactionInformation10();
		
		// PmtId (their reference id)
		PaymentIdentification1 payId = new PaymentIdentification1();
		MInvoice invoice = lpr.getInvoice();
		
		payId.setEndToEndId(invoice.getDocumentNo());
		
		trx.setPmtId(payId);
		
		// Amt
		AmountType3Choice amt = new AmountType3Choice();
		ActiveOrHistoricCurrencyAndAmount amtCur = new ActiveOrHistoricCurrencyAndAmount();
		amtCur.setCcy(lpr.currency);
		amtCur.setValue(BigDecimal.valueOf(lpr.payAmount));
		amt.setInstdAmt(amtCur);
		trx.setAmt(amt);

		trx.setCdtr(convert(lpr.getBpartner()));
		
		MBPBankAccount dstBa = lpr.getDstAccount();
		ca = new CashAccount16();
		try {
			ca.setId(convert(countryCodeSrcAccount, dstBa));
		} catch (Exception e) {
			throw new Exception(lpr.getBpartner().getName() + " : " + e.getMessage());
		}
		ca.setCcy(lpr.currency);
		
		trx.setCdtrAcct(ca);

		BranchAndFinancialInstitutionIdentification4 bafiit = new BranchAndFinancialInstitutionIdentification4();
		FinancialInstitutionIdentification7 fii = new FinancialInstitutionIdentification7();
        String bic = dstBa.get_ValueAsString("swiftcode");
        String iban = BgUtil.removeBlanks(dstBa.get_ValueAsString("iban"));
		fii.setBIC(bic.toUpperCase());
		bafiit.setFinInstnId(fii);
		
		trx.setCdtrAgt(bafiit);
		
        boolean isOCR = invoice.get_Value("isOCR")!=null && "true".equalsIgnoreCase(invoice.get_ValueAsString("isOCR"));
        String OCR = (String)invoice.get_Value("OCR"); 				    // OCR reference
        String BPInvoiceNo = (String)invoice.get_Value("BPDocumentNo");		// Invoice number if not OCR

        RemittanceInformation5 rmt = new RemittanceInformation5();
        if (isOCR) {
        	StructuredRemittanceInformation7 ocr = new StructuredRemittanceInformation7();
        	CreditorReferenceInformation2 cri = new CreditorReferenceInformation2();
        	cri.setRef(OCR);
        	ocr.setCdtrRefInf(cri);
        	if ("NO".equals(countryCodeSrcAccount) && iban.startsWith("NO")) {
	        	CreditorReferenceType2 tp = new CreditorReferenceType2();
	        	cri.setTp(tp);
	        	CreditorReferenceType1Choice cdor = new CreditorReferenceType1Choice();
	        	tp.setCdOrPrtry(cdor);
	        	cdor.setCd(DocumentType3Code.SCOR);
        	}
        	rmt.getStrd().add(ocr);
        } else {
	        List<String> ocrList = rmt.getUstrd();
	        ocrList.add(BPInvoiceNo);
        }
        
        trx.setRmtInf(rmt);

		List<CreditTransferTransactionInformation10> trxList = pmt.getCdtTrfTxInf();
		trxList.add(trx);
        
		return pmt;
	}

	private AccountIdentification4Choice convert(MBankAccount senderBankAccount) throws Exception {

		if (senderBankAccount == null)
			throw new Exception("Sender bank account must be selected");
		
        String iban = BgUtil.removeBlanks(senderBankAccount.getIBAN());
        if (iban==null || iban.length()==0)
        	throw new Exception("Sender bank account must have IBAN");

        AccountIdentification4Choice result = new AccountIdentification4Choice();

        result.setIBAN(iban);
        
        return result;
		
	}
	

	/**
	 * Creates receiver account
	 * 
	 * @param src
	 * @param receiverBankAccount
	 * @return
	 * @throws Exception
	 */
	private AccountIdentification4Choice convert(String countryCode, MBPBankAccount receiverBankAccount) throws Exception {

		BankAccountUtil bau = BankAccountUtil.buildFromMBPBankAccount(receiverBankAccount);
		
        String iban = BgUtil.removeBlanks(bau.getIban());
        
        AccountIdentification4Choice result = new AccountIdentification4Choice();
    	GenericAccountIdentification1 other = new GenericAccountIdentification1();
    	AccountSchemeName1Choice ascheme = new AccountSchemeName1Choice();
        
        if (iban!=null && iban.trim().length()>0) {
	        if (iban.startsWith(countryCode)) {
	        	String routingNo = receiverBankAccount.getRoutingNo();
	        	String accountNo = receiverBankAccount.getAccountNo();
	        	// Norway
	        	if ("NO".equals(countryCode) && (routingNo==null || routingNo.trim().length()==0)) {
	        		// Use IBAN to create BBAN
	        		routingNo = "";
	        		accountNo = iban.substring(4);
	        	}
	        	other.setId(BgUtil.toDigitsOnly(routingNo+accountNo));
	        	ascheme.setCd("BBAN");
	        	other.setSchmeNm(ascheme);
	        	result.setOthr(other);
	        } else {
	        	result.setIBAN(iban);
	        }
	        return result;
        }
        
        if (bau.getAccountType().equals(BankAccountUtil.BankAccountType.BANKGIRO)) {
        	other.setId(BgUtil.toDigitsOnly(bau.getDstAcct()));
        	ascheme.setCd("BGNR");
        	other.setSchmeNm(ascheme);
        	result.setOthr(other);
        	return result;
        }
        
        if (bau.getAccountType().equals(BankAccountUtil.BankAccountType.PLUSGIRO)) {
        	other.setId(BgUtil.toDigitsOnly(bau.getDstAcct()));
        	ascheme.setCd("PGNR");
        	other.setSchmeNm(ascheme);
        	result.setOthr(other);
        }
        
        // TODO: Handle payment to domestic bank account
        
        return result;
		
	}
	
	/**
	 * Converts a MBPartner to XML representation
	 * 
	 * @param bp
	 * @return
	 */
	private PartyIdentification32 convert(MBPartner bp) {
		PartyIdentification32 result = new PartyIdentification32();
		result.setNm(bp.getName());
		return result;
	}

	/**
	 * Returns payment validator for PAIN payments.
	 */
	@Override
	public PaymentValidator getPaymentValidator() {
		return new PainPaymentValidator();
	}

}
