package oracle.apps.psr;

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

import oracle.apps.NullEntityResolver;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.utility.JoesBaseClass;

import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UnusedTaskFlowsLRG extends JoesBaseClass{
    
    private int m_page_reviewCount = 0;
    private int m_pageDef_reviewCount = 0;
    private String m_sSummaryReport = "";
    
    private HashSet<String> m_pageList = new HashSet<String>();
    private HashMap<String,String> m_pageDefList = new HashMap<String,String>();
    private HashSet<String> m_CPXList = new HashSet<String>();
    private HashMap<String, String> m_usageIdToPageDef = new HashMap<String, String>();
    private HashMap<String,String> m_pageToPageDef = new HashMap<String,String>();
    private HashMap<String,String> m_PageToCpx = new HashMap<String,String>();
    
    private boolean bDebug = false;
    
    private static BufferedWriter writer;
    private static HashMap<String,HashSet> exemptions = new HashMap<String,HashSet>();
    private static final Pattern pTaskFlowUsage = Pattern.compile("bindings\\.(\\w+)\\.");
    private static final String issuetype = "UnusedTaskFlow";
    
    public UnusedTaskFlowsLRG() 
    {
        super(JoesBaseClass.CRAWL_TYPE.JSPX_AND_JSFF_AND_PAGE_DEF_AND_CPX);
    }

    protected String getSummaryReportSubject() 
    {
        return "SCRIPT: Find unused task flows";
    }

    protected String getSummaryReport() 
    {
        m_sSummaryReport = "For this run I reviewed a total of " +m_page_reviewCount +" pages.\n" + m_sSummaryReport;
        m_sSummaryReport = "For this run I reviewed a total of " +m_pageDef_reviewCount +" pageDefs.\n" + m_sSummaryReport;

        return m_sSummaryReport;
    }

    protected void processPage(String f, boolean bDoADE) 
    {
      m_page_reviewCount++;
      if(bDebug)
         System.out.println("Checking Page:  " +f.toString());

        try {
            DOMParser parser = new DOMParser();
            parser.setDebugMode(true);
            parser.setErrorStream(System.out);
            parser.setEntityResolver(new NullEntityResolver());
            parser.showWarnings(true);
            parser.setValidationMode(DOMParser.NONVALIDATING);

            parser.parse("file:" + f);

            XMLDocument doc = parser.getDocument();
            NodeList adfc = doc.getElementsByTagName("jsp:root");
            if (adfc == null) 
            {
                System.out.println("REVIEW:  Not a page file ---> " + f);
                return;
            }

          NodeList regionList = doc.getElementsByTagName("region");
          HashSet<String> usedTaskFlows = new HashSet<String>();

          for (int i = 0; i < regionList.getLength(); i++) 
          {
            Node iter = regionList.item(i);
            NamedNodeMap attributes = iter.getAttributes();
            Node valueNode = attributes.getNamedItem("value");
            if(valueNode == null)
                continue;
            String value = valueNode.getNodeValue();
            Matcher m = pTaskFlowUsage.matcher(value);
            if(m.find())
                usedTaskFlows.add(m.group(1));   
          }
            
            int index = f.indexOf("public_html") +11;
            String key = f.substring(index);
            String pageDef = m_pageToPageDef.get(key);
            String cpx = m_PageToCpx.get(key);
            if(pageDef != null)
                reviewPageDef(pageDef,usedTaskFlows,FamilyModuleHelper.getPathAfterViewRoot(f),cpx);
            
        } catch (Exception e) {
            e.printStackTrace();
            logException(f, e);
            System.out.println("ERROR:  Error while processing file.  review carefully if the script updated it. " +f);
            return;
        }
    }

    protected void reviewPageDef(String pageDef, HashSet<String> usedTaskFlows,String jsff, String cpx) {
        m_pageDef_reviewCount++;
        try {
            File f = new File(pageDef);
            DOMParser parser = new DOMParser();
            parser.setDebugMode(true);
            parser.setErrorStream(System.out);
            parser.setEntityResolver(new NullEntityResolver());
            parser.showWarnings(true);
            parser.setValidationMode(DOMParser.NONVALIDATING);
            
            if(f.exists())
               parser.parse("file:" + pageDef);
            else return;

            XMLDocument doc = parser.getDocument();
            NodeList adfc = doc.getElementsByTagName("pageDefinition");
            if (adfc == null) 
            {
              if(bDebug)
                System.out.println("REVIEW:  Not a pageDefinition file ---> " + pageDef);
               return;
            }

          NodeList taskFlowList = doc.getElementsByTagName("taskFlow");

          for (int i = 0; i < taskFlowList.getLength(); i++) 
          {
            Node taskFlow = taskFlowList.item(i);
            
            String id = XMLParserHelper.getAttributeValue(taskFlow, "id");
            
            boolean isUsed = usedTaskFlows.contains(id);
              
            if(isUsed)
                continue;
            if(id.equals("attachmentRepositoryBrowseTaskFlow1") || id.contains("AdvancedSearchFlow")
                || id.equals("editTaskflow") || id.equals("searchTaskflow") || id.equals("pageeditorpanel")
                || id.equals("createTaskflow") || id.equals("delTaskflow") || id.equals("dupTaskflow") 
                || id.equals("dynamicRegion1") || id.equals("ManageLinkFlow1") || id.equals("ManageLinkFlow2"))
                continue;
              
            if(isExempt(id,FamilyModuleHelper.getPathAfterViewRoot(pageDef)))
                continue;
           
            writer.write(FamilyModuleHelper.getFileNameInfo(pageDef) + jsff+ "," + cpx + "," + id + "," + XMLParserHelper.getAttributeValue(taskFlow, "taskFlowId") 
                + "," + XMLParserHelper.getAttributeValue(taskFlow, "activation")
                + "," + XMLParserHelper.getAttributeValue(taskFlow, "active")
                + "," + XMLParserHelper.getAttributeValue(taskFlow, "Refresh") + "," + 
                XMLParserHelper.getAttributeValue(taskFlow, "RefreshCondition") + "," + isUsed + "\n");           
            
          }
        } catch (Exception e) {
            e.printStackTrace();
            logException(pageDef, e);
            System.out.println("ERROR:  Error while processing file.  review carefully if the script updated it. " +pageDef);
            return;
        }
        return;
    }
   
    public void processFile(File fName, boolean bDoADE) 
    {
      String fileName = fName.getPath();

      if(fileName.endsWith(".jsff") || fileName.endsWith(".jspx"))
          m_pageList.add(fileName);
      
    //        processPage(fName, bDoADE);
      
          if(fileName.contains("PageDef"))
              if (fileName.matches(".*PageDef[0-9]*\\.xml")){
                  String relPath = fileName;
                  int index = fileName.indexOf("oracle/apps");
                  if(index != -1)
                      relPath = fileName.substring(index);
                m_pageDefList.put(relPath,fileName);
              }

      if(fileName.endsWith(".cpx"))
          m_CPXList.add(fileName);
    //        processPageDef(fName, bDoADE);
    }

    private String getRealPageDefPath(String path) 
    {
        String actual = path.replaceAll("\\.", "/") +".xml";
        
        if(m_pageDefList.containsKey(actual))
            return m_pageDefList.get(actual);
        
        return "not found";
    }

      private void digestCPX(String f) 
      {   
          if(bDebug)
            System.out.println("Reviewing CPX:  " + f);
          try {
              DOMParser parser = new DOMParser();
              parser.setDebugMode(true);
              parser.setErrorStream(System.out);
              parser.setEntityResolver(new NullEntityResolver());
              parser.showWarnings(true);
              parser.setValidationMode(DOMParser.NONVALIDATING);

              parser.parse("file:" + f);

              XMLDocument doc = parser.getDocument();

          NodeList pageMaps = doc.getElementsByTagName("pageMap");
          NodeList pageDefUsages = doc.getElementsByTagName("pageDefinitionUsages");
          if(pageMaps.getLength() != 1 || pageDefUsages.getLength() != 1)
          {
              System.out.println("ERROR:  Unexpected number of pageMap/pageDefinitionUsages sections in file ---> " +f);
              return;
          }        
              
          Node pageDefUsage = pageDefUsages.item(0);
          NodeList usages = pageDefUsage.getChildNodes();
              
          for(int i = 0; i < usages.getLength(); i++) 
          {
              Node page = usages.item(i);
              if(page.getNodeType() == Node.TEXT_NODE || page.getNodeType() == Node.COMMENT_NODE)
                  continue;

              NamedNodeMap attributes = page.getAttributes();
              Node usageId = attributes.getNamedItem("id");
              Node path = attributes.getNamedItem("path");
              
              if(bDebug)
                 System.out.println("found path and usageId --> " + path.getNodeValue() + ",    " +usageId.getNodeValue());
              String nv = path.getNodeValue();


            String sUsageId = usageId.getNodeValue();
           
              String realPath = getRealPageDefPath(nv);                
              m_usageIdToPageDef.put(sUsageId, realPath);
          }
              
          Node pageMap = pageMaps.item(0);
          NodeList pages = pageMap.getChildNodes();
              
          for(int i = 0; i < pages.getLength(); i++) 
          {
              Node page = pages.item(i);
              if(page.getNodeType() == Node.TEXT_NODE || page.getNodeType() == Node.COMMENT_NODE)
                  continue;

              NamedNodeMap attributes = page.getAttributes();

              Node path = attributes.getNamedItem("path");
              Node usageId = attributes.getNamedItem("usageId");
              
              if(bDebug)
                System.out.println("found path and usageId --> " + path.getNodeValue() + ",    " +usageId.getNodeValue());
              //check if it is there already
              String sPage = path.getNodeValue();
              String sUsageId = usageId.getNodeValue();   
              
              String pagedef = m_usageIdToPageDef.get(sUsageId);
              
              if(pagedef != null) {
                  m_pageToPageDef.put(sPage,pagedef);
                  m_PageToCpx.put(sPage,FamilyModuleHelper.getPathAfterViewRoot(f));
              }
              
          }
          } catch (Exception e) {
              e.printStackTrace();
              logException(f, e);
              System.out.println("ERROR:  Error while processing file.  review carefully if the script updated it. " +f);
              return;
          }
      }

    public void processFiles(boolean bDoADE) 
    {
      // first load the cpx so we have a mapping between pages and pageDefs
        for(Iterator i = m_CPXList.iterator() ; i.hasNext();)
          digestCPX((String)i.next());
        
        for(Iterator i = m_pageList.iterator(); i.hasNext();)
            processPage((String)i.next(), bDoADE);
    }

    public static void main(String[] args) {
        try {
            if (args.length != 1 || args[0] == null || args[0].equals("")) {
                System.out.println("Series has to be specified for " +
                                   issuetype);
                System.exit(1);
            }

            String series = args[0];
    
            String label = LRGUtil.getLatestLabel(series);
            System.out.println("Running " + issuetype + " Scan on series: " +
                               series + ".......");
            System.out.println("Latest label is: " + label);

            writer = new BufferedWriter(new FileWriter("UnusedTaskFlow_LRG.csv"));
            writer.write("Family,Module,Product,PageDef Filename,Label,JsffFileName,CpxFileName,Id,TaskFlowId,Activation,Active(condition),Refresh,RefreshCondition,IsUsed?\n");
            UnusedTaskFlowsLRG x = new UnusedTaskFlowsLRG();
            generateExemptionList();
          
          String serverTop = FamilyModuleHelper.getLabelServerTop1(series);
          String viewRoot = serverTop + "/"+series+".rdd/"+label+"/";

            ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);          
          
            for (int j = 0; j < families.size(); j++) {
              
                String family = families.get(j);
              String sCrawlDir = LRGUtil.getCrawlDirNew(series, family, "RowCountThresholdNew");
              
              System.out.println("Crawling dir '" + viewRoot + sCrawlDir + "' for family: " + family);                
              x.crawlDirectoryNew(viewRoot,sCrawlDir,false);   
            }
            x.processFiles(false);
            writer.close();

            LRGUtil.addLabelInfoToAB(issuetype, series, label);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void generateExemptionList() {
        
    try{
      BufferedReader fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
            "/fatools/opensource/jauditFixScripts/txt/angrybirdCodescanExemptions.txt"));
      String line = null;
      
      while((line = fileReader.readLine()) != null) {
        String[] parts = line.split(",");
        if(parts.length < 3)
            continue;
        String issue = parts[2].trim();
        if(!issue.equals("Unused TaskFlow"))
            continue;
        String fileName = parts[0].trim();
        String taskFlowId = parts[1].trim();
          
        HashSet<String> exemptLines = null;
        if(!exemptions.containsKey(fileName)) 
          exemptLines = new HashSet<String>();
        else 
          exemptLines = exemptions.get(fileName);
        
        exemptLines.add(taskFlowId);
        exemptions.put(fileName, exemptLines);
      }
    }catch(Exception e) {
        e.printStackTrace();
    }
    }
    
    private boolean isExempt(String id, String pageDef){
        
        if(!exemptions.containsKey(pageDef)) 
            return false;
        
        HashSet<String> lines = (HashSet<String>)exemptions.get(pageDef);
        if(lines.contains(id))
            return true;
        
        return false;
    }
}
