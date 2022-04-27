package org.robostangs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import edu.wpi.first.util.datalog.DataLogReader;
import edu.wpi.first.util.datalog.DataLogRecord;

/**
 * Hello world!
 */
public final class App {
    private App() {
    }

    public static boolean outputUnconnectedData = false;
    public static boolean outputConnectedData = true;
    public static Map<String, String> stringMappings = new HashMap<>();
    public static Map<String, ArrayList<String>> outputFiles = new HashMap<>();

    /**
     * Says hello to the world.
     * 
     * @param args The arguments of the program.
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     */

    public static void main(String[] args) throws ParserConfigurationException, SAXException {
        double decimationPeriod = 0.02;
        boolean fillMessagesBlanks = false;

        File folder = new File(System.getProperty("user.dir"));
        System.out.println("Looking in " + System.getProperty("user.dir"));
        try {
            File configFile = new File("fixerConfig.xml");
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(configFile, new SAXHandler());
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (File file : folder.listFiles(((dir, name) -> {
            return name.endsWith(".wpilog") && name.startsWith("FRC");
        }))) {
            try {
                if (outputUnconnectedData)
                    processFile(file, folder, "FIXED2_", decimationPeriod, false, false);
                if (outputConnectedData)
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
                System.out.println(
                        "File " + outfilename + " already exists. Skipping. Delete main file to generate sub-files");
                return;
            }
        }
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
                long thisVal = record.getInteger();
                String outString = "";
                if (thisHeading.name.equals("systemTime")) {
                    outString = Instant.EPOCH.plus(
                            Duration.ofNanos(
                                    TimeUnit.MICROSECONDS.toNanos(thisVal)))
                            .atZone(ZoneId.of("America/Detroit")).format(DateTimeFormatter.ofPattern("hh:mm:ss.SSS a"));
                } else {
                    outString = Long.toString(thisVal);
                }

                thisDataAsString.add(outString);
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

        // decimate
        ArrayList<SimpleEntry<Double, Map<Integer, ArrayList<String>>>> decimatedTableData = new ArrayList<>();
        Map.Entry<Double, Map<Integer, ArrayList<String>>> entryToWrite = null;
        Double lastEntryTimestamp = 0.0;
        int numEntriesDone = 0;
        Double lastPercentShown = 0.0;
        for (Map.Entry<Double, Map<Integer, ArrayList<String>>> thisEntry : tableData.entrySet()) {
            if (entryToWrite != null) {
                Map<Integer, ArrayList<String>> combinedValues = entryToWrite.getValue();
                for (Map.Entry<Integer, ArrayList<String>> entry : thisEntry.getValue().entrySet()) {
                    if (combinedValues.containsKey(entry.getKey()) && (combinedValues.get(entry.getKey()).size() > 0)
                            && (records.get(entry.getKey()).getKey().type.equals("string"))) {
                        String oldValue = combinedValues.get(entry.getKey()).get(0);
                        String newValue = entry.getValue().get(0);
                        entry.getValue().set(0,
                                combinedValues.get(entry.getKey()).get(0) + ";" + entry.getValue().get(0));
                        // newArr.add(oldValue +";"+newValue);
                        combinedValues.put(entry.getKey(), entry.getValue());
                    } else {
                        combinedValues.put(entry.getKey(), entry.getValue());
                    }
                }
                // combinedValues.putAll(thisEntry.getValue()); // add new values to
                // entrytowrite
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
        for (Map.Entry<String, ArrayList<String>> fileFormat : App.outputFiles.entrySet()) {
            writeToFile(outfilename, fileFormat.getKey(), String.join("|", fileFormat.getValue()), records,
                    decimatedTableData);
        }
    }

    private static void writeToFile(String outfilename, String outfileFolder, String regexString,
            Map<Integer, SimpleEntry<DataLogRecord.StartRecordData, Integer>> records,
            ArrayList<SimpleEntry<Double, Map<Integer, ArrayList<String>>>> decimatedTableData) throws IOException {
        Pattern regex = Pattern.compile(regexString, Pattern.CASE_INSENSITIVE);
        String targetFileRelativePath = outfileFolder + ((outfileFolder.equals("") ? "" : File.separator)) + outfilename;
        File targetFile = new File(targetFileRelativePath);
        if (!outfileFolder.equals("")) {
            File parentDir = targetFile.getParentFile();
            if (parentDir == null || !parentDir.exists()) {
                parentDir.mkdirs();
            }
        }
        if(targetFile.exists()){
            System.out.println("File "+targetFileRelativePath+" already exists. Skipping");
            return;
        }
        FileWriter unbwriter = new FileWriter(
                targetFile, false);
        BufferedWriter writer = new BufferedWriter(unbwriter, 10000000);
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
                if (regex.matcher(printColumnName).find())//TODO no work
                    writer.write(printColumnName + ",");// allowed per regex
            }
        }
        writer.write("\n");
        writer.flush();
        System.out.println("Print column names set");

        int numEntriesDone;
        Double lastPercentShown;
        numEntriesDone = 0;
        lastPercentShown = 0.0;
        Map<Integer, Boolean> skippedColumns = new TreeMap<>();
        for (SimpleEntry<Double, Map<Integer, ArrayList<String>>> row : decimatedTableData) {
            StringBuilder builder = new StringBuilder(row.getKey() + ",");
            for (int logEntryIdx = 1; logEntryIdx < row.getValue().size() + 1; logEntryIdx++) {
                ArrayList<String> thisColumnSetEntry = row.getValue().get(logEntryIdx);
                SimpleEntry<DataLogRecord.StartRecordData, Integer> headingItem = records.get(logEntryIdx);
                if (!skippedColumns.containsKey(logEntryIdx)){
                    skippedColumns.put(logEntryIdx,!regex.matcher(headingItem.getKey().name).find());
                }
                if(skippedColumns.get(logEntryIdx))
                    continue;//not in regex

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
