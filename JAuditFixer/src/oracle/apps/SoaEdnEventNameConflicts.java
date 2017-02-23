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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SoaEdnEventNameConflicts {
    static HashMap<String,ArrayList<String>> ednEventNames = new HashMap<String,ArrayList<String>>();
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
        
        if(!fName.endsWith(".edl"))
            return;
        
        XMLDocument doc = XMLParserHelper.getXMLDocument(viewRoot + fName);
        if(doc == null)
            return;
        
        NodeList eventList = doc.getElementsByTagName("event-definition");
        if(eventList == null) 
            return;
        
        String namespace = "";
        NodeList definitions = doc.getElementsByTagName("definitions");
        if(definitions != null && definitions.getLength() ==1) {
            Node definitionsNode = definitions.item(0);
            namespace = XMLParserHelper.getAttributeValue(definitionsNode, "targetNamespace");
            String xmlns = XMLParserHelper.getAttributeValue(definitionsNode, "xmlns");
            if(xmlns == null)
                xmlns="http://schemas.oracle.com/events/edl";
            if(namespace !=null){
                namespace = namespace.replace(xmlns,"");
                namespace = namespace.replace("http://xmlns.oracle.com","");
            }
        }        
        for(int i =0; i< eventList.getLength(); i++) {
            
            Node eventDefinition = eventList.item(i);
            String eventName = XMLParserHelper.getAttributeValue(eventDefinition, "name");
            String fullyQualifiedName = eventName;
           // Node childContent = XMLParserHelper.getChildNodeWithName(eventDefinition, "content");
          //  String namespace = XMLParserHelper.getAttributeValue(childContent, "xmlns:ns0");
        
            if(namespace != null)
                fullyQualifiedName = namespace+eventName;
            ArrayList<String> edlFiles = new ArrayList<String>();
            if(ednEventNames.containsKey(fullyQualifiedName))
                edlFiles = ednEventNames.get(fullyQualifiedName);
            edlFiles.add(fName);
            ednEventNames.put(fullyQualifiedName,edlFiles);        
        }
            
    }
    
    public static void main(String[] args) throws Exception{
        
        String series = "";
        java.util.Date start = new java.util.Date();
            if(args.length> 0 && args[0] != null && args[0].trim() != "")
                series = args[0].trim();
        
        //series = "FUSIONAPPS_PT.V2MIB_LINUX.X64";
        viewRoot = FamilyModuleHelper.getLabelServerTop1(series) + "/"+series+".rdd/LATEST/";
        System.out.println("Starting SoaEdnEventConflicts scan: " + start);
        
        //viewRoot = "/ade_autofs/ud21_fa/FUSIONAPPS_11.1.1.5.1_LINUX.X64.rdd/LATEST/";
        
        String absPath = new File(viewRoot + ".labellog.emd.gz").getCanonicalPath();
        String[] parts = absPath.split("/");
        if(parts.length > 4)
            label = parts[4];
        
        ProcessExecutor pe = new ProcessExecutor();   
        ProcessBuilder pb = new ProcessBuilder("bash","-c","zcat " + viewRoot + 
                                                ".labellog.emd.gz | grep 'FILE fusionapps' | egrep '.*\\.edl' " +
            "| grep -vi '/noship/' | cut -f2 -d ' '");
        pe.runProcess(pb, "/tmp/edlFileList.txt", false); 
        
        BufferedReader reader = new BufferedReader(new FileReader("/tmp/edlFileList.txt"));
        writer = new BufferedWriter(new FileWriter("soaEdnEventConflicts.csv"));
        writer.write("FAMILY,MODULE,PRODUCT,FILENAME, EDN EVENT NAME, CONFLICTING EDL FILES\n");
        
        SoaEdnEventNameConflicts scanner = new SoaEdnEventNameConflicts();
        
        String line = "";
        while((line = reader.readLine()) != null) {
            String compositeFile = line.trim();
            scanner.processFile(compositeFile);
        }
        
        reader.close();
        scanner.printDuplicates();
        writer.close();
        
        scanner.dumpToAngrybirds(series);
        FamilyModuleHelper.printScanDuration(start,"SoaEdnEventConflicts");
        
    }
    
    private void printDuplicates() throws Exception{
        
        for(Iterator<String> it = ednEventNames.keySet().iterator(); it.hasNext();) {
            
            String eventName = it.next();
            ArrayList<String> edlFiles = ednEventNames.get(eventName);
            if(edlFiles.size() > 1) {
                String fileName = edlFiles.get(0);
                String fileConflicts = "";
                for(int i = 1; i < edlFiles.size(); i++)
                    fileConflicts += edlFiles.get(i) + " ";
                writer.write(getFileNameDetails(fileName) + eventName + "," + fileConflicts + "\n");
            }
        }
    }
    
    private void dumpToAngrybirds(String series) throws Exception {
        
         Connection con =
          DriverManager.getConnection("jdbc:oracle:thin:codescan/codescan@angrybirds.oracle.com:1521:codescan");
        PreparedStatement insert =
          con.prepareStatement("INSERT INTO codescan_results (SERIES,LABEL,FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION) VALUES (?,?,?,?,?,?,?,?,?)");     
        PreparedStatement delete =
          con.prepareStatement("DELETE FROM codescan_results where issuetype = 'SoaEdnEventConflicts' and series='" + series + "'");
        delete.execute();   
        
        BufferedReader reader = new BufferedReader(new FileReader("soaEdnEventConflicts.csv"));
        String line = reader.readLine();
        
        while((line = reader.readLine()) != null) {
            
            String[] parts = line.split(",");
            if(parts.length< 6){
                System.out.println(line);
                continue;
            }
            insert.setString(1,series);
            insert.setString(2,label);
            insert.setString(3,parts[0].trim());
            insert.setString(4,parts[1].trim());
            insert.setString(5,parts[2].trim());
            insert.setString(6,parts[3].trim());
            insert.setString(7,"SoaEdnEventConflicts");
            insert.setString(8,parts[4].trim()); //SUB-Issue -> EDN Event name
            insert.setString(9,parts[5].trim()); //Description -> conflicting edl files
            insert.addBatch();
        }
        
        insert.executeBatch();
        insert.close();
        con.commit();
        con.close();  
    }
}
