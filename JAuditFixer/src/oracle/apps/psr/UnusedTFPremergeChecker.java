package oracle.apps.psr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.HashSet;
import java.util.Iterator;

import oracle.apps.helpers.FamilyModuleHelper;

public class UnusedTFPremergeChecker {
    static UnusedTaskFlows scanner = new UnusedTaskFlows();
    static   HashSet<String> pageDefs = new HashSet<String>();
        
    public static void main(String[] args) throws Exception {
       
      //String[] files = args[0].trim().split("\n");
       String ade_view_root = args[1].trim();
       String filelistpath=args[0].trim();
       String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root);
                 
       HashSet<String> projectDirs = new HashSet<String>();   
       
       UnusedTaskFlows.writer = new BufferedWriter(new FileWriter("unusedTFs_scan_temp.csv") );
       
       //rctScanner.premergeMode = true;
       
       for(int i = 0; i < files.length; i++) {
           String filePath = ade_view_root + "/" + files[i].trim();
           File f = new File(filePath);
           if(!f.exists())
               continue;
           if(filePath.endsWith("PageDef.xml") || filePath.endsWith(".jsff")) {
              pageDefs.add(files[i].trim());
              String jprDir = findProjectDirectory(filePath);
              projectDirs.add(jprDir);
           }          
       }          
       
       if(projectDirs.size() > 0) {
         scanner.generateExemptionList();      
         crawlDirectories(projectDirs);   
         scanner.processFiles(false);
       }
       
       //UnusedTaskFlows.writer.write("\n\n\nRowCountThresholds for the above tables should be set to -1\n\n");
       UnusedTaskFlows.writer.close();
       
       //REport only violations in files in the current transaction
       
         BufferedWriter outputFileWriter = new BufferedWriter(new FileWriter("unusedTFs_scan.csv"));
         
         BufferedReader reader = new BufferedReader(new FileReader("unusedTFs_scan_temp.csv"));
         String line = "";
         boolean hasViolation = false;
         while((line = reader.readLine()) != null){
             String[] parts = line.split(",");
             if(parts.length < 14)
                 continue;
             
             String fileName = parts[3].trim();
             String uiFile = parts[5].trim();
             if(pageDefs.contains(fileName) || pageDefs.contains(uiFile)){
                 hasViolation = true;
                 outputFileWriter.write(fileName + " -> " + "TaskFlow " + parts[7].trim() + " is not used on the corresponding jsff page.\n\n");
             }
         }
         if(hasViolation)
             outputFileWriter.write("\n\nPlease see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/UnusedTaskFlows\n");
         
         outputFileWriter.close();
         reader.close();
     }
    
    private static String findProjectDirectory(String filePath) throws Exception {
        
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
    
    private static void crawlDirectories(HashSet<String> projectDirectories){
      
        for(Iterator it = projectDirectories.iterator(); it.hasNext();){
          String path = (String)it.next();
          crawlDirectory(path);      
        }
    }
    
    private static void crawlDirectory(String path){
        
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
              if(fileOfInterest(sName)) 
                  scanner.processFile(listOfFiles[i], false);            
          }
          else if(listOfFiles[i].isDirectory()) 
              crawlDirectory(listOfFiles[i].getPath());            
          else 
              System.out.println("ERROR:  node is neither file or directory: "+listOfFiles[i]);            
      }
    }
    
    private static boolean fileOfInterest(String sName){
        
      if (sName.endsWith(".cpx"))
          return true;
      if (sName.endsWith("PageDef.xml"))
        return true;
      if (sName.endsWith(".jspx"))
        return true;
      if (sName.endsWith(".jsff"))
        return true;
      if(sName.contains("PageDef"))
      if (sName.matches(".*PageDef[0-9]*\\.xml"))
          return true;
      return false;
    }
}
