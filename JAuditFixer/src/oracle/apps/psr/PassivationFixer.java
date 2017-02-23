package oracle.apps.psr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;

import java.io.FileWriter;


import java.util.HashMap;

import java.util.HashSet;

import java.util.Iterator;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.XMLParserHelper;

import oracle.apps.helpers.XmlFixer;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PassivationFixer {
   
   static BufferedWriter writer1;
   static BufferedWriter writer2;
   static AdeDiffHelper diffHelper = new AdeDiffHelper("diff.txt");
   static String sCrawlDir;
   boolean bDebug = true;
   static int num_skipped = 0;
   static int num_audit_exemptions = 0;
   
    public static void main(String[] args) throws Exception{
        
      sCrawlDir = args[0].trim();
      System.out.println("Analyzing all files in crawl directory: " + sCrawlDir);
      writer1 = new BufferedWriter(new FileWriter("passivation_audit_exemption.csv"));
      writer1.write("Rule,File,Attribute,Bug\n");
      writer2 = new BufferedWriter(new FileWriter("errors.txt"));
      diffHelper.startDiffLog();     
      
      PassivationFixer fixer = new PassivationFixer();
      fixer.fixPassivationPatterns();
      diffHelper.closeDiffLog(); 
      writer1.close();
      writer2.close();
      System.out.println("Number of patterns skipped: " + num_skipped);
      System.out.println("Number of audit exemptions: " + num_audit_exemptions);
    }
    
    private void fixPassivationPatterns() throws Exception {
        
      BufferedReader reader = new BufferedReader(new FileReader("passivation_scan.csv"));
      String line = reader.readLine();
      String pathPrefix = getPathBeforeFusionapps();
      XmlFixer xmlFixer = null;
      XMLDocument doc = null;
      String prevFilePath = null;
      String fileContentsAsString = null;
      String[] fileContents = null;
      HashMap<String,HashSet> voAttributesPassivateAll = new HashMap<String,HashSet>();
      HashSet attributeNames = null;
      
      while((line = reader.readLine()) != null) {
          
        String[] parts = line.split(",");
        if(parts.length < 18)
          continue;
        
        String filePath = pathPrefix + parts[3].trim();
        String attrName = parts[6].trim();
          
        String vo_passivate_all = parts[11].trim();
          
        if(vo_passivate_all.equalsIgnoreCase("true")) {      
            
            if(voAttributesPassivateAll.containsKey(filePath))
              attributeNames = voAttributesPassivateAll.get(filePath);        
            else
              attributeNames = new HashSet<String>();
            
            attributeNames.add(attrName);
            voAttributesPassivateAll.put(filePath, attributeNames);
            continue;
        }
          
        String updateable = parts[9].trim();
        if(updateable.equalsIgnoreCase("true")) {
          writer1.write("Passivation," + filePath + "," + attrName + ",Bug\n");
          num_audit_exemptions++;
        }
          
        try {
          if(!filePath.equals(prevFilePath)) {
            
              if(prevFilePath != null) {
                fileContentsAsString = xmlFixer.getFileContentsAsString(false);        
                applyFix(prevFilePath,fileContentsAsString);
              }
            
            doc = XMLParserHelper.getXMLDocument(filePath);        
            xmlFixer = new XmlFixer(filePath);
            fileContents = xmlFixer.convertFileToStringArray();
          }
          prevFilePath = filePath;        
            
         Node n = findElement(doc,parts[6].trim());
         xmlFixer.removeAttribute(n, "Passivate", "true");
              
        } catch (Exception e) {
              System.out.println("Some exception occured while processing file: " + filePath);
              e.printStackTrace();
              System.out.println("Skipping to next file...");
              writer2.write("Some exception occured while processing file: " + filePath + ": " + e.getMessage() + "\n\n");
        }
      }
      reader.close();
      //apply fix to the last file
      fileContentsAsString = xmlFixer.getFileContentsAsString(false);        
      applyFix(prevFilePath,fileContentsAsString);
      
      //Now fix VO_PAssivate_All attributes
      fixVoPassivateAllAttributes(voAttributesPassivateAll);
    }
    
    private void fixVoPassivateAllAttributes(HashMap<String,HashSet> voAttributesPassivateAll) throws Exception{
        
      printMap(voAttributesPassivateAll);
      
      Iterator it = voAttributesPassivateAll.keySet().iterator();
      XmlFixer xmlFixer = null;
      String[] fileContents = null;
      String fileContentsAsString = null;
      XMLDocument doc = null;
      
      while(it.hasNext()){
        String filePath = (String)it.next();
        HashSet<String> attributeNames = voAttributesPassivateAll.get(filePath);
          
        xmlFixer = new XmlFixer(filePath);
        doc = XMLParserHelper.getXMLDocument(filePath); 
        fileContents = xmlFixer.convertFileToStringArray();
          
        NodeList list = doc.getElementsByTagName("ViewObject");
          
        if(list.getLength() != 1) {
          System.out.println("File does not have exactly one ViewObject...Skipping");
          continue;
        }        
        Node viewObj = list.item(0);
        xmlFixer.removeAttribute(viewObj, "Passivate", "All");
        
        list = doc.getElementsByTagName("ViewAttribute");
        for(int i = 0; i< list.getLength(); i++) {
            Node n = list.item(i);
            String attrName = XMLParserHelper.getAttributeValue(n, "Name");
            if(attributeNames.contains(attrName)){
                String attrPassivateSetting = XMLParserHelper.getAttributeValue(n, "Passivate");
                if(attrPassivateSetting != null && attrPassivateSetting.equals("true"))
                    xmlFixer.removeAttribute(n, "Passivate", "true");
            }else {
                String attrPassivateSetting = XMLParserHelper.getAttributeValue(n, "Passivate");
                if(attrPassivateSetting != null && !attrPassivateSetting.equals("true") ) {
                  //TODO: write some error
                    continue;
                }
                if(attrPassivateSetting == null)
                  xmlFixer.addAttribute(n, "Passivate", "true");
            }
        }
        fileContentsAsString = xmlFixer.getFileContentsAsString(false);
        applyFix(filePath,fileContentsAsString);
          
      } //while
    } //method
    
  private String getPathBeforeFusionapps(){
    int i = sCrawlDir.indexOf("fusionapps/");
    if(i == -1)
        i = sCrawlDir.indexOf("fsm/");
    return sCrawlDir.substring(0,i);
  }
  
  private void applyFix(String absPath,
                      String newFileContents) throws Exception {
     if(bDebug)
         System.out.println("applying Fix to file: "+ absPath);
     File file = new File(absPath);
     if (!file.exists()) {
         throw new Exception("while making fix, file not found: " +
                             absPath);
     }
     diffHelper.checkoutAndDelete(file);
      
     System.out.println("Writing new file...");
     FileWriter fw = new FileWriter(file.getAbsolutePath());
     BufferedWriter bw = new BufferedWriter(fw);
     bw.write(newFileContents);
     if (bw != null)
         bw.close();
     if (fw != null)
         fw.close();
     diffHelper.checkinAndDiff(file);
     //diffHelper.diff(file);
  }
  
  private Node findElement(XMLDocument doc, String attrName){
    NodeList list = doc.getElementsByTagName("ViewAttribute");
    for(int i = 0; i< list.getLength(); i++) {
        Node n = list.item(i);
        String attrValue = XMLParserHelper.getAttributeValue(n, "Name");
        if(attrValue != null && attrValue.equals(attrName)) return n;
    }
    return null;
  }
  
  private void printMap(HashMap<String,HashSet> voAttributesPassivateAll) {
    Iterator it = voAttributesPassivateAll.keySet().iterator();
    while(it.hasNext()){
      String filePath = (String)it.next();
      HashSet attributeNames = voAttributesPassivateAll.get(filePath);
      System.out.println(filePath + " --> " + attributeNames );
    }
  }
}
