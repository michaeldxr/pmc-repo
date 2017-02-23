package oracle.apps.sqlanalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.lovWidth.ModifiedComboboxParser;
import oracle.apps.utility.JoesBaseClass;

import oracle.apps.votuning.UiFileHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class LOVAutoSuggest  extends JoesBaseClass{
    
    private static HashSet<String> m_pageList = new HashSet<String>();
    private static HashMap<String,String> m_pageDefList = new HashMap<String,String>();
    private static HashSet<String> m_CPXList = new HashSet<String>();
    
    private static HashMap<String, String> m_usageIdToPageDef = new HashMap<String, String>();
    private static HashMap<String,String> m_pageToPageDef = new HashMap<String,String>();
    private static HashMap<String,String> m_PageToCpx = new HashMap<String,String>();
    static BufferedWriter writer;
    //static HashMap<String,String> lovVOToBaseVOMapping = new HashMap<String,String>();
    static ModifiedComboboxParser parser;
    static String m_family;
    String bcPath = "";
    static HashMap<String,String> vos = new HashMap<String,String>();
    static String viewRoot = "";
    HashMap<String,String> VOVAs= new HashMap<String,String>();
    
    public LOVAutoSuggest() {
        super(JoesBaseClass.CRAWL_TYPE.JSPX_AND_JSFF_AND_PAGE_DEF_AND_CPX_AND_VO);
    }
    
    public static void main(String[] args) throws Exception {
        
        LOVAutoSuggest scanner = new LOVAutoSuggest();
        writer = new BufferedWriter(new FileWriter("AutoSuggest.csv")); 
        writer.write("Family,Module,Product,Filename,Series,Label,ViewFile,Component,Id \n");
        
        System.out.println("Start: " + new Date());
        String sCrawlDir = "/ade_autofs/ud21_fa/FUSIONAPPS_11.1.1.5.1_LINUX.X64.rdd/LATEST/fusionapps";
        m_family = ViewCriteriaHelper.getFamily(sCrawlDir); 
        viewRoot = ViewCriteriaHelper.getViewRoot(sCrawlDir);
        
        parser = new ModifiedComboboxParser(m_family.toLowerCase(), ModifiedComboboxParser.ScanType.ALL);
        
        scanner.crawlDirectory(sCrawlDir, false);            
        scanner.processFiles();     
        
        writer.close();
        
        SqlIndexFinder.readTableDataFromFile_rel11();
        SqlIndexFinder.readViewDataFromFile_rel11();
        
        writer = new BufferedWriter(new FileWriter("AutoSuggest1.csv")); 
        writer.write("Family,Module,Product,Filename,Series,Label,ViewFile,Component,Id,ListOfTables, MaxRows, Blocks, TableWithMaxRows \n");
        BufferedReader reader = new BufferedReader(new FileReader("AutoSuggest.csv"));
        
        String line = reader.readLine();
        while((line = reader.readLine()) != null){
            
            String[] parts = line.split(",");
            String fileName = parts[6].trim();
            String origFileName = fileName;
            if(fileName.startsWith("oracle/"))
                fileName = vos.get(fileName);
            else 
                fileName = viewRoot + fileName;
            
            if(fileName == null || fileName.equalsIgnoreCase("not found"))
                System.out.println(origFileName);
            
                
            ViewCriteriaHelper.getMaxRows1(fileName,writer,line);
        }
        writer.close();        
        System.out.println("End: " + new Date());
        
    }

    protected void processFile(File fName, boolean bDoADE) {
        String fileName = fName.getPath();

        if(fileName.endsWith(".jsff") || fileName.endsWith(".jspx"))
            m_pageList.add(fileName);
        
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
        
        if(fileName.endsWith("VO.xml")){
            
            int index = fileName.indexOf("/oracle/");
            if(index ==-1)
                return;
            vos.put(fileName.substring(index+1),fileName);
            
        }
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
            
           NodeList lovList1 = doc.getElementsByTagName("inputComboboxListOfValues");
           NodeList lovList2 = doc.getElementsByTagName("inputListOfValues"); 
           HashSet<Node> lovNodes = new HashSet<Node>();

            for (int i = 0; i < lovList1.getLength(); i++)     {  
              Node lovNode = lovList1.item(i);
              String rendered = XMLParserHelper.getAttributeValue(lovNode, "rendered");
              if(!ViewCriteriaHelper.isEmpty(rendered) && rendered.equals("false"))
                 continue;
                
               Node autoSuggest=  XMLParserHelper.getChildNodeWithName(lovNode, "af:autoSuggestBehavior");
               if(autoSuggest == null)
                 continue;
                
              lovNodes.add(lovNode);                
            }
            
            for (int i = 0; i < lovList2.getLength(); i++) {
                Node lovNode = lovList2.item(i);
                String rendered = XMLParserHelper.getAttributeValue(lovNode, "rendered");
                if(!ViewCriteriaHelper.isEmpty(rendered) && rendered.equals("false"))
                    continue;
                
                Node autoSuggest=  XMLParserHelper.getChildNodeWithName(lovNode, "af:autoSuggestBehavior");
                if(autoSuggest == null)
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
                reviewPageDef(pageDef,f,cpx,lovNodes);
            
        } catch (Exception e) {
            e.printStackTrace();
            logException(f, e);
            System.out.println("ERROR:  Error while processing file.  review carefully if the script updated it. " +f);
            return;
        }
    }
    
    protected void reviewPageDef(String pageDef, String jsff, String cpx, HashSet<Node> lovNodes) {
     
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
          
          //Process LOV VOs            
        for(Iterator<Node> iter = lovNodes.iterator(); iter.hasNext();) {
            
            Node lovNode = iter.next();                
            try {  
               
                String modelValue = XMLParserHelper.getAttributeValue(lovNode, "model");
    //                if(modelValue.contains("ShipToLocation"))
    //                    System.out.println("break here");
                    
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
                
//                String family = ViewCriteriaHelper.getFamily(jsff); 
//                if(family != m_family){
//                    m_family = family;
//                   parser = new ModifiedComboboxParser(m_family, ModifiedComboboxParser.ScanType.ALL);
//                }
                
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
                processLovVO(voXml, bcPath, modelJprLocation, lovUses,jsff,modelValue,lovNode.getNodeName(),
                             XMLParserHelper.getAttributeValue(lovNode, "id"));
                
            } catch(Exception e) {
                    e.printStackTrace();
                    System.out.println("Skipping...");
            }
        } //for
            
    } catch (Exception e) {
            e.printStackTrace();
            logException(pageDef, e);
            System.out.println("ERROR:  Error while processing file.  review carefully if the script updated it. " +pageDef);
    }
    }
    
    public void processLovVO(XMLDocument voXml, String voFileName, String modelJprLocation, 
                             String lovUses, String jsff, String modelValue, String component, String id) throws Exception {
        
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
        
        if(!ViewCriteriaHelper.isEmpty(bcPath)){
            //debugWriter.write(bcPath + "," + displayCriteriaValue + "," + modelValue + "," + voFileName + "," + lovUses + "\n");
            processVO(voXml, bcPath, jsff, component,viewAccessorvcUsages,voFileName,id);
        }
    }
    
    public void processVO(XMLDocument doc, String f, String jsff, 
                         String component, ArrayList<String> viewUsageVCs, String origVOName, String id) throws Exception {    
      
        if(doc == null) {
          System.out.println("Could not parse XMLDocument..Skipping");
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
        
        if(VOVAs.containsKey(f)){
            String reqdVA = VOVAs.get(f);
            if(reqdVA.equals("NoReqdVA"))
                writer.write(ViewCriteriaHelper.getFileNameInfo1(jsff) + ViewCriteriaHelper.getFileName(f) + 
                      "," + component + "," + id + "\n");
             
            return;
        }
        
        if(viewCriteria == null || viewCriteria.getLength() == 0 || viewUsageVCs.size() == 0){
            VOVAs.put(f,"NoReqdVA");
            writer.write(ViewCriteriaHelper.getFileNameInfo1(jsff) + ViewCriteriaHelper.getFileName(f) + 
                  "," + component + "," + id + "\n");
            return;
        }
        
        boolean reqdVAVCfound = false;
        
        for(int i = 0; i < viewUsageVCs.size(); i++){
            
            if(reqdVAVCfound)
                break;
            
            String criteriaName = viewUsageVCs.get(i);
            for(int j = 0; j< viewCriteria.getLength(); j++){
                
              Node vc = viewCriteria.item(j);
                
                if(reqdVAVCfound)
                    break;
                
              String viewCriteriaName = XMLParserHelper.getAttributeValue(vc, "Name");
              if(!viewCriteriaName.equals(criteriaName) && !ViewCriteriaHelper.parentVCMatches(vc,criteriaName))
                continue;
              NodeList vcRows = vc.getChildNodes();
            
              for(int k = 0; k < vcRows.getLength(); k++ ){
                  
                  if(reqdVAVCfound)
                      break;
                
                  Node vc_row = vcRows.item(k);
                  if(ViewCriteriaHelper.isEmpty(vc_row.getNodeName()) || !vc_row.getNodeName().equals("ViewCriteriaRow"))
                      continue;
                  String vcRequired = "";
                  NodeList vc_row_children = vc_row.getChildNodes();
              
                  for(int c =0; c < vc_row_children.getLength(); c++) {
                    
                    Node vcItem = vc_row_children.item(c);
                    String nodeName = vcItem.getNodeName();
                    
                    if(ViewCriteriaHelper.isEmpty(nodeName) || !nodeName.equals("ViewCriteriaItem")) continue;
                      
                    vcRequired = "";
                    if(vcRequired != null && !vcRequired.equals("Optional")){                    
                        reqdVAVCfound = true;
                        break;
                    }                    
                  }//c -> vcitems
              }//k --> vc rows
            }// j --> viewcriteria
                      
        } //i --> viewAccessorVC
        if(!reqdVAVCfound){
            VOVAs.put(f,"NoReqdVA");
            writer.write(ViewCriteriaHelper.getFileNameInfo1(jsff) + ViewCriteriaHelper.getFileName(f) + 
                  "," + component + "," + id + "\n");
        }
        else{
            VOVAs.put(f,"ReqdVA");
        }
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
    
    private String getRealPageDefPath(String path) 
    {
        String actual = path.replaceAll("\\.", "/") +".xml";
        
        if(m_pageDefList.containsKey(actual))
            return m_pageDefList.get(actual);
        
        return "not found";
    }

    public void processFiles()
    {
    // first load the cpx so we have a mapping between pages and pageDefs
      for(Iterator i = m_CPXList.iterator() ; i.hasNext();)
        digestCPX((String)i.next());
      
      for(Iterator i = m_pageList.iterator(); i.hasNext();)
          processPage((String)i.next());
    }
}
