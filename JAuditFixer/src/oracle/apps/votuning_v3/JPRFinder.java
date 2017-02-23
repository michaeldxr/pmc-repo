package oracle.apps.votuning_v3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import oracle.apps.utility.JoesBaseClass;
import oracle.apps.votuning_v3.file_type.Application;
import oracle.apps.votuning_v3.file_type.Project;


public class JPRFinder extends JoesBaseClass {

  private static final String[] l_family =
  { "ic", "scm", "prj", "prc", "hcm", "grc", "fin", "crm", "com", "atf",
    "fscm" };
  private static String label;
  private static final int MAX_DEPTH = 5;
  
  public JPRFinder() {
    super(JoesBaseClass.CRAWL_TYPE.JWS);
  }

  public static void main(String[] args) {
    
    if (args.length != 3 || args[2] == null|| args[2].equals("")) {
      System.out.println("USAGE: jarAnalyzerForADFTuning.sh <tempSerialiazationDir> <outputDir>");
      System.exit(1);
    }
    
    if(args[0] == null || args[0].isEmpty()){
      System.out.println("$VIEW_LABEL is not defined, please run the script inside a view");
      System.exit(1);
    }
      
    if(args[1] == null || args[1].isEmpty()){
      System.out.println("$ADE_VIEW_ROOT is not defined, please run the script inside a view");
      System.exit(1);
    }
    
    label = args[0];
    String adeViewRoot = args[1];
    String topSerTempDir = args[2];
    
    File f = new File(adeViewRoot);
    if(!f.isDirectory()){
      System.out.println("ERROR: Invalid ADE VIEW ROOT.");
      System.exit(1);
    }
    
    f = new File(topSerTempDir);
    if(!f.isDirectory() || !f.canRead() || !f.canWrite()){
      System.out.println("ERROR: The directory specified to store serialization files is either unreadable or unwritable.");
      System.exit(1);
    }

    f = new File(topSerTempDir + "/" + label);
    // if the directory does not exist, create it
    if (!f.exists()) {
      System.out.println("creating temp directory: " + topSerTempDir + "/" +
                         label);
      boolean result = f.mkdir();
      if (result) {
        System.out.println("DIR created");
      } else {
        System.out.println("ERROR: failed to create temp dir");
        System.exit(1);
      }
    }else if(new File(f.getAbsolutePath()+"/jlib.ser").exists() && new File(f.getAbsolutePath()+"/JarSrcMapping.ser").exists()){
        System.out.println("Serializtion already exists. No need to scan the jpr file again.");
        System.exit(1);   
    }
      
    JPRFinder jPRFinder = new JPRFinder();
    jPRFinder.markStartTime();
    System.out.println("Start finding all the jlib directories and jpr files...");
    if(label.contains("FSM")){
        String path = adeViewRoot + "/fsm";
        System.out.println("Processing " + path);
        jPRFinder.crawlDirectory(path, MAX_DEPTH, false);
    }else{
        for (String family : l_family) {
          String path = adeViewRoot + "/fusionapps/" + family + "/components";
          System.out.println("Processing " + path);
          jPRFinder.crawlDirectory(path, MAX_DEPTH, false);
        }
    }

    jPRFinder.serializeResult(f.getAbsolutePath());
    
    jPRFinder.markEndTime();

  }

  protected void processFile(File fName, boolean bDoADE) {
    if(fName.getName().contains("Ess.jws") || fName.getName().contains("Soa.jws") )
      return;
    Application.processFile(fName.getAbsolutePath());
  }

  private void serializeResult(String path) {
    try {
      if(!Project.adePathMapping.keySet().isEmpty()){
        FileOutputStream fos = new FileOutputStream(path + "/JarSrcMapping.ser");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(Project.adePathMapping);
        oos.close();
      }
    } catch (Exception ex) {
      System.out.println(("Exception thrown when generating the JarSrcMapping serialization: " + ex.toString()));
    }

    try {
      if(!Project.s_jlib.isEmpty()){
        FileOutputStream fos = new FileOutputStream(path + "/jlib.ser");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(Project.s_jlib);
        oos.close();
      }
    } catch (Exception ex) {
      System.out.println(("Exception thrown when generating the jlib serialization: " + ex.toString()));
    }
  }


  protected String getSummaryReport() {
    return null;
  }

  protected String getSummaryReportSubject() {
    return null;
  }
}
