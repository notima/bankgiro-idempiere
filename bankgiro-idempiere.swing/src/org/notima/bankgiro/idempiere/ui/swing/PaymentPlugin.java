package org.notima.bankgiro.idempiere.ui.swing;

import java.awt.Cursor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.swing.Action;
import javax.swing.JOptionPane;

import org.compiere.apps.ADialog;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.notima.bankgiro.adempiere.HeadlessPaymentPlugin;
import org.notima.bankgiro.adempiere.PaymentExtendedRecord;
import org.notima.bankgiro.adempiere.form.I_LBSettingsPanel;
import org.notima.bankgiro.adempiere.form.VLBManagementPanel;
import org.notima.bankgiro.adempiere.model.MLBFile;
import org.notima.bankgiro.adempiere.model.MLBSettings;

/**
 * Abstract class for representing a payment plugin. The plugin is a graphical component
 * and is added in the bankgiro custom window.
 * 
 * The plugin contains the following:
 * 
 * * A settings panel for any settings this plugin provides getPanel()
 * * Actions that this panel can do getActions()
 * 
 * @author daniel.tamm
 *
 */
public abstract class PaymentPlugin extends HeadlessPaymentPlugin {
	
	private CLogger		log			= CLogger.getCLogger(PaymentPlugin.class);
	
	protected VLBManagementPanel				m_frame;
	
	/**
	 * Attaches the plugin to the panel. 
	 * @param frame		Pointer to the parent panel.
	 */
	public PaymentPlugin(Properties ctx, VLBManagementPanel frame) throws Exception {
		super(ctx);
		m_frame = frame;
		// Create payment factory
		m_paymentFactory = createPaymentFactory();
	}

	public boolean isHeadless() {
		return false;
	}
	
	/**
	 * This is the action to start this plugin
	 * @return
	 */
	public abstract Action[] getActions();
	
	/**
	 * If plugin type is both, the actions should be separated in ReceivableActions and PayableActions
	 * 
	 * @return
	 */
	public Action[] getReceivableActions() {
		return null;
	}

	/**
	 * If plugin type is both, the actions should be separated in ReceivableActions and PayableActions
	 * 
	 * @return
	 */
	public Action[] getPayableActions() {
		return null;
	}
	
	/**
	 * This is the settings panel for the plugin. If no panel is necessary
	 * null is returned.
	 * @return
	 */
	public abstract I_LBSettingsPanel getPanel();
	
