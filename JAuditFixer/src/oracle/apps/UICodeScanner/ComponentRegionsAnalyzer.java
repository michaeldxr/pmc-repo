package oracle.apps.UICodeScanner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.utility.JoesBaseClass;

import oracle.xml.parser.v2.XMLDocument;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.security.MessageDigest;
import javax.xml.bind.DatatypeConverter;

import oracle.apps.utility.LRGUtil;

/* This class does staticcode analysis to construct all component Ids*/
/* Later on do this on a codeline basis */

public class ComponentRegionsAnalyzer extends JoesBaseClass{
    
    static BufferedWriter writer;
    static String series = "FUSIONAPPS_11.1.8_LINUX.X64";
    static HashMap<String,String> pageViewIds = new HashMap<String,String>();
    static HashMap<String,String> jspxViewIds = new HashMap<String,String>();
    
    static HashMap<String,String> regionIdToJsff = new HashMap<String,String>();
    static HashMap<String,String> jspxToView = new HashMap<String,String>(); 
    static HashMap<String,String> pageToPath = new HashMap<String,String>(); 
    
    static MessageDigest md;
    
    String currentViewId="";
    static Pattern pResourceBundle = Pattern.compile("^[#\\$]\\{(?:adfBundle|mobileBundle)\\['([\\w\\.]+)'\\]\\}$");
    static Pattern pBundleReference = Pattern.compile("#\\{(\\w+)\\['(.+?)'\\]\\}");
    static Pattern pBundleReference1 = Pattern.compile("#\\{.*?(\\w+)\\['(.+?)'\\].*?\\}");
    static Pattern pApplcoreReference = Pattern.compile("#\\{applcoreBundle\\.(.+?)\\}");
    static Pattern pApplcoreReference1 = Pattern.compile("applcoreBundle\\.(.+?)");
    static Pattern pDffBinding = Pattern.compile("#\\{bindings\\.(\\w+)\\}");
    static Pattern pBinding1 = Pattern.compile("#\\{bindings\\.(\\w+)\\.hints\\.tooltip\\}");
    static Pattern pBinding2 = Pattern.compile("#\\{bindings\\.(\\w+)\\.hints\\.(\\w+)\\.tooltip\\}");
    
    
    static HashMap<String,String> nodesToScan = new HashMap<String,String>();
    static{
        //Add component Class
        nodesToScan.put("inputText","shortDesc/label/labelAndAccessKey");
        nodesToScan.put("outputText","value");
        nodesToScan.put("inputFile","shortDesc/label/labelAndAccessKey");
        nodesToScan.put("richTextEditor","label/labelAndAccessKey/shortDesc");
        nodesToScan.put("inputListOfValues","shortDesc/label/labelAndAccessKey");
        nodesToScan.put("commandToolbarButton","shortDesc/text/textAndAccessKey");
        nodesToScan.put("commandButton","shortDesc/text/textAndAccessKey");
        nodesToScan.put("inputComboboxListOfValues","popupTitle"); //dynamic
        nodesToScan.put("keyFlexfield","label"); //dynamic
        nodesToScan.put("descriptiveFlexfield","label"); //dynamic
        nodesToScan.put("attachment","shortDesc"); //dynamic
        nodesToScan.put("query","headerText"); //dynamic
        nodesToScan.put("quickQuery","headerText"); //dynamic
        nodesToScan.put("inputDate","shortDesc/label/labelAndAccessKey");//dynamic
        nodesToScan.put("commandMenuItem","text/shortDesc/textAndAccessKey");
        nodesToScan.put("commandImageLink","text/shortDesc/textAndAccessKey");
        nodesToScan.put("commandNavigationItem","shortDesc/text/textAndAccessKey");
        nodesToScan.put("treeTable","summary");
        nodesToScan.put("table","summary");
        nodesToScan.put("dialog","title");
        nodesToScan.put("showDetail","undisclosedText");
        nodesToScan.put("selectOneChoice","label/labelAndAccessKey");
        nodesToScan.put("selectOneRadio","label/labelAndAccessKey");
        nodesToScan.put("selectManyCheckbox","label/labelAndAccessKey");
        nodesToScan.put("poll","");
        nodesToScan.put("panelCollection","");
        nodesToScan.put("showDetailHeader","text");
        nodesToScan.put("showDetailItem","shortDesc/text/textAndAccessKey");
        nodesToScan.put("menu","shortDesc/text/textAndAccessKey");
        nodesToScan.put("chooseDate","");
        
        nodesToScan.put("selectBooleanCheckbox","text/shortDesc/label/value");
        nodesToScan.put("commandLink","shortDesc/text/textAndAccessKey");
        nodesToScan.put("applicationsPanel","title"); //dynamic
        nodesToScan.put("keyFlexfilter","label");
        nodesToScan.put("applicationsTable",""); //dynamic
        nodesToScan.put("popup","");
        
    }
    
