package oracle.apps.di;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import oracle.apps.di.checker.BindingsChecker;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;
import oracle.apps.sqlanalyzer.ViewCriteriaHelper;
import oracle.apps.utility.JoesBaseClass;

import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ADFdiSharedInstance  extends JoesBaseClass{
    private static BufferedWriter writer;
    private static HashMap<String,String> voNamesToScan = new HashMap<String,String>();
    private static boolean makeFix = false;
    private static String viewRoot = "";
    private static String mode="view"; //can be view/LRG/premerge
    private static final String issuetype = "ADFdiSharedInstanceVA";
    private static String sCrawlDir = "";
    
    public ADFdiSharedInstance(JoesBaseClass.CRAWL_TYPE type) {
        super(type);
    }
    
    public static void main(String[] args) throws Exception{
          
        writer = new BufferedWriter(new FileWriter("ADFdiSharedInstanceVA.csv"));
        writer.write("FAMILY,MODULE,PRODUCT,FILENAME,LABEL,SERIES,VIEWACCESSOR,SHAREDINSTANCE,PAGEDEF,EXCEL\n");
        
        mode = System.getProperty("mode");
        if(mode == null)
            mode = "view";
        
        if(mode.equals("view")) {
            if(args.length > 1)
                makeFix = new Boolean(args[1].trim());
            
            //sCrawlDir = args[0];
            sCrawlDir = "/ade_autofs/ade_fusion_linux/FUSIONAPPS_PT.R8INT_LINUX.X64.rdd/LATEST/fusionapps";            
            viewRoot = ViewCriteriaHelper.getViewRoot(sCrawlDir);
            
            ADFdiSharedInstance x = new ADFdiSharedInstance(JoesBaseClass.CRAWL_TYPE.XLS); 
            
            //Just construct a global list of VOs to scan in the first pass by crawling the current directory
            System.out.println("Scanning the current directory (" + sCrawlDir + ") to look for Excels...");
            x.crawlDirectory(sCrawlDir,false);
            
            //This should run at top family level fusionapps/fsm/emgc/.. to scan the VOs
            int index = sCrawlDir.indexOf("/components/");
            if(index != -1)
                sCrawlDir = sCrawlDir.substring(0, index+12);
            x = new ADFdiSharedInstance(JoesBaseClass.CRAWL_TYPE.VO_XML);
            System.out.println("Now scanning at the family level (" + sCrawlDir + ") to look for VOs corresponding to those Excels...");
            x.crawlDirectory(sCrawlDir,false);
            
            writer.close();
            if(makeFix){
                System.out.println("Fixing bind variable mismatches");
                x.startDiffLog();     
                x.makeFixes();
                x.closeDiffLog();    
                System.out.println("Done!");
            }               
        } else if(mode.equals("LRG")) {
            
            String series = args[0];
            String label = LRGUtil.getLatestLabel(series);
            System.out.println("Running " + issuetype + " Scan on series: " + series + ".......");
            System.out.println("Latest label is: " + label);
            
            ADFdiSharedInstance x = new ADFdiSharedInstance(JoesBaseClass.CRAWL_TYPE.XLS); 
            
            String sCrawlDir = LRGUtil.getCrawlDir(series, null, label, issuetype);
            System.out.println("Crawling dir: " + sCrawlDir);                    
            x.crawlDirectory(sCrawlDir,false);             
            x = new ADFdiSharedInstance(JoesBaseClass.CRAWL_TYPE.VO_XML);
            x.crawlDirectory(sCrawlDir,false);
            System.out.println("Done crawling directory.");
            writer.close();
                     
            LRGUtil.addLabelInfoToAB(issuetype, series, label);
        } else if(mode.equals("premerge")) {
            //TODO: This may be hard to support as the change itslef may be in VO.xml files and not in adfDi spreadsheet .. 
        }
    }
    
    public void makeFixes() throws Exception {
        
        BufferedReader fileReader = new BufferedReader(new FileReader("ADFdiSharedInstanceVA.csv"));
        String line = fileReader.readLine(); //first line is the header - read & ignore
        
        String prevFilePath = null;
        XMLDocument doc = null;
        XmlFixer xmlFixer = null;
        String fileContentsAsString = null;
        BufferedWriter writer1 = new BufferedWriter(new FileWriter("CouldNotFix.txt"));
        
        while((line = fileReader.readLine()) != null) {
          
          try{
            
            String[] parts = line.split(",");
            if(parts.length < 10)
                continue;
            
            String filePath = viewRoot + parts[3].trim();
            String viewAccessor = parts[6].trim(); 
            String sharedInstanceExisting = parts[7].trim().toLowerCase();
        
            if(!filePath.equals(prevFilePath)) {
              
                if(prevFilePath != null) {
                  fileContentsAsString = xmlFixer.getFileContentsAsString(false);        
                  doADE(true,new File(prevFilePath),fileContentsAsString); 
                }
              
              doc = XMLParserHelper.getXMLDocument(filePath);     
                if(doc == null) 
                    continue;
              xmlFixer = new XmlFixer(filePath);
            }
            prevFilePath = filePath;
            
            if(doc == null) 
                continue;
            
            NodeList viewaccessors = doc.getElementsByTagName("ViewAccessor");            
                       
            for(int i =0; i < viewaccessors.getLength(); i++){
                Node va = viewaccessors.item(i);
                String name = XMLParserHelper.getAttributeValue(va, "Name");
                if(name.equals(viewAccessor)){
                    xmlFixer.modifyAttribute(va, "SharedInstance", sharedInstanceExisting, "true", true);
                    break;
                }                
            }
        } catch (Exception e) {
                e.printStackTrace();
        }
    }
        
    if(xmlFixer != null) {
      fileContentsAsString = xmlFixer.getFileContentsAsString(false);        
      doADE(true,new File(prevFilePath),fileContentsAsString); 
    }
    writer1.close();
}

    protected void processFile(File fName, boolean bDoADE) {        
       
        try {
            String filepath = fName.getAbsolutePath();        
            
            if(filepath.endsWith("xlsx") || filepath.endsWith("xlsm")) {
                
                ADFDiWorkbook workbook = ADFDiWorkbook.getDIWorkbook(filepath, false);
                HashSet<String> pageDefNames = BindingsChecker.getListOfPageDefsInWorkbook(workbook);
                HashSet<String> absoluteageDefPaths = BindingsChecker.getAbsolutePageDefPaths(pageDefNames, workbook.getSpreadsheetPath());
                if (absoluteageDefPaths == null || absoluteageDefPaths.size() < 1) {
                    System.out.println("[ERROR] There was an error resolving page defs for workbook: " + workbook.getSpreadsheetPath());
                    return;
                }
                
                for(Iterator<String> it = absoluteageDefPaths.iterator(); it.hasNext();) {
                    String pagedef = it.next();
                    System.out.println("Processing PageDef: " + pagedef + " corresponding to workbook: " + filepath);
                    try{
                        XMLDocument doc = XMLParserHelper.getXMLDocument(pagedef);
                        if(doc == null)
                            continue;
                        parsePageDef(doc,pagedef,filepath);
                    }catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Skipping pagedef: " + pagedef);
                    }
                }
            } else if(filepath.endsWith("VO.xml")) {
                
                int index = filepath.indexOf("/oracle/apps/");
                if(index != -1) {
                    String pathkey = filepath.substring(index+1).replace("/", ".").replace(".xml","");
                    
                    if(voNamesToScan.containsKey(pathkey)) {
                        
                        String pagedef_workbook = voNamesToScan.get(pathkey);
                        
                        XMLDocument doc = XMLParserHelper.getXMLDocument(filepath);                        
                        NodeList viewAccesors = doc.getElementsByTagName("ViewAccessor");
                       
                        for(int i =0; i < viewAccesors.getLength(); i++) {
                            
                            Node va = viewAccesors.item(i);
                            String sharedInstance = XMLParserHelper.getAttributeValue(va, "SharedInstance"); 
                            if(sharedInstance != null && sharedInstance.equals("true"))
                                continue;
                            
                            writer.write(FamilyModuleHelper.getFileNameInfo1(filepath) + XMLParserHelper.getAttributeValue(va, "Name")
                                         + "," + sharedInstance + "," + pagedef_workbook  + "\n");                    
                        }       
                    }
                }                
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Skipping File: " + fName.getAbsolutePath());
        }
    }
    
    //Just construct a global list of VOs to scan in the first pass
    private void parsePageDef(XMLDocument doc, String pagedefName, String workbookName) throws Exception{
        
        //HashMap<String,String> treeIterBindings = new HashMap<String,String>();
       
        NodeList bindingsNode = doc.getElementsByTagName("bindings");
        if(XMLParserHelper.hasOneNOnlyOneNode(bindingsNode)) {
            NodeList bindingChildList = bindingsNode.item(0).getChildNodes();
            if(bindingChildList != null && bindingChildList.getLength() >0) {
                for(int i =0; i < bindingChildList.getLength(); i++) {
                    Node bindingChild = bindingChildList.item(i);
                    if(XMLParserHelper.isNodeName(bindingChild, "tree")){
                        Node nodeDefinition = XMLParserHelper.getChildNodeWithName(bindingChild, "nodeDefinition");
                        String voName= "";
                        if(nodeDefinition != null)
                            voName = XMLParserHelper.getAttributeValue(nodeDefinition, "DefName");
                        //treeIterBindings.put(XMLParserHelper.getAttributeValue(bindingChild, "id"), voName);
                        voNamesToScan.put(voName,FamilyModuleHelper.getPathAfterFusionapps(pagedefName)+"," 
                                                 + FamilyModuleHelper.getPathAfterFusionapps(workbookName));
                    }
                }
            }
        }
        //return treeIterBindings;
    }

    protected String getSummaryReport() {
        return null;
    }

    protected String getSummaryReportSubject() {
        return null;
    } 
}
