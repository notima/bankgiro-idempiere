DROP FUNCTION IF EXISTS pg_temp.activatePlugin();
CREATE FUNCTION pg_temp.activatePlugin() returns void AS
$BODY$

DECLARE
	elementId	numeric(10);
	tableId		numeric(10);

BEGIN	

-- ELEMENT ID
select max(ad_element_id) into elementId
	FROM AD_Element WHERE columnname='PaymentFileCreatorClass' AND EntityType='LB'
	
IF elementId > 0 THEN
	INSERT INTO AD_Element (ad_element_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby,
				columnname, 
				entitytype, name, printname, ad_reference_id, fieldlength) values 
				(nextval('ad_element_id'), 0, 0, 'Y', now(), 100, now(), 'PaymentFileCreatorClass', 'LB', 'Payment File Creator Class', 'Payment File Creator Class',
				10, 255);
	select max(ad_element_id) into elementId FROM AD_Element;
END IF;

-- COLUMN
SELECT AD_Table_ID into tableId
	FROM AD_Table WHERE tablename ilike 'XC_LBPlugin';
	
-- INSERT INTO AD_Column

END;
$BODY$
language plpgsql;

select pg_temp.activatePlugin();