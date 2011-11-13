package logic;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.JTable;
import sppo.Main;

/**
 *
 * @author iliax
 */

 
public class TSIManager {
    
    private Map<String, Integer> TSI;
    private Map<String , Boolean> TSI_bools;
    private JTable tsitable;

    public TSIManager() {
        TSI=new HashMap<String, Integer>();
        TSI_bools=new HashMap<String, Boolean>();
    }

    public TSIManager(JTable tsitable) {
        this.tsitable = tsitable;
        TSI=new HashMap<String, Integer>();
        TSI_bools=new HashMap<String, Boolean>();
    }


    public void addToTSI(String name, Integer adr) throws RepeatedLabelException{
        if(TSI.get(name)!=null)
            throw new RepeatedLabelException();
        else{
            TSI.put(name, adr);
            TSI_bools.put(name, false);
        }
    }

    public void addToTSI(String name, Integer adr, boolean bool) throws RepeatedLabelException{
        if(TSI.get(name)!=null)
            throw new RepeatedLabelException();
        else {
            TSI.put(name, adr);
            TSI_bools.put(name, bool);
        }
    }

    public Integer getLabelsAddress(String lbl){
        Integer res = TSI.get(lbl);
//        if(res == null && lbl.startsWith("[")){
//            return TSI.get(lbl.substring(1, lbl.length()-1));
//        }
        return res;
    }

    public boolean isLblExternal(String str){
        if(TSI_bools.get(str)!=null)
            return TSI_bools.get(str);
        else
            return false;
    }
    
    public void showWithTable(JTable table){
        table.removeAll();
        table.repaint();

        System.out.println("TSI SIZE= "+TSI.size());

        int i = 0;
        for(Map.Entry<String, Integer> entry : TSI.entrySet()){
            table.setValueAt(entry.getKey(), i, 0);
            if(entry.getValue() == -1)
                table.setValueAt("init it!", i, 1);
            else
                table.setValueAt(MainProcessor.toHexStr(entry.getValue()), i, 1);
            table.setValueAt(isLblExternal(entry.getKey()) ? "*" : "" , i, 2);
            i++;
        }
    }

    public void repaintTSITable(){
        if(tsitable!=null)
            showWithTable(tsitable);
        else
            throw  new RuntimeException("ololo");
    }

    public void clear() {
        TSI.clear();
        TSI_bools.clear();
        if(tsitable!=null)
            Main.clearJTable(tsitable);
    }

    public void addAddressToLbl(String lbl, int ip) {
        TSI.remove(lbl);
        TSI.put(lbl, ip);
        
    }

    public class RepeatedLabelException extends RuntimeException {
        // внутренний, чтоб классы новые особо не плодить + ошибка тематическая -> есть связьность с TSIManagerom
    }


    public Set<String> getLabelsSet(){
        return TSI.keySet();
    }

}
