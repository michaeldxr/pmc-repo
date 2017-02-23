package oracle.apps.sqlanalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import java.io.File;

import java.io.FileWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.lovWidth.ModifiedComboboxParser;

import oracle.apps.utility.JoesBaseClass;

import oracle.apps.utility.LRGUtil;
import oracle.apps.votuning.UiFileHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ViewCriteriaBindVarValue extends JoesBaseClass{
    
      
    private static HashSet<String> m_pageList = new HashSet<String>();
    private static HashMap<String,String> m_pageDefList = new HashMap<String,String>();
    private static HashSet<String> m_CPXList = new HashSet<String>();
    
    private static HashMap<String, String> m_usageIdToPageDef = new HashMap<String, String>();
    private static HashMap<String,String> m_pageToPageDef = new HashMap<String,String>();
    private static HashMap<String,String> m_PageToCpx = new HashMap<String,String>();
    
    static BufferedWriter writer;
    String bcPath = "";
   
    static String m_family;
    
    //static HashSet<String> exemptions = new HashSet<String>();
    
    static Pattern pModelUsage = Pattern.compile("bindings\\.(\\w+)\\.");
    static ModifiedComboboxParser parser;
    static HashSet<LovVO> lovVosToProcess = new HashSet<LovVO>();
    
    static HashMap<String,XMLDocument> pathToDoc = new HashMap<String,XMLDocument>();
    static HashMap<String,HashSet<String>> voVcs = new HashMap<String,HashSet<String>>();
    
    public ViewCriteriaBindVarValue() 
    {
        super(JoesBaseClass.CRAWL_TYPE.JSPX_AND_JSFF_AND_PAGE_DEF_AND_CPX);
    }

    protected void processFile(File fName, boolean bDoADE) {
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
    }

    protected String getSummaryReport() {
        return null;
    }

    protected String getSummaryReportSubject() {
        return null;
    }
    
    protected void processPage(String f) 
    {
       
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
          NodeList queryList2 = doc.getElementsByTagName("quickQuery"); 
          TreeMap<String,Node> usedQueryModels = new TreeMap<String,Node>();

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
                usedQueryModels.put(m.group(1),iter);  
            else
                System.out.println("Weird af:query model");           
          }
            
            for (int i = 0; i < queryList2.getLength(); i++) 
            {
              Node iter = queryList2.item(i);
              NamedNodeMap attributes = iter.getAttributes();
              Node modelNode = attributes.getNamedItem("model");
              if(modelNode == null)
                  continue;
              String value = modelNode.getNodeValue();
              Matcher m = pModelUsage.matcher(value);
                if(m.find())
                  usedQueryModels.put(m.group(1),iter);                
                else
                  System.out.println("Weird af:query model");           
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
                reviewPageDef(pageDef,usedQueryModels,f,cpx,lovNodes);
            
        } catch (Exception e) {
            e.printStackTrace();
            logException(f, e);
            System.out.println("ERROR:  Error while processing file.  review carefully if the script updated it. " +f);
            return;
        }
    }

    protected void reviewPageDef(String pageDef, TreeMap<String,Node> usedModels, String jsff, String cpx, HashSet<Node> lovNodes) {
     
        lovVosToProcess.clear();
      //  System.out.println("Reviewing pagedef: " + pageDef);

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
                  
                  //TODO: if criteria=implicit - generally iterator bind is weirdly specified root.am.criteria and needs special handling to resolve...
                  //This is actually true for a handful of cases not necessary related to implicit criteria (ER when time permits)
                  
                  Node iterator = iteratorNodes.get(searchRegionBinds);
                  String voUsage = XMLParserHelper.getAttributeValue(iterator, "Binds");
                  
                  String dataControl = XMLParserHelper.getAttributeValue(iterator, "DataControl");
                  
                  String amName = ViewCriteriaHelper.getAMNameFromDataControlName(dataControl);
                  String dcPackage = ViewCriteriaHelper.getDataControlPackage(dataControl,cpx);
                  
                  String amAbsPath = ViewCriteriaHelper.getAmFile(dcPackage + "." + amName, jsff);
                  ArrayList<String> viewUsageVCs = new ArrayList<String>();//TODO: Required? 
                  String voFullPackage = ViewCriteriaHelper.getViewUsagePackageFromAm(amAbsPath, voUsage,viewUsageVCs);
                 
                  String modelJprLocation = UiFileHelper.getJprLocationForModelFile(amAbsPath);
                  parser.setJprAbsFilePath(modelJprLocation);
                  
                  bcPath = "";
                  
                  Object[] returnObjects = ViewCriteriaHelper.getViewObject(modelJprLocation, voFullPackage, jsff);
                  XMLDocument voXml = (XMLDocument)returnObjects[0];
                  bcPath = (String)returnObjects[1];
                  
                  if(voXml == null || ViewCriteriaHelper.isEmpty(bcPath))
                      continue;
                  
                  pathToDoc.put(bcPath,voXml);
                  HashSet<String> vcs = new HashSet<String>();
                  if(voVcs.containsKey(bcPath))
                      vcs = voVcs.get(bcPath);
                  
                  vcs.add(criteria);
                  for(int j = 0; j < viewUsageVCs.size(); j++)
                      vcs.add(viewUsageVCs.get(j));
                  
                  voVcs.put(bcPath,vcs);
                  
                  
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
                        continue;
                    }
                    Node pageDefTreeBinding = treeNodes.get(tableIteratorId);
                    
                    String iterName = XMLParserHelper.getAttributeValue(pageDefTreeBinding, "IterBinding");
                    if(iterName != null)
                        pageDefIterBinding = iteratorNodes.get(iterName);
                    if (pageDefIterBinding == null) {
                        System.out.println("Could not obtain iterator binding:" 
                                           + modelValue + "(" + pageDef + ")");
                        continue;
                    }
                    
                } else {
                    Node listBinding = listNodes.get(modelIteratorId);
                    if (listBinding == null) {
                        System.out.println("Could not obtain list binding:" 
                                           + modelIteratorId + "(" + pageDef + ")");
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
        if (ViewCriteriaHelper.isEmpty(listDataSource)) {
            System.out.println("Could not obtain the List data source for the lov name: " + lovUses + " vo file: " + voFileName);
            return;
        }
        
        Node displayCriteria = XMLParserHelper.getChildNodeWithName(listBindingNode, "DisplayCriteria");
        String displayCriteriaValue=null;
        if(displayCriteria != null) 
            displayCriteriaValue = XMLParserHelper.getAttributeValue(displayCriteria, "Name");

        Node viewcriteriaUsage = XMLParserHelper.getChildNodeWithName(listBindingNode, "ViewCriteriaUsage");
        String vcUsageValue=null;
        if(viewcriteriaUsage != null) 
            vcUsageValue = XMLParserHelper.getAttributeValue(viewcriteriaUsage, "Name");
       
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
        if(ViewCriteriaHelper.isEmpty(bcPath) || voXml == null)
            return;
        
        pathToDoc.put(bcPath,voXml);
        HashSet<String> vcs = new HashSet<String>();
        if(voVcs.containsKey(bcPath))
            vcs = voVcs.get(bcPath);
        
        if(displayCriteriaValue != null) 
            vcs.add(displayCriteriaValue);
            
        for(int j = 0; j < viewAccessorvcUsages.size(); j++)
            vcs.add(viewAccessorvcUsages.get(j));      
        
        if(vcUsageValue != null && 
           (component.equals("af:inputComboboxListOfValues") || component.equals("query panel combo LOV"))) 
            vcs.add(vcUsageValue);
        
        voVcs.put(bcPath,vcs);
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
        
        
        ViewCriteriaBindVarValue scanner = new ViewCriteriaBindVarValue();
        String sCrawlDir = "/ade_autofs/ud21_fa/FUSIONAPPS_11.1.1.5.1_LINUX.X64.rdd/LATEST/fusionapps/";
        writer = new BufferedWriter(new FileWriter("VO_VCs_UsedInUIs.csv"));   
        writer.write("Family, VO Name,ViewCriteria,VCRow,VCItem,IsBindVarValue\n");
        
        ArrayList<String> families = LRGUtil.getFamiliesFromSeries("FUSIONAPPS_11.1.1.5.1_LINUX.X64");
        
        for(int i =0; i<families.size(); i++) {
            
            m_pageList = new HashSet<String>();
            m_pageDefList = new HashMap<String,String>();
            m_CPXList = new HashSet<String>();
            m_usageIdToPageDef = new HashMap<String, String>();
            m_pageToPageDef = new HashMap<String,String>();
            m_PageToCpx = new HashMap<String,String>();
            pathToDoc = new HashMap<String,XMLDocument>();
            voVcs = new HashMap<String,HashSet<String>>();
            
            m_family= families.get(i);
            parser = new ModifiedComboboxParser(m_family.toLowerCase(), ModifiedComboboxParser.ScanType.ALL);
            
            scanner.crawlDirectory(sCrawlDir+m_family+"/components", false);            
            scanner.processFiles();
            scanner.processVOsUsedInUI();
        }
        writer.close();
    }
    
   /* public static void main(String[] args) throws Exception{
        
        ViewCriteriaBindVarValue scanner = new ViewCriteriaBindVarValue();
        String sCrawlDir = "/ade_autofs/ud21_fa/FUSIONAPPS_11.1.1.5.1_LINUX.X64.rdd/LATEST/fusionapps/prc/components/";
        writer = new BufferedWriter(new FileWriter("VO_VCs_UsedInUIs_prc.csv"));   
        writer.write("VO Name,ViewCriteria,VCRow,VCItem,IsBindVarValue\n");
        
        m_family = ViewCriteriaHelper.getFamily(sCrawlDir); 
        parser = new ModifiedComboboxParser(m_family, ModifiedComboboxParser.ScanType.ALL);
        
        scanner.crawlDirectory(sCrawlDir, false);            
        scanner.processFiles();
        
        scanner.processVOsUsedInUI();
        writer.close();
    }*/
    
    public void processVOsUsedInUI() {
        
        try{
            for(Iterator<String> it = pathToDoc.keySet().iterator(); it.hasNext();){
                String voPath = it.next();
                XMLDocument doc = pathToDoc.get(voPath);
                
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
                    continue;
                HashSet<String> usedVCs = voVcs.get(voPath);
                
                for(int i = 0; i< viewCriteria.getLength(); i++){
                    
                  Node vc = viewCriteria.item(i);
                    
                  String viewCriteriaName = XMLParserHelper.getAttributeValue(vc, "Name");
                  if(!usedVCs.contains(viewCriteriaName))
                      continue;
                    
                 NodeList vcRows = vc.getChildNodes();
                    
                 for(int k = 0; k < vcRows.getLength(); k++ ){
                     
                   Node vc_row = vcRows.item(k);
                   if(ViewCriteriaHelper.isEmpty(vc_row.getNodeName()) || !vc_row.getNodeName().equals("ViewCriteriaRow"))
                       continue;
                     
                     String vcRowName = XMLParserHelper.getAttributeValue(vc_row, "Name");
                     NodeList vc_row_children = vc_row.getChildNodes();
                     
                     for(int j =0; j < vc_row_children.getLength(); j++) {
                       
                       Node vcItem = vc_row_children.item(j);
                       String nodeName = vcItem.getNodeName();
                       
                       if(ViewCriteriaHelper.isEmpty(nodeName) || !nodeName.equals("ViewCriteriaItem")) 
                           continue;
                         
                         String vciname = XMLParserHelper.getAttributeValue(vcItem, "Name");
                         String operator = XMLParserHelper.getAttributeValue(vcItem,"Operator");
                         if(operator != null && !operator.equalsIgnoreCase("STARTSWITH"))
                             continue;
                         
                         String bindVarValue = XMLParserHelper.getAttributeValue(vcItem,"IsBindVarValue");
                         writer.write(m_family + ", " + voPath+", " + viewCriteriaName + ", " + vcRowName + ", " + vciname + ", " + bindVarValue + "\n");
                     } //for vc items
                 } //for vc rows
                
             } //for vc
            }// for all vo
            
            
        }catch(Exception e) {
            e.printStackTrace();
        }
        
    }
}
