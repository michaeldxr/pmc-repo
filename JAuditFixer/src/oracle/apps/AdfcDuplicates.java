package oracle.apps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;

import java.io.FileWriter;

import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;

import java.sql.PreparedStatement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import java.util.Iterator;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.ProcessExecutor;
import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class AdfcDuplicates {
    
    HashMap<String,ManagedBean> faBeans = new HashMap<String,ManagedBean>();
    HashMap<String,ConfigView> faViews = new HashMap<String,ConfigView>();
    static BufferedWriter writer;
    static BufferedWriter writer1;
    //static BufferedWriter exemptionWriter;
    static String viewRoot;
    static String label;
    
   public static String getFileNameDetails(String fileName) {
        String family = "";
        String product = "";
        String module = "";
        
        String[] parts = fileName.split("/");
        if(parts.length > 1) family = parts[1].trim().toUpperCase();
        if(parts.length > 3) module = parts[3].trim().toUpperCase();
        if(parts.length > 4) product = parts[4].trim().toUpperCase();
        
        return family + ", " + module + ", " + product + ", " + fileName + ", ";
    }

    private void processFile(String fName) {

        String lowerCaseName = fName.toLowerCase();
        if (lowerCaseName.contains("/noship/") ||
            lowerCaseName.contains("/test/") ||
            fName.contains("Test/") ||
            lowerCaseName.contains("servicetest") ||
            lowerCaseName.contains("datasecuritytest") ||
            lowerCaseName.contains("securitypublictest") ||
            lowerCaseName.contains("publicuitest") ||
            lowerCaseName.contains("structuretest") ||
            lowerCaseName.contains("modeltest") ||
            lowerCaseName.contains("uitest") ||
            lowerCaseName.contains("testui") ||
            lowerCaseName.contains("setest") ||
            lowerCaseName.contains("testviewcontroller") ||
            lowerCaseName.contains("flextest") ||
            lowerCaseName.contains("uimodeler-launch-test") ||
            lowerCaseName.contains("publicuibasetest") ||
            lowerCaseName.contains("uipickertest"))
            return;
        
        XMLDocument doc = XMLParserHelper.getXMLDocument(viewRoot + fName);
        if(doc == null)
            return;
        
        NodeList beanList = doc.getElementsByTagName("managed-bean");

        for (int i = 0; i < beanList.getLength(); i++) {
            Node bean = beanList.item(i);
            NodeList beanChildren = bean.getChildNodes();

            String sScope = "not set";
            String sBeanName = "not set";
            String sBeanClass = "not set";
            HashSet<String> sBeanProperties = new HashSet<String>();
            String propertyName= "";
            String propertyValue = "";

            for (int j = 0; j < beanChildren.getLength(); j++) {
                Node child = beanChildren.item(j);
                if (child.getNodeName().equals("managed-bean-scope")) {
                    sScope = child.getTextContent().trim();
                } else if (child.getNodeName().equals("managed-bean-name")) {
                    sBeanName = child.getTextContent().trim();
                } else if (child.getNodeName().equals("managed-bean-class")) {
                    sBeanClass = child.getTextContent().trim();
                } else if(child.getNodeName().equals("managed-property")){
                    Node pName = XMLParserHelper.getChildNodeWithName(child, "property-name");
                    if(pName != null)
                        propertyName = pName.getTextContent().trim();
                    Node pValue = XMLParserHelper.getChildNodeWithName(child, "value");
                    if(pValue != null)
                        propertyValue = pValue.getTextContent().trim();

                    sBeanProperties.add(propertyName + ":" + propertyValue);
                }
            } // bean children - class, name & propteries
            if(!faBeans.containsKey(sBeanName)){
                ManagedBean mBean = new ManagedBean(sBeanName, sBeanClass, sScope, sBeanProperties, fName);
                faBeans.put(sBeanName,mBean);
            }
            else {
                ManagedBean mBean = faBeans.get(sBeanName);
                mBean.compareBean(sBeanClass, sScope, sBeanProperties, fName, writer1);
            }
            
        } //beanList
        
        
        NodeList viewList = doc.getElementsByTagName("view");
        for(int i =0; i< viewList.getLength(); i++) {
            
            String viewId = "";
            String page = "";
            
            Node v = viewList.item(i);
            if(v != null) 
                viewId = XMLParserHelper.getAttributeValue(v, "id");
            Node p = XMLParserHelper.getChildNodeWithName(v, "page");
            if(p != null)
                page = p.getTextContent().trim();
            
            if(!faViews.containsKey(viewId)){
                ConfigView mView = new ConfigView(viewId, page, fName, "view");
                faViews.put(viewId, mView);
            }
            else {
                ConfigView mView = faViews.get(viewId);
                mView.compareView(page, fName, writer1, "view");
            }           
        }
        
        NodeList urlViewList = doc.getElementsByTagName("url-view");
        for(int i =0; i< urlViewList.getLength(); i++) {
            
            String viewId = "";
            String page = "";
            
            Node v = urlViewList.item(i);
            if(v != null) 
                viewId = XMLParserHelper.getAttributeValue(v, "id");
            Node p = XMLParserHelper.getChildNodeWithName(v, "url");
            if(p != null)
                page = p.getTextContent().trim();
            
            if(!faViews.containsKey(viewId)){
                ConfigView mView = new ConfigView(viewId, page, fName, "url-view");
                faViews.put(viewId, mView);
            }
            else {
                ConfigView mView = faViews.get(viewId);
                mView.compareView(page, fName, writer1, "url-view");
            }           
        }
        
        NodeList methodCallList = doc.getElementsByTagName("method-call");
        for(int i =0; i< methodCallList.getLength(); i++) {
            
            String id = "";
            Node m = methodCallList.item(i);
            if(m != null) 
                id = XMLParserHelper.getAttributeValue(m, "id");
            
            if(!faViews.containsKey(id)){
                ConfigView mView = new ConfigView(id, "", fName, "method-call");
                faViews.put(id, mView);
            }
            else {
                ConfigView mView = faViews.get(id);
                mView.compareView("", fName, writer1, "method-call");
            }       
        }
    }

    
    public static void main(String[] args) throws Exception{
        
        String series = "";
        java.util.Date start = new java.util.Date();
        if(args.length> 0 && args[0] != null && args[0].trim() != "")
            series = args[0].trim();
        
        //series = "FUSIONAPPS_11.1.1.5.1_LINUX.X64";
        viewRoot = FamilyModuleHelper.getLabelServerTop1(series) + "/"+series+".rdd/LATEST/";
        System.out.println("Starting AdfcDuplicates scan: " + start);
        
        //viewRoot = "/ade_autofs/ud21_fa/FUSIONAPPS_11.1.1.5.1_LINUX.X64.rdd/LATEST/";
        
        String absPath = new File(viewRoot + ".labellog.emd.gz").getCanonicalPath();
        String[] parts = absPath.split("/");
        if(parts.length > 4)
            label = parts[4];
        
        ProcessExecutor pe = new ProcessExecutor();   
        ProcessBuilder pb = new ProcessBuilder("bash","-c","zcat " + viewRoot + 
                                                ".labellog.emd.gz | grep 'FILE fusionapps' | grep 'adfc-config.xml' " +
            "| grep -vi '/noship/' | cut -f2 -d ' '");
        pe.runProcess(pb, "/tmp/adfcFileList.txt", false); 
        
        BufferedReader reader = new BufferedReader(new FileReader("/tmp/adfcFileList.txt"));
        writer = new BufferedWriter(new FileWriter("adfcDuplicates.txt"));
        writer1 = new BufferedWriter(new FileWriter("adfcDuplicates.csv"));
        //exemptionWriter = new BufferedWriter(new FileWriter("exemptions1.csv"));
        writer1.write("FAMILY,MODULE,PRODUCT,FILENAME, BEAN NAME/ID, BEAN CLASS/PAGE, BEAN SCOPE & PROPERTIES\n");
        
        AdfcDuplicates scanner = new AdfcDuplicates();
        
        String line = "";
        while((line = reader.readLine()) != null) {
            String configFile = line.trim();
            scanner.processFile(configFile);
        }
        
        reader.close();
        scanner.printDuplicates();
        writer.close();
        writer1.close();
        
        scanner.filterExemptions();
        //exemptionWriter.close();
        
        scanner.dumpToAngrybirds(series);
        FamilyModuleHelper.printScanDuration(start,"AdfcDuplicates");
        
    }
    
    private void filterExemptions() throws Exception {
        
        HashSet<String> exemptions = new HashSet<String>();
        BufferedReader reader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
            "/fatools/opensource/jauditFixScripts/JAuditFixer/exemptions1.csv"));
        String line = "";
        while((line = reader.readLine()) != null) {
            String parts[] = line.split(",");
            if(parts.length < 5)
                continue;
            String fileName = parts[3].trim();
            String adfcArtifactName = parts[4].trim();
            exemptions.add(fileName+","+adfcArtifactName);
        }
        reader.close();
        
        BufferedWriter w = new BufferedWriter(new FileWriter("adfcDuplicates1.csv"));
        reader = new BufferedReader(new FileReader("adfcDuplicates.csv"));
        
        line = reader.readLine();
        while((line = reader.readLine()) != null) {
            String parts[] = line.split(",");
            if(parts.length < 5)
                continue;
            String fileName = parts[3].trim();
            String adfcArtifactName = parts[4].trim();
            if(exemptions.contains(fileName+","+adfcArtifactName))
                continue;
            
            w.write(line+"\n");
        }        
        
        w.close();
        reader.close();
    }
    
    private void dumpToAngrybirds(String series) throws Exception {
        
         Connection con =
          DriverManager.getConnection("jdbc:oracle:thin:codescan/codescan@angrybirds.oracle.com:1521:codescan");
        PreparedStatement insert =
          con.prepareStatement("INSERT INTO codescan_results (SERIES,LABEL,FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION) VALUES (?,?,?,?,?,?,?,?,?)");     
        PreparedStatement delete =
          con.prepareStatement("DELETE FROM codescan_results where issuetype = 'AdfcDuplicates' and series='" + series + "'");
        delete.execute();   
        
        BufferedReader reader = new BufferedReader(new FileReader("adfcDuplicates1.csv"));
        String line = reader.readLine();
        
        while((line = reader.readLine()) != null) {
            
            String description = "";
            String[] parts = line.split(",");
            if(parts.length< 5){
                System.out.println(line);
                continue;
            }
            insert.setString(1,series);
            insert.setString(2,label);
            insert.setString(3,parts[0].trim());
            insert.setString(4,parts[1].trim());
            insert.setString(5,parts[2].trim());
            insert.setString(6,parts[3].trim());
            insert.setString(7,"AdfcDuplicates");
            insert.setString(8,parts[4].trim());
            
            if(parts.length > 5)
                description = parts[5].trim();
            if(parts.length > 6)
                description += ";" + parts[6].trim();
            
            insert.setString(9,description);
            insert.addBatch();
        }
        
        insert.executeBatch();
        insert.close();
        con.commit();
        con.close();  
    }
    
    
    private void printDuplicates() {
        
        for(Iterator<String> it = faBeans.keySet().iterator(); it.hasNext();){
            
            String beanName = it.next();
            ManagedBean bean = faBeans.get(beanName);
            if(bean.containsDifferences())
                bean.printSourceFiles(writer);            
        }
        
        for(Iterator<String> it = faViews.keySet().iterator(); it.hasNext();){
            
            String viewId = it.next();
            ConfigView v = faViews.get(viewId);
            if(v.containsDifferences())
                v.printSourceFiles(writer);            
        }
    }
}

