package oracle.apps.ppr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import oracle.apps.utility.JoesBaseClass;
import oracle.apps.ppr.file_type.BC4J_XCFG;
import oracle.apps.ppr.file_type.DataControl;
import oracle.apps.ppr.file_type.EntityObject;
import oracle.apps.ppr.file_type.PageDef;
import oracle.apps.ppr.file_type.PageDefUsages;
import oracle.apps.ppr.file_type.PageMap;
import oracle.apps.ppr.file_type.UIPageTunableElements;
import oracle.apps.ppr.file_type.ViewObject;
import oracle.apps.ppr.file_type.ViewUsage;
import oracle.apps.ppr.file_type.FusionFileInfo;


public class ExemptionListGenerator extends JoesBaseClass {
  HashMap<String, FusionFileInfo> m_duplicatedFusionContent =
    new HashMap<String, FusionFileInfo>();
  HashMap<String, FusionFileInfo> m_fusionContent =
    new HashMap<String, FusionFileInfo>();

  ArrayList<String> jar_list = new ArrayList<String>();

  HashMap<String, ViewObject> viewObject_map =
    new HashMap<String, ViewObject>();

  public static ArrayList<ViewObject> viewObject_list =
    new ArrayList<ViewObject>();
  public static ArrayList<EntityObject> entityObject_list =
    new ArrayList<EntityObject>();
  public static ArrayList<ViewUsage> viewUsage_list =
    new ArrayList<ViewUsage>();
  
  public static String label;


  public ExemptionListGenerator() {
    super(JoesBaseClass.CRAWL_TYPE.JAR);
  }

//  public static void main(String[] args) {
//    ExemptionListGenerator ja = new ExemptionListGenerator();
//    ja.markStartTime();
//
//    if (args.length != 5 || args[1] == null || args[2] == null || args[3] == null || args[1].equals("") || args[2].equals("")|| args[3].equals("")) {
//      System.out.println("USAGE: jarAnalyzerForADFTuning.sh <tempSerialiazationDir> <outputDir>");
//      System.exit(1);
//    }
//
//    if(args[0] == null || args[0].isEmpty()){
//      System.out.println("$VIEW_LABEL is not defined, please run the script inside a view");
//      System.exit(1);
//    }
//    
//    if(args[4] == null || args[4].isEmpty() || !(args[4].equals("fix") || args[4].equals("scan"))){
//      System.out.println("Please spicify the running mode (scan/fix)");
//      System.exit(1);
//    }
//    
//    String mode = args[4].trim();
//    label = args[0];
//    String topSerTempDir = args[1];
//    String outputDir = args[2];
//    
//    String currentDir = null;
//    if(args[3].indexOf("/fusionapps") != -1)
//      currentDir = args[3].substring(args[3].indexOf("/fusionapps"));
//    else if(args[3].indexOf("/fsm") != -1)
//      currentDir = args[3].substring(args[3].indexOf("/fsm"));
//    else{
//      System.out.println("Please run this script under /fusionapps or /fsm.");
//      System.exit(1);
//    }
//
//      
//    
//    File f = new File(topSerTempDir);
//    if(!f.isDirectory() || !f.canRead() || !f.canWrite()){
//      System.out.println("ERROR: The directory specified to store serialization files is either unreadable or unwritable.");
//      System.exit(1);
//    }
//    
//    f = new File(outputDir);
//    if(!f.exists()){
//      System.out.println("ERROR: output dir does not exist.");
//      System.exit(1);
//    }else if (!f.canWrite()){
//      System.out.println("ERROR: output dir is unwritable.");
//      System.exit(1);
//    }
//    
//    f = new File(topSerTempDir + "/" + label);
//    // if the directory does not exist, create it
//    if (!f.exists()) {
//        System.out.println("ERROR: temp serialization dir does not exist.");
//        System.exit(1);
//    }else if(!new File(f.getAbsolutePath()+"/jlib.ser").exists() || !new File(f.getAbsolutePath()+"/JarSrcMapping.ser").exists()){
//        System.out.println("ERROR: Serialtion files don't exist");
//        System.exit(1);   
//    }
//    
//    
//    HashSet<String> s_jlib = null;
//    FileInputStream fis;
//    
//    try {
//      fis = new FileInputStream(topSerTempDir + "/" + label + "/jlib.ser");
//      ObjectInputStream ois = new ObjectInputStream(fis);
//      s_jlib = (HashSet<String>)ois.readObject();
//      ois.close();
//    } catch (Exception e) {
//      System.out.println("jlib path serialization file not found.");
//      System.exit(1);
//    }
//      
//    System.out.println("Start parsing jar files.....");
//    ja.start(s_jlib);
//
//    HashMap<String,String> jarSrcMapping = null;
//    try{
//      fis = new FileInputStream(topSerTempDir + "/" + label + "/JarSrcMapping.ser");
//      ObjectInputStream ois = new ObjectInputStream(fis);
//      jarSrcMapping = (HashMap<String, String>)ois.readObject();
//      ois.close();
//    } catch (Exception e) {
//      System.out.println("Jar-Source Mapping serialization file not found");
//    }
//      
//    System.out.println("Start validating the components.....");
//    ScanResultsAnalyzer analyzer = new ScanResultsAnalyzer(jarSrcMapping);
//    analyzer.AnalyzeResult();
//    
//
//    ArrayList<ScanResult> al_violations = ja.validate(analyzer.al_results);
//    
//    al_violations = Exemptions.filterExemptions(al_violations);
//    
//    Iterator<ScanResult> iter = al_violations.iterator();
//    while(iter.hasNext()){
//      if( currentDir != null && !isUnderCurrentDir(iter.next(), currentDir))
//        iter.remove();
//    }
//
//    if(mode.equals("scan")){
//      System.out.println("Start generating output spreadsheet..... ");
//      ja.writeExcel(al_violations, outputDir);
//    }
//    
//    if(mode.equals("fix")){
//      System.out.println("Start fixing the violations..... ");
//      try {
//        new VOTuningFixer().fixAll(al_violations, outputDir);
//      } catch (Exception e) {
//        e.printStackTrace();
//      }
//    }
//    ja.markEndTime();
//  }

