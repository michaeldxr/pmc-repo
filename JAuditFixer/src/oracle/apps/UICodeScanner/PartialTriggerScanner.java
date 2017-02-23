package oracle.apps.UICodeScanner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.io.IOException;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.utility.JoesBaseClass;
import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class PartialTriggerScanner extends JoesBaseClass{
    
    private static String  sCrawlDir;
    private static String mode="view"; //can be view/LRG/premerge
    private static BufferedWriter writer1;
    private static BufferedWriter writer2;
    private static HashSet<String> m_componentType = new HashSet<String>();
    public static HashMap<String,HashSet<String>> exemption_unusedAutosubmit = new HashMap<String,HashSet<String>>();
    public static HashMap<String,HashSet<String>> exemption_nonExistentPPR = new HashMap<String,HashSet<String>>();
    public static HashMap<String,HashSet<String>> s_rel10 = new HashMap<String,HashSet<String>>();
    static int count = 0;
    static int count1 = 0;
    static int releaseNum = 0;  
    
    public PartialTriggerScanner() 
    {
        super(JoesBaseClass.CRAWL_TYPE.JSPX_AND_JSFF);
    }
    
    public static void main(String[] args) throws Exception{
        PartialTriggerScanner scanner = new PartialTriggerScanner();
        scanner.init();
        
        mode = System.getProperty("mode");
        if(mode == null)
            mode="view";
        System.out.println("Operation mode is: " + mode);
        
        writer1 = new BufferedWriter(new FileWriter("UnusedAutoSubmit.csv"));         
        writer1.write("Family,Module,Product,Filename,Series,Label," +
          "ComponentName, ComponentId, Description\n");   
        
        writer2 = new BufferedWriter(new FileWriter("NonExistantPartialTriggers.csv"));         
        writer2.write("Family,Module,Product,Filename,Series,Label," +
          "ComponentId, Original Partial Trigger Id, Description\n"); 
        
        if(mode.equals("view")){
            
            if (args.length < 2 || args[0] == null || args[0].trim().equals("") || args[1] == null || args[1].trim().equals("")) {
                 System.out.println("USAGE: reviewPartialTriggers.sh <SERIES>");
                 System.exit(1);
             }
            sCrawlDir = args[0].trim();
             //sCrawlDir = "/ade_autofs/ud21_fa/FUSIONAPPS_PT.R9INT_LINUX.X64.rdd/LATEST/fusionapps";
            String series = args[1];
            
            File f = new File(sCrawlDir);
            if(!f.exists()) {
                System.out.println("Crawl directory does not exist - " + sCrawlDir);
                System.exit(1);
            }
            
          if (!series.contains("FSM")) {
            String release = FamilyModuleHelper.getRelease(series);
          
            try {
              releaseNum = Integer.parseInt(release);
            } catch (NumberFormatException e) {
              releaseNum = 0;
            }          
          }
            
          generateExemptionList(series);
          
          
            
            System.out.println("Analyzing all files in directory: " + sCrawlDir);
            scanner.crawlDirectory(sCrawlDir,false);           
            System.out.println("Done crawlying dir for " + sCrawlDir);      
            System.out.println(count + " violations found in total.");
            System.out.println("Please review UnusedAutoSubmit.csv for details. ");
        }
        if(mode.equals("LRG")){
            if (args.length < 1 || args[0] == null || args[0].trim().equals("")) {
                 System.out.println("USAGE: runUnusedPartialTrigger.sh <series>");
                 System.exit(1);
             }

            String series = args[0];
         //String series = "FUSIONAPPS_PT.V2MIBFINC_LINUX.X64";
      
            if (!series.contains("FSM")) {
              String release = FamilyModuleHelper.getRelease(series);
      
              try {
                releaseNum = Integer.parseInt(release);
              } catch (NumberFormatException e) {
                releaseNum = 0;
              }
      
            }

            Date start = new Date();
            
            generateExemptionList(series);
             
            String label = LRGUtil.getLatestLabel(series);
            System.out.println("Running Unused AutoSubmit and NonExistentPartialTrigger Scan on series: " + series + ".......");
            
            ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
            if(releaseNum == 10){
              families.remove("scm");
              families.remove("ic");
              families.remove("prc");
            }
            for (int j = 0; j < families.size(); j++) {
                String family = families.get(j);
                String sCrawlDir = LRGUtil.getCrawlDir(series, family, label, "Unused AutoSubmit");
                System.out.println("Crawling dir '" + sCrawlDir + "' for family: " + family);                
                scanner.crawlDirectory(sCrawlDir,false);
                System.out.println("Done crawling for family "+family+".");
            }
            
            System.out.println(count1);
            
          FamilyModuleHelper. printScanDuration(start,"UnusedAutoSubmit");
            
        }else if(mode.equals("premerge")) {
          
          //String[] files = args[0].trim().split("\n");
          String ade_view_root = args[1].trim();
          String filelistpath=args[0].trim();
          String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root);   
          String label = args[2].trim();
          
          String release= FamilyModuleHelper.getRelease(label);
          System.out.println("Release: " + release);
          
          try{            
              releaseNum = Integer.parseInt(release);
              if (releaseNum < 10) //This check was mandated post release 10 only, hence this check is necessary to avoid the current rel9 branches from failing
                  return;              
          }catch(NumberFormatException e){
              System.out.println("Could not determine release. Skipping UnusedAutoSubmit check...");
              return;
          }
          
          String[] parts= label.split("X64");
          if(parts.length <1){
              System.out.println("Could not determine sereis. Skipping UnusedAutoSubmit check...");
              return;
          }
          String series = parts[0]+"X64";
          
          //bug-22584972 Check if transaction contains jsffs before scanning code.
          boolean jsffExists = false;
          for(int i = 0; i < files.length; i++) {             
              String filePath = ade_view_root + "/" + files[i].trim();
              if(filePath.endsWith(".jsff")){
                  jsffExists = true;
                  break;
              }
          }
          
          if(!jsffExists){
              writer2.write("Skipping UnusedAutoSubmit scan. No jsffs in the transaction.");
          }else{          
              generateExemptionList(series);
            
              for(int i = 0; i < files.length; i++) {             
                  String filePath = ade_view_root + "/" + files[i].trim();
                  File f = new File(filePath);
                  if(!f.exists())
                      continue;
                  if(!filePath.endsWith(".jsff"))
                      continue;
                  System.out.println("Processing " + f.getAbsoluteFile());
                  scanner.processFile(f,false);
              }
              writer2.write("\n\nPlease see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/UnusedPartialTriggers " +
                  "for description of the issue and resolution.\n\n");
          }
          
      }
        
        writer1.close();
        writer2.close();
    }
    
    protected void processFile(File fName, boolean bDoADE) {
        
        String fileName = fName.getAbsolutePath();

        if(!fileName.endsWith(".jsff") && !fileName.endsWith(".jspx")) 
            return;
        
        try{
            
            XMLDocument doc = XMLParserHelper.getXMLDocument(fileName, false);
            if(doc == null)
                return;
            NodeList adfc = doc.getElementsByTagName("jsp:root");
            if (adfc == null) 
            {
                System.out.println("REVIEW:  Not a page file ---> " + fileName);
                return;
            }
            
            //Added by Yao. Ignore all page which contains Dynamic Declarative components
            NodeList nl = doc.getElementsByTagName("componentDef");
            if (nl != null && nl.getLength() != 0) 
            {
                //System.out.println("Ignore the page which contains Dynamic Declarative components:  " + fileName);
                return;
            }
          
          //
          ArrayList<String> l_regionid = new ArrayList<String>();
          nl = doc.getElementsByTagName("region");
          if (nl != null && nl.getLength() != 0) 
          {
            for(int i = 0; i < nl.getLength(); i++) {
              Node n = nl.item(i);
              String id = XMLParserHelper.getAttributeValue(n, "id");
              l_regionid.add(id);
            }             
          }
            HashSet<String> partialTriggerIds = new HashSet<String>();
            
            NodeList list = doc.getElementsByTagNameNS("*","*");
            
            //Step1. Build hashSet of all partial triggers
            for(int i = 0; i < list.getLength(); i++) {
                
                Node child = list.item(i);
                String partialTriggers = XMLParserHelper.getAttributeValue(child, "partialTriggers");
                if(partialTriggers != null) {
                    
                    String[] parts = partialTriggers.split(" ");
                    for(int j = 0; j < parts.length; j++){
                        boolean isRemoteRef = false;
                        String partialTrigId = parts[j].trim();
                      
                          for(String regionid : l_regionid){
                            if(partialTrigId.startsWith(regionid + ":") || partialTrigId.contains(":"+regionid + ":")){
                              count1++;
                              //System.out.println(partialTrigId);
                              //System.out.println(fName.getAbsolutePath());
                              isRemoteRef = true;
                              break;
                            }
                          }
                            
                        partialTrigId = partialTrigId.replace("::", "");
                        String[] parts1 = partialTrigId.split(":");
                        partialTrigId = parts1[parts1.length-1];
                        
                        if(!isRemoteRef)
                          partialTriggerIds.add(partialTrigId);
                    }
                }
                
                partialTriggers = XMLParserHelper.getAttributeValue(child, "createPartialTriggers");
                if(partialTriggers != null) {
                    
                    partialTriggers = partialTriggers.replace("[", "");
                    partialTriggers = partialTriggers.replace("]", "");
                    String[] parts = partialTriggers.split(" ");
                    for(int j = 0; j < parts.length; j++){
                        boolean isRemoteRef = false;
                        String partialTrigId = parts[j].trim();
                      
                        for(String regionid : l_regionid){
                          if(partialTrigId.contains(regionid + ":")){
                            count1++;
                            //System.out.println(partialTrigId);
                            //System.out.println(fName.getAbsolutePath());
                            isRemoteRef = true;
                            break;
                          }
                        }
                      
                        partialTrigId = partialTrigId.replace("::", "");
                        String[] parts1 = partialTrigId.split(":");
                        partialTrigId = parts1[parts1.length-1];
                        partialTrigId = partialTrigId.replaceAll(",", "");
                        
                        if(!isRemoteRef)
                          partialTriggerIds.add(partialTrigId);
                    }
                }
                
                partialTriggers = XMLParserHelper.getAttributeValue(child, "deletePartialTriggers");
                if(partialTriggers != null) {
                    
                    partialTriggers = partialTriggers.replace("[", "");
                    partialTriggers = partialTriggers.replace("]", "");
                    String[] parts = partialTriggers.split(" ");
                    for(int j = 0; j < parts.length; j++){
                        boolean isRemoteRef = false;
                        String partialTrigId = parts[j].trim();
                      
                        for(String regionid : l_regionid){
                          if(partialTrigId.contains(regionid + ":")){
                            count1++;
                            //System.out.println(partialTrigId);
                            //System.out.println(fName.getAbsolutePath());
                            isRemoteRef = true;
                            break;
                          }
                        }
                      
                        partialTrigId = partialTrigId.replace("::", "");
                        String[] parts1 = partialTrigId.split(":");
                        partialTrigId = parts1[parts1.length-1];
                        partialTrigId = partialTrigId.replaceAll(",", "");
                        
                        if(!isRemoteRef)
                          partialTriggerIds.add(partialTrigId);
                    }
                }
                
                partialTriggers = XMLParserHelper.getAttributeValue(child, "editPartialTriggers");
                if(partialTriggers != null) {
                    
                    partialTriggers = partialTriggers.replace("[", "");
                    partialTriggers = partialTriggers.replace("]", "");
                    String[] parts = partialTriggers.split(" ");
                    for(int j = 0; j < parts.length; j++){
                        boolean isRemoteRef = false;
                        String partialTrigId = parts[j].trim();
                      
                        for(String regionid : l_regionid){
                          if(partialTrigId.contains(regionid + ":")){
                            count1++;
                            //System.out.println(partialTrigId);
                            //System.out.println(fName.getAbsolutePath());
                            isRemoteRef = true;
                            break;
                          }
                        }
                        partialTrigId = partialTrigId.replace("::", "");
                        String[] parts1 = partialTrigId.split(":");
                        partialTrigId = parts1[parts1.length-1];
                        partialTrigId = partialTrigId.replaceAll(",", "");
                        
                        if(!isRemoteRef)
                          partialTriggerIds.add(partialTrigId);
                    }
                }
                partialTriggers = XMLParserHelper.getAttributeValue(child, "duplicatePartialTriggers");
                if(partialTriggers != null) {
                    
                    partialTriggers = partialTriggers.replace("[", "");
                    partialTriggers = partialTriggers.replace("]", "");
                    String[] parts = partialTriggers.split(" ");
                    for(int j = 0; j < parts.length; j++){
                        boolean isRemoteRef = false;
                        String partialTrigId = parts[j].trim();
                      
                        for(String regionid : l_regionid){
                          if(partialTrigId.contains(regionid + ":")){
                            count1++;
                            //System.out.println(partialTrigId);
                            //System.out.println(fName.getAbsolutePath());
                            isRemoteRef = true;
                            break;
                          }
                        }
                      
                        partialTrigId = partialTrigId.replace("::", "");
                        String[] parts1 = partialTrigId.split(":");
                        partialTrigId = parts1[parts1.length-1];
                        partialTrigId = partialTrigId.replaceAll(",", "");
                        
                        if(!isRemoteRef)
                          partialTriggerIds.add(partialTrigId);
                    }
                }
            }
            
            //Step2. Find all components that have autoSubmit = true, but thier id does not appear in any partial trigger list
            ArrayList<Node> result = getNodesInListWithMatchingAttributeNotNull(list,"autoSubmit","true");
            for(int i= 0; i < result.size(); i++) {
                
                Node n = result.get(i);
                String nodeName = n.getNodeName().trim();
                
                //Only flag cerntain components
                if(!m_componentType.contains(nodeName)){
                    continue;
                }
                
                String nodeId = XMLParserHelper.getAttributeValue(n, "id");
                
                if(nodeId != null && !partialTriggerIds.contains(nodeId)){
                    
                    String valueChangeListener = XMLParserHelper.getAttributeValue(n, "valueChangeListener");
                    if(valueChangeListener != null && !valueChangeListener.trim().equals(""))
                        continue;
                    
                    String validator = XMLParserHelper.getAttributeValue(n, "validator");
                    if(validator != null && !validator.trim().equals(""))
                        continue;
                    
                    //bug 18761030
                    ArrayList<Node> al = XMLParserHelper.getChildNodesWithName(n,"af:convertNumber");
                    if(al != null && al.size() > 0)
                      continue;
                    
                    al = XMLParserHelper.getChildNodesWithName(n,"af:convertDateTime");
                    if(al != null && al.size() > 0)
                      continue;
                    //String actionListener = XMLParserHelper.getAttributeValue(n, "actionListener");
                    
                    if(!isExempt1(fileName, nodeId)){         
                      
                      if(releaseNum == 10 && fixInREL10(fileName, nodeId)){
                        writer1.write(FamilyModuleHelper.getFileNameInfo1(fileName) + nodeName + "," + nodeId 
                              + ",Node does not appear in partialTriggers list for any other node and " +
                              "nor has a valueChangeListener. autoSubmit should be set to false.\n");
                        count++;
                      }else if (releaseNum > 10){
                        writer1.write(FamilyModuleHelper.getFileNameInfo1(fileName) + nodeName + "," + nodeId 
                              + ",Node does not appear in partialTriggers list for any other node and " +
                              "nor has a valueChangeListener. autoSubmit should be set to false.\n");
                        count++;
                      }
                    }
                      
                }
            }
            
            //Build a hashset of all component ids used in the page
            HashSet<String> ids = new HashSet<String>();
            
            for(int i = 0; i < list.getLength(); i++) {
                
                Node child = list.item(i);
                String id = XMLParserHelper.getAttributeValue(child, "id");
                if(id != null) 
                   ids.add(id);
            }
          
            //bug-18679479
            nl = doc.getElementsByTagName("keyFlexfield");
            if (nl != null && nl.getLength() != 0) 
            {
              for(int i = 0; i < nl.getLength(); i++) {
                Node n = nl.item(i);
                String id = XMLParserHelper.getAttributeValue(n, "id");
                ids.add(id + "CS");
              }             
            }
            
            ids.add("delete");
            ids.add("deleteMenuItem");
            ids.add("create");
            ids.add("createMenuItem");
            ids.add("duplicate");
            ids.add("duplicateMenuItem");
            ids.add("edit");
            ids.add("editMenuItem");
            ids.add("confirm");
            //bug-18679664
            ids.add("save");
            
            for(Iterator<String> it = partialTriggerIds.iterator(); it.hasNext();) {
                
                String origPartialTrigId = it.next();
                String partialTrigId = origPartialTrigId.replace("::", "");
                String[] parts = partialTrigId.split(":");
                partialTrigId = parts[parts.length-1];
                
                //Every Application table has panel Collections, Menu Items etc which can trigger PPR and can be used for setting PartialTriggers.
                // These will not be available in source though explicitly.
                if(partialTrigId.equals("_ATp") || partialTrigId.equals("_ATTp") || partialTrigId.contains("APsv") || partialTrigId.equals("ATctem")|| partialTrigId.equals("ATeam") || partialTrigId.equals("APca") || partialTrigId.equals("APcn") || partialTrigId.equals("APsca") || partialTrigId.equals("APscn"))
                  continue;
              
                if(!partialTrigId.trim().equals("") && !ids.contains(partialTrigId)){
                      
                  if(!isExempt2(fileName, partialTrigId.trim())){ 
                    
                    if(releaseNum == 10 && fixInREL10(fileName, partialTrigId)){                  
                      writer2.write(FamilyModuleHelper.getFileNameInfo1(fileName) + partialTrigId + 
                                    "," + origPartialTrigId + ",Non-existent PartialTrigger\n");
                      
                    }else if(releaseNum > 10){
                      writer2.write(FamilyModuleHelper.getFileNameInfo1(fileName) + partialTrigId + 
                                    "," + origPartialTrigId + ",Non-existent PartialTrigger\n");
                    }
                  }
                }
            }
            
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private ArrayList<Node> getNodesInListWithMatchingAttributeNotNull(NodeList nodelist,
                                                           String attribute,
                                                           String attrValue) {
        ArrayList<Node> result = new ArrayList<Node>();
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

    protected String getSummaryReport() {
        return null;
    }

    protected String getSummaryReportSubject() {
        return null;
    }

    private void init() {
        m_componentType.add("af:inputText");
        m_componentType.add("af:selectOneChoice");
        m_componentType.add("af:inputDate");
        m_componentType.add("af:selectBooleanCheckbox");
    }

  
  private static void generateExemptionList(String series) throws IOException{
    
    BufferedReader fileReader = null;
    String line = null;
    
    try {
      //Exemptions for attribute level validations
      fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
          "/fatools/opensource/jauditFixScripts/txt/EoAttrValidation.csv"));
      line = null;
      
      while((line = fileReader.readLine()) != null) {
        String[] parts = line.split(",");
        if(parts.length < 2)
            continue;

          String fileName = parts[0].trim();
          String id = parts[1].trim();
          
          int index = fileName.indexOf("oracle/");
          
          if(index != -1)
            fileName = fileName.substring(index);
            
          HashSet<String> exemptLines = null;
          if(!exemption_unusedAutosubmit.containsKey(fileName)) 
            exemptLines = new HashSet<String>();
          else 
            exemptLines = exemption_unusedAutosubmit.get(fileName);
          
          exemptLines.add(id);
          exemption_unusedAutosubmit.put(fileName, exemptLines);
        
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Skipping exemptions as file could not be found: " + e.getMessage());
    }finally{
      if(fileReader != null)
        fileReader.close();  
    }
    
    try{
      //regular exemptions
      fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
          "/fatools/opensource/jauditFixScripts/txt/angrybirdCodescanExemptions_Yao.txt"));
      line = null;
      
      while((line = fileReader.readLine()) != null) {
        String[] parts = line.split(",");
        if(parts.length < 3)
            continue;
        String issue = parts[0].trim();
        if(issue.equals("UnusedAutoSubmit")){
          String fileName = parts[1].trim();
          String id = parts[2].trim();
          
          int index = fileName.indexOf("oracle/");
          
          if(index != -1)
            fileName = fileName.substring(index);
            
          HashSet<String> exemptLines = null;
          if(!exemption_unusedAutosubmit.containsKey(fileName)) 
            exemptLines = new HashSet<String>();
          else 
            exemptLines = exemption_unusedAutosubmit.get(fileName);
          
          exemptLines.add(id);
          exemption_unusedAutosubmit.put(fileName, exemptLines);
        }
        
        if(issue.equals("NonExistentPartialTriggers")){
          String fileName = parts[1].trim();
          String id = parts[2].trim();
          
          int index = fileName.indexOf("oracle/");
          
          if(index != -1)
            fileName = fileName.substring(index);
            
          HashSet<String> exemptLines = null;
          if(!exemption_nonExistentPPR.containsKey(fileName)) 
            exemptLines = new HashSet<String>();
          else 
            exemptLines = exemption_nonExistentPPR.get(fileName);
          
          exemptLines.add(id);
          exemption_nonExistentPPR.put(fileName, exemptLines);
        }
      }
    }catch(Exception e) {
        System.out.println("Skipping exemptions as file could not be found: " + e.getMessage());
    }finally{
      if(fileReader != null)
        fileReader.close();  
    }
    
    try{
      //generate list of files HCM and CRM target to fix at REL10
      fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
           "/fatools/opensource/jauditFixScripts/txt/UnusedAutoSubmit.txt"));
      line = null;
      //skip the header.
      fileReader.readLine();
      
      while((line = fileReader.readLine()) != null) {
        String[] parts = line.split(",");
        if(parts.length < 2)
            continue;
        String fileName = parts[0].trim();
        String id = parts[1].trim();
            
          HashSet<String> exemptLines = null;
          if(!s_rel10.containsKey(fileName)) 
            exemptLines = new HashSet<String>();
          else 
            exemptLines = s_rel10.get(fileName);
          
          exemptLines.add(id);
          s_rel10.put(fileName, exemptLines);
        }    
    }catch(Exception e) {
        System.out.println("Skipping exemptions as file could not be found: " + e.getMessage());
    }finally{
      if(fileReader != null)
        fileReader.close();  
    }
  }
  
  
  private static boolean isExempt1(String page, String id){
    //bug-18685393
    if(isDOO(page))
      return true;
    
    //bug-20055285
    if(releaseNum < 14 && isMKT(page))
      return true;
    
    //bug-20293098
    if(isZMM(page))
      return true;
    
    //bug-19358432
    if(page.contains("fusionapps/fin/components/payables/fa/additions/ui/public_html/oracle/apps/financials/assets/additions/ui/page/SourceLineSearch.jsff") && id.equals("inputText1"))
      return true;
    if(page.contains("fusionapps/fin/components/payables/fa/additions/ui/public_html/oracle/apps/financials/assets/additions/ui/page/Asset.jsff") && (id.equals("inputText2") || id.equals("inputText3") || id.equals("inputText4") || id.equals("LifeMonths") || id.equals("AdjustedRate") || id.equals("soc5")))
      return true;
    if(page.contains("fusionapps/fin/components/payables/fa/massTransactions/shared/publicUi/public_html/oracle/apps/financials/assets/massTransactions/shared/publicUi/page/MassTransaction.jsff") && (id.equals("it150") || id.equals("soc16")))
      return true;
    
    //bug-19066112
    if(page.contains("fusionapps/fin/components/payables/fa/")){
      if(releaseNum == 10){
        int index = page.indexOf("oracle/");
        
        if(index != -1)
          page = page.substring(index);
        
        HashSet<String> s = exemption_unusedAutosubmit.get(page);
        if(s == null)
          return false;
        else if(s.contains("ALL")){
          return true;
        }
        else{
          return s.contains(id);
        }
      }else{
        return false;
      }
    }
    
    int index = page.indexOf("oracle/");
    
    if(index != -1)
      page = page.substring(index);
    
    HashSet<String> s = exemption_unusedAutosubmit.get(page);
    if(s == null)
      return false;
    else if(s.contains("ALL")){
      return true;
    }
    else{
      return s.contains(id);
    }
  }
  
  private static boolean isExempt2(String page, String id){
    //bug-18685393
    if(isDOO(page))
      return true;
    
    //bug-20055285
    if(releaseNum < 14 && isMKT(page))
      return true;
    
    //bug-20293098
    if(isZMM(page))
      return true;
    
    if(releaseNum == 10 && page.contains("/prc/"))
      return true;
    
    int index = page.indexOf("oracle/");
    
    if(index != -1)
      page = page.substring(index);
    
    HashSet<String> s = exemption_nonExistentPPR.get(page);
    if(s == null)
      return false;
    else if(s.contains("ALL")){
      return true;
    }
    else{
      return s.contains(id);
    }
  }

  private static boolean isDOO(String page) {
    if(page.contains("fusionapps/scm/components/orderOrchestration/doo/common/constraints/") ||
       page.contains("fusionapps/scm/components/orderOrchestration/doo/common/holds/") ||
       page.contains("fusionapps/scm/components/orderOrchestration/doo/common/status/") ||
       page.contains("fusionapps/scm/components/orderOrchestration/doo/orchestration/processDefinition/") ||
       page.contains("fusionapps/scm/components/orderOrchestration/doo/orchestration/processPlanning/"))
      return true;
    
    return false;
  }
  
  private static boolean isMKT(String page) {
    if(page.contains("fusionapps/crm/components/sales/mkt/commonMarketing/fckPlugin/ui/MktFckPlugin/") ||
       page.contains("fusionapps/crm/components/sales/mkt/commonMarketing/budgets/publicUi/") ||
       page.contains("fusionapps/crm/components/sales/mkt/commonMarketing/budgets/hwui/") ||
       page.contains("fusionapps/crm/components/sales/mkt/commonMarketing/activityTasks/hwui/") ||
       page.contains("fusionapps/crm/components/sales/mkt/commonMarketing/productSearch/ui/") ||
       page.contains("fusionapps/crm/components/sales/mkt/searchMarketing/") ||
       page.contains("fusionapps/crm/components/sales/mkt/bi/ui/") ||
       page.contains("fusionapps/crm/components/sales/mkt/response/ui/") ||
       page.contains("fusionapps/crm/components/sales/mkt/salesCampaigns/ui/") ||
       page.contains("fusionapps/crm/components/sales/mkt/esdClient/MonitorEssProcesses/") ||
       page.contains("fusionapps/crm/components/sales/mkt/esdClient/jobMonitor/ui/") ||
       page.contains("fusionapps/crm/components/sales/mkt/coreMarketing/events/ui/") ||
       page.contains("fusionapps/crm/components/sales/mkt/coreMarketing/treatments/ui/") ||
       page.contains("fusionapps/crm/components/sales/mkt/coreMarketing/segments/ui/") ||
       page.contains("fusionapps/crm/components/sales/mkt/coreMarketing/sourcecode/ui/") ||
       page.contains("fusionapps/crm/components/sales/mkt/coreMarketing/campaigns/ui/") ||
       page.contains("fusionapps/crm/components/sales/mkt/coreMarketing/campaigns/hwui/") ||
       page.contains("fusionapps/crm/components/sales/mkt/coreMarketing/lists/ui/") ||
       page.contains("fusionapps/crm/components/sales/mkt/coreMarketing/dialogs/ui/"))
      return true;
    
    return false;
  }
  
  private static boolean isZMM(String page) {
    if(page.contains("fusionapps/crm/components/crmCommon/zmm/activities/") ||
       page.contains("fusionapps/crm/components/crmCommon/zmm/interactions/"))
      return true;
    
    return false;
  }

  private boolean fixInREL10(String fileName, String nodeId) {
    if(!(fileName.contains("/crm/") ||  fileName.contains("/hcm/") ||  fileName.contains("/scm/") || fileName.contains("/ic/") || fileName.contains("/prc/")))
      return true;
    
    int index = fileName.indexOf("fusionapps/");
    
    if(index != -1)
      fileName = fileName.substring(index);
    
    Set<String> ids = s_rel10.get(fileName);
    
    if(ids == null)
      return false;
    
    if(ids.contains("ALL"))
      return true;
    
    if(!ids.contains(nodeId))
        return false;
    
    return true;
  }
}
