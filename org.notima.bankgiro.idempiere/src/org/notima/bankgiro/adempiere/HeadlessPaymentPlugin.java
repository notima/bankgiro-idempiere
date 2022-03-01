package org.notima.bankgiro.adempiere;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;

import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.compiere.model.MBankAccount;
import org.compiere.model.Query;
import org.compiere.model.X_C_BankAccountDoc;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.notima.bankgiro.adempiere.model.MLBFile;
import org.notima.bankgiro.adempiere.model.MLBPlugin;
import org.notima.bankgiro.adempiere.model.MLBSettings;

/**
 * Headless payment plugin = No graphical user interface. Used to read payments
 * without direct user interaction.
 * 
 * @author daniel.tamm
 * 
 */
public abstract class HeadlessPaymentPlugin {

	public final static String TYPE_PAYABLE = "PAYABLE";
	public final static String TYPE_RECEIVABLE = "RECEIVABLE";
	public final static String TYPE_BOTH = "BOTH";

	protected SortedMap<String, MLBSettings> m_lbSettings; // Store LB-settings
															// for user
	protected CLogger m_log;
	protected Properties m_ctx;
	protected MLBPlugin m_plugin;
	protected PaymentFactory m_paymentFactory;
	protected String		m_archiveDir;
	protected String		m_incomingDir;
	protected String		m_reportSubDir = "";
	protected String		m_reportDir;
	protected List<String>	warnMessages;
	

	public final static String COL_REPORTSUBDIRECTORY = "ReportSubDirectory";
	public final static String COL_ARCHIVE_DIRECTORY = "Archive_Directory";
	public final static String COL_FILE_DIRECTORY = "File_Directory";
	public final static String DISABLE_DRY_RUN_CHECK = "Disable_Dry_Run_Check";

	public HeadlessPaymentPlugin(Properties ctx) throws Exception {
		m_lbSettings = PluginRegistry.registry.getLbSettings();
		m_log = CLogger.getCLogger(this.getClass());
		m_ctx = ctx;
		// Read plugin information
		m_plugin = MLBPlugin.getPluginByClass(m_ctx, this.getClass(), null);
		if (isHeadless()) {
			// Make sure there's a payment report template
			InputStream is = getPaymentReportTemplate();
			if (is==null) 
				throw new Exception("No payment report template defined for this plugin");
			m_paymentFactory = createPaymentFactory();
			if (m_paymentFactory==null)
				CLogger.get().warning("The createPaymentFactory method returned null. Please check your code.");
		}
	}

	/**
	 * The reason for this method is because GUI payment plugins extend the
	 * HeadlessPaymentPlugin.
	 * 
	 * @return	true if this implementation is headless.
	 */
	protected abstract boolean isHeadless();
	
	/**
	 * Creates the payment factory
	 */
	protected abstract PaymentFactory createPaymentFactory();
	
	/**
	 * Starting point for automatic file processing.
	 * Use this method for processing a bank account with only the same payment plugin.
	 * 
	 * Preferred method is to use the process ProcessFilesForBankAccount
	 * 
	 * @param ba
	 *            The bank account to process
	 * @see	  org.notima.bankgiro.adempiere.process.ProcessFilesForBankAccount
	 */
	public void processDocumentFilesFor(MBankAccount ba) throws Exception {

		// Look through the bank account's document types to check if this
		// plugin is represented
		List<X_C_BankAccountDoc> docs = new Query(ba.getCtx(),
				X_C_BankAccountDoc.Table_Name,
				"C_BankAccount_ID=? AND XC_Plugin_ID=?", ba.get_TrxName())
				.setParameters(new Object[] { ba.get_ID(), m_plugin.get_ID() })
				.setOnlyActiveRecords(true).list();

		if (docs == null || docs.size() == 0)
			// No plugin of this type, just return
			return;


		/**
		 * Go through each plugin. More than one plugin means more incoming
		 * directories.
		 */
		for (X_C_BankAccountDoc bad : docs) {

			processDocumentFilesFor(ba, bad);
			
		}

	}

	/**
	 * Override this if you want a specific file filter for the files in the incoming directory. 
	 * @return
	 */
	public FileFilter getFileFilter() {
		return null;
	}
	
