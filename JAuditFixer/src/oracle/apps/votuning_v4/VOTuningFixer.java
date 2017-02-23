package oracle.apps.votuning_v4;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.regex.Matcher;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class VOTuningFixer {
  static AdeDiffHelper diffHelper;
  static BufferedWriter writer1;
  static BufferedWriter errorWriter;
  static int count_success = 0;
  static int count_failure = 0;        
  
  public VOTuningFixer() {
    super();
  }
  
  
  public void fixAll(ArrayList<ScanResult> al_violations, String outputDir) throws Exception{
    
    if(outputDir == null || outputDir.isEmpty()){
      outputDir = "";
    } 
    else if (!outputDir.endsWith("/")) {
      outputDir = outputDir + "/";
    }
    
    diffHelper = new AdeDiffHelper(outputDir + "diff.txt");
    errorWriter = new BufferedWriter(new FileWriter(outputDir + "errors.txt"));
    diffHelper.startDiffLog();
    writer1 = new BufferedWriter(new FileWriter(outputDir + "CouldNotFix.txt"));
    for(ScanResult r : al_violations){
      try{
      if(makeFixes(r))
        count_success++;
      else
        count_failure++;
      }catch(Exception e){
        count_failure++;
        errorWriter.write(e.getMessage() + "\n");
        continue;
      }
    }
    diffHelper.closeDiffLog(); 
    errorWriter.close();
    writer1.close();
    
    System.out.println();
    System.out.println("********************Summary********************");
    System.out.println(count_success + " violations have been fully fixed. Please review diff.txt to confirm the changes. ");
    System.out.println(count_failure + " violations have not been fully fixed. Please review CouldNotFix.txt to review all the failed cases and fix them manually");
    System.out.println("This script only scans jar files, instead of real UI or ADF BC files in the source path. Therefore, you may have to redeploy the jar if you want to run the scanner again to make sure the code is clean. Otherwise, the scanner won't be able to know the changes you made.");
    System.out.println("NOTE: YOU HAVE TO DO ALL JUNIT TESTING AND RUNTIME TESTING BEFORE CHECKING IN YOUR CHANGES.");
    System.out.println("***********************************************");
  }
  
  public boolean makeFixes(ScanResult r) throws Exception{
    boolean isAutoHgtFixed = true;
    boolean isIterRangesizeFixed = true;
    boolean isVUFetchsizeFixed = true;
    boolean isVODefaultFetchsizeFixed = true;
    
      if(r.getNew_autoHeight() != null)
        isAutoHgtFixed = fixGeneric(r.getPage(), r.getType(), r.getTableId(), "id", "autoHeightRows", r.getAutoHeightRows(),r.getNew_autoHeight());
      
      if(r.getNew_iterRangeSize() != null)
        isIterRangesizeFixed = fixGeneric(r.getPageDef(), "iterator", r.getIterator(), "id", "RangeSize", r.getIterRangeSize(),r.getNew_iterRangeSize().toString());
      
      if(r.getNew_VUFetchSize() != null){
        if (r.getType().equals("table") || r.getType().equals("treeTable"))
        isVUFetchsizeFixed = fixGeneric(r.getAM(),"ViewUsage", r.getBinds(), "Name", "FetchSize", r.getVOUsageFetchSize() == null? null : r.getVOUsageFetchSize().toString(), r.getNew_VUFetchSize().toString());
        else{
          isVUFetchsizeFixed = fixGeneric(r.getEntityObject() == null ? r.getVO() : r.getEntityObject(), "ViewAccessor", r.getViewAccessor(), "Name","FetchSize", r.getVa_fetchSize().equals("Not Set") ? null : r.getVa_fetchSize().toString(), r.getNew_VUFetchSize().toString());
        }
      }
      
      if(r.getNew_VODefaultFetchSize() != null){
        if (r.getType().equals("table") || r.getType().equals("treeTable"))
          isVODefaultFetchsizeFixed = fixGeneric(r.getVO(),"ViewObject", null , null, "FetchSize", r.getVODefaultFetchSize() == null? null : r.getVODefaultFetchSize().toString(), r.getNew_VODefaultFetchSize().toString());
        else
          isVODefaultFetchsizeFixed = fixGeneric(r.getVa_VO(),"ViewObject", null, null, "FetchSize", r.getVODefaultFetchSize() == null? null : r.getVODefaultFetchSize().toString(), r.getNew_VODefaultFetchSize().toString());
      }
    
    return isAutoHgtFixed && isIterRangesizeFixed && isVUFetchsizeFixed && isVODefaultFetchsizeFixed;
  }
  
  private boolean fixGeneric(String filePath, String nodeType, String nodeID, String identityName, String attrName, String oldVal, String newVal) throws Exception{
    XMLDocument doc = null;
    XmlFixer xmlFixer = null;
    String fileContentsAsString = null;
    
    doc = XMLParserHelper.getXMLDocument(filePath);     
    if(doc == null) {
      writer1.write("Could not parse " + filePath.substring(filePath.indexOf("oracle/")) + " using DOM" + ". ");
      writer1.write("Please change the value of " + attrName + "for node with " + identityName + " " + nodeID + " to " + newVal + ".\n");
      return false;
    }
    xmlFixer = new XmlFixer(filePath);
    
    NodeList components = doc.getElementsByTagName(nodeType);
    if(components == null || components.getLength() < 1){
        writer1.write("Could not fix: can't find any node with name <"+nodeType +"> in file " + filePath + ". ");
        writer1.write("Please change the value of " + attrName + "for node with " + identityName + " " + nodeID + " to " + newVal + ".\n");
        return false;
    }
    Node n = null;
    
    if(identityName != null){
      for(int i = 0; i < components.getLength(); i++) {
        n = components.item(i);
        String id = XMLParserHelper.getAttributeValue(n, identityName);
        if(id !=null && id.equals(nodeID))
            break;
        else
            n=null;
      }
    }
    else{
      n = components.item(0);
    }
    
    Matcher m = null;
    
    if(n !=null) {
      String existingAttrVal = XMLParserHelper.getAttributeValue(n, attrName);

      if (existingAttrVal == null && oldVal != null) {
        writer1.write(attrName + " for node with " + identityName + " " +
                           nodeID + " in file " + filePath +
                           " does not match with the scan result spreadsheet. Script will not fix it." + "\n");
        return false;
      }
      else if(existingAttrVal == null && oldVal ==null){
        xmlFixer.addAttribute(n,attrName,newVal);
      }
      else if (existingAttrVal.equals(oldVal)){
        try{
          xmlFixer.modifyAttribute(n,attrName,escapeSpecialChar(oldVal),newVal,false);
        }catch(Exception e){
          xmlFixer.modifyAttribute(n,attrName,oldVal,newVal,false);
        }
      }
      else if (existingAttrVal.equals(newVal)){
        return true;
      }
      else{
        writer1.write(attrName + " for node with " + identityName + " " +
                           nodeID + " in file " + filePath +
                           " does not match with the scan result spreadsheet. Script will not fix it." + "\n");
        return false;
      }
    }
    else{
      writer1.write("Could not fix: can't find the node with "+identityName +" equals to " + nodeID + " in file " + filePath + ". ");
      writer1.write("Please change the value of " + attrName + "for node with " + identityName + " " + nodeID + " to " + newVal + ".\n");
      return false;
    }
    
    fileContentsAsString = xmlFixer.getFileContentsAsString(false);    //this will get the changed file contents  
    diffHelper.applyFix(filePath,fileContentsAsString); 
      
    return true;
  }
  
  public static String escapeSpecialChar(String origin) throws IOException {
    //return if EL is not like: "#{bindings.ManageRemittanceBatchUI1.iteratorBinding.rangeSize <= 15 ? bindings.ManageRemittanceBatchUI1.iteratorBinding.rangeSize : 15}"
    if(!origin.startsWith("#") || ! origin.contains("?"))
      return origin;
  
   return origin.replaceAll(">=", "&gt;=").replaceAll("<=", "&lt;=").replaceAll(">","&gt;").replaceAll("<", "&lt;").replaceAll("&", "&amp;");
  }
}
