package oracle.apps;

import java.util.HashSet;
import java.util.Set;

import oracle.apps.helpers.FamilyModuleHelper;


public class VOTuningPMC {
  public VOTuningPMC() {
    super();
  }

  private static Set<String> h_family = new HashSet<String>();   

  public static void main(String[] args) {
   // String[] files = args[0].trim().split("\n");
    String adeViewRoot = args[1].trim();
    String filelistpath=args[0].trim();
    String[] files=FamilyModuleHelper.getFileList(filelistpath, adeViewRoot); 
    String label = args[2].trim();
    String family = args[3];

    String release = FamilyModuleHelper.getRelease(label);

    int releaseNum;

    try {
      releaseNum = Integer.parseInt(release);
      //run votuing_v2 on R9 and R10 branch, votuning_v3 on R11+ branches
      if (releaseNum < 11){
        System.out.println("Running votuing_v2 scan.");
        oracle.apps.votuning_v3.JarAnalyzerPMC.process(files, adeViewRoot, label, family);
      }
      else{
        System.out.println("R11+ branch found. Running votuing_v3 scan.");
        oracle.apps.votuning_v4.JarAnalyzerPMC.process(files, adeViewRoot, label, family);
      }
    } catch (NumberFormatException e) {
      System.out.println("Could not determine release. Skipping VOTuning_V3 check...");
      return;
    }

  }
}