    public ComponentRegionsAnalyzer(CRAWL_TYPE type){
        super(type);
    }
    
    static Connection strcon;
    static Connection devcon;
    static PreparedStatement psdev;
    static PreparedStatement psstr;
    static ResultSet rsstr;
  
    public static void main(String[] args) throws Exception{
        
        md = MessageDigest.getInstance("SHA-1");
        
        String sCrawlDir = "/ade_autofs/ud21_fa/FUSIONAPPS_11.1.8GRC_LINUX.X64.rdd/LATEST/fusionapps/grc/components";
        
        strcon = DriverManager.getConnection("jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan","STRSCANNER","strscanner");
        psstr = strcon.prepareStatement("SELECT string_value from strings,bundles where strings.bundle_id = bundles.id" +
            " and series = 'FUSIONAPPS_PT.R8INT_LINUX.X64' and string_key = ? and fqn = ?");
        
     /*   devcon = DriverManager.getConnection("jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan","findev","findev");
        psdev  = devcon.prepareStatement("INSERT INTO ADF_COMPONENTS_MAP(UI_FILE,COMPONENT_ID,COMPONENT_TYPE,DISPLAY_TEXT," +
                "RESOLVED_DISPLAY_TEXT,SEQ_ID) VALUES(?,?,?,?,?,LOG_COMPONENTS_SEQ.nextval)");
        
       Statement st = devcon.createStatement();
//        st.executeUpdate("DELETE FROM ADF_COMPONENTS_MAP where component_type = 'Taskmenu itemNode'");

        
        ComponentRegionsAnalyzer scanner = new ComponentRegionsAnalyzer(CRAWL_TYPE.TASKMENU);
        scanner.crawlDirectory(sCrawlDir,false);
        
        int count = st.executeUpdate("DELETE FROM adf_components_map where seq_id in (select seq_id from adf_components_map acm," +
                         " (select ui_file,display_text, max(seq_id) as best_id from adf_components_map where " +
            "component_Type = 'Taskmenu itemNode' group by ui_file,display_text) best where " +
            "best.ui_file = acm.ui_file AND acm.component_type = 'Taskmenu itemNode' " +
            "and best.display_text = acm.display_text and best.best_id <> acm.seq_id)");
        
        st.close();
        psdev.close();
        devcon.commit();
        devcon.close(); */

        
//        scanner.enterStaticData();
//        scanner.scanRootMenuFile();
        
        ComponentRegionsAnalyzer scanner = new ComponentRegionsAnalyzer(CRAWL_TYPE.JSPX_AND_JSFF);
       
        writer = new BufferedWriter(new FileWriter("adfComponentDetails.csv"));
        writer.write("Filename, NodeName, NodeId, DisplayText,ResolvedDisplayText\n");
       
        scanner.crawlDirectory(sCrawlDir,false);
        writer.close();
        scanner.writeToDB();
        
        //ArrayList<String> families = LRGUtil.getFamiliesFromSeries("FUSIONAPPS_PT.R8INT_LINUX.X64");
        
       /* for(int i = 0; i < families.size(); i++){
            String family = families.get(i);
            
            scanner = new ComponentRegionsAnalyzer(CRAWL_TYPE.JPR_TASKFLOW);
            writer = new BufferedWriter(new FileWriter("adfComponentDetails.csv"));
            writer.write("Filename, NodeName, NodeId, DisplayText,ResolvedDisplayText\n");
            regionIdToJsff = new HashMap<String,String>();
            jspxToView = new HashMap<String,String>(); 
            pageToPath = new HashMap<String,String>(); 
            String sCrawlDir = "/ade_autofs/gr25_fa/" + series + ".rdd/LATEST/fusionapps/" + family + "/components";
            System.out.println("Scanning directory: " + sCrawlDir);
            scanner.crawlDirectory(sCrawlDir,false);
            
            //scanner = new ComponentRegionsAnalyzer(CRAWL_TYPE.JSPX_AND_JSFF);
            scanner.crawlDirectory(sCrawlDir,false);

            writer.close();
            
            scanner.writeToDB();
        }*/
        
        if (strcon != null) strcon.close();
        if(rsstr != null) rsstr.close();
        if(psstr != null) psstr.close(); 
        
    }
    

