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

public class SoaCompositeConflicts {
    
    static HashMap<String,ArrayList<String>> compositeNames = new HashMap<String,ArrayList<String>>();
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
        
        XMLDocument doc = XMLParserHelper.getXMLDocument(viewRoot + fName);
        if(doc == null)
            return;
        
        NodeList compositeList = doc.getElementsByTagName("composite");
        if(compositeList == null || compositeList.getLength() != 1) {
            System.out.println("composite.xml has either no or more than one composite element: " + fName);
            return;
        }
        Node composite = compositeList.item(0);
        String compositeName = XMLParserHelper.getAttributeValue(composite, "name");
        ArrayList<String> compositeFiles = new ArrayList<String>();
        
        if(compositeNames.containsKey(compositeName))
            compositeFiles = compositeNames.get(compositeName);
        
        compositeFiles.add(fName);
        compositeNames.put(compositeName,compositeFiles);
            
    }
    
    public static void main(String[] args) throws Exception{
        
        String series = "";
        java.util.Date start = new java.util.Date();
        if(args.length> 0 && args[0] != null && args[0].trim() != "")
            series = args[0].trim();
        
      //  series = "FUSIONAPPS_PT.V2MIB_LINUX.X64";
        viewRoot = FamilyModuleHelper.getLabelServerTop1(series) + "/"+series+".rdd/LATEST/";
        System.out.println("Starting SoaCompositeConflicts scan: " + start);
        
        //viewRoot = "/ade_autofs/ud21_fa/FUSIONAPPS_11.1.1.5.1_LINUX.X64.rdd/LATEST/";
        
        String absPath = new File(viewRoot + ".labellog.emd.gz").getCanonicalPath();
        String[] parts = absPath.split("/");
        if(parts.length > 4)
            label = parts[4];
        
        ProcessExecutor pe = new ProcessExecutor();   
        ProcessBuilder pb = new ProcessBuilder("bash","-c","zcat " + viewRoot + 
                                                ".labellog.emd.gz | grep 'FILE fusionapps' | grep 'composite.xml' " +
            "| grep -vi '/noship/' | cut -f2 -d ' '");
        pe.runProcess(pb, "/tmp/compositeFileList.txt", false); 
        
        BufferedReader reader = new BufferedReader(new FileReader("/tmp/compositeFileList.txt"));
        writer = new BufferedWriter(new FileWriter("soaCompositeConflicts.csv"));
        writer.write("FAMILY,MODULE,PRODUCT,FILENAME, COMPOSITE NAME, CONFLICTING COMPOSITE FILES\n");
        
        SoaCompositeConflicts scanner = new SoaCompositeConflicts();
        
        String line = "";
        while((line = reader.readLine()) != null) {
            String compositeFile = line.trim();
            scanner.processFile(compositeFile);
        }
        
        reader.close();
        scanner.printDuplicates();
        writer.close();
        
        scanner.dumpToAngrybirds(series);
        FamilyModuleHelper.printScanDuration(start,"SoaCompositeNameConflicts");
        
    }
    
    private void printDuplicates() throws Exception{
        
        for(Iterator<String> it = compositeNames.keySet().iterator(); it.hasNext();) {
            
            String compositeName = it.next();
            ArrayList<String> compositeFiles = compositeNames.get(compositeName);
            if(compositeFiles.size() > 1) {
                String fileName = compositeFiles.get(0);
                String fileConflicts = "";
                for(int i = 1; i < compositeFiles.size(); i++)
                    fileConflicts += compositeFiles.get(i) + " ";
                writer.write(getFileNameDetails(fileName) + compositeName + "," + fileConflicts + "\n");
            }
        }
    }
    
    private void dumpToAngrybirds(String series) throws Exception {
        
         Connection con =
          DriverManager.getConnection("jdbc:oracle:thin:codescan/codescan@angrybirds.oracle.com:1521:codescan");
        PreparedStatement insert =
          con.prepareStatement("INSERT INTO codescan_results (SERIES,LABEL,FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION) VALUES (?,?,?,?,?,?,?,?,?)");     
        PreparedStatement delete =
          con.prepareStatement("DELETE FROM codescan_results where issuetype = 'SoaCompositeNameConflicts' and series='" + series + "'");
        delete.execute();   
        
        BufferedReader reader = new BufferedReader(new FileReader("soaCompositeConflicts.csv"));
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
            insert.setString(7,"SoaCompositeNameConflicts");
            insert.setString(8,parts[4].trim()); //SUB-Issue -> Composite Name
            insert.setString(9,parts[5].trim()); //Description -> conflicting files
            insert.addBatch();
        }
        
        insert.executeBatch();
        insert.close();
        con.commit();
        con.close();  
    }
    
}
