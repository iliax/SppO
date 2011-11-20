/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package sppo;

import gui.MainForm;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

/**
 *
 * @author iliax
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                    try {
                        UIManager.setLookAndFeel(info.getClassName());
                    } catch (Exception ex) {}
                break;
            }
        }

        MainForm.main(args);
    }

    public static void clearJTable(JTable table){
        for(int i=0; i<table.getRowCount(); i++)
            for(int j=0; j< table.getColumnCount(); j++)
                table.setValueAt(null, i, j);
    }

}
