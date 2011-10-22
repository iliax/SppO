package logic;

import java.util.*;
import javax.swing.*;
import logic.TKOManager.TKOItem;

/**
 *
 * @author iliax
 */
public class MainProcessor {
    
    private TKOManager tKOManager;
    private TSIManager tSIManager;

    private GuiConfig guiConfig;

    private int ip=0;
    private int programSize=0;

    private List<AdditionalTableItem> additionalTable=new ArrayList<AdditionalTableItem>();

    public MainProcessor(GuiConfig gc) {
        guiConfig=gc;

        tKOManager=new TKOManager(guiConfig.TKOTable);
        tSIManager=new TSIManager(guiConfig.TSITable);
    }

    public void processFirstScan() {
        tKOManager.reloadTKO();
        tSIManager.clear();

        getProgramStartAddress();

        for(int i=1; guiConfig.SourceTable.getValueAt(i, 1)!=null; i++){
            processLabelField(i);

            //guiConfig.AdditionalTable.setValueAt(ip, i, 0); 

            processOperation(i);
            System.out.println("ip=="+ip);
        }

        showAdditionalTable();
    }

    private void processOperation(int i){
        if(isDirective((String)guiConfig.SourceTable.getValueAt(i, 1)) != null){
            processDirective(i);
        }   else {
            TKOItem item = tKOManager.checkOperationCode((String)guiConfig.SourceTable.getValueAt(i, 1));
            if(item!=null){
                AdditionalTableItem ati = new AdditionalTableItem(ip, 
                        checkCommandOperandsAndGetCode(item.getBinaryCode(), i),
                        new String[]{(String)guiConfig.SourceTable.getValueAt(i, 2),
                            (String)guiConfig.SourceTable.getValueAt(i, 3)});

                additionalTable.add(ati);

                ip+=item.getSize();

            } else {
                print1stScanError("operation not defined! "+i);
            }
        }
    }

    private void processDirective(int i){
        Pair<Integer, Boolean>  inf
                = isDirective((String)guiConfig.SourceTable.getValueAt(i, 1));
        if(inf.get2() == true) {
            AdditionalTableItem ati=new AdditionalTableItem(ip, -1,
                    new String[]{(String )guiConfig.SourceTable.getValueAt(i, 1),
                        (String)guiConfig.SourceTable.getValueAt(i, 2)});

            additionalTable.add(ati);
            ip+=(inf.get1() * getOperandSize2(i));
            
        } else {
            
           AdditionalTableItem ati=new AdditionalTableItem(ip, -2,
                    new String[]{(String )guiConfig.SourceTable.getValueAt(i, 1),
                        (String)guiConfig.SourceTable.getValueAt(i, 2) });
            additionalTable.add(ati);
            ip+=(inf.get1() * getOperandSize(i));
        }
    }

    private int getOperandSize(int i){
        String str = (String)guiConfig.SourceTable.getValueAt(i, 2);
        if(str!=null)
            if(str.startsWith("\'"))
                return str.length()-2;
            else
                try {
                    int op = Integer.parseInt(str);
                    return op;
                } catch(Exception e){
                    print1stScanError("operand parsing error! "+i);
                    return 0;
                }
        print1stScanError("operand parsing error "+i);
        return 0;
    }

    private int getOperandSize2(int i){         // for WORD and BYTE
        String str = (String)guiConfig.SourceTable.getValueAt(i, 2);
        if(str!=null)
            if(str.startsWith("\'"))
                return str.length()-2;
            else
                try {
                    int op = Integer.parseInt(str);
                    return 1;
                } catch(Exception e){
                    print1stScanError("operand parsing error! "+i);
                    return 0;
                }
        print1stScanError("operand parsing error "+i);
        return 0;
    }

    private void processLabelField(int i){
        String lbl= (String)guiConfig.SourceTable.getValueAt(i, 0);
        if(lbl != null && !lbl.isEmpty())
            try {
                    tSIManager.addToTSI(lbl, ip);
                    System.out.println("lbl added!");
                } catch(Exception e){
                    print1stScanError("repeated Label!");
                }

        tSIManager.repaintTSITable();       //////
    }

