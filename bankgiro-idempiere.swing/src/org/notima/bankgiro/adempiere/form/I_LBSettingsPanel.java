/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.notima.bankgiro.adempiere.form;

import java.util.SortedMap;

import org.notima.bankgiro.adempiere.model.MLBSettings;

/**
 *
 * @author Daniel Tamm
 */
public interface I_LBSettingsPanel {

    /**
     * Get values from UI-controls
     */
    public void getForm();

    /**
     * Sets values to UI-controls from DB-settings
     */
    public void setForm();

    /**
     * This must be called when the panel is initialized
     * 
     * @param settings
     */
    public void initPanel(XX_LBSettingsDialog parent, SortedMap<String, MLBSettings> settings);
    
    public String getTitle();

    public String getTip();

}
