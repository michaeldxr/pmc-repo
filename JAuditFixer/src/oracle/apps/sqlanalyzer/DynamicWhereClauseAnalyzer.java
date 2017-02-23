package oracle.apps.sqlanalyzer;

import java.io.BufferedWriter;
import java.io.File;

import java.io.FileWriter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.FileParserHelper;

public class DynamicWhereClauseAnalyzer {
   
    static BufferedWriter writer;
    static int count = 0;

    Pattern pDynamicWhereClause = Pattern.compile("(setWhereClause|addWhereClause)\\s*\\(\\s*(?!null).*?\\)\\s*;",Pattern.DOTALL);
    public static void main(String[] args) throws Exception {
        
        //        if(args == null || args.length < 1){
        //          System.out.println("Crawl directory has to be specified");
        //          System.exit(1);
        //        }
        //        String sCrawlDir = args[0].trim();
        String sCrawlDir = "/ade_autofs/ade_fusion_linux/FUSIONAPPS_11.1.1.5.1_LINUX.X64.rdd/LATEST/fusionapps/hcm/components";
        
        if (sCrawlDir == null || sCrawlDir.equals("")) {
            System.out.println("Crawl directory has to be specified");
            System.exit(1);
        }
        
        File f = new File(sCrawlDir);
        if(!f.exists ()){
            System.out.println("Crawl directory does not exist - " + sCrawlDir);
            System.exit(1);
        }
                
        boolean append = false;
        if(args.length > 1)
           append = new Boolean(args[1].trim());
        
        System.out.println("Analyzing all files in directory:" + sCrawlDir);
        
        DynamicWhereClauseAnalyzer scanner = new DynamicWhereClauseAnalyzer();
             
        writer = new BufferedWriter(new FileWriter("dynamic_where_clauses.csv", append) );
    
        if(!append)
            writer.write("Family, Module, SubModule, Filename, Series, Label, Description\n");     
        
        scanner.crawlDirectory(sCrawlDir); 
        writer.close();
        
        System.out.println("Total number of violations: " + count);
       
    
    }
    
    private void crawlDirectory(String path) throws Exception
    {
        if(path.contains(".ade_path") || path.contains("/classes/"))
            return;
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
            
        if(listOfFiles == null)
            return;      
        for(int i = 0; i <listOfFiles.length; i++)
        {
            String sName = listOfFiles[i].getAbsolutePath(); 
            if(listOfFiles[i].isFile() && fileOfInterest(sName))            
                    processFile(listOfFiles[i]);              
            else if(FamilyModuleHelper.isPathOfInterest(sName) && listOfFiles[i].isDirectory()) 
                crawlDirectory(listOfFiles[i].getAbsolutePath());            
        }      
    }
    
    public boolean fileOfInterest(String sName) 
    {
      if(!FamilyModuleHelper.isPathOfInterest(sName))
          return false;  
      if(!sName.contains("components"))
          return false;
     
      if (sName.endsWith(".java"))
          return true;
      return false;
    }
    
    public void processFile(File f) {
        try{
            
          String fileContents = FileParserHelper.getFileContentsAsString(f.getAbsolutePath());
          Matcher m = pDynamicWhereClause.matcher(fileContents);
            
            while(m.find()) {
              writer.write(FamilyModuleHelper.getFileNameInfo1(f.getAbsolutePath()) + m.group(0).replace("\n", " ").replace(",","<COMMA>") + "\n");
                count++;
            }
            
          writer.flush();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
