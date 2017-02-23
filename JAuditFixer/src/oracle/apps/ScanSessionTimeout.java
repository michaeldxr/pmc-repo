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
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ScanSessionTimeout {
    static Writer writer;
    HashSet<String> exemptions = new HashSet<String>();
    private static String mode="view";// can be LRG/view/premerge
    private static final String issuetype="SessionTimeoutSetting";
       
    public static void main(String[] args) throws Exception{
        
        ScanSessionTimeout scanner = new ScanSessionTimeout();
        //scanner.generateExemptionList();             
        writer = new BufferedWriter(new FileWriter("SessionTimeoutSetting.csv") );
        writer.write("Family, Module, SubModule, Filename, Series, Label, Description\n");     
        
        mode = System.getProperty("mode");
        if(mode == null)
            mode = "view";
        
        if(mode.equals("view")) {       
            String sCrawlDir = args[0].trim();
          //  String sCrawlDir = "/ade_autofs/ade_fusion_linux/FUSIONAPPS_PT.R8INT_LINUX.X64.rdd/LATEST/fusionapps/"; 
            System.out.println("Analyzing all files in directory (Session Timeout check):" + sCrawlDir);       
            scanner.crawlDirectory(sCrawlDir); 
        } else if(mode.equals("LRG")) {
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
        }else if(mode.equals("premerge")) {
            
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
                    if (releaseNum < 9) //This check was mandated post release 9 only, hence this check is necessary to avoid the current rel7 and rel8 branches from failing
                        return;              
                }catch(NumberFormatException e){
                    System.out.println("Could not determine release. Skipping SessionTimeout check...");
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
                writer.write("\n\nPlease see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/SessionTimeoutSetting " +
                    "for description of the issue and resolution.\n\n");                
            } else {
                System.out.println("Unrecognized mode of operation: " + mode);
                System.out.println("Mode must be view/LRG/premerge");
                System.exit(1);
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
            System.out.println("Processing file: " + f.getAbsolutePath());
            if(f.getAbsolutePath().contains("fusionapps/hcm/components/hcmEss/SuperWeb/public_html/WEB-INF/web.xml"))
                return; //Exemption Bug 18163384
            
            XMLDocument doc = XMLParserHelper.getXMLDocument(f.getAbsolutePath());
            if(doc==null)
                return;
            NodeList timeout = doc.getElementsByTagName("session-timeout");
            if(timeout == null || timeout.getLength() == 0)            
                writer.write(FamilyModuleHelper.getFileNameInfo1(f.getAbsolutePath()) + "Session-timeout missing\n");
            else if(timeout.getLength() > 1)
                writer.write(FamilyModuleHelper.getFileNameInfo1(f.getAbsolutePath()) + "More than one Session-timeout setting\n");
            else{
                Node sessionTimeout = timeout.item(0);
                String timeoutSetting = sessionTimeout.getTextContent();
                if(timeoutSetting == null)
                    timeoutSetting="";
            
                if(f.getAbsolutePath().contains("AtkHomePageSuperWeb") && timeoutSetting.trim().equals("30"))
                    return;
                if(!f.getAbsolutePath().contains("AtkHomePageSuperWeb") && timeoutSetting.trim().equals("30"))
                    return;
            
                writer.write(FamilyModuleHelper.getFileNameInfo1(f.getAbsolutePath()) + "Session timeout setting is: " + timeoutSetting + "\n");
            }            
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("Skipping...");
        }
           
    }
    
    public boolean fileOfInterest(String sName) 
    {
      if(!FamilyModuleHelper.isPathOfInterest(sName))
          return false;  
      if(!sName.contains("components"))
          return false;
     
      if (sName.toLowerCase().contains("superweb") && sName.endsWith("/web.xml"))
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
        if(!issue.equals("SessionTimeoutSetting"))
            continue;
        String fileName = parts[1].trim();
          
        exemptions.add(fileName);
      }
    }catch(Exception e) {
        System.out.println("Skipping exemptions as file could not be found: " + e.getMessage());
    }
    }
}
