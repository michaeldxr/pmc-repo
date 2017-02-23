package oracle.apps.psr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import oracle.apps.helpers.FamilyModuleHelper;

public class DeprecatedTriggersPreMergeChecker {
   
  public static void main(String[] args) throws Exception {
     
     
     DeprecatedTriggers scanner = new DeprecatedTriggers();
    // String[] files = args[0].trim().split("\n");
     String ade_view_root = args[1].trim();
     String filelistpath=args[0].trim();
     String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root); 
    
     DeprecatedTriggers.writer = new BufferedWriter(new FileWriter("deprecatedTriggers.csv") );
     DeprecatedTriggers.writer.write("Family,Module,Product,FileName,Label,Description\n");   
     
     for(int i = 0; i < files.length; i++) {
         String filePath = ade_view_root + "/" + files[i].trim();
         File f = new File(filePath);
         if(!f.exists())
             continue;
         if(scanner.isValidUIFile(filePath))            
            scanner.processFile(f, false, false);
                  
     }       
     
     DeprecatedTriggers.writer.write("\n\n\nPartial Triggers on tables with syntax delete, deleteMenuItem, create, createMenuItem," +
         "duplicate,duplicateMenuItem, edit, editMenuItem are deprecated. Correct syntax is to precede these with :: e.g. ::delete instead of delete.\n");
     DeprecatedTriggers.writer.close();
   }
}
