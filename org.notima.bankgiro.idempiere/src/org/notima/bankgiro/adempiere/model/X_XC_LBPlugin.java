/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2007 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package org.notima.bankgiro.adempiere.model;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for XC_LBPlugin
 *  @author Adempiere (generated) 
 *  @version Release 3.6.0LTS - $Id$ */
public class X_XC_LBPlugin extends PO implements I_XC_LBPlugin, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20130905L;

    /** Standard Constructor */
    public X_XC_LBPlugin (Properties ctx, int XC_LBPlugin_ID, String trxName)
    {
      super (ctx, XC_LBPlugin_ID, trxName);
      /** if (XC_LBPlugin_ID == 0)
        {
			setXC_LBPlugin_ID (0);
        } */
    }

    /** Load Constructor */
    public X_XC_LBPlugin (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org 
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuffer sb = new StringBuffer ("X_XC_LBPlugin[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Classname.
		@param Classname 
		Java Classname
	  */
	public void setClassname (String Classname)
	{
		set_Value (COLUMNNAME_Classname, Classname);
	}

	/** Get Classname.
		@return Java Classname
	  */
	public String getClassname () 
	{
		return (String)get_Value(COLUMNNAME_Classname);
	}

	/** Set Classname Headless Processing.
		@param Classname_Headless Classname Headless Processing	  */
	public void setClassname_Headless (String Classname_Headless)
	{
		set_Value (COLUMNNAME_Classname_Headless, Classname_Headless);
	}

	/** Get Classname Headless Processing.
		@return Classname Headless Processing	  */
	public String getClassname_Headless () 
	{
		return (String)get_Value(COLUMNNAME_Classname_Headless);
	}

	/** Set Name.
		@param Name 
		Alphanumeric identifier of the entity
	  */
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName () 
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

	/** Set XC_LBPlugin_ID.
		@param XC_LBPlugin_ID XC_LBPlugin_ID	  */
	public void setXC_LBPlugin_ID (int XC_LBPlugin_ID)
	{
		if (XC_LBPlugin_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_XC_LBPlugin_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_XC_LBPlugin_ID, Integer.valueOf(XC_LBPlugin_ID));
	}

	/** Get XC_LBPlugin_ID.
		@return XC_LBPlugin_ID	  */
	public int getXC_LBPlugin_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_XC_LBPlugin_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}