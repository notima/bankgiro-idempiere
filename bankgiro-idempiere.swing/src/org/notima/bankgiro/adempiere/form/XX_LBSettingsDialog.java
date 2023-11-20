/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * XX_LBSettingsDialog.java
 *
 * Created on 2009-feb-23, 10:58:51
 */

package org.notima.bankgiro.adempiere.form;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.compiere.grid.ed.VLookup;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.notima.bankgiro.adempiere.model.MLBSettings;
import org.notima.bankgiro.idempiere.ui.swing.PaymentPlugin;

/**
 *
 * @author Daniel Tamm
 */
public class XX_LBSettingsDialog extends javax.swing.JDialog implements VetoableChangeListener {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private SortedMap<String, MLBSettings>      m_settings;
    private VLookup                             fBPartner;
    private int                                 m_WindowNo;
    private Vector<I_LBSettingsPanel>			m_pluginPanels;

    /** Creates new form XX_LBSettingsDialog */
    public XX_LBSettingsDialog(java.awt.Frame parent, boolean modal, SortedMap<String, MLBSettings> settings, int windowNo, Vector<PaymentPlugin> plugins) {

        super(parent, modal);
        m_pluginPanels = new Vector<I_LBSettingsPanel>();
        m_WindowNo = windowNo;
        fillPicks();

        initComponents();
        unknownPayerPanel.add(fBPartner);
        m_settings = settings;
        
        // Add plugin panels
        I_LBSettingsPanel panel;
        if (plugins!=null) {
            for (Iterator<PaymentPlugin> it = plugins.iterator(); it.hasNext(); ) {
            	panel = it.next().getPanel();
            	if (panel!=null) {
            		panel.initPanel(this, m_settings);
            		addPluginPanel(panel);
            	}
            }
        }

        setForm();

    }

    private void fillPicks() {
        // Create business partner pick
        MLookup bpL = MLookupFactory.get(Env.getCtx(), m_WindowNo, 0, 2762, DisplayType.Search);
        fBPartner = new VLookup("C_BPartner_ID", false, false, true, bpL);
        fBPartner.addVetoableChangeListener(this);
    }


    /**
     * Sets the values of the form components based on the settings
     */
    public void setForm() {

        MLBSettings unknownPayer = m_settings.get(MLBSettings.BG_UNKNOWN_PAYER_BPID);
        if (unknownPayer!=null) {
            fBPartner.setValue(new Integer(unknownPayer.getName()));
        }
        MLBSettings dryRunFlag = m_settings.get(MLBSettings.BG_DRYRUN_FLAG);
        dryRunCheck.setSelected(dryRunFlag!=null && !"false".equalsIgnoreCase(dryRunFlag.getName()));

        MLBSettings pdfReportPath = m_settings.get(MLBSettings.PDF_REPORT_PATH);
        pdfReportTextField.setText(pdfReportPath!=null ? pdfReportPath.getName() : "");
        
        MLBSettings pdfAutoSave = m_settings.get(MLBSettings.PDF_AUTO_SAVE);
        autoSaveReportsCheck.setSelected(pdfAutoSave!=null && !"false".equalsIgnoreCase(pdfAutoSave.getName()));
        
        MLBSettings autoComplete = m_settings.get(MLBSettings.PMT_AUTO_COMPLETE);
        autoCompleteCheck.setSelected(autoComplete!=null && !"false".equalsIgnoreCase(autoComplete.getName()));
        
        MLBSettings createOutboundReport = m_settings.get(MLBSettings.PMT_REPORT_FOR_OUT_FILE);
        createOutboundFileReportCheck.setSelected(createOutboundReport!=null && !"false".equalsIgnoreCase(createOutboundReport.getName()));
        
        MLBSettings amountThreshold = m_settings.get(MLBSettings.AMT_THRESHOLD);
        tresholdText.setText(amountThreshold!=null ? amountThreshold.getName() : "");
        
        // Set plugin panels
        I_LBSettingsPanel pluginPanel;
        Component c;
        for (int i=0; i<baseTabbedPane.getTabCount(); i++) {
            c = baseTabbedPane.getComponent(i);
            if (c instanceof I_LBSettingsPanel) {
                pluginPanel = (I_LBSettingsPanel)c;
                pluginPanel.setForm();
            }
        }

    }

