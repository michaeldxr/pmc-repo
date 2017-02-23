package oracle.apps.ppr;


import au.com.bytecode.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.ppr.file_type.Application;
import oracle.apps.ppr.file_type.Project;
import oracle.apps.ppr.file_type.ViewObject;
import oracle.apps.utility.LRGUtil;


public class ExemptionList{
    
  private static int releaseNum=0;
  
  public static void main(String[] args) {
    
    if(args.length != 1 || args[0] == null || args[0].isEmpty()){
      System.out.println("USAGE: JarAnalyzerLRG <series>");
      System.exit(1);
    }
    String series = args[0]; 

    
//    if(!series.contains("FSM")){
//        String release= FamilyModuleHelper.getRelease(series);
//        
//        try{              
//            releaseNum = Integer.parseInt(release);
//        }catch(NumberFormatException e){
//            releaseNum=0;
//        }
//        
//        if(releaseNum != 9){
//            System.out.println("WARNING: you are trying to run votuning on series " +series + " , which is not a Release 9 branch. Please run votuning_v3 for R10+.");
//            System.exit(1);
//        }
//    }
    
    ExemptionList analyzer = new ExemptionList();
    
    analyzer.markStartTime();
    String label = LRGUtil.getLatestLabel(series);
    System.out.println("Generating exemption list for UnusedAutoSubmit and NonExistentPartialTrigger scan on series: " + series + ".......");
    
    ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);

    for (int j = 0; j < families.size(); j++) {
        String family = families.get(j);
        String sCrawlDir = LRGUtil.getCrawlDir(series, family, label, "UnusedAutoSubmit");
        System.out.println("Crawling dir '" + sCrawlDir + "' for family: " + family);                
        analyzer.crawlDirectory(sCrawlDir);      
        System.out.println("Done crawling for family "+family+".");
    }
    
    ExemptionListGenerator generator = new ExemptionListGenerator();
    
    System.out.println("Start parsing jar files.....");
    generator.start(Project.s_jlib);
    
    
    System.out.println("Start validating the components.....");
    ScanResultsAnalyzer resultAnalyzer = new ScanResultsAnalyzer(Project.adePathMapping);
    resultAnalyzer.AnalyzeResult();
    
    CSVWriter writer = null;
        
    try {
      System.out.println("uploading exemption list to Angrybirds database....");
      DBWrapper.uploadToDB(resultAnalyzer.al_results,series);
    } catch (Exception e) {
      System.out.println("Failed to close the JDBC connection.");
    }

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
