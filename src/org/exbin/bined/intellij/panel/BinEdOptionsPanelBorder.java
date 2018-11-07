/*
 * Copyright (C) ExBin Project
 *
 * This application or library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This application or library is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along this application.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.exbin.bined.intellij.panel;

import org.exbin.bined.swing.extended.ExtCodeArea;

/**
 * Hexadecimal editor options panel with border.
 *
 * @version 0.1.5 2017/09/30
 * @author ExBin Project (http://exbin.org)
 */
public class BinEdOptionsPanelBorder extends javax.swing.JPanel {

    public BinEdOptionsPanelBorder() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        optionsPanel = new BinEdOptionsPanel();

        setLayout(new java.awt.BorderLayout());

        optionsPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(optionsPanel, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private BinEdOptionsPanel optionsPanel;
    // End of variables declaration//GEN-END:variables

    public void setFromCodeArea(ExtCodeArea codeArea) {
        optionsPanel.setFromCodeArea(codeArea);
    }

    public void applyToCodeArea(ExtCodeArea codeArea) {
        optionsPanel.applyToCodeArea(codeArea);
        codeArea.repaint();
    }

    public boolean isShowValuesPanel() {
        return optionsPanel.isShowValuesPanel();
    }

    public void setShowValuesPanel(boolean flag) {
        optionsPanel.setShowValuesPanel(flag);
    }

    public void store() {
        optionsPanel.store();
    }

    public boolean isDeltaMemoryMode() {
        return optionsPanel.isDeltaMemoryMode();
    }
}
