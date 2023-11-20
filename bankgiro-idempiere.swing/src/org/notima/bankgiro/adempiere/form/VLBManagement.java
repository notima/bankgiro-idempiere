/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.notima.bankgiro.adempiere.form;

import java.awt.BorderLayout;
import java.awt.Dimension;

import org.compiere.apps.form.FormFrame;
import org.compiere.apps.form.FormPanel;
import org.compiere.swing.CPanel;

/**
 *
 * @author Daniel Tamm
 */
public class VLBManagement extends CPanel implements FormPanel {

    private int         m_WindowNo;
    private FormFrame   m_frame;
    private VLBManagementPanel  m_panel;

    @Override
    public void init(int WindowNo, FormFrame frame) {

        m_WindowNo = WindowNo;
        m_frame = frame;
        m_panel = new VLBManagementPanel(this);
        // Add panes.
        // =====================
        m_frame.getContentPane().setPreferredSize(new Dimension(1400, 800));
        m_frame.getContentPane().add(m_panel, BorderLayout.CENTER);
    }

    public int getWindowNo() {
        return(m_WindowNo);
    }

    public FormFrame    getFrame() {
        return(m_frame);
    }

    @Override
    public void dispose() {
		if (m_frame != null)
			m_frame.dispose();
		m_frame = null;
    }

}