    protected void processFile(File fName, boolean bDoADE) {
        
        String absPath = fName.getAbsolutePath();
        
//        if(absPath.endsWith(".jpr"))
//            processJpr(absPath);
//        else if(absPath.endsWith("Flow.xml") || (absPath.contains("/flow/") && absPath.endsWith(".xml")))
//            processTF(absPath);
          if(absPath.endsWith(".jsff") || absPath.endsWith(".jspx")) 
            processUIPage(absPath);
//        else if(absPath.endsWith("taskmenu.xml"))
//            processTaskMenu(absPath);
    }
    
    private void processTaskMenu(String absPath){
        
        try{
            XMLDocument doc = XMLParserHelper.getXMLDocument(absPath);
            
            if(doc == null){
                System.out.println("XML file could not be parsed: " + absPath);
                return;
            }
            NodeList itemNodes = doc.getElementsByTagName("itemNode");
            if(itemNodes.getLength() == 0)
                return;
            
            Pattern menuBundle = Pattern.compile("#\\{adfBundle\\['([\\w\\.]+)'\\]\\['([\\w\\.]+)'\\]\\}");
            Matcher m = null;
            
            Node itemNode = null;
            String resolvedLabel = "";
            String label = "";
            String id = "";
            String fqn = "";
            String strKey = "";
            String focusViewId = "";
            
            for(int i =0; i< itemNodes.getLength(); i++) {
                
                itemNode = itemNodes.item(i);
                label = XMLParserHelper.getAttributeValue(itemNode, "label");
                if(label == null)
                    continue;
                focusViewId = XMLParserHelper.getAttributeValue(itemNode, "focusViewId");
                m = menuBundle.matcher(label);
                
                if(m.find()){
                    strKey = m.group(2);
                    fqn = m.group(1);
                    resolvedLabel = getDescriptiveString(strKey,fqn);
                    
                    md.update(resolvedLabel.getBytes("UTF-8"));
                    id = DatatypeConverter.printHexBinary(md.digest());
                    
                    psdev.setString(1,focusViewId);
                    psdev.setString(2,id.toLowerCase());
                    psdev.setString(3,"Taskmenu itemNode");
                    psdev.setString(4,label);
                    psdev.setString(5,resolvedLabel);
                    psdev.addBatch();
                }
            }
            psdev.executeBatch();
           
        }catch(Exception e){
            e.printStackTrace();
        }
        
    }
    
