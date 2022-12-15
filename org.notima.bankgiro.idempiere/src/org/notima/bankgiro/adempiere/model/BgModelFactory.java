package org.notima.bankgiro.adempiere.model;

import java.sql.ResultSet;

import org.adempiere.base.IModelFactory;
import org.compiere.model.PO;
import org.compiere.util.Env;

public class BgModelFactory implements IModelFactory {

	@Override
	public Class<?> getClass(String tableName) {
		if (MLBFile.Table_Name.equalsIgnoreCase(tableName))
			return MLBFile.class;
		if (MLBPlugin.Table_Name.equalsIgnoreCase(tableName))
			return MLBPlugin.class;
		if (MLBSettings.Table_Name.equalsIgnoreCase(tableName))
			return MLBSettings.class;
		return null;
	}

	@Override
	public PO getPO(String tableName, int Record_ID, String trxName) {
		if (MLBFile.Table_Name.equalsIgnoreCase(tableName)) {
			return new MLBFile(Env.getCtx(), Record_ID, trxName);
		}
		if (MLBPlugin.Table_Name.equalsIgnoreCase(tableName)) {
			return new MLBPlugin(Env.getCtx(), Record_ID, trxName);
		}
		if (MLBSettings.Table_Name.equalsIgnoreCase(tableName)) {
			return new MLBSettings(Env.getCtx(), Record_ID, trxName);
		}
		return null;
	}

	@Override
	public PO getPO(String tableName, ResultSet rs, String trxName) {
		if (MLBFile.Table_Name.equalsIgnoreCase(tableName)) {
			return new MLBFile(Env.getCtx(), rs, trxName);
		}
		if (MLBPlugin.Table_Name.equalsIgnoreCase(tableName)) {
			return new MLBPlugin(Env.getCtx(), rs, trxName);
		}
		if (MLBSettings.Table_Name.equalsIgnoreCase(tableName)) {
			return new MLBSettings(Env.getCtx(), rs, trxName);
		}
		return null;
	}

}
