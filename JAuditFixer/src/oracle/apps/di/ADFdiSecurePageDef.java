package oracle.apps.di;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import oracle.apps.di.checker.BindingsChecker;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.utility.JoesBaseClass;
import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class ADFdiSecurePageDef extends JoesBaseClass{
    private static BufferedWriter writer;
    private static String mode="view"; //can be view/LRG/premerge
    private static final String issuetype = "AdfDiSecurePageDef";
    private static String sCrawlDir = "";
    private static HashSet<String> applicationSecureResources = new HashSet<String>();
    private static HashSet<String> applicationSecureMemberResources = new HashSet<String>();
    
    public ADFdiSecurePageDef(JoesBaseClass.CRAWL_TYPE type) {
        super(type);
    }
    
    public static void main(String[] args) throws Exception{
      
        scan(args);
        
    }
    
    public static void scan(String[] args) throws Exception{
      
      writer = new BufferedWriter(new FileWriter("AdfDiSecurePageDef.csv"));
      writer.write("FAMILY,MODULE,PRODUCT,FILENAME,LABEL,SERIES,PageDef,Found in Resources, Found in Member Resources\n");
      ADFdiSecurePageDef x = new ADFdiSecurePageDef(JoesBaseClass.CRAWL_TYPE.XLS); 
      
      mode = System.getProperty("mode");
      if(mode == null)
          mode = "view";
      
      if(mode.equals("view")) {
          sCrawlDir = args[0];
          //sCrawlDir = "/ade_autofs/ade_fusion_linux/FUSIONAPPS_PT.R8INT_LINUX.X64.rdd/LATEST/fusionapps/fin/components/";            
          x.scanJazns(sCrawlDir);            
          System.out.println("Scanning the current directory (" + sCrawlDir + ") to look for Excels...");            
          x.crawlDirectory(sCrawlDir,false);            
          
      } else if(mode.equals("LRG")) {
          
          String series = args[0];
          //String series = "FUSIONAPPS_PT.R8INT_LINUX.X64";
          String label = LRGUtil.getLatestLabel(series);
          System.out.println("Running " + issuetype + " Scan on series: " + series + ".......");
          System.out.println("Latest label is: " + label);
          
          ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
          for (int j = 0; j < families.size(); j++) {
              String family = families.get(j);
              sCrawlDir = LRGUtil.getCrawlDir(series, family, label, issuetype);
              
              applicationSecureResources = new HashSet<String>();
              applicationSecureMemberResources = new HashSet<String>();
              
              x.scanJazns(sCrawlDir);                     
              System.out.println("Crawling dir '" + sCrawlDir +"' for family: " + family);
              x.crawlDirectory(sCrawlDir,false);
              System.out.println("Crawling done for family "+family+".");
              writer.flush();
          }                     
          LRGUtil.addLabelInfoToAB(issuetype, series, label);
      } else if(mode.equals("premerge")) {
          
      }
      
      writer.close();
    }
    
    private void scanJazns(String sCrawlDir) {
        
        ArrayList<String> jazns = new ArrayList<String>();
        int index = sCrawlDir.indexOf("/components");
        if(index == -1) {
            System.out.println("invalid path !!!!");
            return;
        }
        
        String afterComponents = sCrawlDir.substring(index+11);
        if(afterComponents.startsWith("/")) {
            afterComponents= afterComponents.substring(1);
            index++;
        }
        
        int index1 = afterComponents.indexOf("/");
        if(index1 != -1)
         jazns.add(sCrawlDir.substring(0, index+11+index1)+ "/src/META-INF/jazn-data.xml");
        else if(afterComponents.trim().length() > 1)
            jazns.add(sCrawlDir + "/src/META-INF/jazn-data.xml");
        else {
            String pathToScan=sCrawlDir.substring(0, index+11);
            File folder = new File(pathToScan);
            File[] listOfFiles = folder.listFiles(); 
            for(int i = 0; i <listOfFiles.length; i++)
            {
                String sName = listOfFiles[i].getAbsolutePath();
                if(listOfFiles[i].isDirectory()) {
                    File f = new File(sName + "/src/META-INF/jazn-data.xml");
                    if(f.exists())
                        jazns.add(f.getAbsolutePath());
                }
            }
        }
        
        System.out.println("Jazns: " + jazns);       
        for(int i =0; i< jazns.size(); i++) {
            
            String jazn = jazns.get(i);            
            try {
                XMLDocument doc = XMLParserHelper.getXMLDocument(jazn);
                if(doc == null)
                    continue;
                
                NodeList resources = doc.getElementsByTagName("resource");
                HashSet<String> resourceNames = new HashSet<String>();
                for(int j = 0; j < resources.getLength(); j++) {
                    Node resource = resources.item(j);
                    Node child = XMLParserHelper.getChildNodeWithName(resource, "name");
                    if(child != null){
                        String resourceName=child.getTextContent();
                        if(resourceName.contains("PageDef"))
                            resourceNames.add(resourceName);
                    }
                }
                
                applicationSecureResources.addAll(resourceNames);
                
                NodeList memberresources = doc.getElementsByTagName("member-resource");
                HashSet<String> memberresourceNames = new HashSet<String>();
                
                for(int j = 0; j < memberresources.getLength(); j++) {
                    Node memresource = memberresources.item(j);
                    Node child = XMLParserHelper.getChildNodeWithName(memresource, "resource-name");
                    if(child != null){
                        String resourceName=child.getTextContent();
                        if(resourceName.contains("PageDef"))
                            memberresourceNames.add(resourceName);
                    }
                }
                
                applicationSecureMemberResources.addAll(memberresourceNames);
                
            }catch(Exception e) {
                e.printStackTrace();
                System.out.println("Skipping...");
            }
        }
    }
    

    protected void processFile(File fName, boolean doADE) {        
       
        try {
            String filepath = fName.getAbsolutePath();        
                
            ADFDiWorkbook workbook = ADFDiWorkbook.getDIWorkbook(filepath, false);
            HashSet<String> pageDefNames = BindingsChecker.getListOfPageDefsInWorkbook(workbook);
            HashSet<String> absoluteageDefPaths = BindingsChecker.getAbsolutePageDefPaths(pageDefNames, workbook.getSpreadsheetPath());
            if (absoluteageDefPaths == null || absoluteageDefPaths.size() < 1) {
                System.out.println("[ERROR] There was an error resolving page defs for workbook: " + workbook.getSpreadsheetPath());
                return;
            }
            
            for(Iterator<String> it = absoluteageDefPaths.iterator(); it.hasNext();) {
                String pagedef = it.next();
                pagedef = FamilyModuleHelper.getPathAfterViewRoot(pagedef).replace(".xml", "").replace("/", ".");
                int index = pagedef.indexOf("oracle.");
                if(index != -1)
                    pagedef = pagedef.substring(index);
                
                boolean resourceFound = false;
                boolean memberResourceFound = false;
                if(applicationSecureResources.contains(pagedef))
                    resourceFound=true;
                if(applicationSecureMemberResources.contains(pagedef))
                    memberResourceFound = true;
                if(resourceFound && memberResourceFound)
                    continue;
                
                writer.write(FamilyModuleHelper.getFileNameInfo1(filepath) + pagedef + "," + "," +resourceFound + "," + memberResourceFound + "\n");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Skipping File: " + fName.getAbsolutePath());
        }
    }

    protected String getSummaryReport() {
        return null;
    }

    protected String getSummaryReportSubject() {
        return null;
    }
}
