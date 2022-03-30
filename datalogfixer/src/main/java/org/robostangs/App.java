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
        for (int runidx = 0; runidx < 2; runidx++) {

            for (File file : folder.listFiles(((dir, name) -> {
                return name.endsWith(".csv") && name.startsWith("FRC");
            }))) {
                System.out.println("Found file " + file.getName());
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    FileWriter writer;
                    if (runidx == 0) {
                        writer = new FileWriter("FIXED_" + file.getName(), false);
                    } else {
                        writer = new FileWriter("FIXED_CONNECTED_" + file.getName(), false);
                    }
                    String line = "";

                    double timestamp;
                    double chunkTimestamp = 0;
                    ArrayList<String> outLine = new ArrayList<>();
                    while (line != null) {
                        if (line.length() > 0 && Character.isDigit(line.charAt(0))) {
                            timestamp = Double.parseDouble(line.substring(0, line.indexOf(',')));
                            if (line.length() > 0) {
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
                            }

                            if ((timestamp - chunkTimestamp) > 0.02) {
                                if ((runidx == 0)||line.contains("Timestamp")) {
                                    outLine = new ArrayList<String>(Arrays.asList(line.split("[,]")));
                                } else {
                                    //don't clear line
                                }
                            }

                            if (((timestamp - chunkTimestamp) > 0.02)) {
                                chunkTimestamp = timestamp;
                                writer.write(String.join(",", outLine) + "\r\n");
                                // System.out.println(String.join(",",outLine));

                            } else {
                                ArrayList<String> incomingLine = new ArrayList<String>(
                                        Arrays.asList(line.split("[,]")));

                                while (incomingLine.size() > outLine.size()) {
                                    outLine.add("");
                                }

                                for (int i = 1; i < incomingLine.size(); i++) {// skip timestamp
                                    if (!incomingLine.get(i).equals(""))
                                        outLine.set(i, incomingLine.get(i));
                                }

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
