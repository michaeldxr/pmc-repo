package oracle.apps.rup2scans;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import oracle.apps.helpers.FamilyModuleHelper;


public class RSIPremergeChecker {
    
    public static void main(String[] args) throws Exception {
       
       RowSetIteratorsNotClosed scanner = new RowSetIteratorsNotClosed();
       //String[] files = args[0].trim().split("\n");
       String ade_view_root = args[1].trim();
       String filelistpath=args[0].trim();
       String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root);
       RowSetIteratorsNotClosed.csvWriter = new BufferedWriter(new FileWriter("RSI_issues_temp.csv"));
      // RowSetIteratorsNotClosed.csvWriter.write("Family,Module,Product,FileName,Label,MethodName,MethodStartLineNo, RSI Variable Not Closed,Iterator Name\n");      
       
       for(int i = 0; i < files.length; i++) {
           String filePath = ade_view_root + "/" + files[i].trim();
           File f = new File(filePath);
           if(!f.exists())
               continue;
           if(scanner.fileOfInterest(filePath)) 
              scanner.processFile(f);
             
       }          
       
        RowSetIteratorsNotClosed.csvWriter.close();
        
        BufferedReader reader = new BufferedReader(new FileReader("RSI_issues_temp.csv"));
        String line = "";
        boolean hasViolations = false;
        
        BufferedWriter writer = new BufferedWriter(new FileWriter("RSI_issues.csv"));
        //"Family,Module,Product,FileName,Label,MethodName,MethodStartLineNo, RSI Variable Not Closed,Iterator Name\n");
        
        while((line = reader.readLine()) != null) {
            
            String[] parts = line.split(",");
            if(parts.length < 9) {
                System.out.println("Skipping: " + line);
                continue;
            }
            String iteratorName = parts[8].trim();
            if(!iteratorName.equals("null"))
                continue;
            
            hasViolations = true;
            writer.write("FileName: " + parts[3] + "\n");
            writer.write("Method Name: " + parts[5].trim() + "\n");
            writer.write("Issue: RowSetIterator " + parts[7].trim() + " is not closed.\n\n");           
            
        }
    
        if(hasViolations)
            writer.write("\n\nPlease see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/RowSetIteratorNotClosed " +
                "for instructions on how to fix the RowSetIterator violations. \n");
         
         writer.close();
 
    }
}
