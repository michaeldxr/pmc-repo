package oracle.apps.sqlanalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;

import oracle.apps.utility.JoesBaseClass;
import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class AutoExecuteVCScanLRG extends JoesBaseClass{
    
    private static BufferedWriter writer;
    private static HashMap<String,HashSet> exemptions = new HashMap<String,HashSet>();
    private static final String issuetype = "AutoExecuteVC";
    
    private HashMap<String,String> bindVarDefaults = new HashMap<String,String>(); //for each VO, store bind variable -> default value
    
    public AutoExecuteVCScanLRG(){
      super(JoesBaseClass.CRAWL_TYPE.VO_XML);
    }
    
    public static void main(String[] args){
        
        try {
            if (args.length != 1 || args[0] == null || args[0].equals("")) {
                System.out.println("Series has to be specified for " + issuetype);
                System.exit(1);
            }

            String series = args[0];
          //String series = "FSM_MAIN_GENERIC";
      
            String label = LRGUtil.getLatestLabel(series);
            System.out.println("Running " + issuetype + " Scan on series: " + series + ".......");
            System.out.println("Latest label is: " + label);

            writer = new BufferedWriter(new FileWriter("AutoExecuteVC_LRG.csv"));
            writer.write("Family,Module,Product,Filename,Series,Label,ViewCriteria, Description\n"); 

            generateExemptionList();

            ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
          String serverTop = FamilyModuleHelper.getLabelServerTop1(series);
          String viewRoot = serverTop + "/"+series+".rdd/"+label + "/";
          AutoExecuteVCScanLRG scanner = new AutoExecuteVCScanLRG();
          
            for (int j = 0; j < families.size(); j++) {
              
                String family = families.get(j);
              String sCrawlDir = LRGUtil.getCrawlDirNew(series, family, issuetype);     
              System.out.println("Crawling dir '" + viewRoot + sCrawlDir + "' for family: " + family);
              scanner.crawlDirectoryNew(viewRoot,sCrawlDir,false);           
                System.out.println("Done crawling for family "+family+".");
            }
            writer.close();
            
            LRGUtil.addLabelInfoToAB(issuetype, series, label);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    protected void processFile(File f, boolean doADE) {    
    
        try{
        XMLDocument doc = XMLParserHelper.getXMLDocument(f.getAbsolutePath());
        
      if(doc == null) {
        System.out.println("Could not parse XMLDocument..Skipping");
        return;
      }
      
        NodeList viewObjects = doc.getElementsByTagName("ViewObject");
        if(viewObjects == null || viewObjects.getLength() != 1) 
        {
         //System.out.println("WARNING:  Unexpected number of view objects found");
         // System.out.println("Skipping...");
          return;
        }
        
        bindVarDefaults = new HashMap<String,String>(); 
        
        NodeList bindVariables = doc.getElementsByTagName("Variable");
        for(int i = 0; i < bindVariables.getLength(); i++) {
            Node bind = bindVariables.item(i);
            String defaultValue = XMLParserHelper.getAttributeValue(bind, "DefaultValue");
          
          if(defaultValue == null){
              Node transientExpression = XMLParserHelper.getChildNodeWithName(bind, "TransientExpression");
              if(transientExpression != null)
                  defaultValue=transientExpression.getTextContent();
          }
            bindVarDefaults.put(XMLParserHelper.getAttributeValue(bind, "Name"), defaultValue);
        }
        NodeList viewCriteria = doc.getElementsByTagName("ViewCriteria");
        
        for(int i =0; i < viewCriteria.getLength(); i++) {
            
            boolean atleastOneSelReqdVCHasDefaultVal = false;
            boolean selReqdVCIFound = false;
            
            Node vc = viewCriteria.item(i);
            String vcName = XMLParserHelper.getAttributeValue(vc, "Name");
            String runAutomatically = "false";
            
            Node properties = XMLParserHelper.getChildNodeWithName(vc, "Properties");
            if(properties != null) {
                Node customProperties = XMLParserHelper.getChildNodeWithName(properties, "CustomProperties");
                if(customProperties !=null) {
                    Node autoExecute = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(customProperties, "Property", "Name", "autoExecute");
                    if(autoExecute != null){
                        runAutomatically = XMLParserHelper.getAttributeValue(autoExecute, "Value");
                    }
                }
            }
            
            if(runAutomatically.equals("false"))
                continue; 
            
            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();
            xpath.setNamespaceContext(new MyNamespaceContext());

            String expr = "//bc4j:ViewCriteria[@Name='" + vcName + "']//bc4j:ViewCriteriaItem";
            XPathExpression xExpr = xpath.compile(expr);
            NodeList vcis = (NodeList)xExpr.evaluate(doc,XPathConstants.NODESET);
            
            for(int j = 0; j < vcis.getLength(); j++) {
                Node vci = vcis.item(j);
                String reqd = XMLParserHelper.getAttributeValue(vci, "Required");
                if(reqd!=null && reqd.contains("Required")) { //reqd or sel reqd item
                    String value = XMLParserHelper.getAttributeValue(vci, "Value");
                    
                    if(value == null) {
                        Node vciValue = XMLParserHelper.getChildNodeWithName(vci, "ViewCriteriaItemValue");
                        value = XMLParserHelper.getAttributeValue(vciValue, "Value");
                    }
                    
                    String bind = null;
                    if(value != null) {
                       if(!value.contains(":"))
                           bind="hardcoded";
                       else{
                           value = value.replace(":", "");
                           bind = bindVarDefaults.get(value);
                       }
                    }
                    if(bind == null && reqd.equals("Required"))
                        if(!isExempt(ViewCriteriaHelper.getFileName(f.getAbsolutePath()),vcName))
                            writer.write(ViewCriteriaHelper.getFileNameInfo1(f.getAbsolutePath()) + vcName + "," 
                                        + XMLParserHelper.getAttributeValue(vci, "Name") + ": " + "Reqd VCIs for ViewCriteria that are Automaticaly run must have default values\n");
                    if(bind !=null && reqd.equals("SelectivelyRequired"))
                        atleastOneSelReqdVCHasDefaultVal = true;
                    if(reqd.equals("SelectivelyRequired"))
                        selReqdVCIFound = true;
                }               
            }
            if(!atleastOneSelReqdVCHasDefaultVal && selReqdVCIFound) {
                if(!isExempt(ViewCriteriaHelper.getFileName(f.getAbsolutePath()),vcName))
                    writer.write(ViewCriteriaHelper.getFileNameInfo1(f.getAbsolutePath()) + vcName + "," 
                                + "At least one selectively Reqd VCI for ViewCriteria that are Automaticaly run must have a default value.\n");
            }
        }
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("Skipping: " + f.getAbsolutePath());
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
        if(!issue.equals("AutoRunVC"))
            continue;
        String fileName = parts[0].trim();
        String vcName = parts[1].trim(); //ViewCriteria name
          
        HashSet<String> exemptLines = null;
        if(!exemptions.containsKey(fileName)) 
          exemptLines = new HashSet<String>();
        else 
          exemptLines = exemptions.get(fileName);
        
        exemptLines.add(vcName);
        exemptions.put(fileName, exemptLines);
      }
    }catch(Exception e) {
        e.printStackTrace();
    }
    }
    
    private boolean isExempt(String fileName, String viewCriteria){
        
        if(!exemptions.containsKey(fileName)) 
            return false;
        
        HashSet<String> lines = (HashSet<String>)exemptions.get(fileName);
        if(lines.contains(viewCriteria))
            return true;
        
        return false;
    }

  protected String getSummaryReport() {
    return null;
  }

  protected String getSummaryReportSubject() {
    return null;
  }
}

