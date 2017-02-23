package oracle.apps.votuning_v2;

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.idAnalyzer.ComponentDetails;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ComponentOccurrenceCounter {

    /**
     * Number of jsff files
     */
    static int m_jsffCount = 0;

    /**
     * number of jspx files
     */
    static int m_jspxCount = 0;

    HashMap<String, Integer> m_countOfComponents =
        new HashMap<String, Integer>();

    private HashSet<String> componentsNamesToCount = new HashSet<String>();

    public ComponentOccurrenceCounter() {
        super();
        initializeCompoenntsToCount();
    }

    public void initializeCompoenntsToCount() {
        componentsNamesToCount.add("af:table");
        componentsNamesToCount.add("af:treeTable");
        componentsNamesToCount.add("af:inputComboboxListOfValues");
        componentsNamesToCount.add("af:selectOneChoice");
        componentsNamesToCount.add("af:selectOneListbox");
        componentsNamesToCount.add("af:selectManyListbox");
        componentsNamesToCount.add("af:selectManyChoice");
        componentsNamesToCount.add("af:selectManyCheckbox");
        componentsNamesToCount.add("af:inputListOfValues");

    }

    public static void main(String[] args) {

        if (args.length != 1 || isEmpty(args[0])) {
            System.out.println("Usage:  ComponentOccurrenceCounter.sh <path to your code in ade>");
            System.exit(1);
        }
        String sCrawlDir = args[0];
        if (isEmpty(sCrawlDir)) {
            System.out.println("Crawl directory empty");
            System.exit(1);
        }

        ComponentOccurrenceCounter counter = new ComponentOccurrenceCounter();
        counter.crawlDirectory(sCrawlDir);
        counter.printStatistics();
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
    private void crawlDirectory(String path) {
        if (path.contains(".ade_path"))
            return;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();
                if (isValidUIFile(absFilePath)) {


                    if (absFilePath.trim().endsWith(".jsff")) {
                        m_jsffCount++;
                    } else if (absFilePath.trim().endsWith(".jspx")) {
                        m_jspxCount++;
                    }
                    processUiFile(listOfFiles[i]);

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

    public void printStatistics() {
        System.out.println("Run Statistics");
        System.out.println("Total Number of jspx files reviewed: " +
                           m_jspxCount);
        System.out.println("Total Number of jsff files reviewed: " +
                           m_jsffCount + "\n");
        System.out.println("=============================");
        if (m_countOfComponents != null && m_countOfComponents.size() > 0) {
            Set<String> keys = m_countOfComponents.keySet();
            Iterator<String> keyIter = keys.iterator();
            while (keyIter.hasNext()) {
                String compName = keyIter.next();
                Integer cnt = m_countOfComponents.get(compName);
                System.out.println(String.format("component Name: %s : Count %s",
                                                 compName, cnt.toString()));
            }
        } else {
            System.out.println("No occurrencs of components");
        }
    }

    public void processUiFile(File file) {


        String absPath = file.getAbsolutePath();
        System.out.println("Proccessing file: " + absPath);
        XMLDocument uiXml = XMLParserHelper.getXMLDocument(absPath);
        Element rootNode = uiXml.getDocumentElement();

        if (rootNode.hasChildNodes()) {

            traverseNodes(rootNode.getChildNodes(), file);
        }

    }

    public boolean isValidDirectory(String absDirPath) {
        if (!absDirPath.contains("/model/") &&
            !absDirPath.contains("/dbSchema/") &&
            !absDirPath.contains("/publicModel/") &&
            !absDirPath.contains("/classes/") &&
            !absDirPath.contains("/test/")) {
            return true;
        }
        return false;
    }

    public boolean isValidUIFile(String absFilePath) {
        if (!isEmpty(absFilePath)) {
            if (absFilePath.contains("/test/") ||
                absFilePath.contains("/modelTest/") ||
                absFilePath.contains("/classes/") ||
                absFilePath.contains("/model/") ||
                absFilePath.contains("/publicModel/")) {
                return false;
            }
            if (absFilePath.endsWith(".jspx") ||
                absFilePath.endsWith(".jsff")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }

    public void incrementComponentCounter(String componentName) {
        if (isEmpty(componentName)) {
            return;
        }
        if (m_countOfComponents == null) {
            return;
        }

        Integer cnt = 0;
        if (m_countOfComponents.containsKey(componentName)) {
            cnt = m_countOfComponents.get(componentName);
            if (cnt == null) {
                cnt = 0;
            }
        }

        cnt++;

        m_countOfComponents.put(componentName, cnt);
    }

    public void traverseNodes(NodeList childNodes, File uiFile) {

        if (childNodes == null) {
            return;
        }

        int childNodeLength = childNodes.getLength();
        for (int i = 0; i < childNodeLength; i++) {
            Node child = childNodes.item(i);

            String componentName = child.getNodeName();

            if (!isEmpty(componentName) &&
                componentsNamesToCount.contains(componentName.trim())) {
                incrementComponentCounter(componentName);
            }

            if (child.hasChildNodes()) {
                traverseNodes(child.getChildNodes(), uiFile);
            }

        }
    }

}
