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
    
    public Map<String, Integer> TSI;
    private JTable tsitable;
  private Map<String , Boolean> TSI_bools;
    public TSIManager() {
        TSI=new HashMap<String, Integer>();  TSI_bools=new HashMap<String, Boolean>();
    }

    public TSIManager(Map<String, Integer> TSI) {
        this.TSI = TSI;
    }

    public TSIManager(JTable tsitable) {
        this.tsitable = tsitable;
        TSI=new HashMap<String, Integer>();  TSI_bools=new HashMap<String, Boolean>();
    }



    public void setTSI(Map<String, Integer> TSI) {
        this.TSI = TSI;
    }


    public void addToTSI(String name, Integer adr){

        if(TSI.get(name)!=null){
                TSI.remove(name);
                //TSI_bools.remove(name);

                TSI.put(name, adr);
        }
        else{
            TSI.put(name, adr);
            TSI_bools.put(name, false);
        }

            showWithTable(tsitable);
    }
   public boolean isLblExternal(String str){
        if(TSI_bools.get(str)!=null)
            return TSI_bools.get(str);
        else
            return false;
    }
    public Integer getLabelsAddress(String lbl){
        return TSI.get(lbl);
    }

    public void showWithTable(JTable table){
        table.removeAll();
        table.repaint();

        System.out.println("TSI SIZE= "+TSI.size());

        int i = 0;
        for(Map.Entry<String, Integer> entry : TSI.entrySet()){
            
            table.setValueAt(entry.getKey(), i, 0);
            if(entry.getValue() == -1 )
                table.setValueAt("no address", i, 1);
            else{
                if(isLblExternal(entry.getKey()))
                    table.setValueAt(MainProcessor.toHexStr(entry.getValue())+" *", i, 1);
                    else
                    table.setValueAt(MainProcessor.toHexStr(entry.getValue()), i, 1);
            }
            i++;
        }
    }


//       public void showWithTable(JTable table){
//        table.removeAll();
//        table.repaint();
//
//        System.out.println("TSI SIZE= "+TSI.size());
//
//        int i = 0;
//        for(Map.Entry<String, Integer> entry : TSI.entrySet()){
//            table.setValueAt(entry.getKey(), i, 0);
//            if(entry.getValue() == -1)
//                table.setValueAt("init it!", i, 1);
//            else
//                table.setValueAt(MainProcessor.toHexStr(entry.getValue()), i, 1);
//            table.setValueAt(isLblExternal(entry.getKey()) ? "*" : "" , i, 2);
//            i++;
//        }
//    }

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
    public void addToTSI(String name, Integer adr, boolean bool) throws RepeatedLabelException{
        if(bool==true && TSI.get(name)!=null){
            if(TSI_bools.get(name) == false){
                TSI_bools.remove(name);
                TSI_bools.put(name, true);
            } else {
                throw new RepeatedLabelException();
            }
            return;
        }

        if(TSI.get(name)!=null){
            throw new RepeatedLabelException();
        }
        else {
            TSI.put(name, adr);
            TSI_bools.put(name, bool);
        }
    }

}
