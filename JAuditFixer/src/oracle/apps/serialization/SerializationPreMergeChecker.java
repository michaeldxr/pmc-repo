package oracle.apps.serialization;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class SerializationPreMergeChecker {
    
   public static void main(String[] args) throws Exception {
      
      SerializationScanner scanner = new SerializationScanner();
      String[] files = args[0].trim().split("\n");
      String ade_view_root = args[1].trim();    
      
      //System.out.println("Files:" + files);
      
      SerializationScanner.outputFileWriter = new BufferedWriter(new FileWriter("serialization_scan.csv") );
      SerializationScanner.outputFileWriter.write("Family, Module, SubModule, Filename, " +
          "LineNum, NodeName, Id, Comment1, Comment2, ScanType, Label\n");        
      
      for(int i = 0; i < files.length; i++) {
          String filePath = ade_view_root + "/" + files[i].trim();
          File f = new File(filePath);
          if(!f.exists())
              continue;
          if(scanner.fileOfInterest(filePath))              
             scanner.processFile(f);                 
      }          
      
      SerializationScanner.outputFileWriter.write("\n\n\nUse bindings.attrName.inputValue or bindings.attrName.attributeValue pattern " +
          "instead of bindings.attrName pattern in pageDefExecutables and SetActionListeners.\n\n Please see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/SerializationIssues\n");
      SerializationScanner.outputFileWriter.close();
    }
}
