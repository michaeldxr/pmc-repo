package oracle.apps.warnings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.HashSet;

import java.util.Iterator;

import oracle.apps.helpers.FamilyModuleHelper;

public class SDOPremergeChecker {
    
    static NonSerializableSdoInterfaces sdoScanner = new NonSerializableSdoInterfaces();
    static HashSet<String> javaFilesInTransaction = new HashSet<String>();
          
    public static void main(String[] args) throws Exception {
       
     // String[] files = args[0].trim().split("\n");
       String ade_view_root = args[1].trim();
       String filelistpath=args[0].trim();
       String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root);
       
       HashSet<String> projectDirs = new HashSet<String>();   
       
       NonSerializableSdoInterfaces.outputFileWriter = new BufferedWriter(new FileWriter("sdo_scan_temp.csv") );       
       sdoScanner.generateExemptionList();
       
       for(int i = 0; i < files.length; i++) {
           String filePath = ade_view_root + "/" + files[i].trim();
           File f = new File(filePath);
           if(!f.exists())
               continue;
           if(filePath.endsWith(".java")) {
              javaFilesInTransaction.add(files[i].trim());
              String jprDir = findProjectDirectory(filePath);
              projectDirs.add(jprDir);
           }          
       }          
       
       if(projectDirs.size() > 0) 
         crawlDirectories(projectDirs);          
       
       NonSerializableSdoInterfaces.outputFileWriter.close();
       
       BufferedWriter outputFileWriter = new BufferedWriter(new FileWriter("sdo_scan.csv"));
       
       BufferedReader reader = new BufferedReader(new FileReader("sdo_scan_temp.csv"));
       String line = "";
       boolean hasViolation = false;
       while((line = reader.readLine()) != null){
           String[] parts = line.split(",");
           if(parts.length < 8)
               continue;
           
           String fileName = getPathFromViewRoot(parts[7].trim());
           if(javaFilesInTransaction.contains(fileName)){
               hasViolation = true;
               outputFileWriter.write(fileName + " -> " + "SDO Interface should implement Serializable.\n\n");
           }
       }
       if(hasViolation)
           outputFileWriter.write("Please see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/SDOInterfaceNotSerializable\n");
       
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
              if(sdoScanner.fileOfInterest(sName)) 
                  sdoScanner.processFile(listOfFiles[i], false);            
          }
          else if(listOfFiles[i].isDirectory()) 
              crawlDirectory(listOfFiles[i].getPath());            
          else 
              System.out.println("ERROR:  node is neither file or directory: "+listOfFiles[i]);            
      }
    }
    
    private static String getPathFromViewRoot(String filePath) {
        int index = filePath.indexOf("fusionapps/");
        if(index >= 0)
            return filePath.substring(index);
        else return filePath;
    }
}
