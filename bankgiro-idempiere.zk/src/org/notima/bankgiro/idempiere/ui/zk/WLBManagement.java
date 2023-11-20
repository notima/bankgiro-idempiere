package org.notima.bankgiro.idempiere.ui.zk;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListModelTable;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.component.ListboxFactory;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Tab;
import org.adempiere.webui.component.Tabbox;
import org.adempiere.webui.component.Tabpanel;
import org.adempiere.webui.component.Tabpanels;
import org.adempiere.webui.component.Tabs;
import org.adempiere.webui.component.WListbox;
import org.adempiere.webui.editor.WDateEditor;
import org.adempiere.webui.editor.WNumberEditor;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.event.TableValueChangeEvent;
import org.adempiere.webui.event.TableValueChangeListener;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.CustomForm;
import org.adempiere.webui.panel.IFormController;
import org.adempiere.webui.panel.StatusBarPanel;
import org.adempiere.webui.session.SessionManager;
import org.compiere.minigrid.ColumnInfo;
import org.compiere.minigrid.IDColumn;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.notima.bankgiro.adempiere.BankInfo;
import org.notima.bankgiro.adempiere.LBManagement;
import org.notima.bankgiro.adempiere.LbPaymentRow;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.North;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;

public class WLBManagement implements IFormController {

	private CustomForm form = new CustomForm();

	private int		m_WindowNo = 0;
	private int     m_AD_Client_ID = Env.getAD_Client_ID(Env.getCtx());
	private int     m_AD_Org_ID = Env.getAD_Org_ID(Env.getCtx());
	private int     m_by = Env.getAD_User_ID(Env.getCtx());
	
	/**
	 * Show Mode
	 */
	public static String[] showMode = new String[] {
		Msg.translate(Env.getCtx(), "All"),
		Msg.translate(Env.getCtx(), "Not.Paid"),
		Msg.translate(Env.getCtx(), "In.Transit"),
		Msg.translate(Env.getCtx(), "Paid"),
		Msg.translate(Env.getCtx(), "Not.Approved")
	};
	
	/**
	 * Initialize Panel
	 */
	public WLBManagement() {
		
		m_WindowNo = form.getWindowNo();
		lbManagement = new LBManagement();
		LBManagement.log.info("WinNo=" + m_WindowNo
				+ " - AD_Client_ID=" + m_AD_Client_ID + ", AD_Org_ID=" + m_AD_Org_ID + ", By=" + m_by);
			Env.setContext(Env.getCtx(), m_WindowNo, "IsSOTrx", "N");
			
			
		bPartnerSearch = WSearchEditor.createBPartner(m_WindowNo);
		try {
			
			zkInit();
			dynInit();
			
			LayoutUtils.addSclass("status-border", statusBar);
		} catch (Exception e) {
			LBManagement.log.log(Level.SEVERE, e.getMessage(), e);
		}
		
	}
	
	private LBManagement lbManagement;
	
	private Panel mainPanel = new Panel();
	private StatusBarPanel statusBar = new StatusBarPanel();
	private Borderlayout mainLayout = new Borderlayout();
	private Panel northPanel = new Panel();
	private Grid northLayout = GridFactory.newGridLayout();
	
	private Label sourceBankLabel = new Label();
	private Listbox sourceBankListBox = ListboxFactory.newDropdownListbox();

	private Button	refreshButton = new Button();
	
	private Listbox showModeListBox = ListboxFactory.newDropdownListbox(showMode);
	
	private Label dateFromLabel = new Label();
	private WDateEditor dateFrom = new WDateEditor("DateFrom", false, false, true, "DateFrom");
	
	private Label dateToLabel = new Label();
	private WDateEditor dateTo = new WDateEditor("DateTo", false, false, true, "DateTo");
	
	private Label bPartnerLabel = new Label();
	private WSearchEditor bPartnerSearch = null;
	
	private Panel centerPanel = new Panel();
	private Borderlayout centerLayout = new Borderlayout();
	
	private Tabbox paymentsTabs = new Tabbox();
	private Tab payablesTab = new Tab();
	private Tab receivablesTab = new Tab();
	private Tabpanel payablesTabPanel = new Tabpanel();
	private Borderlayout payablesLayout = new Borderlayout();
	private Tabpanel receivablesTabPanel = new Tabpanel();
	