    /**
     * Gets values from the form's components and saves to database.
     */
    public void getForm() {
        if (fBPartner.getValue()!=null) {
            MLBSettings.setSetting(MLBSettings.BG_UNKNOWN_PAYER_BPID, fBPartner.getValue().toString());
        }
        MLBSettings.setSetting(MLBSettings.BG_DRYRUN_FLAG, Boolean.valueOf(dryRunCheck.isSelected()).toString());
        MLBSettings.setSetting(MLBSettings.PDF_REPORT_PATH, pdfReportTextField.getText());
        MLBSettings.setSetting(MLBSettings.PDF_AUTO_SAVE, Boolean.valueOf(autoSaveReportsCheck.isSelected()).toString());
        MLBSettings.setSetting(MLBSettings.PMT_AUTO_COMPLETE, Boolean.valueOf(autoCompleteCheck.isSelected()).toString());
        MLBSettings.setSetting(MLBSettings.PMT_REPORT_FOR_OUT_FILE, Boolean.valueOf(createOutboundFileReportCheck.isSelected()).toString());
        try {
        	double threshold = Double.parseDouble(tresholdText.getText());
        	MLBSettings.setSetting(MLBSettings.AMT_THRESHOLD, Double.toString(threshold));
        } catch (Exception e) {
        	JOptionPane.showMessageDialog(null, "Amount threshold : " + tresholdText.getText() + " must be a number. " + e.getMessage());
        }
        
        // Save on plugin panels
        for (int i=0; i<m_pluginPanels.size(); i++) {
        	m_pluginPanels.get(i).getForm();
        }

    }

    /**
     * Adds plugin panel
     * @param pluginPanel
     */
    private void addPluginPanel(I_LBSettingsPanel pluginPanel) {
        baseTabbedPane.addTab(pluginPanel.getTitle(), null, (Component)pluginPanel, pluginPanel.getTitle());
        m_pluginPanels.add(pluginPanel);
    }

    /**
     * Browses for the output directory
     */
    public String browseForDirectory(String startDir) {

        JFileChooser fc = new JFileChooser(startDir);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int retVal = fc.showDialog(this, "Select directory");
        if (retVal==JFileChooser.APPROVE_OPTION) {
            return(fc.getSelectedFile().getAbsolutePath());
        }
        return(null);
    }

    /**
     * Browses for a file
     */
    public String browseForFile(String startFile) {

        JFileChooser fc = new JFileChooser(startFile);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int retVal = fc.showDialog(this, "Select file");
        if (retVal==JFileChooser.APPROVE_OPTION) {
            return(fc.getSelectedFile().getAbsolutePath());
        }
        return(null);
    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        baseTabbedPane = new javax.swing.JTabbedPane();
        generalOptionsPanel = new javax.swing.JPanel();
        unknownPayerLabel = new javax.swing.JLabel();
        unknownPayerPanel = new javax.swing.JPanel();
        dryRunCheck = new javax.swing.JCheckBox();
        filePanel = new javax.swing.JPanel();
        pdfReportDirectoryLabel = new javax.swing.JLabel();
        pdfReportTextField = new javax.swing.JTextField();
        browseButton = new javax.swing.JButton();
        autoSaveReportsCheck = new javax.swing.JCheckBox();
        completeOptionPanel = new javax.swing.JPanel();
        autoCompleteCheck = new javax.swing.JCheckBox();
        tresholdLabel = new javax.swing.JLabel();
        tresholdText = new javax.swing.JTextField();
        createOutboundFileReportCheck = new javax.swing.JCheckBox();
        buttonPanel = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("LB settings");

        baseTabbedPane.setName("General options"); // NOI18N
        baseTabbedPane.setPreferredSize(new java.awt.Dimension(480, 340));

        generalOptionsPanel.setLayout(new java.awt.GridBagLayout());

        unknownPayerLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        unknownPayerLabel.setText("Business partner for unidentified payments");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 5, 0);
        generalOptionsPanel.add(unknownPayerLabel, gridBagConstraints);

