package oracle.apps.serialization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;

import java.util.Date;
import java.util.HashSet;
import java.util.StringTokenizer;

import oracle.apps.helpers.Mail;
import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import oracle.xml.parser.v2.XMLNode;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SerializationScanner {
    
  public static Writer outputFileWriter;
      
  static int pageDef_files = 0;
  static int page_files = 0;
  static int java_files = 0;
  static String exceptions ="";
  
  static boolean bDebug = false;
  HashSet<String> exemptions = new HashSet<String>(); 
  HashSet<String> actionIds = new HashSet<String>();
  
  public static void main(String[] args) throws Exception{             
   
      if(args == null || args.length < 1){
        System.out.println("Crawl directory has to be specified");
        System.exit(1);
      }
      String sCrawlDir = args[0];
     
      if (sCrawlDir == null || sCrawlDir.trim().equals("")) {
          System.out.println("Crawl directory has to be specified");
          System.exit(1);
      }
      
      boolean append = false;
      if(args.length > 1)
         append = new Boolean(args[1].trim());
      
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
      
      System.out.println("Analyzing all files in directory:" + sCrawlDir);

    SerializationScanner scanner = new SerializationScanner();
    scanner.initializeExemptions();

    try {         
        outputFileWriter = new BufferedWriter(new FileWriter("serialization_scan.csv", append) );
        if(!append)
        outputFileWriter.write("Family, Module, SubModule, Filename, LineNum, NodeName, Id, Comment1, Comment2, ScanType, Label\n");           
        scanner.crawlDirectory(sCrawlDir);       
        
        System.out.println("Number of page files (.jsff & .jspx) processed: "+ page_files);
        System.out.println("Number of pagedef files processed: "+ pageDef_files);
        //System.out.println("Number of Java files processed: "+ java_files);   
        if(!exceptions.equals("")) {
          Date runDate = new Date();
          String summary = "Crawl Directory: " + sCrawlDir + "\n";
          summary += "\n Following exceptions were recorded during the scan: \n" + exceptions; 
          //Mail.sendMail("sudipti.gupta@oracle.com", summary, "SCRIPT: Serialization Scan run on " + runDate.toString());
        }
      
    } catch (Exception e) {
        e.printStackTrace();     
        Mail.sendMail("sudipti.gupta@oracle.com", e.getMessage(), "SCRIPT: Serialization Scan on " + sCrawlDir + " failed");
    }
    finally{
      outputFileWriter.close();
    }
  }  
  
  public void initializeExemptions() {
      
    try{
      String userHomeDir = System.getProperty("user.home");
    
      String fName = userHomeDir + "/exemption_serialization.txt";
    
      BufferedReader reader = new BufferedReader(new FileReader(fName));
      String line = "";
      while((line = reader.readLine()) != null)
          exemptions.add(line.trim());
    } catch(Exception e) {
         if(bDebug) e.printStackTrace();
    }
          
    exemptions.add("fusionapps/hcm/components/hcmCore/employment/" +
        "workRelationships/ui/adfmsrc/oracle/apps/hcm/employment/" +
        "workRelationships/ui/page/AddWorkRelationshipAssignmentPageDef.xml");
    
    exemptions.add("fusionapps/hcm/components/hcmCore/people/portrait/ui/" +
       "adfmsrc/oracle/apps/hcm/people/portrait/ui/page/PortraitPageDef.xml");
    
    exemptions.add("fusionapps/hcm/components/hcmCore/predictives/core/publicUi/" +
     "adfmsrc/oracle/apps/hcm/predictives/core/publicUi/page/RetentionRiskRegionPageDef.xml");

    exemptions.add("fusionapps/hcm/components/hcmCoreSetup/common/batchLoader/ui/adfmsrc/oracle/apps/hcm/common/batchLoader/ui/page/EssScheduleRequestPageDef.xml");
        
    
  }
  
  public void processFile(File fName)
  {
    try {           
      if(isJsffJspx(fName.getName()))
          processPage(fName);
      if(isPageDef(fName.getName()))
          processPageDef(fName);
    } 
    catch (Exception e) {
        e.printStackTrace();        
        System.out.println("ERROR:  Error while processing file: " +fName);
        exceptions += "\n Exception in file: " + fName + ": " + e.getMessage();
        return;
    }
  }
  
  private void processPage(File f) throws Exception {
    
    page_files++;
    
    XMLDocument doc = XMLParserHelper.getXMLDocument(f.getAbsolutePath());  
    
    if(doc == null) {
      System.out.println("Could not parse page file: " + f.getName());
      return;
    }
    //Scan for setAction Listener
    NodeList nodes = doc.getElementsByTagName("setActionListener");      
    
    for(int i = 0; i < nodes.getLength(); i++) {
        
      Node n = nodes.item(i);
      String from = XMLParserHelper.getAttributeValue(n,"from");
      String to = XMLParserHelper.getAttributeValue(n,"to");
        
      if((from.contains("bindings.") && !from.contains("inputValue") && !from.contains("attributeValue") && !from.contains("attrValue")) ||  
          (from.contains("inputValue") && !from.contains("bindings."))) {
          
          if(from.contains(".label")) continue;
          String[] parts = from.split("\\.");
          if(parts.length >= 3) continue;
          
          outputFileWriter.write(getModuleName(f.getAbsolutePath()) + ","
                    + getFileName(f.getAbsolutePath()) + "," + ((XMLNode)n).getLineNumber() + "," + n.getNodeName() + "," + 
                    XMLParserHelper.getAttributeValue(n, "id") + ",from:" + 
                        from + ",to:" + to + ",setActionListener," + getLabel(f.getCanonicalPath()) + "\n");
         }
    }   
    
    //Scan for treeTable nodes
    
  /*  nodes = doc.getElementsByTagName("treeTable");   
    
    for(int i = 0; i < nodes.getLength(); i++) {
        
        Node treeNode = nodes.item(i);
        String var = XMLParserHelper.getAttributeValue(treeNode, "var");
        
        NodeList children = treeNode.getChildNodes();
        
        for(int j = 0; j < children.getLength(); j++) {
            
          Node child = children.item(j);
          processChildNode(f,child,var);
        }
    }*/
    
     
      /*  NodeList nodes = doc.getElementsByTagNameNS("*","*");
     
    for(int i = 0; i < nodes.getLength(); i++) {
        
      Node n = nodes.item(i);
      NamedNodeMap attributes = n.getAttributes();
      if (attributes == null) return;
      
        for(int k =0; k < attributes.getLength(); k++){
            
          Node attrNode = attributes.item(k);
          String attrValue = attrNode.getNodeValue();
          if((attrValue.contains("inputValue") && !attrValue.contains("bindings")) ||
              (attrValue.contains("bindings") && !attrValue.contains("inputValue") && !attrValue.contains("hints.") && !attrValue.contains("label")))
              
            outputFileWriter.write(getModuleName(f.getAbsolutePath()) + ","
                        + getFileName(f.getAbsolutePath()) + "," + ((XMLNode)n).getLineNumber() + "," + n.getNodeName() + "," + 
                        XMLParserHelper.getAttributeValue(n, "id") + ",attrName=" + 
                            attrNode.getNodeName() + ",attrValue=" + attrValue  + "\n");
        }
        
    }  */
  }
  
 /* private void processChildNode(File f, Node child, String var) throws Exception{
    
    NamedNodeMap attributes = child.getAttributes();
    if (attributes == null) return;
    
      for(int k =0; k < attributes.getLength(); k++){
          
        Node attrNode = attributes.item(k);
        String attrValue = attrNode.getNodeValue();
        if((attrValue.contains(var) && attrValue.contains("inputValue") && !attrValue.contains("bindings")) ||
            (attrValue.contains(var) && attrValue.contains("bindings") && !attrValue.contains("inputValue") && !attrValue.contains("attributeValue")))
            
          outputFileWriter.write(getModuleName(f.getAbsolutePath()) + ","
                      + getFileName(f.getAbsolutePath()) + "," + ((XMLNode)child).getLineNumber() + "," + child.getNodeName() + "," + 
                      XMLParserHelper.getAttributeValue(child, "id") + ",attrName:" + 
                          attrNode.getNodeName() + ",attrValue:" + attrValue + ",treeTable nodes\n");
      }
      
    NodeList children = child.getChildNodes();
    for(int i = 0; i < children.getLength(); i++)
        processChildNode(f, children.item(i), var);
  } */
  
  private void processPageDef(File f) throws Exception {
    pageDef_files++;
    XMLDocument doc = XMLParserHelper.getXMLDocument(f.getAbsolutePath()); 
    
    NodeList actions = doc.getElementsByTagName("action");
  
    for(int i = 0; i< actions.getLength(); i++) {
        String id = XMLParserHelper.getAttributeValue(actions.item(i), "id");
        actionIds.add(id);
    }
    
    //Scan for setAction Listener
    NodeList nodes = doc.getElementsByTagName("executables");       
    
    for(int i = 0; i < nodes.getLength(); i++) {
        
      Node n = nodes.item(i);
      NodeList children = n.getChildNodes();
      for(int j = 0; j < children.getLength(); j++) {
          
        Node child = children.item(j);
        String nodeName = child.getNodeName();
        if(nodeName.equals("biContent") || nodeName.equals("variable") || 
           nodeName.equals("portlet")  || nodeName.equals("invokeAction") || nodeName.equals("iterator"))
            continue;
        processExecutableChildNode(f,child);
      }
    }
  }
  
  private void processExecutableChildNode(File f, Node child) throws Exception{
    
    NamedNodeMap attributes = child.getAttributes();
    if (attributes == null) return;
    
      for(int k =0; k < attributes.getLength(); k++){
          
        Node attrNode = attributes.item(k);
        if(attrNode.getNodeName().equals("RefreshCondition") ||
            attrNode.getNodeName().equals("active")) continue;
        String attrValue = attrNode.getNodeValue();
        if(attrValue.contains("inputValue")|| attrValue.contains("bindings")) {
            
          StringTokenizer tokenizer = new StringTokenizer(attrValue," \t\n\r\f=?:{}#");
          while(tokenizer.hasMoreTokens())          
          {
            String token = tokenizer.nextToken();
            if(token.contains("bindings") && !token.contains("inputValue")  && !token.contains("readOnly")
               && !token.contains("attributeValue") && !token.contains("attrValue") && !token.contains("queryPerformed")) {
                
                 String[] parts = token.split("\\.");
                 if(parts.length >= 3) continue;
                 
                if(actionIds.contains(parts[1].trim()))
                    continue;
                 
                 if(isExempt(getFileName(f.getAbsolutePath()),child)) continue;
                
                 outputFileWriter.write(getModuleName(f.getAbsolutePath()) + ","
                          + getFileName(f.getAbsolutePath()) + "," + ((XMLNode)child).getLineNumber() + "," + child.getNodeName() + "," + 
                          XMLParserHelper.getAttributeValue(child, "id") + ",attrName:" + 
                          attrNode.getNodeName() + ",attrValue:" + token + ",pageDef executables," + getLabel(f.getCanonicalPath()) + "\n");
               }
          }
        }
      }
      
    NodeList children = child.getChildNodes();
    for(int i = 0; i < children.getLength(); i++) {      
      Node n = children.item(i);
      String nodeName = n.getNodeName();
      if(nodeName.equals("biContent") || nodeName.equals("variable") || 
         nodeName.equals("portlet")  || nodeName.equals("invokeAction") || nodeName.equals("iterator"))
          continue;
        processExecutableChildNode(f, n);
    }
  }
  
  private boolean isExempt(String fileName, Node n) {
   // String result ="";
   String result =fileName.trim();
   /* String nodeName = n.getNodeName();
    String nodeId = XMLParserHelper.getAttributeValue(n, "id");
    String taskFlowId = "";
    
    if(nodeName.equals("parameter")) {
       Node parent = n.getParentNode();
       while(parent !=null && !parent.getNodeName().equals("taskflow"))
           parent = parent.getParentNode();
       
       if(parent != null)
           taskFlowId = XMLParserHelper.getAttributeValue(parent, "id");
       
       result =  fileName.trim() + "," + nodeName + "," + nodeId + "," + taskFlowId;*/
       if(exemptions.contains(result)) 
           return true;
    //}    
    return false;
  }
  
  /*private String getTypeFromTaskFlow(File f, Node n) {
      
    String absPath = f.getAbsolutePath();
    String nodeName = n.getNodeName();
    String taskFlowId = null;
    if(nodeName.equals("taskFlow"))
        taskFlowId = XMLParserHelper.getAttributeValue(n, "taskFlowId");
    else if(nodeName.equals("parameter")){
        Node taskflowNode = n.getParentNode().getParentNode();
        taskFlowId = XMLParserHelper.getAttributeValue(taskflowNode, "taskFlowId");
    }
    
    String[] parts = taskFlowId.split("#");
    String taskFlowFile = parts[0];
    String taskFlow = null;
    if(parts.length > 1) 
        taskFlow = parts[1];
    
    
    return "";
  }*/
  
  
  private void crawlDirectory(String path) 
  {
      File folder = new File(path);
      File[] listOfFiles = folder.listFiles();
          
      if(listOfFiles == null)
          return;
      
      for(int i = 0; i <listOfFiles.length; i++)
      {
          if(listOfFiles[i].isFile()) 
          {
              String sName = listOfFiles[i].getAbsolutePath();              
              if(fileOfInterest(sName))               
                  processFile(listOfFiles[i]);                           
          }
          else if(listOfFiles[i].isDirectory()) 
              crawlDirectory(listOfFiles[i].getAbsolutePath());          
          else 
              System.out.println("ERROR:  node is neither file or directory: "+listOfFiles[i]);          
      }      
  }

  public boolean fileOfInterest(String sName) 
  {
      
    String absPathLowerCase = sName.toLowerCase();
    if (absPathLowerCase.contains("/dbschema/") || absPathLowerCase.contains("/.ade_path/")
        || absPathLowerCase.contains("test.") || absPathLowerCase.contains("/test/") || absPathLowerCase.contains("/classes/") 
        || absPathLowerCase.contains("modeltest") || absPathLowerCase.contains("/uitest") || absPathLowerCase.contains("/noship/")
        || absPathLowerCase.contains("/publicuitest")  
        || absPathLowerCase.contains("servicetest") || absPathLowerCase.contains("/testui")
        || absPathLowerCase.contains("/structuretest"))
        return false;
        
    
    if (isPageDef(sName) || isJsffJspx(sName))
      return true;
    else
      return false;
  }
  
  private boolean isPageDef(String sName) {
    if (sName.endsWith("PageDef.xml"))
      return true;
    if(sName.contains("PageDef"))
      if (sName.matches(".*PageDef[0-9]*\\.xml"))
        return true;
    return false;
  }
  
  private boolean isJsffJspx(String sName) {
    if (sName.endsWith(".jspx"))
        return true;
    if (sName.endsWith(".jsff"))
        return true;
    return false;
  }
  
   
  private String getModuleName(String filePath){
     
    if (filePath.contains("/fusionapps/")) {
        Integer fusionappsIndex = filePath.indexOf("/fusionapps/");
        String path_after_fusionapps =
            filePath.substring(fusionappsIndex);
        if (!isEmpty(path_after_fusionapps)) {
            String[] parts = path_after_fusionapps.split("/");
            if (parts != null && parts.length > 5) 
                return parts[2].toUpperCase() + "," + parts[4] + "," + parts[5].trim().toUpperCase();            
        }
    }
    if (filePath.contains("/fsm/")) {
        Integer fusionappsIndex = filePath.indexOf("/fsm/");
        String path_after_fusionapps =
            filePath.substring(fusionappsIndex);
        if (!isEmpty(path_after_fusionapps)) {
            String[] parts = path_after_fusionapps.split("/");
            if (parts != null && parts.length > 4) 
                return parts[1].toUpperCase() + "," + parts[3] + "," + parts[4].trim().toUpperCase();            
        }
    }
      if (filePath.contains("/atgpf/")) {
          Integer fusionappsIndex = filePath.indexOf("/atgpf/");
          String path_after_fusionapps =
              filePath.substring(fusionappsIndex);
          if (!isEmpty(path_after_fusionapps)) {
              String[] parts = path_after_fusionapps.split("/");
              if (parts != null && parts.length > 4) 
                  return parts[1].toUpperCase() + "," + parts[2] + "," + parts[4].trim().toUpperCase();            
          }
      }
    return ",,";
  }
  
  private boolean isEmpty(String str) {
      if (str == null || str.trim().equals("")) 
          return true;      
      return false;
  }  
  
  private String getFileName(String filePath) {
      
    int index = -1;
    if (filePath.contains("fusionapps/")) {
            index = filePath.indexOf("fusionapps/");            
    }
    if (filePath.contains("fsm/")) {
            index = filePath.indexOf("fsm/");
    }
      if (filePath.contains("atgpf/")) {
              index = filePath.indexOf("atgpf/");
      }
      
      if(index != -1)
         return filePath.substring(index);

    return filePath;
  }
  
  private String getLabel(String filePath) {
    int index = -1;
    if (filePath.contains("fusionapps/")) {
            index = filePath.indexOf("fusionapps/");            
    }
    if (filePath.contains("fsm/")) {
            index = filePath.indexOf("fsm/");
    }
      if (filePath.contains("atgpf/")) {
              index = filePath.indexOf("atgpf/");
      }
    
      if(index < 1) return "";
      String path_before_fusionapps = filePath.substring(0, index-1);
      String[] parts = path_before_fusionapps.split("/");
      if(parts == null || parts.length < 2)
          return "";
      String label = parts[parts.length -2] + "_" + parts[parts.length -1];      
      label = label.replace(".rdd","");   
      return label;
  }
}