class ConfigView {
    
    String id;
    String page;
    String nodeType;
    ArrayList<String> sourceFiles;
    boolean containsDifferences;
    boolean firstDifference;
    
    public ConfigView(String id, String page, String fName, String nodeType) {
        this.id = id;
        this.page = page;
        sourceFiles = new ArrayList<String>();
        sourceFiles.add(fName);
        containsDifferences = false;
        firstDifference = true;
        this.nodeType = nodeType;
    }
    
    public boolean containsDifferences() {
        return containsDifferences;
    }
    
    public void compareView(String page, String fName, BufferedWriter w, String nodeType) {
        if(!nodeType.equals("view") || !page.equals(this.page)){
            containsDifferences = true;
            
            try{
                w.write(AdfcDuplicates.getFileNameDetails(fName) + nodeType + ": " + id + ", " + page + "\n");
                if(firstDifference)
                    w.write(AdfcDuplicates.getFileNameDetails(sourceFiles.get(0)) + this.nodeType + ": " + id + ", " + this.page + "\n");
                firstDifference = false;
            }catch(IOException e){
                e.printStackTrace();
            }
        }
        
        sourceFiles.add(fName);
    }
    
    public void printSourceFiles(BufferedWriter writer){
        
        try{
            writer.write("VIEW ID: " + id + "\n");
            for(int i =0; i < sourceFiles.size(); i++)
                writer.write(sourceFiles.get(i) + "\n");
            writer.write("\n");
            
        }catch(Exception e) {
            e.printStackTrace();
        }
        
    }
}

