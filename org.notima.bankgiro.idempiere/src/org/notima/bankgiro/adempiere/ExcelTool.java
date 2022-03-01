package org.notima.bankgiro.adempiere;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class ExcelTool {

	private int rowNum;
	private int cellNum;
	private Row	row;
	private Cell cell;
	
	private String artNr;
	private Double qty;
	private Double price;
	private Double listPrice;
	private String name;
	private String packInfo;
	private String brandName;
	private String unitName;
	private double multiplier;
	private String shipmentNo;
	private int shipLocationId;
	
	
	public File createExcelFromLbPayments(List<LbPaymentRow> rows) throws IOException {

		File f = File.createTempFile("LB", ".xlsx");
		
		int rowNum = 0;
		HSSFWorkbook wb = new HSSFWorkbook();
		Sheet sh = wb.createSheet("LB");
		
		createHeader(sh);

		// Add rows
		for (LbPaymentRow lrow : rows) {
//			createRow(sh, lrow);
		}
		
		
		return null;
	}
	
	public void createHeader(Sheet sh) {
		cellNum = 0;
		
		row = sh.createRow(rowNum++);
		cell = row.createCell(cellNum++);
		cell.setCellValue("Vårt art nr");
		cell = row.createCell(cellNum++);
		cell.setCellValue("Market art nr");
		sh.setColumnWidth(cellNum-1, 3000);
		
		cell = row.createCell(cellNum++);
		cell.setCellValue("Antal st");
		cell = row.createCell(cellNum++);
		cell.setCellValue("Pris/st");
		cell = row.createCell(cellNum++);
		cell.setCellValue("Rabatt");
		cell = row.createCell(cellNum++);
		cell.setCellValue("Lager");

	}
	
	
	
	
}
