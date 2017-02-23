package oracle.apps.di;

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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AdfDiVersionCheck {
    
    static Writer writer;
    HashSet<String> exemptions = new HashSet<String>();
    private static String mode="view";// can be LRG/view/premerge
    private static final String issuetype="ADFdiVersionCheckMissing";
    private static int releaseNum=0;    
    
    public static void main(String[] args) throws Exception{
        scan(args);
    }
    
    public static void scan(String[] args) throws Exception{
      AdfDiVersionCheck scanner = new AdfDiVersionCheck();
      scanner.generateExemptionList();        
      FamilyModuleHelper.generateDeferedPaths();
      writer = new BufferedWriter(new FileWriter("ADFdiVersionCheckMissing.csv") );
      writer.write("Family, Module, SubModule, Filename, Series, Label, Description\n");     
      
      mode = System.getProperty("mode");
      if(mode == null)
          mode = "view";
      
      if(mode.equals("view")) {        
          String sCrawlDir = args[0].trim();
          //String sCrawlDir = "/ade_autofs/ud21_fa/FUSIONAPPS_PT.INTERPSCM_LINUX.X64.rdd/130523.1007.S/fusionapps/scm/components/costManagement/cml/";
          
          if(args.length > 1){
              String series = args[1].trim();
              String release= FamilyModuleHelper.getRelease(series);
              try{              
                  releaseNum = Integer.parseInt(release);
              }catch(NumberFormatException e){
                 e.printStackTrace();
                 releaseNum=0;
              }
          }
          System.out.println("Analyzing all files in directory (ADFdi version check):" + sCrawlDir);       
          scanner.crawlDirectory(sCrawlDir); 
      } else if(mode.equals("LRG")) {
          String series = args[0];
          
          String release= FamilyModuleHelper.getRelease(series);
          try{              
              releaseNum = Integer.parseInt(release);
          }catch(NumberFormatException e){
             e.printStackTrace();
             releaseNum=0;
          }
          String label = LRGUtil.getLatestLabel(series);
          System.out.println("Running " + issuetype + " Scan on series: " + series + ".......");
          System.out.println("Latest label is: " + label);
          
          String sCrawlDir = LRGUtil.getCrawlDir(series, null, label, issuetype);
          System.out.println("Crawling dir: " + sCrawlDir);                    
          scanner.crawlDirectory(sCrawlDir);                    
          System.out.println("Done crawling directory.");
                   
          LRGUtil.addLabelInfoToAB(issuetype, series, label);
      }else if(mode.equals("premerge")) {
          
              //String[] files = args[0].trim().split("\n");
              String ade_view_root = args[1].trim();
              String filelistpath=args[0].trim();
              String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root);
              
              String label = args[2].trim();
              
              String release= FamilyModuleHelper.getRelease(label);
              System.out.println("Release: " + release);
              
              try{              
                  releaseNum = Integer.parseInt(release);
                  if (releaseNum < 9) //This check was mandated post release 9 only, hence this check is necessary to avoid the current rel7 and rel8 branches from failing
                      return;              
              }catch(NumberFormatException e){
                  System.out.println("Could not determine release. Skipping ADFdiVersionCheck...");
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
              writer.write("\n\nPlease see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/AdfdiVersionConfiguration " +
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
        if(FamilyModuleHelper.isDeferredPath(releaseNum, FamilyModuleHelper.getPathAfterViewRoot(path)))
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
            XMLDocument doc = XMLParserHelper.getXMLDocument(f.getAbsolutePath());
            NodeList servlets = doc.getElementsByTagName("servlet");
            
            boolean isViolation = false;
            boolean adfdiservletfound = false;
            for(int i = 0;i < servlets.getLength(); i++) {
                
                Node servlet = servlets.item(i);
                Node servletName = XMLParserHelper.getChildNodeWithName(servlet, "servlet-name");
                String servlet_name = "";
                if(servletName != null)
                    servlet_name = servletName.getTextContent();
                
                if(servlet_name.equals("adfdiRemote")) {
                    adfdiservletfound = true;
                    isViolation = true; //potential violation found
                    ArrayList<Node> initParams = XMLParserHelper.getChildNodesWithName(servlet, "init-param");
                    
                    for(int j = 0; j < initParams.size(); j++) {                        
                        Node initParam = initParams.get(j);
                        String childParamName = XMLParserHelper.getChildNodeWithName(initParam, "param-name").getTextContent().trim();
                        if(childParamName.equals("ClientServerVersionCheck.Precision")) {
                            String childParamValue = XMLParserHelper.getChildNodeWithName(initParam, "param-value").getTextContent().trim();
                            if(childParamValue.equals("5"))
                                isViolation=false;
                            break;
                        }                        
                    }     
                    if(isViolation &&  !exemptions.contains(FamilyModuleHelper.getPathAfterViewRoot(f.getAbsolutePath())))
                        writer.write(FamilyModuleHelper.getFileNameInfo1(f.getAbsolutePath()) + "ADFdi Version Check not configured properly\n");
                    break;
                }
            }
            if(!adfdiservletfound) {
                String filecontents = FileParserHelper.getFileContentsAsString(f.getAbsolutePath());
                if(filecontents.contains("adfdiRemote") && !exemptions.contains(FamilyModuleHelper.getPathAfterViewRoot(f.getAbsolutePath())))
                    writer.write(FamilyModuleHelper.getFileNameInfo1(f.getAbsolutePath()) + "ADFdi Version Check not configured properly\n");
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
        if(!issue.equals("ADFdiVersionCheck"))
            continue;
        String fileName = parts[1].trim();
          
        exemptions.add(fileName);
      }
    }catch(Exception e) {
        System.out.println("Skipping exemptions as file could not be found: " + e.getMessage());
    }
    }
}
