/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.notima.bankgiro.adempiere;

/**
 * Interface used to update sums in presentation layers.
 *
 * @author Daniel Tamm
 */
public interface SumUpdate {

    public void updateSums(double selectedAmount, int selectedCount, double allAmount, int allCount);

}
