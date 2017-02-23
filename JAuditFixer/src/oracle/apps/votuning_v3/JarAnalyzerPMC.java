package oracle.apps.votuning_v3;

import java.io.BufferedWriter;
import java.io.File;

import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;

import java.util.HashSet;

import java.util.Iterator;

import java.util.Set;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.votuning_v3.file_type.Application;
import oracle.apps.votuning_v3.file_type.Project;

public class JarAnalyzerPMC {
  public JarAnalyzerPMC() {
    super();
  }
  
  private static Set<String> h_family= new HashSet<String>();

  public static void main(String[] args) {
    String[] files = args[0].trim().split("\n");
    String adeViewRoot = args[1].trim();
    String label = args[2].trim();
    String family = args[3];
    
    process(files,adeViewRoot,label,family);
    
  }
  
  public static void process(String[] files, String adeViewRoot, String label, String family){
    if(family != null && !family.isEmpty()){
      h_family.add(family.trim().toLowerCase());
    }

    
    HashSet<String> hs = filterAffectedFiles(files, adeViewRoot);
    
    if(hs.size() == 0){
        System.out.println("Neither UI nor xml files found in the txn. Skipping VOTuning_V2 check");
        return;
    }
    
    String release= FamilyModuleHelper.getRelease(label);
    System.out.println("Release: " + release);
    
    int releaseNum;
    
    try{              
        releaseNum = Integer.parseInt(release);
        if (releaseNum < 9) //This check was mandated post release 9 only, hence this check is necessary to avoid the current rel7 and rel8 branches from failing
            return;              
    }catch(NumberFormatException e){
        System.out.println("Could not determine release. Skipping VOTuning_V2 check...");
        return;
    }
    
    JarAnalyzerPMC analyzer = new JarAnalyzerPMC();
        if (label.contains("FSM")) {
            String path = adeViewRoot + "/fsm";
            System.out.println("Processing " + path);
            analyzer.crawlDirectory(path);
        } else {
            for (String f : h_family) {
                String path =
                    adeViewRoot + "/fusionapps/" + f + "/components";
                System.out.println("Processing " + path);
                analyzer.crawlDirectory(path);
            }
        }
    
    JarAnalyzer finder = new JarAnalyzer();
    
    System.out.println("Start parsing jar files.....");
    finder.start(Project.s_jlib);
    
    System.out.println("Start validating the components.....");
    ScanResultsAnalyzer resultAnalyzer = new ScanResultsAnalyzer(Project.adePathMapping);
    resultAnalyzer.AnalyzeResult();
    
    ArrayList<ScanResult> al_violations = finder.validate(resultAnalyzer.al_results);
    
    al_violations = Exemptions.filterExemptions(al_violations);
    
    al_violations = analyzer.isAffectedFiles(al_violations, hs);


    try {
      System.out.println("Writing the scan results to file.....");
      writeToFile(al_violations);
    } catch (Exception e) {
      System.out.println("Error: Failed to write votuning scan results to disk. ");
    }
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

  private ArrayList<ScanResult> isAffectedFiles(ArrayList<ScanResult> al_violations, HashSet<String> files) {
    
    ScanResult r;
    
    Iterator<ScanResult> i = al_violations.iterator();
    
    while(i.hasNext()){
      r = i.next();
      if(files.contains(r.getPage()) || files.contains(r.getPageDef()) || files.contains(r.getAM()) || files.contains(r.getVO()) || files.contains(r.getVa_VO())){
        continue;
      }else{
        i.remove();
      }
    }
    return al_violations;
  }

  private static HashSet<String> filterAffectedFiles(String[] files, String adeViewRoot) { 
    
      Pattern p = Pattern.compile("fusionapps/(.+?)/components");
      Matcher m;
      
      HashSet<String> hs = new HashSet<String>();
      for (String path : files) {
          if (path == null || path.isEmpty())
              continue;

          path = path.trim();

          String lowerCaseName = path.toLowerCase();
          if (lowerCaseName.contains("/noship/") ||
              lowerCaseName.contains("/test/") ||
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
              lowerCaseName.contains("uipickertest") ||
              lowerCaseName.contains("/dist/") ||
              lowerCaseName.contains("/fsm/dev"))
              continue;

          if (path.endsWith(".jsff") || path.endsWith(".jspx") ||
              path.endsWith("PageDef.xml") || path.endsWith("AM.xml") ||
              path.endsWith("VO.xml") || path.endsWith("EO.xml")){
                hs.add(adeViewRoot + "/" + path);
                m = p.matcher(path);
                if(m.find())
                  h_family.add(m.group(1).trim().toLowerCase());
              }
      }
      return hs;
  }

  private static void writeToFile(ArrayList<ScanResult> al_violations) throws Exception {
    
    BufferedWriter writer = new BufferedWriter(new FileWriter("votuning.txt"));
    
    for(ScanResult r : al_violations){
      if(writer != null){
          writer.write("\n\nFileName: " + r.getPage());
          writer.write("\nComponentType: " + r.getType());
          writer.write("\nComponentId: " + r.getTableId());
          writer.write("\nDescription: " + r.getDescription());
      }
    }
    
    writer.write("\n\nPlease see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/NewVOTuning" +
        " for description of the issue and resolution.\n\n");
    writer.close();   
  }
  

}
