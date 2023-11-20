package org.notima.bankgiro.adempiere.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.Query;
import org.compiere.util.Env;

/**
 * Object used to store information about files read / constructed by bank payment plugins.
 * 
 * @author Daniel Tamm
 *
 */
public class MLBFile extends X_XC_LBFile {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4250668592207546143L;

	public MLBFile(Properties ctx, int XC_LBFile_ID, String trxName) {
		super(ctx, XC_LBFile_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	public MLBFile(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Return file matching the name parameter.
	 * 
	 * @param ctx
	 * @param name
	 * @param pluginId
	 * @param trxName
	 * @return
	 */
	public static MLBFile findByName(Properties ctx, int pluginId, String name, String trxName) {
		
		X_XC_LBFile file = new Query(ctx, MLBFile.Table_Name, "XC_LBPlugin_ID=? AND FileName=? AND AD_Client_ID=?", trxName)
							.setParameters(new Object[]{pluginId, name, Env.getAD_Client_ID(ctx)})
							.first();
		if (file!=null) {
			MLBFile f = new MLBFile(ctx, file.get_ID(), trxName);
			return(f);
		} else {
			return null;
		}
		
	}
	
}
