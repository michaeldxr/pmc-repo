package oracle.apps.references;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;


import oracle.apps.helpers.FamilyModuleHelper;


public class InvalidJarRefPreMergeChecker {

  private static CleanInvalidJarReferences invalidJarCleaner;
  
  public static void main(String[] args) throws Exception {
      
   // String[] files = args[0].trim().split("\n");
   // String[] files = "fusionapps/scm/components/pricing/QpSuperWeb/ADF_Library_Dependencies.library".split("\n");
    String ade_view_root = args[1].trim();
    String filelistpath=args[0].trim();
    String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root); 
      
    invalidJarCleaner = new CleanInvalidJarReferences();
    invalidJarCleaner.initializeInvalidJars();
    invalidJarCleaner.initializeCrmInvalidJars();    
    
    invalidJarCleaner.writer = new BufferedWriter(new FileWriter("invalidJarReferences.csv"));
    invalidJarCleaner.writer.write("family,module,product,filename,label,issue,subissue,description\n");
    
    for(int i = 0; i < files.length; i++) {
        String filePath = ade_view_root + "/" + files[i].trim();
        File f = new File(filePath);
        if(!f.exists()) {
            System.out.println("File not found: " + filePath);
            continue;
        }
        if(invalidJarCleaner.isValidJpr(filePath)) {
            System.out.println("Processing jpr: " + filePath);
           invalidJarCleaner.processJpr(filePath, false);
           invalidJarCleaner.removeHangingInvalidLibraryRefs(filePath, false);
           invalidJarCleaner.removeNonExistentLibraryRefs(filePath, false);
        }
        else if (invalidJarCleaner.isValidLibraryFile(filePath)) {
            System.out.println("Processing library: " + filePath);
          invalidJarCleaner.processADFLibraryFile(filePath, false);
        }
    }    
    
    invalidJarCleaner.writer.close();
    
//    invalidJarCleaner.writer = new BufferedWriter(new FileWriter("invalidJarReferences.csv", true)); //open in append mode
//    
//    BufferedReader reader = new BufferedReader(new FileReader("invalidJarReferences1.csv"));
//    String line = null;
//    while((line = reader.readLine()) != null){
//      invalidJarCleaner.writer.write(line+"\n");
//    }
//    
//    invalidJarCleaner.writer.close();
   
  }
  
}