    private void getProgramStartAddress(){
        String str = (String)guiConfig.SourceTable.getValueAt(0, 2);
        if(str!=null)
            try{
                //str="0x"+str;
                ip= Integer.parseInt(str,16);
                System.out.println("ip="+ip);
            } catch(Exception e){
                print1stScanError("start address missed!");
                ip=0;
            }
        else
            System.out.println("ip="+ip);
    }

    void print1stScanError(String err){
        guiConfig.firstScanErrors.setText(guiConfig.firstScanErrors.getText()+err+"\n");
        System.err.println(err);
    }

    private Pair<Integer, Boolean> isDirective(String str){
        if(str == null)
            return null;
        if(str.equalsIgnoreCase("RESB"))
            return new Pair<Integer, Boolean>(1, false);
        else    if(str.equalsIgnoreCase("RESW"))
            return new Pair<Integer, Boolean>(3, false);
        else    if(str.equalsIgnoreCase("BYTE"))
            return new Pair<Integer, Boolean>(1, true);
        else    if(str.equalsIgnoreCase("WORD"))
            return new Pair<Integer, Boolean>(3, true);
        else
            return null;
    }

    private void showAdditionalTable() {
        int i=0;
        for(AdditionalTableItem ati : additionalTable){
            guiConfig.AdditionalTable.setValueAt(toHexStr(ati.getAdress()), i, 0);
            if(ati.getOperationCode() > 0){
                guiConfig.AdditionalTable.setValueAt(ati.getOperationCode()+"h", i, 1);
                if(ati.getOpers().length > 0){
                    guiConfig.AdditionalTable.setValueAt(ati.getOpers()[0], i, 2);
                    if(ati.getOpers()[1] !=null)
                        guiConfig.AdditionalTable.setValueAt(ati.getOpers()[1], i, 3);
                }
            } else {
                if(ati.getOperationCode() == -1){
                    guiConfig.AdditionalTable.setValueAt(ati.getOpers()[0],i,1);
                    guiConfig.AdditionalTable.setValueAt(ati.getOpers()[1], i, 2);
                } else {
                    guiConfig.AdditionalTable.setValueAt(ati.getOpers()[0],i,1);
                }
            }
            i++;
        }
    }


    private int checkCommandOperandsAndGetCode(int binaryCode, int i) {
        String op1 = (String)guiConfig.SourceTable.getValueAt(i, 2);
       // String op2 = (String)guiConfig.SourceTable.getValueAt(i, 3);

        int adresation=1, result;
        if(op1 != null){
            if(checkRegister(op1))
                adresation=0;
        } else
            return binaryCode;

        String s = Integer.toBinaryString(binaryCode);
        s+="0";
        s+=adresation;
        result= Integer.parseInt(s,2);
        return result;
    }

    public boolean checkRegister(String s){
        if(s.equalsIgnoreCase("R1") || s.equalsIgnoreCase("R2")
                || s.equalsIgnoreCase("R3")
                || s.equalsIgnoreCase("R4")
                || s.equalsIgnoreCase("R5")
                || s.equalsIgnoreCase("R6")
                || s.equalsIgnoreCase("R7")
        )
            return true;
        else
            return false;
    }

    public static String toHexStr(Object obj) {
        if (obj == null )
            return null;
        if(obj instanceof Integer){
            int num= (Integer)obj;
            String s=Integer.toHexString(num);
            if(s.length()<6){
                int len=s.length();
                for(int i=0; i<(6-len); i++)
                    s="0"+s;
            }
            return s;
        }
        return null;
    }

    private class Pair<A,B>{
        private A a;
        private B b;
        public Pair(A _a, B _b){ a=_a;  b=_b; }
        public A getA(){ return a; }
        public B getB(){ return b; }
        public A get1(){ return a; }
        public B get2(){ return b; }
    }

    public class AdditionalTableItem {
        private int adress;
        private int binaryOperationCode;
        private String operands[];
        
        public AdditionalTableItem(int adr, int binCode, String opers[]){
            adress=adr;
            binaryOperationCode=binCode;
            operands=opers;
        }

        public String [] getOpers(){
            return operands;
        }

        public int getAdress(){
            return adress;
        }

        public int getOperationCode(){
            return binaryOperationCode;
        }
    }
}
