/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.notima.bankgiro.adempiere;

import java.util.List;

import org.compiere.model.MClient;
import org.compiere.model.MInvoice;
import org.compiere.model.MSysConfig;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.*;
import org.notima.bg.BgUtil;


/**
 *
 * @author Daniel Tamm
 */
public class BGInvoiceModelValidator implements ModelValidator {

    private final static String PAYMENTRULE_Bankgiro = "Z";

	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(BGInvoiceModelValidator.class);
	/** Client			*/
	private int		m_AD_Client_ID = -1;
	/** User	*/
	private int		m_AD_User_ID = -1;
	/** Role	*/
	private int		m_AD_Role_ID = -1;
	
	private boolean	strictDuplicateChecking = true;

    @Override
    public void initialize(ModelValidationEngine engine, MClient client) {
        if (client!=null) {
            m_AD_Client_ID = client.getAD_Client_ID();
        }
        engine.addDocValidate(MInvoice.Table_Name, this);
        engine.addModelChange(MInvoice.Table_Name, this);
        
        strictDuplicateChecking = MSysConfig.getBooleanValue("BG_STRICT_DUPLICATE_CHECKING", true, m_AD_Client_ID);
        
        log.info("BGInvoiceModelValidator registered");
    }

    @Override
    public int getAD_Client_ID() {
        return(m_AD_Client_ID);
    }

    @Override
	/**
	 *	User Login.
	 *	Called when preferences are set
	 *	@param AD_Org_ID org
	 *	@param AD_Role_ID role
	 *	@param AD_User_ID user
	 *	@return error message or null
	 */
	public String login (int AD_Org_ID, int AD_Role_ID, int AD_User_ID)
	{
		m_AD_User_ID = AD_User_ID;
		m_AD_Role_ID = AD_Role_ID;
		return null;
	}	//	login

    /**
     * Checks that OCR number sums up (no guarantee that it is correct)
     * @param po
     * @param type
     * @return
     * @throws java.lang.Exception
     */
    @Override
    public String modelChange(PO po, int type) throws Exception {
        if (type==ModelValidator.TYPE_CHANGE || type==ModelValidator.TYPE_NEW) {
            MInvoice invoice = (MInvoice)po;
            
            // Check OCR number
            
        	boolean isOcr = "true".equalsIgnoreCase(invoice.get_ValueAsString("IsOCR"));
            if (invoice.isSOTrx()) {
            	if (isOcr) {
            		// Generate OCR id
            		String ocr = BgUtil.toOCRNumber(invoice.getDocumentNo());
            		invoice.set_ValueOfColumn("OCR", ocr);
            	}
            } else {
	            boolean isOcrPayee = PAYMENTRULE_Bankgiro.equalsIgnoreCase(invoice.getPaymentRule()) &&
	                    isOcr;
	            if (isOcrPayee) {
	                String ocr = invoice.get_ValueAsString("OCR");
	                ocr = BgUtil.toDigitsOnly(ocr);
	                if (!BgUtil.isValidOCRNumber(ocr)) {
	                    return(Msg.getMsg(Env.getCtx(), "OCRNotValid") + ": " + ocr);
	                }
	            }
            }
            
            // Check for duplicates (before complete)
            if (!invoice.isComplete()) {
				String bpDocumentNo = (String)invoice.get_Value("BPDocumentNo");
				List<MInvoice> invoices = null;
				if (bpDocumentNo!=null) {
					// Check for others
					invoices = new Query(po.getCtx(), MInvoice.Table_Name, 
							"C_BPartner_ID=? AND BPDocumentNo=? AND AD_Org_ID=? AND C_DocType_ID=? AND IsSOTrx=? AND NOT DocStatus IN ('VO') AND NOT C_Invoice_ID=?", po.get_TrxName())
						.setParameters(new Object[]{
								invoice.getC_BPartner_ID(), 
								bpDocumentNo, 
								invoice.getAD_Org_ID(),
								invoice.getC_DocType_ID(),
								invoice.isSOTrx() ? "Y" : "N",
								invoice.get_ID()})
						.list();
					if (invoices.size()>0) {
						String msg = "Fakturanumret " + bpDocumentNo + " används redan på " + invoices.get(0).getDocumentNo();
						if (strictDuplicateChecking)
							return (msg);
						else
							log.warning(msg);
					}
				}
            }
            
            // Check paid flag (if allocation is removed and the invoice becomes unpaid
            // change the LB-status as well)
            if (type==ModelValidator.TYPE_CHANGE && invoice.getPaymentRule()!=null && PAYMENTRULE_Bankgiro.equalsIgnoreCase(invoice.getPaymentRule())) {
	            if (((Boolean)invoice.get_ValueOld(MInvoice.COLUMNNAME_IsPaid)).booleanValue()==true && !invoice.isPaid()) {
	            	invoice.set_ValueOfColumn("LbStatus", LbPaymentRow.PAYSTATUS_ANY); // = Not paid. The constants needs to be looked over.
	            }
            }
            
        }
        return(null);
    }

    @Override
    public String docValidate(PO po, int timing) {
        // Make sure there is an invoice number or OCR number
    	if (timing==ModelValidator.TIMING_BEFORE_COMPLETE) {
    		MInvoice invoice = (MInvoice)po;
    		// Only validate vendor invoices.
    		if (invoice.isSOTrx()) return(null);
    		if (PAYMENTRULE_Bankgiro.equalsIgnoreCase(invoice.getPaymentRule())) {
				String bpDocumentNo = invoice.get_ValueAsString("BPDocumentNo");
				if (bpDocumentNo==null || bpDocumentNo.trim().length()==0) {
					return(Msg.getMsg(Env.getCtx(), "MissingBPDocumentNo"));
				}    		
	    		boolean isOcr = "true".equalsIgnoreCase(invoice.get_ValueAsString("IsOCR"));
	    		if (isOcr) {
	    			String ocr = invoice.get_ValueAsString("OCR");
	    			if (ocr==null || ocr.trim().length()==0) {
	    				return(Msg.getMsg(Env.getCtx(), "MissingOCRRef"));
	    			}
	    		}
    		}
    	}
    	
    	return(null);
    }

}
