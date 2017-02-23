package oracle.apps.rowCountAnalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;
import oracle.apps.utility.JoesBaseClass;

import oracle.apps.utility.LRGUtil;
import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RowCountThresholdNew extends JoesBaseClass {
    
    HashSet<String> m_pageList = new HashSet<String>();
    HashMap<String,String> m_pageDefList = new HashMap<String,String>();
    HashSet<String> m_CPXList = new HashSet<String>();
    
    static AdeDiffHelper diffHelper = new AdeDiffHelper("diff.txt");
    static BufferedReader reader;
    static BufferedWriter errorWriter;
    static String viewRoot;
    HashSet<String> alreadyModifiedIterators = new HashSet<String>();
    
    static String mode="view"; //LRG/view/premerge mode
    
    HashMap<String, ArrayList<String>> m_pageToPageDefUsageId = new HashMap<String, ArrayList<String>>();
    HashMap<String, String> m_usageIdToPageDef = new HashMap<String, String>();
    static BufferedWriter writer;
    
    public static Boolean bDoADE = false;
    
    boolean bDebug = false;  
    HashMap<String,HashSet> exemptions = new HashMap<String,HashSet>();    
    
    public RowCountThresholdNew() {
        super(JoesBaseClass.CRAWL_TYPE.JSPX_AND_JSFF_AND_PAGE_DEF_AND_CPX);
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
    }

    protected String getSummaryReport() {
        return "SCRIPT: Set RowCountThreshold to -1";
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

          NodeList tableList = doc.getElementsByTagName("table");
          HashMap<String,HashSet<String>> usedTableIterators = new  HashMap<String,HashSet<String>>();

              for (int i = 0; i < tableList.getLength(); i++) 
              {
                Node iter = tableList.item(i);
                  
                String id = XMLParserHelper.getAttributeValue(iter, "id");
                String value = XMLParserHelper.getAttributeValue(iter, "value");
                  
                  if(value == null)
                      continue;
                  
                  int index = value.indexOf("bindings.");
                  if(index == -1)
                      continue;
                  String binding = value.substring(index+9);
                  index = binding.indexOf(".");
                      if(index == -1)
                          continue;
                  binding = binding.substring(0, index);
                  
                  HashSet<String> tableIds = new HashSet<String>();
                  if(usedTableIterators.containsKey(binding))
                      tableIds = usedTableIterators.get(binding);

                tableIds.add(id);
                usedTableIterators.put(binding,tableIds);
                
              }
            
            int index = f.indexOf("public_html") +11;
            String key = f.substring(index);
            
            ArrayList<String> usageIdList = m_pageToPageDefUsageId.get(key);
            if(usageIdList == null) return;

            for(int i = 0; i < usageIdList.size(); i++)
            {
              String usageId = usageIdList.get(i);
            
              String path = m_usageIdToPageDef.get(usageId);
                if(path == null || path.equals("not found"))
                    continue;
                reviewPageDef(path,usedTableIterators,FamilyModuleHelper.getPathAfterViewRoot(f));
            }
           
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR:  Error while processing file.  review carefully if the script updated it. " +f);
        }
    }
    
    protected void reviewPageDef(String pageDef, HashMap<String,HashSet<String>> usedTableIterators,String jsff) {
        try{
            if(mode.equals("premerge") && !RCTPremergeChecker.processPageDef(pageDef))
                return;
            XMLDocument doc = XMLParserHelper.getXMLDocument(pageDef);
            if(doc == null)
                return;
            NodeList adfc = doc.getElementsByTagName("pageDefinition");
            if (adfc == null) 
            {
              if(bDebug)
                System.out.println("REVIEW:  Not a pageDefinition file ---> " + pageDef);
               return;
            }
                       
            NodeList iteratorList = doc.getElementsByTagName("iterator");
            HashMap<String,String> iterators = new HashMap<String,String>();

            for (int i = 0; i < iteratorList.getLength(); i++) 
            {
              Node iter = iteratorList.item(i);
              String id = XMLParserHelper.getAttributeValue(iter, "id");
              String rct = XMLParserHelper.getAttributeValue(iter, "RowCountThreshold");
                if(rct == null) rct ="null";
            
             iterators.put(id, rct);
            }
            
            NodeList treeList = doc.getElementsByTagName("tree");
           // HashMap<String,String> usedTreeIterators = new HashMap<String,String>();
            
            for (int i = 0; i < treeList.getLength(); i++) 
            {
              Node tree = treeList.item(i);
              String id = XMLParserHelper.getAttributeValue(tree, "id");
                if(usedTableIterators.containsKey(id)){
                    String iter = XMLParserHelper.getAttributeValue(tree, "IterBinding");  
                    
                    if(iterators.containsKey(iter)){
                        String rct = iterators.get(iter);
                        if(!rct.equals("-1") && !isExempt(iter,pageDef))
                            writer.write(FamilyModuleHelper.getFileNameInfo(pageDef) + jsff
                             + "," + iter + "," + id + "," + getTableIds(usedTableIterators.get(id)) + "," + rct + "\n");
                    }
                }
            }

        }catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR:  Error while processing file.  review carefully if the script updated it. " +pageDef);
        }                
    }
    
    private String getTableIds(HashSet<String> tableIds) {
        
        StringBuffer sb = new StringBuffer();
        for(Iterator<String> it = tableIds.iterator(); it.hasNext(); ){
            sb.append(it.next() + " ");
        }
        
        return sb.toString();
    }
    
    public static void main(String[] args) throws Exception
    {
        Date start = new Date();
        mode = System.getProperty("mode");
        if(mode == null)
            mode = "view";
        //mode = "LRG";
        
        RowCountThresholdNew x = new RowCountThresholdNew();
        x.generateExemptionList();

        writer = new BufferedWriter(new FileWriter("rowCountThreshold_new.csv"));
        writer.write("Family,Module,Product,Filename,Label,UX File,Iterator Id, Tree Id, TableId,RCT\n");  
        
        if(mode.equals("view")) {
       
            viewRoot = args[0].trim();
            String sCrawlDir = args[1].trim();
            
            if(args.length > 2)
               bDoADE = new Boolean(args[2].trim());
            
            File f = new File(sCrawlDir);
            if(!f.exists()){
                System.out.println("Crawl directory does not exist - " + sCrawlDir);
                System.exit(1);
            }
            
            //String sCrawlDir = "/ade_autofs/ud21_fa/FUSIONAPPS_11.1.1.5.1_LINUX.X64.rdd/LATEST/fusionapps";            
            System.out.println("Crawling directory: " + sCrawlDir + " at: " + new java.util.Date());
            x.crawlDirectory(sCrawlDir, bDoADE);                
            x.processFiles();         
            writer.close();
            
            System.out.println("Finished crawling directory at: " + new java.util.Date() );
            
            if(bDoADE){
                System.out.println("Now fixing violations..");
                reader=new BufferedReader(new FileReader("rowCountThreshold_new.csv"));
                errorWriter = new BufferedWriter(new FileWriter("errors.txt"));
                diffHelper.startDiffLog();
                x.makeFixes();
                diffHelper.closeDiffLog(); 
                errorWriter.close();
                reader.close();
                System.out.println("Finished at: " + new java.util.Date());
            }    
        }
        
        else if(mode.equals("LRG")){
            
            String series = args[0]; //only needs series name as the argument      
            //String series = "FSM_MAIN_GENERIC"; 
            
            String label = LRGUtil.getLatestLabel(series);
            System.out.println("Running RowCountThresholdNew Scan on series: " + series + ".......");
            
            String serverTop = FamilyModuleHelper.getLabelServerTop1(series);
            String viewRoot = serverTop + "/"+series+".rdd/"+label+"/";
            
            ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
            
            for (int j = 0; j < families.size(); j++) {
                
                String family = families.get(j);
                String sCrawlDir = LRGUtil.getCrawlDirNew(series, family, "RowCountThresholdNew");
                
                System.out.println("Crawling dir '" + viewRoot + sCrawlDir + "' for family: " + family);                
                x.crawlDirectoryNew(viewRoot,sCrawlDir,false);   
                x.processFiles();  
                System.out.println("Done crawling for family "+family+".");
                x.resetDataStructures();
            }     
            
            writer.close();
            x.dumpToAB(series); 
            LRGUtil.addLabelInfoToAB("RowCountThresholdNew", series, label);    
        }
        
        FamilyModuleHelper.printScanDuration(start,"RowCountThresholdNew");
                                       
    }
    
    private void dumpToAB(String series) throws Exception{
        
        Connection con = DriverManager.getConnection("jdbc:oracle:thin:codescan/codescan@angrybirds.oracle.com:1521:codescan");
        con.setAutoCommit(false);
        PreparedStatement insert = con.prepareStatement("INSERT INTO codescan_results(SERIES,LABEL,FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION) VALUES (?,?,?,?,?,?,?,?,?)");     
        PreparedStatement delete = con.prepareStatement("DELETE FROM codescan_results where issuetype = 'RowCountThresholdNew' and series='" + series + "'");
        delete.execute();   
        delete.close();
        
        reader=new BufferedReader(new FileReader("rowCountThreshold_new.csv"));
        String line = reader.readLine();
        while((line = reader.readLine()) != null) {
            
            String[] parts = line.split(",");
            
            String seriesLabel = parts[4].trim();
            String[] labelParts = seriesLabel.split("X64_");
            String label = seriesLabel;
            if(labelParts.length > 1){
                label = labelParts[1].trim();
            }else if(series.contains("FSM")){
              int index = seriesLabel.indexOf("GENERIC");
              if(index != -1)
                 label = seriesLabel.substring(index+8);
              else
                label = "unknown";
            }
            
            String description = "";
            String rct = parts[9].trim();
            if(rct.equals("null"))
                description = "Iterator with Id " + parts[6] + " does not have RowCountThreshold set (defaults to 0)";
            else
                description =  "Iterator with Id " + parts[6] + " has RowCountThreshold set to " + rct;                                
            
            insert.setString(1, series);
            insert.setString(2, label);
            insert.setString(3, parts[0].trim());
            insert.setString(4, parts[1].trim());
            insert.setString(5, parts[2].trim());
            insert.setString(6, parts[3].trim());
            insert.setString(7, "RowCountThresholdNew");
            insert.setString(8, "TableId: " + parts[8].trim());
            insert.setString(9, description);
            insert.addBatch();
        }      
      
      insert.executeBatch();
      insert.close();
      con.commit();
      con.close();  
      reader.close();
    }
    
    private void resetDataStructures() {
        m_pageList = new HashSet<String>();
        m_pageDefList = new HashMap<String,String>();
        m_CPXList = new HashSet<String>();
        m_pageToPageDefUsageId = new HashMap<String, ArrayList<String>>();
        m_usageIdToPageDef = new HashMap<String, String>();
    }
    
    private void makeFixes() throws Exception{
        
        String line = reader.readLine(); //first line is the header - read & ignore
        String prevFilePath = null;
        XMLDocument doc = null;
        XmlFixer xmlFixer = null;
        String fileContentsAsString = null;
        
        while((line = reader.readLine()) != null) {
          
          try{            
            String[] parts = line.split(",");
              if(parts.length < 10) {
                errorWriter.write("Cannot fix due to some missing info:" + line);
                continue;
              }
            
            String filePath = viewRoot + "/" + parts[3].trim();
            String iteratorId = parts[6].trim();       
            String rct = parts[9].trim();
              if(rct.equals("null")) rct = null;
          
            if(!filePath.equals(prevFilePath)) {
              
                if(prevFilePath != null) {
                  fileContentsAsString = xmlFixer.getFileContentsAsString(false);    //this will get the changed file contents  
                  diffHelper.applyFix(prevFilePath,fileContentsAsString); 
                }
              
              doc = XMLParserHelper.getXMLDocument(filePath);     
                if(doc == null) 
                    continue;
              xmlFixer = new XmlFixer(filePath);
            }
            prevFilePath = filePath;
            
            if(doc == null) 
                continue;
              
            NodeList components = doc.getElementsByTagName("iterator");
            if(components == null || components.getLength() < 1)
                continue;
            Node n = null;
              
            for(int i = 0; i < components.getLength(); i++) {
              n = components.item(i);
              String id = XMLParserHelper.getAttributeValue(n, "id");
              if(id !=null && id.equals(iteratorId))
                  break;
              else
                  n=null;
            }
              
              if(n !=null) {
                if(!alreadyModifiedIterators.contains(parts[3].trim() + " " + iteratorId))
                    xmlFixer.modifyAttribute(n,"RowCountThreshold",rct,"-1",true);
                alreadyModifiedIterators.add(parts[3].trim() + " " + iteratorId);
              }
              

            } catch (Exception e) {
                e.printStackTrace();
            }
        } //while
        
        if(xmlFixer != null) { //to take care of the last set of violations in the last file
          fileContentsAsString = xmlFixer.getFileContentsAsString(false);        
          diffHelper.applyFix(prevFilePath,fileContentsAsString);
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
    
    private void digestCPX(String f) 
    {   
        try {
            
            XMLDocument doc = XMLParserHelper.getXMLDocument(f);

        NodeList pageMaps = doc.getElementsByTagName("pageMap");
        NodeList pageDefUsages = doc.getElementsByTagName("pageDefinitionUsages");
        if(pageMaps.getLength() != 1 || pageDefUsages.getLength() != 1)
        {
            System.out.println("ERROR:  Unexpected number of pageMap/pageDefinitionUsages sections in file ---> " +f);
            return;
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
            String sPage = "";
            String sUsageId = "";
        
            //check if it is there already
            if(path != null)
                sPage = path.getNodeValue();
            if(usageId !=null)
                sUsageId = usageId.getNodeValue();
            
            ArrayList<String> usageIdList = m_pageToPageDefUsageId.get(sPage);
            if(usageIdList == null)
            {
                usageIdList = new ArrayList<String>();
                m_pageToPageDefUsageId.put(sPage, usageIdList);
            }
            
            usageIdList.add(sUsageId);
            
    //            m_pageToPageDefUsageId.put(sPage, sUsageId);
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
            
                String nv = path.getNodeValue();

                String sUsageId= "";
                if(usageId != null)
                    sUsageId = usageId.getNodeValue();
             
                String realPath = getRealPageDefPath(nv);
              
                m_usageIdToPageDef.put(sUsageId, realPath);
            }

        } catch (Exception e) {
            e.printStackTrace();
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
    
    public void generateExemptionList() {
        
    try{
      BufferedReader fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
          "/fatools/opensource/jauditFixScripts/txt/angrybirdCodescanExemptions_Su.txt"));
      String line = null;
      
      while((line = fileReader.readLine()) != null) {
        String[] parts = line.split(",");
        if(parts.length < 3)
            continue;
        String issue = parts[0].trim();
        if(!issue.equals("RowCountThresholdNew"))
            continue;
        String fileName = parts[1].trim();
        String iteratorId = parts[2].trim();
          
        HashSet<String> exemptLines = null;
        if(!exemptions.containsKey(fileName)) 
          exemptLines = new HashSet<String>();
        else 
          exemptLines = exemptions.get(fileName);
        
        exemptLines.add(iteratorId);
        exemptions.put(fileName, exemptLines);
      }
    }catch(Exception e) {
        System.out.println("Skipping exemptions as file could not be found: " + e.getMessage());
    }
    }
    
    private boolean isExempt(String id, String pageDef){
        
        pageDef = FamilyModuleHelper.getPathAfterViewRoot(pageDef);
        if(!exemptions.containsKey(pageDef)) 
            return false;
        
        HashSet<String> lines = (HashSet<String>)exemptions.get(pageDef);
        if(lines.contains(id))
            return true;
        
        return false;
    }

}