        unknownPayerPanel.setPreferredSize(new java.awt.Dimension(10, 20));
        unknownPayerPanel.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        generalOptionsPanel.add(unknownPayerPanel, gridBagConstraints);

        dryRunCheck.setText("Test mode (no payments will be created)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        generalOptionsPanel.add(dryRunCheck, gridBagConstraints);

        filePanel.setLayout(new java.awt.GridBagLayout());

        pdfReportDirectoryLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        pdfReportDirectoryLabel.setText("Default directory for PDF-reports");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        filePanel.add(pdfReportDirectoryLabel, gridBagConstraints);

        pdfReportTextField.setToolTipText("Directory where PDF-reports will be saved");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        filePanel.add(pdfReportTextField, gridBagConstraints);

        browseButton.setText("Browse");
        browseButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        filePanel.add(browseButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 0, 0);
        generalOptionsPanel.add(filePanel, gridBagConstraints);

        autoSaveReportsCheck.setText("Save reports automatically");
        autoSaveReportsCheck.setToolTipText("Saves reports without prompting the user");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 0, 0);
        generalOptionsPanel.add(autoSaveReportsCheck, gridBagConstraints);

        autoCompleteCheck.setText("Complete if amount and invoice matches");
        autoCompleteCheck.setMargin(new java.awt.Insets(2, 0, 2, 2));
        completeOptionPanel.add(autoCompleteCheck);

        tresholdLabel.setText("Amount treshold (in acct curr)");
        completeOptionPanel.add(tresholdLabel);

        tresholdText.setColumns(4);
        completeOptionPanel.add(tresholdText);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 7, 0, 0);
        generalOptionsPanel.add(completeOptionPanel, gridBagConstraints);

        createOutboundFileReportCheck.setText("Create report for outbound files");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 0, 0);
        generalOptionsPanel.add(createOutboundFileReportCheck, gridBagConstraints);

        baseTabbedPane.addTab("General Options", generalOptionsPanel);

        getContentPane().add(baseTabbedPane, java.awt.BorderLayout.CENTER);

        buttonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        buttonPanel.add(okButton);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        buttonPanel.add(cancelButton);

        getContentPane().add(buttonPanel, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed

        getForm();
        setVisible(false);
        dispose();

    }//GEN-LAST:event_okButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed

        setVisible(false);
        dispose();

    }//GEN-LAST:event_cancelButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox autoCompleteCheck;
    private javax.swing.JCheckBox autoSaveReportsCheck;
    private javax.swing.JTabbedPane baseTabbedPane;
    private javax.swing.JButton browseButton;
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JPanel completeOptionPanel;
    private javax.swing.JCheckBox createOutboundFileReportCheck;
    private javax.swing.JCheckBox dryRunCheck;
    private javax.swing.JPanel filePanel;
    private javax.swing.JPanel generalOptionsPanel;
    private javax.swing.JButton okButton;
    private javax.swing.JLabel pdfReportDirectoryLabel;
    private javax.swing.JTextField pdfReportTextField;
    private javax.swing.JLabel tresholdLabel;
    private javax.swing.JTextField tresholdText;
    private javax.swing.JLabel unknownPayerLabel;
    private javax.swing.JPanel unknownPayerPanel;
    // End of variables declaration//GEN-END:variables

    @Override
    public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
        
    }

	public int getWindowNo() {
		return m_WindowNo;
	}

}
