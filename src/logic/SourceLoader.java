package logic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTable;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import sppo.Main;

/**
 *
 * @author iliax
 */
public class SourceLoader {

    private int index = 0;
    private String pieces[] = null;

    private void process(JTable table, String line) {
        for (int i = 0; i < (pieces = line.split(" ")).length; i++) {
            if (!pieces[i].equals("?:")) {
                table.getModel().setValueAt(pieces[i], index, i);
            }
        }
    }

    public void load(File path, JTable source, JTable tko)  {
        boolean sourceMode = true;
        index=0;

        Main.clearJTable(tko);
        Main.clearJTable(source);

        try {
            BufferedReader in = new BufferedReader(new FileReader(path));
            String line = null;
            while ((line = in.readLine()) != null) {
                if (line.equals("*****")) {
                    sourceMode = false;
                    index = 0;
                    continue;

                }
                if (sourceMode) {
                    pieces = null;
                    process(source, line);
                    index++;
                } else {
                    pieces = null;
                    process(tko, line);
                    index++;
                }

            }

        } catch (Exception ex) {
            Logger.getLogger(SourceLoader.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}