  public void start(HashSet<String> s_jlib) {
      Iterator<String> iterator = s_jlib.iterator();
      while (iterator.hasNext()) {
        String dir = iterator.next();
        if(dir.endsWith("jlib")){
          System.out.println("Parsing " + dir);
          crawlDirectory(dir, false);
        }
      }
      
//      if(jdevHome != null && !jdevHome.isEmpty()){
//        System.out.println("Parsing applcore: " + jdevHome + "/atgpf/modules");
//        crawlDirectory(jdevHome + "/atgpf/modules", doADE);
//      }
  }
  
  @Override
  protected void processFile(File fName, boolean bDoADE) {
    if (fName.getAbsolutePath().endsWith("Test.jar"))
      return;

    if (!fName.getAbsolutePath().contains("/jlib/"))
      return;

    jar_list.add(fName.getAbsolutePath());

    try {
      ZipFile jar = new ZipFile(fName);
      Enumeration entries = jar.entries();

      while (entries.hasMoreElements()) {
        ZipEntry ze = (ZipEntry)entries.nextElement();
        String name = ze.getName();

        FusionFileInfo newFFI = new FusionFileInfo(jar, ze);
        FusionFileInfo ffi = m_fusionContent.get(newFFI.getDigest());

        if (ffi == null) {
          m_fusionContent.put(newFFI.getDigest(), newFFI);
        } else {
          m_duplicatedFusionContent.put(newFFI.getDigest(), newFFI);
          //                    System.out.println("****************************************************");
          //                    System.out.println("Skipping file as it was already processed");
          //                    System.out.println(newFFI.toString());
          //                    System.out.println(ffi.toString());
          //                    System.out.println("****************************************************");
          continue;
        }

        FILE_TYPE t = getFileType(jar, ze);

        switch (t) {
        case BC4JXCFG:
          BC4J_XCFG.processXcfgFile(jar, ze);
          break;

        case VIEW_OBJECT:
          ViewObject vo = new ViewObject(jar, ze);
            
          viewObject_list.add(vo);

          ViewObject tmp = viewObject_map.get(name);
          if (tmp != null) {
            Long l1 = vo.getDefaultFetchSize();
            Long l2 = tmp.getDefaultFetchSize();

            if (l1 == null && l2 == null) {
              // ok, weird dups that have consistent tuning will calculate out same
            } else if (l1 != null && l2 != null &&
                       l1.longValue() == l2.longValue()) {
              // ok, weird dups that have consistent tuning will calculate out same
            } else {

              String type =
                "View Objects are duplicated, but have different fetch size";
              String desc =
                "There are two View Objects that have the same name and path, but they have different fetchSizes defined.  This will present an obstacle for the scanner to calculate optimal tuning values.  --> " +
                l1 + " and " + l2;
            }
          } else {
            viewObject_map.put(name, vo);
          }
          break;

        case APPLICATION_MODULE:
          ViewUsage.processViewUsages(jar, ze, viewUsage_list);
          break;

        case UI_FILE:
          UIPageTunableElements.processPage(jar, ze);
          break;
        case PAGE_DEF:
          PageDef.processPage(jar, ze);
          break;
        case CPX_FILE:
          PageMap.processCPXFile(jar, ze);
          PageDefUsages.processCPXFile(jar, ze);
          DataControl.processCPXFile(jar, ze);
          break;
        case ENTITY:
          EntityObject eo = new EntityObject(jar, ze);
          entityObject_list.add(eo);
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(e.toString());
    }
  }
  
//  public ArrayList<ScanResult> validate(ArrayList<ScanResult> al_all){
//    ScanResult r = null; 
//    Iterator<ScanResult> i = al_all.iterator();
//    while(i.hasNext()){
//      r = i.next();
//      if(r.getType().equals("selectOneChoice") || r.getType().equals("inputComboboxListOfValues")){
//        if(Rules.validateLOVs(r))
//          i.remove();
//      }
//      else{
//          Rules.validateTables1stRun(r);
//      }
//    }
//    
//    i = al_all.iterator();
//    while(i.hasNext()){
//      r = i.next();
//      if ((r.getType().equals("table") || r.getType().equals("treeTable")) && Rules.validateTables2ndRun(r))
//        i.remove();
//    }
//    
//    return al_all;
//  }


  @Override
  protected String getSummaryReportSubject() {
    return "Jar Analyzer for ADF Tuning";
  }

  @Override
  protected String getSummaryReport() {
    return "Not Implemented";
  }

//  private static boolean isUnderCurrentDir(ScanResult r, String currentDir) {
//    if(r.getPage() != null && r.getPage().contains(currentDir))
//      return true;
//    
//    if(r.getPageDef() != null && r.getPageDef().contains(currentDir))
//      return true;
//    
//    if(r.getAM() != null && r.getAM().contains(currentDir))
//      return true;
//    
//    if(r.getVO() != null && r.getVO().contains(currentDir))
//      return true;
//    
//    if(r.getVa_VO() != null && r.getVa_VO().contains(currentDir))
//      return true;
//    
//    return false;
//  }
}
