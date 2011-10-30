package logic;

import java.util.HashMap;
import java.util.Map;
import javax.swing.JTable;
import sppo.Main;

/**
 *
 * @author iliax
 */

 
public class TSIManager {
    
    private Map<String, Integer> TSI;
    private JTable tsitable;

    public TSIManager() {
        TSI=new HashMap<String, Integer>();
    }

    public TSIManager(Map<String, Integer> TSI) {
        this.TSI = TSI;
    }

    public TSIManager(JTable tsitable) {
        this.tsitable = tsitable;
        TSI=new HashMap<String, Integer>();
    }



    public void setTSI(Map<String, Integer> TSI) {
        this.TSI = TSI;
    }


    public void addToTSI(String name, Integer adr) throws RepeatedLabelException{
        if(TSI.get(name)!=null)
            throw new RepeatedLabelException();
        else
            TSI.put(name, adr);
    }

    public Integer getLabelsAddress(String lbl){
        Integer res = TSI.get(lbl);
//        if(res == null && lbl.startsWith("[")){
//            return TSI.get(lbl.substring(1, lbl.length()-1));
//        }
        return res;
    }

    public void showWithTable(JTable table){
        table.removeAll();
        table.repaint();

        System.out.println("TSI SIZE= "+TSI.size());

        int i = 0;
        for(Map.Entry<String, Integer> entry : TSI.entrySet()){
            table.setValueAt(entry.getKey(), i, 0);
            table.setValueAt(MainProcessor.toHexStr(entry.getValue()), i, 1);
            i++;
        }
    }

    public void repaintTSITable(){
        if(tsitable!=null)
            showWithTable(tsitable);
        else
            throw  new RuntimeException("ololo");
    }

    void clear() {
        TSI.clear();
        if(tsitable!=null)
            Main.clearJTable(tsitable);
    }

    public class RepeatedLabelException extends RuntimeException {
        // внутренний, чтоб классы новые особо не плодить + ошибка тематическая -> есть связьность с TSIManagerom
    }


}
