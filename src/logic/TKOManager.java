package logic;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JTable;

/**
 *
 * @author iliax
 */
public class TKOManager {

    private List<TKOItem> TKO;
    private JTable tkoTable;

    public TKOManager() {
        TKO=new ArrayList<TKOItem>(10);
    }

    public TKOManager(List<TKOItem> TKO) {
        this.TKO = TKO;
    }

    public TKOManager(JTable tkoTable) {
        this.tkoTable = tkoTable;
        TKO=new ArrayList<TKOItem>();
    }

    public void setTKO(List<TKOItem> TKO) {
        this.TKO = TKO;
    }

    public List<TKOItem> getTKO() {
        return TKO;
    }


    public TKOItem checkOperationCode(String opCode){
        for(TKOItem item: TKO)
            if(item.getOperationCode().equalsIgnoreCase(opCode))
                return item;
        return null;
    }

    public void reloadTKO(){
        if(tkoTable!=null)
            loadTKOFromTable(tkoTable);
        else
            throw new RuntimeException("ololo");
    }

    public void loadTKOFromTable(JTable table){
        TKO.clear();
        
        for(int i=0; i<table.getRowCount(); i++){
            String name;
            Integer binCode, size;

            if(table.getValueAt(i, 0)!=null){
                name = (String)table.getValueAt(i, 0);
                binCode=Integer.parseInt((String)table.getValueAt(i, 1));
                size=Integer.parseInt((String)table.getValueAt(i, 2));
                TKO.add(new TKOItem(name, binCode, size));
            }
        }

        //printTKO();
    }

    public void printTKO(){
        System.out.println("TKO:");
        for(TKOItem item: TKO)
            System.out.println(item.operationCode+" "+item.binaryCode+" "+item.size);
    }
    
    public class TKOItem {
        private String operationCode;
        private Integer binaryCode;
        private Integer size;
        
        public TKOItem(String mOperationCode, int binaryCode, int size) {
            this.operationCode = mOperationCode;
            this.binaryCode = binaryCode;
            this.size = size;
        }
        public String getOperationCode() {
            return operationCode;
        }
        public int getBinaryCode() {
            return binaryCode;
        }
        public int getSize() {
            return size;
        }
    }
}
