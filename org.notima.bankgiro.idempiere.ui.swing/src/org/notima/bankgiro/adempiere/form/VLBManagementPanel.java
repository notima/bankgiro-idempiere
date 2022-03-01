/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * VLBManagementPanel.java
 *
 * Created on 2009-mar-05, 20:02:36
 */

package org.notima.bankgiro.adempiere.form;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.lang.reflect.Constructor;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

import org.compiere.apps.ADialog;
import org.compiere.grid.ed.VCellRenderer;
import org.compiere.grid.ed.VComboBox;
import org.compiere.grid.ed.VDate;
import org.compiere.grid.ed.VLookup;
import org.compiere.grid.ed.VNumber;
import org.compiere.model.MBankAccount;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.swing.CLabel;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.notima.bankgiro.adempiere.BankInfo;
import org.notima.bankgiro.adempiere.LbPayStatusCellRenderer;
import org.notima.bankgiro.adempiere.LbPaymentRow;
import org.notima.bankgiro.adempiere.LbPaymentTableModel;
import org.notima.bankgiro.adempiere.PluginRegistry;
import org.notima.bankgiro.adempiere.ReceivablesTableModel;
import org.notima.bankgiro.adempiere.SumUpdate;
import org.notima.bankgiro.adempiere.model.MLBPlugin;
import org.notima.bankgiro.adempiere.model.MLBSettings;
import org.notima.bankgiro.idempiere.ui.swing.DateCellEditor;
import org.notima.bankgiro.idempiere.ui.swing.PaymentPlugin;
import org.notima.bankgiro.idempiere.ui.swing.PriceCellEditor;

/**
 *
 * @author Daniel Tamm
 */
public class VLBManagementPanel extends javax.swing.JPanel implements ActionListener, VetoableChangeListener, SumUpdate {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1892465441791317691L;

	private static CLogger Log = CLogger.getCLogger(VLBManagementPanel.class);

    private LbPaymentTableModel     payablesModel;
    private ReceivablesTableModel   receivablesModel;
    private VLBManagement           m_parent;
    private VComboBox fieldBankAccount = new VComboBox();
    private VNumber   balanceText = new VNumber();
    private Action      m_paymentSettingsAction;
    private Action      m_changePayStatusAction;
    private CLabel      m_startDateLabel;
    private VDate       m_startDate;
    private CLabel      m_endDateLabel;
    private VDate       m_endDate;
    private VLookup     fBPartner;
    private NumberFormat m_nf;
    private Vector<PaymentPlugin> m_plugins = new Vector<PaymentPlugin>();

