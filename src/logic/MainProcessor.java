package logic;

import java.util.*;
import javax.swing.*;
import logic.TKOManager.TKOItem;
import sppo.Main;

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
    private int startAddress = 0;

    public MainProcessor(GuiConfig gc) {
        guiConfig=gc;

        tKOManager=new TKOManager(guiConfig.TKOTable);
        tSIManager=new TSIManager(guiConfig.TSITable);
    }

    public void processSecondScan(){
        guiConfig.ObjectModuleArea.setText("");
        guiConfig.secondScanErrors.setText("");

        showHeader();

        for(int i=0; i<additionalTable.size(); i++)
            processAdditionalTableItem(i);

        showEnding();
        
    }

    private void showHeader(){
        String header="H  ";
        String progName, startAdr;
        progName=(String)guiConfig.SourceTable.getValueAt(0, 0);
        if(progName == null)
            header+="MissingProgName ";
        else
            header += (progName +" ");
        startAdr=(String)guiConfig.SourceTable.getValueAt(0, 2);
        if(startAdr == null)
            header+="000000h ";
        else
            header += toHexStr(Integer.parseInt(startAdr,16)).toString()+ "h ";

        header += Integer.toHexString(programSize) + "h\n";

        guiConfig.ObjectModuleArea.setText(header);
    }

    private void showEnding() {
        guiConfig.ObjectModuleArea.setText(guiConfig.ObjectModuleArea.getText()+"E  "+toHexStr(startAddress) + "h");
    }

    public void processFirstScan() {
        tKOManager.reloadTKO();
        guiConfig.firstScanErrors.setText("");
        tSIManager.clear();
        Main.clearJTable(guiConfig.AdditionalTable);

        getProgramStartAddress();

        for(int i=1; guiConfig.SourceTable.getValueAt(i, 1)!=null
                && !((String)guiConfig.SourceTable.getValueAt(i, 1)).equalsIgnoreCase("END"); i++){

            processLabelField(i);

            processOperation(i);
            System.out.println("ip=="+ip);
        }

        showAdditionalTable();

        programSize = ip - startAddress;
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
                            (String)guiConfig.SourceTable.getValueAt(i, 3)},
                            item.getSize()
                            );

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
        int res= _getOperandSize(str);
        if(res!= -1)
            return res;
        else
            print1stScanError("operand parsing error "+i);
        return 0;
    }

    private int _getOperandSize(String str){
        if(str!=null){
            if(str.startsWith("\'"))
                return str.length()-2;
            else
                try {
                    int op = Integer.parseInt(str);
                    return 1;
                } catch(Exception e){
                    print1stScanError("operand parsing error! "+str);
                    return 0;
                }
        }
        return -1;
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
                startAddress=ip;
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

    void print2ndScanError(String err){
        guiConfig.secondScanErrors.setText(guiConfig.secondScanErrors.getText()+err+"\n");
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
                guiConfig.AdditionalTable.setValueAt(Integer.toHexString(ati.getOperationCode())+"h", i, 1);
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
            if(checkRegister(op1) > 0)
                adresation=0;
        } else
            return binaryCode;

        String s = Integer.toBinaryString(binaryCode);
        s+="0";
        s+=adresation;
        result= Integer.parseInt(s,2);
        return result;
    }

    public int checkRegister(String s){
        if(s.startsWith("R")){
            try {
                int val= Integer.parseInt(s.substring(1));
                if(val <= 16 && val >= 1)
                    return val;
                else
                    return 0;
            } catch(Exception e){
                return  0;
            }
        }
        else
            return 0;
    }

    public static String toHexStr(Object obj) {
        if (obj == null )
            return null;
        if(obj instanceof Integer){
            String s=Integer.toHexString((Integer)obj);
            if(s.length()< 6){
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
        private int tkoOperationSize;
        
        public AdditionalTableItem(int adr, int binCode, String opers[]){
            adress=adr;
            binaryOperationCode=binCode;
            operands=opers;
        }

        public AdditionalTableItem(int adr, int binCode, String opers[], int tkoOperationCode){
            adress=adr;
            binaryOperationCode=binCode;
            operands=opers;
            this.tkoOperationSize=tkoOperationCode;
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

        public int getTkoOperationSize() {
            return tkoOperationSize;
        }

    }


    private void processAdditionalTableItem(int index){
        AdditionalTableItem ati=additionalTable.get(index);

        String body="T  ";
        body+=toHexStr(ati.adress)+"   ";
        
        if(ati.getOperationCode() == -1){       //byte or word
            if(ati.operands[0].equalsIgnoreCase("BYTE"))
                body+= Integer.toHexString(_getOperandSize(ati.operands[1])*2)+"  ";
            else
                body+= Integer.toHexString(_getOperandSize(ati.operands[1])*6)+"  ";

            body+=" "+getOperandsRealView(ati.operands[1])+" ";

        } else if(ati.getOperationCode() == -2){    //resb or resw
           ;// body+=toHexStr(ati.adress);
        } else {                                    //operation
            body+= Integer.toHexString( ati.getTkoOperationSize()*2) + "h ";
            body+= Integer.toHexString(ati.getOperationCode()) + "h ";
            try {
                body+= "  "+getOperandsInRightWay(ati.operands);
            } catch(IllegalArgumentException e){
                body="";
                print2ndScanError(e.getMessage());
            }
        }

        body+=" \n";

        guiConfig.ObjectModuleArea.setText(guiConfig.ObjectModuleArea.getText()+body);
    }

    private String getOperandsInRightWay(String strs[]){
        if(strs == null || (strs.length==0))
            return "";

        String result="";
        
        for(String s : strs){
            if (s!=null){
                if(checkRegister(s) > 0){       //register
                    result+=checkRegister(s)+" ";
                } else {
                    if(s.startsWith("'")){          //string
                        result+=s+"_h ";
                    } else {                    //maybe label?
                        if(tSIManager.getLabelsAdress(s) != null){
                            result+=toHexStr(tSIManager.getLabelsAdress(s))+" ";
                        } else {                                         //number or unregistered label
                                String test= toHexStr(s);
                                if(test!=null)
                                    result+=test;
                                else
                                    throw new IllegalArgumentException("unregistered label '" + s+"'");
                        }
                    }
                }
            }
        }
        return result;
    }

    public String getOperandsRealView(String s){
        String res="";
        
        if(s.startsWith("'")){
            for(int i=1, t; i<s.length()-1; i++ ){
                t=s.charAt(i);
                res += Integer.toHexString(t) + " ";
            }
        } else {
            try { res+=Integer.toHexString(Integer.parseInt(s)); }
            catch(Exception e){
                res+=s;
            }
        }

        return res;
    }
   
}


