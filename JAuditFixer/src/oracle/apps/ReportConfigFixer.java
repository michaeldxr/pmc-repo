package oracle.apps;

import java.io.BufferedWriter;
import java.io.File;

import java.io.FileWriter;

import java.util.Date;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.Mail;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ReportConfigFixer {
    public static boolean bDebug = false;
    public static int m_numberOfReportsFound = 0;
    public static int m_numberOfReportConfigsFound = 0;
    public static int m_numberOfReportConfigsFixed = 0;

    public static int m_numberOfDocumentCacheFixed = 0;
    public static int m_numberOfDataModelCacheFixed = 0;

    public static boolean m_xpathOptimization = true;
    public static boolean m_disableDocumentCaching = true;
    public static boolean m_disableDataModelCaching = true;
    public static boolean m_scalabilityFixes = true;

    static AdeDiffHelper diffHelper = new AdeDiffHelper("diff.txt");

    public ReportConfigFixer() {
        super();
    }

    /**
     * Usage: reportConfigFixer -Ddebug=<true/false> -DxpathOptimization=<true/false> -DdisableDocCaching=<true/false>
     */
    public static String procesSystemProperties() {
        StringBuffer commandLineArgs = new StringBuffer();
        String propDebug = System.getProperty("debug");
        if (propDebug != null && propDebug.trim().equals("true"))
            bDebug = true;
        else if (propDebug != null && propDebug.trim().equals("false"))
            bDebug = false;

        String strMakeXPathOpt = System.getProperty("xpathOptimization");
        if (strMakeXPathOpt != null && strMakeXPathOpt.trim().equals("true"))
            m_xpathOptimization = true;
        else if (strMakeXPathOpt != null &&
                 strMakeXPathOpt.trim().equals("false"))
            m_xpathOptimization = false;
commandLineArgs.append(" -DxpathOptimization="+m_xpathOptimization);
        String strdisableDocCaching = System.getProperty("disableDocCaching");
        if (strdisableDocCaching != null &&
            strdisableDocCaching.trim().equals("true"))
            m_disableDocumentCaching = true;
        else if (strdisableDocCaching != null &&
                 strdisableDocCaching.trim().equals("false"))
            m_disableDocumentCaching = false;
        commandLineArgs.append(" -DdisableDocCaching="+m_disableDocumentCaching);
        String strDisableDatamodelCaching = System.getProperty("disableDataModelCaching");
        if (strDisableDatamodelCaching != null &&
            strDisableDatamodelCaching.trim().equals("true"))
            m_disableDataModelCaching = true;
        else if (strDisableDatamodelCaching != null &&
                 strDisableDatamodelCaching.trim().equals("false"))
            m_disableDataModelCaching = false;
        commandLineArgs.append(" -DdisableDataModelCaching="+m_disableDataModelCaching);
        
        String strScalabilityFixes = System.getProperty("scalabilityFixes");
        if (strScalabilityFixes != null &&
            strScalabilityFixes.trim().equals("true"))
            m_scalabilityFixes = true;
        else if (strScalabilityFixes != null &&
                 strScalabilityFixes.trim().equals("false"))
            m_scalabilityFixes = false;
        commandLineArgs.append(" -DscalabilityFixes="+m_scalabilityFixes);
        return commandLineArgs.toString();
    }

    public static void main(String[] args) throws Exception {
        ReportConfigFixer reportConfigFixer = new ReportConfigFixer();

        // Make sure it is run with correct number of arguments
        if (args.length != 3 || isEmpty(args[0]) || isEmpty(args[1]) ||
            isEmpty(args[2])) {
            System.out.println("Usage:  reportFixer.sh <path to your code in an open ade view> <makeFix> <bDoADE> [-DxpathOptimization=true]");
            System.exit(1);
        }

        String cmdLine = procesSystemProperties();
        System.out.println("Running with args: " + cmdLine);


        String sCrawlDir = args[0];
        Boolean bfixViolations = new Boolean(args[1]);
        Boolean bDoADE = new Boolean(args[2]);
        System.out.println("Analyzing Directory: " + sCrawlDir);

        // Remove any existing diff.txt. Create a new bufferred writer for diff.txt
        if (bDoADE) {
            System.out.println("removing any existing diff.txt");
            diffHelper.startDiffLog();
        }

        try {
            reportConfigFixer.crawlDirectory(sCrawlDir, bfixViolations,
                                             bDoADE);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        String summary = reportConfigFixer.printResults();

        if (bDoADE) {
            diffHelper.closeDiffLog();
        }

        Date runDate = new Date();
       // Mail.sendMail("zeesha.currimbhoy@oracle.com", summary,
        //              "Report Config Fixer Run At:  " + runDate.toString());

    }

    private String printResults() {
        StringBuffer sbResults = new StringBuffer();
        sbResults.append("\n=================================");
        sbResults.append("\n========= RESULTS ==========");
        sbResults.append("\n=================================");
        sbResults.append("\n Number of Report Files Found: " +
                         m_numberOfReportsFound);
        sbResults.append("\n Number of Report Config Files Found: " +
                         m_numberOfReportConfigsFound);
        sbResults.append("\n Number of Report Config Files Fixed: " +
                         m_numberOfReportConfigsFixed);
        sbResults.append("\n Number of Document Caching Issues Fixed: " +
                         m_numberOfDocumentCacheFixed);
        sbResults.append("\n Number of Data Model Caching Issues Fixed: " +
                         m_numberOfDataModelCacheFixed);
        System.out.println(sbResults.toString());
        return sbResults.toString();
    }

    private void crawlDirectory(String crawlDir, boolean makeFix,
                                boolean bDoADE) {
        if (crawlDir.contains(".ade_path") || crawlDir.contains("/classes/"))
            return;

        File folder = new File(crawlDir);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();
                if (isValidReportFile(absFilePath)) {
                    m_numberOfReportsFound++;
                    analyzeReportForChangesRequred(listOfFiles[i], makeFix,
                                                   bDoADE);
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

    public void analyzeReportForChangesRequred(File report, boolean makeFix,
                                               boolean bDoADE) {
        if (m_xpathOptimization) {
            processReportforXPath(report, makeFix, bDoADE);
        }
        if (m_disableDocumentCaching) {
            processReportForDocumentCaching(report, makeFix, bDoADE);
        }
        if (m_disableDataModelCaching) {
            processReportForDataModelCaching(report, makeFix, bDoADE);
        }

    }

    public void processReportForDataModelCaching(File report, boolean makeFix,
                                                boolean bDoADE) {
        String reportAbsPath = report.getAbsolutePath();
        String reportFileName = report.getName();

        if (bDebug)
            System.out.println("[DataModel Caching Check] Processing report: " +
                               reportAbsPath);

        Document reportXml = XMLParserHelper.getXMLDocument(reportAbsPath);
        if (reportXml == null) {
            System.out.println("[ERROR] Cannot get xml for report: " +
                               reportAbsPath);
            return;
        }

        Node reportNode = null;
        try {
            reportNode = getReportRootNode(reportXml, reportAbsPath);
        } catch (Exception ex) {
            System.out.println("Exception while trying to get report Node: " +
                               reportAbsPath);
            ex.printStackTrace();
        }

        Node dataModelNode =
            XMLParserHelper.getChildNodeWithMatchingAttributeAndName(reportNode,
                                                                              "dataModel",
                                                                              "cache",
                                                                              "true");
        if (dataModelNode == null) {
            return;
        }

        try {
            applyDataModelFix(dataModelNode, reportAbsPath,
                               makeFix, bDoADE);
        } catch (Exception ex) {
            System.out.println("Skipping file. Exception occurred while making fix: " +
                               reportAbsPath);
        }
       
    }


    public void processReportForDocumentCaching(File report, boolean makeFix,
                                                boolean bDoADE) {
        String reportAbsPath = report.getAbsolutePath();
        String reportFileName = report.getName();

        if (bDebug)
            System.out.println("[Document Caching Check] Processing report: " +
                               reportAbsPath);

        Document reportXml = XMLParserHelper.getXMLDocument(reportAbsPath);
        if (reportXml == null) {
            System.out.println("[ERROR] Cannot get xml for report: " +
                               reportAbsPath);
            return;
        }

        Node reportNode = null;
        try {
            reportNode = getReportRootNode(reportXml, reportAbsPath);
        } catch (Exception ex) {
            System.out.println("Exception while trying to get report Node: " +
                               reportAbsPath);
            ex.printStackTrace();
        }

        Element cacheDocumentProperty =
            (Element)XMLParserHelper.getChildNodeWithMatchingAttributeAndName(reportNode,
                                                                              "property",
                                                                              "name",
                                                                              "cacheDocument");
        if (cacheDocumentProperty == null) {
            if (bDebug) {
                System.out.println("Cache document property not present");
            }
            return;
        }

        String cacheDocumentVal = cacheDocumentProperty.getAttribute("value");
        if (!isEmpty(cacheDocumentVal) &&
            cacheDocumentVal.trim().equals("true")) {
            System.out.println("[FIX TO BE MADE] Cache document val needs to be changed to false ");
            try {
                applyDocCachingFix(cacheDocumentProperty, reportAbsPath,
                                   makeFix, bDoADE);
            } catch (Exception ex) {
                System.out.println("Skipping file. Exception occurred while making fix: " +
                                   reportAbsPath);
            }
        }
    }

    /**
     * Remove attr:cache = true is present in datamodel tag -> delete the attribute
     * @param dataModelNode
     * @param reportAbsPath
     * @param makeFix
     * @param bDoADE
     * @throws Exception
     */
    public void applyDataModelFix(Node dataModelNode,
                                   String reportAbsPath, boolean makeFix,
                                   boolean bDoADE) throws Exception {
        if (!makeFix)
            return;
        XmlFixer reportFixer = new XmlFixer(reportAbsPath);
        reportFixer.setBDebug(bDebug);
        try {
            reportFixer.removeAttribute(dataModelNode, "cache", "true");
        } catch (Exception ex) {
            System.out.println("Exception while trying to make modification for file: " +
                               reportAbsPath);
            ex.printStackTrace();
            throw ex;
        }
        applyFix(reportFixer, reportAbsPath, makeFix, bDoADE);
        m_numberOfDataModelCacheFixed++;

    }


    public void applyDocCachingFix(Node cacheDocumentNode,
                                   String reportAbsPath, boolean makeFix,
                                   boolean bDoADE) throws Exception {
        if (!makeFix)
            return;
        XmlFixer reportFixer = new XmlFixer(reportAbsPath);
        reportFixer.setBDebug(bDebug);
        try {
            reportFixer.modifyAttribute(cacheDocumentNode, "value", "true",
                                        "false", false);
        } catch (Exception ex) {
            System.out.println("Exception while trying to make modification for file: " +
                               reportAbsPath);
            ex.printStackTrace();
            throw ex;
        }
        applyFix(reportFixer, reportAbsPath, makeFix, bDoADE);
        m_numberOfDocumentCacheFixed++;

    }

    public void applyFix(XmlFixer fixer, String absPath, boolean makeFix,
                         boolean bDoADE) throws Exception {
        if (makeFix && bDoADE) {
            try {
                String configNewContents =
                    fixer.getFileContentsAsString(false);
                applyFix(absPath, configNewContents);
            } catch (Exception fixex) {
                System.out.println("[ERROR.. skipping file] Excpetion while making fix for file: " +
                                   absPath);
                fixex.printStackTrace();
                throw fixex;
            }

        }
    }

    public Node getReportRootNode(Document reportXml,
                                  String absPath) throws Exception {
        NodeList reportNodeList = reportXml.getElementsByTagName("report");
        if (!XMLParserHelper.hasOneNOnlyOneNode(reportNodeList))
            throw new Exception("Expecting one and only one report Node: " +
                                absPath);
        return reportNodeList.item(0);
    }

    public void processReportforXPath(File report, boolean makeFix,
                                      boolean bDoADE) {
        String reportAbsPath = report.getAbsolutePath();
        String reportFileName = report.getName();

        if (bDebug)
            System.out.println("Processing report: " + reportAbsPath);

        String strReportDir = getPreviousDirectory(reportAbsPath);
        File reportDir = new File(strReportDir);
        if (!reportDir.exists()) {
            System.out.println("Report directory does not exist: " +
                               reportDir.getAbsolutePath());
            return;
        }

        String reportFileNameWithoutExt =
            getFileNameWithoutExtension(reportFileName);
        String configFileName = reportFileNameWithoutExt + ".cfg";
        String configFilePath = strReportDir + "/" + configFileName;
        File reportConfigFile = new File(configFilePath);
        if (!reportConfigFile.exists()) {
            return;
        }
        m_numberOfReportConfigsFound++;
        try {
            processReportConfig(reportConfigFile, makeFix, bDoADE);
        } catch (Exception ex) {
            System.out.println("Exception while processing config file: " +
                               configFilePath);
            ex.printStackTrace();
        }

    }

    public Node getXpathOptimizationPropertyNode(Node propertiesNode) {

        return XMLParserHelper.getChildNodeWithMatchingAttributeAndName(propertiesNode,
                                                                        "property",
                                                                        "name",
                                                                        "xslt-xpath-optimization");

    }

    public void makeXPathOptimizationFix(File reportConfigFile,
                                         boolean bMakeFix,
                                         boolean bDoADE) throws Exception {


        String reportConfigAbsPath = reportConfigFile.getAbsolutePath();
        XMLDocument configXml =
            XMLParserHelper.getXMLDocument(reportConfigAbsPath);
        XmlFixer configFixer = new XmlFixer(reportConfigAbsPath);

        if (bDebug)
            System.out.println("Processing config: " + reportConfigAbsPath);

        NodeList propertiesNodes =
            configXml.getElementsByTagName("properties");
        if (!XMLParserHelper.hasOneNOnlyOneNode(propertiesNodes)) {
            if (bDebug) {
                String msg =
                    "[ERROR] Expecting one and only one properties node in the config xml";
                System.out.println(msg);
                throw new Exception(msg);
            }
        }

        Node propertiesNode = propertiesNodes.item(0);
        Node xpathOptimizationNode =
            getXpathOptimizationPropertyNode(propertiesNode);
        if (xpathOptimizationNode != null) {
            String textContent = xpathOptimizationNode.getTextContent();
            if (!isEmpty(textContent) && textContent.trim().equals("true")) {
                if (bDebug) {
                    System.out.println("Xpath optimization property already present");
                }
                return;
            } else if (!isEmpty(textContent) &&
                       !textContent.trim().equals("false")) {
                System.out.println("[ERROR] Cannot recognize value of xpath optimizatio node. It should be either true or false");
                return;
            }
        }
        if (m_xpathOptimization) {
            if (xpathOptimizationNode == null) {
                // add node to properties
                String strXPathNodeToAdd =
                    "<property name=\"xslt-xpath-optimization\">true</property>";
                configFixer.addAsLastChildToNode(propertiesNode,
                                                 strXPathNodeToAdd, "   ");
                if (bMakeFix && bDoADE) {
                    try {
                        String configNewContents =
                            configFixer.getFileContentsAsString(false);
                        applyFix(reportConfigAbsPath, configNewContents);
                    } catch (Exception fixex) {
                        System.out.println("[ERROR.. skipping file] Excpetion while making fix for file: " +
                                           reportConfigAbsPath);
                        fixex.printStackTrace();
                    }
                    m_numberOfReportConfigsFixed++;
                }
            } else {
                // modify attribute of xpath optimization node
                boolean madeChange =
                    configFixer.modifyTextContent(xpathOptimizationNode,
                                                  "name=\"xslt-xpath-optimization\"",
                                                  "false", "true");
                if (madeChange && bMakeFix && bDoADE) {
                    try {
                        String configNewContents =
                            configFixer.getFileContentsAsString(false);
                        applyFix(reportConfigAbsPath, configNewContents);
                    } catch (Exception fixex) {
                        System.out.println("[ERROR.. skipping file] Excpetion while making fix for file: " +
                                           reportConfigAbsPath);
                        fixex.printStackTrace();
                    }
                    m_numberOfReportConfigsFixed++;
                }
            }
        }

    }

    public void processReportConfig(File reportConfigFile, boolean bMakeFix,
                                    boolean bDoADE) throws Exception {
        if (m_xpathOptimization) {
            makeXPathOptimizationFix(reportConfigFile, bMakeFix, bDoADE);
        }

    }

    /**
     * Report Files have to reside in
     * /bipub/ and /reports/
     * @param path
     * @return
     */
    public boolean isValidReportFile(String path) {
        if (isEmpty(path))
            return false;

        if (path.contains("/classes/") || path.contains("/.ade_path/"))
            return false;

        if (path.contains("/dbSchema/"))
            return false;

        if (path.endsWith("Test"))
            return false;

        if (!path.contains("/reports/"))
            return false;

        if (!path.contains("/bipub/"))
            return false;

        if ((path.contains("/reports/") || path.contains("/report/")) &&
            path.endsWith(".xdo"))
            return true;

        return false;
    }

    public boolean isValidDirectory(String path) {
        if (isEmpty(path))
            return false;

        if (path.contains("/classes/") || path.contains("/.ade_path/"))
            return false;

        if (path.contains("/dbSchema/"))
            return false;

        if (path.endsWith("Test"))
            return false;


        return true;
    }

    private String getPreviousDirectory(String path) {
        if (!isEmpty(path)) {
            int indexLastSlash = path.lastIndexOf("/");
            if (indexLastSlash != -1) {
                return path.substring(0, indexLastSlash);
            }
        }
        return null;
    }

    private String getFileNameWithoutExtension(String reportFileName) {
        if (isEmpty(reportFileName))
            return null;

        int indexOfDot = reportFileName.lastIndexOf(".");
        if (indexOfDot == -1)
            return reportFileName;
        return reportFileName.substring(0, indexOfDot);
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }


    public void applyFix(String absPath,
                         String newFileContents) throws Exception {
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

}
