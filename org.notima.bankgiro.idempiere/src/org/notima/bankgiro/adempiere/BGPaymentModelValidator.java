/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.notima.bankgiro.adempiere;

import org.compiere.model.*;
import org.compiere.util.*;
import java.util.*;

/**
 *
 * @author Daniel Tamm
 */
public class BGPaymentModelValidator implements ModelValidator {

    public final static String TENDERTYPE_Bankgiro = "Z";

	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(BGPaymentModelValidator.class);
	/** Client			*/
	private int		m_AD_Client_ID = -1;
	/** User	*/
	private int		m_AD_User_ID = -1;
	/** Role	*/
	private int		m_AD_Role_ID = -1;


	/**
	 *	Constructor.
	 */
	public BGPaymentModelValidator()
	{
		super ();
	}	//	MyValidator


    @Override
    public void initialize(ModelValidationEngine engine, MClient client) {
        if (client!=null) {
            m_AD_Client_ID = client.getAD_Client_ID();
        }
        engine.addDocValidate(MPayment.Table_Name, this);
        log.fine("BGPaymentModelValidator registered");
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


    @Override
    public String modelChange(PO po, int type) throws Exception {
        return(null);
    }

    @Override
    public String docValidate(PO po, int timing) {
        // When 

        // When payment document is completing
        if (timing == TIMING_BEFORE_COMPLETE) {

           MPayment payment = (MPayment)po;
           // anba 041015
            if (payment.getC_Invoice_ID() != 0)
            {
                if (payment.getTenderType().equals(TENDERTYPE_Bankgiro)) {
//                    sendBankgiroInformation(payment);
                }
            }

        }
        return(null);
    }


}
