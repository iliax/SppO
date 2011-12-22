package logic;

import java.util.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.*;
import logic.TKOManager.TKOItem;
import sppo.Main;
import sun.misc.Version;

/**
 *
 * @author iliax
 */
public class MainProcessor  implements  Runnable{
    
    private TKOManager tKOManager;
    private TSIManager tSIManager;

    private GuiConfig guiConfig;

    private int ip=0;
    public volatile int programSize=-1;

    private List<AdditionalTableItem> additionalTable=new ArrayList<AdditionalTableItem>();
    private int startAddress = 0;

    public AtomicBoolean atomicBoolean=new AtomicBoolean(true);

    Map<String , List<Integer>> labelNamesLists = new HashMap<String, List<Integer>>();
    private boolean stepMode;

    List<Integer> tuneTableList = new ArrayList<Integer>();

    class ShowItem {
        public List<String> items = new ArrayList<String>();

        public void add(String str){
            items.add(str);
        }
    }

    List<ShowItem> showItems =new ArrayList<ShowItem>();

    public MainProcessor(GuiConfig gc, AtomicBoolean ab) {
        guiConfig=gc;
        atomicBoolean=ab;
        if(atomicBoolean.get() == false)
            stepMode=true;

        tKOManager=new TKOManager(guiConfig.TKOTable);
        tSIManager=new TSIManager(guiConfig.TSITable);
    }


    void doCleaningStaff(){
        tKOManager.reloadTKO();
        guiConfig.firstScanErrors.setText("");
        tSIManager.clear();
        Main.clearJTable(guiConfig.AdditionalTable);
    }

    @Override
    public void run() {
        processMainScan();
    }

    public void processMainScan(){
        doCleaningStaff();

        //getProgramStartAddress();    //maybe nessesary...xz

         for(int i=1; guiConfig.SourceTable.getValueAt(i, 1)!=null && !((String)guiConfig.SourceTable.getValueAt(i, 1)).equalsIgnoreCase("END"); i++){
             processLabelField(i);

             tryLock();

            if(stepMode)
                atomicBoolean.set(false);
             
             ShowItem newShowItem = processOperation_(i);
             
             if(newShowItem != null)
                 showItems.add(newShowItem);
            

             if(!guiConfig.firstScanErrors.getText().trim().isEmpty()){
                     programSize=0;
                     return;
             }

             showShowItems();

             tryLock();
             
             if(stepMode)
                atomicBoolean.set(false);

         }
        programSize = ip - startAddress;
        showShowItems();
    }

    void tryLock(){
         while(atomicBoolean.get()==false){
            synchronized(guiConfig){
                try {
                    guiConfig.wait();
                } catch (InterruptedException ex) {
                    Logger.getLogger(MainProcessor.class.getName()).log(Level.SEVERE, null, ex);
                }
             }
         }
    }


    void showShowItems(){
         guiConfig.ObjectModuleArea.setText("");

         if(programSize != -1)
            guiConfig.ObjectModuleArea.setText(guiConfig.ObjectModuleArea.getText()+showHeader());

         for(ShowItem si: showItems){
             guiConfig.ObjectModuleArea.setText(guiConfig.ObjectModuleArea.getText()+"T  ");
             for(String s : si.items)
                 guiConfig.ObjectModuleArea.setText(guiConfig.ObjectModuleArea.getText()+s+"  ");
             guiConfig.ObjectModuleArea.setText(guiConfig.ObjectModuleArea.getText()+"\n");
         }

         if(programSize != -1){
             for(Integer mod : tuneTableList)
                 guiConfig.ObjectModuleArea.setText(guiConfig.ObjectModuleArea.getText()+"M  "+toHexStr(mod)+"\n");
             showEnding();
        }

         showTuneTable();
    }

    void showTuneTable(){
        guiConfig.TuneTable.setText("");
        for(Integer tune : tuneTableList){
            guiConfig.TuneTable.setText(guiConfig.TuneTable.getText()+toHexStr(tune)+"\n");
        }
    }

