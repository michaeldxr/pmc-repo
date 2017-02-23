package oracle.apps.psr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;


import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLNode;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DeprecatedTriggersLRG {
    
    private HashSet<String> setDeprecatedTriggers = new HashSet<String>();
    private ArrayList<String> exceptions = new ArrayList<String>();

    private boolean bDebug = false;
    
    private static AdeDiffHelper diffHelper = new AdeDiffHelper("diff.txt");
    private static BufferedWriter writer;
    private static int numberDeprecatedTFound = 0;
    private static int numberTFilesFound = 0;
    private static int numberDeprecatedTFixedd = 0;
    private static int numberTFilesFixed = 0;
    private static int numberOfFilesLookedAt = 0;
    private static int numberOfExceptions = 0;
    private static final String issuetype = "DeprecatedPartialTriggers";
    
    public DeprecatedTriggersLRG() {
        super();
        setDeprecatedTriggers.add("deleteMenuItem");
        setDeprecatedTriggers.add("createMenuItem");
        setDeprecatedTriggers.add("editMenuItem");
        setDeprecatedTriggers.add("duplicateMenuItem");
        setDeprecatedTriggers.add("create");
        setDeprecatedTriggers.add("delete");
        setDeprecatedTriggers.add("edit");
        setDeprecatedTriggers.add("duplicate");
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }

    public static void main(String[] args) {
        try {
            if (args.length != 1 || args[0] == null || args[0].equals("")) {
                System.out.println("Series has to be specified for " +
                                   issuetype);
                System.exit(1);
            }

            String series = args[0];
        
            String label = LRGUtil.getLatestLabel(series);
            System.out.println("Running " + issuetype + " Scan on series: " + series + ".......");
            System.out.println("Latest label is: " + label);

            writer = new BufferedWriter(new FileWriter("DeprecatedPartialTriggers_LRG.csv"));
            writer.write("Family,Module,Product,FileName,Label,Description\n");

            ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
            for (int j = 0; j < families.size(); j++) {
                String family = families.get(j);
                String sCrawlDir = LRGUtil.getCrawlDir(series, family, label, issuetype);
                System.out.println("Crawling dir '" + sCrawlDir + "' for family: " + family);
                
                DeprecatedTriggersLRG deprecatedTriggers = new DeprecatedTriggersLRG();
                deprecatedTriggers.crawlDirectory(sCrawlDir, false, false);
                
                System.out.println("Crawling done for family "+family+".");
            }
            
            writer.close();

            LRGUtil.addLabelInfoToAB(issuetype, series, label);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void crawlDirectory(String crawlDir, boolean makeFix, boolean bDoADE) {
        if (crawlDir.contains(".ade_path") || crawlDir.contains("/classes/") || crawlDir.contains("/noship"))
            return;

        File folder = new File(crawlDir);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();
                if (isValidUIFile(absFilePath)) {

                    processFile(listOfFiles[i], makeFix, bDoADE);
                }
            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                if (isValidDirectory(dir)) {
                    crawlDirectory(dir, makeFix, bDoADE);
                }

            } else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }

    
    public boolean isValidUIFile(String absFilePath) {
        absFilePath = absFilePath.toLowerCase();
        if (!isEmpty(absFilePath)) {
            if (absFilePath.contains("/test/") || absFilePath.contains("/modeltest/") || absFilePath.contains("/classes/") ||
                absFilePath.contains("/model/") || absFilePath.contains("/publicmodel/") || absFilePath.contains("/publicmodeltest")
                || absFilePath.contains("/uitest") || absFilePath.contains("/testui") || absFilePath.contains("/publicuitest")
                || absFilePath.contains("servicetest") || absFilePath.contains("/publicservicetest") || absFilePath.contains("/structuretest")) {
                return false;
            }
            if (absFilePath.endsWith(".jspx") || absFilePath.endsWith(".jsff")) {
                return true;
            }
        }
        return false;
    }

    public boolean isValidDirectory(String absDirPath) {
        if (!absDirPath.contains("/model/") && !absDirPath.contains("/dbSchema/") && !absDirPath.contains("/publicModel/") &&
            !absDirPath.contains("/classes/") && !absDirPath.contains("/test/")) {
            return true;
        }
        return false;
    }

    public void processFile(File fName, boolean makeFix, boolean bDoADE) {
        numberOfFilesLookedAt++;
        try {
            String absPath = fName.getAbsolutePath();
            String canonicalPath = fName.getCanonicalPath();
            String fileName = fName.getName();
            if(bDebug)
            {
              System.out.println("Processing: " + absPath);
              System.out.println("Name: " + fileName);
            }
            XMLDocument xmlDoc = XMLParserHelper.getXMLDocument(absPath);
            if (xmlDoc == null) {
                System.out.println("cannot process file as xml");
            }
            Element rootNode = xmlDoc.getDocumentElement();
            ArrayList<Node> violatingNodes = new ArrayList<Node>();
            getViolatingTableNodesInFile(rootNode.getChildNodes(), violatingNodes);

            if(violatingNodes.size() > 0) {                
                String description = "";
                for(int i = 0; i < violatingNodes.size(); i++){
                    XMLNode n = (XMLNode)violatingNodes.get(i);
                    description += n.getNodeName() + "(line:" + n.getLineNumber() + ") ";
                }
                writer.write(getFileNameInfo(canonicalPath) + description + "\n");
            }
            if (makeFix && violatingNodes != null && violatingNodes.size() > 0) {
                numberTFilesFound++;
                // Need to make fix
                if (bDebug) {
                    System.out.println("Need to make fix");
                }
                makeFix(violatingNodes, absPath, bDoADE);
            }
        } catch (Exception ex) {
            logException(ex);
            ex.printStackTrace();
        }


    }

    private boolean makeFix(ArrayList<Node> violatingNodes, String absFilePath, boolean bDoAde) throws Exception {
        HashSet<String> tableIdsFixed = new HashSet<String>();
        boolean bFixMade = false;
        try {
            XmlFixer xmlFixer = new XmlFixer(absFilePath);
            xmlFixer.setBDebug(bDebug);
            Iterator<Node> nodeIter = violatingNodes.iterator();
            while (nodeIter.hasNext()) {
                Node tableNodeToFix = nodeIter.next();
                String triggerToFix = XMLParserHelper.getAttributeValue(tableNodeToFix, "partialTriggers");
                String modifiedTrigger = getFixedTriggerString(triggerToFix);
                if (!isEmpty(modifiedTrigger) && !modifiedTrigger.trim().equals(triggerToFix.trim())) {
                    // make fix
                    String tableId = XMLParserHelper.getAttributeValue(tableNodeToFix, "id");
                    int numberTrToFix = getNumberofTriggerViol(triggerToFix);
                    if (tableIdsFixed.contains(tableId)) {
                        numberDeprecatedTFixedd = numberDeprecatedTFixedd + numberTrToFix;
                        continue;
                    }
                    xmlFixer.modifyAttribute(tableNodeToFix, "partialTriggers", triggerToFix, modifiedTrigger, false);
                    bFixMade = true;
                    tableIdsFixed.add(tableId);
                    numberDeprecatedTFixedd = numberDeprecatedTFixedd + numberTrToFix;
                }
            }
            if (bFixMade) {
                if (bDoAde) {
                    // write the file
                    applyFix(absFilePath, xmlFixer.getFileContentsAsString(false));
                }
                numberTFilesFixed++;
            }
        } catch (Exception ex) {
            System.out.println("Exception while fixing: " + absFilePath);
            ex.printStackTrace();
            logException(ex);
            return false;
        }

        return true;
    }


    public void applyFix(String absPath, String newFileContents) throws Exception {
        File file = new File(absPath);
        if (!file.exists()) {
            throw new Exception("while making fix, file not found: " + absPath);
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

    public boolean isInvalidTrigger(String trigger) {
        if (!isEmpty(trigger)) {
            if (setDeprecatedTriggers.contains(trigger.trim()))
                return true;
        }

        return false;
    }

    public String getFixedTriggerString(String oldTriggerString) {

        StringBuffer modifiedTrigger = new StringBuffer();

        String[] triggerParts = oldTriggerString.split(" ");
        int trigLen = triggerParts.length;
        for (int i = 0; i < trigLen; i++) {
            String trigger = triggerParts[i];
            if (isInvalidTrigger(trigger)) {
                trigger = "::" + trigger;
            }
            modifiedTrigger.append(trigger);
            modifiedTrigger.append(" ");
            // get all the individiaul riggers
            // for each violation, append ::

        }

        return modifiedTrigger.toString().trim();
    }

    public void getViolatingTableNodesInFile(NodeList childNodes, ArrayList<Node> violatingNodes) {
        if (childNodes == null) {
            return;
        }
        int childNodeLength = childNodes.getLength();
        for (int i = 0; i < childNodeLength; i++) {

            Node child = childNodes.item(i);

            if (XMLParserHelper.isNodeName(child, "fnd:applicationsTable")) {
                traverseNodesForAfTable(child.getChildNodes(), violatingNodes);
            }
            if (child.hasChildNodes()) {
                getViolatingTableNodesInFile(child.getChildNodes(), violatingNodes);
            }
        }
    }

    public void traverseNodesForAfTable(NodeList childNodes, ArrayList<Node> violatingNodes) {
        if (childNodes == null) {
            return;
        }
        int childNodeLength = childNodes.getLength();
        for (int i = 0; i < childNodeLength; i++) {

            Node child = childNodes.item(i);

            if (XMLParserHelper.isNodeName(child, "af:table")) {
                String partialTriggers = XMLParserHelper.getAttributeValue(child, "partialTriggers");
                int numTViolInStr = getNumberofTriggerViol(partialTriggers);

                if (numTViolInStr > 0) {
                    violatingNodes.add(child);
                    numberDeprecatedTFound = numberDeprecatedTFound + numTViolInStr;
                }
            }
            if (child.hasChildNodes()) {
                traverseNodesForAfTable(child.getChildNodes(), violatingNodes);
            }
        }
    }

    public int getNumberofTriggerViol(String partialTriggerString) {
        int numberTViol = 0;

        if (!isEmpty(partialTriggerString)) {
            String[] triers = partialTriggerString.split(" ");
            if (triers == null || triers.length < 1) {
                return numberTViol;
            }
            int len = triers.length;
            for (int i = 0; i < len; i++) {
                String trigger = triers[i];
                if (isInvalidTrigger(trigger)) {
                    numberTViol++;
                }
            }
        }
        return numberTViol;
    }

    private void logException(Exception e) {
        numberOfExceptions++;
        StringBuffer exStr = new StringBuffer();


        StackTraceElement[] element = e.getStackTrace();
        for (int i = 0; i < element.length; i++) {
            exStr.append(element[i].toString());
            exStr.append(" \n ");
        }
        exceptions.add(exStr.toString());
    }
    
  private String getFileNameInfo(String absPath) {
    int i = 0;
    String family = "";
    String blankString =  "NA,NA,NA,NA,NA,";
    if(isEmpty(absPath))
        return blankString;
    if(absPath.startsWith("/ade/"))
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
    String pathAfterFusionApps = absPath.substring(i);
    String[] parts = pathAfterFusionApps.split("/");
    String module = "";
    String product = "";
    
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

}