	/**
	 * Processes for a specific bank account document.
	 * 
	 * @param bad
	 * @return	A list of files processed
	 * @throws Exception
	 */
	public List<String> processDocumentFilesFor(MBankAccount ba, X_C_BankAccountDoc bad) throws Exception {

		warnMessages = new ArrayList<String>();

		// Set context if this is run in a server process
		Env.setContext(m_ctx, "AD_Client_ID", ba.getAD_Client_ID());
		
		if (m_paymentFactory==null) {
			m_paymentFactory = createPaymentFactory();
		}
		// Set bank account on payment factory
		m_paymentFactory.setBankAccount(ba);
		
		String incomingDir;
		String subDir;
		String archiveDir;
		List<String> result = new ArrayList<String>();
		boolean processed = false;

		// Read files from the incoming directory
		incomingDir = bad.get_ValueAsString(COL_FILE_DIRECTORY);
		archiveDir = bad.get_ValueAsString(COL_ARCHIVE_DIRECTORY);
		subDir = bad.get_ValueAsString(COL_REPORTSUBDIRECTORY);
		// Check for overrides
		if (m_incomingDir!=null)
			incomingDir = m_incomingDir;
		if (m_archiveDir!=null)
			archiveDir = m_archiveDir;
		if (m_reportSubDir!=null && m_reportSubDir.trim().length()>0)
			subDir=m_reportSubDir;
		
		if (incomingDir == null || incomingDir.trim().length() == 0
				|| archiveDir == null || archiveDir.trim().length() == 0
				|| subDir == null
				|| subDir.trim().length() == 0) {
			throw new Exception(
					"Not all directories are defined on the bank document with id "
							+ bad.get_ID() + " and bank account " + ba.getName());
		}

		// List all files in incoming directory (non recursive)
		File inDir = new File(incomingDir);
		// Check if directory exists
		if (!inDir.exists()) {
			throw new Exception("In directory " + incomingDir + " is not found.");
		}
		// Make sure report directory exists before proceeding
		MLBSettings pdfReportDir = m_lbSettings
				.get(MLBSettings.PDF_REPORT_PATH);
		// Check if sub directory exists
		if (pdfReportDir != null) {
			String path = pdfReportDir.getName();
			File outPdfDir = new File(path);
			if (!outPdfDir.exists())
				throw new Exception("PDF out directory " + path + " doesn't exist.");
		} else {
			throw new Exception("No out directory for PDF-reports is defined.");
		}
		
		String[] files = inDir.list();
		File f;
		FileFilter ff = getFileFilter();
		for (int i = 0; i < files.length; i++) {
			f = new File(inDir.getAbsolutePath() + File.separator
					+ files[i]);
			if (!f.isDirectory() && (ff==null || ff.accept(f))) {
				if (!alreadyProcessed(f)) {
					processed = processFile(f);
					if (processed) {
						// Save the report
						createPaymentReport(subDir, files[i] + ".pdf");
						if (m_paymentFactory.m_notProcessedPayments!=null && m_paymentFactory.m_notProcessedPayments.size()>0) {
							warnMessages.add(files[i] + " resulted in " + m_paymentFactory.m_notProcessedPayments.size() + " non processed payments.");
						}
						if (!m_paymentFactory.isDryRun()) {
							// Move the file to archive directory
							f.renameTo(new File(archiveDir + File.separator
									+ files[i]));
							result.add(files[i]);
							// Save the file as processed
							saveAsProcessed(f, null);
						} else {
							String msg = files[i] + " was set to dry run."; 
							result.add(msg);
							warnMessages.add(msg);
							// reset dry run flag for next run
							m_paymentFactory.setDryRun(false);
						}
					}
				}
			}
		}
		
		return result;
		
	}
	
	/**
	 * Return the file dir for the given bank account
	 * 
	 * @param ba
	 * @return
	 */
	public String getIncomingDir(MBankAccount ba) {
    	X_C_BankAccountDoc bdoc = 
    			new Query(m_ctx, X_C_BankAccountDoc.Table_Name, "C_BankAccount_ID=? AND XC_LBPlugin_ID=?", null)
    					.setParameters(new Object[] {ba.getC_BankAccount_ID(), m_plugin.getXC_LBPlugin_ID()})
    					.first();
    	if (bdoc!=null) {
    		return (String)bdoc.get_Value(COL_FILE_DIRECTORY);
    	} else {
    		return null;
    	}
	}
	
	/**
	 * Return the file dir for the given bank account
	 * 
	 * @param ba
	 * @return
	 */
	public String getArchiveDir(MBankAccount ba) {
    	X_C_BankAccountDoc bdoc = 
    			new Query(m_ctx, X_C_BankAccountDoc.Table_Name, "C_BankAccount_ID=? AND XC_LBPlugin_ID=?", null)
    					.setParameters(new Object[] {ba.getC_BankAccount_ID(), m_plugin.getXC_LBPlugin_ID()})
    					.first();
    	if (bdoc!=null) {
    		return (String)bdoc.get_Value(COL_ARCHIVE_DIRECTORY);
    	} else {
    		return null;
    	}
	}
	
