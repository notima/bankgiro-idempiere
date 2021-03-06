/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * SelectPayStatusDialog.java
 *
 * Created on 2009-mar-10, 14:49:13
 */

package org.notima.bankgiro.adempiere.form;

import javax.swing.JOptionPane;

import org.notima.bankgiro.adempiere.LbPaymentRow;

/**
 *
 * @author Daniel Tamm
 */
public class SelectPayStatusDialog extends javax.swing.JDialog {

    private int m_retVal = JOptionPane.CANCEL_OPTION;

    /** Creates new form SelectPayStatusDialog */
    public SelectPayStatusDialog(java.awt.Frame parent, boolean modal, int initialSelection) {
        super(parent, modal);
        initComponents();
        switch(initialSelection) {
            case LbPaymentRow.PAYSTATUS_PAID: paidRadio.setSelected(true); break;
            case LbPaymentRow.PAYSTATUS_INTRANSIT: inTransitRadio.setSelected(true); break;
            case LbPaymentRow.PAYSTATUS_NOTPAID: notPaidRadio.setSelected(true); break;
        }
    }

    public int getRetVal() {
        return m_retVal;
    }

    /**
     *
     * @return
     */
    public int getPaymentStatus() {
        if (paidRadio.isSelected()) return LbPaymentRow.PAYSTATUS_PAID;
        if (inTransitRadio.isSelected()) return LbPaymentRow.PAYSTATUS_INTRANSIT;
        if (notPaidRadio.isSelected()) return LbPaymentRow.PAYSTATUS_NOTPAID;
        return(LbPaymentRow.PAYSTATUS_ANY);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup = new javax.swing.ButtonGroup();
        mainPanel = new javax.swing.JPanel();
        paidRadio = new javax.swing.JRadioButton();
        inTransitRadio = new javax.swing.JRadioButton();
        notPaidRadio = new javax.swing.JRadioButton();
        bottomPanel = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Set pay status");

        mainPanel.setLayout(new javax.swing.BoxLayout(mainPanel, javax.swing.BoxLayout.PAGE_AXIS));

        buttonGroup.add(paidRadio);
        paidRadio.setText("Paid");
        paidRadio.setAlignmentX(0.5F);
        paidRadio.setEnabled(false);
        paidRadio.setMaximumSize(new java.awt.Dimension(100, 23));
        paidRadio.setMinimumSize(new java.awt.Dimension(100, 23));
        paidRadio.setPreferredSize(new java.awt.Dimension(100, 23));
        mainPanel.add(paidRadio);

        buttonGroup.add(inTransitRadio);
        inTransitRadio.setText("In transit");
        inTransitRadio.setAlignmentX(0.5F);
        inTransitRadio.setMaximumSize(new java.awt.Dimension(100, 23));
        inTransitRadio.setMinimumSize(new java.awt.Dimension(100, 23));
        inTransitRadio.setPreferredSize(new java.awt.Dimension(100, 23));
        mainPanel.add(inTransitRadio);

        buttonGroup.add(notPaidRadio);
        notPaidRadio.setText("Not paid");
        notPaidRadio.setAlignmentX(0.5F);
        notPaidRadio.setMaximumSize(new java.awt.Dimension(100, 23));
        notPaidRadio.setMinimumSize(new java.awt.Dimension(100, 23));
        notPaidRadio.setPreferredSize(new java.awt.Dimension(100, 23));
        mainPanel.add(notPaidRadio);

        getContentPane().add(mainPanel, java.awt.BorderLayout.PAGE_START);

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        bottomPanel.add(okButton);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        bottomPanel.add(cancelButton);

        getContentPane().add(bottomPanel, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        m_retVal = JOptionPane.OK_OPTION;
        dispose();
        setVisible(false);
    }//GEN-LAST:event_okButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        m_retVal = JOptionPane.CANCEL_OPTION;
        dispose();
        setVisible(false);
    }//GEN-LAST:event_cancelButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel bottomPanel;
    private javax.swing.ButtonGroup buttonGroup;
    private javax.swing.JButton cancelButton;
    private javax.swing.JRadioButton inTransitRadio;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JRadioButton notPaidRadio;
    private javax.swing.JButton okButton;
    private javax.swing.JRadioButton paidRadio;
    // End of variables declaration//GEN-END:variables

}