class ManagedBean {
    
    String name;
    String beanClass;
    String scope;
    HashSet<String> properties;
    ArrayList<String> sourceFiles;
    boolean containsDifferences;
    boolean firstDifference;
    
    public ManagedBean(String sBeanName, String sBeanClass, String sScope, HashSet<String> sBeanProperties, String fName){
        
        sourceFiles = new ArrayList<String>();
        sourceFiles.add(fName);
        name = sBeanName;
        beanClass = sBeanClass;
        scope = sScope;
        properties = sBeanProperties;
        containsDifferences = false;
        firstDifference = true;
    }
    
    public void compareBean(String sBeanClass, String sScope, HashSet<String> sBeanProperties, String fName, BufferedWriter w){
        
        if(!beanClass.equals(sBeanClass) || !scope.equals(sScope) || !compareProperties(sBeanProperties)){
            containsDifferences = true;
            
            try{
                
                w.write(AdfcDuplicates.getFileNameDetails(fName) + "BEAN: " + name + "," + sBeanClass + "," + sScope + 
                        ";" + getProperties(sBeanProperties) + "\n");
                if(firstDifference)
                    w.write(AdfcDuplicates.getFileNameDetails(sourceFiles.get(0)) + "BEAN: " + name + "," + beanClass + "," + scope + 
                            ";" + getProperties(properties) + "\n");
                firstDifference = false;
            }catch(IOException e){
                e.printStackTrace();
            }
        }        
        
        sourceFiles.add(fName);           
        
    }
    
    private String getProperties(HashSet<String> properties) {
        
        StringBuffer sb = new StringBuffer();
        for(Iterator<String> it = properties.iterator(); it.hasNext();)
            sb.append(it.next() +" ");
        return sb.toString();
    }
    
    public boolean containsDifferences() {
        return containsDifferences;
    }
    
    public void printSourceFiles(BufferedWriter writer){
        
        try{
            writer.write("BEAN: " + name + "\n");
            for(int i = 1; i < sourceFiles.size(); i++)
                writer.write(sourceFiles.get(i) + "\n");
            writer.write("\n");
            
        }catch(Exception e) {
            e.printStackTrace();
        }
        
    }
    
    // return true if the properties match
    private boolean compareProperties(HashSet<String> pr) {
        
        if(pr.size() != properties.size())
            return false;
        
        for(Iterator<String> it = pr.iterator(); it.hasNext();){
            String prop = it.next();
            if(!properties.contains(prop))
                return false;
        }
                                                                
        return true;
    }
}