    private void processAdditionalTableItem(int index){
        AdditionalTableItem ati=additionalTable.get(index);

        String body="T  ";
        body+=toHexStr(ati.address)+"   ";

        if(ati.getOperationCode() == -1){       //byte or word
            if(ati.operands[0].equalsIgnoreCase("BYTE"))
                body+= Integer.toHexString(_getOperandSize(ati.operands[1])*2)+"  ";
            else
                body+= Integer.toHexString(_getOperandSize(ati.operands[1])*6)+"  ";

            body+=" "+getOperandsRealView(ati.operands[1])+" ";

        } else
            if(ati.getOperationCode() == -2){    //resb or resw
                ;
            } else {                                    //operation
                body+= Integer.toHexString( ati.getTkoOperationSize()*2) + "h ";
                body+= Integer.toHexString(ati.getOperationCode()) + "h ";
                if(ati.getTkoOperationSize() != 1){   //if size=1 -> no operands
                    if(checkOperandsValidity(ati)){
                        try {
                            body+= "  "+getOperandsInRightWay(ati.operands);
                        } catch(IllegalArgumentException e){
                            body="";
                            print2ndScanError(e.getMessage());
                        }
                    } else {
                        body+= "invalid operands ";
                    }
                }
            }

        body+=" \n";

        guiConfig.secondScanErrors.setText(guiConfig.secondScanErrors.getText()+body);
    }

    private void processLabelField(int i){
        String lbl= (String)guiConfig.SourceTable.getValueAt(i, 0);
        if(lbl != null && !lbl.isEmpty()){
            
                    if(Pattern.compile("^[A-Z_a-z]+([A-Za-z0-9_]){0,15}$").matcher(lbl).matches() && checkRegister(lbl) == 0){

                        if(tSIManager.getLabelsAddress(lbl) == null){       //вобще нет
                            tSIManager.addToTSI(lbl, ip);
                            return;
                        }

                        if(tSIManager.getLabelsAddress(lbl) != -1){         //есть и с адресом
                            print1stScanError("error was already defined!");
                            return;
                        }

                        if(tSIManager.getLabelsAddress(lbl) == -1){     //есть но без адреса - есть вспом список
                            tSIManager.addToTSI(lbl, ip);
                            labelNamesLists.remove(lbl);
                            processLabelInjection(lbl, ip);
                        }
                        
                    } else {
                        print1stScanError("error lbl definition- "+lbl);
                    }      
        }

        tSIManager.repaintTSITable();       //////
    }

    void processLabelInjection(String lbl, Integer ipp){
        for(ShowItem si : showItems){
            for(String s : si.items){

                if(s.contains("**") && s.contains(lbl)){
                    int indOfPart = si.items.indexOf(s);
                    si.items.remove(s);

                    int siInd = showItems.indexOf(si);

                    //if(siInd== showItems.size()-1)
                    //    si.items.add(indOfPart, "**"+lbl+"**");  //пока оставляем

                    int min = Integer.parseInt(showItems.get(siInd+1).items.get(0) , 16);
                    

                    if((ipp - min) > 0)
                        si.items.add(indOfPart, toHexStr((ipp - min))+"_");
                    else
                        si.items.add(indOfPart, toHexStr((ipp - min))+"|");

                } else {

                    if(s.contains("*") && s.contains(lbl)){
                        String replace = s.replace("*" + lbl + "*", ipp + "");
                        int ind = si.items.indexOf(s);
                        si.items.remove(s);
                        si.items.add(ind, toHexStr(replace));
                    }
                    
                }
            }
        }
    }

    private void getProgramStartAddress(){
        String str = (String)guiConfig.SourceTable.getValueAt(0, 2);
        if(str!=null)
            try{
                int tst= Integer.parseInt(str,16);
                if(tst > 0 && tst < 0xffffff){
                    ip=tst;
                    startAddress=ip;
                    System.out.println("ip="+ip);
                } else {
                    print1stScanError("invalid start address!!!");
                    ip=0;
                    startAddress=0;
                }
            } catch(Exception e){
                print1stScanError("start address missed!");
                ip=0;
            }
        else
            System.out.println("ip="+ip);
    }