    private void scanRootMenuFile() throws Exception {
        
        Pattern menuBundle = Pattern.compile("#\\{menuBundle\\['oracle.apps.menu.ResourcesAttrBundle'\\]\\.(\\w+)\\}");
        Matcher m = null;
        XMLDocument doc = XMLParserHelper.getXMLDocument("/scratch/sudgupta/root_menu.xml");
        if(doc == null){
            System.out.println("root-menu file could not be parsed.");
            return;
        }
        
        HashMap<String,String> resolvedLabels = new HashMap<String,String>();
        Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan","STRSCANNER","strscanner");
        String sql = "SELECT string_key,string_value from strings,bundles where strings.bundle_id = bundles.id" +
            " and series = 'FUSIONAPPS_11.1.1.5.1_LINUX.X64' and fqn = 'oracle.apps.menu.ResourcesAttrBundle'";
        Statement st = conn.createStatement();
        
        ResultSet rs = st.executeQuery(sql);
        while(rs.next()){
            resolvedLabels.put(rs.getString("STRING_KEY"),rs.getString("STRING_VALUE"));
        }
        if(rs != null) rs.close();
        if(st != null) st.close();
        if(conn != null) conn.close();
        
        conn = DriverManager.getConnection("jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan","findev","findev");
        st = conn.createStatement();
        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO ADF_COMPONENTS_MAP(UI_FILE,COMPONENT_ID,COMPONENT_TYPE,DISPLAY_TEXT," +
                "RESOLVED_DISPLAY_TEXT,SEQ_ID) VALUES(?,?,?,?,?,LOG_COMPONENTS_SEQ.nextval)");
        
        st.executeUpdate("DELETE FROM ADF_COMPONENTS_MAP where component_type = 'itemNode'");
        if(st != null) st.close();
        
        NodeList itemNodes = doc.getElementsByTagName("itemNode");
        String labelString = "";
        String resolvedLabel = "";
        String label = "";
        String id = "";
        
        for(int i =0; i< itemNodes.getLength(); i++) {
            
            Node itemNode = itemNodes.item(i);
            label = XMLParserHelper.getAttributeValue(itemNode, "label");
            id = XMLParserHelper.getAttributeValue(itemNode,"id");
            m = menuBundle.matcher(label);
            
            if(m.find()){
                labelString = m.group(1);
                resolvedLabel = resolvedLabels.get(labelString);
                pstmt.setString(1,"");
                pstmt.setString(2,id);
                pstmt.setString(3,"itemNode");
                pstmt.setString(4,labelString);
                pstmt.setString(5,resolvedLabel);
                pstmt.addBatch();
            }
        }
        pstmt.executeBatch();
        pstmt.close();
        conn.close();
    }
    
    private void processJpr(String absPath){
        
        int lastIndex = absPath.lastIndexOf("/");
        if(lastIndex == -1)
            return;
        String adfcFile = absPath.substring(0,lastIndex)+"/public_html/WEB-INF/adfc-config.xml";
        
        File f = new File(adfcFile);
        if(!f.exists())
            return;
        
        XMLDocument doc = XMLParserHelper.getXMLDocument(adfcFile);
        if(doc == null)
            return;
        currentViewId="";
        NodeList views = doc.getElementsByTagName("view");
        for(int i = 0; i < views.getLength(); i++) {
            Node view = views.item(i);
            String viewId = XMLParserHelper.getAttributeValue(view,"id");
            currentViewId+=  "/"+viewId + " "; 
            Node page = XMLParserHelper.getChildNodeWithName(view, "page");
            
            if(page == null)
                continue;
            String pageTxt = page.getTextContent().trim();
            //jspxViewIds.put(pageTxt,",/" + viewId);
            jspxToView.put(pageTxt,"/"+viewId);
        }                    
    }
    
    private void processTF(String absPath){
        
        try{             
            
            XMLDocument doc = XMLParserHelper.getXMLDocument(absPath);
            if(doc == null){
                System.out.println("XML file could not be parsed: " + absPath);
                return;
            }
            NodeList tfs = doc.getElementsByTagName("task-flow-definition");
            if(tfs.getLength() == 0){
                //System.out.println("XML has no task flow definitions. Skipping... " + absPath);
                return;
            }
            
            for(int i =0; i < tfs.getLength(); i++) {
                
                Node tf = tfs.item(i);
                String tfId = XMLParserHelper.getAttributeValue(tf, "id");
                
                ArrayList<Node> views = XMLParserHelper.getChildNodesWithName(tf, "view");
                for(int j = 0; j < views.size(); j++) {
                    Node view = views.get(j);
                    String viewId = XMLParserHelper.getAttributeValue(view, "id");
                    Node page = XMLParserHelper.getChildNodeWithName(view, "page");
                    if(page == null)
                        continue;
                    String pageTxt = page.getTextContent().trim();
                    
                    regionIdToJsff.put("/"+tfId + "/" + viewId,pageTxt);
                    jspxToView.put(pageTxt,currentViewId);
                }
            }
        } catch(Exception e){
            e.printStackTrace();
        }            
        
    }

