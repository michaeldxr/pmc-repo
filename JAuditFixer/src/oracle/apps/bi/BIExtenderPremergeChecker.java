package oracle.apps.bi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.Arrays;
import java.util.HashSet;

import oracle.apps.helpers.FamilyModuleHelper;

public class BIExtenderPremergeChecker {
      
    public static void main(String[] args) throws Exception{
        
        BIExtenderAutomationScanner scanner = new BIExtenderAutomationScanner();
        scanner.initializeExemptions();
        //String[] files = args[0].trim().split("\n");
        String ade_view_root = args[1].trim();
        String filelistpath=args[0].trim();
        String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root);
        BIExtenderAutomationScanner.outputFileWriter = new BufferedWriter(new FileWriter("BIExtenderAutomation_scan_temp.csv") );
        //BIExtenderAutomationScanner.label = scanner.getLabel(sCrawlDir);
        
        boolean runBIScan = false;
        
        HashSet<String> fileSet = new HashSet<String>(Arrays.asList(files));
        System.out.println(fileSet);
        
        String filePath = "";
        for(int i = 0; i < files.length; i++) {
            filePath = ade_view_root + "/" + files[i].trim();
            File f = new File(filePath);
            if(!f.exists())
                continue;
            if(scanner.fileOfInterest(filePath)) {
               runBIScan = true;             
               break;
            }
        }    
        
        String sCrawlDir = null;
        if(runBIScan) {
            int index = filePath.indexOf("fusionapps/");
            if(index != -1)
                index = filePath.indexOf("/", index+11);
            if(index != -1)
                sCrawlDir = filePath.substring(0,index);
        }
        
        if(sCrawlDir != null) {
            System.out.println("CrawlDir: " + sCrawlDir);
            scanner.crawlDirectory(sCrawlDir);   
        }
       
        
        BIExtenderAutomationScanner.outputFileWriter.close();
        
        BufferedReader reader = new BufferedReader(new FileReader("BIExtenderAutomation_scan_temp.csv"));
        String line = "";
        boolean hasViolations = false;
        
        BufferedWriter writer = new BufferedWriter(new FileWriter("BIExtenderAutomation_scan.csv"));
        //"Family,Module,Product,FileName,Label,MethodName,MethodStartLineNo, RSI Variable Not Closed,Iterator Name\n");
        
        while((line = reader.readLine()) != null) {
            
            String[] parts = line.split(",");
            if(parts.length < 10) {
                System.out.println("Skipping: " + line);
                continue;
            }
            System.out.println(parts[3].trim());
            if(!fileSet.contains(parts[3].trim()))
                continue;           
           
            hasViolations = true;
            writer.write("\n\nFileName: " + parts[3] + "\n");
            writer.write("Description: " + parts[7].trim() + "(" + parts[5].trim() + ") -" + parts[8].trim() + "\n");
            writer.write("Issue: BIExtenderAutomation\n"); 
            writer.write("Sub-Issue: " + parts[6].trim() + "\n");
            
        }
        
        if(hasViolations)
            writer.write("\n\n\nPlease see https://stbeehive.oracle.com/teamcollab/wiki/BI+Integration+for+Fusion+Apps+Procurement:RUP4+-+BI+Extender+Automation+Update+for+FA " +
                "for more details on BIExtenderAutomation scan. \n");
         
         writer.close();
    }
}