    public void processFirstScan() {
        tKOManager.reloadTKO();
        guiConfig.firstScanErrors.setText("");
        tSIManager.clear();
        Main.clearJTable(guiConfig.AdditionalTable);

        getProgramStartAddress();

        for(int i=1; guiConfig.SourceTable.getValueAt(i, 1)!=null && !((String)guiConfig.SourceTable.getValueAt(i, 1)).equalsIgnoreCase("END"); i++){

            processLabelField(i);

            processOperation(i);
            System.out.println("ip=="+ip);
        }

        showAdditionalTable();

        programSize = ip - startAddress;
    }



    public void processSecondScan(){
        guiConfig.ObjectModuleArea.setText("");
        //guiConfig.secondScanErrors.setText("");

        guiConfig.ObjectModuleArea.setText(guiConfig.ObjectModuleArea.getText()+showHeader());

        for(int i=0; i<additionalTable.size(); i++)
            processAdditionalTableItem(i);



        showEnding();
        
    }

    private String showHeader(){
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
            header += toHexStr(startAddress).toString()+ "h ";

        header += Integer.toHexString(programSize) + "h\n";

        return header;
    }

    private void showEnding() {
        guiConfig.ObjectModuleArea.setText(guiConfig.ObjectModuleArea.getText()+"E  "+toHexStr(startAddress) + "h");
    }

