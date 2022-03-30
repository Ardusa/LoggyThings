package org.robostangs;

import java.awt.Component;
import java.awt.TrayIcon.MessageType;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Collectors;

import javax.security.auth.callback.TextInputCallback;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import edu.wpi.first.util.datalog.DataLogReader;
import edu.wpi.first.util.datalog.DataLogRecord;


/**
 * Hello world!
 */
public final class App {
    private App() {
    }

    /**
     * Says hello to the world.
     * 
     * @param args The arguments of the program.
     */

    public static void main(String[] args) {
        double decimationPeriod = 0.02;
        boolean fillBlanks = true;
        boolean fillMessagesBlanks = false;
        // JTextField decimationField = new JTextField(String.valueOf(decimationPeriod));
        // // JCheckBox fillBlanksField = new JCheckBox("Fill blanks?",fillBlanks);
        // JCheckBox fillMessagesBlanksField = new JCheckBox("...Even Messages?", fillMessagesBlanks);
        // Object[] options = { fillMessagesBlanksField, "Decimation period:" };
        // decimationPeriod = Double.valueOf(JOptionPane.showInputDialog((Component) null, options,
        //         "Configure Output File", JOptionPane.QUESTION_MESSAGE));
        // //fillBlanks = ((JCheckBox) (options[0])).isSelected();
        // fillMessagesBlanks = ((JCheckBox) (options[0])).isSelected();

        File folder = new File(System.getProperty("user.dir"));
        System.out.println("Looking in " + System.getProperty("user.dir"));
        for (File file : folder.listFiles(((dir, name) -> {
            return name.endsWith(".wpilog") && name.startsWith("FRC");
        }))) {
            try {
                processFile(file, folder, "FIXED2_", decimationPeriod, false, false);
                processFile(file, folder, "FIXED2_CONNECTED_", decimationPeriod, true, fillMessagesBlanks);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void processFile(File thisFile, File thisFolder, String prefix, double decimationPeriod,
            boolean fillBlanks, boolean fillMessageBlanks) throws Exception {
        String outfilename = prefix + thisFile.getName().replace("wpilog", "csv");
        // for (File maybeDupFile : thisFolder.listFiles()) {
        //     if (maybeDupFile.getName().equals(outfilename)) {
        //         System.out.println("File " + outfilename + " already exists. Skipping.");
        //         return;
        //     }
        // }
        FileWriter writer = new FileWriter(outfilename, false);
        DataLogReader reader = new DataLogReader(thisFile.getName());
        if(!reader.isValid()){
            System.err.println("File "+thisFile.getName()+" is invalid");
            return;
        }
        
        Map<Integer, DataLogRecord.StartRecordData> records = new TreeMap<>();
        Map<Integer, Map<Integer,String>> printColumnHeadings = new TreeMap<>();
        ArrayList<SimpleEntry<Double, Map<Integer,ArrayList<String>>>> tableData = new ArrayList<>();
        long lastTimestamp = -1000000;
        for(DataLogRecord record : reader){
            try {
                if(record.isStart()){
                    DataLogRecord.StartRecordData startData = record.getStartData();
                    records.put(startData.entry, startData);
                    Map<Integer, String> thisColumnHeading = new TreeMap<>();
                    thisColumnHeading.put(0, startData.name);
                    printColumnHeadings.put(startData.entry,thisColumnHeading);
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(record.isFinish()||record.isControl()||record.isSetMetadata()){
                continue;
            }
            
            DataLogRecord.StartRecordData thisHeading = records.get(record.getEntry());
            ArrayList<String> thisDataAsString = new ArrayList<>();
            if(thisHeading.type.equals("double")){
                thisDataAsString.add(String.valueOf(record.getDouble()));
            }else if (thisHeading.type.equals("string")){
                thisDataAsString.add(record.getString().replace(",", ";"));
            }else if (thisHeading.type.equals("int64")){
                thisDataAsString.add(Long.toString(record.getInteger()));
            }else if (thisHeading.type.equals("boolean")){
                thisDataAsString.add(record.getBoolean()?"1":"0");
            }else if (thisHeading.type.equals("double[]")){
                double[] thisRecordArr = record.getDoubleArray();
                for(int i=0;i<thisRecordArr.length;i++){
                    printColumnHeadings.get(record.getEntry()).put(i, thisHeading.name+"["+String.valueOf(i)+"]");
                }
                thisDataAsString =new ArrayList<>();
                for(int i=0;i<thisRecordArr.length;i++){
                    thisDataAsString.add(Double.toString(thisRecordArr[i]));
                }
            }else if (thisHeading.type.equals("int64[]")){
                long[] thisRecordArr = record.getIntegerArray();
                for(int i=0;i<thisRecordArr.length;i++){
                    printColumnHeadings.get(record.getEntry()).put(i, thisHeading.name+"["+String.valueOf(i)+"]");
                }
                thisDataAsString =new ArrayList<>();
                for(int i=0;i<thisRecordArr.length;i++){
                    thisDataAsString.add(Long.toString(thisRecordArr[i]));
                }
            }else if (thisHeading.type.equals("string[]")){
                String[] thisRecordArr = record.getStringArray();
                for(int i=0;i<thisRecordArr.length;i++){
                    printColumnHeadings.get(record.getEntry()).put(i, thisHeading.name+"["+String.valueOf(i)+"]");
                }
                thisDataAsString =new ArrayList<>(Arrays.asList(thisRecordArr));
            }
            if(record.getTimestamp()>(lastTimestamp+decimationPeriod*1000000.0)){
                if(!fillBlanks||tableData.size()==0){
                    Map<Integer, ArrayList<String>> dataRow = new TreeMap<>();
                    SimpleEntry<Double, Map<Integer,ArrayList<String>>> blankEntry = new SimpleEntry<Double, Map<Integer,ArrayList<String>>>(((double)record.getTimestamp())/1000000.0,dataRow);
                    tableData.add(blankEntry);
                }else{
                    SimpleEntry<Double, Map<Integer,ArrayList<String>>> copiedEntry = new SimpleEntry<Double, Map<Integer,ArrayList<String>>>(((double)record.getTimestamp())/1000000.0,tableData.get(tableData.size()-1).getValue());
                    tableData.add(copiedEntry);
                    if(!fillMessageBlanks){
                        try {
                            Integer messageColumn = records.entrySet().stream().filter(entry ->entry.getValue().name.equals("messages")).map(Map.Entry::getKey).findFirst().get();
                            copiedEntry.getValue().put(messageColumn,new ArrayList<String>());
                        } catch (Exception e) {
                            //TODO: handle exception
                        }
                        
                    }
                        
                }
                lastTimestamp = record.getTimestamp();
            }
            Map<Integer,ArrayList<String>> thisRow = tableData.get(tableData.size()-1).getValue();
            thisRow.put(record.getEntry(),thisDataAsString);
        }
        System.out.println("Read through file");

        writer.write("Timestamp,");
        ArrayList<Integer> arraySizes = new ArrayList<>();
        for(Map<Integer,String> printColumnHeading: new ArrayList<Map<Integer,String>>(printColumnHeadings.values())){
            for(String actualColumnHeading: new ArrayList<String>(printColumnHeading.values())){
                writer.write(actualColumnHeading+",");
            }
            arraySizes.add(printColumnHeading.size());
        }
        writer.write("\n");
        writer.flush();

        for(SimpleEntry<Double, Map<Integer,ArrayList<String>>> row: tableData){
            writer.write(String.valueOf(row.getKey())+",");

            for(int col=1;col<arraySizes.size();col++){
                ArrayList<String> thisColumnSetEntry = row.getValue().get(col);
                for(int integerElementColumn = 0;integerElementColumn<arraySizes.get(col);integerElementColumn++){
                    try{
                        writer.write(thisColumnSetEntry.get(integerElementColumn));
                    }catch (Exception e){
                        //no element there
                    }
                    writer.write(",");
                }
            }
            writer.write("\n");
        }
        System.out.println("Writeout Done");
        writer.flush();
        writer.close();
    }

    public static void oldmain(String[] args) {
        File folder = new File(System.getProperty("user.dir"));
        System.out.println("Looking in " + System.getProperty("user.dir"));
        for (int runidx = 0; runidx < 2; runidx++) {// run twice once just decimating then again filling in
            for (File file : folder.listFiles(((dir, name) -> {
                return name.endsWith(".csv") && name.startsWith("FRC");
            }))) {

                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    String outfilename;
                    if (runidx == 0) {
                        outfilename = "FIXED_" + file.getName();
                    } else {
                        outfilename = "FIXED_CONNECTED_" + file.getName();
                    }
                    boolean dupFound = false;
                    for (File maybeDupFile : folder.listFiles()) {
                        if (maybeDupFile.getName().equals(outfilename)) {
                            System.out.println("File " + outfilename + " already exists. Skipping.");
                            dupFound = true;
                            break;
                        }
                    }

                    if (dupFound)
                        continue;

                    FileWriter writer = new FileWriter(outfilename, false);
                    System.out.println("Generating " + outfilename);
                    String line = "";

                    double timestamp;
                    double chunkTimestamp = 0;
                    ArrayList<String> outLine = new ArrayList<>();
                    while (line != null) {
                        if (line.length() > 0 && Character.isDigit(line.charAt(0))) {
                            timestamp = Double.parseDouble(line.substring(0, line.indexOf(',')));

                            // Check if escaped inside quotes
                            boolean inEscaped = false;
                            char[] linearr = line.toCharArray();
                            for (int i = 0; i < line.length(); i++) {
                                if (linearr[i] == '\"') {
                                    inEscaped = !inEscaped;
                                }

                                if (inEscaped) {
                                    if (linearr[i] == ',') {
                                        linearr[i] = ';';
                                    }
                                }
                            }
                            line = String.valueOf(linearr);

                            ArrayList<String> incomingLine = new ArrayList<String>(
                                    Arrays.asList(line.split("[,]")));

                            if ((timestamp - chunkTimestamp) > 0.02) {
                                // write out
                                chunkTimestamp = timestamp;
                                writer.write(String.join(",", outLine) + "\r\n");
                                // System.out.println(String.join(",",outLine));

                                // clear line if not doing connected
                                if ((runidx == 0)) {
                                    outLine = new ArrayList<String>();
                                } else {
                                    // don't clear line
                                }
                            }

                            while (incomingLine.size() > outLine.size()) {
                                outLine.add("");
                            }

                            for (int i = 0; i < incomingLine.size(); i++) {
                                String thisItem = incomingLine.get(i);
                                if (thisItem.equals(""))
                                    continue;

                                // bool to integer for graph
                                if (thisItem.equalsIgnoreCase("false")) {
                                    thisItem = "0";
                                }
                                if (thisItem.equalsIgnoreCase("true")) {
                                    thisItem = "1";
                                }

                                outLine.set(i, thisItem);

                            }

                        } else {
                            writer.write(line);
                        }
                        line = reader.readLine();
                    }
                    writer.flush();
                    writer.close();
                    reader.close();

                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
}
