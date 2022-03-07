-- ﻿DROP FUNCTION IF EXISTS pg_temp.activatePlugin();
CREATE FUNCTION pg_temp.activatePlugin() returns void AS
$BODY$

DECLARE
	pluginId	numeric(10);

BEGIN	
	
select max(xc_lbplugin_id) into pluginId
	FROM XC_LBPlugin WHERE classname like '%Iso20022%' or classname_headless like '%Iso20022%';

IF pluginId > 0 THEN
	update XC_LBPlugin set 
		classname='org.notima.idempiere.iso20022.ui.swing.Iso20022PaymentPlugin',
		classname_headless='org.notima.idempiere.iso20022.Iso20022HeadlessPlugin',
		paymentfilecreatorclass='org.notima.idempiere.iso20022.Iso20022FileFactory'
		where XC_LBPlugin_ID=pluginId;

ELSE

	insert into XC_LBPlugin (xc_lbplugin_id, ad_client_id, ad_org_id, createdby, updatedby,
		isactive, name, classname_headless, paymentfilecreatorclass, classname) values 
		(nextval('xc_lbplugin_seq'), 1000000, 0, 100, 100,'Y','ISO20022 Payments', 
			'org.notima.idempiere.iso20022.Iso20022HeadlessPlugin',
			'org.notima.idempiere.iso20022.Iso20022FileFactory',
			'org.notima.idempiere.iso20022.ui.swing.Iso20022PaymentPlugin'
			);

END IF;
END;
$BODY$
language plpgsql;

select pg_temp.activatePlugin();