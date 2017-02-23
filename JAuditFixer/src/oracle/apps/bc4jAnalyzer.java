package oracle.apps;

import java.io.File;

import java.util.ArrayList;

import java.util.Date;
import java.util.HashMap;

import java.util.Iterator;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class bc4jAnalyzer {
    public bc4jAnalyzer() {
        super();
    }
    public HashMap<String, ArrayList<Record>> map_records =
        new HashMap<String, ArrayList<Record>>();

    public class Record {
        public String appConfigName = "";
        public String applicationName = "";
        public String deployPlatform = "";
        public String jdbcDS = "";
        public String dbLockingMode = "";
        public String dbJboTypeMapEntries = "";
        public String securityJndiName = "";
        public String ampoolingMaxPoolSize = "";
        public String ampoolingUseExclusive = "";

    }

    /**
     * Number of java files
     */
    static int m_xcfgCnt = 0;

    public static void main(String[] args) {

        // Make sure it is run with correct number of arguments
        if (args.length != 1 || args[0] == null) {
            System.out.println("Usage:  bc4jAnalyzer.sh <path to your code in an open ade view>");
            System.exit(1);
        }


        Date startDate = new Date();

        // get arguments
        String sCrawlDir = args[0];
        if (isEmpty(sCrawlDir)) {
            System.out.println("Crawl directory has to be specified");
            System.exit(1);
        }
        System.out.println("Analyzing all files in directory:" + sCrawlDir);

        bc4jAnalyzer bc4jAnalyzer = new bc4jAnalyzer();


        try {
            bc4jAnalyzer.crawlDirectory(sCrawlDir);
        } catch (Exception ex) {
            System.out.println("Exception occurred: " + ex.getMessage());
        }

        try {
            bc4jAnalyzer.writeOutputXml(bc4jAnalyzer.map_records,
                                               "output.xml");
        } catch (Exception ex) {
            System.out.println("Exception in writing xml");
        }

        bc4jAnalyzer.printStatistics(startDate);

    }
    public void printStatistics(Date startDate) {
        // Print Statistics
        Date endDate = new Date();
        System.out.println("=================================================");
        System.out.println("Run Statistics");
        System.out.println("Start Time: " + startDate.toString());
        System.out.println("End Time: " + endDate.toString());
        long elapsedTime = endDate.getTime() - startDate.getTime();
        System.out.println("Elapsed Time (sec): " + elapsedTime / 1000);
        System.out.println("=================================================");

        System.out.println("=================================================");
        System.out.println("Total Number of java files analyzed: " +
                           m_xcfgCnt);
       
        System.out.println("=================================================");

    }

    /**
     * recurse over the crawl directory and for each valid ui file - process it.
     * Valid directories -
     * - should not contain, /dbSchema/ or /model/ or /publicModel/ or /test/ or /classes/
     * Valid UI files
     * - jspx or jsff extensions
     * - and not in folder with /classes/ or /test/ or /model/ or /publicModel/
     * @param path
     * @param bDoADE
     */
    private void crawlDirectory(String crawlDir) {
        if (crawlDir.contains(".ade_path"))
            return;

        File folder = new File(crawlDir);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();
                if (!isEmpty(absFilePath) && absFilePath.endsWith(".xcfg")) {
                    m_xcfgCnt++;

                    processXcfgFileFile(listOfFiles[i]);
                }

            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                if (isValidDirectory(dir)) {
                    crawlDirectory(dir);
                }

            } else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }

    public boolean isValidDirectory(String absDirPath) {
        if (!absDirPath.contains("/dbSchema/") &&
            !absDirPath.contains("/classes/") &&
            !absDirPath.contains("/test/")) {
            return true;
        }
        return false;
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }

    public void processXcfgFileFile(File file) {

        ArrayList<Record> list_records = new ArrayList<Record>();
        String absPath = file.getAbsolutePath();
         System.out.println("Processing file: " + absPath);

        XMLDocument xmlDoc = XMLParserHelper.getXMLDocument(absPath);
        NodeList bc4jNodes = xmlDoc.getElementsByTagName("BC4JConfig");
        if (!XMLParserHelper.hasOneNOnlyOneNode(bc4jNodes)) {
            System.out.println(String.format("[Error] Skipping File: %s.. expected one and only one BC4jConfig node",
                                             absPath));
            return;
        }
        Node bc4jconfigNode = bc4jNodes.item(0);

        Node appModuleConfigBag =
            XMLParserHelper.getChildNodeWithName(bc4jconfigNode,
                                                 "AppModuleConfigBag");
        if (appModuleConfigBag == null) {
            System.out.println("Skipping.. Did not find tag AppModuleConfigBag in file: " +
                               absPath);
            return;
        }
        NodeList appModuleConfigChildren = appModuleConfigBag.getChildNodes();
        if (appModuleConfigChildren == null ||
            appModuleConfigChildren.getLength() <= 0) {
            System.out.println("No config children.. skipping.. " + absPath);
            return;
        }
        for (int i = 0; i < appModuleConfigChildren.getLength(); i++) {
            Node appConfigChild = appModuleConfigChildren.item(i);
            Record rec = new Record();
            String nodeName = appConfigChild.getNodeName();
            if (!isEmpty(nodeName) &&
                nodeName.trim().equals("AppModuleConfig")) {
                String appModuleConfigName =
                    XMLParserHelper.getAttributeValue(appConfigChild, "name");
                rec.appConfigName = appModuleConfigName;
                String appName =
                    XMLParserHelper.getAttributeValue(appConfigChild,
                                                      "ApplicationName");
                rec.applicationName = appName;
                String deplPlat =
                    XMLParserHelper.getAttributeValue(appConfigChild,
                                                      "DeployPlatform");
                rec.deployPlatform = deplPlat;
                Node customNode =
                    XMLParserHelper.getChildNodeWithName(appConfigChild,
                                                         "Custom");
                if (customNode != null) {
                    String jdbcSrc =
                        XMLParserHelper.getAttributeValue(customNode,
                                                          "JDBCDataSource");
                    rec.jdbcDS = jdbcSrc;
                }
                Node databaseNode =
                    XMLParserHelper.getChildNodeWithName(appConfigChild,
                                                         "Database");
                if (databaseNode != null) {
                    String lockingMode =
                        XMLParserHelper.getAttributeValue(databaseNode,
                                                          "jbo.locking.mode");
                    rec.dbLockingMode = lockingMode;
                }
                Node securityNode =
                    XMLParserHelper.getChildNodeWithName(appConfigChild,
                                                         "Security");
                if (securityNode != null) {
                    String jndi =
                        XMLParserHelper.getAttributeValue(securityNode,
                                                          "AppModuleJndiName");
                    rec.securityJndiName = jndi;
                }
                Node ampoolingNode =
                    XMLParserHelper.getChildNodeWithName(appConfigChild,
                                                         "AM-Pooling");
                if (ampoolingNode != null) {
                    String poolsize =
                        XMLParserHelper.getAttributeValue(ampoolingNode,
                                                          "jbo.ampool.maxpoolsize");
                    rec.ampoolingMaxPoolSize = poolsize;
                    String exclusivemode =
                        XMLParserHelper.getAttributeValue(ampoolingNode,
                                                          "jbo.ampool.isuseexclusive");
                    rec.ampoolingUseExclusive = exclusivemode;
                }

                list_records.add(rec);

            }
        }
        map_records.put(absPath, list_records);

    }

    /**
     * Gets the product name from the file path
     *
     * @param uiFilePath
     * @return
     */
    public String getProductName(String uiFilePath) {
        String productName = "";
        Integer productIndex = 5;
        // file path: /ade/<view_name>/fusionapps/fin/components/ledger/gl
        if (uiFilePath.contains("/fusionapps/")) {
            Integer fusionappsIndex = uiFilePath.indexOf("/fusionapps/");
            String path_after_fusionapps =
                uiFilePath.substring(fusionappsIndex);
            if (!isEmpty(path_after_fusionapps)) {
                String[] parts = path_after_fusionapps.split("/");
                if (parts != null & parts.length > productIndex) {
                    return parts[productIndex];
                }
            }
        }
        return "";
    }

    private void writeOutputXml(HashMap<String, ArrayList<Record>> mapToWrite,
                                String xmlFileName) {
        String ROOT_NODE = "bc4jAnalysis";
        String FILE_NODE = "file";
        String FILE_NAME_ATTR = "fileName";
        String FILE_PATH_ATTR = "filePath";
        String AM_CONFIGS = "configs";
        String AM_CONFIG = "config";
        String AM_CONFIG_NAME = "name";
        String AM_CONFIG_APP = "application";
        String AM_DEPLOYPLAT = "deployPlatform";
        String AM_JDBC = "jdbcConnection";
        String DB_LOCKINGMODE = "DbLockingMode";
        String PRODUCT = "product";
        Document doc = createXmlDocument();
        Element rootNode = doc.createElement(ROOT_NODE);

        Set<String> keySet = mapToWrite.keySet();
        Iterator<String> keyIter = keySet.iterator();
        while (keyIter.hasNext()) {
            String filePath = keyIter.next();
            String fileName = "";
            int lastSlashIndex = filePath.lastIndexOf("/");
            if (lastSlashIndex != -1) {
                fileName = filePath.substring(lastSlashIndex + 1);
            }

            Element fileNode = doc.createElement(FILE_NODE);
            String prod = getProductName(filePath);
            fileNode.setAttribute(PRODUCT, prod);
            fileNode.setAttribute(FILE_NAME_ATTR, fileName);
            fileNode.setAttribute(FILE_PATH_ATTR, filePath);

            Element configsNode = doc.createElement(AM_CONFIGS);

            ArrayList<Record> recordsList = mapToWrite.get(filePath);
            if (recordsList != null && recordsList.size() > 0) {
                Iterator<Record> recordIter = recordsList.iterator();
                while (recordIter.hasNext()) {
                    Element recordNode = doc.createElement(AM_CONFIG);
                    Record rec = recordIter.next();

                    if (!isEmpty(rec.appConfigName)) {
                        recordNode.setAttribute(AM_CONFIG_NAME,
                                                rec.appConfigName);
                    }
                    if (!isEmpty(rec.applicationName)) {
                        recordNode.setAttribute(AM_CONFIG_APP,
                                                rec.applicationName);
                    }
                    if (!isEmpty(rec.deployPlatform)) {
                        recordNode.setAttribute(AM_DEPLOYPLAT,
                                                rec.deployPlatform);
                    }
                    if (!isEmpty(rec.jdbcDS)) {
                        recordNode.setAttribute("jdbcString",
                                                rec.jdbcDS);
                    }
                    if (!isEmpty(rec.dbLockingMode)) {
                        recordNode.setAttribute("dbLockingMode",
                                                rec.dbLockingMode);
                    }
                    if (!isEmpty(rec.dbJboTypeMapEntries)) {
                        recordNode.setAttribute("typeMapEntries",
                                                rec.dbJboTypeMapEntries);
                    }
                    if (!isEmpty(rec.ampoolingMaxPoolSize)) {
                        recordNode.setAttribute("appPoolingMaxSize",
                                                rec.ampoolingMaxPoolSize);
                    }
                    if (!isEmpty(rec.ampoolingUseExclusive)) {
                        recordNode.setAttribute("ampoolingUseExclusive",
                                                rec.ampoolingUseExclusive);
                    }
                    if (!isEmpty(rec.securityJndiName)) {
                        recordNode.setAttribute("securityJndiName",
                                                rec.securityJndiName);
                    }
                   
                    configsNode.appendChild(recordNode);
                }
            }
            fileNode.appendChild(configsNode);
            rootNode.appendChild(fileNode);

        }

        doc.appendChild(rootNode);
        writeXmlFile(doc, xmlFileName);
    }

    private Document createXmlDocument() {
        try {
            DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
            DocumentBuilder bd = fact.newDocumentBuilder();
            return bd.newDocument();
        } catch (Exception ex) {
            System.out.println("Exception while trying to create xml document");
        }
        return null;
    }

    // This method writes a DOM document to a file

    private void writeXmlFile(Document doc, String filename) {
        try {
            // Prepare the DOM document for writing
            Source source = new DOMSource(doc);

            // Prepare the output file
            File file = new File(filename);
            Result result = new StreamResult(file);

            // Write the DOM document to the file
            Transformer xformer =
                TransformerFactory.newInstance().newTransformer();
            xformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            System.out.println("Exception while writing xml output");
        } catch (TransformerException e) {
            System.out.println("Exception while writing xml output");
        }
    }
}
