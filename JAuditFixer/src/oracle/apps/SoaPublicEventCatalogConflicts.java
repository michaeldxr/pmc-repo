package oracle.apps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.ProcessExecutor;
import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SoaPublicEventCatalogConflicts {
    
    static HashMap<String,ArrayList<String>> eventNames = new HashMap<String,ArrayList<String>>();
    static HashMap<String,ArrayList<String>> eventDisplayNames = new HashMap<String,ArrayList<String>>();
   // static HashMap<String,ArrayList<String>> businessEventNames = new HashMap<String,ArrayList<String>>();
    static BufferedWriter writer;
    static String viewRoot;
    static String label;
    
    public static String getFileNameDetails(String fileName) {
         String family = "";
         String product = "";
         String module = "";
         
         String[] parts = fileName.split("/");
         if(parts.length > 1) family = parts[1].trim().toUpperCase();
         if(parts.length > 3) module = parts[3].trim().toUpperCase();
         if(parts.length > 4) product = parts[4].trim().toUpperCase();
         
         return family + ", " + module + ", " + product + ", " + fileName + ", ";
     }
    private void processFile(String fName) {

        String lowerCaseName = fName.toLowerCase();
        if (lowerCaseName.contains("/noship/") ||
            lowerCaseName.contains("/test/") ||
            fName.contains("Test/") ||
            lowerCaseName.contains("servicetest") ||
            lowerCaseName.contains("datasecuritytest") ||
            lowerCaseName.contains("securitypublictest") ||
            lowerCaseName.contains("publicuitest") ||
            lowerCaseName.contains("structuretest") ||
            lowerCaseName.contains("modeltest") ||
            lowerCaseName.contains("uitest") ||
            lowerCaseName.contains("testui") ||
            lowerCaseName.contains("setest") ||
            lowerCaseName.contains("testviewcontroller") ||
            lowerCaseName.contains("flextest") ||
            lowerCaseName.contains("uimodeler-launch-test") ||
            lowerCaseName.contains("publicuibasetest") ||
            lowerCaseName.contains("uipickertest"))
            return;
        
        System.out.println("Processing file: " + fName);
        XMLDocument doc = XMLParserHelper.getXMLDocument(viewRoot + fName);
        if(doc == null)
            return;
        
        NodeList eventList = doc.getElementsByTagName("event");
        if(eventList == null) 
            return;
        
        for(int i =0; i< eventList.getLength(); i++) {
            
            Node event = eventList.item(i);
            String eventName = XMLParserHelper.getAttributeValue(event, "name");
            if(eventName.contains(":")){
                String[] parts = eventName.split(":");
                String ns = parts[0];
                String namespace=XMLParserHelper.getAttributeValue(event, "xmlns:"+ns);
                if(namespace != null && namespace != "")
                    eventName = eventName.replace(ns, namespace);
            }
           
            Node displayName= XMLParserHelper.getChildNodeWithName(event, "displayName");
            String displayNameText = "";
            if(displayName != null) 
                displayNameText = displayName.getTextContent();
            /*Node businessEventName = XMLParserHelper.getChildNodeWithName(event, "businessEventName");
            String businessEventNameText = "";
            if(businessEventName != null)
                businessEventNameText = businessEventName.getTextContent();*/
            
            ArrayList<String> files = new ArrayList<String>();
            if(eventNames.containsKey(eventName))
                files = eventNames.get(eventName);
            files.add(fName);
            eventNames.put(eventName,files);
            
            files = new ArrayList<String>();
            if(displayNameText != "" && eventDisplayNames.containsKey(displayNameText))
                files = eventDisplayNames.get(displayNameText);
            files.add(fName);
            eventDisplayNames.put(displayNameText,files);
            
           /* files = new ArrayList<String>();
            if(businessEventNameText != "" && businessEventNames.containsKey(businessEventNameText))
                files = businessEventNames.get(businessEventNameText);
            files.add(fName);
            businessEventNames.put(businessEventNameText,files);*/
            
        }
            
    }
    
    public static void main(String[] args) throws Exception{
        
        String series = "";
        java.util.Date start = new java.util.Date();
            if(args.length> 0 && args[0] != null && args[0].trim() != "")
                series = args[0].trim();
        
        //series = "FUSIONAPPS_PT.V2MIB_LINUX.X64";
        viewRoot = FamilyModuleHelper.getLabelServerTop1(series) + "/"+series+".rdd/LATEST/";
        System.out.println("Starting SoaPublicEventCatalogConflicts scan: " + start);
        
        String absPath = new File(viewRoot + ".labellog.emd.gz").getCanonicalPath();
        String[] parts = absPath.split("/");
        if(parts.length > 4)
            label = parts[4];
        
        ProcessExecutor pe = new ProcessExecutor();   
        ProcessBuilder pb = new ProcessBuilder("bash","-c","zcat " + viewRoot + 
                                                ".labellog.emd.gz | grep 'FILE fusionapps' | grep 'PublicEventCatalog.xml' " +
            "| grep -vi '/noship/' | cut -f2 -d ' '");
        pe.runProcess(pb, "/tmp/eventCatalogFileList.txt", false); 
        
        BufferedReader reader = new BufferedReader(new FileReader("/tmp/eventCatalogFileList.txt"));
        writer = new BufferedWriter(new FileWriter("soaPublicEventCatalogConflicts.csv"));
        writer.write("FAMILY,MODULE,PRODUCT,FILENAME, PUBLIC EVENT NAME, TYPE, CONFLICTING CATALOG FILES\n");
        
        SoaPublicEventCatalogConflicts scanner = new SoaPublicEventCatalogConflicts();
        
        String line = "";
        while((line = reader.readLine()) != null) {
            String catalogFile = line.trim();
            scanner.processFile(catalogFile);
        }
        
        reader.close();
        scanner.printDuplicates();
        writer.close();
        
        scanner.dumpToAngrybirds(series);
        FamilyModuleHelper.printScanDuration(start,"SoaPublicEventCatalogConflicts");
        
    }
    
    private void printDuplicates() throws Exception{
        
        for(Iterator<String> it = eventNames.keySet().iterator(); it.hasNext();) {
            
            String eventName = it.next();
            ArrayList<String> files = eventNames.get(eventName);
            if(files.size() > 1) {
                String fileName = files.get(0);
                String fileConflicts = "";
                for(int i = 1; i < files.size(); i++)
                    fileConflicts += files.get(i) + " ";
                writer.write(getFileNameDetails(fileName) + eventName + ", Event Name attribute, " + fileConflicts + "\n");
            }
        }
        
        for(Iterator<String> it = eventDisplayNames.keySet().iterator(); it.hasNext();) {
            
            String eventDisplayName = it.next();
            ArrayList<String> files = eventDisplayNames.get(eventDisplayName);
            if(files.size() > 1) {
                String fileName = files.get(0);
                String fileConflicts = "";
                for(int i = 1; i < files.size(); i++)
                    fileConflicts += files.get(i) + " ";
                writer.write(getFileNameDetails(fileName) + eventDisplayName + ", EventDisplayName, " + fileConflicts + "\n");
            }
        }
        
        /*for(Iterator<String> it = businessEventNames.keySet().iterator(); it.hasNext();) {
            
            String businessEventName = it.next();
            ArrayList<String> files = businessEventNames.get(businessEventName);
            if(files.size() > 1) {
                String fileName = files.get(0);
                String fileConflicts = "";
                for(int i = 1; i < files.size(); i++)
                    fileConflicts += files.get(i) + " ";
                writer.write(getFileNameDetails(fileName) + businessEventName + ", BusinessEventName, " + fileConflicts + "\n");
            }
        }*/
    }
    
    private void dumpToAngrybirds(String series) throws Exception {
        
         Connection con =
          DriverManager.getConnection("jdbc:oracle:thin:codescan/codescan@angrybirds.oracle.com:1521:codescan");
        PreparedStatement insert =
          con.prepareStatement("INSERT INTO codescan_results (SERIES,LABEL,FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION) VALUES (?,?,?,?,?,?,?,?,?)");     
        PreparedStatement delete =
          con.prepareStatement("DELETE FROM codescan_results where issuetype = 'SoaPublicEventCatalogConflicts' and series='" + series + "'");
        delete.execute();   
        
        BufferedReader reader = new BufferedReader(new FileReader("soaPublicEventCatalogConflicts.csv"));
        String line = reader.readLine();
        
        while((line = reader.readLine()) != null) {
            
            String[] parts = line.split(",");
            if(parts.length< 7){
                System.out.println(line);
                continue;
            }
            insert.setString(1,series);
            insert.setString(2,label);
            insert.setString(3,parts[0].trim());
            insert.setString(4,parts[1].trim());
            insert.setString(5,parts[2].trim());
            insert.setString(6,parts[3].trim());
            insert.setString(7,"SoaPublicEventCatalogConflicts");
            insert.setString(8,parts[4].trim() + " (" + parts[5].trim() + ")"); //SUB-Issue ->  Event name/displayname (name attr/displayName)
            insert.setString(9,parts[6].trim()); //Description -> conflicting  files
            insert.addBatch();
        }
        
        insert.executeBatch();
        insert.close();
        con.commit();
        con.close();  
    }
    }
