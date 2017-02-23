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
import java.util.HashSet;

import java.util.Iterator;

import java.util.Random;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.ProcessExecutor;
import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DataBindingsUsageConflicts {
    static HashMap<String,HashSet<String>> usageIdNames = new HashMap<String,HashSet<String>>();
    static BufferedWriter writer;
    static String viewRoot;
    static String label;
    //static HashMap<String,String> exemptions = new HashMap<String,String>();
    
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
        
        NodeList pageMaps = doc.getElementsByTagName("pageMap");
        if(pageMaps.getLength() != 1) return;
        
        ArrayList<Node> pages = XMLParserHelper.getChildNodesWithName(pageMaps.item(0), "page");

        for(int i = 0; i < pages.size(); i++) {
            Node page = pages.get(i);
            String usageId = XMLParserHelper.getAttributeValue(page, "usageId");
            HashSet<String> files = new HashSet<String>();
            if(usageIdNames.containsKey(usageId))
                files = usageIdNames.get(usageId);
            files.add(fName);
            usageIdNames.put(usageId,files);
        }       
        
    }
    
    public static void main(String[] args) throws Exception{
        
        String series = "";
        java.util.Date start = new java.util.Date();
        if(args.length> 0 && args[0] != null && args[0].trim() != "")
            series = args[0].trim();
        
       // series = "FUSIONAPPS_PT.V2MIB_LINUX.X64";
        viewRoot = FamilyModuleHelper.getLabelServerTop1(series) + "/"+series+".rdd/LATEST/";
        System.out.println("Starting DataBindingsUsageConflicts scan: " + start);
        
        String absPath = new File(viewRoot + ".labellog.emd.gz").getCanonicalPath();
        String[] parts = absPath.split("/");
        if(parts.length > 4)
            label = parts[4];
        
        ProcessExecutor pe = new ProcessExecutor();   
        ProcessBuilder pb = new ProcessBuilder("bash","-c","zcat " + viewRoot + 
                                                ".labellog.emd.gz | grep 'FILE fusionapps' | egrep '.*\\.cpx' " +
            "| grep -vi '/noship/' | cut -f2 -d ' '");
        pe.runProcess(pb, "/tmp/cpxFileList.txt", false); 
        
        BufferedReader reader = new BufferedReader(new FileReader("/tmp/cpxFileList.txt"));
        writer = new BufferedWriter(new FileWriter("dataBindingsUsageConflicts.csv"));
        writer.write("FAMILY,MODULE,PRODUCT,FILENAME, USAGE ID, DESCRIPTION\n");
        
        DataBindingsUsageConflicts scanner = new DataBindingsUsageConflicts();
        
        String line = "";
        while((line = reader.readLine()) != null) {
            String cpxFile = line.trim();
            scanner.processFile(cpxFile);
        }
        
        reader.close();
        scanner.printDuplicates();
        writer.close();
        
        //one time exemptions printout
     /*   BufferedWriter w = new BufferedWriter(new FileWriter("exemptionsCpxUsages.txt"));
        for(Iterator<String> it = exemptions.keySet().iterator(); it.hasNext();){
            String usageId = it.next();
            String fileName= exemptions.get(usageId);
            w.write(usageId + " --> " + fileName);
        }
        w.close();*/
        
        scanner.dumpToAngrybirds(series);
        FamilyModuleHelper.printScanDuration(start,"DataBindingsUsageConflicts");
        
    }
    
    private void printDuplicates() throws Exception{
        
        //Random randomGenerator = new Random(); 
        
        for(Iterator<String> it = usageIdNames.keySet().iterator(); it.hasNext();) {
            
            String usageId = it.next();
            ArrayList<String> files = new ArrayList(usageIdNames.get(usageId));
            if(files.size() <= 1) 
                continue;
            
           // int r = randomGenerator.nextInt(files.size());
           // exemptions.put(usageId,files.get(r));
            for(int i = 0; i < files.size(); i++){
                //if(i == r) continue;
                writer.write(getFileNameDetails(files.get(i)) + usageId + ",page usageIds must be unique accross all FA .cpx files\n");
            }                
            
        }
    }
    
    private void dumpToAngrybirds(String series) throws Exception {
        
         Connection con =
          DriverManager.getConnection("jdbc:oracle:thin:codescan/codescan@angrybirds.oracle.com:1521:codescan");
        PreparedStatement insert =
          con.prepareStatement("INSERT INTO codescan_results (SERIES,LABEL,FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION) VALUES (?,?,?,?,?,?,?,?,?)");     
        PreparedStatement delete =
          con.prepareStatement("DELETE FROM codescan_results where issuetype = 'DataBindingsUsageConflicts' and series='" + series + "'");
        delete.execute();   
        
        BufferedReader reader = new BufferedReader(new FileReader("dataBindingsUsageConflicts.csv"));
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
            insert.setString(7,"DataBindingsUsageConflicts");
            insert.setString(8,parts[4].trim()); //SUB-Issue ->  Event name
            insert.setString(9,parts[5].trim()); //Description -> conflicting edl files
            insert.addBatch();
        }
        
        insert.executeBatch();
        insert.close();
        con.commit();
        con.close();  
    }
}
