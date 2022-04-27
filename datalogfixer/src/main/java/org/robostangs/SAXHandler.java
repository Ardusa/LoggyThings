package org.robostangs;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SAXHandler extends DefaultHandler {
    boolean inStringMappings = false;
    boolean inGeneralConfigs = false;
    boolean inOutputFiles = false;
    boolean regexStarted = false;

    String thisOutputFileName = null;
    StringBuilder thisRegex = new StringBuilder();
    ArrayList<String> thisOutputFileRegexes = new ArrayList<>();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        System.out.println(uri);
        if (qName.equals("stringMappings")) {
            inStringMappings = true;
        } else if (qName.equals("generalConfigs")) {
            inGeneralConfigs = true;
        } else if (qName.equals("outputFiles")) {
            inOutputFiles = true;
        } else if (inStringMappings) {
            if (qName.equals("mapItem")) {
                App.stringMappings.put(attributes.getValue("original"), attributes.getValue("replacement"));
            }
        } else if (inGeneralConfigs) {
            if (qName.equals("outputUnconnectedData") && (attributes.getValue("enable").equals("true"))) {
                App.outputUnconnectedData = true;
            } else if (qName.equals("outputConnectedData") && (attributes.getValue("enable").equals("true"))) {
                App.outputConnectedData = true;
            }
        } else if (inOutputFiles) {
            if (qName.equals("outputFile")) {
                thisOutputFileName = attributes.getValue("name");
            } else if (qName.equals("columnNameRegex")) {
                regexStarted = true;
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("stringMappings")) {
            inStringMappings = false;
        } else if (qName.equals("generalConfigs")) {
            inGeneralConfigs = false;
        } else if (qName.equals("outputFiles")) {
            inOutputFiles = false;
        } else if (qName.equals("outputFile")) {
            App.outputFiles.put(thisOutputFileName, thisOutputFileRegexes);
            thisOutputFileRegexes = new ArrayList<>();
        } else if (qName.equals("columnNameRegex")) {
            regexStarted = false;
            thisOutputFileRegexes.add(thisRegex.toString());
            thisRegex = new StringBuilder();
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        // TODO Auto-generated method stub
        if (regexStarted)
            thisRegex.append(ch, start, length);
    }
}