    private void processUIPage(String absPath){
        
        try{   
            int index = absPath.indexOf("/oracle");
            if(index == -1){
                System.out.println("UI file path does not contain /oracle: " + absPath);
                return;
            }
            String pathAfterOracle = absPath.substring(index);
            pageToPath.put(pathAfterOracle,absPath);
           // if(true) return;
            
            //System.out.println(absPath);
            XMLDocument doc = XMLParserHelper.getXMLDocument(absPath);
            if(doc == null){
                System.out.println("XML file could not be parsed: " + absPath);
                return;
            }
            
            NodeList resourceBundles = doc.getElementsByTagName("set");
            HashMap<String,String> resourceBundleNames = new  HashMap<String,String>();
            
            for(int i = 0; i < resourceBundles.getLength(); i++) {
                
                Node setNode = resourceBundles.item(i);
                String var = XMLParserHelper.getAttributeValue(setNode, "var");
                String value = XMLParserHelper.getAttributeValue(setNode, "value");
                Matcher m = pResourceBundle.matcher(value);
                if(m.matches())
                    resourceBundleNames.put(var,m.group(1));
            }
            
            for(Iterator<String> it = nodesToScan.keySet().iterator(); it.hasNext();){
                
                String nodeName = it.next();
                String nodeAttr = nodesToScan.get(nodeName);
                String[] parts = nodeAttr.split("/");
                
                NodeList children = doc.getElementsByTagName(nodeName);
                for(int i = 0; i < children.getLength(); i++) {
                    
                    Node child = children.item(i);
                    String nodeId = XMLParserHelper.getAttributeValue(child, "id");
                    
                    String attrValue = null;
                    
                    for(int j = 0 ; j< parts.length; j++) {
                        attrValue = XMLParserHelper.getAttributeValue(child, parts[j].trim());
                        if(attrValue != null)
                            break;
                    }
                    
                    if(nodeName.equals("popup") && attrValue == null){
                        Node childDialog = XMLParserHelper.getChildNodeWithName(child, "af:dialog");
                        if(childDialog != null)
                            attrValue = XMLParserHelper.getAttributeValue(childDialog, "title");
                    }
                    
                    if(nodeName.equals("commandLink") && attrValue == null) {
                        Node childOutput = XMLParserHelper.getChildNodeWithName(child, "af:outputText");
                        if(childOutput != null)
                            attrValue = XMLParserHelper.getAttributeValue(childOutput, "value");
                    }
                    
                    if(nodeName.equals("applicationsTable")){
                        String tableId = XMLParserHelper.getAttributeValue(child, "tableId");
                        NodeList tables = doc.getElementsByTagName("table");
                        for(int t =0; t < tables.getLength(); t++) {
                            Node table = tables.item(t);
                            String id = XMLParserHelper.getAttributeValue(table, "id");
                            if(id.equals(tableId)){
                                attrValue = XMLParserHelper.getAttributeValue(table, "summary");
                                break;
                            }
                        }
                    }
                   
                    if(nodeName.equals("keyFlexfield") && attrValue == null){
                        Node parentCol = XMLParserHelper.getParentNodeWithName(child, "af:column");
                        attrValue = XMLParserHelper.getAttributeValue(parentCol, "headerText");    
                        
                    }
                    if(attrValue == null) {
                        Node parentCol = XMLParserHelper.getParentNodeWithName(child, "af:panelLabelAndMessage");
                        String forId = XMLParserHelper.getAttributeValue(parentCol, "for"); 
                        if(forId !=null && forId.equals(nodeId)){
                            attrValue = XMLParserHelper.getAttributeValue(parentCol, "label");    
                            if(attrValue == null)
                                attrValue = XMLParserHelper.getAttributeValue(parentCol, "labelAndAccessKey");   
                        }
                    }
                    
                    if(attrValue == null && nodeName.equals("showDetail"))
                        attrValue = "show/hide";
                    if(attrValue == null)
                        attrValue = "";
                    
                    
                    String descriptiveTxt = getDescriptiveString(attrValue,resourceBundleNames);
                    if(nodeName.equals("poll")){
                        attrValue = "poll";
                        descriptiveTxt = "poll";
                    }
                    
          
//                    if(!nodeName.equals("keyFlexField") && !nodeName.equals("attachment") && !nodeName.equals("descriptiveFlexfield")
//                       && !nodeName.equals("applicationsPanel") && !nodeName.equals("applicationsTable"))
                        writer.write(FamilyModuleHelper.getPathAfterViewRoot(absPath) + "," + nodeName + "," 
                                 + nodeId + "," + attrValue.replace(",","<Comma>") + "," + 
                                 descriptiveTxt.replace(",","<Comma>") + "\n");
                    
                    if(nodeName.equals("descriptiveFlexfield")){
                            
                        String value = XMLParserHelper.getAttributeValue(child,"value");
                        
                        Matcher m = pDffBinding.matcher(value);
                        String binding = "";
                        if(m.find())
                            binding = m.group(1);
                        
                        Node childFlexSegmentHints = XMLParserHelper.getChildNodeWithName(child, "fnd:flexfieldSegmentHints");
                        if(childFlexSegmentHints != null){
                            
                            ArrayList<Node> childFlexSegments = XMLParserHelper.getChildNodesWithName(childFlexSegmentHints, "fnd:flexfieldSegmentHint");
                            for(int j = 0; j< childFlexSegments.size(); j++){
                                String segmentCode = XMLParserHelper.getAttributeValue(childFlexSegments.get(j), "segmentCode");
                                writer.write(FamilyModuleHelper.getPathAfterViewRoot(absPath) + ",descriptiveFlexField (id = " + nodeId + "),"
                                             + nodeId + "_" + binding + "_" + segmentCode + "_Display,dff segment, DescriptiveFlexField Segment\n");
                            }
                        }                         
                    
                    }
                }
            }
            
        }catch (Exception e){
            e.printStackTrace();
        }         
        
    }
    
