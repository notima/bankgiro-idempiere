/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.notima.bankgiro.adempiere;

import java.math.BigDecimal;

/**************************************************************************
 *  Bank Account Info - Adempiere independent class used for presentation purposes.
 */
public class BankInfo
{

    public int C_BankAccount_ID;
    public int C_Currency_ID;
    public String Name;
    public String Currency;
    public BigDecimal Balance;
    public boolean Transfers;
	
	
	public BankInfo (int newC_BankAccount_ID, int newC_Currency_ID,
        String newName, String newCurrency, BigDecimal newBalance, boolean newTransfers)
    {
        C_BankAccount_ID = newC_BankAccount_ID;
        C_Currency_ID = newC_Currency_ID;
        Name = newName;
        Currency = newCurrency;
        Balance = newBalance;
    }

    public String toString()
    {
        return Name + " : " + Currency;
    }
}   //  BankInfo
