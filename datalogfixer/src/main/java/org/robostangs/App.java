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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
        // JTextField decimationField = new
        // JTextField(String.valueOf(decimationPeriod));
        // // JCheckBox fillBlanksField = new JCheckBox("Fill blanks?",fillBlanks);
        // JCheckBox fillMessagesBlanksField = new JCheckBox("...Even Messages?",
        // fillMessagesBlanks);
        // Object[] options = { fillMessagesBlanksField, "Decimation period:" };
        // decimationPeriod = Double.valueOf(JOptionPane.showInputDialog((Component)
        // null, options,
        // "Configure Output File", JOptionPane.QUESTION_MESSAGE));
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
        for (File maybeDupFile : thisFolder.listFiles()) {
            if (maybeDupFile.getName().equals(outfilename)) {
                System.out.println("File " + outfilename + " already exists. Skipping.");
                return;
            }
        }
        FileWriter unbwriter = new FileWriter(outfilename, false);
        BufferedWriter writer = new BufferedWriter(unbwriter, 10000000);
        DataLogReader reader = new DataLogReader(thisFile.getName());
        if (!reader.isValid()) {
            System.err.println("File " + thisFile.getName() + " is invalid");
            return;
        }

        // Map is a map of StartRecordData and max array size, indexed by data record ID
        Map<Integer, SimpleEntry<DataLogRecord.StartRecordData, Integer>> records = new TreeMap<>();
        Map<Double, Map<Integer, ArrayList<String>>> tableData = new TreeMap<>();
        DecimalFormat printFormat = new DecimalFormat("#.#");
        printFormat.setDecimalSeparatorAlwaysShown(false);
        for (DataLogRecord record : reader) {
            try {
                if (record.isStart()) {
                    DataLogRecord.StartRecordData startData = record.getStartData();
                    // Size of 1 by default
                    records.put(startData.entry, new SimpleEntry<DataLogRecord.StartRecordData, Integer>(startData, 1));
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (record.isFinish() || record.isControl() || record.isSetMetadata()) {
                continue;
            }

            // Key is heading, entry is array size
            DataLogRecord.StartRecordData thisHeading = records.get(record.getEntry()).getKey();
            Integer maxArraySize = records.get(record.getEntry()).getValue();
            ArrayList<String> thisDataAsString = new ArrayList<>();
            if (thisHeading.type.equals("double")) {
                thisDataAsString.add(printFormat.format(record.getDouble()));
            } else if (thisHeading.type.equals("float")) {
                thisDataAsString.add(printFormat.format(record.getFloat()));
            } else if (thisHeading.type.equals("string")) {
                String inString = record.getString().replace(",", ";");
                String outString = fixEntryValueString(inString);
                thisDataAsString.add(outString.replace(",", ";"));
            } else if (thisHeading.type.equals("int64")) {
                thisDataAsString.add(Long.toString(record.getInteger()));
            } else if (thisHeading.type.equals("boolean")) {
                thisDataAsString.add(record.getBoolean() ? "1" : "0");
            } else if (thisHeading.type.equals("double[]")) {
                double[] thisRecordArr = record.getDoubleArray();
                maxArraySize = Math.max(maxArraySize, thisRecordArr.length);
                thisDataAsString = new ArrayList<>();
                for (int i = 0; i < thisRecordArr.length; i++) {
                    thisDataAsString.add(printFormat.format(thisRecordArr[i]));
                }
            } else if (thisHeading.type.equals("float[]")) {
                float[] thisRecordArr = record.getFloatArray();
                maxArraySize = Math.max(maxArraySize, thisRecordArr.length);
                thisDataAsString = new ArrayList<>();
                for (int i = 0; i < thisRecordArr.length; i++) {
                    thisDataAsString.add(printFormat.format(thisRecordArr[i]));
                }
            } else if (thisHeading.type.equals("int64[]")) {
                long[] thisRecordArr = record.getIntegerArray();
                maxArraySize = Math.max(maxArraySize, thisRecordArr.length);
                thisDataAsString = new ArrayList<>();
                for (int i = 0; i < thisRecordArr.length; i++) {
                    thisDataAsString.add(Long.toString(thisRecordArr[i]));
                }
            } else if (thisHeading.type.equals("string[]")) {
                String[] thisRecordArr = record.getStringArray();
                maxArraySize = Math.max(maxArraySize, thisRecordArr.length);
                thisDataAsString = new ArrayList<>(Arrays.asList(thisRecordArr));
            } else if (thisHeading.type.equals("boolean[]")) {
                boolean[] thisRecordArr = record.getBooleanArray();
                maxArraySize = Math.max(maxArraySize, thisRecordArr.length);
                thisDataAsString = new ArrayList<>();
                for (int i = 0; i < thisRecordArr.length; i++) {
                    thisDataAsString.add(thisRecordArr[i] ? "1" : "0");
                }
            } else {
                System.out.println(thisHeading.type);
            }
            double targetDoubleTimestamp = ((double) record.getTimestamp() / 1000000);
            Map<Integer, ArrayList<String>> targetTableRow;
            if (tableData.containsKey(targetDoubleTimestamp)) {
                // Already a record with this exact timestamp, have to interleave
                targetTableRow = tableData.get(targetDoubleTimestamp);
            } else {
                targetTableRow = new TreeMap<>();
                tableData.put(targetDoubleTimestamp, targetTableRow);
            }
            targetTableRow.put(record.getEntry(), thisDataAsString);
            records.get(record.getEntry()).setValue(maxArraySize);// idk
        }
        System.out.println("Read through file");

        writer.write("Timestamp,");
        for (SimpleEntry<DataLogRecord.StartRecordData, Integer> recordItem : new ArrayList<>(records.values())) {
            Integer thisEntryArrSize = recordItem.getValue();
            String thisEntryName = recordItem.getKey().name;
            for (int i = 0; i < thisEntryArrSize; i++) {
                String printColumnName = thisEntryName;
                if (thisEntryArrSize > 1) {
                    printColumnName = printColumnName + "[" + i + "]";
                }
                for (Map.Entry<String, String> potentialMatch : stringMappings.entrySet()) {
                    if (printColumnName.contains(potentialMatch.getKey())) {
                        printColumnName = printColumnName.replace(potentialMatch.getKey(), potentialMatch.getValue());
                    }
                }
                writer.write(printColumnName + ",");
            }
        }
        writer.write("\n");
        writer.flush();
        System.out.println("Print column names set");
        // decimate
        ArrayList<SimpleEntry<Double, Map<Integer, ArrayList<String>>>> decimatedTableData = new ArrayList<>();
        Map.Entry<Double, Map<Integer, ArrayList<String>>> entryToWrite = null;
        Double lastEntryTimestamp = 0.0;
        int numEntriesDone = 0;
        Double lastPercentShown = 0.0;
        for (Map.Entry<Double, Map<Integer, ArrayList<String>>> thisEntry : tableData.entrySet()) {
            if (entryToWrite != null) {
                Map<Integer, ArrayList<String>> combinedValues = entryToWrite.getValue();
                for(Map.Entry<Integer, ArrayList<String>> entry: thisEntry.getValue().entrySet()){
                    if(combinedValues.containsKey(entry.getKey()) && (combinedValues.get(entry.getKey()).size()>0) && (records.get(entry.getKey()).getKey().type.equals("string"))){
                        String oldValue = combinedValues.get(entry.getKey()).get(0);
                        String newValue = entry.getValue().get(0);
                        entry.getValue().set(0, combinedValues.get(entry.getKey()).get(0) + ";" + entry.getValue().get(0));
                        //newArr.add(oldValue +";"+newValue);
                        combinedValues.put(entry.getKey(), entry.getValue());
                    }else{
                        combinedValues.put(entry.getKey(), entry.getValue());
                    }
                }
                //combinedValues.putAll(thisEntry.getValue()); // add new values to entrytowrite
                thisEntry.setValue(combinedValues);
                entryToWrite = thisEntry;
            } else {
                entryToWrite = thisEntry;
            }

            if (thisEntry.getKey() > (lastEntryTimestamp + decimationPeriod)) {
                decimatedTableData
                        .add(new SimpleEntry<>((double) entryToWrite.getKey(), Map.copyOf(entryToWrite.getValue())));
                lastEntryTimestamp = thisEntry.getKey();
                if (fillBlanks) {
                    if (!fillMessageBlanks) {
                        Optional<Integer> messageColumn = records.entrySet().stream()
                                .filter(entry -> entry.getValue().getKey().name.equals("messages"))
                                .map(Map.Entry::getKey).findFirst();
                        if (messageColumn.isPresent())
                            entryToWrite.getValue().put(messageColumn.get(), new ArrayList<String>());
                    }
                } else {
                    entryToWrite = null;
                }

            }
            Double thisPercent = ((double) numEntriesDone++) / (double) tableData.size();
            if (thisPercent > (lastPercentShown + 0.1)) {
                System.out.println("Decimation " + thisPercent + " done");
                lastPercentShown = thisPercent;
            }
        }
        System.out.println("Decimation done");

        numEntriesDone = 0;
        lastPercentShown = 0.0;
        for (SimpleEntry<Double, Map<Integer, ArrayList<String>>> row : decimatedTableData) {
            StringBuilder builder = new StringBuilder(row.getKey() + ",");
            for (int logEntryIdx = 1; logEntryIdx < row.getValue().size() + 1; logEntryIdx++) {
                ArrayList<String> thisColumnSetEntry = row.getValue().get(logEntryIdx);
                SimpleEntry<DataLogRecord.StartRecordData, Integer> headingItem = records.get(logEntryIdx);
                for (int arrayIdx = 0; arrayIdx < headingItem.getValue(); arrayIdx++) {
                    // For each spot in the heading list
                    if (thisColumnSetEntry != null) {
                        if (arrayIdx < thisColumnSetEntry.size())
                            builder.append(thisColumnSetEntry.get(arrayIdx));
                    }
                    builder.append(",");
                }
            }
            builder.append("\n");
            writer.write(builder.toString());

            Double thisPercent = ((double) numEntriesDone++) / (double) decimatedTableData.size();
            if (thisPercent > (lastPercentShown + 0.1)) {
                System.out.println("Writeout " + thisPercent + " done");
                lastPercentShown = thisPercent;
            }
        }
        System.out.println("Writeout Done");
        writer.flush();
        writer.close();
    }

    public static final Map<String, String> stringMappings;
    static {
        stringMappings = new HashMap<>();
        stringMappings.put("joystick0", "Driver");
        stringMappings.put("joystick1", "Manip");
        stringMappings.put("buttons[0]", "Btn_A");
        stringMappings.put("buttons[1]", "Btn_B");
        stringMappings.put("buttons[2]", "Btn_X");
        stringMappings.put("buttons[3]", "Btn_Y");
        stringMappings.put("buttons[4]", "Btn_LBumper");
        stringMappings.put("buttons[5]", "Btn_RBumper");
        stringMappings.put("buttons[6]", "Btn_WindowButton");
        stringMappings.put("buttons[7]", "Btn_MenuButton");
        stringMappings.put("buttons[8]", "Btn_LStick");
        stringMappings.put("buttons[9]", "Btn_RStick");
        stringMappings.put("axes[0]", "Left_XAxis");
        stringMappings.put("axes[1]", "Left_YAxis");
        stringMappings.put("axes[2]", "Left_Trigger");
        stringMappings.put("axes[3]", "Right_Trigger");
        stringMappings.put("axes[4]", "Right_XAxis");
        stringMappings.put("axes[5]", "Right_YAxis");
    }

    public static String fixEntryValueString(String inString) {
        if (inString.startsWith(" UnderVoltage")) { // Falcon faults
            String[] dataArr = inString.split(" ");
            String outString = "";
            for (String potentialFaults : dataArr) {
                if (potentialFaults.contains("1")) {
                    outString = outString + potentialFaults + ";";
                }
            }
            if (outString.length() == 0) {
                return "No Faults";
            }
            return outString;
        }

        return inString;
    }
}
