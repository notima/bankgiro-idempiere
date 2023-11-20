package org.notima.bankgiro.adempiere;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;

import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MBankAccount;
import org.compiere.model.MConversionRate;
import org.compiere.model.MCurrency;
import org.compiere.model.MInvoice;
import org.compiere.model.MLocation;
import org.compiere.model.MOrg;
import org.compiere.model.MOrgInfo;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.notima.bankgiro.adempiere.model.MLBSettings;
import org.notima.bg.BgUtil;
import org.notima.bg.LbFile;
import org.notima.bg.lb.LbPayment;
import org.notima.bg.lb.LbSet;
import org.notima.bg.lb.LbTk3Record;
import org.notima.bg.lb.LbUtlSet;

/**
 * Creates LB file
 * 
 * @author daniel.tamm
 *
 */
public class LbFileFactory implements PaymentFileFactory {

	public static String KEY_LB_FILE = "KEY_LB_FILE";
	
	private MBankAccount bankAccount;
	private List<LbPaymentRow> payments;
	private SortedMap<String, MLBSettings> m_lbSettings;
	private File outDir;
	private String 	lbEmptyRef;
	
	@Override
	public File createPaymentFile(MBankAccount srcAccount,
			List<LbPaymentRow> suggestedPayments, File outDir, String trxName) {
		bankAccount = srcAccount;
		payments = suggestedPayments;
		this.outDir = outDir;
		m_lbSettings = PluginRegistry.registry.getLbSettings();
		MLBSettings tmpSet = m_lbSettings.get(MLBSettings.LB_EMTPY_REF);
		if (tmpSet!=null) {
			lbEmptyRef = tmpSet.getName();
		}
		return createPaymentFile();
	}
	
