package oracle.apps.sqlanalyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import java.util.logging.Level;

import java.util.regex.Matcher;

import java.util.regex.Pattern;

import oracle.apps.helpers.JprHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.votuning.PageDef;
import oracle.apps.votuning.TuningAnalysisRecord;
import oracle.apps.votuning.TuningConstants;
import oracle.apps.votuning.UiFileHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ViewCriteriaHelper {
    
    public static boolean bDebug= false;
    public static Pattern pTablesColumn = Pattern.compile("(.*?)\\((.*?)\\)");
    public static String resolvedTableColumn = " ";
    public static boolean isView = false;
    
    public static void main(String[] args) {
        
        String bcFullPackage= "oracle.apps.financials.abcd.xyz";
        String intelligentBcPath = bcFullPackage.replaceFirst("oracle\\.apps\\..*?\\.", "");
        System.out.println(intelligentBcPath);
        
        bcFullPackage= "oracle.apps.dafdsf.abcd.xyz";
        intelligentBcPath = bcFullPackage.replaceFirst("oracle\\.apps\\..*?\\.", "");
        System.out.println(intelligentBcPath);
    }
    
    public static String getFileNameInfoFromRelativePath(String path,String jsff) throws IOException {
       
        
        String family = " ";
        String module = " ";
        String product = " ";
        
        //get label from jsff
        File f = new File(jsff);
        String absPath = jsff;
        try{
            absPath = f.getCanonicalPath();
        } catch(Exception e) {
            absPath = jsff;
        }
        int i = 0;
        
        if(absPath.contains("fusionapps/")) 
          i = absPath.indexOf("fusionapps/");
        
        if(absPath.contains("fsm/")) 
          i = absPath.indexOf("fsm/");
        
        if (absPath.contains("atgpf/")) 
          i = absPath.indexOf("atgpf/");
        
        if (absPath.contains("emcore/"))
          i = absPath.indexOf("emcore/");
        
        String path_before_fusionapps = absPath.substring(0, i-1);
        String[] parts = path_before_fusionapps.split("/");
        String label = " ";
        if(parts != null && parts.length >=2) {
            label = parts[parts.length -2] + "_" + parts[parts.length -1];      
            label = label.replace(".rdd","");   
        }
        
        //get family, module,product from path (vo relative path)
        parts = path.split("/");
        
        if(parts.length > 2)
            family = parts[2].trim().toUpperCase();
        
        if(family.equals("FINANCIALS"))
            family = "FIN";
        if(family.equals("CDM") || family.equals("CONTRACTS"))
            family = "CRM";
        if(family.equals("PROJECTS"))
            family = "PRJ";
        if(family.equals("PARTNERMGMT")){
            family = "CRM";
            module= "partnerMgmt";
        }
        if(family.equals("FUNCTIONALCORE"))
          family="FSM";
        if(family.equals("MARKETING")){
            family="CRM";
            module="marketing";
        }
        if(family.equals("INCENTIVECOMPENSATION"))
            family = "IC";
        
        if(parts.length > 3)
            product = parts[3].trim().toUpperCase();
        
        if(product.equals("ASSETS")){
            module="payables";
            product="FA";
        }
        if(product.equals("HUBBASE")){
            product="ZCH";
            module="crmCommon";
        }
        if(product.equals("CASHMANAGEMENT")){
            module="payables";
            product="CE";
        }
        if(product.equals("GENERALLEDGER")){
            module="ledger";
            product="GL";
        }
        
        return family + "," + module + "," + product + "," + path + "," + label + ",";
    }
    
    public static String getFileNameInfoFromRelativePath(String path) throws IOException {
       
        
        String family = " ";
        String module = " ";
        String product = " ";
        
        //get family, module,product from path (vo relative path)
        String[] parts = path.split("/");
        
        if(parts.length > 1)
            family = parts[1].trim().toUpperCase();
        
        if(parts.length > 3)
            module = parts[3].trim().toUpperCase();
        if(parts.length > 4)
            product = parts[4].trim().toUpperCase();
        
        return family + "," + module + "," + product + ",";
    }
    
    public static String getDbFileNameInfo(String absPath){
        
        int i = 0;
        String family = "";
        String module = "";
        String product = "";
        String blankString =  "NA,NA,NA,NA,NA,";
        if(isEmpty(absPath))
            return blankString;
        
        if(absPath.contains("fusionapps/")) {
         i = absPath.indexOf("fusionapps/");
        }
        if(absPath.contains("fsm/")){
         i = absPath.indexOf("fsm/");
         family = "FSM";
        }
          if (absPath.contains("atgpf/")) {
              i = absPath.indexOf("atgpf/");
              family = "ATGPF";
          }
          
        if (absPath.contains("emcore/")) {
            i = absPath.indexOf("emcore/");
            family = "EMGC";
            module = "EMCORE";
        }
          
        String pathAfterFusionApps = absPath.substring(i);
        String[] parts = pathAfterFusionApps.split("/");
        
        
        if(parts == null) return blankString;
        
        if(absPath.contains("fusionapps/")) {
            int partsLength = parts.length;
            if(partsLength>1)
              family = parts[1].trim().toUpperCase();  
            if(partsLength>3)
              module = parts[3].trim();
            if(partsLength>2)
              product = parts[2].trim().toUpperCase();
            
        }
        if(absPath.contains("fsm/")) {
          int partsLength = parts.length;
          if(partsLength>3)
            module = parts[3].trim();
          if(partsLength>2)
            product = parts[2].trim().toUpperCase();
        }
        
          if (absPath.contains("atgpf/")) {
              int partsLength = parts.length;
              if (partsLength > 3)
                  module = parts[3].trim();
              if (partsLength > 2)
                  product = parts[2].trim().toUpperCase();
          }
        if (absPath.contains("emcore/")) {
            int partsLength = parts.length;
            if (partsLength > 1)
                product = parts[1].trim().toUpperCase();
        }
           
        if(i<1)
           return family + "," + module + "," + product + "," + pathAfterFusionApps + ", ," ;
        
        String path_before_fusionapps = absPath.substring(0, i-1);
        parts = path_before_fusionapps.split("/");
        if(parts==null || parts.length<2)
          return blankString;
        
        String label = parts[parts.length -2] + "_" + parts[parts.length -1];      
        label = label.replace(".rdd","");   
        
        return family + "," + module + "," + product + "," + pathAfterFusionApps + "," + label + ",";
        
    }
    
    public static String getFileNameInfo(String absPath,String jsff) throws IOException {
        
      File f = new File(absPath);
      
      if(!isEmpty(jsff)) {
          if(absPath.startsWith("oracle/"))
              return getFileNameInfoFromRelativePath(absPath,jsff);
      }
      if(absPath.contains("/plsql/"))
          return getDbFileNameInfo(f.getCanonicalPath());
      
           
      absPath = f.getCanonicalPath();
      int i = 0;
      String family = "";
      String module = "";
      String product = "";
      String blankString =  "NA,NA,NA,NA,NA,";
      if(isEmpty(absPath))
          return blankString;
      
      if(absPath.contains("fusionapps/")) {
       i = absPath.indexOf("fusionapps/");
      }
      if(absPath.contains("fsm/")){
       i = absPath.indexOf("fsm/");
       family = "FSM";
      }
        if (absPath.contains("atgpf/")) {
            i = absPath.indexOf("atgpf/");
            family = "ATGPF";
        }
        if (absPath.contains("emcore/")) {
            i = absPath.indexOf("emcore/");
            family = "EMGC";
            module = "EMCORE";
        }
        
      String pathAfterFusionApps = absPath.substring(i);
      String[] parts = pathAfterFusionApps.split("/");
      
      if(parts == null) return blankString;
      
      if(absPath.contains("fusionapps/")) {
          int partsLength = parts.length;
          if(partsLength>1)
            family = parts[1].trim().toUpperCase();  
          if(partsLength>3)
            module = parts[3].trim();
          if(partsLength>4)
            product = parts[4].trim().toUpperCase();
          
      }
      if(absPath.contains("fsm/")) {
        int partsLength = parts.length;
        if(partsLength>2)
          module = parts[2].trim();
        if(partsLength>3)
          product = parts[3].trim().toUpperCase();
      }
      
        if (absPath.contains("atgpf/")) {
            int partsLength = parts.length;
            if (partsLength > 1)
                module = parts[1].trim();
            if (partsLength > 3)
                product = parts[3].trim().toUpperCase();
        }
        if (absPath.contains("emgc/")) {
            int partsLength = parts.length;
            if (partsLength > 1)
                product = parts[1].trim();
        }
         
      if(i<1)
         return family + "," + module + "," + product + "," + pathAfterFusionApps + ",," ;
      
      String path_before_fusionapps = absPath.substring(0, i-1);
      parts = path_before_fusionapps.split("/");
      if(parts==null || parts.length<2)
        return blankString;
     
      String label = parts[parts.length -2] + "_" + parts[parts.length -1];      
      label = label.replace(".rdd","");   
      
      return family + "," + module + "," + product + "," + pathAfterFusionApps + "," + label + ",";
    }
    
    
    public static String getFileNameInfo1(String absPath) throws IOException {
        
      File f = new File(absPath);           
      absPath = f.getCanonicalPath();
      int i = 0;
      String family = "";
      String module = "";
      String product = "";
      String blankString =  "NA,NA,NA,NA,NA,";
      if(isEmpty(absPath))
          return blankString;
      
      if(absPath.contains("fusionapps/")) {
       i = absPath.indexOf("fusionapps/");
      }
      if(absPath.contains("fsm/")){
       i = absPath.indexOf("fsm/");
       family = "FSM";
      }
        if (absPath.contains("atgpf/")) {
            i = absPath.indexOf("atgpf/");
            family = "ATGPF";
        }
        if (absPath.contains("emcore/")) {
            i = absPath.indexOf("emcore/");
            family = "EMGC";
            module = "EMCORE";
        }
        
      String pathAfterFusionApps = absPath.substring(i);
      String[] parts = pathAfterFusionApps.split("/");
      
      if(parts == null) return blankString;
      
      if(absPath.contains("fusionapps/")) {
          int partsLength = parts.length;
          if(partsLength>1)
            family = parts[1].trim().toUpperCase();  
          if(partsLength>3)
            module = parts[3].trim();
          if(partsLength>4)
            product = parts[4].trim().toUpperCase();
          
      }
      if(absPath.contains("fsm/")) {
        int partsLength = parts.length;
        if(partsLength>2)
          module = parts[2].trim();
        if(partsLength>3)
          product = parts[3].trim().toUpperCase();
      }
      
        if (absPath.contains("atgpf/")) {
            int partsLength = parts.length;
            if (partsLength > 1)
                module = parts[1].trim();
            if (partsLength > 3)
                product = parts[3].trim().toUpperCase();
        }
        if (absPath.contains("emgc/")) {
            int partsLength = parts.length;
            if (partsLength > 1)
                product = parts[1].trim();
        }
         
      if(i<1)
         return family + "," + module + "," + product + "," + pathAfterFusionApps + ",," ;
      
      String path_before_fusionapps = absPath.substring(0, i-1);
      parts = path_before_fusionapps.split("/");
      if(parts==null || parts.length<2)
        return blankString;
     
      String series = parts[parts.length -2];
      series = series.replace(".rdd","");   
      String label = parts[parts.length -1]; 
      
      return family + "," + module + "," + product + "," + pathAfterFusionApps + "," + series + "," + label + ",";
    }
    
    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }
    
    public static String getFileName(String absPath, String jsff) throws Exception{
        
        String result = getFileNameInfo(absPath,jsff);
        String[] parts = result.split(",");
        if(parts.length>3)
        return parts[3].trim();
        else
            return getFileName(absPath);
    }
    
    public static String getFileName(String absPath) {
        
        if(isEmpty(absPath))
            return absPath;
        File f = new File(absPath);
        try{
         if(f.exists())
            absPath = f.getCanonicalPath();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        int i = 0;
        if(absPath.contains("fusionapps/")) 
         i = absPath.indexOf("fusionapps/");
        
        if(absPath.contains("fsm/"))
         i = absPath.indexOf("fsm/");
        
        if(absPath.contains("atgpf/"))
         i = absPath.indexOf("atgpf/");
        
        if(absPath.contains("emcore/"))
         i = absPath.indexOf("emcore/");
        
        String pathAfterFusionApps = absPath.substring(i);
        
        return pathAfterFusionApps;
    }
    
    public static String getFileNames(String path) {
        if(isEmpty(path))
            return path;
        
        String[] parts = path.trim().split(" ");
        String returnpath = "";
        for(int i = 0; i < parts.length; i++) {
            String path1 = parts[i];
            returnpath += getFileName(path1) + " ";
        }
        return returnpath;
    }
    
    public static String getFamily(String sCrawlDir) {
        
      String family = "fin"; // default value if it cannot be determined
      int i = -1;
      
      if(sCrawlDir.contains("fsm/")) {
       i = sCrawlDir.indexOf("fsm/");
       family = "fsm";
      } 
      else if(sCrawlDir.contains("atgpf/")) {
       i = sCrawlDir.indexOf("atgpf/");
       family = "atgpf";
      } 
      else if(sCrawlDir.contains("emcore/")) {
       i = sCrawlDir.indexOf("emcore/");
       family = "emgc";
      } 
      else if(sCrawlDir.contains("fusionapps/")) {
          
        i = sCrawlDir.indexOf("fusionapps/");    
        if(i != -1) {
          String pathAfterFusionApps = sCrawlDir.substring(i);
          String[] parts = pathAfterFusionApps.split("/");
          if(parts.length > 1)
            family = parts[1].trim();  
        }
      }
      return family.toUpperCase();
    }
    
    public static String getViewName(String absPath) {
        
        if(absPath == null || absPath.trim().equals(""))
            return "";
        
        int index = absPath.trim().lastIndexOf("/");
           
        return absPath.trim().substring(index+1);
    }
    
    public static String getViewRoot(String curDir) {
        
        String path_before_fusionapps = "";
        if (curDir.contains("/fusionapps")) {
                int i = curDir.indexOf("/fusionapps") + 1;
                path_before_fusionapps = curDir.substring(0,i);                
        }
        else if (curDir.contains("/fsm")) {
                int i = curDir.indexOf("/fsm") + 1;
                path_before_fusionapps = curDir.substring(0,i);                
        }
        else if (curDir.contains("/atgpf")) {
                int i = curDir.indexOf("/atgpf") + 1;
                path_before_fusionapps = curDir.substring(0,i);                
        }
        else if (curDir.contains("/emcore")) {
                int i = curDir.indexOf("/emcore") + 1;
                path_before_fusionapps = curDir.substring(0,i);                
        }
        return path_before_fusionapps;
    }
    
    public static String getAMNameFromDataControlName(String datacontrolName) {
        if (isEmpty(datacontrolName)) {
            return null;
        }
        String amName = "";
        int lastindexDc = datacontrolName.lastIndexOf("DataControl");
        if (lastindexDc != -1) {
            amName= datacontrolName.substring(0, lastindexDc);
            
            if(!amName.endsWith("AM")){ 
              System.out.println("DataControl Name: " + datacontrolName);
              lastindexDc = datacontrolName.lastIndexOf("AM");
              if (lastindexDc != -1) 
                  return datacontrolName.substring(0, lastindexDc+2);                    
            } 
            return amName;
        }        
        return null;
    }
    
    
    public static String getDataControlPackage(String dataControl,String cpx) {
        String dcPackage = "";
        
        XMLDocument doc = XMLParserHelper.getXMLDocument(cpx);
        if(doc == null)
            return dcPackage;
        
        NodeList datacontrolNodes =
            doc.getElementsByTagName("dataControlUsages");
        if (!XMLParserHelper.hasOneNOnlyOneNode(datacontrolNodes)) {
            if(bDebug)
            {
                System.out.println("Could not find any dataControlUsages tag in databindings file: " +
                               cpx);
            }
            return dcPackage;
        }
        Node dcUsagesNode = datacontrolNodes.item(0);
        NodeList dcChildren = dcUsagesNode.getChildNodes();
        int dcChildLen = dcChildren.getLength();

        for (int j = 0; j < dcChildLen; j++) {
            Node bc4jControl = dcChildren.item(j);
            String nodeName = bc4jControl.getNodeName();
            if (nodeName != null &&
                nodeName.trim().equals("BC4JDataControl")) {
                String dcUsageId = XMLParserHelper.getAttributeValue(bc4jControl, "id");
                String dcUsagePackage = XMLParserHelper.getAttributeValue(bc4jControl, "Package");
                if (!isEmpty(dcUsageId) && dcUsageId.equals(dataControl)) 
                   return dcUsagePackage;            
            }
        }       
        return dcPackage;
    }
    
    public static String getAmFile(String amFullPackage,String uiAbsPath) {
        File amFile = null;

        String amPathToFind = amFullPackage.replaceAll("\\.", "/");
        amPathToFind = amPathToFind + ".xml";
        
        String baseLbaPath = UiFileHelper.getBaseLBAPath(uiAbsPath);

        String uiJprFullPath = UiFileHelper.getJprLocationForUiFile(uiAbsPath);
        if (uiJprFullPath != null) {
            HashSet<String> modelDependencyJars = JprHelper.getProjectDependencysInJar(uiJprFullPath);

            if (modelDependencyJars != null) {
                Iterator<String> modelJprIterator = modelDependencyJars.iterator();
                while (modelJprIterator.hasNext()) {
                    String modelJprLocation = modelJprIterator.next();
                    File modelFile = new File(modelJprLocation);
                    if (modelFile.exists()) {
                        String modelDir = getPreviousDirectory(modelJprLocation);
                        amFile = findBCInDirectory(modelDir, amPathToFind);
                        if (amFile != null) {
                            return amFile.getAbsolutePath();
                        }
                    }
                }

            }
        } else {
            return null;
        }

        if (isEmpty(baseLbaPath)) {
          if (bDebug)
            System.out.println("ERROR: Base UI Path is empty.. skipping file:");
            return null;
        }
        ArrayList<String> modelDirs = getListOfModelDirectoriesInPath(baseLbaPath);

        for (int i = 0; i < modelDirs.size(); i++) {
            String modelDir = modelDirs.get(i);
            amFile = findBCInDirectory(modelDir, amPathToFind);
            if (amFile != null && amFile.exists()) {
                return amFile.getAbsolutePath();

            }
        }

        ArrayList<String> modelDirsInPrevDir = getListOfAllModelDirsInPrevDir(baseLbaPath);
        if (modelDirsInPrevDir != null && modelDirsInPrevDir.size() > 0) {
            int modeldirsinDirSize = modelDirsInPrevDir.size();
            for (int i = 0; i < modeldirsinDirSize; i++) {
                String modelDirInDir = modelDirsInPrevDir.get(i);
                amFile = findBCInDirectory(modelDirInDir, amPathToFind);
                if (amFile != null && amFile.exists()) {
                    return amFile.getAbsolutePath();

                }
            }
        }


        String msg =
            String.format("[ERROR] Could not find am in the probable directories.. AM: %s, UIFile: %s.. will look at dependencies in ui jpr now.. however during modifications will not be possible to modify this am...  ",
                          amPathToFind, uiAbsPath);
        if (bDebug) {
            System.out.println(msg); 
        }
        if (isEmpty(uiJprFullPath)) {
            System.out.println("Could not find jpr for ui file: " + uiAbsPath);
            return null;
        }

        return null;

    }
    
    private static ArrayList<String> getListOfAllModelDirsInPrevDir(String baseDir) {
        ArrayList<String> modelDirs = new ArrayList<String>();
        if (isEmpty(baseDir)) {
            return null;
        }

        // am still not found.. then go one directory higher
        String prevDir = getPreviousDirectory(baseDir);
        if (isEmpty(prevDir)) {
            return null;
        }

        ArrayList<String> alldirs = getListOFAllValidDirectoriesInPath(prevDir);
        if (alldirs != null) {
            Iterator<String> allDirsIter = alldirs.iterator();
            while (allDirsIter.hasNext()) {
                String dir = allDirsIter.next();
                ArrayList<String> modeDirsInDir = getListOfModelDirectoriesInPath(dir);
                modelDirs.addAll(modeDirsInDir);
            }
        } else {
            return null;
        }
        return modelDirs;
    }
    
    private static ArrayList<String> getListOFAllValidDirectoriesInPath(String baseDir) {
        if (isEmpty(baseDir)) {
            return null;
        }
        File f = new File(baseDir);
        ArrayList<String> list_allDirs = new ArrayList<String>();
        if (f.exists()) {
            File[] files = f.listFiles();
            int fileLen = files.length;
            for (int i = 0; i < fileLen; i++) {
                File file = files[i];
                String absPath = file.getAbsolutePath();

                if (file.isDirectory() && !absPath.contains(".ade_path"))
                    list_allDirs.add(absPath);
            }

        }
        return list_allDirs;
    }
    
    private static ArrayList<String> getListOfModelDirectoriesInPath(String baseDir) {
        if (isEmpty(baseDir)) {
            return null;
        }
        File f = new File(baseDir);
        ArrayList<String> list_modeDir = new ArrayList<String>();
        if (f.exists()) {
            File[] files = f.listFiles();
            int fileLen = files.length;
            for (int i = 0; i < fileLen; i++) {
                File file = files[i];
                String absPath = file.getAbsolutePath();

                if (file.isDirectory() && isValidModelDir(absPath))
                    list_modeDir.add(absPath);
            }

        }
        return list_modeDir;
    }
    
    private static boolean isValidModelDir(String dirPath) {
        if (isEmpty(dirPath)) {
            return false;
        }

        int lastSlash = dirPath.lastIndexOf("/");
        String lastDir = "nothing";
        if (lastSlash != -1) {
            lastDir = dirPath.substring(lastSlash);
            lastDir = lastDir.toLowerCase();
        }

        if (dirPath.contains(".ade_path") || lastDir.contains("dbschema") || lastDir.endsWith("test")) {
            return false;
        }

        if ((lastDir.contains("ui") || lastDir.contains("publicui")) && !lastDir.contains("model")) {
            return false;
        }
        if ((lastDir.contains("di") || lastDir.contains("publicdi")) && !lastDir.contains("model")) {
            return false;
        }

        return true;
    }

    
    private static File findBCInDirectory(String modelDir, String amPath) {
        String amFullPAth = modelDir + "/src/" + amPath;
        File f = new File(amFullPAth);
        if (f.exists()) {
            return f;
        }
        return null;
    }

    private static String getPreviousDirectory(String path) {
        if (!isEmpty(path)) {
            int indexLastSlash = path.lastIndexOf("/");
            if (indexLastSlash != -1) {
                return path.substring(0, indexLastSlash);
            }
        }
        return null;
    }
    
    public static String getViewUsagePackageFromAm(String amAbsPath, String viewUsageName, ArrayList<String> viewUsageVCs) throws Exception {
        
        if (isEmpty(amAbsPath)) 
           throw new Exception("Am abs path was null");            
        
        XMLDocument amXml = XMLParserHelper.getXMLDocument(amAbsPath);
        NodeList appmoduleNodeList = amXml.getElementsByTagName("AppModule");
        
        if (!XMLParserHelper.hasOneNOnlyOneNode(appmoduleNodeList)) 
           throw new Exception("Expecting one and only one AppModule node in the amxml");        

        Node appModuleNode = appmoduleNodeList.item(0);
        
        Node viewUsageNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(appModuleNode, "ViewUsage", "Name", viewUsageName);
        if(viewUsageNode == null)
           throw new Exception(String.format("View usage tag not found in am %s:%s", amAbsPath,viewUsageName));
        
        NodeList VUChildren = viewUsageNode.getChildNodes();
        for(int j =0; j < VUChildren.getLength();j++){
            Node VUChild = VUChildren.item(j);
            if(VUChild != null && !isEmpty(VUChild.getNodeName()) && VUChild.getNodeName().equals("ViewCriteriaUsage"))
                viewUsageVCs.add(XMLParserHelper.getAttributeValue(VUChild, "Name"));
        }
        return XMLParserHelper.getAttributeValue(viewUsageNode, "ViewObjectName");
        
        
       /* NodeList appModuleChildren = appModuleNode.getChildNodes();
        if (appModuleChildren == null) 
            throw new Exception(String.format("View usage tag not found in am %s", amAbsPath));
        
        int appModuleChildLen = appModuleChildren.getLength();
        
        for (int i = 0; i < appModuleChildLen; i++) {
            Node appModuleChild = appModuleChildren.item(i);
            String tagName = appModuleChild.getNodeName();
            if (!isEmpty(tagName) && tagName.trim().equals("ViewUsage")) {
                
                String obtainedVoUsageName = XMLParserHelper.getAttributeValue(appModuleChild, "Name");

                if (!isEmpty(obtainedVoUsageName) && obtainedVoUsageName.trim().equals(viewUsageName)) {
                    
                   NodeList VUChildren = appModuleChild.getChildNodes();
                   for(int j =0; i < VUChildren.getLength();j++){
                       Node VUChild = VUChildren.item(j);
                       if(VUChild != null && !isEmpty(VUChild.getNodeName()) && VUChild.getNodeName().equals("ViewCriteriaUsage"))
                           viewUsageVCs.add(XMLParserHelper.getAttributeValue(VUChild, "Name"));
                   }
                   return XMLParserHelper.getAttributeValue(appModuleChild, "ViewObjectName");
                }
                
            }
        } */
        //return null;
    }
    
    public static Object[] getViewObject(String modelJprLocation, String voFullPackage, String jsff) throws Exception {
        if (isEmpty(voFullPackage)) {
            if (bDebug) 
                System.out.println("[ERROR] getViewObject: VO package empty");
            return new Object[]{null,null};
        }
        
        XMLDocument voXml = null;
        String bcPathToFind = voFullPackage.replaceAll("\\.", "/");
        bcPathToFind = bcPathToFind + ".xml";

        String bcPath = getBCObjectPath(modelJprLocation, voFullPackage, jsff);

        if (!isEmpty(bcPath)) {
            return new Object[]{XMLParserHelper.getXMLDocument(bcPath),bcPath};
        }


        // Search location 4 - in all dependent model jars
        // If it reaches this stage means it did not find the view object in any of the most probable directories..
       // now have to scan all the referenced jars in the jpr

        if (isEmpty(modelJprLocation)) {
            if (bDebug) 
               System.out.println("Could not obtain jpr");
            return new Object[]{null,null};
        }
        HashSet<String> adfLibrarys = JprHelper.getAllAdfLibrarysInJpr(modelJprLocation, true);
        if (adfLibrarys != null && adfLibrarys.size() > 0) {
            Iterator<String> libraryIter = adfLibrarys.iterator();
            while (libraryIter.hasNext()) {
                String lib = libraryIter.next();
                JarFile jar = null;
                try {
                    jar = new JarFile(lib);

                    if (jar != null) {

                        JarEntry voEntry = jar.getJarEntry(bcPathToFind);

                        if (voEntry != null) {
                            if (bDebug) {
                                System.out.println("Found the vo in: " + jar.getName());
                            }
                            InputStream contents = jar.getInputStream(voEntry);
                            voXml = XMLParserHelper.getXMLDocument(contents);
                            bcPath = bcPathToFind;
                            if (contents != null) {
                                contents.close();
                            }
                            break;
                        }
                    }
                } catch (IOException ex) {
                    String msg = "[ERROR]:getViewObject: IOException: " + ex.getMessage()+" lib file: " + lib;
                    if (bDebug) 
                      System.out.println(msg); 
                    continue;
                }
            }
        }

        HashSet<String> adfbcImports = JprHelper.getAllBcImportsInJpr(modelJprLocation, true);
        if (adfbcImports != null && adfbcImports.size() > 0) {
            Iterator<String> libraryIter = adfbcImports.iterator();
            while (libraryIter.hasNext()) {
                String lib = libraryIter.next();
                JarFile jar = null;
                try {
                    jar = new JarFile(lib);

                    if (jar != null) {

                        JarEntry voEntry = jar.getJarEntry(bcPathToFind);

                        if (voEntry != null) {
                            if (bDebug) 
                                System.out.println("Found the vo in: " + jar.getName());
                            InputStream contents = jar.getInputStream(voEntry);
                            voXml = XMLParserHelper.getXMLDocument(contents);
                            bcPath = bcPathToFind;
                            if (contents != null) {
                                contents.close();
                            }
                            break;
                        }
                    }
                } catch (IOException ex) {
                    String msg = "[ERROR]:getViewObject: IOException: " + ex.getMessage();
                    if (bDebug)
                       System.out.println(msg); 
                    continue;
                }
            }
        }
        return new Object[]{voXml,bcPath};
    } 
    
    private static String getBCObjectPath(String baseJprLocation, String bcFullPackage, String uiAbsPath) throws Exception {
        if (isEmpty(bcFullPackage)) {
            String msg = "[getBCObjectPath] bcFullPackage was empty";
             if(bDebug)
                System.out.println(msg);    
            throw new Exception(msg);
        }

        String bcPathToFind = bcFullPackage.replaceAll("\\.", "/");
        bcPathToFind = bcPathToFind + ".xml";
        
        String baseLbaPath = UiFileHelper.getBaseLBAPath(uiAbsPath);

        //Search location 1 - in the same base directory that the am is in
        if (!isEmpty(baseJprLocation)) {
            String modelBaseDir = getPreviousDirectory(baseJprLocation);
            File voFile = findBCInDirectory(modelBaseDir, bcPathToFind);
            if (voFile != null) {
                return voFile.getAbsolutePath();
            }
        }

        //Search location 2 - in all valid model directories in the base lba path
        ArrayList<String> modelDirslist = getListOfModelDirectoriesInPath(baseLbaPath);
        if (modelDirslist == null || modelDirslist.size() <= 0) {
            // System.out.println("[INFO] Could not obtain any valid model dirs");
        } else {
            for (int i = 0; i < modelDirslist.size(); i++) {
                String modelDir = modelDirslist.get(i);
                File bcFile = findBCInDirectory(modelDir, bcPathToFind);
                if (bcFile != null) {
                    return bcFile.getAbsolutePath();
                }
            }
        }

        // Search location 3 - in all valid model directories within the previous directory
        ArrayList<String> modelDirsInPrevDir = getListOfAllModelDirsInPrevDir(baseLbaPath);
        if (modelDirsInPrevDir != null && modelDirsInPrevDir.size() > 0) {
            int modeldirsinDirSize = modelDirsInPrevDir.size();
            for (int i = 0; i < modeldirsinDirSize; i++) {
                String modelDirInDir = modelDirsInPrevDir.get(i);
                File voFile = findBCInDirectory(modelDirInDir, bcPathToFind);
                if (voFile != null && voFile.exists()) {
                    return voFile.getAbsolutePath();
                }
            }
        }

    
        /* Search location 4 - try to look for shared vos etc
         * Drawback only single workspace lookups
         * From the BC package get the part between oracle.apps.financials && (view || publicView) ; This should give an idea of the location
         * e.g. oracle.apps.financials.receivables.customers.uiModel.view.CustomerProfileUIVO
         * So here we would extract customers/uiModel
         * From the ui file path get the product e.g. receivables/ar
         * put the two together e.g fusionapps/fin/components/receivables/ar/customers/uiModel
         * 
         * if directory does not exist go one level up and find all model directorys in there.
         * else if directory exists look for bc in that directory
         * */
        String beforeintelligentDir = "";
        if (uiAbsPath.contains("/components/")) {
            String baseProdPath = "/components/";
            int index = uiAbsPath.indexOf(baseProdPath);
            beforeintelligentDir = uiAbsPath.substring(0, index + baseProdPath.length());
            String afterPath = uiAbsPath.substring(index + baseProdPath.length());
            String[] parts = afterPath.split("/");

            if (parts != null && parts.length >= 2) {
                beforeintelligentDir = beforeintelligentDir + parts[0] + "/" + parts[1];
            } else {
                return "";
            }
        }
        String intelligentBcPath = "";
        if (bcFullPackage.contains("oracle.apps.")) {
            intelligentBcPath = bcFullPackage.replaceFirst("oracle\\.apps\\..*?\\.", "");
            int firstPackageSepInd = intelligentBcPath.indexOf(".");
            if (firstPackageSepInd == -1)
                return "";
           
            if (intelligentBcPath.length() < firstPackageSepInd + 2)
                return "";
            intelligentBcPath = intelligentBcPath.substring(firstPackageSepInd + 1);
            // means we are lookig for a vo
            if (intelligentBcPath.contains(".view.")) {
                int viewIndex = intelligentBcPath.indexOf(".view");
                intelligentBcPath = intelligentBcPath.substring(0, viewIndex);
                intelligentBcPath = intelligentBcPath.replaceAll("\\.", "/");

                String intelligentBcFilePath = beforeintelligentDir + "/" + intelligentBcPath;
                File intelligentBcFile = new File(intelligentBcFilePath);
                if (!intelligentBcFile.exists()) {
                    // if it does not exist, go one directory up and find all model classes in this directory
                    String prevDirintelligentDir = getPreviousDirectory(intelligentBcFilePath);
                    intelligentBcFile = new File(prevDirintelligentDir);
                    if (!intelligentBcFile.exists())
                        return "";
                    ArrayList<String> trialModelDirs = getListOFAllValidDirectoriesInPath(prevDirintelligentDir);
                    if (trialModelDirs != null && trialModelDirs.size() > 0) {
                        int modeldirsinDirSize = trialModelDirs.size();
                        for (int i = 0; i < modeldirsinDirSize; i++) {
                            String modelDirInDir = trialModelDirs.get(i);
                            File voFile = findBCInDirectory(modelDirInDir, bcPathToFind);
                            if (voFile != null && voFile.exists()) {
                                return voFile.getAbsolutePath();
                            }
                        }
                    }
                    return "";
                }
                if (isValidModelDir(intelligentBcFile.getAbsolutePath())) {
                    File voFile = findBCInDirectory(intelligentBcFile.getAbsolutePath(), bcPathToFind);
                    if (voFile != null && voFile.exists()) {
                        return voFile.getAbsolutePath();
                    }
                }
            }
        }


        return "";
    }
    
    public static void parsePageDef(String pageDefPath,HashMap<String,Node> iteratorNodes, HashMap<String,Node> listNodes,
                                       HashMap<String,Node> treeNodes) {
        if (isEmpty(pageDefPath)) {
            return;
        }

        XMLDocument doc = XMLParserHelper.getXMLDocument(pageDefPath);
        NodeList executableNodes = doc.getElementsByTagName("executables");
        
        if (XMLParserHelper.hasOneNOnlyOneNode(executableNodes)) {
            
            Node executableNode = executableNodes.item(0);
            NodeList executableChildList = executableNode.getChildNodes();
            
            if (executableChildList != null && executableChildList.getLength() > 0) {

                for (int i = 0; i < executableChildList.getLength(); i++) {
                    Node executableChild = executableChildList.item(i);
                    String nodeName = executableChild.getNodeName();
                    if (!isEmpty(nodeName) && nodeName.trim().toLowerCase().endsWith("iterator")) 
                        iteratorNodes.put(XMLParserHelper.getAttributeValue(executableChild, "id"),executableChild);
                    
                }
            }
        }

        NodeList bindingsNodes = doc.getElementsByTagName("bindings");
        if (XMLParserHelper.hasOneNOnlyOneNode(bindingsNodes)) {
            
            Node bindingNode = bindingsNodes.item(0);
            NodeList bindingsChildList = bindingNode.getChildNodes();
            if (bindingsChildList != null && bindingsChildList.getLength() > 0) {
           
                int childLen = bindingsChildList.getLength();
                for (int i = 0; i < childLen; i++) {
                    
                    Node bindingChild = bindingsChildList.item(i);
                    if (XMLParserHelper.isNodeName(bindingChild, "tree")) 
                        treeNodes.put(XMLParserHelper.getAttributeValue(bindingChild, "id"), bindingChild);
                    else if (XMLParserHelper.isNodeName(bindingChild, "listOfValues") ||
                               XMLParserHelper.isNodeName(bindingChild, "list") || 
                             XMLParserHelper.isNodeName(bindingChild, "navigationlist")) 
                        listNodes.put(XMLParserHelper.getAttributeValue(bindingChild, "id"), bindingChild);                    
                }
            }
        }
    }
    
    public static String extractIteratorNameFromEL(String elExp) {
        String extractedValue = "";
        if (isEmpty(elExp)) {
            return extractedValue;
        }
        if (elExp.startsWith("#{bindings.")) {
            int indexbindings = elExp.indexOf("#{bindings.");
            int startPos = indexbindings + "#{bindings.".length();
            int indexNextDot = elExp.indexOf(".", startPos);
            if (startPos != -1 && indexNextDot != -1) {
                String iteratorId = elExp.substring(startPos, indexNextDot);
                //System.out.println("IterName is : " + iteratorId);
                return iteratorId;
            } else {
                  if (bDebug)
                    System.out.println("[ERROR] Could not find iter name from bindings");
            }
        } else if (elExp.contains(".bindings.")) {
            int indexbindings = elExp.indexOf(".bindings.");
            int startPos = indexbindings + ".bindings.".length();
            int indexNextDot = elExp.indexOf(".", startPos);
            if (startPos != -1 && indexNextDot != -1) {
                String iteratorId = elExp.substring(startPos, indexNextDot);
                //System.out.println("IterName is : " + iteratorId);
                return iteratorId;
            } else {
                if (bDebug)
                   System.out.println("[ERROR] Could not find iter name from bindings");
            }
        } else if (elExp.startsWith("#{")) {
            int indexbindings = elExp.indexOf("#{");
            int startPos = indexbindings + "#{".length();
            int indexfirstDot = elExp.indexOf(".", startPos);
            startPos = indexfirstDot + ".".length();
            int endPos = elExp.indexOf(".", startPos);
            if (startPos != -1 && endPos != -1) {
                String iteratorId = elExp.substring(startPos, endPos);
                if (bDebug) {
                    System.out.println("IterName is : " + iteratorId + ".. elexp is: " + elExp);
                }
                return iteratorId;
            }
        } else {
            if (bDebug) 
              System.out.println("Not a valid EL Exp.. should start with #{");
        }
        return extractedValue;
    }

    public static boolean isRowLevelBinding(String possibleEL) {
        if (!isEmpty(possibleEL) && possibleEL.trim().startsWith("#{row.")) {
            return true;
        }
        return false;
    }
    
    public static String getTableIteratorIdForRow(Node componentNode) {
        String iteratorId = "";
        Node traversalNode = componentNode;
        while (true) {
            Node parentNode = traversalNode.getParentNode();
            String parentNodeName = parentNode.getNodeName();
            if (isEmpty(parentNodeName)) {
                break;
            }
            if (parentNodeName.equals("af:table") || parentNodeName.equals("af:treeTable")
                || parentNodeName.equals("af:iterator")) {
                String modelValue = XMLParserHelper.getAttributeValue(parentNode, "value");
                iteratorId = extractIteratorNameFromEL(modelValue);
                break;
            }
            if (parentNodeName.equals("jsp:root")) {
                break;
            }
            traversalNode = parentNode;
        }

        return iteratorId;
    }
    
    public static String getListVOName(XMLDocument voXml, String dataSourceName, String voPath, String jsff, ArrayList<String> viewAccessorvcUsages) throws Exception {
        
        if (voXml == null) {
            throw new Exception("Could not obtain vo xml");
        }
        XMLDocument vaXml = voXml;
        boolean isEoBasedVA = false;
        String viewAccessorName = dataSourceName;
        String bcPath = voPath;
        if (isEOBasedVA(dataSourceName)) {
            if (bDebug) {
                System.out.println("EOBAsed VA !!");
            }
            isEoBasedVA = true;
            String[] dataSourceParts = dataSourceName.split("\\.");
            if (dataSourceParts == null || dataSourceParts.length < 2) {
                throw new Exception("Not an EOBased VO.. strange.. dont know what this is: " + dataSourceName + " vo name: " + voPath);
            }
            String eoName = dataSourceParts[0];
            viewAccessorName = dataSourceParts[1];
            String eoPackage = getEOUsageFromVOXml(voXml, eoName);
            //String amPath = record.getAmPath();
            String modelJprLocation = UiFileHelper.getJprLocationForModelFile(voPath);
            String eoPath = getBCObjectPath(modelJprLocation, eoPackage, jsff);
            //vaXml = getViewObject(amPath, eoPackage, record, false);
            if (isEmpty(eoPath)) {
                throw new Exception("could not get entity for view accessor: " + eoName + ", va" + viewAccessorName);
            }
            bcPath = eoPath;
            vaXml = XMLParserHelper.getXMLDocument(bcPath);
            if (vaXml == null) 
                throw new Exception(String.format("Could not obtain eo xml. eo Path %s, vo " +
                    "Path that contains listBinding %s, ui file %s ", eoPath, voPath, jsff));
        }

        Node vaParentNode = null;
        if (isEoBasedVA) {
            NodeList listViewObj = vaXml.getElementsByTagName("Entity");
            if (!XMLParserHelper.hasOneNOnlyOneNode(listViewObj)) 
                throw new Exception("Expecting one and only one view object node in vo file: " + voPath);
            
            vaParentNode = listViewObj.item(0);
        } else {
            NodeList listViewObj = vaXml.getElementsByTagName("ViewObject");
            if (!XMLParserHelper.hasOneNOnlyOneNode(listViewObj)) 
                throw new Exception("Expecting one and only one view object node in vo file: " + voPath);
            
            vaParentNode = listViewObj.item(0);
        }

        Node viewAccessorNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(vaParentNode, "ViewAccessor", "Name", viewAccessorName);
        if (viewAccessorNode == null) 
            throw new Exception(String.format("Could not obtain view accessor with name %s in vo: %s", viewAccessorName, bcPath));
        
        String viewObjectName = XMLParserHelper.getAttributeValue(viewAccessorNode, "ViewObjectName");
        
        NodeList VAChildren = viewAccessorNode.getChildNodes();
        for(int i = 0; i < VAChildren.getLength();i++) {
            Node VAChild = VAChildren.item(i);
            if(!isEmpty(VAChild.getNodeName()) && VAChild.getNodeName().equals("ViewCriteriaUsage"))
                viewAccessorvcUsages.add(XMLParserHelper.getAttributeValue(VAChild, "Name"));
        }
       
        return viewObjectName;

    }

    public static boolean isEOBasedVA(String dataSourceName) {
        if (!isEmpty(dataSourceName) && dataSourceName.contains(".")) {
            return true;
        }
        return false;
    }

    public static String getEOUsageFromVOXml(XMLDocument voXml, String EOName) throws Exception {
        if (voXml == null) {
            throw new Exception("Could not obtain vo xml");
        }
        NodeList listViewObj = voXml.getElementsByTagName("ViewObject");
        // should never happen
        if (!XMLParserHelper.hasOneNOnlyOneNode(listViewObj)) {
            // System.out.println("Expecting one and only one view object node");
            throw new Exception("Expecting one and only one view object node in vo file: ");
        }
        Node voNode = listViewObj.item(0);
        Node eoUsageNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(voNode, "EntityUsage", "Name", EOName);
        if (eoUsageNode == null) {
            return null;
        }
        return XMLParserHelper.getAttributeValue(eoUsageNode, "Entity");

    }
    
    public static void getNumRowsBlocks(String indexedTableName, String resolvedTableColumn, String table_name, StringBuffer numRows, 
                                        StringBuffer numBlocks, StringBuffer tableOnWhichRowsDetermined, HashSet<String> exemptedTables) throws Exception{
        
        if(!isEmpty(indexedTableName)) {
            int rows = 0;
            int blocks = 0;
            try{
             rows = Integer.parseInt(SqlIndexFinder.tableRows.get(indexedTableName.trim()));
             blocks = Integer.parseInt(SqlIndexFinder.tableBlocks.get(indexedTableName.trim()));
            }catch(NumberFormatException ex) {
                rows = 0;
                blocks = 0;
            }
            numRows.append(rows);
            numBlocks.append(blocks);
            tableOnWhichRowsDetermined.append(indexedTableName);
            return;
        }
        if(!isEmpty(resolvedTableColumn)) {
            Matcher m = pTablesColumn.matcher(resolvedTableColumn);
            if(m.find())  
                table_name = m.group(1);   
        }
        
        getNumRowsBlocks(table_name,numRows,numBlocks,tableOnWhichRowsDetermined,exemptedTables);
        
    }
    
    public static void getNumRowsBlocks(String table_name, StringBuffer numRows, StringBuffer numBlocks, 
                                        StringBuffer tableOnWhichRowsDetermined, HashSet<String> exemptedTables) throws Exception{
        
        int largestNumRows = 0;
        int largestNumBlocks = 0;
        String largestTable = "";
        int tempRows = 0;
        int tempBlocks = 0;
        
        if(table_name == null || table_name.trim().equals(""))
            return;
         String[] parts = table_name.split(" ");
         
         for(int i = 0; i < parts.length; i++) {
             
             String table = parts[i].trim();
             
             if(SqlIndexFinder.synonyms.containsKey(table))
                 table = SqlIndexFinder.synonyms.get(table);

             if(SqlIndexFinder.viewNameText.containsKey(table))
                 table = SqlIndexFinder.getReferencedTables(table);                 
             
             table_name = table_name.replace(parts[i], table);
            
         } //for
        
         parts = table_name.split(" ");
             
         
         for(int i = 0; i < parts.length; i++) {
           
           String useTableName = parts[i].trim();

             if(useTableName.endsWith("_TL"))
                 continue;
             if(exemptedTables.contains(useTableName))
                 continue;
             try{
              tempRows = Integer.parseInt(SqlIndexFinder.tableRows.get(useTableName.trim()));
              tempBlocks = Integer.parseInt(SqlIndexFinder.tableBlocks.get(useTableName.trim()));
             }catch(NumberFormatException ex) {
                 tempRows = 0;
                 tempBlocks = 0;
             }
             if(tempRows >= largestNumRows && SqlIndexFinder.tableRows.containsKey(useTableName.trim())) {
                 largestNumRows = tempRows;
                 largestNumBlocks = tempBlocks;
                 largestTable = useTableName;
             }
         }
         
         numRows.append(largestNumRows);
         numBlocks.append(largestNumBlocks);
         tableOnWhichRowsDetermined.append(largestTable);
    }
    
    public static void getMaxRows(String fileName,BufferedWriter writer,String line, 
                                  XMLDocument voXml,HashSet<String> exemptedTables1, HashSet<String> exemptedTables2_maxRows) throws Exception{
        
        StringBuffer numRows = new StringBuffer();
        StringBuffer numBlocks = new StringBuffer();
        StringBuffer tableOnWhichRowsDetermined = new StringBuffer();
        
        if(voXml == null)
            voXml = XMLParserHelper.getXMLDocument(fileName);
        if(voXml == null){
            System.out.println("Could not get VO XML for file:  " +fileName);
            return;
        }
        NodeList viewObjects = voXml.getElementsByTagName("ViewObject");
        if(viewObjects == null || viewObjects.getLength() != 1) 
        {
          System.out.println("WARNING:  Unexpected number of view objects found for file:  " +fileName);
          return;
        }
        Node viewObject = viewObjects.item(0);
        
        HashMap<String,String> tableAliasNames = new HashMap<String,String>();
        String fromList = XMLParserHelper.getAttributeValue(viewObject,"FromList");
        
        if(fromList != null) {        
           SQLParserHelper.processFromPart(fromList,tableAliasNames);
           
        }else {
            Node sql = XMLParserHelper.getChildNodeWithName(viewObject, "SQLQuery");
            if(sql == null)
                return;
            
            String sqlQuery = sql.getTextContent();
            if(sqlQuery == null)
                return;
                
            sqlQuery = sqlQuery.replaceAll("/\\*.*?\\*/", ""); //remove hints and comments
            sqlQuery = sqlQuery.replaceAll("--.*$",""); //remove single line comments            
            
            Matcher m = SQLParserHelper.fromJoinPattern.matcher(sqlQuery);
            if(m.find())
                SQLParserHelper.processFromJoinPart(m,tableAliasNames);
            else{        
              m = SQLParserHelper.fromPattern.matcher(sqlQuery);
              if(!m.find())
                  m = SQLParserHelper.fromOnlyPattern.matcher(sqlQuery);
              else
                  m.reset();       
              
              while(m.find())
                SQLParserHelper.processFromPart(m.group(1),tableAliasNames);                 
            }            
        }
        
        String tables = "";
        for(Iterator<String> it = tableAliasNames.values().iterator();it.hasNext();)
            tables += it.next() + " ";
        
        tables = tables.replace("(","").trim();
        
        if(tables.equals("DUAL"))
            return;
        
        boolean whereContainsBind = false;
        String whereClause = XMLParserHelper.getAttributeValue(viewObject, "Where");
        if(isEmpty(whereClause)){
            Node sql = XMLParserHelper.getChildNodeWithName(viewObject, "SQLQuery");
            if(sql != null) {
                String sqlQuery = sql.getTextContent();
                SQLParserHelper.num_nestedQueriesFound = 0;
                SQLParserHelper.nestedQueries = new HashMap<String,String>(); 
                sqlQuery = sqlQuery.replaceAll("/\\*.*?\\*/", ""); //remove hints and comments
                sqlQuery = sqlQuery.replaceAll("--.*$",""); //remove single line comments      
                
                sqlQuery = SQLParserHelper.removeNestedQueries(sqlQuery); 
                Matcher m = SQLParserHelper.fromPattern.matcher(sqlQuery);
                while(m.find())
                    sqlQuery = sqlQuery.replace(m.group(),"");
                m = SQLParserHelper.selectFromPattern.matcher(sqlQuery);
                while(m.find())
                    sqlQuery = sqlQuery.replace(m.group(),"");
                
                if(sqlQuery.contains(":"))
                    whereContainsBind = true;
                
                if(!whereContainsBind){
                    for(Iterator<String> it = SQLParserHelper.nestedQueries.values().iterator(); it.hasNext();) {
                        String nestedQuery = it.next();
                        nestedQuery = nestedQuery.replaceAll("/\\*.*?\\*/", ""); //remove hints and comments
                        nestedQuery = nestedQuery.replaceAll("--.*$",""); //remove single line comments    
                        
                        Matcher m1 = SQLParserHelper.fromPattern.matcher(nestedQuery);
                        while(m1.find())
                            nestedQuery = nestedQuery.replace(m1.group(),"");
                        m1 = SQLParserHelper.selectFromPattern.matcher(nestedQuery);
                        while(m1.find())
                            nestedQuery = nestedQuery.replace(m1.group(),"");
                        
                        if(nestedQuery.contains(":")){
                            whereContainsBind = true;
                            break;
                        }
                    }
                }
            }      
            
        } else {
            if(whereClause.contains(":"))
                whereContainsBind = true;
        }       
        
        getNumRowsBlocks(tables, numRows, numBlocks, tableOnWhichRowsDetermined, exemptedTables1);
        
        if(tableOnWhichRowsDetermined.toString().contains("LOOKUP"))
            return;        
        if(exemptedTables2_maxRows.contains(tableOnWhichRowsDetermined.toString()))
            return;
          
        if(isEmpty(numBlocks.toString()))
            return;
        
        int blocks = 0;
          try{
            blocks = Integer.parseInt(numBlocks.toString());
          }catch(Exception e){
              e.printStackTrace();
          }
          
          if(blocks < 200)
              return;
        
        if(whereContainsBind)
            return;
        
        writer.write(line + "," + tables + "," + numRows + "," + numBlocks + "," 
                     + tableOnWhichRowsDetermined + "," + whereContainsBind +"\n");
    }
    
    
    public static void getMaxRows1(String fileName,BufferedWriter writer,String line) throws Exception{
        
        StringBuffer numRows = new StringBuffer();
        StringBuffer numBlocks = new StringBuffer();
        StringBuffer tableOnWhichRowsDetermined = new StringBuffer();
        
        XMLDocument voXml = XMLParserHelper.getXMLDocument(fileName);
        if(voXml == null) return;
        
        NodeList viewObjects = voXml.getElementsByTagName("ViewObject");
        if(viewObjects == null || viewObjects.getLength() != 1) 
        {
          System.out.println("WARNING:  Unexpected number of view objects found for file:  " +fileName);
          return;
        }
        Node viewObject = viewObjects.item(0);
        
        HashMap<String,String> tableAliasNames = new HashMap<String,String>();
        String fromList = XMLParserHelper.getAttributeValue(viewObject,"FromList");
        
        if(fromList != null) {        
           SQLParserHelper.processFromPart(fromList,tableAliasNames);
           
        }else {
            Node sql = XMLParserHelper.getChildNodeWithName(viewObject, "SQLQuery");
            if(sql == null)
                return;
            
            String sqlQuery = sql.getTextContent();
            if(sqlQuery == null)
                return;
                
            sqlQuery = sqlQuery.replaceAll("/\\*.*?\\*/", ""); //remove hints and comments
            sqlQuery = sqlQuery.replaceAll("--.*$",""); //remove single line comments            
            
            Matcher m = SQLParserHelper.fromJoinPattern.matcher(sqlQuery);
            if(m.find())
                SQLParserHelper.processFromJoinPart(m,tableAliasNames);
            else{        
              m = SQLParserHelper.fromPattern.matcher(sqlQuery);
              if(!m.find())
                  m = SQLParserHelper.fromOnlyPattern.matcher(sqlQuery);
              else
                  m.reset();       
              
              while(m.find())
                SQLParserHelper.processFromPart(m.group(1),tableAliasNames);                 
            }            
        }
        
        String tables = "";
        for(Iterator<String> it = tableAliasNames.values().iterator();it.hasNext();)
            tables += it.next() + " ";
        
        tables = tables.replace("(","").trim();
        
        if(tables.equals("DUAL"))
            return;
        
        boolean whereContainsBind = false;
        String whereClause = XMLParserHelper.getAttributeValue(viewObject, "Where");
        if(isEmpty(whereClause)){
            Node sql = XMLParserHelper.getChildNodeWithName(viewObject, "SQLQuery");
            if(sql != null) {
                String sqlQuery = sql.getTextContent();
                SQLParserHelper.num_nestedQueriesFound = 0;
                SQLParserHelper.nestedQueries = new HashMap<String,String>(); 
                sqlQuery = sqlQuery.replaceAll("/\\*.*?\\*/", ""); //remove hints and comments
                sqlQuery = sqlQuery.replaceAll("--.*$",""); //remove single line comments      
                
                sqlQuery = SQLParserHelper.removeNestedQueries(sqlQuery); 
                Matcher m = SQLParserHelper.fromPattern.matcher(sqlQuery);
                while(m.find())
                    sqlQuery = sqlQuery.replace(m.group(),"");
                m = SQLParserHelper.selectFromPattern.matcher(sqlQuery);
                while(m.find())
                    sqlQuery = sqlQuery.replace(m.group(),"");
                
                if(sqlQuery.contains(":"))
                    whereContainsBind = true;
                
                if(!whereContainsBind){
                    for(Iterator<String> it = SQLParserHelper.nestedQueries.values().iterator(); it.hasNext();) {
                        String nestedQuery = it.next();
                        nestedQuery = nestedQuery.replaceAll("/\\*.*?\\*/", ""); //remove hints and comments
                        nestedQuery = nestedQuery.replaceAll("--.*$",""); //remove single line comments    
                        
                        Matcher m1 = SQLParserHelper.fromPattern.matcher(nestedQuery);
                        while(m1.find())
                            nestedQuery = nestedQuery.replace(m1.group(),"");
                        m1 = SQLParserHelper.selectFromPattern.matcher(nestedQuery);
                        while(m1.find())
                            nestedQuery = nestedQuery.replace(m1.group(),"");
                        
                        if(nestedQuery.contains(":")){
                            whereContainsBind = true;
                            break;
                        }
                    }
                }
            }      
            
        } else {
            if(whereClause.contains(":"))
                whereContainsBind = true;
        }       
        
        getNumRowsBlocks(tables, numRows, numBlocks, tableOnWhichRowsDetermined,new HashSet<String>());
        
        if(tableOnWhichRowsDetermined.toString().contains("LOOKUP"))
            return;        
                  
        if(isEmpty(numBlocks.toString()))
            return;
        
        int blocks = 0;
          try{
            blocks = Integer.parseInt(numBlocks.toString());
          }catch(Exception e){
              e.printStackTrace();
          }
          
          if(blocks < 200)
              return;
        
        if(whereContainsBind)
            return;
        writer.write(line + "," + tables + "," + numRows + "," + numBlocks + "," 
             + tableOnWhichRowsDetermined +"\n");
    }
    
    
    public static void getMaxRows(String fileName,BufferedWriter writer,String line, 
                                  XMLDocument voXml,HashSet<String> exemptedTables1, HashSet<String> exemptedTables2_maxRows, 
                                  HashMap<String,String> lovVOToBaseVOMapping, HashMap<String,String> voMaxRowsCalculated) throws Exception{
        
        StringBuffer numRows = new StringBuffer();
        StringBuffer numBlocks = new StringBuffer();
        StringBuffer tableOnWhichRowsDetermined = new StringBuffer();
        
        if(voXml == null)
            voXml = XMLParserHelper.getXMLDocument(fileName);
        if(voXml == null){
            System.out.println("Could not get VO XML for file:  " +fileName);
            return;
        }
        NodeList viewObjects = voXml.getElementsByTagName("ViewObject");
        if(viewObjects == null || viewObjects.getLength() != 1) 
        {
          System.out.println("WARNING:  Unexpected number of view objects found for file:  " +fileName);
          return;
        }
        if(voMaxRowsCalculated.containsKey(fileName)) {
            
            String violation = voMaxRowsCalculated.get(fileName);
            if(violation.equals("No Violation"))
                return;
            
            if(line.contains("No DisplayViewCriteria") || line.contains("No ViewCriteriaUsage")
               || line.contains("VO has no ViewCriteria (LOVSmartFilterNoViewCriteria)")){
                
                   String[] parts = line.split(",");
                   if(parts.length < 10){
                       System.out.println("Shouldnt happen !!!!!!!!!!");
                       return;
                   }
                   String mruCount = " ";
                   if(parts.length>10)
                       mruCount = parts[10];
                   
                   String key = parts[3].trim() + "," + parts[7].trim()+","+parts[8].trim();
                   if(lovVOToBaseVOMapping.containsKey(key)){
                       String baseVo = lovVOToBaseVOMapping.get(key); 
                       writer.write(getFileNameInfoFromRelativePath(baseVo) + baseVo 
                            + "," + parts[4] + "," + parts[5] + "," + parts[6] + "," + parts[7] + "," + parts[8] + "," 
                            + parts[9] + "," + mruCount + "," + violation +"\n");
                      
                   }
            //               else
            //                   System.out.println("break here");
                   
            } else  
                    writer.write(line + "," + violation +"\n");
            
            return;
        }
        
        
        Node viewObject = viewObjects.item(0);
        
        HashMap<String,String> tableAliasNames = new HashMap<String,String>();
        String fromList = XMLParserHelper.getAttributeValue(viewObject,"FromList");
        
        if(fromList != null) {        
           SQLParserHelper.processFromPart(fromList,tableAliasNames);
           
        }else {
            Node sql = XMLParserHelper.getChildNodeWithName(viewObject, "SQLQuery");
            if(sql == null){
                voMaxRowsCalculated.put(fileName,"No Violation");
                return;
            }
            
            String sqlQuery = sql.getTextContent();
            if(sqlQuery == null){
                voMaxRowsCalculated.put(fileName,"No Violation");
                return;
            }
                
            sqlQuery = sqlQuery.replaceAll("/\\*.*?\\*/", ""); //remove hints and comments
            sqlQuery = sqlQuery.replaceAll("--.*$",""); //remove single line comments            
            
            Matcher m = SQLParserHelper.fromJoinPattern.matcher(sqlQuery);
            if(m.find())
                SQLParserHelper.processFromJoinPart(m,tableAliasNames);
            else{        
              m = SQLParserHelper.fromPattern.matcher(sqlQuery);
              if(!m.find())
                  m = SQLParserHelper.fromOnlyPattern.matcher(sqlQuery);
              else
                  m.reset();       
              
              while(m.find())
                SQLParserHelper.processFromPart(m.group(1),tableAliasNames);                 
            }            
        }
        
        String tables = "";
        for(Iterator<String> it = tableAliasNames.values().iterator();it.hasNext();)
            tables += it.next() + " ";
        
        tables = tables.replace("(","").trim();
        
        if(tables.equals("DUAL")){
            voMaxRowsCalculated.put(fileName,"No Violation");
            return;
        }
        
        boolean whereContainsBind = false;
        String whereClause = XMLParserHelper.getAttributeValue(viewObject, "Where");
        if(isEmpty(whereClause)){
            Node sql = XMLParserHelper.getChildNodeWithName(viewObject, "SQLQuery");
            if(sql != null) {
                String sqlQuery = sql.getTextContent();
                SQLParserHelper.num_nestedQueriesFound = 0;
                SQLParserHelper.nestedQueries = new HashMap<String,String>(); 
                sqlQuery = sqlQuery.replaceAll("/\\*.*?\\*/", ""); //remove hints and comments
                sqlQuery = sqlQuery.replaceAll("--.*$",""); //remove single line comments      
                
                sqlQuery = SQLParserHelper.removeNestedQueries(sqlQuery); 
                Matcher m = SQLParserHelper.fromPattern.matcher(sqlQuery);
                while(m.find())
                    sqlQuery = sqlQuery.replace(m.group(),"");
                m = SQLParserHelper.selectFromPattern.matcher(sqlQuery);
                while(m.find())
                    sqlQuery = sqlQuery.replace(m.group(),"");
                
                if(sqlQuery.contains(":"))
                    whereContainsBind = true;
                
                if(!whereContainsBind){
                    for(Iterator<String> it = SQLParserHelper.nestedQueries.values().iterator(); it.hasNext();) {
                        String nestedQuery = it.next();
                        nestedQuery = nestedQuery.replaceAll("/\\*.*?\\*/", ""); //remove hints and comments
                        nestedQuery = nestedQuery.replaceAll("--.*$",""); //remove single line comments    
                        
                        Matcher m1 = SQLParserHelper.fromPattern.matcher(nestedQuery);
                        while(m1.find())
                            nestedQuery = nestedQuery.replace(m1.group(),"");
                        m1 = SQLParserHelper.selectFromPattern.matcher(nestedQuery);
                        while(m1.find())
                            nestedQuery = nestedQuery.replace(m1.group(),"");
                        
                        if(nestedQuery.contains(":")){
                            whereContainsBind = true;
                            break;
                        }
                    }
                }
            }      
            
        } else {
            if(whereClause.contains(":"))
                whereContainsBind = true;
        }       
        
        getNumRowsBlocks(tables, numRows, numBlocks, tableOnWhichRowsDetermined, exemptedTables1);
        
        if(tableOnWhichRowsDetermined.toString().contains("LOOKUP")){
            voMaxRowsCalculated.put(fileName,"No Violation");
            return;        
        }
        if(exemptedTables2_maxRows.contains(tableOnWhichRowsDetermined.toString())) {
            voMaxRowsCalculated.put(fileName,"No Violation");
            return;
        }
          
        if(isEmpty(numBlocks.toString())){
            voMaxRowsCalculated.put(fileName,"No Violation");
            return;
        }
        
        int blocks = 0;
          try{
            blocks = Integer.parseInt(numBlocks.toString());
          }catch(Exception e){
              e.printStackTrace();
          }
          
          if(blocks < 200){
              voMaxRowsCalculated.put(fileName,"No Violation");
              return;
          }
        
          if(whereContainsBind){
            voMaxRowsCalculated.put(fileName,"No Violation");
            return;
          }
        
        
        if(line.contains("No DisplayViewCriteria") || line.contains("No ViewCriteriaUsage")
           || line.contains("VO has no ViewCriteria (LOVSmartFilterNoViewCriteria)")){
            
               String[] parts = line.split(",");
               if(parts.length < 10){
                   System.out.println("Shouldnt happen !!!!!!!!!!");
                   return;
               }
               String mruCount = " ";
               if(parts.length>10)
                   mruCount = parts[10];
               
               String key = parts[3].trim() + "," + parts[7].trim()+","+parts[8].trim();
               if(lovVOToBaseVOMapping.containsKey(key)){
                   String baseVo = lovVOToBaseVOMapping.get(key); 
                   writer.write(getFileNameInfoFromRelativePath(baseVo) + baseVo 
                        + "," + parts[4] + "," + parts[5] + "," + parts[6] + "," + parts[7] + "," + parts[8] + "," 
                        + parts[9] + "," + mruCount + "," + tables + "," + numRows + "," + numBlocks + "," 
                        + tableOnWhichRowsDetermined + "," + whereContainsBind +"\n");
                  
                   voMaxRowsCalculated.put(fileName,tables + "," + numRows + "," + numBlocks + "," 
                        + tableOnWhichRowsDetermined + "," + whereContainsBind);
               }
//               else
//                   System.out.println("break here");
               
        } else  {        
                writer.write(line + "," + tables + "," + numRows + "," + numBlocks + "," 
                     + tableOnWhichRowsDetermined + "," + whereContainsBind +"\n");
            
                 voMaxRowsCalculated.put(fileName,tables + "," + numRows + "," + numBlocks + "," 
                 + tableOnWhichRowsDetermined + "," + whereContainsBind);
        }
    }
    
    public static boolean parentVCMatches(Node vc, String criteriaName) throws Exception {
        
        Node parent = vc.getParentNode();
        while(!parent.getNodeName().equals("ViewObject")) {
            
            if(parent.getNodeName().equals("ViewCriteria")){
                String vcName = XMLParserHelper.getAttributeValue(parent, "Name");
                if(vcName.equals(criteriaName))
                    return true;
            }
            parent = parent.getParentNode();
        }
        return false;
    }
    
    
    public static String convertViewNamesIntoTableNames(String table_name, String aliasName) throws Exception{
            
          
           if(table_name == null || table_name.trim().equals(""))
               return "";
           String resolvedTableColumn = "";
            String[] parts = table_name.split(" ");
            
            for(int i = 0; i < parts.length; i++) {
                
                String table = parts[i].trim();
                
                if(SqlIndexFinder.synonyms.containsKey(table))
                    table = SqlIndexFinder.synonyms.get(table);
                
                if(SqlIndexFinder.viewNameText.containsKey(table)) {
                    isView = true;
                    
                    if(ViewCriteriaHelper.isEmpty(resolvedTableColumn)) 
                          resolvedTableColumn = SqlIndexFinder.getResolvedTablesColumn(table,aliasName);      
                    
                   Matcher m = pTablesColumn.matcher(resolvedTableColumn);
                   if(m.find()){
                       
                      table = m.group(1).trim();
                      if(SqlIndexFinder.viewNameText.containsKey(table)) {
                          resolvedTableColumn = SqlIndexFinder.getResolvedTablesColumn(table,m.group(2));
                          Matcher m1 = pTablesColumn.matcher(resolvedTableColumn);
                          if(m1.find()) {
                              table = m1.group(1).trim();
                              if(SqlIndexFinder.viewNameText.containsKey(table)) {
                                  resolvedTableColumn = SqlIndexFinder.getResolvedTablesColumn(table,m1.group(2));
                                  Matcher m2 = pTablesColumn.matcher(resolvedTableColumn);
                                  if(m2.find())
                                      table=m2.group(1).trim();
                              }
                          }
                      }
                      
                   }                
                }       
                table_name = table_name.replace(parts[i], table);
            } //for
            return table_name;        
        }
    
}