    private void enterStaticData() throws Exception {
        
        Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan","findev","findev");
        Statement st = conn.createStatement();
        st.executeUpdate("INSERT INTO ADF_COMPONENTS_MAP(RESOLVED_DISPLAY_TEXT,SEQ_ID) values ('ESS Schedule Request Basic - ',-1)");
        st.executeUpdate("INSERT INTO ADF_COMPONENTS_MAP(RESOLVED_DISPLAY_TEXT,SEQ_ID) values ('ESS Schedule Request Advanced - ',-2)");
        st.executeUpdate("INSERT INTO ADF_COMPONENTS_MAP(RESOLVED_DISPLAY_TEXT,COMPONENT_TYPE,SEQ_ID) values ('poll','poll',-3)");
        st.executeUpdate("INSERT INTO ADF_COMPONENTS_MAP(DISPLAY_TEXT, RESOLVED_DISPLAY_TEXT,SEQ_ID) " +
            "values ('UI Shell Main Area dynamic tab','UI Shell Main Area dynamic tab',-4)");
        st.executeUpdate("INSERT INTO ADF_COMPONENTS_MAP(DISPLAY_TEXT, RESOLVED_DISPLAY_TEXT,SEQ_ID) " +
            "values ('UI Shell Main Area dynamic component ','UI Shell Main Area dynamic component',-5)");
        if(st != null) st.close();
        if(conn != null) conn.close();
    }
    
    
    private void writeToDB() throws Exception {
        
        Connection conn = null;
        try{
            conn = DriverManager.getConnection("jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan","findev","findev");
            conn.setAutoCommit(false);
            
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("DELETE FROM ADF_COMPONENTS_MAP where UI_FILE like 'fusionapps/grc/components/%' and component_type not in " +
                "('Taskmenu itemNode','itemNode') and seq_id > 0");
            //stmt.executeUpdate("DELETE FROM REGION_PAGE_MAP");
            //stmt.executeUpdate("DELETE FROM VIEW_PAGE_MAP");
//            stmt.executeUpdate("DELETE FROM ADF_COMPONENTS_MAP");
            //stmt.executeUpdate("DELETE FROM UI_PAGE_DETAILS");
            
           /* PreparedStatement pstmt = conn.prepareStatement("INSERT INTO REGION_PAGE_MAP(SERIES,REGIONID,UI_PAGE,VIEWID) " +
                    "VALUES(?,?,?,?)");
            
            String regionId="";
            String jsff = "";
            String pagePath = "";
            String viewId = "";
            
            for(Iterator<String> it = regionIdToJsff.keySet().iterator(); it.hasNext();){
                
                regionId=it.next();
                jsff = regionIdToJsff.get(regionId);
                pagePath = pageToPath.get(jsff);
                viewId = jspxToView.get(jsff);
               
                if(pagePath == null || pagePath.trim().equals(""))
                    continue;
                
                pstmt.setString(1, series);
                pstmt.setString(2,regionId);
                pstmt.setString(3,FamilyModuleHelper.getPathAfterViewRoot(pagePath));       
                pstmt.setString(4,viewId);
                pstmt.addBatch();     
            }
            
//            //Add empty region id row to avoid FK violations when tagging log component details
//            pstmt.setString(1, series);
//            pstmt.setString(2,"");
//            pstmt.setString(3,"");
//            pstmt.addBatch();
            
            pstmt.executeBatch();
            pstmt.close();
            
            pstmt = conn.prepareStatement("INSERT INTO VIEW_PAGE_MAP(SERIES,VIEWID,UI_PAGE) " +
                                "VALUES(?,?,?)");
           
            for(Iterator<String> it = jspxToView.keySet().iterator(); it.hasNext();){
                
                jsff=it.next();
                regionId = jspxToView.get(jsff);
                pagePath = pageToPath.get(jsff);
                if(pagePath == null || pagePath.trim().equals(""))
                    continue;
                
                pstmt.setString(1, series);
                pstmt.setString(2,regionId);
                pstmt.setString(3,FamilyModuleHelper.getPathAfterViewRoot(pagePath));
                pstmt.addBatch();              
            }
            
//            //Add empty view id row to avoid FK violations when tagging log component details
//            pstmt.setString(1, series);
//            pstmt.setString(2,"");
//            pstmt.setString(3,"");
//            pstmt.addBatch();
            pstmt.executeBatch();
            pstmt.close();
            
            ResultSet rs = stmt.executeQuery("SELECT PRODUCTID,CODE from products");
            HashMap<String,String> productCodeIds = new HashMap<String,String>();
            String productid="";
            String productcode="";
            while(rs.next()){
                productid = rs.getString("PRODUCTID");
                productcode=rs.getString("CODE");
                productCodeIds.put(productcode,productid);
            }
            rs.close();
            
            pstmt = conn.prepareStatement("INSERT INTO UI_PAGE_DETAILS(UI_PAGE,FAMILY,MODULE,PRODUCT,PRODUCTID) " +
                                "VALUES(?,?,?,?,?)");
            
            for(Iterator<String> it = pageToPath.values().iterator(); it.hasNext();){
                
                String uiPage = it.next();
                String pageDetails = FamilyModuleHelper.getFileNameInfo(uiPage);
                String[] parts = pageDetails.split(",");                
                pstmt.setString(1,parts[3].trim());
                pstmt.setString(2,parts[0].trim());
                pstmt.setString(3,parts[1].trim());
                pstmt.setString(4,parts[2].trim());
                pstmt.setString(5,productCodeIds.get(parts[2].trim()));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            pstmt.close();*/
            
            //Write the ADF Components Map
            
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO ADF_COMPONENTS_MAP(UI_FILE,COMPONENT_ID,COMPONENT_TYPE,DISPLAY_TEXT," +
                "RESOLVED_DISPLAY_TEXT,SEQ_ID) VALUES(?,?,?,?,?,LOG_COMPONENTS_SEQ.nextval)");
            BufferedReader reader = new BufferedReader(new FileReader("adfComponentDetails.csv"));
            String line = reader.readLine();
            int count = 0;
            while((line = reader.readLine()) != null){
                
                count++;                
                String[] parts = line.split(",");

                pstmt.setString(1,parts[0].trim());
                pstmt.setString(2,parts[2].trim());
                pstmt.setString(3,parts[1].trim());
                if(parts.length>3 && parts[3].length() <200)
                    pstmt.setString(4,parts[3].trim().replace("<COMMA>",","));
                else
                    pstmt.setString(4,"");
                if(parts.length>4 && parts[4].length() <200)
                    pstmt.setString(5,parts[4].trim().replace("<COMMA>",","));
                else
                    pstmt.setString(5,"");

                pstmt.addBatch();
                if(count%1000==0)
                    pstmt.executeBatch();            
            }
            
            pstmt.executeBatch();
            pstmt.close();
           // stmt.close();
            conn.commit();
            
        }catch(Exception e){
            e.printStackTrace();
            conn.rollback();
        }finally{
            if(conn != null)
                conn.close();
        }
    }
    