    /**
     * Creates payment file to send to Bankgirot.
     * 
     * @return	True if creation succeeded
     */
    private File createPaymentFile() {
    	
        MBankAccount senderBankAccount = bankAccount;
        String srcBankAccountCurrency = senderBankAccount.getC_Currency().getISO_Code();
        // Set sender accounts
        // If the bank account is marked as isBankgiro the sending bankgiro is the same as accountNO.
        // If the bank account is not marked as bankgiro the sending bankgiro is the AD_Bankgiro field.
        boolean isBankgiro = (Boolean)senderBankAccount.get_Value("IsBankgiro");
        String bankAccountNo = senderBankAccount.getAccountNo();
        String AD_Bankgiro = isBankgiro ? bankAccountNo : senderBankAccount.get_ValueAsString("AD_Bankgiro");
        bankAccountNo = BgUtil.toDigitsOnly(bankAccountNo);
		// Clean up number
		AD_Bankgiro = BgUtil.toDigitsOnly(AD_Bankgiro);
		boolean validBankgiro = BgUtil.validateBankgiro(AD_Bankgiro);
		if (!validBankgiro)
		{
			MessageCenter.error(null, Msg.getMsg(Env.getCtx(),"BankgiroProblem"), Msg.getMsg(Env.getCtx(),"BGClientNotValid"));
			return null;
		}

        // Create header in file
        LbFile m_bgFile = new LbFile();
        
        // Create sets for SEK and EUR (only possible currencies when not foreign payments)
        LbSet m_lbSEKSet = LbSet.createPayableSet(AD_Bankgiro);
        LbSet m_lbEURSet = LbSet.createPayableSet(AD_Bankgiro);
        m_lbEURSet.setCurrency("EUR");
        
        // Create set for foreign payments
        // ===========================================
        Properties ctx = Env.getCtx();
        int orgId = senderBankAccount.getAD_Org_ID();
        if (orgId==0) {
        	orgId = Env.getAD_Org_ID(ctx);
        }
        MOrg org = MOrg.get(ctx, orgId);
        MOrgInfo orgInfo = MOrgInfo.get(Env.getCtx(), orgId);
        MLocation loc = new MLocation(Env.getCtx(), orgInfo.getC_Location_ID(), null);
        // Get format for UTL file
        MLBSettings utlBankFileFmt = m_lbSettings.get(MLBSettings.LB_UTL_FMT);
        String n = utlBankFileFmt!=null ? utlBankFileFmt.getName() : "1";
        int utlBankFmtInt = LbUtlSet.BANK_HANDELSBANKEN; // Default to Handelsbanken
        try {
        	utlBankFmtInt = Integer.parseInt(n);
        } catch (NumberFormatException e) {
        }
        
        LbUtlSet m_lbUtlSet = LbUtlSet.createPayableSet(AD_Bankgiro, org.getName(), loc.toString(), utlBankFmtInt);
        // ============================================
        // == End of create set for foreign payments
        
        Timestamp dueDate;

        // Iterate through all rows in the table, skip the ones with wrong currency
        List<LbPaymentRow> checkedRows = payments;
        
        LbPaymentRow row;
        LbPayment lbPayment;
        String currency;
		for (Iterator<LbPaymentRow> it = checkedRows.iterator(); it.hasNext();)
		{
			row = it.next();
            // Read the selected invoice
            MInvoice invoice = row.getInvoice();
            KeyNamePair bPartnerKey = row.getBpartner().getKeyNamePair();
            dueDate = row.payDate;
            BigDecimal PayAmt = new BigDecimal(row.payAmount);
            lbPayment = createLbPayment(invoice, dueDate, bPartnerKey.toString(), PayAmt, bankAccountNo); 
            currency = invoice.getCurrencyISO();
            if (lbPayment!=null) {
            	// Domestic payment
            	if (!lbPayment.isForeign()) {
	    			if ("SEK".equalsIgnoreCase(currency)) {
	    				m_lbSEKSet.addTransaction(lbPayment);
	    			} else if ("EUR".equalsIgnoreCase(currency)) {
	    				m_lbEURSet.addTransaction(lbPayment);
	    			} else {
	    				// Skipping this invoice since it's wrong currency
	    				// TODO: Some kind of feedback to user.
	    				continue;
	    			}
            	} else {
            		// International payment
            		// Make sanity checks.
            		if (isBankgiro) {
            			MessageCenter.error(null, "File not created", "International payments must be sent from the bankaccount, not from a pure bankgiro account. Please select appropriate source account");
            			return null;
            		}
            		if ("EUR".equalsIgnoreCase(currency) && !"EUR".equalsIgnoreCase(srcBankAccountCurrency)) {
            			boolean answer = MessageCenter.confirm(
	            					"Payment to " + bPartnerKey.getName() + " of " + lbPayment.getForeignAmount() + " EUR will be sent from an account in " + srcBankAccountCurrency + 
	            					"\nDo you want to proceed?"
            					, false);
            			if (!answer) {
            				return null;
            			}
            		}
            		m_lbUtlSet.addTransaction(lbPayment);
            	}
            } else {
            	MessageCenter.error(null, "File not created", "One or more errors were encountered. File not created");
            	return null;
            }

    	}   //  for all rows in table

		if (m_lbSEKSet.getRecords().size()>0) {
			m_bgFile.addSet(m_lbSEKSet);
		}
		if (m_lbEURSet.getRecords().size()>0) {
			m_bgFile.addSet(m_lbEURSet);
		}
		if (m_lbUtlSet.getRecords().size()>0) {
			m_bgFile.addSet(m_lbUtlSet);
		}
		
        // Write file
        String outputDir = outDir.getAbsolutePath();

        if (!outputDir.endsWith(File.separator)) outputDir += File.separator;
        File bgcData = new File(outputDir + MLBSettings.getFileName(m_lbSettings));
        if (bgcData.exists()) {
            bgcData.delete();
        }
        try {
            Charset cs = Charset.forName("Cp850");
            m_bgFile.writeToFile(bgcData, cs);
            
        } catch (Exception ioe) {
        	MessageCenter.error(null, "Unexpected error", ioe.getMessage());
            ioe.printStackTrace();
            return(null);
        }

		return bgcData;

    }

