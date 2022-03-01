package org.notima.bankgiro.adempiere;

import java.util.Properties;

import javax.xml.bind.annotation.XmlTransient;

import org.compiere.model.MCharge;
import org.compiere.model.MPayment;
import org.compiere.model.MProduct;
import org.compiere.model.Query;

/**
 * Record for describing a payment fee. How the fee is handled is detemined by the 
 * implementation of the payment factory
 * Normally this is a transaction fee or a factoring fee.
 * 
 * @author Daniel Tamm
 *
 */
public class PaymentFeeRecord {

	@XmlTransient
	protected MProduct	product;
	protected String	productValue;
	@XmlTransient
	protected MCharge	charge;
	protected int		chargeId;
	protected double	qty;
	protected double	price;
	protected double	vatAmount;
	protected boolean	useVatAmount;
	protected boolean	feeAsPayment = false;
	protected String	description;
	@XmlTransient
	protected MPayment	feePayment;

	public PaymentFeeRecord() {}
	
	/**
	 * 
	 * @param ctx
	 * @param productValue
	 * @param chargeId
	 * @param qty
	 * @param price
	 * @param trxName
	 * @deprecated - Don't use this because lookup of product / charge should happen at a later stage,
	 * 				 in PaymentFactory.
	 */
	public PaymentFeeRecord(Properties ctx, String productValue, int chargeId, double qty, double price, String trxName) {
		if (productValue!=null && productValue.trim().length()>0) {
			product = new Query(ctx, MProduct.Table_Name, "value=?", trxName)
					.setClient_ID()
					.setParameters(new Object[]{productValue})
					.first();
		} else if (chargeId!=0) {
			charge = new Query(ctx, MCharge.Table_Name, "c_charge_id=?", trxName)
					.setClient_ID()
					.setParameters(new Object[]{chargeId})
					.first();
		}
		this.productValue = productValue;
		this.chargeId = chargeId;
		this.qty = qty;
		this.price = price;
	}

	public PaymentFeeRecord(String productValue, double qty, double price) {
		this.productValue = productValue;
		this.qty = qty;
		this.price = price;
	}
	
	public PaymentFeeRecord(int chargeId, double qty, double price) {
		this.chargeId = chargeId;
		this.qty = qty;
		this.price = price;
	}
	
	public void setCharge(MCharge charge) {
		this.charge = charge;
		if (charge!=null)
			chargeId = charge.get_ID();
	}
	
	/**
	 * If no tax is involved in the fee, it is possible to create a payment for the fee.
	 * This makes it less document intensive since we don't have to create allocations
	 * and payments for a fee invoice.
	 */
	public boolean isFeeAsPayment() {
		return feeAsPayment;
	}

	public void setFeeAsPayment(boolean feeAsPayment) {
		this.feeAsPayment = feeAsPayment;
	}

	@XmlTransient
	public MCharge getCharge() {
		return charge;
	}
	
	@XmlTransient
	public MProduct getProduct() {
		return product;
	}
	public void setProduct(MProduct product) {
		this.product = product;
	}
	public double getQty() {
		return qty;
	}
	public void setQty(double qty) {
		this.qty = qty;
	}

	/**
	 * Return total for this fee.
	 * 
	 * @return
	 */
	public double getFeeTotalIncVat() {
		double result = price * qty;
		if (useVatAmount) {
			result += (vatAmount * qty);
		}
		return result;
	}
	
	/**
	 * Price excluding VAT.
	 * @return
	 */
	public double getPrice() {
		return price;
	}
	public void setPrice(double amount) {
		this.price = amount;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * This value is used if isUseVatAmount is true.
	 * @return
	 */
	public double getVatAmount() {
		return vatAmount;
	}

	public void setVatAmount(double vatAmount) {
		this.vatAmount = vatAmount;
	}

	/**
	 * If true the vatAmount is set explicitly to the value found in
	 * vatAmount.
	 * @return
	 */
	public boolean isUseVatAmount() {
		return useVatAmount;
	}

	public void setUseVatAmount(boolean useVatAmount) {
		this.useVatAmount = useVatAmount;
	}

	public MPayment getFeePayment() {
		return feePayment;
	}

	public void setFeePayment(MPayment feePayment) {
		this.feePayment = feePayment;
	}

	public String getProductValue() {
		return productValue;
	}

	public void setProductValue(String productValue) {
		this.productValue = productValue;
	}

	public int getChargeId() {
		return chargeId;
	}

	public void setChargeId(int chargeId) {
		this.chargeId = chargeId;
	}
	
	
	
}
