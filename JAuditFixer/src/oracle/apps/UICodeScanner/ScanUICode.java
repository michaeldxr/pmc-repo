package oracle.apps.UICodeScanner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.JoesBaseClass;
import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ScanUICode extends JoesBaseClass {
    
    int m_page_reviewCount = 0;
    String m_sSummaryReport = "";
 
    static Writer outputFileWriter;
    BufferedReader fileReader;
    
    Pattern padding = Pattern.compile("(padding\\s*:.*;+?)");
    Pattern width = Pattern.compile("width\\s*:\\s*([0-9]*\\.?[0-9]*)%");
    Pattern paddingValue = Pattern.compile("padding\\s*:\\s*([0-9]*\\.?[0-9]*)(em|px)");
    Pattern height = Pattern.compile("([0-9]*\\.?[0-9]*)(em|px)?");
    
    HashMap<String,HashSet> exemptions = new HashMap<String,HashSet>();
      
    public ScanUICode() {
        super(JoesBaseClass.CRAWL_TYPE.JSPX_AND_JSFF);
    }

    public static void main(String[] args) {               
     
      String sCrawlDir = args[0];
    
      if (sCrawlDir == null || sCrawlDir.trim().equals("")) {
          System.out.println("Crawl directory has to be specified");
          System.exit(1);
      }
      
      File f = new File(sCrawlDir.trim());
      if(!f.exists()){
          System.out.println("Crawl directory does not exist - " + sCrawlDir);
          System.exit(1);
      }
      
       /*if(sCrawlDir.contains("/LATEST")) {
           int ind = sCrawlDir.indexOf("/LATEST");
           String pathBeforeLatest = sCrawlDir.substring(0,ind);
           File series = new File(pathBeforeLatest);
           File[] labels = series.listFiles();
           
           long lastModified = 0;
           String latestLabel = "";
           for(int i = 0; i < labels.length; i++) {
               if(labels[i].lastModified() > lastModified) {
                   lastModified = labels[i].lastModified();
                   latestLabel = labels[i].getName();
               }
           }
           if(!latestLabel.equals(""))
               sCrawlDir = sCrawlDir.replace("LATEST", latestLabel);
       }*/
      
      boolean append = false;
      if(args.length > 1)
          append = new Boolean(args[1].trim());
      System.out.println("Analyzing all files in directory:" + sCrawlDir);

      ScanUICode scanner = new ScanUICode();

      try {         
          scanner.generateExemptionList();
          outputFileWriter = new BufferedWriter(new FileWriter("ui_scan.csv",append) );
          if(!append)
            outputFileWriter.write("Family, Module, Product, Filename, Label, IssueType, NodeName, NodeId, Description\n");           
          scanner.crawlDirectory(sCrawlDir, false);
          outputFileWriter.close();
        
      } catch (Exception e) {
          e.printStackTrace();
          scanner.logException(sCrawlDir, e);
      }
      scanner.logUsageReport(sCrawlDir, true);
   }
    
    
 protected void processFile(File fName, boolean bDoADE) 
  {
    String fileName = fName.getPath();
    
    if(fileName.endsWith("index.jspx"))
        return;

    if(fileName.endsWith(".jsff") || fileName.endsWith(".jspx")) {
        processPage(fName, bDoADE);
    }
  }
  
  protected String getSummaryReport() {
    m_sSummaryReport = "For this run I reviewed a total of " +m_page_reviewCount +" pages.\n" + m_sSummaryReport;
    return m_sSummaryReport;
  }
  
  protected String getSummaryReportSubject() {
    return "UI Code Scan";
  }
  
  public void processPage(File f, boolean bDoADE) 
  {
    m_page_reviewCount++;

    //System.out.println("Checking Page:  " +f);

      try {          

          XMLDocument doc = XMLParserHelper.getXMLDocument(f.getAbsolutePath());
          NodeList adfc = doc.getElementsByTagName("jsp:root");
          if (adfc == null) 
          {
              System.out.println("REVIEW:  Not a page file ---> " + f);
              return;
          }
        // findHeaderEmptyText(doc, f, "panelHeader");    
         //findHeaderEmptyText(doc, f, "showDetailHeader");    
        // analyzePaddings(doc, f, "panelFormLayout");   
         analyzePaddings(doc, f, "panelHeader");
         analyzePaddings(doc, f, "showDetailHeader");
         //findInlineStyleWidthOver90(doc,f);
         findHeadersContainingApplicationsTableAndNoContentPadding(doc,f, "panelHeader");
         findHeadersContainingApplicationsTableAndNoContentPadding(doc,f, "showDetailHeader");
         //findPaddingsMoreThan5px(doc,f);
         findElementsNotRendered(doc,f);
         //findButtonTypeMismatches(doc, f);
         // findSpacersMoreThan5(doc,f);
         findElementsNotVisible(doc,f);
         
      } 
      catch (Exception e) {
          e.printStackTrace();
          logException(f.getName(), e);
          System.out.println("ERROR:  Error while processing file.  " +
              "review carefully if the script updated it. " +f);
          return;
      }
  }
  
  protected ArrayList getNodesInListWithMatchingAttribute(NodeList nodelist,
                                                         String attribute,
                                                         String attrValue) {
      ArrayList result = new ArrayList();
      if (nodelist == null || nodelist.getLength() <= 0) {
          return result;
      }

      int len = nodelist.getLength();
      for (int i = 0; i < len; i++) {
          Node child = nodelist.item(i);
          String idVal = XMLParserHelper.getAttributeValue(child, attribute);
          if (idVal == null || idVal.trim().equals(attrValue)) {
              result.add(child);
          }
      }
      return result;
  }
  
  protected ArrayList getNodesInListWithMatchingAttributeNotNull(NodeList nodelist,
                                                         String attribute,
                                                         String attrValue) {
      ArrayList result = new ArrayList();
      if (nodelist == null || nodelist.getLength() <= 0) {
          return result;
      }

      int len = nodelist.getLength();
      for (int i = 0; i < len; i++) {
          Node child = nodelist.item(i);
          String idVal = XMLParserHelper.getAttributeValue(child, attribute);
          if (idVal!=null && idVal.trim().equals(attrValue)) {
              result.add(child);
          }
      }
      return result;
  }
  
  private boolean isEmpty(String str) {
      if (str == null || str.trim().equals("")) {
          return true;
      }
      return false;
  }
  
  private String findPadding(Node n){
    String style = XMLParserHelper.getAttributeValue(n, "inlineStyle");
    String result = "";
    Matcher m;
    if(style != null) {
        m = padding.matcher(style);
        if(m.find()) 
            result += m.group() + "(inlineStyle)" ;
    }
    
    style = XMLParserHelper.getAttributeValue(n, "contentStyle");
    if(style != null) {
        m = padding.matcher(style);
        if(m.find()) 
            result += m.group() + "(contentStyle)";
    }
    return result;
  }
  
  /**
     * Find all <af:panelHeader> elements whose "text" attribute is missing or empty string
     * @param doc
     * @param f
     * @throws IOException
     */
  private void findHeaderEmptyText(XMLDocument doc, File f, String tagName) throws IOException {
    
    NodeList list = doc.getElementsByTagName(tagName);

    ArrayList result = getNodesInListWithMatchingAttribute(list,"text","");
    
    for(int i = 0; i < result.size(); i++) {
        Node n = (Node)result.get(i);
        String rendered = "rendered=" + XMLParserHelper.getAttributeValue(n, "rendered");
        if(rendered != null && rendered.equals("false")) continue;
        if(isExempt(XMLParserHelper.getAttributeValue(n, "id"), getFileName(f.getAbsolutePath()), "Empty PanelHeader Text"))
            continue;
        outputFileWriter.write(getFileNameInfo(f.getAbsolutePath()) + "Empty PanelHeader Text," + n.getNodeName() + "," + 
                            XMLParserHelper.getAttributeValue(n, "id") + ",PanelHeader text attribute is empty\n");
    }
  }
  
  /**
     * Find all panelFormLayout elements and analyze the paddings
     * @param doc
     * @param f
     * @throws IOException
     */
  private void analyzePaddings(XMLDocument doc, File f, String elementName) throws IOException {
   
     NodeList list = doc.getElementsByTagName(elementName);    
        
     for (int i = 0; i < list.getLength(); i++) {
       Node child = list.item(i);           
       String inlinePadding = XMLParserHelper.getAttributeValue(child, "inlineStyle");         
       String rendered = "rendered=" + XMLParserHelper.getAttributeValue(child, "rendered");
       if(rendered != null && rendered.equals("false")) continue;
         
        if(isExempt(XMLParserHelper.getAttributeValue(child, "id"), getFileName(f.getAbsolutePath()), "Padding Issue"))
            continue;
         
       if(inlinePadding != null && inlinePadding.contains("padding:"))
          outputFileWriter.write(getFileNameInfo(f.getAbsolutePath()) + "Padding Issue," + child.getNodeName() + "," + 
                             XMLParserHelper.getAttributeValue(child, "id") + ",Header contains inlineStyle padding\n");
    }
  }
  
  /**
  * Find all elements whose width is > 90%
  * @param doc
  * @param f
  * @throws IOException
  */
  private void findInlineStyleWidthOver90(XMLDocument doc, File f) throws IOException {
  
    NodeList list = doc.getElementsByTagNameNS("*","*");
    Matcher m;
    Node n;
    Float widthValue;
  
    for (int i = 0; i < list.getLength(); i++) {
        widthValue = 0F;
      n = list.item(i); 
      String style = XMLParserHelper.getAttributeValue(n, "inlineStyle");    
      String styleClass = XMLParserHelper.getAttributeValue(n, "styleClass");  
      if(style != null) {
          m = width.matcher(style);
          if(m.find()) {
              widthValue= new Float(m.group(1));
              if(widthValue >= 90)  
                outputFileWriter.write(getFileNameInfo(f.getAbsolutePath()) + n.getNodeName() + "," + 
                                  XMLParserHelper.getAttributeValue(n, "id") + "," + 
                                       "width=" + widthValue + "% , " + "," + ((styleClass!=null)?styleClass:"") + "\n");
          }
      }
    }//end of for
    
  } //end of method
  
  private void findHeadersContainingApplicationsTableAndNoContentPadding(XMLDocument doc, File f, String tagname) throws IOException {
      
    NodeList list = doc.getElementsByTagName(tagname);    
    Node n = null; 
    Node child = null;
    String contentStyle = null;

    for (int i = 0; i < list.getLength(); i++) {
        n = list.item(i);
        child = XMLParserHelper.getChildNodeWithName(n, "fnd:applicationsTable");
        if (child != null)  {
            contentStyle = XMLParserHelper.getAttributeValue(n, "contentStyle");
            String rendered = "rendered=" + XMLParserHelper.getAttributeValue(n, "rendered");
            if(rendered != null && rendered.equals("false")) continue;
            
            if(contentStyle == null || !contentStyle.contains("padding"))
               outputFileWriter.write(getFileNameInfo(f.getAbsolutePath()) + "Padding Issue," + n.getNodeName() + "," + 
                            XMLParserHelper.getAttributeValue(n, "id") + "," +                             
                            "Header contains applicationsTable but no contentStyle padding\n");
        }
    }
  }
  
  /**
  * Find all elements whose padding is > 5px or 0.313 em
  * @param doc
  * @param f
  * @throws IOException
  */
  private void findPaddingsMoreThan5px(XMLDocument doc, File f) throws IOException {
  
    NodeList list = doc.getElementsByTagNameNS("*","*");
    Matcher m;
    Node n;
    Float padding;
    String paddingUnit;
    boolean paddingMoreThan5px;
    String style;
  
    for (int i = 0; i < list.getLength(); i++) {
        
      padding = 0F;
      n = list.item(i); 
      paddingMoreThan5px = false;
        
      style = XMLParserHelper.getAttributeValue(n, "inlineStyle");  
      if(style == null)
        style = XMLParserHelper.getAttributeValue(n, "contentStyle");      
  
      if(style != null) {
          m = paddingValue.matcher(style);
          if(m.find()) {
              padding= new Float(m.group(1));
              paddingUnit = m.group(2);
              if(paddingUnit.equals("px"))
                  paddingMoreThan5px = (padding > 5);
              else
                  paddingMoreThan5px = (padding > 0.32);
              if(paddingMoreThan5px)  
                outputFileWriter.write(getFileNameInfo(f.getAbsolutePath()) + n.getNodeName() + "," + 
                                  XMLParserHelper.getAttributeValue(n, "id") + "," + 
                                       "padding=" + padding + paddingUnit + "\n");
          }
      }
    }//end of for
    
  } //end of method
  
  private void findButtonTypeMismatches(XMLDocument doc, File f) throws IOException {
    NodeList list = doc.getElementsByTagName("commandToolbarButton");
    HashSet<String> parents = new HashSet<String>();
    for(int i = 0; i< list.getLength(); i++) {
        
        Node n = list.item(i);
        Node parent = n.getParentNode();
        if(parents.contains(XMLParserHelper.getAttributeValue(parent, "id"))) continue;
        NodeList siblings = parent.getChildNodes();   
        parents.add(XMLParserHelper.getAttributeValue(parent, "id"));
        
        for(int j = 0; j < siblings.getLength(); j++) {
            
          Node sib = siblings.item(j);          
            if(sib.getNodeName().equals("af:commandButton")) {
                
            if(isExempt(XMLParserHelper.getAttributeValue(sib, "id"), getFileName(f.getAbsolutePath()), "Button Type Mismatch"))
                continue;
                
            outputFileWriter.write(getFileNameInfo(f.getAbsolutePath()) + "Button Type Mismatch," + sib.getNodeName() + "," + 
                              XMLParserHelper.getAttributeValue(sib, "id") + ",Some siblings are of type commandToolbarButton\n");
            }
        }
    }
  }
  
  private void findSpacersMoreThan5(XMLDocument doc, File f) throws IOException {
      
      NodeList list =  doc.getElementsByTagName("spacer");
      float h1 = 0, w1 = 0;
      Matcher m1 =null,m2 =null;
      
      for(int i = 0; i < list.getLength(); i++) {
          
        boolean heightMoreThan5 = false;
        boolean widthMoreThan5 = false;
        boolean spacerMatters = true;
          
        Node n = list.item(i);
        String h = XMLParserHelper.getAttributeValue(n, "height");
        String w = XMLParserHelper.getAttributeValue(n, "width");
          
        if(h != null) m1 = height.matcher(h);
        if(w != null) m2 = height.matcher(w);
          
        if(m1 != null && m1.find()){
          if(!m1.group(1).trim().equals("")) h1 = new Float(m1.group(1));
          if(m1.group(2) != null && m1.group(2).equals("em") && h1 > 0.315)
            heightMoreThan5 = true;
          else if(h1 > 5)  heightMoreThan5 = true;
        }
          
        if(m2 != null && m2.find()){
          if(!m2.group(1).trim().equals("")) w1 = new Float(m2.group(1));
          if(m2.group(2) != null && m2.group(2).equals("em") && w1 > 0.315)
            widthMoreThan5 = true;
          else if(w1 > 5)  widthMoreThan5 = true;
        }
          
        if(heightMoreThan5 || widthMoreThan5) {
            
          boolean rendered = true;
          Node p = n.getParentNode();
          String layout = XMLParserHelper.getAttributeValue(p, "layout");
          String render1 = XMLParserHelper.getAttributeValue(n, "rendered");
          String render2 = XMLParserHelper.getAttributeValue(p, "rendered");
          
          if((render1 != null && render1.trim().equals("false")) || 
             (render2 != null && render2.trim().equals("false")))
              rendered = false;
          
          if(heightMoreThan5 && layout != null && layout.trim().equals("horizontal"))
            spacerMatters = false;
          
          if(widthMoreThan5 && layout != null && layout.trim().equals("vertical"))
            spacerMatters = false;
          
          if(spacerMatters)          
                  outputFileWriter.write(getFileNameInfo(f.getAbsolutePath()) + n.getNodeName() + "," + 
                            XMLParserHelper.getAttributeValue(n, "id") + "," + 
                                "spacer with height/width > 5px, height = " + 
                                 h + ", width = " + w + "," + (rendered ? "rendered":"not rendered")
                                 + "," + p.getNodeName()+ "," + "" + "\n");
        }
      }// end of for    
  }
  
 
  
  private void findElementsNotRendered(XMLDocument doc, File f) throws IOException {
    NodeList list = doc.getElementsByTagNameNS("*","*");
    ArrayList result = getNodesInListWithMatchingAttributeNotNull(list,"rendered","false");
    Node n;
    int num_children=0;
    for(int i = 0; i < result.size(); i++) {
        n = (Node)result.get(i); 
        if(n.getNodeName().contains("dvt:"))
            continue;
        num_children = countChildren(n) + 1;  
        String binding =  XMLParserHelper.getAttributeValue(n, "binding");
        if(binding == null){
            if(isExempt(XMLParserHelper.getAttributeValue(n, "id"), getFileName(f.getAbsolutePath()), "rendered=false"))
                continue;
            outputFileWriter.write(getFileNameInfo(f.getAbsolutePath()) + "rendered=false," + n.getNodeName() + "," + 
                    XMLParserHelper.getAttributeValue(n, "id") + ",# Nodes that won't render: " + num_children + "\n");
        }
    }
  }
  
  private void findElementsNotVisible(XMLDocument doc, File f) throws IOException {
    NodeList list = doc.getElementsByTagNameNS("*","*");
    ArrayList result = getNodesInListWithMatchingAttributeNotNull(list,"visible","false");
    Node n; 
    int num_children=0;
    for(int i = 0; i < result.size(); i++) {
        n = (Node)result.get(i);
        if(n.getNodeName().contains("dvt:"))
            continue;
        num_children = countChildren(n) + 1;        
        String binding =  XMLParserHelper.getAttributeValue(n, "binding");
        String componentName = n.getNodeName();
        if(componentName.equals("af:column"))
            continue;
        if(binding == null){
            if(isExempt(XMLParserHelper.getAttributeValue(n, "id"), getFileName(f.getAbsolutePath()), "visible=false"))
                continue;
            outputFileWriter.write(getFileNameInfo(f.getAbsolutePath()) + "visible=false," + n.getNodeName() + "," + 
                  XMLParserHelper.getAttributeValue(n, "id") + ",# Nodes that won't be visible: " + num_children + "\n");
        }
    }
  }
  
  
  private int countChildren(Node n){
    int num_children = 0;
    NodeList children = n.getChildNodes();   
    
    for(int j =0; j< children.getLength(); j++) {
      Node child = children.item(j);
        if(child.getNodeType() == Node.ELEMENT_NODE) { 
            num_children++;
            num_children += countChildren(child);
        }
    }
    return num_children;
 }
  
  private String getFileNameInfo(String absPath) throws IOException{
      //System.out.println("filennameInfo: " + absPath);
      File f = new File(absPath);
      absPath = f.getCanonicalPath();
      int i = 0;
      String family = "";
      String blankString = "NA,NA,NA,NA,NA,";
      if (isEmpty(absPath))
          return blankString;
      if (absPath.startsWith("/ade/"))
          return blankString;

      if (absPath.contains("fusionapps/")) {
          i = absPath.indexOf("fusionapps/");
      }
      if (absPath.contains("fsm/")) {
          i = absPath.indexOf("fsm/");
          family = "FSM";
      }
      if (absPath.contains("atgpf/")) {
          i = absPath.indexOf("atgpf/");
          family = "ATGPF";
      }
      String pathAfterFusionApps = absPath.substring(i);
      String[] parts = pathAfterFusionApps.split("/");
      String module = "";
      String product = "";

      if (parts == null)
          return blankString;

      if (absPath.contains("fusionapps/")) {
          int partsLength = parts.length;
          if (partsLength > 1)
              family = parts[1].trim().toUpperCase();
          if (partsLength > 3)
              module = parts[3].trim();
          if (partsLength > 4)
              product = parts[4].trim().toUpperCase();

      }
      if (absPath.contains("fsm/")) {
          int partsLength = parts.length;
          if (partsLength > 2)
              module = parts[2].trim();
          if (partsLength > 3)
              product = parts[3].trim().toUpperCase();
      }
      if (absPath.contains("atgpf/")) {
          int partsLength = parts.length;
          if (partsLength > 1)
              module = parts[1].trim();
          if (partsLength > 3)
              product = parts[3].trim().toUpperCase();
      }

      if (i < 1)
          return family + "," + module + "," + product + "," + pathAfterFusionApps + ",,";

      String path_before_fusionapps = absPath.substring(0, i - 1);
      parts = path_before_fusionapps.split("/");
      if (parts == null || parts.length < 2)
          return blankString;
      
      String label = parts[parts.length -2] + "_" + parts[parts.length -1];      
      label = label.replace(".rdd","");   

      return family + "," + module + "," + product + "," + pathAfterFusionApps + "," + label + ",";
  }
  
    public void generateExemptionList() {
        
    try{
      BufferedReader fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") + 
                                "/fatools/opensource/jauditFixScripts/txt/angrybirdCodescanExemptions.txt"));
      String line = null;
      
      while((line = fileReader.readLine()) != null) {
        String[] parts = line.split(",");
        if(parts.length < 3)
            continue;
        String issue = parts[2].trim();
        if(!issue.equals("rendered=false") && !issue.equals("visible=false") 
           && !issue.equals("Button Type Mismatch") && !issue.equals("Empty PanelHeader Text"))
            continue;
        String fileName = parts[0].trim();
        String id = parts[1].trim();
          
        HashSet<String> exemptLines = null;
        if(!exemptions.containsKey(fileName)) 
          exemptLines = new HashSet<String>();
        else 
          exemptLines = exemptions.get(fileName);
        
        exemptLines.add(id + "," + issue);
        exemptions.put(fileName, exemptLines);
      }
    }catch(Exception e) {
        e.printStackTrace();
    }
    }
    
    private boolean isExempt(String id, String fileName, String issue){
        
        if(!exemptions.containsKey(fileName)) 
            return false;
        
        HashSet<String> lines = (HashSet<String>)exemptions.get(fileName);
        if(lines.contains(id + "," + issue))
            return true;
        
        return false;
    }
    
    private String getFileName(String absPath) {
        
        int i = 0;
        if(absPath.contains("fusionapps/")) 
         i = absPath.indexOf("fusionapps/");
        
        if(absPath.contains("fsm/"))
         i = absPath.indexOf("fsm/");
        
        if(absPath.contains("atgpf/"))
         i = absPath.indexOf("atgpf/");
        
        String pathAfterFusionApps = absPath.substring(i);
        
        return pathAfterFusionApps;
    }
}
