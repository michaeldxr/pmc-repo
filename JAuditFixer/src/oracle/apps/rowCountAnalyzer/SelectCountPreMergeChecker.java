package oracle.apps.rowCountAnalyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;


import oracle.apps.helpers.FamilyModuleHelper;

public class SelectCountPreMergeChecker {
        
    public static void main(String[] args) throws Exception {
       
       FindRowCountInstances rowCountAnalyzer = new FindRowCountInstances();
       //String[] files = args[0].trim().split("\n");
       String ade_view_root = args[1].trim();
       String filelistpath=args[0].trim();
       String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root);      
       //System.out.println("Files:" + files);
       
       FindRowCountInstances.outputFileWriter = new BufferedWriter(new FileWriter("SelectCountScan.csv") );
       FindRowCountInstances.outputFileWriter.write("Family,Module,Product,Filename,Label, Line, Line#, Type of RowCount, Usage Type\n");        
       
       for(int i = 0; i < files.length; i++) {
           String filePath = ade_view_root + "/" + files[i].trim();
           File f = new File(filePath);
           if(!f.exists())
               continue;
           if(rowCountAnalyzer.isFileOfInterest(filePath)) 
              rowCountAnalyzer.processFile(f);
                 
       }          
       
       //FindRowCountInstances.outputFileWriter.write("\n\n\nAbove rowCount usages should be avoided.\n\n");
       FindRowCountInstances.outputFileWriter.close();
       rowCountAnalyzer.removeExceptionCases();

       FindRowCountInstances.outputFileWriter = new BufferedWriter(new FileWriter("SelectCountScan.csv",true) );
       FindRowCountInstances.outputFileWriter.write("\n\nAbove RowCount usages should be removed. Please see: http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/RowCountAnalyzer\n\n\n");  
       FindRowCountInstances.outputFileWriter.close();    
     }
}
