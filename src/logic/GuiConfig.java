package logic;

import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 *
 * @author iliax
 */
public class GuiConfig {
    
    public JTable TSITable, TKOTable, AdditionalTable, SourceTable ;

    public JTextArea firstScanErrors, secondScanErrors;

    public JTextField header;

    public GuiConfig(JTable TSITable, JTable TKOTable, JTable AdditionalTable, JTable SourceTable, JTextArea FirstScanErrors, JTextArea SecondScanErrors, JTextField header) {
        this.TSITable = TSITable;
        this.TKOTable = TKOTable;
        this.AdditionalTable = AdditionalTable;
        this.SourceTable = SourceTable;
        this.firstScanErrors = FirstScanErrors;
        this.secondScanErrors = SecondScanErrors;
        this.header = header;
    }


    
}
