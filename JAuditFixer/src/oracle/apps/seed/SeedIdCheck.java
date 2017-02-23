package oracle.apps.seed;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SeedIdCheck {
    
    static BufferedWriter writer;
    Pattern p = Pattern.compile("<(\\w+?)>(.*?)</\\w+?>");
    HashSet<String> exemptions = new HashSet<String>();
    private static String mode="view";// can be LRG/view/premerge
    private static final String issuetype="SeedIdCheck";
    private static HashMap<String,HashSet<String>> am_surrogates = new HashMap<String,HashSet<String>>();
    Pattern pAm = Pattern.compile("<SEEDDATA.*?am\\s*=\\s*\"(.*?)\"\\s+");
    
    public static void main(String[] args) {
        
        try{
            SeedIdCheck scanner = new SeedIdCheck();
            scanner.generateExemptionList();
            writer = new BufferedWriter(new FileWriter("SeedIdCheck.csv") );        
            writer.write("Family,Module,Product,FileName,Series_Label,NumericId\n"); 
            
            mode = System.getProperty("mode");
            if(mode == null)
                mode = "view";
            
            if(mode.equals("view")) {
            
                //String sCrawlDir = args[0];
                String sCrawlDir="/ade_autofs/ud21_fa/FUSIONAPPS_PT.R8INT_LINUX.X64.rdd/LATEST/fusionapps";
                System.out.println("Analyzing all files in directory (Seed Id check):" + sCrawlDir);      
                scanner.crawlDirectory1(sCrawlDir); 
                System.out.println(am_surrogates);
                scanner.crawlDirectory2(sCrawlDir);
            } else if(mode.equals("LRG")) {
                
                String series = args[0];
                //String series = "FUSIONAPPS_PT.R8INT_LINUX.X64";
                String label = LRGUtil.getLatestLabel(series);
                System.out.println("Running " + issuetype + " Scan on series: " + series + ".......");
                System.out.println("Latest label is: " + label);
                
                ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
                
                for (int j = 0; j < families.size(); j++) {
                    
                    String family = families.get(j);
                    String sCrawlDir = LRGUtil.getCrawlDir(series, family, label, issuetype);
                    System.out.println("Crawling dir '" + sCrawlDir + "' for family: " + family);                    
                   // scanner.crawlDirectory(sCrawlDir);                    
                    System.out.println("Done crawling for family "+family+".");
                }                
                LRGUtil.addLabelInfoToAB(issuetype, series, label);
                
            } else if(mode.equals("premerge")) {
            
//                String[] files = args[0].trim().split("\n");
//                String ade_view_root = args[1].trim();
//                for(int i = 0; i < files.length; i++) {
//                    
//                    String filePath = ade_view_root + "/" + files[i].trim();
//                    File f = new File(filePath);
//                    if(!f.exists())
//                        continue;
//                    if(!scanner.fileOfInterest(filePath))
//                        continue;
//                    scanner.processFile(filePath);
               // }
                writer.write("\n\nPlease see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/SeedIdCheck " +
                    "for description of the issue and resolution.\n\n");
                writer.close();   
                
            } else {
                System.out.println("Unrecognized mode of operation: " + mode);
                System.out.println("Mode must be view/LRG/premerge");
                System.exit(1);
            }          
            writer.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void crawlDirectory2(String path)
    {
        if (path.contains(".ade_path") || path.contains("/classes/"))
            return;
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++)
        {
            if (listOfFiles[i].isFile())
            {
                String sName = listOfFiles[i].getAbsolutePath();
                if (sName.contains(".ade_path"))
                    continue;

                if (fileOfInterest2(sName))
                    processFile2(listOfFiles[i].getAbsolutePath());
                
            } else if (listOfFiles[i].isDirectory())
            {
                if (isDirOfInterest2(listOfFiles[i].getAbsolutePath()))
                    crawlDirectory2(listOfFiles[i].getAbsolutePath());
            } else
                System.out.println("ERROR:  node is neither file or directory: " +
                                   listOfFiles[i]);
        }
    }

    private boolean isDirOfInterest2(String sName)
    {
        if (sName.contains(".ade_path") || sName.contains("/classes/"))
            return false;

        if (sName == null || sName.equals(""))
            return false;
        
        if(sName.contains("/components/"))
            return false; //SD.xml files cannot be found under components
        
        if(!FamilyModuleHelper.isPathOfInterest(sName))
            return false;  

        return true;
    }
    
    public boolean fileOfInterest2(String sName) 
    {
        if(!FamilyModuleHelper.isPathOfInterest(sName))
            return false;  
        
        if (sName.endsWith("SD.xml"))
            return true;
        
        return false;
    }
    
    private void crawlDirectory1(String path)
    {
        if (path.contains(".ade_path") || path.contains("/classes/"))
            return;
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++)
        {
            if (listOfFiles[i].isFile())
            {
                String sName = listOfFiles[i].getAbsolutePath();
                if (sName.contains(".ade_path"))
                    continue;

                if (fileOfInterest1(sName))
                    processFile1(listOfFiles[i].getAbsolutePath());
                
            } else if (listOfFiles[i].isDirectory())
            {
                if (isDirOfInterest1(listOfFiles[i].getAbsolutePath()))
                    crawlDirectory1(listOfFiles[i].getAbsolutePath());
            } else
                System.out.println("ERROR:  node is neither file or directory: " +
                                   listOfFiles[i]);
        }
    }

    private boolean isDirOfInterest1(String sName)
    {
        if (sName.contains(".ade_path") || sName.contains("/classes/"))
            return false;

        if (sName == null || sName.equals(""))
            return false;
        
        if(!FamilyModuleHelper.isPathOfInterest(sName))
            return false;  

        return true;
    }
    
    public boolean fileOfInterest1(String sName) 
    {
        if(!FamilyModuleHelper.isPathOfInterest(sName))
            return false;  
        
        if (sName.endsWith("AM.xml"))
            return true;
        
        return false;
    }
    
    public void processFile1(String fName) {
        try {
            XMLDocument doc = XMLParserHelper.getXMLDocument(fName);
            if(doc == null)
                return;
            
            NodeList appModules = doc.getElementsByTagName("AppModule");
            if(appModules == null || appModules.getLength() != 1) 
            {
              System.out.println("WARNING:  Unexpected number of app modules found for file:  " +fName);
              System.out.println("Skipping...");
              return;
            }
            Node appModule = appModules.item(0);
            Node properties = XMLParserHelper.getChildNodeWithName(appModule, "Properties");
            if(properties == null)
                return;
            Node customProperties = XMLParserHelper.getChildNodeWithName(properties, "CustomProperties");
            if(customProperties == null)
                return;
            ArrayList<Node> props = XMLParserHelper.getChildNodesWithName(customProperties, "Property");
            HashSet<String> surrogates = new HashSet<String>();
            for(int i=0; i< props.size(); i++) {
                Node p = props.get(i);
                String propName = XMLParserHelper.getAttributeValue(p, "Name");
                if(propName == null || !propName.contains("SD_SURROGATE"))
                    continue;
                String propValue = XMLParserHelper.getAttributeValue(p, "Value");
                if(propValue == null || propValue.trim().equals(""))
                    continue;
                String[] parts = propValue.split("\\.");
                surrogates.add(parts[parts.length-1]);
            }
            String am = FamilyModuleHelper.getPathAfterViewRoot(fName);
            am = am.replace(".xml","").replace("/",".");
            if(surrogates.size() > 0)
                am_surrogates.put(am.substring(am.indexOf("oracle.")),surrogates);
            
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    public void processFile2(String fName) {
        try{
            BufferedReader reader = new BufferedReader(new FileReader(fName));
            String line = "";
            int lineNo = 0;
            HashSet<String> violations = new HashSet<String>();
            HashSet<String> ids = new HashSet<String>();
    
            String fileNameInfo = FamilyModuleHelper.getSeedFileNameInfo(fName);
            boolean isEnabled=true;
            String am = "";
            HashSet<String> surrogates = null;
            
            while((line = reader.readLine()) != null) {
                lineNo++;
                line = line.trim();
                
                if(line.contains("SEEDDATA")){
                 Matcher m1 = pAm.matcher(line);
                    if(m1.find()){
                        am= m1.group(1);
                        if(am_surrogates.containsKey(am))
                            surrogates = am_surrogates.get(am);
                    }
                }
               
                if(line.contains("rowkey")){
                    if(isEnabled && ids.size() > 0)                    
                        violations.addAll(ids);
                                
                    isEnabled=true; //reset
                    ids = new HashSet<String>();
                }
                
                
                if(line.contains("EnabledFlag")){
                    Matcher m = p.matcher(line);
                    if(m.find()){
                        String flag = m.group(2).trim();
                        if(flag.equalsIgnoreCase("n"))
                            isEnabled=false;
                    }
                }
                if(line.toLowerCase().contains("id") && !line.contains("isNull")){
                    
                    Matcher m = p.matcher(line);
                    if(m.find()) {
                        String id = m.group(1).trim();
                        if(!id.toLowerCase().endsWith("id"))
                            continue;
                        String idValue = m.group(2).trim();
                        if(!isNumeric(idValue))
                            continue;
                       ids.add(id);
                    }                        
                }        
            }//while
            
            //process the last record
            if(isEnabled && ids.size() > 0)                    
                violations.addAll(ids);
            
            String fileName = FamilyModuleHelper.getPathAfterViewRoot(fName);
            
            for(Iterator<String> it =violations.iterator(); it.hasNext();) {
                String id = it.next();                
                if(exemptions.contains(fileName + "," + id))
                    continue;
                writer.write(fileNameInfo + id+",");
                if(surrogates != null) //&& surrogates.contains(id)
                    writer.write("AM contains surrogate SD\n");
                else
                    writer.write("\n");
            }            
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    private boolean isNumeric(String v) {
        
        try{
            if(isEmpty(v))
                return false;
            int i = Integer.parseInt(v);
            return true;            
        }catch(Exception e) {
            return false;
        }    
    }
    
    private boolean isEmpty(String v) {
        
        if(v == null || v.trim().equals(""))
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
        if(!issue.equals("SeedDataEndDates"))
            continue;
        String fileName = parts[1].trim();
          
        exemptions.add(fileName);
      }
    }catch(Exception e) {
        System.out.println("Skipping exemptions as file could not be found: " + e.getMessage());
    }
 }
   
}
