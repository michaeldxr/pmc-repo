package oracle.apps.rowCountAnalyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Iterator;
import oracle.apps.helpers.FamilyModuleHelper;

public class RCTPremergeChecker {
   
  static RowCountThresholdNew rctScanner = new RowCountThresholdNew();
  static   HashSet<String> pageDefs = new HashSet<String>();
    
  public static void main(String[] args) throws Exception {
     //String[] files = args[0].trim().split("\n");
     String ade_view_root = args[1].trim();
     String filelistpath=args[0].trim();
     String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root);   
     HashSet<String> projectDirs = new HashSet<String>();   
     RowCountThresholdNew.writer = new BufferedWriter(new FileWriter("rowCountThreshold_scan.csv") );
     RowCountThresholdNew.writer.write("Family,Module,Product,Filename,Label,UX File,Iterator Id, Tree Id, TableId,RCT\n");       
     RowCountThresholdNew.mode = "premerge";
       
     for(int i = 0; i < files.length; i++) {
         String filePath = ade_view_root + "/" + files[i].trim();
         File f = new File(filePath);
         if(!f.exists())
             continue;
         if(filePath.endsWith("PageDef.xml")) {
            pageDefs.add(files[i].trim());
            String jprDir = findProjectDirectory(filePath);
            projectDirs.add(jprDir);
         }          
     }          
     
     if(projectDirs.size() > 0) {
       rctScanner.generateExemptionList();      
       crawlDirectories(projectDirs);   
       rctScanner.processFiles();
     }
     
     RowCountThresholdNew.writer.write("\n\n\nRowCountThresholds for the above tables should be set to -1\n\n");
     RowCountThresholdNew.writer.close();
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
                rctScanner.processFile(listOfFiles[i], false);            
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
  
  public static boolean processPageDef(String path){
      
      int index = -1;
      if(path.contains("fusionapps/"))
         index = path.indexOf("fusionapps/");
      if(path.contains("fsm/"))
         index = path.indexOf("fsm/");
      if(path.contains("atgpf/"))
         index = path.indexOf("atgpf/");
      
      String pathFromFusionapps = path;
      if(index != -1)
         pathFromFusionapps = path.trim().substring(index);
      
      if(pageDefs.contains(pathFromFusionapps)){
        //System.out.println("Processing page def:" + path);
        return true;
      }
    
    return false;
  }
}
