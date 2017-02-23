package oracle.apps.sqlanalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;

import java.util.HashSet;
import java.util.Iterator;

import oracle.xml.parser.v2.XMLDocument;

public class VCPerformanceNewPremerge {
    
    static ViewCriteriaUIPerformance vcScanner = new ViewCriteriaUIPerformance();
    static HashSet<String> viewFilesInTransaction = new HashSet<String>();
    
    public static void main(String[] args) throws Exception {
        
        String[] files = args[0].trim().split("\n");
        String ade_view_root = args[1].trim();            
         
        HashSet<String> projectDirs = new HashSet<String>();   
         
        vcScanner.writer = new BufferedWriter(new FileWriter("VC_scan_UI_Performance.csv"));   
        vcScanner.writer1 = new BufferedWriter(new FileWriter("VC_scan_UI_Performance_noVC.csv"));
        vcScanner.generateExemptionList();
        vcScanner.writer1.write("Family,Module,Product,Filename,Label,ViewName,Issue,Jsff,Model,Component\n");
        vcScanner.writer.write("Family,Module,Product,Filename,Label,ViewName, ViewAttribute," +
          "Column Name, Table Name, IsView, ResolvedTableCol, Indexed Table, NumRows, NumBlocks, Column Type, Index, Column Position, " +
            "VC Required, Is VC Case-insensitive?, IsQueriable?, RenderMode, VCR:VCI, " +
            "UI File, modelValue, componentName\n");
        
        vcScanner.function_indexColumns = SqlIndexFinder.readIndexesFromFile("function");
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
        vcScanner.writer1.close();
        
        BufferedReader reader = new BufferedReader(new FileReader("VC_scan_UI_Performance_noVC.csv"));
        vcScanner.writer1 = new BufferedWriter(new FileWriter("VC_scan_UI_Performance_noVC_rows.csv"));
        vcScanner.writer1.write("Family,Module,Product,Filename,Label,ViewName,Issue," +
                "UI File,Model,Component, ListOfTables, MaxRows, Blocks, TableWithMaxRows,WHEREContainsBinds\n");
        
        String line = reader.readLine();
        while((line = reader.readLine()) != null){
            
            String[] parts = line.split(",");
            String fileName = ade_view_root + "/" + parts[3].trim();       
            XMLDocument voXml = null;
            if(parts[3].trim().startsWith("oracle"))
                voXml = vcScanner.voXmlsFromOtherJars.get(parts[3].trim());
            //ViewCriteriaHelper.getMaxRows(fileName,writer1,line,voXml, exemptions,currentVCViolations);
            ViewCriteriaHelper.getMaxRows(fileName,vcScanner.writer1,line,voXml,new HashSet<String>(),new HashSet<String>());
        }
        
        vcScanner.writer1.close();
        
        BufferedWriter outputFileWriter = new BufferedWriter(new FileWriter("vc_perf_scan.txt"));
        
        reader = new BufferedReader(new FileReader("VC_scan_UI_Performance.csv"));
        line = reader.readLine();
        boolean hasViolation = false;
        while((line = reader.readLine()) != null){
            String[] parts = line.split(",");
            if(parts.length < 25) continue;              
            
            String fileName = parts[22].trim();
            if(viewFilesInTransaction.contains(fileName)){
                hasViolation = true;
                if(line.contains("savedSearch"))
                    outputFileWriter.write("Issue: SavedSearchBadViewCriteriaItems\n");
                else
                    outputFileWriter.write("Issue: BadViewCriteriaItems\n");
                outputFileWriter.write("VO FileName: " + parts[3] + "\n");
                outputFileWriter.write("ViewAttribute: " + parts[6].trim() + "\n");
                outputFileWriter.write("ViewCriteria Name: " + parts[21] + "\n");
                outputFileWriter.write("Table Name: " + parts[8] + "\n");
                outputFileWriter.write("Column Name: " + parts[7] + "\n");
                outputFileWriter.write("Column position in index: " + parts[16] + "\n");
                outputFileWriter.write("UI File: " + parts[22] + "\n");  
                outputFileWriter.write("Model value: " + parts[23] + "\n"); 
                outputFileWriter.write("Component: " + parts[24] + "\n");
                outputFileWriter.write("Description:" + "Required and Selectively Required ViewCriteria items " +
                    "used in query panels and LOVs should be backed by proper indexes\n\n");
            }
        }
        
        reader.close();
        
        if(hasViolation)
            outputFileWriter.write("\n\nPlease see http://myforums.oracle.com/jive3/thread.jspa?threadID=871762&tstart=0 " +
                "for description of the issue and resolution.\n\n\n");
        
        reader = new BufferedReader(new FileReader("VC_scan_UI_Performance_noVC_rows.csv"));
        line = reader.readLine();
        hasViolation = false;
        while((line = reader.readLine()) != null){
            String[] parts = line.split(",");
            if(parts.length < 15) continue;              
            
            String fileName = parts[7].trim();
            if(viewFilesInTransaction.contains(fileName)){
                hasViolation = true;
                if(line.contains("savedSearch"))
                    outputFileWriter.write("Issue: SavedSearchNoViewCriteria\n");
                else
                    outputFileWriter.write("Issue: NoViewCriteria\n");
                outputFileWriter.write("VO FileName: " + parts[3] + "\n");
                outputFileWriter.write("UI File: " + parts[7] + "\n");  
                outputFileWriter.write("Model: " + parts[8] + "\n");
                outputFileWriter.write("Component: " + parts[9] + "\n");
                outputFileWriter.write("Description:" + parts[6] + "\n\n");
            }
        }
        
        reader.close();        
        
        if(hasViolation)
            outputFileWriter.write("\n\nPlease see http://myforums.oracle.com/jive3/thread.jspa?threadID=871763&tstart=0" +
                " for description of the issue and resolution.\n\n");
               
        outputFileWriter.close();
     
    }
    
    private static boolean fileOfInterest(String sName) {
        if(sName == null || sName.equals(""))
            return false;
        
        if(sName.toLowerCase().contains("/noship/") 
           || sName.toLowerCase().contains("/test/")
            || sName.toLowerCase().contains("servicetest")
            || sName.toLowerCase().contains("datasecuritytest")
            || sName.toLowerCase().contains("securitypublictest")
            || sName.toLowerCase().contains("/publicservicetest")
            || sName.toLowerCase().contains("/publicmodeltest")
            || sName.toLowerCase().contains("/protectedmodeltest")
            || sName.toLowerCase().contains("/publicuitest")
            || sName.toLowerCase().contains("/uimodeltest")
            || sName.toLowerCase().contains("/structuretest")
            || sName.toLowerCase().contains("/modeltest")
            || sName.toLowerCase().contains("/uitest")
            || sName.toLowerCase().contains("/testui")
            || sName.toLowerCase().contains("/setest")
            || sName.toLowerCase().contains("/protecteduimodeltest")
            || sName.toLowerCase().contains("/testviewcontroller")
            || sName.toLowerCase().contains("/flextest")
            || sName.toLowerCase().contains("/uimodeler-launch-test")
            || sName.toLowerCase().contains("/publicuibasetest")
            || sName.toLowerCase().contains("/uipickertest"))
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