	/**
	 *  Creates LB Payment
	 *  Creates LB payment-record from in parameters.
	 *  @param	invoice
	 *  @param	dueDate
	 *  @param	bPartner
	 *  @param	payAmount				If payAmount is negative it represents a credit note (deduct from outgoing payments)
	 *  @param	senderBankAccountNo		The sender bankaccount number.
	 *  @return Payment if success. Null at no success.
	 */
	public LbPayment createLbPayment(MInvoice invoice, Timestamp dueDate, String bPartner, BigDecimal payAmount, String senderBankAccountNo)
	{

        // Set information from the invoice
        int C_BPartner_ID = invoice.getC_BPartner_ID();
        boolean isOCR = invoice.get_Value("isOCR")!=null && "Y".equalsIgnoreCase(invoice.get_ValueAsString("isOCR"));
        String OCR = (String)invoice.get_Value("OCR"); 				    // OCR reference
        String BPInvoiceNo = (String)invoice.get_Value("BPDocumentNo");		// Invoice number if not OCR
        String documentNoInvoice = invoice.getDocumentNo();
    	MBPartner bp = new MBPartner(Env.getCtx(), C_BPartner_ID, null);

        MBPBankAccount receiverBankAccount = null;
        
        try {
        	receiverBankAccount = LbPaymentRow.getDestinationAccount(C_BPartner_ID);        
        } catch (Exception eee) {        
        	MessageCenter.error(bp.getName() + ": " + eee.getMessage());
        	return(null);
        }
        
	    String BP_Bankgiro = receiverBankAccount.get_ValueAsString("BP_Bankgiro");
        String BP_Plusgiro = receiverBankAccount.get_ValueAsString("BP_Plusgiro");
        String clearing = receiverBankAccount.getRoutingNo();
        String accountNo = receiverBankAccount.getAccountNo();
        String swift = receiverBankAccount.get_ValueAsString("swiftcode");
        String iban = receiverBankAccount.get_ValueAsString("iban");
        String costDist = receiverBankAccount.get_ValueAsString("UTL_CostDist");
        String trxType = receiverBankAccount.get_ValueAsString("UTL_TrxType");
        // String bankCode = receiverBankAccount.get_ValueAsString("UTL_BankCode");

		// Clean up number
		boolean validBg = BgUtil.validateBankgiro(BP_Bankgiro);
        boolean validPg = BgUtil.validateBankgiro(BP_Plusgiro);
        boolean validBankAccount = BgUtil.validateBankAccount(clearing, accountNo);
        boolean validIban = BgUtil.validateIban(swift, iban);
        
		if (isOCR && (validBg || validPg))
		{
			// Clean up number
			OCR = BgUtil.toDigitsOnly(OCR);
			if (!BgUtil.isValidOCRNumber(OCR))
			{
				MessageCenter.error(null, Msg.getMsg(Env.getCtx(),"OCRNotValid"), Msg.getMsg(Env.getCtx(),"OCRProblem"));
				return null;
			}
		} else {
			// Not OCR
			if (OCR==null || OCR.trim().length()==0) {
				OCR = BPInvoiceNo; // Use BPInvoiceNo if no OCR is given.
				// TODO: If no reference is given, fallback to something the recipient will understand.
				if (OCR==null || OCR.trim().length()==0) {
					OCR = documentNoInvoice;
				}
			}
		}
		
		if (!validBg && !validPg && !validIban &&!validBankAccount)
		{
			MessageCenter.error(null, Msg.getMsg(Env.getCtx(),"BGBpartnerNotValid") + " " + bPartner + " " + Msg.getMsg(Env.getCtx(),"NotValid"), Msg.getMsg(Env.getCtx(),"BankgiroProblem"));
			return null;
		}
		else
		{
            LbPayment lbPayment;
            if (validBg) {
                lbPayment = LbPayment.createBgPayment(BP_Bankgiro, OCR, payAmount.doubleValue(), documentNoInvoice, dueDate);
            } else if (validPg) {
            	
            	if (payAmount.signum()<0) {
            		MessageCenter.error("Credit invoices to Plusgirot not supported in Bankgirot's file format. Doc No: " + documentNoInvoice);
            		return null;
            	}
                lbPayment = LbPayment.createPgPayment(BP_Plusgiro, OCR, payAmount.doubleValue(), documentNoInvoice, dueDate, null);
                
            } else if (validBankAccount) {
            	lbPayment = LbPayment.createBankPayment(BgUtil.getAccountString(clearing, accountNo), payAmount.doubleValue(), OCR, documentNoInvoice, dueDate);
            } else if (validIban) {
            	int recipientNo = bp.get_ID(); // Use Adempiere's internal ID
            	String name = bp.getName();
            	MLocation loc = new MLocation(Env.getCtx(), invoice.getC_BPartner_Location().getC_Location_ID(), null);
            	String address = loc.getAddress1() + ((loc.getAddress2()!=null && loc.getAddress2().trim().length()>0) ? (" " + loc.getAddress2()) : "");
            	String postal = loc.getPostal() + " " + loc.getCity();
            	String countryCode = loc.getC_Country().getCountryCode();
            	String currency = invoice.getCurrencyISO();
            	int bankCode = 101; // TODO: Must be supplied as a parameter
            	double amountSEK = 0;
            	if (!"SEK".equalsIgnoreCase(currency)) {
	            	MCurrency sekCurrency = MCurrency.get(invoice.getCtx(), "SEK");
	            	MCurrency otherCurrency = MCurrency.get(invoice.getCtx(), currency);
	            	
	            	try {
	            	 amountSEK = MConversionRate.convert(
	            			Env.getCtx(), payAmount, 
	            			otherCurrency.get_ID(), sekCurrency.get_ID(), 
	            			invoice.getAD_Client_ID(), invoice.getAD_Org_ID()).doubleValue();
	            	} catch (Exception e) {
	            		amountSEK = 0;
	            	}
	            	if (amountSEK==0) {
	            		CLogger.getCLogger(this.getClass()).fine("No conversion available for " + otherCurrency.getISO_Code() + " -> " + sekCurrency.getISO_Code() + " Invoice: " + invoice.getDocumentNo());
	            	}
	            	 
            	} else {
            		amountSEK = payAmount.doubleValue();
            	}
            	
            	if (costDist==null || costDist.trim().length()==0) {
            		// Default to both paying their costs
            		costDist = LbTk3Record.COST_BEN;
            	}
            	if (trxType==null || trxType.trim().length()==0) {
            		// Default to normal
            		trxType = LbTk3Record.TRX_NORMAL;
            	}
            	// Check for due date if invoice is a credit note
            	// Make sure it's 365 days ahead unless the date is manually set to
            	// be more than 60 days ahead
            	if (payAmount.signum()<0) {
            		int dueFromNow = BgUtil.daysFromNow(dueDate);
            		if (dueFromNow<60) {
            			dueDate = new java.sql.Timestamp(BgUtil.addDays(dueDate, 365-dueFromNow).getTime());
            		}
            	}
            		
            	lbPayment = LbPayment.createUtlPayment(recipientNo, swift, iban, name, address, postal, countryCode, 
            			OCR, amountSEK, payAmount.doubleValue(), currency, documentNoInvoice, dueDate, bankCode, // TODO: Use parameters from business partner
            			costDist, trxType, 
            			senderBankAccountNo);
            	
            } else {
                MessageCenter.error("There's no valid BG or PG on this vendor for invoice " + documentNoInvoice);
                return(null);
            }
			// lbSet.addTransaction(lbPayment);
            return(lbPayment);
		}
	}

	@Override
	public String getKey() {
		return KEY_LB_FILE;
	}

	@Override
	public String getName() {
		return "LB File";
	}

	/**
	 * Basic payment validator (check that there is a destination account)
	 */
	@Override
	public PaymentValidator getPaymentValidator() {
		return new BasicPaymentValidator();
	}
    
    

}
