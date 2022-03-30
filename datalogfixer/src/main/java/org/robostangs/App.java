package org.robostangs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;

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
