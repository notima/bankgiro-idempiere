package org.notima.bankgiro.adempiere.process;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.MBankAccount;
import org.compiere.model.Query;
import org.compiere.model.X_C_BankAccountDoc;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.compiere.util.Ini;
import org.notima.bankgiro.adempiere.HeadlessPaymentPlugin;
import org.notima.bankgiro.adempiere.model.MLBPlugin;

public class ProcessFilesForBankAccount extends SvrProcess {

	private	MBankAccount	ba;
	private int				adUserId;
	private String			incomingDir;
	private String			archiveDir;
	private DateFormat df = new SimpleDateFormat("yyMMddHHmmss");

	
	@Override
	protected void prepare() {
		
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("C_BankAccount_ID"))
				ba = new MBankAccount(getCtx(), para[i].getParameterAsInt(), this.get_TrxName());
			else if (name.equals("AD_User_ID"))
				adUserId = para[i].getParameterAsInt();
			else if (name.equals("IncomingDir"))
				incomingDir = para[i].getParameter().toString();
			else if (name.equals("ArchiveDir"))
				archiveDir = para[i].getParameter().toString();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}	
		
		if (adUserId==0 && Ini.isClient()) {
			adUserId = Env.getAD_User_ID(getCtx());
		}
		
	}

	@Override
	protected String doIt() throws Exception {
		
		int recordId = getRecord_ID();
		
		if (recordId!=0) {
			ba = new MBankAccount(getCtx(), recordId, get_TrxName());
		}
		if (ba==null) return "No bank account selected";
		
		// Set context since it can be run as a server process
        Properties ctx = getCtx();
        Env.setContext(ctx, "#AD_Client_ID", ba.getAD_Client_ID());
        Env.setContext(ctx, "#AD_Org_ID", ba.getAD_Org_ID());
        Env.setCtx(ctx);
		
		// Get bank account documents
		List<X_C_BankAccountDoc> docs = new Query(
				getCtx(), 
				X_C_BankAccountDoc.Table_Name, 
				X_C_BankAccountDoc.COLUMNNAME_C_BankAccount_ID + "=? AND IsActive='Y' AND XC_LBPlugin_ID>0 " + 
																 "AND IsManual='N'",
				get_TrxName())
			.setParameters(new Object[]{ba.get_ID()})
			.list();

		MLBPlugin plugin;
		HeadlessPaymentPlugin paymentPlugin;
		List<String> processedFiles;
		
		for (X_C_BankAccountDoc doc : docs) {
			plugin = new MLBPlugin(getCtx(), doc.get_ValueAsInt(MLBPlugin.COLUMNNAME_XC_LBPlugin_ID), get_TrxName());
			paymentPlugin = plugin.getHeadlessPlugin(getCtx(), get_TrxName());
			if (paymentPlugin==null) {
				String message = "Bank account " + ba.getName() + " doesn't have a headless plugin for " + plugin.getName();
				log.warning(message);
				addLog(message);
			} else {
				// Set overrides
				if (incomingDir!=null && incomingDir.trim().length()>0)
					paymentPlugin.setIncomingDir(incomingDir);
				if (archiveDir!=null && archiveDir.trim().length()>0)
					paymentPlugin.setArchiveDir(archiveDir);
				// Process files
				try {
					processedFiles = paymentPlugin.processDocumentFilesFor(ba, doc);
					if (processedFiles!=null && processedFiles.size()>0) {
						for (String s : processedFiles)
						addLog("Processed " + s);
					}
					if (paymentPlugin.getWarnMessages()!=null && paymentPlugin.getWarnMessages().size()>0) {
						StringBuffer buf = new StringBuffer();
						for (String s : paymentPlugin.getWarnMessages()) {
							buf.append(s + "\n");
						}
						writeMessageToArchiveDir(archiveDir!=null ? archiveDir : paymentPlugin.getArchiveDir(ba), buf.toString());
					}
				} catch (Exception e) {
					// Get a more detailed error message
					Writer swriter = new StringWriter();
					PrintWriter writer = new PrintWriter(swriter);
					e.printStackTrace(writer);
					writeMessageToArchiveDir(archiveDir!=null ? archiveDir : paymentPlugin.getArchiveDir(ba), e.getMessage() + "\n" + writer.toString());
				}
			}
		}
		
		return "";
	}
	
	private void writeMessageToArchiveDir(String archiveDir, String message) throws IOException {
		
		String fileName = df.format(Calendar.getInstance().getTime()) + ".err"; 
		File outFile = new File(archiveDir + File.separator + fileName);
		FileWriter fw = new FileWriter(outFile.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(message);
		bw.close();
		fw.close();
		
	}
	
}
