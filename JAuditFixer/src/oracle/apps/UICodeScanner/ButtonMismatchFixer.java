package oracle.apps.UICodeScanner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.io.Writer;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;

import oracle.apps.helpers.XmlFixer;

import oracle.apps.utility.JoesBaseClass;

import oracle.xml.parser.v2.XMLDocument;

import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ButtonMismatchFixer extends JoesBaseClass{
    
  static AdeDiffHelper diffHelper = new AdeDiffHelper("diff.txt");
  static Writer outputFileWriter; 
  static boolean bDebug = true;
  int m_page_reviewCount = 0;
  int m_button_mismatches = 0;
  float time_taken = 0;
  String m_sSummaryReport = "";
  
  HashMap<String,HashSet> exemptions = new HashMap<String,HashSet>();
  
  public ButtonMismatchFixer() {
    super(JoesBaseClass.CRAWL_TYPE.JSPX_AND_JSFF);
  }
   
  public static void main(String[] args) throws Exception {
      ButtonMismatchFixer buttonMismatchFixer = new ButtonMismatchFixer();

      // Make sure it is run with correct number of arguments
      if (isEmpty(args[0])) {          
          System.out.println("Crawl Directory should not be empty");
          System.exit(1);
      }
      
      
      boolean makeFix = true;
      if(args.length > 1)
          makeFix = new Boolean(args[1].trim());
      
      String sCrawlDir = args[0];
      diffHelper.startDiffLog();     
      
    try {         
        buttonMismatchFixer.generateExemptionList();
        outputFileWriter = new BufferedWriter(new FileWriter("button_mismatches.csv") );
        outputFileWriter.write("Module, SubModule, filename, filePath, NodeName, Id, Comment, inValidAttributes?, partialSubmit\n");          
        System.out.println("Finding button mismatches in crawl directory: " + sCrawlDir);
        buttonMismatchFixer.crawlDirectory(sCrawlDir, false);
        outputFileWriter.close();
        if(makeFix)
          buttonMismatchFixer.fixButtonMismatches();
      
    } catch (Exception e) {
        e.printStackTrace();
        buttonMismatchFixer.logException(sCrawlDir, e);
    }
      diffHelper.closeDiffLog();     

  }
  
  public static boolean isEmpty(String str) {
      if (str == null || str.trim().equals(""))
          return true;
      return false;
  }
  
  public void fixButtonMismatches() throws Exception {
    
    System.out.println("Fixing Button Mismatches...");
      
    BufferedReader fileReader = new BufferedReader(new FileReader("button_mismatches.csv"));
    String line = fileReader.readLine(); //first line is the header - read & ignore
    String prevFilePath = null;
    XMLDocument doc = null;
    XmlFixer xmlFixer = null;
    String[] fileContents = null;
    String fileContentsAsString = null;
    
    while((line = fileReader.readLine()) != null) {
      
      if(bDebug)
        System.out.println("line from csv File:" + line);
        
      String[] parts = line.split(",");
      String filePath = parts[3].trim();
      
      if(bDebug)
        System.out.println("filePath:" + filePath);
        
      if(!filePath.equals(prevFilePath)) {
        
          if(prevFilePath != null) {
            fileContentsAsString = xmlFixer.getFileContentsAsString(fileContents, false);        
            applyFix(prevFilePath,fileContentsAsString);
          }
        
        doc = XMLParserHelper.getXMLDocument(filePath);        
        xmlFixer = new XmlFixer(filePath);
        fileContents = xmlFixer.convertFileToStringArray();
      }
      prevFilePath = filePath;
        
      Node n = findElement(doc,parts[5].trim());
      int lineNum = xmlFixer.getLineNumberInSourceFile(n);
      int lineNumEndTag = xmlFixer.getEndTagLineNumber(n);
        
      if(XMLParserHelper.getAttributeValue(n,"blocking") != null 
         || XMLParserHelper.getAttributeValue(n,"iconPosition") != null) 
          continue; //Since <af:commandToolbarButton> does not support these attributes, skip the change
        
        //change start tag
        for(int i=lineNum-1; i >= 0; i--){
            if(fileContents[i].contains("af:commandButton")) {
              fileContents[i] = fileContents[i].replace("af:commandButton", "af:commandToolbarButton");
              break;
            }
        }
        //change end tag
        if(fileContents[lineNumEndTag-1].contains("af:commandButton"))
          fileContents[lineNumEndTag-1] = fileContents[lineNumEndTag-1].replace("af:commandButton", "af:commandToolbarButton");
          
        //add attribute value if not present
          if(XMLParserHelper.getAttributeValue(n,"partialSubmit") == null) {
           xmlFixer.setFileContents(fileContents);
           xmlFixer.modifyAttribute(n, "partialSubmit", null, "false", true);
           fileContents = xmlFixer.getFileContents();
          }
      
    } // end of csv file
    
    //apply fix to the last file 
    if(xmlFixer != null) {
      fileContentsAsString = xmlFixer.getFileContentsAsString(fileContents, false);        
      applyFix(prevFilePath,fileContentsAsString);
    }
    
  }
  
  private Node findElement(XMLDocument doc, String id){
    NodeList list = doc.getElementsByTagName("commandButton");
    for(int i = 0; i< list.getLength(); i++) {
        Node n = list.item(i);
        if(XMLParserHelper.getAttributeValue(n, "id").equals(id)) return n;
    }
    return null;
  }
  
   public void applyFix(String absPath,
                       String newFileContents) throws Exception {
      if(bDebug)
          System.out.println("applying Fix to file: "+ absPath);
      File file = new File(absPath);
      if (!file.exists()) {
          throw new Exception("while making fix, file not found: " +
                              absPath);
      }
      diffHelper.checkoutAndDelete(file);

      FileWriter fw = new FileWriter(file.getAbsolutePath());
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(newFileContents);
      if (bw != null)
          bw.close();
      if (fw != null)
          fw.close();
      diffHelper.checkinAndDiff(file);
  }

  protected void processFile(File fName, boolean bDoADE) 
   {
     String fileName = fName.getPath();

     if(fileName.endsWith(".jsff") || fileName.endsWith(".jspx")) {
        // m_pageList.add(fileName);    
         processPage(fName, bDoADE);
     }

   }
   
   protected String getSummaryReport() {
  
     m_sSummaryReport += "For this run I reviewed a total of " +m_page_reviewCount +" pages.\n";
     m_sSummaryReport += "Total # of button type mismatches found:" + m_button_mismatches + "\n";
     
     return m_sSummaryReport;
   }
   
   protected String getSummaryReportSubject() {
     return "UI Code Scan";
   }
   
   protected void processPage(File f, boolean bDoADE) 
   {
     m_page_reviewCount++;

       try {          

           XMLDocument doc = XMLParserHelper.getXMLDocument(f.getAbsolutePath());
           NodeList adfc = doc.getElementsByTagName("jsp:root");
           if (adfc == null) 
           {
               if(bDebug) System.out.println("REVIEW:  Not a page file ---> " + f);
               return;
           }
          findButtonTypeMismatches(doc, f);          
      } 
      catch (Exception e) {
          if(bDebug)
          e.printStackTrace();
          logException(f.getName(), e);
          System.out.println("ERROR:  Error while processing file.  " +
              "review carefully if the script updated it. " +f);
          return;
      }
   }

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
                
              m_button_mismatches++;
              
              String invalidAttributes = "";
              if(XMLParserHelper.getAttributeValue(sib,"blocking") != null) 
                  invalidAttributes += "contains blocking ";
              if(XMLParserHelper.getAttributeValue(sib,"iconPosition") != null) 
                  invalidAttributes += "contains iconPosition ";
              
              if(isExempt(XMLParserHelper.getAttributeValue(sib, "id"), getFileName(f.getAbsolutePath())))
                  continue;
              
              outputFileWriter.write(getModuleName(f.getAbsolutePath()) + "," + f.getName() + "," 
                              + f.getAbsolutePath() + "," + sib.getNodeName() + "," + 
                              XMLParserHelper.getAttributeValue(sib, "id") + "," + 
                              "button type mismatch," + invalidAttributes + "," 
                              + XMLParserHelper.getAttributeValue(sib, "partialSubmit") + "\n");
            }
        }
    }
  }

  private String getModuleName(String filePath){
     
    if (filePath.contains("/fusionapps/")) {
        Integer fusionappsIndex = filePath.indexOf("/fusionapps/");
        String path_after_fusionapps =
            filePath.substring(fusionappsIndex);
        if (!isEmpty(path_after_fusionapps)) {
            String[] parts = path_after_fusionapps.split("/");
            if (parts != null) {
                return parts[4] + "," + parts[5].trim().toUpperCase();
            }
        }
    }
    return ",";
  }
  
    private void generateExemptionList() {
        
    try{
      BufferedReader fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
          "/fatools/opensource/jauditFixScripts/txt/angrybirdCodescanExemptions.txt"));
      String line = null;
      
      while((line = fileReader.readLine()) != null) {
        String[] parts = line.split(",");
        if(parts.length < 3)
            continue;
        String issue = parts[2].trim();
        if(!issue.equals("Button Type Mismatch"))
            continue;
        String fileName = parts[0].trim();
        String id = parts[1].trim();
          
        HashSet<String> exemptLines = null;
        if(!exemptions.containsKey(fileName)) 
          exemptLines = new HashSet<String>();
        else 
          exemptLines = exemptions.get(fileName);
        
        exemptLines.add(id);
        exemptions.put(fileName, exemptLines);
      }
    }catch(Exception e) {
        e.printStackTrace();
    }
    }
    
    private boolean isExempt(String id, String fileName){
        
        if(!exemptions.containsKey(fileName)) 
            return false;
        
        HashSet<String> lines = (HashSet<String>)exemptions.get(fileName);
        if(lines.contains(id))
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
  
