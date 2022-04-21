package org.robostangs;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SAXHandler extends DefaultHandler{
    boolean inStringMappings = false;
    boolean inGeneralConfigs = false;
    boolean inOutputFiles = false;

    String thisOutputFileName = "UNDEFINED";
    ArrayList<String> thisOutputFileRegexes = new ArrayList<>();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        System.out.println(uri);
        if(qName.equals("stringMappings")){
            inStringMappings = true;
        } else if(qName.equals("generalConfigs")){
            inGeneralConfigs = true;
        } else if(qName.equals("outputFiles")){
            inOutputFiles = true;
        } else if(inStringMappings){
            if(qName.equals("mapItem")){
                App.stringMappings.put(attributes.getValue("original"), attributes.getValue("replacement"));
            }
        } else if(inGeneralConfigs){
            if(qName.equals("outputUnconnectedData") && (attributes.getValue("enable").equals("true"))){
                App.outputUnconnectedData = true;
            }
            else if(qName.equals("outputConnectedData") && (attributes.getValue("enable").equals("true"))){
                App.outputConnectedData = true;
            }
        } else if(inOutputFiles){
            if(qName.equals("outputFile")){
                thisOutputFileName = attributes.getValue("name");
            }else if(qName.equals("columnNameRegex")){
                thisOutputFileRegexes.toString();
                //TOOD how to get the content of the element
            }
        }
    }
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if(qName.equals("stringMappings")){
            inStringMappings = false;
        }else if(qName.equals("generalConfigs")){
            inGeneralConfigs = false;
        } else if(qName.equals("outputFiles")){
            inOutputFiles = false;
        }
    }
}