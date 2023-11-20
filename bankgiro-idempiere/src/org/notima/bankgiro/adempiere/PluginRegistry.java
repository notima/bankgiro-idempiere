package org.notima.bankgiro.adempiere;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

import org.notima.bankgiro.adempiere.model.MLBSettings;

/**
 * Class to keep track of the payment plugins.
 * 
 * @author daniel.tamm
 *
 */
public class PluginRegistry {

	public static PluginRegistry registry = new PluginRegistry();
	
	private SortedMap<String, PaymentFileFactory>	fileFactories;
	
	private SortedMap<String, MLBSettings> lbSettings; // Store LB-settings for user
	
	public PluginRegistry() {
		
		fileFactories = new TreeMap<String, PaymentFileFactory>();
		// Add default factories
		PaymentFileFactory ff = new LbFileFactory();
		fileFactories.put(ff.getKey(),ff);
		
		ff = new XmlFileFactory();
		fileFactories.put(ff.getKey(), ff);

		ff = new XlsFileFactory();
		fileFactories.put(ff.getKey(), ff);
		
		// Init lb Settings
        // Read LB settings
        lbSettings = new TreeMap<String, MLBSettings>();
        MLBSettings.putSettings(lbSettings);
		
	}
	
	
	
	public void addPaymentFileFactory(PaymentFileFactory ff) {
		if (ff!=null)
			fileFactories.put(ff.getKey(), ff);
	}

	/**
	 * Returns file factory with given key
	 * 
	 * @param key
	 * @return
	 */
	public PaymentFileFactory getFileFactory(String key) {
		return fileFactories.get(key);
	}
	
	public Collection<PaymentFileFactory> getFileFactories() {
		return fileFactories.values();
	}

	public SortedMap<String, MLBSettings> getLbSettings() {
		return lbSettings;
	}

	public void setLbSettings(SortedMap<String, MLBSettings> lbSettings) {
		this.lbSettings = lbSettings;
	}
	
	
}