    private String getDescriptiveString(String key, String fqn) {
        
        String result = key;
        try{
            psstr.setString(1, key);
            psstr.setString(2, fqn);
            rsstr = psstr.executeQuery();
            if(rsstr.next()){
                result = rsstr.getString("STRING_VALUE").replace("&amp;","");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return result;
        
    }
    
    private String getDescriptiveString(String attrValue,HashMap<String,String> resourceBundleNames) {
        
        String result = attrValue;
        if(attrValue == null || attrValue.equals(""))
            return result;
        try{          
            
            Matcher m = pBinding1.matcher(attrValue);
            if(m.find())
                return m.group(1);
            
            m = pBinding2.matcher(attrValue);
            if(m.find())
                return m.group(1)+"."+m.group(2);
            
            
           m = pBundleReference.matcher(attrValue);
           if(m.find()) {
                String bundleVar = m.group(1);
                String stringkey = m.group(2);
                if(resourceBundleNames.containsKey(bundleVar)){
                    String bundleFqn = resourceBundleNames.get(bundleVar);
                    psstr.setString(1, stringkey);
                    psstr.setString(2, bundleFqn);
                    rsstr = psstr.executeQuery();
                    if(rsstr.next()){
                        String s = rsstr.getString("STRING_VALUE").replace("&amp;","");
                        result = result.replace(m.group(), s);
                    }
                }
            } 
            m=pBundleReference1.matcher(result);
            while(m.find()){
                String bundleVar = m.group(1);
                String stringkey = m.group(2);
                if(resourceBundleNames.containsKey(bundleVar)){
                    String bundleFqn = resourceBundleNames.get(bundleVar);
                    psstr.setString(1, stringkey);
                    psstr.setString(2, bundleFqn);
                    rsstr = psstr.executeQuery();
                    if(rsstr.next()){
                        String s = rsstr.getString("STRING_VALUE").replace("&amp;","");
                        result = result.replace(m.group(), s);
                    }
                }
            }
            
            m = pApplcoreReference.matcher(result);
            if(m.find())
                result = result.replace(m.group(),m.group(1));
            
            m = pApplcoreReference1.matcher(result);
            while(m.find())
                result = result.replace(m.group(),m.group(1));
            
//            result = result.replace("#{","");
//            result = result.replace("}","");
            
        }catch(Exception e){
            e.printStackTrace();
        }
        return result;
    }
    
    protected String getSummaryReport() {
        return null;
    }

    protected String getSummaryReportSubject() {
        return null;
    }
    
    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }
}
