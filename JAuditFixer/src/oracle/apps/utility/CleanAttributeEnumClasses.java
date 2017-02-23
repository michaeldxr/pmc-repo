package oracle.apps.utility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;


public class CleanAttributeEnumClasses {
    
    static BufferedWriter writer;
    static String cleanOption = "Enum";
    
    public static void main(String[] args) throws Exception {
        
        if (args.length < 1 || args[0] == null || args[0].trim().equals("")) {
            System.out.println("Crawl directory has to be specified");
            System.exit(1);
        }
        
        String sCrawlDir = args[0].trim();       
           
       //  String sCrawlDir = "/scratch/sudgupta/view_storage/sudgupta_fusionapps_rup4_view/fusionapps/fin/components/receivables";
         
        if (sCrawlDir == null || sCrawlDir.equals("")) {
            System.out.println("Crawl directory has to be specified");
            System.exit(1);
        }
        
        File f = new File(sCrawlDir);
        if(!f.exists()){
            System.out.println("Crawl directory does not exist - " + sCrawlDir);
            System.exit(1);
        }
        if(args.length > 1)
            cleanOption = args[1].trim();
        
        if(!cleanOption.equals("Enum") && !cleanOption.equals("Enum")) {
            System.out.println("Clean option should be Enum or All...setting it to default value i.e. Enum");
            cleanOption = "Enum";
        }
            
        writer = new BufferedWriter(new FileWriter("deletedClasses.txt"));
        crawlDirectory(sCrawlDir);
        writer.close();
    }
    
    
    private static void crawlDirectory(String path) throws Exception{
          if(path.contains(".ade_path"))
              return;

          File folder = new File(path);
          File[] listOfFiles = folder.listFiles();
              
          if(listOfFiles == null)
              return;
          
          for(int i = 0; i <listOfFiles.length; i++)
          {
              if(listOfFiles[i].isFile()) 
              {
                  String sName = listOfFiles[i].getAbsolutePath();              
                  if(sName.endsWith(".class")) { 
                      
                      if(cleanOption.equals("Enum") && !sName.contains("AttributesEnum$"))
                          continue;
                      boolean success = listOfFiles[i].delete();   
                      if(success) writer.write("\nDeleted: " + sName);
                      else writer.write("\nCould not delete: " + sName);
                  }
              }
              else if(listOfFiles[i].isDirectory()) 
                  crawlDirectory(listOfFiles[i].getAbsolutePath());          
              else 
                  System.out.println("ERROR:  node is neither file or directory: "+listOfFiles[i]);          
          }      
    }
}