	/**
	 * Return the file dir for the given bank account
	 * 
	 * @param ba
	 * @return
	 */
	public String getReportSubDir(MBankAccount ba) {
    	X_C_BankAccountDoc bdoc = 
    			new Query(m_ctx, X_C_BankAccountDoc.Table_Name, "C_BankAccount_ID=? AND XC_LBPlugin_ID=?", null)
    					.setParameters(new Object[] {ba.getC_BankAccount_ID(), m_plugin.getXC_LBPlugin_ID()})
    					.first();
    	if (bdoc!=null) {
    		return (String)bdoc.get_Value(COL_REPORTSUBDIRECTORY);
    	} else {
    		return null;
    	}
	}
	
	public void setReportDir(String dir) {
		m_reportDir = dir;
	}
	
	public void setIncomingDir(String dir) {
		m_incomingDir = dir;
	}
	
	/**
	 * Sets the archive dir. If set, it overrides the dir set on bank account level.
	 * @param dirPath
	 */
	public void setArchiveDir(String dirPath) {
		m_archiveDir = dirPath;
	}

	/**
	 * @return	The value of archive dir (which overrides on bank account level).
	 */
	public String getArchiveDir() {
		return m_archiveDir;
	}
	
	/**
	 * Sets the sub directory for reports
	 * Should be only one word, not the full path.
	 * The supplied string is appended to the report path.
	 */
	public void setReportSubDir(String subdir) {
		m_reportSubDir = subdir;
	}
	
	/**
	 * Processes a single file
	 * 
	 * @param f
	 * @return
	 */
	public boolean processFile(File f) {

		m_paymentFactory.setFile(f);
		m_paymentFactory.setFileName(f.getName());
		m_paymentFactory.m_payments = null;
		m_paymentFactory.run();
		if (m_paymentFactory.m_payments != null)
			return true;
		else
			return false;

	}

	/**
	 * Checks if the file has already been processed.
	 * 
	 * @param f
	 * @return
	 */
	protected boolean alreadyProcessed(File f) {

		// Check if this date has been read before (only check name)
		MLBFile matchingFile = MLBFile.findByName(m_ctx, m_plugin.get_ID(),
				f.getName(), null);
		if (matchingFile != null) {
			return true;
		}
		return false;

	}
	
	/**
	 * Marks the file as processed by saving it in the repository
	 */
	protected void saveAsProcessed(File f, String trxName) {
    	// Save file info
    	MLBFile fileInfo = new MLBFile(Env.getCtx(), 0, trxName);
    	fileInfo.setFileName(f.getName());
    	fileInfo.setfilepath(f.getParent());
    	fileInfo.setXC_LBPlugin_ID(m_plugin.getXC_LBPlugin_ID());
    	fileInfo.saveEx(trxName);
	}

	/**
	 * Returns the payment report template NOTE!! This should be overridden.
	 * 
	 * @return
	 */
	public abstract InputStream getPaymentReportTemplate();

	/**
	 * Shows a report with payments
	 * 
	 * @param subReportDir
	 *            Subreport dir to save the reports
	 * @param targetFileName
	 *            The target file name to save
	 *            
	 * @return Full path of created report.
	 */
	protected String createPaymentReport(String subReportDir, String targetFileName) {

		Collection<PaymentExtendedRecord> payments = m_paymentFactory.getPayments();

		// Make sure that non processed payments are last
		List<PaymentExtendedRecord> paymentList = new ArrayList<PaymentExtendedRecord>();
		for (PaymentExtendedRecord p : payments) {
			if (!p.isProcessed()) {
				paymentList.add(p);
			} else {
				paymentList.add(0, p);
			}
		}

		String recipientBankAccount = m_paymentFactory.getBankAccount().getName();

		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("ReportTitle", "Payment report");
		parameters.put("recipientBankAccount", recipientBankAccount);
		parameters.put("fileDate", m_paymentFactory.getTrxDate());
		parameters.put("fileName", m_paymentFactory.getFileName());

		try {

			InputStream is = null;
			// Use default
			is = getPaymentReportTemplate();

			JasperPrint print = JasperFillManager.fillReport(is, parameters,
					new JRBeanCollectionDataSource(paymentList));
			
			MLBSettings pdfReportDir = m_lbSettings
					.get(MLBSettings.PDF_REPORT_PATH);
			// Check if sub directory exists
			String path = "";
			if (pdfReportDir != null) {
				path = pdfReportDir.getName();
			}
			File reportDir = new File(path + File.separator + subReportDir);
			if (!reportDir.exists()) {
				reportDir.mkdirs();
			}
			String destFileName = reportDir + File.separator
					+ targetFileName + ".pdf";

			net.sf.jasperreports.engine.JasperExportManager
					.exportReportToPdfFile(print, destFileName);

			return destFileName;

		} catch (Exception jre) {
			jre.printStackTrace();
		}

		return null;
	}

	public List<String> getWarnMessages() {
		return warnMessages;
	}

	public void setWarnMessages(List<String> warnMessages) {
		this.warnMessages = warnMessages;
	}

	
	
}
