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

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.ProcessExecutor;


public class SoaBpmTaskNameConflicts {
    
    static HashMap<String,ArrayList<String>> taskNames = new HashMap<String,ArrayList<String>>();
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
    
    private String getFamily(String fileName) {
        String family = "";
        String[] parts = fileName.split("/");
        if(parts.length > 1) family = parts[1].trim().toUpperCase();
        return family;
        
    }
    private void processFile(String fName) {

        String lowerCaseName = fName.toLowerCase();
        if(!fName.endsWith(".task"))
            return;
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
        
       String[] parts = fName.split("/");
       String taskName = parts[parts.length-1];
       if(!taskName.endsWith(".task"))
           return;
       taskName = taskName.replace(".task", "");
       ArrayList<String> taskFiles = new ArrayList<String>();
       if(taskNames.containsKey(taskName))
           taskFiles = taskNames.get(taskName);
       taskFiles.add(fName);
       taskNames.put(taskName,taskFiles);
            
    }
    
    public static void main(String[] args) throws Exception{
        
        String series = "";
        java.util.Date start = new java.util.Date();
            if(args.length> 0 && args[0] != null && args[0].trim() != "")
                series = args[0].trim();
        
        //series = "FUSIONAPPS_PT.V2MIB_LINUX.X64";
        viewRoot = FamilyModuleHelper.getLabelServerTop1(series) + "/"+series+".rdd/LATEST/";
        System.out.println("Starting SoaBpmTaskNameConflicts scan: " + start);
        
        //viewRoot = "/ade_autofs/ud21_fa/FUSIONAPPS_11.1.1.5.1_LINUX.X64.rdd/LATEST/";
        
        String absPath = new File(viewRoot + ".labellog.emd.gz").getCanonicalPath();
        String[] parts = absPath.split("/");
        if(parts.length > 4)
            label = parts[4];
        
        ProcessExecutor pe = new ProcessExecutor();   
        ProcessBuilder pb = new ProcessBuilder("bash","-c","zcat " + viewRoot + 
                                                ".labellog.emd.gz | grep 'FILE fusionapps' | egrep '.*\\.task' " +
            "| grep -vi '/noship/' | cut -f2 -d ' '");
        pe.runProcess(pb, "/tmp/bpmTaskList.txt", false); 
        
        BufferedReader reader = new BufferedReader(new FileReader("/tmp/bpmTaskList.txt"));
        writer = new BufferedWriter(new FileWriter("soaBpmTaskNameConflicts.csv"));
        writer.write("FAMILY,MODULE,PRODUCT,FILENAME, BPM TASK NAME, CONFLICTING TASK FILES\n");
        
        SoaBpmTaskNameConflicts scanner = new SoaBpmTaskNameConflicts();
        
        String line = "";
        while((line = reader.readLine()) != null) {
            String taskFile = line.trim();
            scanner.processFile(taskFile);
        }
        
        reader.close();
        scanner.printDuplicates();
        writer.close();
        
        scanner.dumpToAngrybirds(series);
        FamilyModuleHelper.printScanDuration(start,"SoaBpmTaskNameConflicts");
        
    }
    
    private void printDuplicates() throws Exception{
        
        for(Iterator<String> it = taskNames.keySet().iterator(); it.hasNext();) {
            
            String taskName = it.next();
//            if(taskName.equals("ApproversTask") || taskName.equals("PostApprovalProcessTask") ||
//                taskName.equals("SendFYI") || taskName.equals("ApprovalRequest"))
//                continue;
            ArrayList<String> taskFiles = taskNames.get(taskName);
            if(taskFiles.size() > 1) {
                String fileName = taskFiles.get(0);
                String fileConflicts = "";
                for(int i = 1; i < taskFiles.size(); i++)
                    fileConflicts += taskFiles.get(i) + " ";
                boolean skip = checkConflictingFamilies(taskFiles);
                if(!skip)
                    writer.write(getFileNameDetails(fileName) + taskName + "," + fileConflicts + "\n");
            }
        }
    }
    
    private boolean checkConflictingFamilies(ArrayList<String> taskFiles) {
        
        HashSet<String> families = new HashSet<String>();
        
        for(int i =0; i < taskFiles.size();i++){
            String taskFile = taskFiles.get(i);
            String family = getFamily(taskFile);
            families.add(family);            
        }
        if(families.size() > 1)
            return false;
        else
            return true;
                                                   
    }
    
    private void dumpToAngrybirds(String series) throws Exception {
        
         Connection con =
          DriverManager.getConnection("jdbc:oracle:thin:codescan/codescan@angrybirds.oracle.com:1521:codescan");
        PreparedStatement insert =
          con.prepareStatement("INSERT INTO codescan_results (SERIES,LABEL,FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION) VALUES (?,?,?,?,?,?,?,?,?)");     
        PreparedStatement delete =
          con.prepareStatement("DELETE FROM codescan_results where issuetype = 'SoaBpmTaskNameConflicts' and series='" + series + "'");
        delete.execute();   
        
        BufferedReader reader = new BufferedReader(new FileReader("soaBpmTaskNameConflicts.csv"));
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
            insert.setString(7,"SoaBpmTaskNameConflicts");
            insert.setString(8,parts[4].trim()); //SUB-Issue -> Task Name
            insert.setString(9,parts[5].trim()); //Description -> conflicting files
            insert.addBatch();
        }
        
        insert.executeBatch();
        insert.close();
        con.commit();
        con.close();  
    }
}
