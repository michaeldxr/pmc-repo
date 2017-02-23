package oracle.apps.sqlanalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.JprHelper;
import oracle.apps.warnings.NonSerializableSdoInterfaces;

public class VCPremergeChecker {
    static CaseInsensitiveViewCriteria vcScanner = new CaseInsensitiveViewCriteria();
    static HashSet<String> voFilesInTransaction = new HashSet<String>();
    
    public static void main(String[] args) throws Exception {
       
       //String[] files = args[0].trim().split("\n");
       String ade_view_root = args[1].trim();
       String filelistpath=args[0].trim();
         String family = "";
         if(args.length > 2) 
             family = args[2].trim();
         
       String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root);
       HashSet<String> projectDirs = new HashSet<String>();   
       
       CaseInsensitiveViewCriteria.writer = new BufferedWriter(new FileWriter("vc_scan_temp.csv") );    
       vcScanner.m_family = family;
       vcScanner.generateExemptionList();
       vcScanner.caseInsensitive_indexColumns = SqlIndexFinder.readIndexesFromFile("caseInsensitive");
       vcScanner.caseSensitive_indexColumns = SqlIndexFinder.readIndexesFromFile("caseSensitive");
       
       for(int i = 0; i < files.length; i++) {
           String filePath = ade_view_root + "/" + files[i].trim();
           File f = new File(filePath);
           if(!f.exists())
               continue;
           if(filePath !=null && filePath.endsWith("VO.xml")) {
              voFilesInTransaction.add(files[i].trim());
              String jprDir = findProjectDirectory(filePath);
              projectDirs.add(jprDir);
           }          
       }          
       
       if(projectDirs.size() > 0) 
         crawlDirectories(projectDirs);          
       
       CaseInsensitiveViewCriteria.writer.close();
       
        HashMap<String,String> tableColumnsData = SqlIndexFinder.findDataAllCaps(vcScanner.tableColumns);
         
        CaseInsensitiveViewCriteria.writer = new BufferedWriter(new FileWriter("vc_scan_temp1.csv"));
        
         BufferedReader reader = new BufferedReader(new FileReader("vc_scan_temp.csv"));
         
         String line = "";
         
         while((line = reader.readLine()) != null) {
             
           String[] parts = line.split(",");    
           if(parts.length < 16)
               continue;
                
           String tableCol = parts[9].trim() + "," + parts[8].trim();
           String data = tableColumnsData.get(tableCol);
             
           String filePath = parts[3].trim();
           if(filePath.contains("/flex/") || filePath.contains("/publicFlex/"))
               continue;
           
           if(data != null && !data.equals("Not varchar")) {
               line += "," + data;        
               
               if(parts[12].trim().equals("null") || parts[12].trim().equals("Optional"))
                         continue;
               CaseInsensitiveViewCriteria.writer.write(line + "\n");
           }
         }
         CaseInsensitiveViewCriteria.writer.close();
         reader.close();
       
       BufferedWriter outputFileWriter = new BufferedWriter(new FileWriter("vc_scan.csv"));
       
       reader = new BufferedReader(new FileReader("vc_scan_temp1.csv"));
       line = "";
       boolean hasViolation = false;
       while((line = reader.readLine()) != null){
           String[] parts = line.split(",");
           if(parts.length < 16)
               continue;
           
           String fileName = parts[3].trim();
           if(voFilesInTransaction.contains(fileName)){
               hasViolation = true;
               outputFileWriter.write("FileName: " + fileName + "\n");
               outputFileWriter.write("ViewAttribute: " + parts[5].trim() + "\n");
               outputFileWriter.write("ViewCriteria Name: " + parts[15].trim() + ":" + parts[14].trim() + "\n");
               outputFileWriter.write("ViewCriteria IsUpper?: " + parts[13] + "\n");
               outputFileWriter.write("Table Name: " + parts[9] + "\n");
               outputFileWriter.write("Table Column: " + parts[8] + "\n");
               outputFileWriter.write("Column Index: " + parts[11] + "\n");
               outputFileWriter.write("Issue:" + "VC case and column index case are different, so Index will not be utilized\n\n");
           }
       }
       if(hasViolation)
           outputFileWriter.write("\n\nPlease see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/CaseinsensitiveViewCriteria\n");
       
       outputFileWriter.close();
       reader.close();
                                                
     }
    
    
    public static String findProjectDirectory(String filePath) throws Exception {
        
      String folderName = filePath;    
      
      while(true) {
          
        int index = folderName.lastIndexOf("/");
        if(index == -1)
            return folderName;
       
        folderName = folderName.substring(0,index);
        File folder = new File(folderName);
        File[] listOfFiles = folder.listFiles();
        
        if(listOfFiles == null)
            return folderName;
        
        for(int i = 0; i <listOfFiles.length; i++)
        {
          String sName = listOfFiles[i].getName();
          if(sName.contains(".jpr"))
            return folderName;
        }
      }
    //  return folderName;
    }
    
    private static void crawlDirectories(HashSet<String> projectDirectories) throws Exception{
      
        for(Iterator it = projectDirectories.iterator(); it.hasNext();){
          String path = (String)it.next();
          crawlDirectory(path);      
        }
    }
    
    private static void crawlDirectory(String path) throws Exception{
        
      if(path.contains(".ade_path") || path.contains("/classes/"))
            return;
      File folder = new File(path);
      File[] listOfFiles = folder.listFiles();
          
      if(listOfFiles == null)
          return;
      
      for(int i = 0; i <listOfFiles.length; i++)
      {
          if(listOfFiles[i].isFile())             {
              String sName = listOfFiles[i].getPath();          
              if(vcScanner.fileOfInterest(sName)) 
                  vcScanner.processFile(listOfFiles[i]);            
          }
          else if(listOfFiles[i].isDirectory()) 
              crawlDirectory(listOfFiles[i].getPath());            
          else 
              System.out.println("ERROR:  node is neither file or directory: "+listOfFiles[i]);            
      }
    }
}