    private ShowItem processOperation_(int i){
        if(isDirective((String)guiConfig.SourceTable.getValueAt(i, 1)) != null){
            return processDirective_(i);

        } else {

            ShowItem newShowItem = new ShowItem();
            newShowItem.add(toHexStr(ip));
            
            TKOItem item = tKOManager.checkOperationCode((String)guiConfig.SourceTable.getValueAt(i, 1));
            if(item!=null){
                
                ///////////////////////////////
                AdditionalTableItem ati = new AdditionalTableItem(ip,
                        checkCommandOperandsAndGetCode(item.getBinaryCode(), i),
                        new String[]{(String)guiConfig.SourceTable.getValueAt(i, 2),
                            (String)guiConfig.SourceTable.getValueAt(i, 3)},
                            item.getSize()
                        );
                ////////////////////////////

                newShowItem.add(Integer.toHexString(ati.getTkoOperationSize()*2)+"h");
                newShowItem.add(Integer.toHexString(ati.getOperationCode()) + "h ");

                if(item.getSize() != 1) {    //if size=1 -> no operands
                    processShowItemOperands(newShowItem, ati);
                }
                
                incIp(item.getSize(), true);

                return newShowItem;
            } else {
                print1stScanError("operation not defined! "+i);
                return null;
            }
        }
    }


    
    private ShowItem processDirective_(int i){
        Pair<Integer, Boolean>  inf = isDirective((String)guiConfig.SourceTable.getValueAt(i, 1));

        if(inf.get2() == true) { //byte or word

            ShowItem newShowItem = new ShowItem();
            newShowItem.add(toHexStr(ip));

            //
            if(inf.get1() == 1)   //byte
                try {
                    Integer val= Integer.parseInt((String)guiConfig.SourceTable.getValueAt(i, 2));
                    if( val > 127 || val < -128 ){
                        print1stScanError("not valid value for byte directive! str "+i );
                        return null;
                    }
                } catch(Exception e) {}
            else            //word
                try {
                    Integer val= Integer.parseInt((String)guiConfig.SourceTable.getValueAt(i, 2));
                    if( val > 0xffffff-1 || val < -0xffffff ){
                        print1stScanError("not valid value for word directive! str "+i );
                        return null;
                    }
                } catch(Exception e) {}
            //

            newShowItem.add(Integer.toHexString(inf.get1() * getOperandSize2(i) * 2));

            newShowItem.add(getOperandsRealView((String)guiConfig.SourceTable.getValueAt(i, 2)));

            incIp(inf.get1() * getOperandSize2(i), true);

            return newShowItem;
            
        } else {        //RES

            if(((String)guiConfig.SourceTable.getValueAt(i, 2)).startsWith("-")){
                 print1stScanError("operand error! str "+i);
                 return null;
            }
            ShowItem newShowItem = new ShowItem();
            newShowItem.add(toHexStr(ip));

            int ipp=(inf.get1() * getOperandSize(i));
            if(ipp > 0){
                if(ip+ipp > 0xffffff){
                    print1stScanError("operand error! "+i+"->too big area to allocate");
                    return null;
                }
                ip+=ipp;
            } else {
                print1stScanError("operand error! str "+i);
                return null;
            }

            return newShowItem;
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
            incIp(inf.get1() * getOperandSize2(i), true);
            
        } else {        //res
            
           AdditionalTableItem ati=new AdditionalTableItem(ip, -2,
                    new String[]{(String )guiConfig.SourceTable.getValueAt(i, 1),
                        (String)guiConfig.SourceTable.getValueAt(i, 2) });

            if(ati.getOpers()[1].startsWith("-")){
                print1stScanError("operand error! str "+i);
                return;
            }

            
            int ipp=(inf.get1() * getOperandSize(i));
            if(ipp > 0){
                if(ip+ipp > 0xffffff){
                    print1stScanError("operand error! "+i+"->too big area to allocate");
                    return;
                }
                additionalTable.add(ati);
                ip+=ipp;
            } else {
                print1stScanError("operand error! str "+i);
                return;
            }

        }
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

                incIp(item.getSize(), true);

            } else {
                print1stScanError("operation not defined! "+i);
            }
        }
    }
    
    private boolean incIp(int increment, boolean printErr){
        if(ip + increment > 0xffffff){
            if(printErr)
                print1stScanError("ip incrementing error!");
            return false;
        } else {
            ip+=increment;
            return true;
        }

    }

    private int getOperandSize(int i){
        String str = (String)guiConfig.SourceTable.getValueAt(i, 2);
        if(str!=null)
            if(str.trim().startsWith("\'") && str.trim().endsWith("\'") )
                //return str.length()-2;
                return 0;               
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
            if(str.trim().startsWith("\'") && str.trim().endsWith("\'") )
                return str.trim().length()-2;
            else
                try {
                    int op = Integer.parseInt(str);
                    return 1;
                } catch(Exception e){
                    print1stScanError("operand parsing error! "+str);
                    return -1;
                }
        }
        return -1;
    }





    private void print1stScanError(String err){
        guiConfig.firstScanErrors.setText(guiConfig.firstScanErrors.getText()+err+"\n");
        System.err.println(err);
    }

    private void print2ndScanError(String err){
        //guiConfig.secondScanErrors.setText(guiConfig.secondScanErrors.getText()+err+"\n");
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
            guiConfig.AdditionalTable.setValueAt(toHexStr(ati.getAddress()), i, 0);
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
        String op2 = (String)guiConfig.SourceTable.getValueAt(i, 3);
        
        int adresation=1, result;
        if(op1 != null){
            if(checkRegister(op1) > 0)
                adresation=0;
            if(op2 != null && checkRegister(op2) > 0)
                adresation=0;
        } else
            return binaryCode;

        String s = Integer.toBinaryString(binaryCode);
        s+="0";
        s+=adresation;
        result= Integer.parseInt(s,2);
        return result;
    }

    private int checkRegister(String s){
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

        //преобразование 10ого числа d 16ую
        if(obj instanceof String){
            try{
                int val= Integer.parseInt((String)obj);
                return toHexStr(val);
            }
            catch(Exception e){
                return null;
            }
        }
        return null;
    }

    void processSlimLabel(String lbl, ShowItem showItem,AdditionalTableItem ati){

        if(tSIManager.getLabelsAddress(lbl) == null){  // поиск неудачен
            tSIManager.addToTSI(lbl, -1);
            labelNamesLists.put(lbl, null);
            showItem.add("**"+lbl+"**");
        } else {                                        // таки удачен
            if(tSIManager.getLabelsAddress(lbl) != -1){  // адрес есть
                showItem.add( toHexStr( tSIManager.getLabelsAddress(lbl) - ( ip + ati.getTkoOperationSize() ) )+"");
            } else {                                    //адреса нет
                labelNamesLists.put(lbl, null);
                showItem.add("**"+lbl+"**");
            }
        }

        
    }

    public void doStuffWithLabel(String lbl, ShowItem showItem,AdditionalTableItem ati ){

        lbl = lbl.trim();

        if(lbl.startsWith("[") && lbl.endsWith("]")){
            if(Pattern.compile("^[A-Z_a-z]+([A-Za-z0-9_]){0,15}$").matcher(lbl.substring(1, lbl.length()-1)).matches() && (checkRegister(lbl) == 0)){
                
                processSlimLabel(lbl.substring(1, lbl.length()-1),showItem, ati);

                return;

            } else {
               
                print1stScanError("this is gonna be label operand! ipp "+toHexStr(ip));
                return;
            }
        }

        // простая метка
        if( ! ( Pattern.compile("^[A-Z_a-z]+([A-Za-z0-9_]){0,15}$").matcher(lbl).matches() && (checkRegister(lbl) == 0) ) ){
            print1stScanError("this is gonna be label operand! ip "+toHexStr(ip));
            return;
        }

        if(tSIManager.getLabelsAddress(lbl) != null ){                                              //поиск удачен

             if(tSIManager.getLabelsAddress(lbl) != -1){                                                    //адрес есть
                 showItem.add(toHexStr(tSIManager.getLabelsAddress(lbl)));
                 tuneTableList.add(ip);
             } else {                                                           //удачен и (нет адреса) -> должен быть доп список

                 if(labelNamesLists.containsKey(lbl)){
                     showItem.add("*"+lbl+"*");
                     tuneTableList.add(ip);
                 }

             }
         } else {                                                           //поиск неудачен
            tSIManager.addToTSI(lbl, -1);
            labelNamesLists.put(lbl, null);
            showItem.add("*"+lbl+"*");
            tuneTableList.add(ip);
         }

        
    }


    private void processShowItemOperands(ShowItem showItem, AdditionalTableItem ati){
        if(ati.tkoOperationSize == 4){      //должна быть 1 метка
             if(ati.operands.length >=2 && ati.operands[0]!=null  && !ati.operands[0].trim().isEmpty()){
                 
                doStuffWithLabel(ati.operands[0], showItem, ati);
                 
             } else {
                 print1stScanError("invalid operands for this operation size 4");
             }
        }

        if(ati.tkoOperationSize == 2){          // 2 registers
             if(ati.operands.length >=2 && ati.operands[0]!=null && ati.operands[1]!=null
                        && !ati.operands[0].trim().isEmpty()
                        && !ati.operands[1].trim().isEmpty())
                {
                    if((checkRegister(ati.operands[0]) > 0)  && (checkRegister(ati.operands[1]) > 0)){
                        showItem.add(getOperandsInRightWay(ati.operands));
                    }
                    else {
                        print2ndScanError("not valid operands! "+toHexStr(ati.getAddress()));
                    }
                } else {
                    print1stScanError("invalid operands for this operation size 2");
                }
        }

        if(ati.tkoOperationSize == 5){                  //reg + lbl
                if(ati.operands.length >= 2 && ati.operands[0]!=null && ati.operands[1]!=null
                        && !ati.operands[0].trim().isEmpty()
                        && !ati.operands[1].trim().isEmpty())
                {
                    if(checkRegister(ati.operands[0].trim()) > 0 || checkRegister(ati.operands[1].trim()) > 0){
                        if(checkRegister(ati.operands[0].trim()) > 0){
                            showItem.add(checkRegister(ati.operands[0].trim())+"");
                            doStuffWithLabel(ati.operands[1].trim(), showItem, ati);
                        } else {
                            doStuffWithLabel(ati.operands[0].trim(), showItem, ati);
                            showItem.add(checkRegister(ati.operands[1].trim())+"");
                        }


                    } else {
                        print2ndScanError("not valid operands! "+toHexStr(ati.getAddress())+" (should be lbl and register)");
                    }
                } else {
                    print1stScanError("invalid operands for this operation size 5");
                }
            }

        if(ati.tkoOperationSize == 3 || ati.tkoOperationSize > 5 || ati.tkoOperationSize < 1){
            print1stScanError("invalid command size = "+ ati.getTkoOperationSize());
        }
    }

    private boolean checkOperandsValidity(AdditionalTableItem ati) {
        if(ati.tkoOperationSize > 1){
            if(ati.tkoOperationSize == 4){  //должна быть 1 метка
                if(ati.operands.length >=2 && ati.operands[0]!=null  && !ati.operands[0].trim().isEmpty()){
                    if(tSIManager.getLabelsAddress(ati.operands[0]) != null){
                        return true;
                    } else {
                        print2ndScanError("unregitered label! " + ati.operands[0]);
                        return false;
                    }
                }
            }

            if(ati.tkoOperationSize == 2){          // 2 registers
                if(ati.operands.length >=2 && ati.operands[0]!=null && ati.operands[1]!=null
                        && !ati.operands[0].trim().isEmpty()
                        && !ati.operands[1].trim().isEmpty())
                {
                    if((checkRegister(ati.operands[0]) > 0)  && (checkRegister(ati.operands[1]) > 0)){
                        return true;
                    }
                    else {
                        print2ndScanError("not valid operands! "+toHexStr(ati.getAddress()));
                        return false;
                    }
                }
            }

            if(ati.tkoOperationSize == 5){                  //reg + lbl
                if(ati.operands.length >= 2 && ati.operands[0]!=null && ati.operands[1]!=null
                        && !ati.operands[0].trim().isEmpty()
                        && !ati.operands[1].trim().isEmpty())
                {
                    if(checkRegister(ati.operands[0]) > 0 || checkRegister(ati.operands[1]) > 0){
                        if(tSIManager.getLabelsAddress(ati.operands[0])!=null || tSIManager.getLabelsAddress(ati.operands[1])!=null){
                           // addLabelToTuneTable(ati.address);
                            return true;
                        } else      // label in [ ]
                            ;
                    }
                }
            }

            print2ndScanError("not valid operands! "+toHexStr(ati.getAddress()));
            return false;
        }
        else
            return true;
    }



    public class Pair<A,B> {
        private A a;
        private B b;
        public Pair(A _a, B _b){ a=_a;  b=_b; }
        public A getA(){ return a; }
        public B getB(){ return b; }
        public A get1(){ return a; }
        public B get2(){ return b; }
    }

    public class AdditionalTableItem {
        private int address;
        private int binaryOperationCode;
        private String operands[];
        private int tkoOperationSize;
        
        public AdditionalTableItem(int adr, int binCode, String opers[]){
            address=adr;
            binaryOperationCode=binCode;
            operands=opers;
        }

        public AdditionalTableItem(int adr, int binCode, String opers[], int tkoOperationCode){
            address=adr;
            binaryOperationCode=binCode;
            operands=opers;
            this.tkoOperationSize=tkoOperationCode;
        }

        public String [] getOpers(){
            return operands;
        }

        public int getAddress(){
            return address;
        }

        public int getOperationCode(){
            return binaryOperationCode;
        }

        public int getTkoOperationSize() {
            return tkoOperationSize;
        }

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
//                    if(s.startsWith("'")){          //string
//                        result+=getOperandsRealView(s);
//                    }
//                    else                     //maybe label?
                        if(tSIManager.getLabelsAddress(s) != null){
                            result+=toHexStr(tSIManager.getLabelsAddress(s))+" ";
                        } else {                                         //number or unregistered label
//                                String test= toHexStr(s);
//                                if(test!=null)
//                                    result+=test;
//                                else
                                    throw new IllegalArgumentException("unregistered label " + s+"");
                        }
                    
                }
            }
        }
        return result;
    }

    private String getOperandsRealView(String s){
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


