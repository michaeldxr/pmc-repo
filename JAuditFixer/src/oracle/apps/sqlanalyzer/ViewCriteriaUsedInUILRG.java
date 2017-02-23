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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;
import oracle.apps.lovWidth.ModifiedComboboxParser;
import oracle.apps.utility.JoesBaseClass;

import oracle.apps.utility.LRGUtil;
import oracle.apps.votuning.UiFileHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class ViewCriteriaUsedInUILRG extends JoesBaseClass {
    
    private int m_page_reviewCount = 0;
    private int m_pageDef_reviewCount = 0;
    private String m_sSummaryReport = "";
    private boolean bDebug = false;
    private String bcPath = "";
    
    private HashSet<String> m_pageList = new HashSet<String>();
    private HashMap<String,String> m_pageDefList = new HashMap<String,String>();
    private HashSet<String> m_CPXList = new HashSet<String>();
    
    private HashMap<String, String> m_usageIdToPageDef = new HashMap<String, String>();
    private HashMap<String,String> m_pageToPageDef = new HashMap<String,String>();
    private HashMap<String,String> m_PageToCpx = new HashMap<String,String>();
   
    //Store the viwattribute mappings for the current VO being processed
    private HashMap<String,Node> vaAliasAttributeMap = new HashMap<String,Node>();
    private HashMap<String,Node> vaNameAttributeMap = new HashMap<String,Node>();
    private HashMap<String,Node> extendedVaAliasAttributeMap = new HashMap<String,Node>();
    private HashMap<String,Node> extendedVaNameAttributeMap = new HashMap<String,Node>();
    
    //Store the mappings for userdefined SQLQuery
    private HashMap<String,HashSet> colNameTableNames = new HashMap<String,HashSet>();
    private HashMap<String,String> colAliasName = new HashMap<String,String>();
    
    private HashMap<String,HashSet> extendedColNameTableNames = new HashMap<String,HashSet>();
    private HashMap<String,String> extendedColAliasName = new HashMap<String,String>();
    
    private ModifiedComboboxParser parser;
    private Node baseViewObject;
    
    private static BufferedWriter writer;
    private static int nestedVCs = 0;
    private static int nonNestedVCs = 0;
    private static String viewRoot = "";
    private static HashSet<String> exemptions = new HashSet<String>();    
    private static HashSet<String> exemptedTables = new HashSet<String>();
    
    private static Pattern pModelUsage = Pattern.compile("bindings\\.(\\w+)\\.");
    private static Pattern pTablesColumn = Pattern.compile("(.*?)\\((.*?)\\)");
    private static HashMap<String,HashSet> function_indexColumns; //function
    private static HashMap<String,HashSet> default_indexColumns;  //Default
    private static String m_family;
    
    private static boolean isView = false;
    private static String indexedTableName = " ";
    private static String resolvedTableColumn = " ";
    private static HashSet<String> vosAlreadyProcessed = new HashSet<String>();
    private static HashSet<LovVO> lovVosToProcess = new HashSet<LovVO>();
    
    private static int listbindingsNotFound = 0;
    private static int tableiterNotFound = 0;
    private static int iterbindingsNotFound = 0;
    
    private final static String issuetype="UISearches1";
    private static int releaseNum=0;
    
    public ViewCriteriaUsedInUILRG() 
    {
        super(JoesBaseClass.CRAWL_TYPE.JSPX_AND_JSFF_AND_PAGE_DEF_AND_CPX);
    }

    protected String getSummaryReportSubject() 
    {
        return "SCRIPT: Find ViewCriteria case for VC used in UI searches";
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
          NodeList queryList2 = doc.getElementsByTagName("quickQuery"); //quickQuery is always on implicit VC (optional) and results in case-insensitive seacrh by default
          HashMap<String,Node> usedModels = new  HashMap<String,Node>();           

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
                usedModels.put(m.group(1),iter);  
              else
                System.out.println("Weird af:query model");                
          }
            
            for (int i = 0; i < queryList2.getLength(); i++) 
            {
              Node iter = queryList2.item(i);
                
                String rendered = XMLParserHelper.getAttributeValue(iter, "rendered");
                if(!ViewCriteriaHelper.isEmpty(rendered) && rendered.equals("false"))
                    continue;
              
                String value = XMLParserHelper.getAttributeValue(iter,"model");
                
              Matcher m = pModelUsage.matcher(value);
                if(m.find())
                  usedModels.put(m.group(1),iter); 
                else
                  System.out.println("Weird af:quickquery model");     
               
            }
            
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
                reviewPageDef(pageDef,usedModels,f,cpx,lovNodes);
            
        } catch (Exception e) {
            e.printStackTrace();
            logException(f, e);
            System.out.println("ERROR:  Error while processing file.  review carefully if the script updated it. " +f);
            return;
        }
    }

    protected void reviewPageDef(String pageDef, HashMap<String,Node> usedModels, String jsff, String cpx, HashSet<Node> lovNodes) {
     
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
                boolean isUsed = usedModels.keySet().contains(id);                  
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
                  
                  Node component = usedModels.get(id);
                  
                  processVO(voXml, bcPath, modelJprLocation, jsff, criteria, id,component.getNodeName());     
                  
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
            LovVO lovvo = (LovVO)it.next();
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
        String viewCriteriaUsed = "__ImplicitViewCriteria__";
        if(displayCriteria != null) {
            viewCriteriaUsed = XMLParserHelper.getAttributeValue(displayCriteria, "Name");
            if(viewCriteriaUsed == null || viewCriteriaUsed.equals("null"))
                viewCriteriaUsed = "__ImplicitViewCriteria__";
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
       
        voProcessedKey += jsff;
        
        if(vosAlreadyProcessed.contains(voProcessedKey)) 
           return;
        else 
            vosAlreadyProcessed.add(voProcessedKey);
        
        processVO(voXml, bcPath, modelJprLocation,jsff, viewCriteriaUsed, modelValue, component);        
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

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || args[0] == null || args[0].equals("")) {
            System.out.println("Series has to be specified for " + issuetype);
            System.exit(1);
        }
        String series = args[0];
        FamilyModuleHelper.generateDeferedPaths();
        
        String release= FamilyModuleHelper.getRelease(series);
        try{              
            releaseNum = Integer.parseInt(release);
        }catch(NumberFormatException e){
           e.printStackTrace();
           releaseNum=0;
        }
    
        String label = LRGUtil.getLatestLabel(series);
        System.out.println("Running "+issuetype+" Scan on series: "+series+".......");
        System.out.println("Latest label is: "+label);
        
        writer = new BufferedWriter(new FileWriter("UISearches1_LRG.csv"));   
        writer.write("Family,Module,Product,Filename,Label, Component Type, Model Value," +
          "VO file, ViewCriteria, Description,View Attribute, Table Name, Column Name, " +
            "Resolved Table Column, Column Type, Index, VC_Required,Col Position, Case-Sensitive, Category\n");
        
        generateExemptionList();
        function_indexColumns = SqlIndexFinder.readIndexesFromFile("caseInsensitive");
        default_indexColumns = SqlIndexFinder.readIndexesFromFile("default");
        SqlIndexFinder.readColPositionsFromFile();
        SqlIndexFinder.readTableDataFromFile();
        SqlIndexFinder.readViewDataFromFile();
        
        ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
        ViewCriteriaUsedInUILRG x = new ViewCriteriaUsedInUILRG();
        for(int j=0; j<families.size();j++){
            String family = families.get(j);
            m_family = family;
            String sCrawlDir = LRGUtil.getCrawlDir(series, family, label, issuetype);
            viewRoot = ViewCriteriaHelper.getViewRoot(sCrawlDir);
            System.out.println("Crawling dir '"+sCrawlDir+"' for family: "+family);
            x.crawlDirectory(sCrawlDir, false);            
        }
        x.processFiles();
        writer.close();
        LRGUtil.addLabelInfoToAB(issuetype, series, label);
    }
    
    
    public void processVO(XMLDocument doc, String f, String modelJprLocation,String jsff, String criteriaName, 
                          String model, String component) throws Exception {    
      
        if(doc == null) {
          System.out.println("Could not parse XMLDocument..Skipping");
          return;
        }
        
        if(criteriaName.equals("__ImplicitViewCriteria__")){ 
            return;
        }
        
        NodeList viewObjects = doc.getElementsByTagName("ViewObject");
        if(viewObjects == null || viewObjects.getLength() != 1) 
        {
          System.out.println("WARNING:  Unexpected number of view objects found");
          System.out.println("Skipping...");
          return;
        }
        NodeList viewCriteria = doc.getElementsByTagName("ViewCriteria");
        Node viewObject = viewObjects.item(0);
        
        vaAliasAttributeMap = new HashMap<String,Node>();
        vaNameAttributeMap  = new HashMap<String,Node>();
        
        extendedVaAliasAttributeMap = new HashMap<String,Node>();
        extendedVaNameAttributeMap = new HashMap<String,Node>();
        
        colNameTableNames = new HashMap<String,HashSet>();
        colAliasName = new HashMap<String,String>();
        
        extendedColNameTableNames = new HashMap<String,HashSet>();
        extendedColAliasName = new HashMap<String,String>();
        
        if(viewCriteria != null)      {             
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
            
            processViewCriteria(doc, viewCriteria, f, modelJprLocation, jsff, criteriaName, model,component); 
        }
    }
    
    //jsff, criteriaName, searchRegionId
    public void processViewCriteria(XMLDocument voXml, NodeList viewCriteria, String fileName, String modelJprLocation,
                   String jsff, String criteriaName, String model, String component) throws Exception {
      
      Node viewObject = voXml.getElementsByTagName("ViewObject").item(0);

      for(int i = 0; i< viewCriteria.getLength(); i++){
          
        Node vc = viewCriteria.item(i);
          
        String viewCriteriaName = XMLParserHelper.getAttributeValue(vc, "Name");
        if(!viewCriteriaName.equals(criteriaName) && !ViewCriteriaHelper.parentVCMatches(vc,criteriaName))
          continue;
        NodeList vcRows = vc.getChildNodes();
          
        String description = "";
          
        for(int k = 0; k < vcRows.getLength(); k++ ){
            
          Node vc_row = vcRows.item(k);
          if(ViewCriteriaHelper.isEmpty(vc_row.getNodeName()) || !vc_row.getNodeName().equals("ViewCriteriaRow"))
              continue;
        
          String isUpper_row = XMLParserHelper.getAttributeValue(vc_row, "UpperColumns");
          String vcRowName = XMLParserHelper.getAttributeValue(vc_row, "Name");
          String vcRequired = "";
          String isUpper_item = "";        
          
          NodeList vc_row_children = vc_row.getChildNodes();
            
          for(int j =0; j < vc_row_children.getLength(); j++) {
            
            Node vcItem = vc_row_children.item(j);
            String nodeName = vcItem.getNodeName();
              
            Node properitesNode = XMLParserHelper.getChildNodeWithName(vcItem, "Properties");
              if(properitesNode != null) {
                    Node customProperitesNode = XMLParserHelper.getChildNodeWithName(properitesNode, "CustomProperties");
                    if(customProperitesNode != null) {
                        Node renderedPropertyNode = XMLParserHelper.getChildNodeWithMatchingAttribute(customProperitesNode, "Name", "renderedMode");
                        if(renderedPropertyNode != null) {
                            String renderMode = XMLParserHelper.getAttributeValue(renderedPropertyNode, "Value");
                            if(renderMode != null && renderMode.equals("Never"))
                                continue;
                        }
                    }
              }
            
            if(ViewCriteriaHelper.isEmpty(nodeName) || !nodeName.equals("ViewCriteriaItem")) continue;
              
            isUpper_item = "";
            vcRequired = "";
            boolean useBaseViewObject = false;
            vcRequired = XMLParserHelper.getAttributeValue(vcItem, "Required");
              
            String vcViewObjectName = XMLParserHelper.getAttributeValue(vc, "ViewObjectName");
                          
            boolean useNewViewObject = false;
            Node vcViewObject = null;
              
            HashMap<String,HashSet> newColNameTableNames = new HashMap<String,HashSet>();
            HashMap<String,String> newColAliasName = new HashMap<String,String>();
              
            if(vcViewObjectName != null && !vcViewObjectName.contains(XMLParserHelper.getAttributeValue(viewObject, "Name"))) {
                if(baseViewObject != null && vcViewObjectName.contains(XMLParserHelper.getAttributeValue(baseViewObject, "Name"))){
                   //its ok
                } else {                
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
              
            String isUpper = "case-insensitive";
            if(isUpper_item != null && isUpper_item.equals("0"))
                isUpper = "case-sensitive";
            if(isUpper_item == null && isUpper_row != null && isUpper_row.equals("0"))
                isUpper = "case-sensitive";   
              
            description = vcRowName + ":" + vc_name;              
            
            Node va = vaNameAttributeMap.get(viewAttrName);
              
              if(useNewViewObject){
                  va = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(vcViewObject, "ViewAttribute", "Name", viewAttrName);
                  
              } else if(va == null) {
                  va = extendedVaNameAttributeMap.get(viewAttrName);
                  if(va != null)
                      useBaseViewObject = true;
            }          
              
            if(va == null)
                continue;
              String uiControlType = "";
              
            Node properties = XMLParserHelper.getChildNodeWithName(va, "Properties");
              if(properties != null) {
                  Node schemaProps = XMLParserHelper.getChildNodeWithName(properties, "SchemaBasedProperties");
                  if(schemaProps != null) {
                      Node controlType = XMLParserHelper.getChildNodeWithName(schemaProps, "CONTROLTYPE");
                      if(controlType != null) {
                          uiControlType = XMLParserHelper.getAttributeValue(controlType, "Value");
                      }
                  }
              }
              if(uiControlType.equals("choice") || uiControlType.equals("list") 
                 || uiControlType.equals("radio") || uiControlType.equals("check_box"))
                  continue;
              
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
                 continue;
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
              if(colType == null || !colType.contains("VARCHAR"))
                  continue;
            
            if(table_name != null)
               table_name = table_name.trim();
            if(col_name != null)
               col_name= col_name.trim();          
            
            String index = getIndex(table_name,col_name); 
           
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
              
            String col_position = " ";
            if(index.equals("Default")) {
                col_position = SqlIndexFinder.defaultIndexPositions.get(tableName+","+colName);
                
            }else if(index.equals("Function")) {
                col_position = SqlIndexFinder.functionIndexPositions.get(tableName+","+colName);
                
            } else if(index.equals("Both")) {
                col_position += SqlIndexFinder.defaultIndexPositions.get(tableName+","+colName);
                col_position +="::";
                col_position += SqlIndexFinder.functionIndexPositions.get(tableName+","+colName);
            }
              
              String caseSensitive = "No";
              String category = "";
              if(isUpper.contains("case-sensitive"))
                  caseSensitive = "Yes";
              
              if(caseSensitive.equals("No"))
                  continue; //Its already case insensitive
              
              if(caseSensitive.equals("No") && (index.equals("None") || index.equals("Function")))
                  category = "Case2: Valid case-Insensitive search (No Fix)";
              else if(caseSensitive.equals("Yes") && (index.equals("None") || index.equals("Function")))
                  category = "Case3: Case-Sensitive search against data model with no/upper index (Simply make the criteria case-insensitive)";
              else if(vcRequired == null || vcRequired.equals("null") || vcRequired.equals("Optional"))
                  category = "Case4: Optional VCs are assumed to be added to an already good performing SQL (Simply make the criteria case-insensitive)";
              else if(caseSensitive.equals("No") && index.equals("Both"))
                  category = "Case5: Valid case-Insensitive search where plausible index is available to support a performant SQL (No Fix)";
              else if(caseSensitive.equals("No") && index.equals("Default"))
                  category = "Case6: Case-Insensitive search against data model with default index (Search will miss the index and can result in poor performance. Proper indexes should be added)";
              else if(caseSensitive.equals("Yes") && index.equals("Default"))
                  category = "Case7: Case-Sensitive search against data model with default index (Index chnage would be needed to support a performance case-insensitive search)";
              else if(caseSensitive.equals("Yes") && index.equals("Both"))
                  category = "Case8: Case-Sensitive search where both index types are available (Evaluate case-by-case if data model change is required to support case-insensitive search)";
              
            if(category.contains("Case6"))
                continue; 
              
              String simpleCat = ""; //Case1,2 & 5
              if(category.contains("Case2") || category.contains("Case5"))
                  continue;
              if(category.contains("Case3"))
                  simpleCat = "Simply make the search case-insensitive without data model changes (These include Optional VC items; and VC items for which no/upper index exists in DB)";
              if(category.contains("Case7") || category.contains("Case8") || category.contains("Case4"))
                  simpleCat = "Making the search case-insensitive might require data model changes to support good performance (These include Reqd/Selectively Reqd VC items that have default indexes)";
            
              
              if(jsff.contains("oracle/apps/hcm/compensation/salary/core/publicUi/page/SalarySearch.jsff")) //Test page by HCM
                  continue;
              
            if(jsff.contains("fusionapps/hcm/components/hcmPayroll/payrolls/core/ui/public_html/oracle/apps/hcm/payrolls/core/ui/page/PayrollDefinition.jsff")
                && criteriaName.equals("findByLedgerNameDescription")) //VC fixed by FIN already
                continue;
              
            if(!exemptions.contains(ViewCriteriaHelper.getFileName(fileName) + "," + description) && !isExemptedTable(table_name + "," + resolvedTableColumn))
            writer.write(ViewCriteriaHelper.getFileNameInfo(jsff,null) + component + "," + model + "," + ViewCriteriaHelper.getFileName(fileName)
                       + "," +  criteriaName + "," + description + "," + viewAttrName + "," + table_name +  "," + col_name + "," +
                         resolvedTableColumn + "," + colType + "," + index + "," + vcRequired + "," 
                         + col_position + "," + caseSensitive + "," + simpleCat + "\n");

            String lovName = XMLParserHelper.getAttributeValue(va, "LOVName");
              
            if(!ViewCriteriaHelper.isEmpty(lovName) && component.equals("query"))
                lovVosToProcess.add(new LovVO(voXml,fileName, modelJprLocation,lovName,jsff,model,"query panel LOV"));      
            
        }//VC items
      }// VC rows
      }
  }
    
    
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
    
    private static void generateExemptionList() {

     try{
       BufferedReader fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
           "/fatools/opensource/jauditFixScripts/txt/angrybirdCodescanExemptions_new.txt"));
       String line = null;
       
       while((line = fileReader.readLine()) != null) {
         String[] parts = line.split(",");
         if(parts.length < 3)
             continue;
         String issue = parts[0].trim();
         if(issue.equals("UISearches"))
             exemptions.add(parts[1].trim() + "," + parts[2].trim());
         if(issue.equals("UISearchesTable"))
               exemptedTables.add(parts[1].trim());
           //jsff file, VO file, description
       }
        fileReader.close();       
     }catch(Exception e) {
         System.out.println("Skipping exemptions as file could not be found: " + e.getMessage());
     }    
    }
   
    private boolean isExemptedTable(String tables) {
        
        String[] parts = tables.split(",");
        
        if(parts.length < 1)
            return false;
        String origTables = parts[0].trim();
        String tableCol = "";
        if(parts.length > 1)
            tableCol = parts[1].trim();
        
        parts = origTables.split(" ");
        for(int i = 0; i < parts.length; i++){
            if(exemptedTables.contains(parts[i].trim()))
                return true;
        }
        if(!tableCol.equals("")){
            String table = tableCol.replaceAll("\\(\\w+\\)", "").trim();
            if(exemptedTables.contains(table))
                return true;
        }
        return false;
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
    
    public void makeFixes() throws Exception {
        
        BufferedReader fileReader = new BufferedReader(new FileReader("UISearches1_LRG.csv"));
        String line = fileReader.readLine(); //first line is the header - read & ignore
        
        String prevFilePath = null;
        XMLDocument doc = null;
        XmlFixer xmlFixer = null;
        String fileContentsAsString = null;
        
        while((line = fileReader.readLine()) != null) {
            
            String[] parts = line.split(",");
            if(parts.length < 20)
                continue;
            String filePath = viewRoot + parts[7].trim();
            
//            String vcCaseSensitive = parts[18].trim().toLowerCase();
//            String vcRequired = parts[16].trim().toLowerCase();
//            String colIndex = parts[15].trim().toLowerCase();
//            
//            if(vcCaseSensitive.equals("no")) //already case-insensitive
//                continue;
//            if(!vcRequired.equals("optional") && (colIndex.equals("both") || colIndex.equals("default")))
//                continue; //cannot be fixed automatically
            
            String casecategory = parts[19].trim();
            if(!casecategory.startsWith("Simply"))
                continue;
           
            String description = parts[9].trim();
            parts = description.split("-->");
            parts = parts[0].trim().split(":");
            
            String vcRowName = parts[0].trim();
            String vcItemName = parts[1].trim();
            
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
            
            NodeList list = doc.getElementsByTagName("ViewCriteriaItem");
            Node vcItemToFix = null;
            
            for(int i =0; i < list.getLength(); i++){
                Node n = list.item(i);
                String name = XMLParserHelper.getAttributeValue(n, "Name");
                Node parent = XMLParserHelper.getParentNodeWithName(n, "ViewCriteriaRow");
                String vcrName = XMLParserHelper.getAttributeValue(parent, "Name");
                if(name.equals(vcItemName) && vcrName.equals(vcRowName)){
                    vcItemToFix = n;
                    break;
                }                
            }
            if(vcItemToFix != null) {
                String upperCol = XMLParserHelper.getAttributeValue(vcItemToFix, "UpperColumns");
                if(upperCol == null)
                    xmlFixer.addAttribute(vcItemToFix, "UpperColumns", "1");
                else if(upperCol.equals("0"))
                    xmlFixer.modifyAttribute(vcItemToFix, "UpperColumns", "0", "1", true);  
                else
                    System.out.println("Nothing to fix");
            }
        }
        
        if(xmlFixer != null) {
          fileContentsAsString = xmlFixer.getFileContentsAsString(false);        
          doADE(true,new File(prevFilePath),fileContentsAsString); 
        }
    }
    
    //Su: Overriding Joe's base class method to test for defered path
    public void crawlDirectory(String path, boolean bDoADE)
    {
        if (path.contains(".ade_path") || path.contains("/classes/"))
            return;
        
        if(FamilyModuleHelper.isDeferredPath(releaseNum, FamilyModuleHelper.getPathAfterViewRoot(path)))
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
                // Zeesha: Added, because for files modified recently there is an .ade_path file created in that directory, that is neither node, nor dir. Skip this without raising error
                if (sName.contains(".ade_path"))
                    continue;

                if (fileOfInterest(sName))
                {
                    processFile(listOfFiles[i], bDoADE);
                }

            } else if (listOfFiles[i].isDirectory())
            {
                // Added by zeesha - no point going into the directory if we are not interested in it. Better performance
                if (isDirOfInterest(listOfFiles[i].getAbsolutePath()))
                    crawlDirectory(listOfFiles[i].getAbsolutePath(), bDoADE);
            } else
            {
                System.out.println("ERROR:  node is neither file or directory: " +
                                   listOfFiles[i]);
            }
        }

    }
 
}

