package org.notima.bankgiro.adempiere.model;

import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.compiere.model.Query;
import org.compiere.util.Env;
import org.notima.bankgiro.adempiere.HeadlessPaymentPlugin;


public class MLBPlugin extends X_XC_LBPlugin {

	private static final long serialVersionUID = -4534411562071396662L;

	public MLBPlugin(Properties ctx, int XC_LBPlugin_ID, String trxName) {
		super(ctx, XC_LBPlugin_ID, trxName);
		if (XC_LBPlugin_ID==0) {
            set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(ctx));
            setAD_Org_ID(Env.getAD_Org_ID(ctx));
		}
	}

	public MLBPlugin(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	/**
	 * 
	 * @return	A list of active plugins for the current client / org.
	 */
	public static List<MLBPlugin> getActivePlugins() {
		
		List<X_XC_LBPlugin> result = new Query(Env.getCtx(), X_XC_LBPlugin.Table_Name, "", null)
					.setApplyAccessFilter(true)
					.setOnlyActiveRecords(true)
					.list();

		// Special code for backwards compability with legacy Adempiere.
		List<MLBPlugin> r = new ArrayList<MLBPlugin>();
		for (X_XC_LBPlugin p : result) {
			r.add(new MLBPlugin(p.getCtx(), p.get_ID(), p.get_TrxName()));
		}

		return(r);
		
	}

	/**
	 * Returns plugin using class name
	 * 
	 * @param ctx
	 * @param clazz
	 * @param trxName
	 * @return
	 */
	public static MLBPlugin getPluginByClass(Properties ctx, Class clazz, String trxName) {
		
		X_XC_LBPlugin p = new Query(ctx, MLBPlugin.Table_Name, "(classname=? OR classname_headless=?) AND AD_Client_ID=?", trxName)
							.setParameters(new Object[]{clazz.getName(), clazz.getName(), Env.getAD_Client_ID(ctx)})
							.first();

		// Special code for backwards compability with legacy Adempiere.
		if (p!=null) {
			MLBPlugin plugin = new MLBPlugin(p.getCtx(), p.get_ID(), trxName);
			return plugin;
		}
		
		return(null);
		
	}
	
	/**
	 * Returns a headless plugin if one is available
	 * 
	 * @param ctx
	 * @param trxName
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public HeadlessPaymentPlugin getHeadlessPlugin(Properties ctx, String trxName) throws Exception {
		
		// Get plugin
		String clazzHeadless = getClassname_Headless();
		if (clazzHeadless==null || clazzHeadless.trim().length()==0)
			return null;
		
		// Create a new instance of the class
		Constructor constructor;
		Class headlessClazz = null;
		HeadlessPaymentPlugin headlessPlugin;
		headlessClazz = Class.forName(clazzHeadless);
		constructor = headlessClazz.getConstructor(Properties.class);
		
		headlessPlugin = (HeadlessPaymentPlugin)constructor.newInstance(ctx);
		
		return headlessPlugin;
	}
	
	
}
