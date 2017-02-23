package oracle.apps.votuning_v3;

import java.io.File;

import java.sql.SQLException;

import java.util.ArrayList;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.LRGUtil;
import oracle.apps.votuning_v3.file_type.Application;
import oracle.apps.votuning_v3.file_type.Project;


public class JarAnalyzerLRG{
    
  private static int releaseNum=0;
  
  public static void main(String[] args) {
    
    if(args.length != 1 || args[0] == null || args[0].isEmpty()){
      System.out.println("USAGE: JarAnalyzerLRG <series>");
      System.exit(1);
    }
    String series = args[0]; 
    
    if(!series.contains("FSM")){
        String release= FamilyModuleHelper.getRelease(series);
        
        try{              
            releaseNum = Integer.parseInt(release);
        }catch(NumberFormatException e){
            releaseNum=0;
        }
        
        if(releaseNum > 10 || releaseNum < 9){
            System.out.println("WARNING: you are trying to run votuning on series " +series + " , which is not a Release 9 branch nor a Release 10 branch. Please run votuning_v3 for R11+.");
            System.exit(1);
        }
    }
    
    JarAnalyzerLRG analyzer = new JarAnalyzerLRG();
    
    analyzer.markStartTime();
    String label = LRGUtil.getLatestLabel(series);
    System.out.println("Running TableAndLOVTuning_v2 Scan on series: " + series + ".......");
    
    ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
    for (int j = 0; j < families.size(); j++) {
        String family = families.get(j);
        String sCrawlDir = LRGUtil.getCrawlDir(series, family, label, "TableAndLOVTuning_v2");
        System.out.println("Crawling dir '" + sCrawlDir + "' for family: " + family);                
        analyzer.crawlDirectory(sCrawlDir);      
        System.out.println("Done crawling for family "+family+".");
    }
    
    JarAnalyzer finder = new JarAnalyzer();
    
    System.out.println("Start parsing jar files.....");
    finder.start(Project.s_jlib);
    
    System.out.println("Start validating the components.....");
    ScanResultsAnalyzer resultAnalyzer = new ScanResultsAnalyzer(Project.adePathMapping);
    resultAnalyzer.AnalyzeResult();
    
    ArrayList<ScanResult> al_violations = finder.validate(resultAnalyzer.al_results);
    
    al_violations = Exemptions.filterExemptions(al_violations);

    try {
      System.out.println("uploading scan results to Angrybirds database....");
      DBWrapper.uploadToDB(al_violations,series);
    } catch (SQLException e) {
      System.out.println("Failed to close the JDBC connection.");
    }
    
    System.out.println("Start generating output spreadsheet..... ");
    finder.label = series + "_" + label;
    finder.writeExcel(al_violations, "");
    
    LRGUtil.addLabelInfoToAB("TableAndLOVTuning_v2", series, label);   
    analyzer.markEndTime();       
  }
  
  private void crawlDirectory(String path)
  {
      if (path.contains(".ade_path") || path.contains("/classes/") || path.toLowerCase().contains("soa") || path.toLowerCase().contains("ess"))
          return;
      
      File folder = new File(path);
      File[] listOfFiles = folder.listFiles();
      if (listOfFiles == null)
          return;

      for (int i = 0; i < listOfFiles.length; i++)
      {
          if (listOfFiles[i].isFile())
          {
              String sName = listOfFiles[i].getAbsolutePath();
              if (sName.contains(".ade_path"))
                  continue;

              if (fileOfInterest(sName)){
                  processFile(listOfFiles[i].getAbsolutePath());
                  break;
              }
              
          } else if (listOfFiles[i].isDirectory())
          {
              if (isDirOfInterest(listOfFiles[i].getAbsolutePath()))
                  crawlDirectory(listOfFiles[i].getAbsolutePath());
          } else
              System.out.println("ERROR:  node is neither file or directory: " +
                                 listOfFiles[i]);
      }
  }

  private boolean isDirOfInterest(String sName)
  {
        if (sName.contains(".ade_path") || sName.contains("/classes/") || sName.toLowerCase().contains("soa") || sName.toLowerCase().contains("ess"))
          return false;

      if (sName == null || sName.equals(""))
          return false;
      
      if(!FamilyModuleHelper.isPathOfInterest(sName))
          return false;  

      return true;
  }
  
  public boolean fileOfInterest(String sName) 
  {
      if(!FamilyModuleHelper.isPathOfInterest(sName))
          return false;  
      
      if (sName.endsWith(".jws"))
          return true;
      
      return false;
  }

  private void processFile(String absPath) {
    Application.processFile(absPath);
  }
  
  private long start;
  private long end;
  
  public void markStartTime()
  {
      start = System.currentTimeMillis();
  }
  
  public void markEndTime()
  {
      end = System.currentTimeMillis();
      long e = (end - start) / 1000;
      
      System.out.println("Elasped Time is " + e + " seconds.");
  }
}
