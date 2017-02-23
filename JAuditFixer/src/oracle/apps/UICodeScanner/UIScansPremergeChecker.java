package oracle.apps.UICodeScanner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class UIScansPremergeChecker {
    public static void main(String[] args) throws Exception {
       
       ScanUICode scanner = new ScanUICode();
       String[] files = args[0].trim().split("\n");
       String ade_view_root = args[1].trim();    
       
       scanner.generateExemptionList();
       
       //System.out.println("Files:" + files);
       
       ScanUICode.outputFileWriter = new BufferedWriter(new FileWriter("ui_scan_temp.csv") );
       //ScanUICode.outputFileWriter.write("Family,Module,Product,Filename,Label, Line, Line#, Type of RowCount, Usage Type\n");        
       
       for(int i = 0; i < files.length; i++) {
           String filePath = ade_view_root + "/" + files[i].trim();
           File f = new File(filePath);
           if(!f.exists())
               continue;
           if(fileOfInterest(filePath)) 
              scanner.processPage(f,false);
                   
       }          
       
       //FindRowCountInstances.outputFileWriter.write("\n\n\nAbove rowCount usages should be avoided.\n\n");
       ScanUICode.outputFileWriter.close();
    
    //Report only RUP1 violaions for files in current transaction

         BufferedWriter outputFileWriter = new BufferedWriter(new FileWriter("ui_scan.csv"));
         
         BufferedReader reader = new BufferedReader(new FileReader("ui_scan_temp.csv"));
         String line = "";
         boolean hasViolation = false;
         while((line = reader.readLine()) != null){
             String[] parts = line.split(",");
             if(parts.length < 9)
                 continue;
             
             String fileName = parts[3].trim();
             String issue = parts[5].trim();
             
             if(!issue.equals("rendered=false") && !issue.equals("visible=false"))
                 continue;
             else {
                 hasViolation = true;
                 outputFileWriter.write("FileName: " + fileName + "\n");
                 outputFileWriter.write("ComponentId: " + parts[7].trim() + "\n");
                 outputFileWriter.write("Issue: " + issue + "\n\n");
             }
         }
         if(hasViolation)
             outputFileWriter.write("\nPlease see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/UIRenderedFalse, " +
                 "http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/EmptyPanelHeaderText and http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/ButtonTypeMismatchFix\n");
         
         outputFileWriter.close();
         reader.close();   
     }
    
    public static boolean  fileOfInterest(String sName) 
    {
        
        if(sName == null || sName.equals(""))
            return false;
        
        if(sName.toLowerCase().contains("/noship/") 
           || sName.toLowerCase().contains("/test/")
            || sName.toLowerCase().contains("servicetest")
            || sName.toLowerCase().contains("datasecuritytest")
            || sName.toLowerCase().contains("securitypublictest")
            || sName.toLowerCase().contains("/publicservicetest")
            || sName.toLowerCase().contains("/publicmodeltest")
            || sName.toLowerCase().contains("/protectedmodeltest")
            || sName.toLowerCase().contains("/publicuitest")
            || sName.toLowerCase().contains("/uimodeltest")
            || sName.toLowerCase().contains("/structuretest")
            || sName.toLowerCase().contains("/modeltest")
            || sName.toLowerCase().contains("/uitest")
            || sName.toLowerCase().contains("/testui")
            || sName.toLowerCase().contains("/protecteduimodeltest")
            || sName.toLowerCase().contains("/testviewcontroller")
            || sName.toLowerCase().contains("/flextest")
            || sName.toLowerCase().contains("/uimodeler-launch-test")
            || sName.toLowerCase().contains("/publicuibasetest")
            || sName.toLowerCase().contains("/uipickertest"))
            return false;
        
        if(sName.endsWith("index.jspx"))
            return false;

        if(sName.endsWith(".jsff") || sName.endsWith(".jspx")) 
            return true;
        
        return false;
    }
}
