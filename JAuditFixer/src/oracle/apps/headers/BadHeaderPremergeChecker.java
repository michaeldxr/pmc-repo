package oracle.apps.headers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import oracle.apps.helpers.FamilyModuleHelper;

public class BadHeaderPremergeChecker {
    
  public static void main(String[] args) throws Exception {
     
     FixDoubleHeadersV2 header = new FixDoubleHeadersV2();
     header.initializeExemptions();
     //String[] files = args[0].trim().split("\n");
     String ade_view_root = args[1].trim();
     String filelistpath=args[0].trim();
     String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root); 
     //System.out.println("Files:" + files);
     FixDoubleHeadersV2.makeFix = false;
       FixDoubleHeadersV2.filterNoShip = true;
     
     FixDoubleHeadersV2.outputFileWriter = new BufferedWriter(new FileWriter("headers_scan.csv"));        
     FixDoubleHeadersV2.outputFileWriter.write("Label,Family,Product,Filename,Extension,Header1,Header2,Headers Syncronized?,Header With bad syntax,# Headers, # Packages, Issue\n");    
          
     FixDoubleHeadersV2.outputFileWriter1 = new BufferedWriter(new FileWriter("headers_scan_bad_location.csv") );       
     
     for(int i = 0; i < files.length; i++) {
         String filePath = ade_view_root + "/" + files[i].trim();
         
         File f = new File(filePath);
         if(!f.exists())
             continue;
         
         if(header.fileOfInterest(filePath)) {
            
            if(!header.fileOfInterest(filePath))
                continue;
            boolean result = header.processFile(f);
            if(!result)
                header.processPkg(f);
         }          
     }     
     FixDoubleHeadersV2.outputFileWriter1.close();
     FixDoubleHeadersV2.outputFileWriter.close();
     
     BufferedWriter writer = new BufferedWriter(new FileWriter("headers_violations.txt"));
     
     BufferedReader reader = new BufferedReader(new FileReader("headers_scan.csv"));
     String line = reader.readLine(); // read the header line
     
     boolean hasViolation = false;
     while((line = reader.readLine()) != null) {
         
         String[] parts = line.split(",");
         
         writer.write("Filename: " + parts[3].trim() + "\n");
         
       String subissue = "";
       int num_headers = Integer.parseInt(parts[9].trim());
       int num_packages = Integer.parseInt(parts[10].trim());
       if(num_headers >= 2 && num_headers > num_packages)
           subissue = "More Headers than packages";
       else if(num_headers == 0)
           subissue = "File has no headers";
       else if(num_headers < num_packages)
           subissue = "File has fewer headers than packages";
       else
           subissue = "Bad header syntax for header #: " + parts[8].trim();
         
         writer.write("Issue: " + subissue + "\n\n\n");
         hasViolation = true;
     }
     
     reader = new BufferedReader(new FileReader("headers_scan_bad_location.csv"));
     while((line = reader.readLine()) != null) {
       String[] parts = line.split(",");
       
       writer.write("Filename: " + parts[3].trim() + "\n");
       writer.write("Issue: Incorrect Header location - Header should be located after 'Create or Replace Package'\n\n\n");
       hasViolation = true;
     }
     
     if(hasViolation)
         writer.write("\n Please see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/BadSQLHeaders");
     
     writer.close();
   }
}
