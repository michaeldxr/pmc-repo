package oracle.apps.sqlanalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.HashSet;
import java.util.Iterator;

import oracle.apps.helpers.FamilyModuleHelper;

public class UISearchesPremergeChecker_new {
    static ViewCriteriaUsedInUInew vcScanner = new ViewCriteriaUsedInUInew();
    static HashSet<String> viewFilesInTransaction = new HashSet<String>();
           
    public static void main(String[] args) throws Exception {
        
        //String[] files = args[0].trim().split("\n");
        String ade_view_root = args[1].trim();
        String filelistpath=args[0].trim();
        String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root);
         
        HashSet<String> projectDirs = new HashSet<String>();   
         
        vcScanner.writer = new BufferedWriter(new FileWriter("UI_Searches.csv"));   
        vcScanner.generateExemptionList();
        vcScanner.writer.write("Family,Module,Product,Filename,Label, Component Type, Model Value," +
          "VO file, ViewCriteria, Description,View Attribute, Table Name, Column Name, " +
            "Resolved Table Column, Column Type, Index, VC_Required,Col Position, Case-Sensitive, Category\n");
        
        vcScanner.function_indexColumns = SqlIndexFinder.readIndexesFromFile("caseInsensitive");
        vcScanner.default_indexColumns = SqlIndexFinder.readIndexesFromFile("default");
        SqlIndexFinder.readColPositionsFromFile();
        SqlIndexFinder.readTableDataFromFile();
        SqlIndexFinder.readViewDataFromFile();
        
        boolean familySet = false;
        
        for(int i = 0; i < files.length; i++) {
            String filePath = ade_view_root + "/" + files[i].trim();
            File f = new File(filePath);
            if(!f.exists())
                continue;
            if(fileOfInterest(filePath)) {
               viewFilesInTransaction.add(files[i].trim());
               if(!familySet){
                   vcScanner.sCrawlDir = files[i].trim();
                   vcScanner.m_family = ViewCriteriaHelper.getFamily(files[i].trim());
               }
               String jprDir = VCPremergeChecker.findProjectDirectory(filePath);
               projectDirs.add(jprDir);
            }          
        }          
        
        if(projectDirs.size() > 0)             
          crawlDirectories(projectDirs);          
        
        vcScanner.processFiles();
        
        vcScanner.writer.close();
        
        BufferedWriter writer1 = new BufferedWriter(new FileWriter("UI_Searches1.csv")); 
        writer1.write("Family,Module,Product,Filename,Label, Component Type, Model Value," +
          "VO file, ViewCriteria, Description,View Attribute, Table Name, Column Name, " +
            "Resolved Table Column, Column Type, Index, VC_Required,Col Position, Case-Sensitive, Category, Original LOV VO Usage\n");
        
        BufferedReader reader = new BufferedReader(new FileReader("UI_Searches.csv"));
        String line = reader.readLine();
        while((line=reader.readLine()) != null) {
            String[] parts = line.split(",");
            if(parts.length > 10){
                String lovVo = parts[7].trim();
                String vcUsed = parts[8].trim();
                String origVOLOVUses = vcScanner.LovVOToOrigVoMapping.get(lovVo+";"+vcUsed);
                if(origVOLOVUses == null)
                    origVOLOVUses="NA";
                writer1.write(line + "," + origVOLOVUses + "\n");
            }            
        }
        reader.close();
        writer1.close();
        
        BufferedWriter outputFileWriter = new BufferedWriter(new FileWriter("ui_searches_scan.txt"));
        
        reader = new BufferedReader(new FileReader("UI_Searches1.csv"));
        line = reader.readLine();
        boolean hasViolation = false;
        while((line = reader.readLine()) != null){
            
            String[] parts = line.split(",");
            if(parts.length < 20) 
                continue;         
            
            String fileName = parts[3].trim();
            
            if(viewFilesInTransaction.contains(fileName)){
                hasViolation = true;
                outputFileWriter.write("\nIssue: UISearch");
                outputFileWriter.write("\nVO FileName: " + parts[7]);
                outputFileWriter.write("\nViewAttribute: " + parts[10]);
                outputFileWriter.write("\nViewCriteria Name: " + parts[8]);
                outputFileWriter.write("\nTable Name: " + parts[11]);
                outputFileWriter.write("\nColumn Name: " + parts[12]);
                outputFileWriter.write("\nResolved TableCol: " + parts[13]);
                outputFileWriter.write("\nUI File: " + fileName);  
                outputFileWriter.write("\nModel value: " + parts[6]); 
                outputFileWriter.write("\nComponent: " + parts[5]);
                outputFileWriter.write("\nCol Type: " + parts[14]); 
                outputFileWriter.write("\nCol Index: " + parts[15]); 
                outputFileWriter.write("\nVC Required: " + parts[16]);
                if(parts.length > 20) 
                    outputFileWriter.write("\nOriginal VO where LOV is defined & 'LOVUses': " + parts[20]);
                
                outputFileWriter.write("\nDescription:" + "UI Search should be case insensitive\n\n");
            }
        }
        
        reader.close();
        
        if(hasViolation)
            outputFileWriter.write("\n\nPlease see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/UISearchCaseSensitivity " +
                "for description of the issue and resolution.\n\n\n");
               
        outputFileWriter.close();
     
    }
    
    private static boolean fileOfInterest(String sName) {
          if(!FamilyModuleHelper.isPathOfInterest(sName))
              return false;            
        
        if (sName.endsWith(".jspx"))
            return true;
        if (sName.endsWith(".jsff"))
            return true;
        
        return false;
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
                  vcScanner.processFile(listOfFiles[i],false);            
          }
          else if(listOfFiles[i].isDirectory()) 
              crawlDirectory(listOfFiles[i].getPath());            
          else 
              System.out.println("ERROR:  node is neither file or directory: "+listOfFiles[i]);            
      }
    }
    
    private static void crawlDirectories(HashSet<String> projectDirectories) throws Exception{
      
        for(Iterator it = projectDirectories.iterator(); it.hasNext();){
          String path = (String)it.next();
          crawlDirectory(path);      
        }
    }
}
