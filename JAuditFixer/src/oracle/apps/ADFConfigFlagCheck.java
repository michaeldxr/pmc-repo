package oracle.apps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;

import java.util.ArrayList;

import java.util.HashSet;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.FileParserHelper;
import oracle.apps.helpers.XMLParserHelper;

import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;


public class ADFConfigFlagCheck {
    
    static Writer writer;
    HashSet<String> exemptions = new HashSet<String>();
    private static String mode="view"; // can be LRG/view 
    private static final String issuetype ="AvoidLoadingTranslatorNotes";   
    
    public static void main(String[] args) throws Exception{
        
        ADFConfigFlagCheck scanner = new ADFConfigFlagCheck();  
        scanner.generateExemptionList(); 
        writer = new BufferedWriter(new FileWriter("ADFConfigFlagCheckMissing.csv") );
        writer.write("Family, Module, SubModule, Filename, Series, Label, Description\n");     
        
        mode = System.getProperty("mode");
        if(mode == null)
            mode = "view";
        
        if(mode.equals("view")) {        
            String sCrawlDir = args[0].trim();
            //String sCrawlDir = "/ade_autofs/ud21_fa/FUSIONAPPS_PT.DEVINT_LINUX.X64.rdd/LATEST/fusionapps/fin/components/";
            System.out.println("Analyzing all files in directory (ADFConfig Flag check):" + sCrawlDir);       
            scanner.crawlDirectory(sCrawlDir);           
        }
        else if(mode.equals("LRG")) {
            
            String series = args[0];
            String label = LRGUtil.getLatestLabel(series);
            System.out.println("Running " + issuetype + " Scan on series: " + series + ".......");
            System.out.println("Latest label is: " + label);
            
            ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
            
            for (int j = 0; j < families.size(); j++) {
                
                String family = families.get(j);
                String sCrawlDir = LRGUtil.getCrawlDir(series, family, label, issuetype);
                System.out.println("Crawling dir '" + sCrawlDir + "' for family: " + family);                    
                scanner.crawlDirectory(sCrawlDir);                    
                System.out.println("Done crawling for family "+family+".");
            }                
            LRGUtil.addLabelInfoToAB(issuetype, series, label);
        } else if(mode.equals("premerge")) {
                    
            //String[] files = args[0].trim().split("\n");
            String ade_view_root = args[1].trim();
            String filelistpath=args[0].trim();
            String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root);  
            
            String label = args[2].trim();
            
            String release= FamilyModuleHelper.getRelease(label);
            System.out.println("Release: " + release);
            int releaseNum=0;
            
            try{              
                releaseNum = Integer.parseInt(release);
                if (releaseNum < 9){ //This check was mandated post release 9 only, hence this check is necessary to avoid the current rel7 and rel8 branches from failing
                    if(writer != null) writer.close();
                    return;       
                }
            }catch(NumberFormatException e){
                System.out.println("Could not determine release. Skipping AvoidLoadingTranslatorNotes check...");
                if(writer != null) writer.close();
                return;
            }
            for(int i = 0; i < files.length; i++) {
                
                String filePath = ade_view_root + "/" + files[i].trim();
                File f = new File(filePath);
                if(!f.exists())
                    continue;
                if(!scanner.fileOfInterest(filePath))
                    continue;
                scanner.processFile(f);
            }
            writer.write("\n\nPlease see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/ADFConfigFlagSetting " +
                "for description of the issue and resolution.\n\n");                
        }
        writer.close();        
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
            else if(FamilyModuleHelper.isPathOfInterest(sName) && listOfFiles[i].isDirectory() 
                    && !sName.contains("/db/") && !sName.contains("/jlib/") && !sName.contains("fusionapps/translation/")) 
                crawlDirectory(listOfFiles[i].getAbsolutePath());            
        }      
    }
    
    public void processFile(File f) {
        try{
            System.out.println("Processing: " + f.getAbsolutePath());
            boolean adfconfigflagfound=false;
            
          
            if(!adfconfigflagfound) {
                String filecontents = FileParserHelper.getFileContentsAsString(f.getAbsolutePath());
                if (!filecontents.contains("adf-resourcebundle-config"))
                    return;
                if(!filecontents.contains("load-xliff-at-startup-behavior") && !exemptions.contains(FamilyModuleHelper.getPathAfterViewRoot(f.getAbsolutePath())))
                    writer.write(FamilyModuleHelper.getFileNameInfo1(f.getAbsolutePath()) + "ADF Config Flag load-xliff not configured properly\n");
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public boolean fileOfInterest(String sName) 
    {
      if(!FamilyModuleHelper.isPathOfInterest(sName))
          return false;  
      if(!sName.contains("components"))
          return false;
      if(sName.contains("deploy"))
              return false;
     
      if (sName.contains("META-INF") && sName.endsWith("/adf-config.xml"))
          return true;
      return false;
    }
    
    
    public void generateExemptionList() {
        
    try{
      BufferedReader fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
          "/fatools/opensource/jauditFixScripts/txt/angrybirdCodescanExemptions_new.txt"));
      String line = null;
      
      while((line = fileReader.readLine()) != null) {
        String[] parts = line.split(",");
        if(parts.length < 2)
            continue;
        String issue = parts[0].trim();
        if(!issue.equals("ADFConfigFlagCheck"))
            continue;
        String fileName = parts[1].trim();
          
        exemptions.add(fileName);
      }
    }catch(Exception e) {
        System.out.println("Skipping exemptions as file could not be found: " + e.getMessage());
    }
    }   
 
}
