package logic;

import java.util.HashMap;
import java.util.Map;
import javax.swing.JTable;

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

    public Integer getLabelsAdress(String lbl){
        return TSI.get(lbl);
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
    }

    public class RepeatedLabelException extends RuntimeException {
        // внутренний, чтоб классы новые особо не плодить + ошибка тематическая -> есть связьность с TSIManagerom
    }


}
