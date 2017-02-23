package oracle.apps.stringscan;

import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.regex.Pattern;

import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;

public class InfoletConfigCheck {
    
    // declare the fields that are going to be consistent for all the records
    public static final String COMPONENT = "itemNode";
    public static final String ATTRIBUTE = "label";
    public static final String CATEGORY = "HARDCODED";
        
    public static void main(String[] args) {
        
        // check if the command line arguments have been provided correctly
        if (args.length != 2) {
            System.out.println("Error, please provide infolet config file and output file.\n" +
                " e.g. java InfoletConfigCheck <InfoletConfigFile> <LOGDIR>/sumHardStr.csv");
            return;
        }
        
        if (args[0] == null || args[0].length() == 0) {
            System.out.println("Error, infolet config file must be specified.");
            return;
        }
        
        if (args[1] == null || args[1].length() == 0) {
            System.out.println("Error, output file path must be specified.");
            return;
        }
        
        // retrieve the command line arguments
        String infoletConfigFilePath = args[0];
        String outputFile = args[1];
        
        try {
            // open the xml file and parse it
            XMLDocument infoletConfigXML = XMLParserHelper.getXMLDocument(infoletConfigFilePath, false);
            Node rootItemNode = infoletConfigXML.getElementsByTagName("itemNode").item(0);
            
            // retrieve all the second-level itemNodes
            ArrayList<Node> nodes = XMLParserHelper.getChildNodesWithName(rootItemNode, "itemNode");
            
            // go through each itemNode and retrieve their id and label
            for (Node itemNode : nodes) {
                String id = XMLParserHelper.getAttributeValue(itemNode, "id");
                String label = XMLParserHelper.getAttributeValue(itemNode, "label");
                
                if (label == null) {
                    // skip this itemNode
                    continue;
                }
                
                // check if the label uses the adf bundle the expected way
                String adfBundlePattern = "^[ ]*#\\{adfBundle[ ]*\\['[^\\]']+']\\['[^\\]']+']\\}\\s*$";
                if (Pattern.compile(adfBundlePattern).matcher(label).find()) {
                    // this is a valid use of a string
                    continue;
                }
                
                // take the hardcoded portion to be the entire label string
                String hardcodedPortion = label;
                
                // record this in the output file
                writeToOutputFile(infoletConfigFilePath, id, label, hardcodedPortion, outputFile);

            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    public static void writeToOutputFile(String infoletConfigFilePath, String id, String label, String hardcodedPortion,
                                         String outputFile) {
                
        
        // identify the family, product, and filename based on the file path
        String[] pathArray = infoletConfigFilePath.split("/");
        
        // get the depth of the /fusionapps directory 
        int fusionappsDirDepth = 0;
        for (String dir : pathArray) {
            if (!dir.equals("fusionapps")) {
                fusionappsDirDepth += 1;
            } else {
                break;
            }
        }
        
        String family = pathArray[fusionappsDirDepth + 1];
        String product = pathArray[fusionappsDirDepth + 4].toUpperCase();
        String fileName = pathArray[pathArray.length - 1];
        
        // prepare the comma-separated record to be written
        String record = family + "," + product + "," + fileName + "," + COMPONENT + "," + 
                        ATTRIBUTE + "," + id + ",\"" + label + "\"," + CATEGORY + "," + 
                        infoletConfigFilePath + "," + hardcodedPortion;
                
        FileWriter fileWriter = null;
        try {
            // open the output file for appending
            fileWriter = new FileWriter(outputFile, true);
            fileWriter.write(record + "\n");
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    System.out.println("Error writing to " + outputFile);
                    e.printStackTrace();
                }
            }
        }
        
    }
    
}