	/**
	 * Method to create payments
	 * This method is currently overridden by most plugins, but ahead, try to make it more generic.
	 * 
	 * @throws Exception
	 */
	protected void runCreatePayments() throws Exception {
		
		InputStream is = null;
		// Use default
		is = getPaymentReportTemplate();
		if (is==null) {
			JOptionPane.showMessageDialog(m_frame, "No report found / report path not set. Can't display report, won't run.");
			return;
		}
		
        m_frame.getVLBParent().getFrame().setBusy(true);
        m_frame.getVLBParent().getFrame().setEnabled(false);
        m_frame.getVLBParent().getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // Set archive dir if not yet set
        if (m_archiveDir==null || m_archiveDir.trim().length()==0) {
        	m_archiveDir = this.getArchiveDir(m_paymentFactory.getBankAccount());
        }
        // Set subreport dir
        if (m_reportSubDir==null || m_reportSubDir.trim().length()==0) {
        	m_reportSubDir = getReportSubDir(m_paymentFactory.getBankAccount());
        }
        org.compiere.apps.SwingWorker worker = new org.compiere.apps.SwingWorker() {
            @Override
            public Object construct() {
            	try {
            		m_paymentFactory.run();
            	} catch (Exception e) {
            		ADialog.error(0, null, e.getMessage());
            		return Boolean.FALSE;
            	}
                return (Boolean.TRUE);
            }
            @Override
            public void finished() {

            	if (!((Boolean)get()).booleanValue()) {
            		m_frame.doneRunning();
            		return;
            	}
            	
            	
                // Save file info and move file to archive unless dryRun
            	if (!m_paymentFactory.isDryRun()) {
            		
            		if (m_paymentFactory.getFile()!=null) {
            		
	                	try {
	    	            	// Save file info
	    	            	MLBFile fileInfo = new MLBFile(Env.getCtx(), 0, null);
	    	            	fileInfo.setFileName(m_paymentFactory.getFileName());
	    	            	fileInfo.setfilepath(m_paymentFactory.getFile().getPath());
	    	            	fileInfo.setXC_LBPlugin_ID(m_plugin.getXC_LBPlugin_ID());
	    	            	fileInfo.saveEx(null);
	                	} catch (Exception ee) {
	                		ee.printStackTrace();
	                	}
            		
	                	if (m_archiveDir==null) {
	                		log.warning("No archive dir set for plugin " + getClass().getName());
	                	} else {
	                		// Check if archive dir exists
	                		File archiveDirFile = new File(m_archiveDir);
	                		if (!archiveDirFile.exists()) {
	                			// Create
	                			archiveDirFile.mkdirs();
	                		}
			                if (m_paymentFactory.getFile()!=null) {
			                    m_paymentFactory.getFile().renameTo(new File(archiveDirFile, m_paymentFactory.getFile().getName()));
			                    log.info("Moved file " + m_paymentFactory.getFile().getName() + " to directory " + archiveDirFile);
			                }
	                	}
		                
            		} else {
            			
	                	try {
	    	            	// Save file info
	    	            	MLBFile fileInfo = new MLBFile(Env.getCtx(), 0, null);
	    	            	fileInfo.setFileName(m_paymentFactory.getFileName());
	    	            	fileInfo.setfilepath("");
	    	            	fileInfo.setXC_LBPlugin_ID(m_plugin.getXC_LBPlugin_ID());
	    	            	fileInfo.saveEx(null);
	                	} catch (Exception ee) {
	                		ee.printStackTrace();
	                	}
            			
            		}
            	}
            	
            	m_frame.doneRunning();
            	
                boolean showMessage = false;
                StringBuffer doneMessage = new StringBuffer();
                if (m_paymentFactory.getFile()!=null) doneMessage.append("File " + m_paymentFactory.getFile().getName() + " successfully read.\n");
                if (m_paymentFactory.isDryRun()) {
                	doneMessage.append("This is a dry run! Test mode!\n");
                }
                if (m_paymentFactory.getNotProcessedPayments().size()>0) {
                	doneMessage.append(m_paymentFactory.getNotProcessedPayments().size() + " payments were not processed.\n");
                	showMessage = true;
                }
    			MLBSettings autoSavePdf = m_lbSettings.get(MLBSettings.PDF_AUTO_SAVE);
    			if (autoSavePdf==null || "false".equalsIgnoreCase(autoSavePdf.getName())) {

	                doneMessage.append("Do you want to see a report of the payments?");
	                int result = JOptionPane.showConfirmDialog(null, doneMessage.toString(),
	                									"Date " + m_paymentFactory.getTrxDate() + " successfully read",
	                									JOptionPane.YES_NO_OPTION,
	                									JOptionPane.QUESTION_MESSAGE);
	
	                if (result==JOptionPane.YES_OPTION) {
	                	createPaymentReport(m_reportSubDir, ""); // Since autoSavePdf is false, the report will be shown in JasperViewer and the user
	                										// can choose if/where to save the pdf.
	                }
    			} else {
    				if (showMessage) {
    					JOptionPane.showMessageDialog(null, doneMessage.toString(), m_paymentFactory.getFile()!=null ? m_paymentFactory.getFile().getName() : "", JOptionPane.WARNING_MESSAGE);
    				}
    				String fileName = createPaymentReport(m_reportSubDir, m_paymentFactory.getFile().getName() + ".pdf");
    				try {
						openFileWithSystemProgram(fileName);
					} catch (IOException e) {
						e.printStackTrace();
					}
    			}
            	
            }
        };
        worker.start();
		
	}
	

	
	
    /**
     * Opens a file using the windows application associated with the file type.
	 *
     * Convenience method to open a report file
     * 
     * @param fileName
     * @throws IOException
     */
    public void openFileWithSystemProgram(String fileName) throws IOException {
    	ProcessBuilder pb = new ProcessBuilder();
    	List<String> cmds = new ArrayList<String>();
    	String os = System.getProperty("os.name").toLowerCase();
    	if (os.contains("win")) {
    		cmds.add("rundll32");
    		cmds.add("shell32.dll,ShellExec_RunDLL");
    	} else if (os.contains("nix") || os.contains("nux")) {
    		cmds.add("gnome-open");
    	} else {
    		System.err.print("Can't open file " + fileName + " on this os: " + os);
    		return;
    	}
    	cmds.add(fileName);		// Must be absolute path unless you're sure of the working dir.
    	pb.command(cmds);
    	pb.start();
    }
	
	/**
	 * Method to put not identified payments last in the vector (for reporting purposes)
	 * 
	 * @param payments
	 * @return
	 */
	protected Collection<PaymentExtendedRecord> putNotIdentifiedPaymentsLast(Collection<PaymentExtendedRecord> payments) {
		
		Vector<PaymentExtendedRecord> result = new Vector<PaymentExtendedRecord>();
		Vector<PaymentExtendedRecord> tmp = new Vector<PaymentExtendedRecord>();
		PaymentExtendedRecord pmt;
		
		for (Iterator<PaymentExtendedRecord> it = payments.iterator(); it.hasNext(); ) {
			pmt = it.next();
			if (pmt.isBPartnerIdentified()) {
				 result.add(pmt);
			} else {
				tmp.add(pmt);
			}
		}
		// Put all payments in tmp at the end of the resulting vector 
		for (Iterator<PaymentExtendedRecord> it = tmp.iterator(); it.hasNext();) { 
			pmt = it.next();
			result.add(pmt);
		}
		
		return(result);
	}
	
	/**
	 * Plugin type can be either of
	 * 
	 * TYPE_PAYABLE
	 * TYPE_RECEIVABLE
	 * 
	 * The type determines where it will show on the menu (under receivables or payables)
	 * 
	 * @return
	 */
	public abstract String getPluginType();
	
	
}