    /** Creates new form VLBManagementPanel */
    public VLBManagementPanel(VLBManagement parent) {
        m_parent = parent;
        m_nf = DisplayType.getNumberFormat(DisplayType.Amount);
        payablesModel = new LbPaymentTableModel();
        payablesModel.setSumUpdater(this);
        receivablesModel = new ReceivablesTableModel();
        receivablesModel.setSumUpdater(this);
        initComponents();
        readBankAccounts();
        bankAccountPanel.add(fieldBankAccount, BorderLayout.CENTER);
        fieldBankAccount.addActionListener(this);
        balanceText.setReadWrite(false);
        balancePanel.add(balanceText, BorderLayout.CENTER);
        // Add controls to filterPanel
        // Add items to filterCombo
        DefaultComboBoxModel filterComboModel = new DefaultComboBoxModel();
        LbPayStatusCellRenderer payStatusRenderer = new LbPayStatusCellRenderer();
        filterComboModel.addElement(payStatusRenderer.getPayStatusName(0));
        filterComboModel.addElement(payStatusRenderer.getPayStatusName(1));
        filterComboModel.addElement(payStatusRenderer.getPayStatusName(2));
        filterComboModel.addElement(payStatusRenderer.getPayStatusName(3));
        filterCombo.setModel(filterComboModel);
        filterCombo.setSelectedIndex(1);
        m_startDateLabel = new CLabel("Start Date");
        filterPanel.add(m_startDateLabel);
        m_startDate = new VDate();
        filterPanel.add(m_startDate);
        m_endDateLabel = new CLabel("End Date");
        filterPanel.add(m_endDateLabel);
        m_endDate = new VDate();
        filterPanel.add(m_endDate);
        CLabel bPartnerLabel = new CLabel(Msg.translate(Env.getCtx(), "C_BPartner_ID"));
        filterPanel.add(bPartnerLabel);
        // Create business partner pick
        MLookup bpL = MLookupFactory.get(Env.getCtx(), m_parent.getWindowNo(), 0, 2762, DisplayType.Search);
        fBPartner = new VLookup("C_BPartner_ID", false, false, true, bpL);
        fBPartner.addVetoableChangeListener(this);
        filterPanel.add(fBPartner);

        DefaultTableCellRenderer priceRenderer = new VCellRenderer(DisplayType.Amount);
        DefaultTableCellRenderer dateRenderer = new VCellRenderer(DisplayType.Date);
        DateCellEditor dateEditor = new DateCellEditor();
        PriceCellEditor priceEditor = new PriceCellEditor();
        TableColumn col;
        DefaultTableColumnModel colModel = (DefaultTableColumnModel)invoiceTable.getColumnModel();
        col = colModel.getColumn(LbPaymentTableModel.COL_SELECT);
        col.setPreferredWidth(15);
        col = colModel.getColumn(LbPaymentTableModel.COL_PAYSTATUS);
        col.setCellRenderer(payStatusRenderer);
        col.setPreferredWidth(60);
        col = colModel.getColumn(LbPaymentTableModel.COL_BPARTNER);
        col.setPreferredWidth(120);
        col = colModel.getColumn(LbPaymentTableModel.COL_INVOICEID);
        col.setPreferredWidth(70);
        col = colModel.getColumn(LbPaymentTableModel.COL_OCRREF);
        col.setPreferredWidth(120);
        col = colModel.getColumn(LbPaymentTableModel.COL_DSTACCT);
        col.setPreferredWidth(220);
        col = colModel.getColumn(LbPaymentTableModel.COL_INVDATE);
        col.setPreferredWidth(70);
        col.setCellRenderer(dateRenderer);
        col = colModel.getColumn(LbPaymentTableModel.COL_DUEDATE);
        col.setPreferredWidth(70);
        col.setCellRenderer(dateRenderer);
        col = colModel.getColumn(LbPaymentTableModel.COL_PAYDATE);
        col.setPreferredWidth(70);
        col.setCellRenderer(dateRenderer);
        col.setCellEditor(dateEditor);
        col = colModel.getColumn(LbPaymentTableModel.COL_PAYAMOUNT);
        col.setPreferredWidth(80);
        col.setCellRenderer(priceRenderer);
        col.setCellEditor(priceEditor);
        col = colModel.getColumn(LbPaymentTableModel.COL_AMOUNTDUE);
        col.setPreferredWidth(80);
        col.setCellRenderer(priceRenderer);
        col = colModel.getColumn(LbPaymentTableModel.COL_CURRENCY);
        col.setPreferredWidth(20);
        col = colModel.getColumn(LbPaymentTableModel.COL_PRODDATE);
        col.setPreferredWidth(70);
        col.setCellRenderer(dateRenderer);

        invoiceTable.setAutoCreateRowSorter(true);

        colModel = (DefaultTableColumnModel)receivablesTable.getColumnModel();
        col = colModel.getColumn(ReceivablesTableModel.COL_SELECT);
        col.setPreferredWidth(15);
        col = colModel.getColumn(ReceivablesTableModel.COL_PAYSTATUS);
        col.setCellRenderer(payStatusRenderer);
        col.setPreferredWidth(60);
        col = colModel.getColumn(ReceivablesTableModel.COL_BPARTNER);
        col.setPreferredWidth(120);
        col = colModel.getColumn(ReceivablesTableModel.COL_OCRREF);
        col.setPreferredWidth(120);
        col = colModel.getColumn(ReceivablesTableModel.COL_INVDATE);
        col.setCellRenderer(dateRenderer);
        col = colModel.getColumn(ReceivablesTableModel.COL_DUEDATE);
        col.setCellRenderer(dateRenderer);
        col = colModel.getColumn(ReceivablesTableModel.COL_PAYDATE);
        col.setCellRenderer(dateRenderer);
        col.setCellEditor(dateEditor);
        col = colModel.getColumn(ReceivablesTableModel.COL_PAYAMOUNT);
        col.setCellRenderer(priceRenderer);
        col.setCellEditor(priceEditor);
        col = colModel.getColumn(ReceivablesTableModel.COL_AMOUNTDUE);
        col.setCellRenderer(priceRenderer);

        receivablesTable.setAutoCreateRowSorter(true);

        /**
         * Actions
         */
        m_paymentSettingsAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openSettingsDialog();
            }

        };
        m_paymentSettingsAction.putValue(Action.NAME, "Payment Settings");
        m_paymentSettingsAction.putValue(Action.SHORT_DESCRIPTION, "Payment Settings");

        // TODO: Read payment plugins in a dynamic way
        PaymentPlugin plugin;
        
        // Read plugins
        List<MLBPlugin> pluginList = MLBPlugin.getActivePlugins();
        Class pluginClass = null;
        Constructor constructor;
        String className;
        for (Iterator<MLBPlugin> it = pluginList.iterator(); it.hasNext();) {
        	try {
        		className = it.next().getClassname();
        		System.out.println("Adding plugin " + className);
        		pluginClass = Class.forName(className);
        		constructor = pluginClass.getConstructor(Properties.class, VLBManagementPanel.class);
        		plugin = (PaymentPlugin)constructor.newInstance(new Object[]{Env.getCtx(), this});
        		m_plugins.add(plugin);
        	} catch (Exception ee) {
        		Log.warning("Problem adding plugin " + (pluginClass!=null ? pluginClass.getName() : "") + ". Error: " + ee.getMessage());
        		ee.printStackTrace();
        	}
        }

        m_changePayStatusAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setPayStatus();
            }
        };
        m_changePayStatusAction.putValue(Action.NAME, "Set pay status");
        m_changePayStatusAction.putValue(Action.SHORT_DESCRIPTION, "Set pay status");

        // Add custom menu items to the Frame menu
        JMenuBar menuBar = m_parent.getFrame().getJMenuBar();
        JMenu lbSettingsMenu = new JMenu("Payment Settings");
        JMenuItem menuItem = new JMenuItem(m_paymentSettingsAction);
        lbSettingsMenu.add(menuItem);
        JMenu readPayablesMenu = new JMenu("Payables...");
        lbSettingsMenu.add(readPayablesMenu);
        for (Iterator<PaymentPlugin> it = m_plugins.iterator(); it.hasNext();) {
        	plugin = it.next();
        	if (plugin.getPluginType()==PaymentPlugin.TYPE_BOTH && plugin.getPayableActions()!=null) {
        		for (int k=0; k<plugin.getPayableActions().length; k++) {
        			menuItem = new JMenuItem(plugin.getActions()[k]);
        			readPayablesMenu.add(menuItem);
        		}
        	}
        	if (plugin.getPluginType()==PaymentPlugin.TYPE_PAYABLE && plugin.getActions()!=null) {
        		for (int k=0; k<plugin.getActions().length; k++) {
        			menuItem = new JMenuItem(plugin.getActions()[k]);
        			readPayablesMenu.add(menuItem);
        		}
        	}
        }
        JMenu readReceivablesMenu = new JMenu("Receivables...");
        lbSettingsMenu.add(readReceivablesMenu);
        for (Iterator<PaymentPlugin> it = m_plugins.iterator(); it.hasNext();) {
        	plugin = it.next();
        	if (plugin.getPluginType()==PaymentPlugin.TYPE_BOTH && plugin.getReceivableActions()!=null) {
        		for (int k=0; k<plugin.getReceivableActions().length; k++) {
        			menuItem = new JMenuItem(plugin.getReceivableActions()[k]);
        			readReceivablesMenu.add(menuItem);
        		}
        	}
        	if (plugin.getPluginType()==PaymentPlugin.TYPE_RECEIVABLE && plugin.getActions()!=null) {
        		for (int k=0; k<plugin.getActions().length; k++) {
        			menuItem = new JMenuItem(plugin.getActions()[k]);
        			readReceivablesMenu.add(menuItem);
        		}
        	}
        }
        menuItem = new JMenuItem(m_changePayStatusAction);
        lbSettingsMenu.add(menuItem);
        
        menuBar.add(lbSettingsMenu, 3);

        runRefresh(); // Start with refreshing
    }

    public VLBManagement getVLBParent() {
    	return(m_parent);
    }
    
    public MBankAccount getSelectedBankAccount() {
		BankInfo bi = (BankInfo)fieldBankAccount.getSelectedItem();
		MBankAccount ourBa = new MBankAccount(Env.getCtx(), bi.C_BankAccount_ID, null);
		return(ourBa);
    }
    
    /**
     * Refreshes the tables of payments
     */
    private void refresh(PropertyChangeSupport progressMsg) {
        if (fBPartner==null) return; // Form not yet initialized
        Object v = fBPartner.getValue();
        int bPartnerId = 0;
        if (v!=null && v instanceof Integer) {
            bPartnerId = ((Integer)v).intValue();
        }
        try {
            // Check which tab is visible
            int tabIndex = tabbedPane.getSelectedIndex();
            switch(tabIndex) {
                case 0: payablesModel.clear();
                		payablesModel.loadSelection("Z", bPartnerId, (java.util.Date)m_startDate.getValue(), (java.util.Date)m_endDate.getValue(), filterCombo.getSelectedIndex(), progressMsg);
                        break;
                case 1: 
                		receivablesModel.clear();
                		receivablesModel.loadSelection(bPartnerId, (java.util.Date)m_startDate.getValue(), (java.util.Date)m_endDate.getValue(), filterCombo.getSelectedIndex(), progressMsg);
                        break;
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    public void runRefresh() {

        // Check which tab is visible
        int tabIndex = tabbedPane.getSelectedIndex();
        final AbstractTableModel tModel = tabIndex==0 ? payablesModel : receivablesModel;
    	
    	SwingWorker<Object,Object> worker = new SwingWorker<Object,Object>() {

			@Override
			protected Object doInBackground() throws Exception {
				refresh(this.getPropertyChangeSupport());
				return null;
			}

			@Override
			protected void done() {
				super.done();
				tModel.fireTableDataChanged();
				doneRunning();
			}
    		
    	};
    	
    	// TODO: Use theme colors
        progressBar.setForeground(Color.BLUE);
    	
    	worker.addPropertyChangeListener(new PropertyChangeListener() {
    		
    		public void propertyChange(PropertyChangeEvent evt) {
    			if ("text".equals(evt.getPropertyName())) {
    				progressBar.setString(evt.getNewValue().toString());
    			}
    			if ("invoiceMax".equals(evt.getPropertyName())) {
    				progressBar.setMaximum((Integer)evt.getNewValue());
    				progressBar.setMinimum((Integer)evt.getOldValue());
    			}
    			if ("invoiceCount".equals(evt.getPropertyName())) {
    				int count = (Integer)evt.getNewValue();
    				progressBar.setValue(count);
    				progressBar.setString(count + "/" + progressBar.getMaximum());
    				if (count%10==0)
    					tModel.fireTableDataChanged();
    			}
    		}
    		
    	});
    	
    	// m_parent.getFrame().setBusy(true);
    	
    	worker.execute();
    	
    }
    
    /**
     * Call this method to end the "busy" mode of the window.
     */
    public void doneRunning() {
    	m_parent.getFrame().setBusy(false);
    	m_parent.getFrame().setEnabled(true);
    	m_parent.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    	progressBar.setValue(0);
    }
    
    private void readBankAccounts() {

		//  Bank Account Info
		String sql = 
			"SELECT DISTINCT C_BankAccount.C_BankAccount_ID,"                       //  1
			+ "C_Bank.Name || ' ' || AccountNo AS Name,"          //  2
			+ "C_BankAccount.C_Currency_ID, C_Currency.ISO_Code,"                   //  3..4
			+ "CurrentBalance "                              //  5
			+ "FROM C_BankAccount "
			+ "LEFT JOIN C_Bank ON C_Bank.C_Bank_ID=C_BankAccount.C_Bank_ID "
			+ "LEFT JOIN C_Currency ON C_BankAccount.C_Currency_ID=C_Currency.C_Currency_ID "
			+ "WHERE C_BankAccount.isActive='Y' AND C_BankAccount.AD_Client_ID=? "
			+ "AND C_BankAccount.C_BankAccount_ID IN (SELECT C_BankAccount_ID FROM C_BankAccountDoc WHERE XC_LBPlugin_ID>0) "
			+ "ORDER BY 2";
		
		try
		{
			fieldBankAccount.removeAllItems();
			PreparedStatement pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, Env.getAD_Client_ID(Env.getCtx()));
			ResultSet rs = pstmt.executeQuery();
			while (rs.next())
			{
				boolean transfers = false;
				BankInfo bi = new BankInfo (rs.getInt(1), rs.getInt(3),
					rs.getString(2), rs.getString(4),
					rs.getBigDecimal(5), transfers);
				fieldBankAccount.addItem(bi);
			}
			rs.close();
			pstmt.close();
		}
		catch (SQLException e)
		{
			Log.log(Level.SEVERE, "VLBManagementPanel.readBankAccounts - BA", e);
		}
		if (fieldBankAccount.getItemCount() == 0)
			ADialog.error(m_parent.getWindowNo(), this, "VPaySelectNoBank");
		else
			fieldBankAccount.setSelectedIndex(0);
        setBalance();
    }

    private void setBalance() {
        BankInfo bi = (BankInfo)fieldBankAccount.getSelectedItem();
        if (bi!=null) {
            balanceText.setValue(bi.Balance);
        } else {
            balanceText.setValue(null);
        }
    }


    private void closeForm() {
        m_parent.dispose();
    }

    /**
     * Opens settings dialog
     */
    public void openSettingsDialog() {
        XX_LBSettingsDialog diag = new XX_LBSettingsDialog(m_parent.getFrame(), true, PluginRegistry.registry.getLbSettings(), m_parent.getWindowNo(), m_plugins);
        diag.setLocationRelativeTo(m_parent.getFrame());
        diag.setVisible(true);
        // Re-read settings
        MLBSettings.putSettings(PluginRegistry.registry.getLbSettings());
    }

    private void setPayStatus() {
        if (tabbedPane.getSelectedIndex()==0) {
            // Payables
            Vector<LbPaymentRow> selected = payablesModel.getSelected();
            if (selected.size()==0) {
                JOptionPane.showMessageDialog(m_parent.getFrame(), "No invoices are selected.");
                return;
            }
            // If in transit, change to not paid. If not paid, change to in transit.
            int changeToStatus = selected.get(0).payStatus==LbPaymentRow.PAYSTATUS_NOTPAID ? LbPaymentRow.PAYSTATUS_INTRANSIT : LbPaymentRow.PAYSTATUS_NOTPAID;
            SelectPayStatusDialog dialog = new SelectPayStatusDialog(m_parent.getFrame(), true, changeToStatus);
            dialog.setLocationRelativeTo(m_parent.getFrame());
            dialog.setVisible(true);
            if (dialog.getRetVal()==JOptionPane.OK_OPTION) {
                int newPayStatus = dialog.getPaymentStatus();
                for (Iterator<LbPaymentRow> it = selected.iterator(); it.hasNext();) {
                    it.next().setPayStatus(newPayStatus);
                }
            }
        } else {
            // Receivables
            Vector<LbPaymentRow> selected = receivablesModel.getSelected();
            if (selected.size()==0) {
                JOptionPane.showMessageDialog(m_parent.getFrame(), "No customer invoices are selected.");
                return;
            }
            // If in transit, change to not paid. If not paid, change to in transit.
            int changeToStatus = selected.get(0).payStatus==LbPaymentRow.PAYSTATUS_NOTPAID ? LbPaymentRow.PAYSTATUS_PAID : LbPaymentRow.PAYSTATUS_NOTPAID;
            SelectPayStatusDialog dialog = new SelectPayStatusDialog(m_parent.getFrame(), true, changeToStatus);
            dialog.setLocationRelativeTo(m_parent.getFrame());
            dialog.setVisible(true);
            if (dialog.getRetVal()==JOptionPane.OK_OPTION) {
                int newPayStatus = dialog.getPaymentStatus();
                for (Iterator<LbPaymentRow> it = selected.iterator(); it.hasNext();) {
                    it.next().setPayStatus(newPayStatus);
                }
            }
        }
        runRefresh();
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

        topPanel = new javax.swing.JPanel();
        leftTopPanel = new javax.swing.JPanel();
        bankAccountLabel = new javax.swing.JLabel();
        bankAccountPanel = new javax.swing.JPanel();
        balanceLabel = new javax.swing.JLabel();
        balancePanel = new javax.swing.JPanel();
        rightTopPanel = new javax.swing.JPanel();
        refreshButton = new javax.swing.JButton();
        filterPanel = new javax.swing.JPanel();
        filterCombo = new javax.swing.JComboBox();
        tabbedPane = new javax.swing.JTabbedPane();
        mainPanel = new javax.swing.JPanel();
        payablesScrollPane = new javax.swing.JScrollPane();
        invoiceTable = new javax.swing.JTable();
        receivablesPanel = new javax.swing.JPanel();
        receivablesScrollPane = new javax.swing.JScrollPane();
        receivablesTable = new javax.swing.JTable();
        bottomPanel = new javax.swing.JPanel();
        leftBottomPanel = new javax.swing.JPanel();
        sumsLabel = new javax.swing.JLabel();
        sumsText = new javax.swing.JTextField();
        selectedCountLabel = new javax.swing.JLabel();
        selectedCountText = new javax.swing.JTextField();
        allSumsLabel = new javax.swing.JLabel();
        allSumsText = new javax.swing.JTextField();
        allCountLabel = new javax.swing.JLabel();
        allCountText = new javax.swing.JTextField();
        rightBottomPanel = new javax.swing.JPanel();
        progressBar = new javax.swing.JProgressBar();
        closeButton = new javax.swing.JButton();

        setLayout(new java.awt.BorderLayout());

        topPanel.setLayout(new java.awt.GridBagLayout());

        leftTopPanel.setLayout(new java.awt.GridBagLayout());

        bankAccountLabel.setText("Source bank account:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        leftTopPanel.add(bankAccountLabel, gridBagConstraints);

        bankAccountPanel.setPreferredSize(new java.awt.Dimension(180, 20));
        bankAccountPanel.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.75;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        leftTopPanel.add(bankAccountPanel, gridBagConstraints);

        balanceLabel.setText("Current balance:");
        leftTopPanel.add(balanceLabel, new java.awt.GridBagConstraints());

        balancePanel.setPreferredSize(new java.awt.Dimension(140, 20));
        balancePanel.setLayout(new java.awt.BorderLayout());
        leftTopPanel.add(balancePanel, new java.awt.GridBagConstraints());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        topPanel.add(leftTopPanel, gridBagConstraints);

        rightTopPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        refreshButton.setText("Refresh");
        refreshButton.setToolTipText("Refresh");
        refreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshButtonActionPerformed(evt);
            }
        });
        rightTopPanel.add(refreshButton);

        topPanel.add(rightTopPanel, new java.awt.GridBagConstraints());

        filterPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        filterCombo.setPreferredSize(new java.awt.Dimension(80, 20));
        filterCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterComboActionPerformed(evt);
            }
        });
        filterPanel.add(filterCombo);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        topPanel.add(filterPanel, gridBagConstraints);

        add(topPanel, java.awt.BorderLayout.NORTH);

        tabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabbedPaneStateChanged(evt);
            }
        });

        mainPanel.setLayout(new java.awt.BorderLayout());

        invoiceTable.setModel(payablesModel);
        payablesScrollPane.setViewportView(invoiceTable);

        mainPanel.add(payablesScrollPane, java.awt.BorderLayout.CENTER);

        tabbedPane.addTab("Payables", mainPanel);

        receivablesPanel.setLayout(new java.awt.BorderLayout());

        receivablesTable.setModel(receivablesModel);
        receivablesScrollPane.setViewportView(receivablesTable);

        receivablesPanel.add(receivablesScrollPane, java.awt.BorderLayout.CENTER);

        tabbedPane.addTab("Receivables", receivablesPanel);

        add(tabbedPane, java.awt.BorderLayout.CENTER);

        bottomPanel.setLayout(new java.awt.BorderLayout());

        sumsLabel.setText("Sum Selected");
        leftBottomPanel.add(sumsLabel);

        sumsText.setEditable(false);
        sumsText.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        sumsText.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        sumsText.setMinimumSize(new java.awt.Dimension(4, 20));
        sumsText.setPreferredSize(new java.awt.Dimension(100, 20));
        leftBottomPanel.add(sumsText);

        selectedCountLabel.setText("# Selected");
        leftBottomPanel.add(selectedCountLabel);

        selectedCountText.setEditable(false);
        selectedCountText.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        selectedCountText.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        selectedCountText.setPreferredSize(new java.awt.Dimension(50, 20));
        leftBottomPanel.add(selectedCountText);

        allSumsLabel.setText("Sum All");
        leftBottomPanel.add(allSumsLabel);

        allSumsText.setEditable(false);
        allSumsText.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        allSumsText.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        allSumsText.setPreferredSize(new java.awt.Dimension(100, 20));
        leftBottomPanel.add(allSumsText);

        allCountLabel.setText("# All");
        leftBottomPanel.add(allCountLabel);

        allCountText.setEditable(false);
        allCountText.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        allCountText.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        allCountText.setPreferredSize(new java.awt.Dimension(50, 20));
        leftBottomPanel.add(allCountText);

        bottomPanel.add(leftBottomPanel, java.awt.BorderLayout.WEST);

        rightBottomPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 3, 3, 3));
        rightBottomPanel.setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        rightBottomPanel.add(progressBar, gridBagConstraints);

        closeButton.setText("Close");
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });
        rightBottomPanel.add(closeButton, new java.awt.GridBagConstraints());

        bottomPanel.add(rightBottomPanel, java.awt.BorderLayout.CENTER);

        add(bottomPanel, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    private void refreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshButtonActionPerformed

        runRefresh();

    }//GEN-LAST:event_refreshButtonActionPerformed

    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed

        closeForm();

    }//GEN-LAST:event_closeButtonActionPerformed

    private void filterComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterComboActionPerformed

        runRefresh();

    }//GEN-LAST:event_filterComboActionPerformed

    private void tabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabbedPaneStateChanged

        if (evt.getSource() instanceof JTabbedPane) {
            JTabbedPane src = (JTabbedPane)evt.getSource();
            if (src.getSize().width>0) {
//                runRefresh();
            }
        }

    }//GEN-LAST:event_tabbedPaneStateChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel allCountLabel;
    private javax.swing.JTextField allCountText;
    private javax.swing.JLabel allSumsLabel;
    private javax.swing.JTextField allSumsText;
    private javax.swing.JLabel balanceLabel;
    private javax.swing.JPanel balancePanel;
    private javax.swing.JLabel bankAccountLabel;
    private javax.swing.JPanel bankAccountPanel;
    private javax.swing.JPanel bottomPanel;
    private javax.swing.JButton closeButton;
    private javax.swing.JComboBox filterCombo;
    private javax.swing.JPanel filterPanel;
    private javax.swing.JTable invoiceTable;
    private javax.swing.JPanel leftBottomPanel;
    private javax.swing.JPanel leftTopPanel;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JScrollPane payablesScrollPane;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JPanel receivablesPanel;
    private javax.swing.JScrollPane receivablesScrollPane;
    private javax.swing.JTable receivablesTable;
    private javax.swing.JButton refreshButton;
    private javax.swing.JPanel rightBottomPanel;
    private javax.swing.JPanel rightTopPanel;
    private javax.swing.JLabel selectedCountLabel;
    private javax.swing.JTextField selectedCountText;
    private javax.swing.JLabel sumsLabel;
    private javax.swing.JTextField sumsText;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JPanel topPanel;
    // End of variables declaration//GEN-END:variables

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource()==fieldBankAccount) {
            setBalance();
        }
    }

    @Override
    public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {

        if ("C_BPartner_ID".equals(evt.getPropertyName())) {
            runRefresh();
        }


    }

    @Override
    public void updateSums(double selectedAmount, int selectedCount, double allAmount, int allCount) {
        sumsText.setText(m_nf.format(selectedAmount));
		selectedCountText.setText(selectedCount + "");
		allCountText.setText((tabbedPane.getSelectedIndex()==0 ? payablesModel.getRowCount() : receivablesModel.getRowCount()) + "");
		allSumsText.setText(m_nf.format(allAmount));
        
    }

	public LbPaymentTableModel getPayablesModel() {
		return payablesModel;
	}

	public void setPayablesModel(LbPaymentTableModel payablesModel) {
		this.payablesModel = payablesModel;
	}

	public ReceivablesTableModel getReceivablesModel() {
		return receivablesModel;
	}

	public void setReceivablesModel(ReceivablesTableModel receivablesModel) {
		this.receivablesModel = receivablesModel;
	}

}
