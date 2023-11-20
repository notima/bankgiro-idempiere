update ad_form set classname = 'org.notima.bankgiro.adempiere.form.VLBManagement' 
	where classname = 'se.compiere.bg.VLBManagement';
	
update ad_entitytype set modelpackage='org.notima.bankgiro.adempiere.model' where entitytype='LB';

update ad_table set entitytype='LB' where tablename like 'XB_LB%';

update xc_lbplugin set classname='org.notima.bankgiro.adempiere.LbPaymentPlugin' where classname='se.adempiere.payment.LbPaymentPlugin';
update xc_lbplugin set classname='org.notima.bankgiro.adempiere.BgPaymentPlugin' where classname='se.adempiere.payment.BgPaymentPlugin';
update xc_lbplugin set classname='se.cyberphoto.kreditor.ui.swing.KreditorPaymentPlugin' where classname='se.cyberphoto.kreditor.KreditorPaymentPlugin';
update xc_lbplugin set classname='se.cyberphoto.svea.ui.swing.SveaPaymentPlugin' where classname='se.cyberphoto.svea.SveaPaymentPlugin';
update xc_lbplugin set classname='se.cyberphoto.paypal.ui.swing.PaypalPaymentPlugin' where classname='se.cyberphoto.paypal.PaypalPaymentPlugin';
update xc_lbplugin set classname='se.cyberphoto.dibs.ui.swing.DibsPaymentPlugin' where classname='se.cyberphoto.dibs.DibsPaymentPlugin';

update ad_column set callout = replace(callout, 'org.adempiere.model.XX_CalloutInvoice', 'org.notima.bankgiro.adempiere.model.XX_CalloutInvoice') where callout like '%org.adempiere.model.XX_CalloutInvoice%';
update ad_column set callout = replace(callout, 'org.adempiere.model.XX_CalloutBGPayment', 'org.notima.bankgiro.adempiere.model.XX_CalloutBGPayment') where callout like '%org.adempiere.model.XX_CalloutBGPayment%';

update ad_modelvalidator set modelvalidationclass='org.notima.bankgiro.adempiere.BGPaymentModelValidator', entitytype='LB' where modelvalidationclass='se.compiere.bg.BGPaymentModelValidator';
update ad_modelvalidator set modelvalidationclass='org.notima.bankgiro.adempiere.BGInvoiceModelValidator', entitytype='LB' where modelvalidationclass='se.compiere.bg.BGInvoiceModelValidator';