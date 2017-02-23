package oracle.apps.sqlanalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.lovWidth.ModifiedComboboxParser;
import oracle.apps.utility.JoesBaseClass;

import oracle.apps.votuning.UiFileHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class ViewCriteriaUIPerformance extends JoesBaseClass{
    
    int m_page_reviewCount = 0;
    int m_pageDef_reviewCount = 0;

    private String m_sSummaryReport = "";
    
    HashSet<String> m_pageList = new HashSet<String>();
    HashMap<String,String> m_pageDefList = new HashMap<String,String>();
    HashSet<String> m_CPXList = new HashSet<String>();
    
    HashMap<String, String> m_usageIdToPageDef = new HashMap<String, String>();
    HashMap<String,String> m_pageToPageDef = new HashMap<String,String>();
    HashMap<String,String> m_PageToCpx = new HashMap<String,String>();
    
    static BufferedWriter writer;
    static BufferedWriter writer1;
    String bcPath = "";
    static int nestedVCs = 0;
    static int nonNestedVCs = 0;
    static String viewRoot = "";
    
    static HashSet<String> exemptions = new HashSet<String>();
    
    boolean bDebug = false;
    static String  sCrawlDir;
    static boolean append = false;
    
    static Pattern pModelUsage = Pattern.compile("bindings\\.(\\w+)\\.");
    static Pattern pTablesColumn = Pattern.compile("(.*?)\\((.*?)\\)");
    
    static HashMap<String,HashSet> function_indexColumns; //function
    static HashMap<String,HashSet> default_indexColumns;  //Default
    static String m_family;
   
    static boolean refreshFromDB = false; 
    //Store the viwattribute mappings for the current VO being processed
    private HashMap<String,Node> vaAliasAttributeMap = new HashMap<String,Node>();
    private HashMap<String,Node> vaNameAttributeMap = new HashMap<String,Node>();
    private HashMap<String,Node> extendedVaAliasAttributeMap = new HashMap<String,Node>();
    private HashMap<String,Node> extendedVaNameAttributeMap = new HashMap<String,Node>();
    
    //Store the mappings for userdefined SQLQuery
    HashMap<String,HashSet> colNameTableNames = new HashMap<String,HashSet>();
    HashMap<String,String> colAliasName = new HashMap<String,String>();
    
    HashMap<String,HashSet> extendedColNameTableNames = new HashMap<String,HashSet>();
    HashMap<String,String> extendedColAliasName = new HashMap<String,String>();
    ModifiedComboboxParser parser;
    private Node baseViewObject;
    
    public static boolean isView = false;
    static String indexedTableName = " ";
    public static String resolvedTableColumn = " ";
    static HashSet<String> vosAlreadyProcessed = new HashSet<String>();
   // static HashMap<String,HashSet<String>> vosProcessedJsffs = new HashMap<String,HashSet<String>>();
    static HashSet<LovVO2> lovVosToProcess = new HashSet<LovVO2>();
    
    static int listbindingsNotFound = 0;
    static int tableiterNotFound = 0;
    static int iterbindingsNotFound = 0;
    boolean reqdVCFound = false;
    static HashMap<String,XMLDocument> voXmlsFromOtherJars = new HashMap<String,XMLDocument>();
    
    public ViewCriteriaUIPerformance() 
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
        
        m_sSummaryReport += "Total number of nested VCs found: " + nestedVCs + "\n" + m_sSummaryReport;
        m_sSummaryReport += "Total number of Non-nested VCs found that could not be handles: " + nonNestedVCs + "\n" + m_sSummaryReport;
       
        m_sSummaryReport += "Number of list bindings not found: " + listbindingsNotFound + "\n" + m_sSummaryReport;
        m_sSummaryReport += "Number of table iterators not found: " + tableiterNotFound + "\n" + m_sSummaryReport;
        m_sSummaryReport += "Number of iterator bindings not found: " + iterbindingsNotFound + "\n" + m_sSummaryReport;
        return m_sSummaryReport;
    }

    protected void processPage(String f) 
    {
      m_page_reviewCount++;
      if(bDebug)
         System.out.println("Checking Page:  " +f.toString());

        try {
            XMLDocument doc = XMLParserHelper.getXMLDocument(f);
            if(doc == null)
                return;
            NodeList adfc = doc.getElementsByTagName("jsp:root");
            if (adfc == null) 
            {
                System.out.println("REVIEW:  Not a page file ---> " + f);
                return;
            }

          NodeList queryList1 = doc.getElementsByTagName("query");
          //NodeList queryList2 = doc.getElementsByTagName("quickQuery"); //quickQuery is always on implicit VC (optional) and results in case-insensitive seacrh by default
          TreeSet<String> usedQueryModels = new TreeSet<String>();

          for (int i = 0; i < queryList1.getLength(); i++) 
          {
            Node iter = queryList1.item(i);
              
              String rendered = XMLParserHelper.getAttributeValue(iter, "rendered");
              if(!ViewCriteriaHelper.isEmpty(rendered) && rendered.equals("false"))
                  continue;
              
              String value = XMLParserHelper.getAttributeValue(iter,"model");
                if(ViewCriteriaHelper.isEmpty(value))
                    continue;
            Matcher m = pModelUsage.matcher(value);
            if(m.find())
                usedQueryModels.add(m.group(1));  
            else
                System.out.println("Weird af:query model");           
          }
            
           /* for (int i = 0; i < queryList2.getLength(); i++) 
            {
              Node iter = queryList2.item(i);
              NamedNodeMap attributes = iter.getAttributes();
              Node modelNode = attributes.getNamedItem("model");
              if(modelNode == null)
                  continue;
              String value = modelNode.getNodeValue();
              Matcher m = pModelUsage.matcher(value);
                if(m.find())
                  usedModels.add(m.group(1));                
                else
                  System.out.println("Weird af:query model");           
            }*/
            
           NodeList lovList1 = doc.getElementsByTagName("inputComboboxListOfValues");
           NodeList lovList2 = doc.getElementsByTagName("inputListOfValues"); 
           HashSet<Node> lovNodes = new HashSet<Node>();

            for (int i = 0; i < lovList1.getLength(); i++)     {  
              Node lovNode = lovList1.item(i);
              String rendered = XMLParserHelper.getAttributeValue(lovNode, "rendered");
              if(!ViewCriteriaHelper.isEmpty(rendered) && rendered.equals("false"))
                 continue;
                
              lovNodes.add(lovNode);                
            }
            
            for (int i = 0; i < lovList2.getLength(); i++) {
                Node lovNode = lovList2.item(i);
                String rendered = XMLParserHelper.getAttributeValue(lovNode, "rendered");
                if(!ViewCriteriaHelper.isEmpty(rendered) && rendered.equals("false"))
                    continue;
                
                lovNodes.add(lovNode);   
            }
             
            
            int index = f.indexOf("public_html") +11;
            String key = f.substring(index);
            String pageDef = m_pageToPageDef.get(key);
            String cpx = m_PageToCpx.get(key);
            if(pageDef == null)
                System.out.println("Could not find padedef for page:" + key);
            else
                reviewPageDef(pageDef,usedQueryModels,f,cpx,lovNodes);
            
        } catch (Exception e) {
            e.printStackTrace();
            logException(f, e);
            System.out.println("ERROR:  Error while processing file.  review carefully if the script updated it. " +f);
            return;
        }
    }

    protected void reviewPageDef(String pageDef, TreeSet<String> usedModels, String jsff, String cpx, HashSet<Node> lovNodes) {
     
        m_pageDef_reviewCount++;
      //  System.out.println("Reviewing pagedef: " + pageDef);
      lovVosToProcess.clear();

        try {

            XMLDocument doc = XMLParserHelper.getXMLDocument(pageDef);
            if(doc == null)
                return;
            NodeList adfc = doc.getElementsByTagName("pageDefinition");
            if (adfc == null) 
            {
               System.out.println("REVIEW:  Not a pageDefinition file ---> " + pageDef);
               return;
            }
            
            //Build a map of id --> iterator Node
            HashMap<String,Node> iteratorNodes = new HashMap<String,Node>(); 
            HashMap<String,Node> listNodes = new HashMap<String,Node>(); //list and listOfValues
            HashMap<String,Node> treeNodes = new HashMap<String,Node>();
            
            ViewCriteriaHelper.parsePageDef(pageDef,iteratorNodes,listNodes,treeNodes);

          NodeList searchRegionList = doc.getElementsByTagName("searchRegion");

          for (int i = 0; i < searchRegionList.getLength(); i++) 
          {
              Node searchRegion = searchRegionList.item(i);
              
              try{                
                
                String id = XMLParserHelper.getAttributeValue(searchRegion, "id");                
                boolean isUsed = usedModels.contains(id);                  
                if(!isUsed)
                    continue;
                  
               //  get Binds attr value and get the iterator from the iterator map
                  String searchRegionBinds = XMLParserHelper.getAttributeValue(searchRegion, "Binds");
                  String criteria = XMLParserHelper.getAttributeValue(searchRegion, "Criteria");
                  
                  if(criteria == null || criteria.equals("null"))
                      continue;
                  
                  Node iterator = iteratorNodes.get(searchRegionBinds);
                  String voUsage = XMLParserHelper.getAttributeValue(iterator, "Binds");
                  
                  String dataControl = XMLParserHelper.getAttributeValue(iterator, "DataControl");
                  
                  String amName = ViewCriteriaHelper.getAMNameFromDataControlName(dataControl);
                  String dcPackage = ViewCriteriaHelper.getDataControlPackage(dataControl,cpx);
                  
                  String amAbsPath = ViewCriteriaHelper.getAmFile(dcPackage + "." + amName, jsff);
                  ArrayList<String> viewUsageVCs = new ArrayList<String>();
                  String voFullPackage = ViewCriteriaHelper.getViewUsagePackageFromAm(amAbsPath, voUsage,viewUsageVCs);
                 
                  String modelJprLocation = UiFileHelper.getJprLocationForModelFile(amAbsPath);
                  
                  parser = new ModifiedComboboxParser(m_family.toLowerCase(), ModifiedComboboxParser.ScanType.ALL);
                  parser.setJprAbsFilePath(modelJprLocation);
                  
                  bcPath = "";
                  
                  Object[] returnObjects = ViewCriteriaHelper.getViewObject(modelJprLocation, voFullPackage, jsff);
                  XMLDocument voXml = (XMLDocument)returnObjects[0];
                  bcPath = (String)returnObjects[1];
                  
                  if(voXml == null)
                      continue;
                  
                  String voProcessedKey = bcPath+criteria;
                  for(int j = 0; j < viewUsageVCs.size(); j++)
                      voProcessedKey += viewUsageVCs.get(j);
                  
                  voProcessedKey+=jsff;
                  
                  if(vosAlreadyProcessed.contains(voProcessedKey)) {
//                     HashSet<String> temp = vosProcessedJsffs.get(voProcessedKey);
//                     if(temp == null)
//                         temp = new HashSet<String>();
//                     temp.add(jsff);
                     continue;
                  }
                  else {
                      vosAlreadyProcessed.add(voProcessedKey);
                      HashSet<String> temp = new HashSet<String>();
                      temp.add(jsff);
                      //vosProcessedJsffs.put(voProcessedKey, temp);
                  }
                  
                  processVO(voXml, bcPath, modelJprLocation, jsff, criteria, id,"query",viewUsageVCs,voProcessedKey);     
                  
              } catch (Exception e){
                  e.printStackTrace();
                  System.out.println("Skipping ...");
              }
            
          } //for search region list
          
          //Process LOV VOs            
        for(Iterator<Node> iter = lovNodes.iterator(); iter.hasNext();) {
            
            Node lovNode = iter.next();                
            try {  
               
                String modelValue = XMLParserHelper.getAttributeValue(lovNode, "model");
                String modelIteratorId = ViewCriteriaHelper.extractIteratorNameFromEL(modelValue);
                Node pageDefIterBinding = null;
                String lovType = "";
                String lovUses = "";
      
                if (ViewCriteriaHelper.isRowLevelBinding(modelValue)) {
                
                    String tableIteratorId = ViewCriteriaHelper.getTableIteratorIdForRow(lovNode);
                    if (ViewCriteriaHelper.isEmpty(tableIteratorId)) {
                        System.out.println("Could not obtain table iterator id while processing row level lov:" 
                                           + modelValue + "(" + pageDef + ")");
                        tableiterNotFound++;
                        continue;
                    }
                    Node pageDefTreeBinding = treeNodes.get(tableIteratorId);
                    
                    String iterName = XMLParserHelper.getAttributeValue(pageDefTreeBinding, "IterBinding");
                    if(iterName != null)
                        pageDefIterBinding = iteratorNodes.get(iterName);
                    if (pageDefIterBinding == null) {
                        System.out.println("Could not obtain iterator binding:" 
                                           + modelValue + "(" + pageDef + ")");
                        iterbindingsNotFound++;
                        continue;
                    }
                    
                } else {
                    Node listBinding = listNodes.get(modelIteratorId);
                    if (listBinding == null) {
                        System.out.println("Could not obtain list binding:" 
                                           + modelIteratorId + "(" + pageDef + ")");
                        listbindingsNotFound++;
                        continue;
                    }
                    
                    lovUses = XMLParserHelper.getAttributeValue(listBinding, "Uses");
                    String nodeName = listBinding.getNodeName();
                   
                    if(nodeName.equals("navigationList"))
                        lovType = "Navigation";
                    else {
                        if(ViewCriteriaHelper.isEmpty(lovUses))
                            lovType = "Dynamic";
                        else
                            lovType = "ModelDriven";
                    }
                    if(lovType.equals("Navigation") || lovType.equals("Dynamic"))
                        pageDefIterBinding = iteratorNodes.get(XMLParserHelper.getAttributeValue(listBinding, "ListIter"));
                    else 
                        pageDefIterBinding = iteratorNodes.get(XMLParserHelper.getAttributeValue(listBinding, "IterBinding")); 
                    
                    if (pageDefIterBinding == null) {
                        System.out.println("Could not obtain iterator binding:" 
                                           + modelValue + "(" + pageDef + ")");
                        continue;
                    }                                 
                }
                
                if(lovType.equals("Navigation") || lovType.equals("Dynamic"))
                    continue;
                
                String voUsage = XMLParserHelper.getAttributeValue(pageDefIterBinding, "Binds");
                
                String dataControl = XMLParserHelper.getAttributeValue(pageDefIterBinding, "DataControl");
                
                String amName = ViewCriteriaHelper.getAMNameFromDataControlName(dataControl);
                String dcPackage = ViewCriteriaHelper.getDataControlPackage(dataControl,cpx);
                
                String amAbsPath = ViewCriteriaHelper.getAmFile(dcPackage + "." + amName, jsff);
                ArrayList<String> viewUsageVCs = new ArrayList<String>();
                String voFullPackage = ViewCriteriaHelper.getViewUsagePackageFromAm(amAbsPath, voUsage,viewUsageVCs);
                
                String modelJprLocation = UiFileHelper.getJprLocationForModelFile(amAbsPath);
                
                parser = new ModifiedComboboxParser(m_family.toLowerCase(), ModifiedComboboxParser.ScanType.ALL);
                parser.setJprAbsFilePath(modelJprLocation);
                
                bcPath = "";
                
                Object[] returnObjects = ViewCriteriaHelper.getViewObject(modelJprLocation, voFullPackage, jsff);
                XMLDocument voXml = (XMLDocument)returnObjects[0];
                bcPath = (String)returnObjects[1];
                
                if (ViewCriteriaHelper.isRowLevelBinding(modelValue)) {
                    NodeList listViewObj = voXml.getElementsByTagName("ViewObject");
                    if (!XMLParserHelper.hasOneNOnlyOneNode(listViewObj)) 
                        throw new Exception("Expecting one and only one view object node in vo file: " + bcPath);
                    
                    Node voNode = listViewObj.item(0);
                    Node attrNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(voNode, "ViewAttribute", "Name", modelIteratorId);
                    if (attrNode == null) {
                        System.out.println(String.format("Could not obtain row level lov attribute in vo: %s. LOV Attribute %s", bcPath, modelIteratorId));
                        continue;
                    }
                    lovUses = XMLParserHelper.getAttributeValue(attrNode, "LOVName");
                    if (ViewCriteriaHelper.isEmpty(lovUses)) {
                         System.out.println(String.format("could not obtain lov name for attribute %s, in VO %s", modelIteratorId, bcPath));
                         continue;
                    }
                } 
                processLovVO(voXml, bcPath, modelJprLocation, lovUses,jsff,modelValue,lovNode.getNodeName());
                
            } catch(Exception e) {
                    e.printStackTrace();
                    System.out.println("Skipping...");
            }
        } //for
            
            //Process LOV VOs used in query panels
        for(Iterator it = lovVosToProcess.iterator(); it.hasNext();) {
            LovVO2 lovvo = (LovVO2)it.next();
            try{
                processLovVO(lovvo.voXml,lovvo.voFileName,lovvo.modelJprLocation,
                         lovvo.lovName,lovvo.jsff,lovvo.model,lovvo.component);
            } catch(Exception e) {
                e.printStackTrace();
                System.out.println("Skipping ...");
            }
            
        }
            
    } catch (Exception e) {
            e.printStackTrace();
            logException(pageDef, e);
            System.out.println("ERROR:  Error while processing file.  review carefully if the script updated it. " +pageDef);
    }
}
    
    public void processLovVO(XMLDocument voXml, String voFileName, String modelJprLocation, String lovUses, String jsff, String modelValue, String component) throws Exception {
        
        NodeList listViewObj = voXml.getElementsByTagName("ViewObject");
        if (!XMLParserHelper.hasOneNOnlyOneNode(listViewObj)) 
            throw new Exception("Expecting one and only one view object node in vo file: " + voFileName);
        
        Node voNode = listViewObj.item(0);
        
        Node listBindingNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(voNode, "ListBinding", "Name", lovUses);
        if (listBindingNode == null) {
            System.out.println("Could not find listbinding with name: " + lovUses + " in file: " + voFileName);
            return;
        }
        String listDataSource = XMLParserHelper.getAttributeValue(listBindingNode, "ListVOName");
        Node displayCriteria = XMLParserHelper.getChildNodeWithName(listBindingNode, "DisplayCriteria");
        String viewCriteriaUsed = "implicitViewCriteria";
        if(displayCriteria != null) {
            viewCriteriaUsed = XMLParserHelper.getAttributeValue(displayCriteria, "Name");
            if(viewCriteriaUsed == null || viewCriteriaUsed.equals("null"))
                viewCriteriaUsed = "implicitViewCriteria";
        }
        
        if (ViewCriteriaHelper.isEmpty(listDataSource)) {
            System.out.println("Could not obtain the List data source for the lov name: " + lovUses + " vo file: " + voFileName);
            return;
        }
        
        if(ViewCriteriaHelper.isEmpty(bcPath))
            bcPath = voFileName;
         ArrayList<String> viewAccessorvcUsages = new ArrayList<String>();
         String listVOName = ViewCriteriaHelper.getListVOName(voXml, listDataSource, bcPath, jsff,viewAccessorvcUsages);
        if (ViewCriteriaHelper.isEmpty(listVOName)) {
            System.out.println("List VO could not be obtained " + listDataSource);
            return;
        }

        
        Object[] returnObjects = ViewCriteriaHelper.getViewObject(modelJprLocation, listVOName, jsff);
        voXml = (XMLDocument)returnObjects[0];
        bcPath = (String)returnObjects[1];
        
        String voProcessedKey = bcPath+viewCriteriaUsed;
        for(int j = 0; j < viewAccessorvcUsages.size(); j++)
            voProcessedKey += viewAccessorvcUsages.get(j);      
        
        voProcessedKey += jsff;
        
        if(vosAlreadyProcessed.contains(voProcessedKey)) {
//           HashSet<String> temp = vosProcessedJsffs.get(voProcessedKey);
//           if(temp == null)
//               temp = new HashSet<String>();
//           temp.add(jsff);
           return;
        }
        else {
            vosAlreadyProcessed.add(voProcessedKey);
            HashSet<String> temp = new HashSet<String>();
            temp.add(jsff);
            //vosProcessedJsffs.put(voProcessedKey, temp);
        }
        
        processVO(voXml, bcPath, modelJprLocation,jsff, viewCriteriaUsed, modelValue, component,viewAccessorvcUsages,voProcessedKey);
        
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
              XMLDocument doc = XMLParserHelper.getXMLDocument(f);
              if(doc == null)
                  return;

          NodeList pageMaps = doc.getElementsByTagName("pageMap");
          NodeList pageDefUsages = doc.getElementsByTagName("pageDefinitionUsages");
          if(pageMaps.getLength() != 1 || pageDefUsages.getLength() != 1)
           return;  
              
          Node pageDefUsage = pageDefUsages.item(0);
          NodeList usages = pageDefUsage.getChildNodes();
              
          for(int i = 0; i < usages.getLength(); i++) 
          {
              Node page = usages.item(i);
              if(page.getNodeType() == Node.TEXT_NODE || page.getNodeType() == Node.COMMENT_NODE)
                  continue;
              if(page.getNodeName() != null && !page.getNodeName().equals("page"))
                  continue;

              String nv = XMLParserHelper.getAttributeValue(page, "path"); //path
              String sUsageId = XMLParserHelper.getAttributeValue(page, "id"); //id
           
              String realPath = getRealPageDefPath(nv);                
              m_usageIdToPageDef.put(sUsageId, realPath);
          }
              
          Node pageMap = pageMaps.item(0);
          NodeList pages = pageMap.getChildNodes();
              
          for(int i = 0; i < pages.getLength(); i++) 
          {
              try{
                  Node page = pages.item(i);
                  if(page.getNodeType() == Node.TEXT_NODE || page.getNodeType() == Node.COMMENT_NODE)
                      continue;
                  if(page.getNodeName() != null && !page.getNodeName().equals("page"))
                      continue;
    
                  String sPage = XMLParserHelper.getAttributeValue(page, "path"); //path
                  String sUsageId =XMLParserHelper.getAttributeValue(page, "usageId");;   //usageId
                  
                  String pagedef = m_usageIdToPageDef.get(sUsageId);
                  
                  if(pagedef != null) {
                      m_pageToPageDef.put(sPage,pagedef);
                      m_PageToCpx.put(sPage,f);
                  }
              } catch(Exception e){
                  e.printStackTrace();
              }
              
          }
          } catch (Exception e) {
              e.printStackTrace();
              logException(f, e);
              System.out.println("ERROR:  Error while processing file.  review carefully if the script updated it. " +f);
          }
      }

    public void processFiles() 
    {
      // first load the cpx so we have a mapping between pages and pageDefs
        for(Iterator i = m_CPXList.iterator() ; i.hasNext();)
          digestCPX((String)i.next());
        
        for(Iterator i = m_pageList.iterator(); i.hasNext();)
            processPage((String)i.next());
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length < 1 || args[0] == null || args[0].trim().equals("")) {
            System.out.println("Crawl directory must be specififed");
            System.exit(1);
        }
	 sCrawlDir = args[0].trim();

        File f = new File(sCrawlDir);
        if(!f.exists()){
            System.out.println("Crawl directory does not exist - " + sCrawlDir);
            System.exit(1);
        }
        
        if(args.length > 1)
           append = new Boolean(args[1].trim());              

        viewRoot = ViewCriteriaHelper.getViewRoot(sCrawlDir);

        ViewCriteriaUIPerformance x = new ViewCriteriaUIPerformance();
        System.out.println("Scanning View Criteria used on the UI: " + sCrawlDir);
        m_family = ViewCriteriaHelper.getFamily(sCrawlDir);
        writer = new BufferedWriter(new FileWriter("VC_scan_UI_Performance.csv",append));   
        writer1 = new BufferedWriter(new FileWriter("VC_scan_UI_Performance_noVC.csv"));
        writer1.write("Family,Module,Product,Filename,Label,ViewName,Issue,Jsff,Model,Component\n");
        
        if(!append)        
        writer.write("Family,Module,Product,Filename,Label,ViewName, ViewAttribute," +
          "Column Name, Table Name, IsView, ResolvedTableCol, Indexed Table, NumRows, NumBlocks, Column Type, Index, Column Position, " +
            "VC Required, Is VC Case-insensitive?, IsQueriable?, RenderMode, VCR:VCI, " +
            "UI File, modelValue, componentName\n");
        
        if(refreshFromDB) {     
           System.out.println("Refreshing column indexes from the database...");
           Pattern pFunctionIndex = Pattern.compile("\\w+\\(\"(\\w+)\".*?\\)");
           function_indexColumns = SqlIndexFinder.findFunctionIndexes(pFunctionIndex);
           default_indexColumns = SqlIndexFinder.findDefaultIndexes();
            SqlIndexFinder.populateTableData();
            SqlIndexFinder.populateViewData();
        }
        else{
          function_indexColumns = SqlIndexFinder.readIndexesFromFile("function");
          default_indexColumns = SqlIndexFinder.readIndexesFromFile("default");
          SqlIndexFinder.readColPositionsFromFile();
          SqlIndexFinder.readTableDataFromFile();
          SqlIndexFinder.readViewDataFromFile();
        }

        try {
            x.generateExemptionList(); 
            x.crawlDirectory(sCrawlDir, false);            
            x.processFiles();          

        } catch (Exception e) {
            e.printStackTrace();
            x.logException(sCrawlDir, e);
        }

      //  x.logUsageReport(sCrawlDir, !bDoADE);
        writer.close();
        writer1.close();
        
        BufferedReader reader = new BufferedReader(new FileReader("VC_scan_UI_Performance_noVC.csv"));
        writer1 = new BufferedWriter(new FileWriter("VC_scan_UI_Performance_noVC_rows.csv",append));
        if(!append)
            writer1.write("Family,Module,Product,Filename,Label,ViewName,Issue," +
                "UI File,Model,Component, ListOfTables, MaxRows, Blocks, TableWithMaxRows,WHEREContainsBinds\n");
        
        String line = reader.readLine();
        while((line = reader.readLine()) != null){
            
            String[] parts = line.split(",");
            String fileName = viewRoot + parts[3].trim();       
            XMLDocument voXml = null;
            if(parts[3].trim().startsWith("oracle"))
                voXml = voXmlsFromOtherJars.get(parts[3].trim());
            //ViewCriteriaHelper.getMaxRows(fileName,writer1,line,voXml, exemptions,currentVCViolations);
            ViewCriteriaHelper.getMaxRows(fileName,writer1,line,voXml, new HashSet<String>(),new HashSet<String>());
        }
        
        writer1.close();
        
       /* reader = new BufferedReader(new FileReader("VC_scan_UI_Performance.csv"));
        writer = new BufferedWriter(new FileWriter("VC_scan_UI_Performance1.csv",append));
        
        if(!append)
            writer.write("Family,Module,Product,Filename,Label,ViewName, ViewAttribute," +
            "Column Name, Table Name, IsView, ResolvedTableCol, Indexed Table, NumRows, NumBlocks, Column Type, Index, Column Position, " +
            "VC Required, Is VC Case-insensitive?, IsQueriable?, RenderMode, VCR:VCI, " +
            "UI File, modelValue, componentName\n");
        
        line = reader.readLine();
        while((line = reader.readLine()) != null){
            
            String[] parts = line.split(",");
            if(parts.length < 26)
                continue;
            String voProcessedKey = parts[25].trim();
            String jsff = parts[22].trim();
            HashSet<String> jsffList = vosProcessedJsffs.get(voProcessedKey);
            String jsffs = "";
            if (jsffList == null)
                jsffs = jsff;
            else {
                for(Iterator<String> it = jsffList.iterator(); it.hasNext();)
                    jsffs += it.next() + " ";
            }
            jsffs = ViewCriteriaHelper.getFileNames(jsffs);
            line = line.replace(jsff,jsffs);
            //line = line.replace(voProcessedKey,"");
            writer.write(line + "\n");
        }
        writer.close();*/
    }
    
    
    public void processVO(XMLDocument doc, String f, String modelJprLocation,String jsff, String criteriaName, 
                          String model, String component, ArrayList<String> viewUsageVCs, String voProcessedKey) throws Exception {    
      
//        if(f.contains("TransactionTypeManageUIVO.xml"))
//            System.out.println("break here");
        if(doc == null) {
          System.out.println("Could not parse XMLDocument..Skipping");
          return;
        }
        
        if(criteriaName.equals("__ImplicitViewCriteria__") || criteriaName.equals("implicitViewCriteria")) //these are optional (can happen for query panels)
            return;
        
        NodeList viewObjects = doc.getElementsByTagName("ViewObject");
        if(viewObjects == null || viewObjects.getLength() != 1) 
        {
          System.out.println("WARNING:  Unexpected number of view objects found");
          System.out.println("Skipping...");
          return;
        }
        NodeList viewCriteria = doc.getElementsByTagName("ViewCriteria");
        Node viewObject = viewObjects.item(0);
        
        if(viewCriteria == null || viewCriteria.getLength() == 0)  
           if(!exemptions.contains("NoViewCriteria,"+ViewCriteriaHelper.getFileName(f,jsff) + "," + model + "," + ViewCriteriaHelper.getFileName(jsff)))
              writer1.write(ViewCriteriaHelper.getFileNameInfo(f,jsff) + ViewCriteriaHelper.getViewName(f) +
                   ",VO has no ViewCriteria," + ViewCriteriaHelper.getFileName(jsff) + "," + model + "," + component + "\n");
        
        vaAliasAttributeMap = new HashMap<String,Node>();
        vaNameAttributeMap  = new HashMap<String,Node>();
        
        extendedVaAliasAttributeMap = new HashMap<String,Node>();
        extendedVaNameAttributeMap = new HashMap<String,Node>();
        
        colNameTableNames = new HashMap<String,HashSet>();
        colAliasName = new HashMap<String,String>();
        
        extendedColNameTableNames = new HashMap<String,HashSet>();
        extendedColAliasName = new HashMap<String,String>();
           
        
        if(viewCriteria != null) {             
            createVOAttributeMappings(doc);
            
            SQLParserHelper.parseSql(viewObject, colNameTableNames, colAliasName);    
            
            String baseVo = XMLParserHelper.getAttributeValue(viewObjects.item(0), "Extends");      
            if(baseVo != null) {
              
              XMLDocument baseVoDoc = parser.getVOXml(baseVo);      
              if(baseVoDoc != null) {
                  NodeList baseViewObjects = baseVoDoc.getElementsByTagName("ViewObject");
                  if(baseViewObjects == null || baseViewObjects.getLength() != 1)
                      return;
                  baseViewObject = baseViewObjects.item(0);
                  
                  SQLParserHelper.parseSql(baseViewObject, extendedColNameTableNames, extendedColAliasName);   
              }
           }
            
            boolean vcPassed = false;
            String passed = "";
            reqdVCFound = false;
            
            //If MOAC_enabled Attribute of a VO maps to a leading col index, pass the VO
            
            Node voProperties = XMLParserHelper.getChildNodeWithName(viewObject, "Properties");
            if(voProperties != null) {
                Node schemaProperties = XMLParserHelper.getChildNodeWithName(voProperties, "SchemaBasedProperties");
                if (schemaProperties != null) {
                    Node moacEnabled = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(schemaProperties, "fnd:MOAC_ENABLED", "Value", "true");
                    if(moacEnabled != null) {
                        Node moacEnabledAttibute = XMLParserHelper.getChildNodeWithName(schemaProperties, "fnd:MOAC_ATTRIBUTE");
                        if(moacEnabledAttibute != null) {
                            String moacAttribute = XMLParserHelper.getAttributeValue(moacEnabledAttibute,"Value");
                            if(moacAttribute != null) {
                                Node va = vaNameAttributeMap.get(moacAttribute);
                                if(va != null) {
                                    boolean isLeadCol = isLeadColInIndex(va,viewObject);
                                    if(isLeadCol)
                                        return;
                                }
                            }
                        }
                    }
                }
                                                  
            }
        
            for(int i = 0; i < viewUsageVCs.size(); i++){
                passed = processViewCriteria(doc, viewCriteria, f, modelJprLocation,
                                             jsff, viewUsageVCs.get(i), model,component,false, voProcessedKey);
                if(passed.equals("true")){
                    vcPassed = true;
                    break;
                }
            }
            if(vcPassed)
                return; //skip the check - at least one of the ANDed VCs passed.
            
            processViewCriteria(doc, viewCriteria, f, modelJprLocation, jsff, criteriaName, model,component,true,voProcessedKey);
 
            if(!reqdVCFound) {
                //System.out.println(ViewCriteriaHelper.getFileName(f,jsff) + "," + model + "," + ViewCriteriaHelper.getFileName(jsff));
                if(!exemptions.contains("NoViewCriteria,"+ViewCriteriaHelper.getFileName(f,jsff) + "," + model + "," + ViewCriteriaHelper.getFileName(jsff))) 
                  writer1.write(ViewCriteriaHelper.getFileNameInfo(f,jsff) + ViewCriteriaHelper.getViewName(f) +
                    ",VO has no Required/Selectively Required ViewCriteria," + ViewCriteriaHelper.getFileName(jsff) 
                          + "," + model + "," + component + "\n");
                if(f.startsWith("oracle/"))
                    voXmlsFromOtherJars.put(f, doc);
            }
        }      
        
    }
    
    //jsff, criteriaName, searchRegionId
    public String processViewCriteria(XMLDocument voXml, NodeList viewCriteria, String fileName, String modelJprLocation,
                   String jsff, String criteriaName, String model, String component, boolean toWrite, String voProcessedKey) throws Exception {
      
      Node viewObject = voXml.getElementsByTagName("ViewObject").item(0);
      String passed = "";

      for(int i = 0; i< viewCriteria.getLength(); i++){
          
        Node vc = viewCriteria.item(i);
          
        String viewCriteriaName = XMLParserHelper.getAttributeValue(vc, "Name");
        if(!viewCriteriaName.equals(criteriaName) && !ViewCriteriaHelper.parentVCMatches(vc,criteriaName))
          continue;
        NodeList vcRows = vc.getChildNodes();
      
        HashMap<String,String> VCRConjuntion = new HashMap<String,String>(); //VCR Name -> Conjuntion
        HashMap<String,Boolean> VCRLogic1 = new HashMap<String,Boolean>(); //VCR Name -> Logic 1 Satisfied(true/false)
        HashMap<String,String> vcRowItemResult = new HashMap<String,String>(); //vcr:vci -> result for display
          
        for(int k = 0; k < vcRows.getLength(); k++ ){
            
          Node vc_row = vcRows.item(k);
          if(ViewCriteriaHelper.isEmpty(vc_row.getNodeName()) || !vc_row.getNodeName().equals("ViewCriteriaRow"))
              continue;
        
          String isUpper_row = XMLParserHelper.getAttributeValue(vc_row, "UpperColumns");
          String vcRowName = XMLParserHelper.getAttributeValue(vc_row, "Name");
          String vcRequired = "";
          String isUpper_item = "";        
          
          NodeList vc_row_children = vc_row.getChildNodes();
            
           HashMap<String,String> VCIRequired = new HashMap<String,String>(); //VCI Name -> required/optional/selective
           HashMap<String,String> VCIColPosition = new HashMap<String,String>(); //VCI Name -> col index position (null if there is no index)
          
          for(int j =0; j < vc_row_children.getLength(); j++) {
            
            Node vcItem = vc_row_children.item(j);
            String nodeName = vcItem.getNodeName();
            
            if(ViewCriteriaHelper.isEmpty(nodeName) || !nodeName.equals("ViewCriteriaItem")) continue;
              
            vcRequired = "";
            isUpper_item = "";
            
            boolean useBaseViewObject = false;
            
            vcRequired = XMLParserHelper.getAttributeValue(vcItem, "Required");
              
            if(vcRequired == null || vcRequired.equals("Optional"))
                continue;
              
            reqdVCFound = true;
            
            String vcViewObjectName = XMLParserHelper.getAttributeValue(vc, "ViewObjectName");
              
            boolean useNewViewObject = false;
            Node vcViewObject = null;
              
            HashMap<String,HashSet> newColNameTableNames = new HashMap<String,HashSet>();
            HashMap<String,String> newColAliasName = new HashMap<String,String>(); 
              
            if(vcViewObjectName != null && !vcViewObjectName.contains(XMLParserHelper.getAttributeValue(viewObject, "Name"))) {
                if(baseViewObject != null && vcViewObjectName.contains(XMLParserHelper.getAttributeValue(baseViewObject, "Name"))){
                   //its ok
                } else {
//                 
                    XMLDocument vcVoDoc = parser.getVOXml(vcViewObjectName); 
                    if(vcVoDoc == null)
                        continue;
                    if(vcVoDoc.getElementsByTagName("ViewObject").getLength() != 1)
                        continue;
                    vcViewObject = vcVoDoc.getElementsByTagName("ViewObject").item(0);
                    if(vcViewObject == null)
                        continue;
                    useNewViewObject = true;
                    
                    SQLParserHelper.parseSql(vcViewObject, newColNameTableNames, newColAliasName);   
                }
            }
            
            isUpper_item = XMLParserHelper.getAttributeValue(vcItem, "UpperColumns");
            
            String viewAttrName = XMLParserHelper.getAttributeValue(vcItem, "ViewAttribute");
            String vc_name = XMLParserHelper.getAttributeValue(vcItem, "Name");
         
            Node va = vaNameAttributeMap.get(viewAttrName);
              
              if(useNewViewObject){
                  va = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(vcViewObject, "ViewAttribute", "Name", viewAttrName);
                  
              } else if(va == null) {
                  va = extendedVaNameAttributeMap.get(viewAttrName);
                  if(va != null)
                      useBaseViewObject = true;
            }
            
            if(va == null) {     
                String isNestedCriteria = XMLParserHelper.getAttributeValue(vcItem, "IsNestedCriteria");
                if(isNestedCriteria != null && isNestedCriteria.equals("true"))
                    nestedVCs++;
                else
                    nonNestedVCs++;
              System.out.println(fileName + "," + viewAttrName + ", Could not find the ViewAttribute for ViewCriteriaItem");
              continue;
            }
            
            String col_name = "";
            String table_name = "";
            String colType = "";
            
            boolean entityFound = false;
            
            if(XMLParserHelper.getAttributeValue(va, "EntityUsage") != null) { //if it is an entity based attribute
            
              Node ea = null;
              if(useBaseViewObject)
                ea = parser.getEntityAttributeNode(baseViewObject, va, viewAttrName);
              else if(useNewViewObject)
                  ea = parser.getEntityAttributeNode(vcViewObject, va, viewAttrName);
              else
                ea = parser.getEntityAttributeNode(viewObject, va, viewAttrName);

              if (ea != null) {
                  
                entityFound = true;
                col_name = XMLParserHelper.getAttributeValue(ea, "ColumnName");
                table_name = XMLParserHelper.getAttributeValue(ea, "TableName");
                colType = XMLParserHelper.getAttributeValue(ea, "ColumnType");
              }          
            } 
            if(!entityFound) { //it is not an entity based attribute or the corresponding entity object could not be determined
                
               String aliasName = XMLParserHelper.getAttributeValue(va, "AliasName");
               colType = XMLParserHelper.getAttributeValue(va, "SQLType");
               
               if(useBaseViewObject)
                  col_name = extendedColAliasName.get(aliasName);
               else if(useNewViewObject)
                   col_name = newColAliasName.get(aliasName);
               else
                  col_name = colAliasName.get(aliasName);
               
               if(col_name == null) {
               //  errors.write("Could not get column name for attribute alias:" + aliasName + "\n");
                 table_name = "Could not determine";
               }
               else{
                  HashSet possibleTables = null;
                  if(useBaseViewObject)
                     possibleTables = extendedColNameTableNames.get(aliasName);
                  else if(useNewViewObject)
                     possibleTables = newColNameTableNames.get(aliasName);
                  else
                     possibleTables = colNameTableNames.get(aliasName); 
                   
                  for(Iterator it = possibleTables.iterator(); it.hasNext();){
                    String tableName = (String)it.next();
                      table_name+=tableName + " ";
                  }
               }
            }
      
            if(table_name != null)
               table_name = table_name.trim();
            if(col_name != null)
               col_name= col_name.trim();          
            
            String index = getIndex(table_name,col_name); 
              if(index.equals("Could Not Determine"))
                  continue;
              
              String tableName = table_name;
              String colName = col_name;
              
            if(!ViewCriteriaHelper.isEmpty(indexedTableName))
                tableName = indexedTableName;   
              
              String resolvedTableName = " ";
              if(!ViewCriteriaHelper.isEmpty(resolvedTableColumn)){
                  Matcher m = pTablesColumn.matcher(resolvedTableColumn);
                  if(m.find())  {
                      colName = m.group(2).trim();     
                      resolvedTableName = m.group(1).trim();
                  }
              }           
             if(!resolvedTableName.contains(" ") && resolvedTableName.endsWith("_TL")){
                  String language_col_position1 = SqlIndexFinder.defaultIndexPositions.get(resolvedTableName+",LANGUAGE");
                  String language_col_position2 = SqlIndexFinder.functionIndexPositions.get(resolvedTableName+",LANGUAGE");
                  String language_col_position = "";
                  if(language_col_position1 != null)
                      language_col_position += language_col_position1;
                  if(language_col_position2 != null)
                      language_col_position += language_col_position2;
                  if(SqlIndexFinder.LeadColumn(language_col_position))
                      return "true"; //return passed = true
                  
              } 
              
            String col_position = " ";
            if(index.equals("Default")) {
                col_position = SqlIndexFinder.defaultIndexPositions.get(tableName+","+colName);
                
            }else if(index.equals("Function")) {
                col_position = SqlIndexFinder.functionIndexPositions.get(tableName+","+colName);
                
            } else if(index.equals("Both")) {
                col_position += SqlIndexFinder.defaultIndexPositions.get(tableName+","+colName);
                col_position +=";";
                col_position += SqlIndexFinder.functionIndexPositions.get(tableName+","+colName);
            }
            
             if(table_name != null && !table_name.contains("_"))
                 continue;
              
            String isUpper = "true";
            if(isUpper_item != null && isUpper_item.equals("0"))
                isUpper = "false";
            if(isUpper_item == null && isUpper_row != null && isUpper_row.equals("0"))
                isUpper = "false";             
              
            VCIRequired.put(vc_name, vcRequired);
            VCIColPosition.put(vc_name, col_position);
              
              if(!index.equals("None") && SqlIndexFinder.LeadColumn(col_position))
                  continue;              
            
            StringBuffer numRows = new StringBuffer();
            StringBuffer numBlocks = new StringBuffer();
            StringBuffer tableNameforRows = new StringBuffer();
            ViewCriteriaHelper.getNumRowsBlocks(indexedTableName,resolvedTableColumn,table_name,numRows,numBlocks,tableNameforRows, new HashSet<String>());
              
            Node properitesNode = XMLParserHelper.getChildNodeWithName(vcItem, "Properties");
            Node customProperitesNode = XMLParserHelper.getChildNodeWithName(properitesNode, "CustomProperties");
            Node renderedPropertyNode = XMLParserHelper.getChildNodeWithMatchingAttribute(customProperitesNode, "Name", "renderedMode");
            String renderMode = XMLParserHelper.getAttributeValue(renderedPropertyNode, "Value");
              if(renderMode == null)
                  renderMode = " ";
              
            String isQueriable = XMLParserHelper.getAttributeValue(va, "IsQueriable");
              if(isQueriable == null)
                  isQueriable = " ";
            String lovName = XMLParserHelper.getAttributeValue(va, "LOVName");
              
            if(!ViewCriteriaHelper.isEmpty(lovName) && component.equals("query"))
                lovVosToProcess.add(new LovVO2(voXml,fileName, modelJprLocation,lovName,jsff,model,"query panel LOV"));
           
              if(col_name.equals("NULL"))
                  continue;
              
            vcRowItemResult.put(vcRowName + ":" + vc_name, ViewCriteriaHelper.getFileNameInfo(fileName,jsff) + 
                                ViewCriteriaHelper.getViewName(fileName) + "," + viewAttrName + "," +
                  col_name + "," + table_name + "," + isView + "," + resolvedTableColumn + "," + indexedTableName + "," + numRows 
                     + "(" +tableNameforRows + ")" +  "," + numBlocks + "," + colType + "," + index + "," 
                       + col_position + "," + vcRequired + "," + isUpper +"," + isQueriable + "," + renderMode);
            
        } //for vc_items
            boolean logic1Satisfied1 = false; // at least 1 VCI for this VCR is required and maps to first column of an index
            boolean logic1Satisfied2 = true; //every selectively required VCI is lead col in some index
            boolean selectivelyReqdVCFound = false;
            
            for(Iterator<String> it = VCIRequired.keySet().iterator(); it.hasNext();){
                
                String vci = it.next();
                String required = VCIRequired.get(vci);
                String colPosition = VCIColPosition.get(vci);
                
                if(required != null && required.equals("Required") && SqlIndexFinder.LeadColumn(colPosition))
                    logic1Satisfied1 = true;
                
                if(required != null && required.equals("SelectivelyRequired")){
                  logic1Satisfied2 = logic1Satisfied2 && SqlIndexFinder.LeadColumn(colPosition); 
                    selectivelyReqdVCFound = true;
                }
            }
           if(!selectivelyReqdVCFound)
               logic1Satisfied2 = false;

          VCRConjuntion.put(vcRowName,XMLParserHelper.getAttributeValue(vc_row, "Conjunction"));
          VCRLogic1.put(vcRowName,logic1Satisfied1 || logic1Satisfied2);
            
            if(logic1Satisfied1 || logic1Satisfied2) {
                passed = "true";
                if(!toWrite)
                    return passed;
            }
       } //for vc_rows
          
       boolean logic2Satisfied = false; //IS VCR conjunction = AND and Logic1 is satisfied
          
       for(Iterator<String> it = VCRLogic1.keySet().iterator(); it.hasNext();) {
           String vcrName = it.next();
           boolean logic1Satisfied = VCRLogic1.get(vcrName);
           String conjunction = VCRConjuntion.get(vcrName);
           
           if(conjunction != null && conjunction.equals("AND") && logic1Satisfied)
               logic2Satisfied = true;
       }
          
        for(Iterator<String> it = vcRowItemResult.keySet().iterator(); it.hasNext();) {
          
          String vcri = it.next();
          String[] parts = vcri.split(":");
          
          String vcrName = parts[0].trim();
          boolean logic1Satisfied = VCRLogic1.get(vcrName);
            
          boolean isViolation = !logic1Satisfied && !logic2Satisfied;       
            
//          if(!toWrite && isViolation)
//            return "false"; //return false -> VC failed

            if(toWrite && isViolation) {
                
              //Apply filters
              String result = vcRowItemResult.get(vcri);
              String[] resultParts = result.split(",");
              if(resultParts.length < 21)
                  continue;
                String tableName = resultParts[8].trim();
                String resolvedTableCol = resultParts[10].trim();
                  
                if(tableName.contains("LOOKUP") || resolvedTableCol.contains("LOOKUP"))
                    continue;
                  
                String num_blocks = resultParts[13].trim();
                if(ViewCriteriaHelper.isEmpty(num_blocks))
                    continue;
                
                int blocks = 0;
                  try{
                    blocks = Integer.parseInt(num_blocks);
                  }catch(Exception e){
                      e.printStackTrace();
                  }
                  
                  if(blocks < 200)
                      continue;
                  
                  if(resultParts[19].trim().equalsIgnoreCase("false") || resultParts[20].trim().equals("Never"))
                      continue;
                  
                  if(exemptions.contains("BadViewCriteriaItems,"+resultParts[3].trim() + "," + model + "," + ViewCriteriaHelper.getFileName(jsff)))
                      continue;
                  
                  writer.write(result + "," + vcri + "," + ViewCriteriaHelper.getFileName(jsff) + "," + 
                               model + "," + component + "," + voProcessedKey + "\n");     
            }
          
        }      
      }//for vc
      return passed;
    }
    
    /*public void processViewAttributes(Node viewObject, String fileName, String jsff, String model, String component) throws Exception {
      
      NodeList viewAttributes = viewObject.getChildNodes();
          
        for(int j =0; j < viewAttributes.getLength(); j++) {
            
            Node va = viewAttributes.item(j);
            String nodeName = va.getNodeName();
            
            if(!nodeName.equals("ViewAttribute")) continue;       
            
            String col_name = "";
            String table_name = "";
            String colType = "";
            
            boolean entityFound = false;
            
            if(XMLParserHelper.getAttributeValue(va, "EntityUsage") != null) { //if it is an entity based attribute
            
              Node ea = null;
              ea = parser.getEntityAttributeNode(viewObject, va, XMLParserHelper.getAttributeValue(va, "Name"));

              if (ea != null) {
                  
                entityFound = true;
                col_name = XMLParserHelper.getAttributeValue(ea, "ColumnName");
                table_name = XMLParserHelper.getAttributeValue(ea, "TableName");
                colType = XMLParserHelper.getAttributeValue(ea, "ColumnType");
              }          
            } 
            if(!entityFound) { //it is not an entity based attribute or the corresponding entity object could not be determined
                
               String aliasName = XMLParserHelper.getAttributeValue(va, "AliasName");
               colType = XMLParserHelper.getAttributeValue(va, "SQLType");
               col_name = colAliasName.get(aliasName);
               
               if(col_name == null) {
               //  errors.write("Could not get column name for attribute alias:" + aliasName + "\n");
                 table_name = "Could not determine";
               }
               else{
                  HashSet possibleTables = null;
                  possibleTables = colNameTableNames.get(aliasName);
                   
                  for(Iterator it = possibleTables.iterator(); it.hasNext();){
                    String tableName = (String)it.next();
                      table_name+=tableName + " ";
                  }
               }
            }
            
            String index = getIndex(table_name,col_name);
            String tableName = table_name;
            if(!indexedTableName.equals(""))
              tableName = indexedTableName;   
            
            String col_position = " ";
            if(index.equals("Default")) {
                col_position = SqlIndexFinder.defaultIndexPositions.get(tableName+","+col_name);
                
            }else if(index.equals("Function")) {
                col_position = SqlIndexFinder.functionIndexPositions.get(tableName+","+col_name);
                
            } else if(index.equals("Both")) {
                col_position += SqlIndexFinder.defaultIndexPositions.get(tableName+","+col_name);
                col_position +=";";
                col_position += SqlIndexFinder.functionIndexPositions.get(tableName+","+col_name);
            }
            
            StringBuffer numRows = new StringBuffer();
            StringBuffer numBlocks = new StringBuffer();
            StringBuffer tableNameforRows = new StringBuffer();
            ViewCriteriaHelper.getNumRowsBlocks(table_name,numRows,numBlocks,tableNameforRows);
              
           if(!index.equals("None") && SqlIndexFinder.LeadColumn(col_position))
                continue;
           
            if(table_name != null && !table_name.contains("_"))
                continue; 
    
          writer.write(ViewCriteriaHelper.getFileNameInfo(fileName,jsff) + ViewCriteriaHelper.getViewName(fileName) + "," + 
                       XMLParserHelper.getAttributeValue(va, "Name") + "," + col_name + "," + table_name + "," +
                        isView + "," + resolvedTableColumn + "," + indexedTableName + "," + numRows  + "(" +tableNameforRows + ")" +  "," 
                       + numBlocks + "," + colType + "," + index 
                       + "," + col_position + "," + "lov attribute" + "," + false + "," 
                       + XMLParserHelper.getAttributeValue(va, "IsQueriable")
                       + ",null,lov attribute,null,null,true," + ViewCriteriaHelper.getFileName(jsff) + "," + model + 
                       ","  + component + "\n"); 
            
        } //for vc_row_children
    } */
    
    private void createVOAttributeMappings(XMLDocument doc) throws Exception{
      
      NodeList view_attributes = doc.getElementsByTagName("ViewAttribute");
      
      for(int i = 0; i< view_attributes.getLength(); i++) {
          
        Node va = view_attributes.item(i);
        String alias = XMLParserHelper.getAttributeValue(va, "AliasName");    
        if(alias == null) {
         //errors.write(fileName + "->" +  XMLParserHelper.getAttributeValue(va, "Name") + " -> No Alias\n");
         continue;
        }
        String entityUsage = XMLParserHelper.getAttributeValue(va,"EntityUsage");
          
        if(entityUsage != null)
            alias += "_" + entityUsage;
        vaAliasAttributeMap.put(alias.trim().toUpperCase(), va);        
          
        String name = XMLParserHelper.getAttributeValue(va, "Name");
        vaNameAttributeMap.put(name,va);
      } //for
      
      NodeList viewObjects = doc.getElementsByTagName("ViewObject");
      if(viewObjects == null || viewObjects.getLength() != 1)
          return;
      String baseVo = XMLParserHelper.getAttributeValue(viewObjects.item(0), "Extends");
      
      if(baseVo != null) {
          
        XMLDocument baseVoDoc = parser.getVOXml(baseVo);      
        if(baseVoDoc == null) return;
        NodeList baseViewObjects = baseVoDoc.getElementsByTagName("ViewObject");
        if(baseViewObjects == null || baseViewObjects.getLength() != 1)
            return;
        baseViewObject = baseViewObjects.item(0);
        
        view_attributes = baseVoDoc.getElementsByTagName("ViewAttribute");
        
        for(int i = 0; i< view_attributes.getLength(); i++){
          Node va = view_attributes.item(i);
          String alias = XMLParserHelper.getAttributeValue(va, "AliasName");    
          if(alias == null) {
           //errors.write(fileName + "->" +  XMLParserHelper.getAttributeValue(va, "Name") + " -> No Alias\n");
           continue;
          }
          String entityUsage = XMLParserHelper.getAttributeValue(va,"EntityUsage");
            
          if(entityUsage != null)
              alias += "_" + entityUsage;
          extendedVaAliasAttributeMap.put(alias.trim().toUpperCase(), va);        
            
          String name = XMLParserHelper.getAttributeValue(va, "Name");
          extendedVaNameAttributeMap.put(name,va);
        }      //for
      } //if baseVo != null
      
    }
    
    private static String getIndex(String tableName, String colName) throws Exception {
        
        isView = false;
        resolvedTableColumn = " ";
        indexedTableName = " ";
        String[] tableNames = null;
        
        tableName = convertViewNamesIntoTableNames(tableName,colName);
        
        String index = "None";
        if(tableName == null) 
            return index;
          
        if(isView) {
            
            Matcher m = pTablesColumn.matcher(resolvedTableColumn);
            if(m.find()){
                tableName = m.group(1);     
                colName = m.group(2);
            }
            else
                return "Could Not Determine";
        }
        
        
        tableNames = tableName.trim().split(" ");
       
       for(int i = 0; i < tableNames.length; i++) {
       
           String tablNameToUse = tableNames[i].trim();
          if(SqlIndexFinder.synonyms.containsKey(tablNameToUse))
              tablNameToUse = SqlIndexFinder.synonyms.get(tablNameToUse);
           
           HashSet caseInsensitiveIndexes = function_indexColumns.get(tablNameToUse);
           if(caseInsensitiveIndexes != null) {
               for(Iterator it = caseInsensitiveIndexes.iterator(); it.hasNext();) {
                 String indexColumn = (String)it.next();
                   if(indexColumn.equals(colName)) {
                       index = "Function";
                       indexedTableName = tablNameToUse;
                       break;
                   }
               }
           }
           
          HashSet caseSensitiveIndexes = default_indexColumns.get(tablNameToUse);
          if(caseSensitiveIndexes != null) {
              for(Iterator it = caseSensitiveIndexes.iterator(); it.hasNext();) {
                String indexColumn = (String)it.next();
                  if(indexColumn.equals(colName)) {
                      if(index.equals("None"))
                       index = "Default";
                      else
                       index = "Both";
                      
                      indexedTableName = tablNameToUse;
                      break;
                  }
              }
          }
        
         if(!index.equals("None"))
               return index;
      } //for
       return index;
    }
    
    public static String convertViewNamesIntoTableNames(String table_name, String aliasName) throws Exception{
        
      
       if(table_name == null || table_name.trim().equals(""))
           return "";
        String[] parts = table_name.split(" ");
        
        for(int i = 0; i < parts.length; i++) {
            
            String table = parts[i].trim();
            
            if(SqlIndexFinder.synonyms.containsKey(table))
                table = SqlIndexFinder.synonyms.get(table);
            
            if(SqlIndexFinder.viewNameText.containsKey(table)) {
                isView = true;
                
                if(ViewCriteriaHelper.isEmpty(resolvedTableColumn)) 
                      resolvedTableColumn = SqlIndexFinder.getResolvedTablesColumn(table,aliasName);      
                
               Matcher m = pTablesColumn.matcher(resolvedTableColumn);
               if(m.find()){
                   
                  table = m.group(1).trim();
                  if(SqlIndexFinder.viewNameText.containsKey(table)) {
                      resolvedTableColumn = SqlIndexFinder.getResolvedTablesColumn(table,m.group(2));
                      Matcher m1 = pTablesColumn.matcher(resolvedTableColumn);
                      if(m1.find()) {
                          table = m1.group(1).trim();
                          if(SqlIndexFinder.viewNameText.containsKey(table)) {
                              resolvedTableColumn = SqlIndexFinder.getResolvedTablesColumn(table,m1.group(2));
                              Matcher m2 = pTablesColumn.matcher(resolvedTableColumn);
                              if(m2.find())
                                  table=m2.group(1).trim();
                          }
                      }
                  }
                  
               }                
            }       
            table_name = table_name.replace(parts[i], table);
        } //for
        return table_name;        
    }
    
 public void generateExemptionList() {
        
    try{
      BufferedReader fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
          "/fatools/opensource/jauditFixScripts/txt/rup2Exemptions.txt"));
      String line = null;
      
      while((line = fileReader.readLine()) != null) {
        String[] parts = line.split(",");
        if(parts.length < 4)
            continue;
        String issue = parts[0].trim();
        if(!issue.equals("NoViewCriteria") && !issue.equals("BadViewCriteriaItems"))
            continue;
        exemptions.add(issue + "," + parts[1].trim() + "," + parts[2].trim() + "," + parts[3].trim());
      }
       fileReader.close();       
    }catch(Exception e) {
        System.out.println("Skipping exemptions as file could not be found: " + e.getMessage());
    }
    
 }
 
 public boolean isLeadColInIndex(Node va, Node viewObject) {
     
     String col_name = "";
     String table_name = "";
     String colType = "";
     
     boolean entityFound = false;
     
     if(XMLParserHelper.getAttributeValue(va, "EntityUsage") != null) { //if it is an entity based attribute
     
       Node ea = parser.getEntityAttributeNode(viewObject, va, XMLParserHelper.getAttributeValue(va, "Name"));

       if (ea != null) {
           
         entityFound = true;
         col_name = XMLParserHelper.getAttributeValue(ea, "ColumnName");
         table_name = XMLParserHelper.getAttributeValue(ea, "TableName");
         colType = XMLParserHelper.getAttributeValue(ea, "ColumnType");
       }          
     } 
     if(!entityFound) { //it is not an entity based attribute or the corresponding entity object could not be determined
         
        String aliasName = XMLParserHelper.getAttributeValue(va, "AliasName");
        colType = XMLParserHelper.getAttributeValue(va, "SQLType");        
        col_name = colAliasName.get(aliasName);
        
        if(col_name == null) 
          table_name = "Could not determine";
        else{
           HashSet possibleTables = colNameTableNames.get(aliasName); 
            
           for(Iterator it = possibleTables.iterator(); it.hasNext();){
             String tableName = (String)it.next();
               table_name+=tableName + " ";
           }
        }
     }
     
     if(table_name != null)
        table_name = table_name.trim();
     if(col_name != null)
        col_name= col_name.trim();          
    
     String index = "";
     try{
       index = getIndex(table_name,col_name); 
       if(index.equals("Could Not Determine"))
           return false;
     } catch (Exception e){
         return false;
     }
       
       String tableName = table_name;
       String colName = col_name;
       
     if(!ViewCriteriaHelper.isEmpty(indexedTableName))
         tableName = indexedTableName;   
       
       if(!ViewCriteriaHelper.isEmpty(resolvedTableColumn)){
           Matcher m = pTablesColumn.matcher(resolvedTableColumn);
           if(m.find())  
               colName = m.group(2);                 
       }              
       
     String col_position = " ";
     if(index.equals("Default")) {
         col_position = SqlIndexFinder.defaultIndexPositions.get(tableName+","+colName);
         
     }else if(index.equals("Function")) {
         col_position = SqlIndexFinder.functionIndexPositions.get(tableName+","+colName);
         
     } else if(index.equals("Both")) {
         col_position += SqlIndexFinder.defaultIndexPositions.get(tableName+","+colName);
         col_position +=";";
         col_position += SqlIndexFinder.functionIndexPositions.get(tableName+","+colName);
     }
     
      if(table_name != null && !table_name.contains("_"))
          return false;
       
       if(!index.equals("None") && SqlIndexFinder.LeadColumn(col_position))
           return true;        
       
       return false;
 }

}

class LovVO2 {
    XMLDocument voXml;
    String lovName;
    String jsff;
    String model;
    String component;
    String voFileName;
    String modelJprLocation;
    
    public LovVO2(XMLDocument voXml, String voFileName, String modelJprLocation,
                 String lovName, String jsff, String model, String component) {
        this.voXml = voXml;
        this.voFileName = voFileName;
        this.lovName = lovName;
        this.jsff = jsff;
        this.model = model;
        this.component = component;
        this.modelJprLocation = modelJprLocation;
    }
}