	private WListbox payablesTable = ListboxFactory.newDataTable();
	
	private Panel southPanel = new Panel();
	private Grid southLayout = GridFactory.newGridLayout();
	
	private Label sumSelectedLabel = new Label();
	private WNumberEditor sumSelected = new WNumberEditor("sumSelected", false, true, false, DisplayType.Amount, "sumSelected");
	
	private Label countSelectedLabel = new Label();
	private WNumberEditor countSelected = new WNumberEditor("countSelected", false, true, false, DisplayType.Quantity, "countSelected");

	private Label sumAllLabel = new Label();
	private WNumberEditor sumAll = new WNumberEditor("sumAll", false, true, false, DisplayType.Amount, "sumAll");

	private Label countAllLabel = new Label();
	private WNumberEditor countAll = new WNumberEditor("countAll", false, true, false, DisplayType.Quantity, "countAll");
	
	private Button downloadExcelButton = new Button();
	
	private Button downloadLbFileButton = new Button();
	
	private ListModelTable payablesModel = new ListModelTable();
	private List<LbPaymentRow> payablesList = new ArrayList<LbPaymentRow>();
	
	@Override
	public ADForm getForm() {
		return form;
	}

	private void zkInit() throws Exception {
		
		form.appendChild(mainPanel);
		mainPanel.setStyle("width: 99%; height: 100%; padding: 0; margin: 0");
		mainPanel.appendChild(mainLayout);

		mainLayout.setWidth("100%");
		mainLayout.setHeight("100%");

		northPanel.appendChild(northLayout);
		
		sourceBankLabel.setText(Msg.translate(Env.getCtx(), "Source Bank Account"));
		dateFromLabel.setText(Msg.translate(Env.getCtx(), "DateFrom"));
		dateToLabel.setText(Msg.translate(Env.getCtx(), "DateTo"));
		bPartnerLabel.setText(Msg.translate(Env.getCtx(), "C_BPartner_ID"));
		sumSelectedLabel.setText(Msg.translate(Env.getCtx(), "Sum selected"));
		countSelectedLabel.setText(Msg.translate(Env.getCtx(), "# Selected"));
		sumAllLabel.setText(Msg.translate(Env.getCtx(), "Sum all"));
		countAllLabel.setText(Msg.translate(Env.getCtx(), "# All"));
		payablesTab.setLabel("Payables");
		receivablesTab.setLabel("Receivables");
		
		refreshButton.setLabel("Refresh");

		North north = new North();
		mainLayout.appendChild(north);
		north.appendChild(northPanel);
		
		Rows rows = northLayout.newRows();
		Row row = rows.newRow();
		
		row.appendChild(sourceBankLabel.rightAlign());
		row.appendChild(sourceBankListBox);
		row.appendChild(refreshButton);
		row.appendChild(new Space());
		row.appendChild(new Space());
		row.appendChild(new Space());
		row.appendChild(new Space());
		
		row = rows.newRow();
		row.appendChild(showModeListBox);
		row.appendChild(dateFromLabel);
		row.appendChild(dateFrom.getComponent());
		row.appendChild(dateToLabel);
		row.appendChild(dateTo.getComponent());
		row.appendChild(bPartnerLabel);
		row.appendChild(bPartnerSearch.getComponent());

		centerPanel.appendChild(centerLayout);
		
		Center center = new Center();
		mainLayout.appendChild(center);
		
		// TAB
		paymentsTabs.setParent(center);
		paymentsTabs.setHeight("100%");
		Tabs tabs = new Tabs();
		tabs.setWidth("100%");
		paymentsTabs.appendChild(tabs);
		tabs.appendChild(payablesTab);
		tabs.appendChild(receivablesTab);
		Tabpanels tpanels = new Tabpanels();
		paymentsTabs.appendChild(tpanels);
		tpanels.appendChild(payablesTabPanel);

		payablesTabPanel.appendChild(payablesLayout);
		center = new Center();
		payablesLayout.appendChild(center);
		center.appendChild(payablesTable);

		tpanels.appendChild(receivablesTabPanel);
		
		South south = new South();
		mainLayout.appendChild(south);
		southPanel.appendChild(southLayout);
		
		south.appendChild(southPanel);
		
		rows = southLayout.newRows();
		row = rows.newRow();
		
		row.appendChild(sumSelectedLabel);
		row.appendChild(sumSelected.getComponent());
		row.appendChild(countSelectedLabel);
		row.appendChild(countSelected.getComponent());
		row.appendChild(sumAllLabel);
		row.appendChild(sumAll.getComponent());
		row.appendChild(countAllLabel);
		row.appendChild(countAll.getComponent());
		
		downloadExcelButton.setLabel("Download Excel");
		downloadExcelButton.addActionListener(new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				createAndDownloadExcel();
			}
			
		});
		downloadLbFileButton.setLabel("Download LB-file");
		
		
	}
	
	private void dynInit() {
		
		ColumnInfo[] payablesCol = new ColumnInfo[] {

				new ColumnInfo(" ",                             ".", IDColumn.class, false, false, ""),
				new ColumnInfo("Pay Status", ".", String.class),
				new ColumnInfo(Msg.translate(Env.getCtx(), "C_BPartner_ID"), ".", KeyNamePair.class, "."),
				new ColumnInfo(Msg.translate(Env.getCtx(), "C_Invoice_ID"), ".", KeyNamePair.class, "."),
				new ColumnInfo("OCR / Ref", ".", String.class),
				new ColumnInfo("Dst acct", ".", String.class),
				new ColumnInfo("Invoice Date", ".", Timestamp.class),
				new ColumnInfo("Due Date", ".", Timestamp.class),
				new ColumnInfo("Pay Date", ".", Timestamp.class),
				new ColumnInfo("Amount due", ".", Double.class),
				new ColumnInfo("Payment amount", ".", Double.class),
				new ColumnInfo("Currency", ".", String.class),
				new ColumnInfo("Prod date", ".", Timestamp.class)
				
		};
		
		List<String> columnNames = new ArrayList<String>();
		for (ColumnInfo i : payablesCol) {
			columnNames.add(i.getColHeader());
		}
		
		
		KeyNamePair kp;
		
		List<BankInfo> bi = lbManagement.readBankAccounts();
		for (BankInfo i : bi) {
			kp = new KeyNamePair(i.C_BankAccount_ID, i.toString());
			sourceBankListBox.addItem(kp);			
		}
		
		lbManagement.loadSelection("Z", 0, null, null, 1, null, payablesList);
		
		List<List<Object>> listToRender = new ArrayList<List<Object>>();
		for (LbPaymentRow r : payablesList) {
			listToRender.add(LbPaymentRowToList(r));
		}
		
		payablesModel.addAll(listToRender);
		payablesTable.setData(payablesModel, columnNames);
		payablesTable.setColumnReadOnly(0, false);
		payablesTable.setColumnReadOnly(8, false);
		payablesTable.setColumnReadOnly(10, false);
		
		WLBListItemRenderer itemRenderer = new WLBListItemRenderer(columnNames);
		
		payablesTable.setItemRenderer(itemRenderer);

		// Listen to value changes
		itemRenderer.addTableValueChangeListener(new TableValueChangeListener() {

			@Override
			public void tableValueChange(TableValueChangeEvent event) {
				if (event.getColumn()==0) {
					updateSumSelected();
				}
			}
			
		});
		
	}
	
	/**
	 * Convert an LbPaymentRow to a list (corresponding to columns in table)
	 * 
	 * @param r
	 * @return
	 */
	private List<Object> LbPaymentRowToList(LbPaymentRow r) {
		List<Object> result = new ArrayList<Object>();

		result.add(new Boolean(false));
		result.add(new Integer(r.payStatus));
		result.add(new KeyNamePair(r.getBpartner().get_ID(), r.getBpartner().getName()));
		result.add(new KeyNamePair(r.getInvoice().get_ID(), r.getInvoice().getDocumentNo()));
		result.add("OCR");
		result.add(r.dstAcct);
		result.add(r.getInvoice().getDateInvoiced());
		result.add(r.getInvoice().get_Value("paydatebg"));
		result.add(r.payDate);
		result.add(r.getInvoice().getOpenAmt());
		result.add(r.payAmount);
		result.add(r.currency);
		result.add(null);
		
		return result;
	}
	
	public void dispose() {
		SessionManager.getAppDesktop().closeActiveWindow();
	}
	
	protected void updateSumSelected() {
		
		System.out.println("Update sum selected");
		
	}

	protected void createAndDownloadExcel() {

		
		
	}
	
}
