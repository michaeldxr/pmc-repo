package oracle.apps.idAnalyzer;

import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;

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


import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class NoIdAnalyzer {

    /**
     * Name of the diff file
     */
    final static String DIFF_FILE_NAME = "diff.txt";

    /**
     * for analysis purposes only - generates an xml file that contains a list of all the violations found
     */
    final static String OUTPUT_XML_FILE_NAME = "output.xml";

    /**
     * Bufferred writer for the diff.txt
     */
    private BufferedWriter diff;

    /**
     * total number of violations fixed
     */
    int m_totalViolationsFixed = 0;

    /**
     * total number of violations that could not be fixed due to formatting
     */
    int m_violationsNotFixed_formatting = 0;

    static HashMap<String, ArrayList<String>> map_linesNotFixed_formattig =
        new HashMap<String, ArrayList<String>>();

    /**
     * Map of all the violations. Used for reporting purposes
     */
    HashMap<String, ArrayList<ComponentDetails>> resultComponents =
        new HashMap<String, ArrayList<ComponentDetails>>(500);

    public NoIdAnalyzer() {
        super();
    }

    /**
     * Usage:  runNoIdAnalyzer.sh <path to your code in an open ade view> <Do ADE checkin/checkout>
     * @param args
     * arg[0] - path to the code in the open ade view
     * arg[1] - whether to do ade operations like check in and check out. Boolean value.
     *        - specify false if running locally and want to debug. Instaed of check-in/out will do println operations
     */
    public static void main(String[] args) {

        // Make sure it is run with correct number of arguments
        if (args.length != 2 || args[0] == null || args[0].equals("") ||
            args[1] == null || args[1].equals("")) {
            System.out.println("Usage:  runComponentIdAnalyzer.sh <path to your code in an open ade view> <Do ADE checkin/checkout>");
            System.exit(1);
        }

        Date startDate = new Date();

        // get arguments
        String sCrawlDir = args[0];
        Boolean bDoADE = new Boolean(args[1]);

        if (isEmpty(sCrawlDir)) {
            System.out.println("Crawl directory has to be specified");
            System.exit(1);
        }
        System.out.println("Analyzing all files in directory:" + sCrawlDir);

        NoIdAnalyzer componentanalyzer = new NoIdAnalyzer();

        try {
            if (bDoADE.booleanValue()) {
                // Remove any existing diff.txt. Create a new bufferred writer for diff.txt
                componentanalyzer.startDiffLog();
            }

            componentanalyzer.process(sCrawlDir, bDoADE);

            if (bDoADE.booleanValue()) {
                componentanalyzer.closeDiffLog();
            }

        } catch (Exception ex) {
            System.out.println("Exception occurred: " + ex.getMessage());
        }

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
        HashMap<String, ArrayList<ComponentDetails>> results =
            componentanalyzer.getResultComponents();
        Integer totalViolations = getTotalNumberOfViolations(results);
        System.out.println("Total violations: " + totalViolations);
        System.out.println("Number of components fixed: " +
                           componentanalyzer.getTotalViolationsFixed());
        System.out.println("Number of components not fixed due to formatting issues: " +
                           componentanalyzer.getViolationsNotFixed_formatting());
        System.out.println("=================================================");
        if (map_linesNotFixed_formattig != null &&
            map_linesNotFixed_formattig.size() > 0) {
            System.out.println("==============================================");
            System.out.println("Lines not fixed due to formatting");
            Set<String> file_set = map_linesNotFixed_formattig.keySet();
            Iterator<String> file_iter = file_set.iterator();
            while (file_iter.hasNext()) {
                String file = file_iter.next();
                ArrayList<String> list_lines =
                    map_linesNotFixed_formattig.get(file);
                Iterator<String> lines_iter = list_lines.iterator();
                while (lines_iter.hasNext()) {
                    String line = lines_iter.next();
                    System.out.println("File: " + file + " line: " + line);
                }

            }
            System.out.println("==============================================");
        }
    }

    /**
     * Based on the result map, finds out the total number of violations
     * @param results
     * Result Map
     * @return
     */
    public static Integer getTotalNumberOfViolations(HashMap<String, ArrayList<ComponentDetails>> results) {

        Integer totalViolations = 0;
        Set<String> filekeySet = results.keySet();
        Iterator<String> keyIter = filekeySet.iterator();
        while (keyIter.hasNext()) {

            String fileName = keyIter.next();
            ArrayList<ComponentDetails> components = results.get(fileName);
            totalViolations += components.size();
        }

        return totalViolations;
    }

    public void process(String crawlDirectory, boolean doADE) {

        crawlDirectory(crawlDirectory, doADE);

        if (!doADE) {
            // For analyzing purposes,not required when we are running the script in production
            writeOutputXml(resultComponents, OUTPUT_XML_FILE_NAME);
        }
    }

    /**
     * Process each valid ui file. If the file has any found violations, fix them and increment the violation count
     * @param file
     * @param bDoADE
     */
    public void processUiFile(File file, boolean bDoADE) {

        NoIdFileProcessor noComponentIdProcessor = new NoIdFileProcessor();

        String fixedFileContents =
            noComponentIdProcessor.processUiFileForNoIds(file, bDoADE);

        HashMap<Integer, ComponentDetails> all_comp_map =
            noComponentIdProcessor.getComponentsWithoutIds();
        ArrayList<ComponentDetails> components =
            noComponentIdProcessor.getAllViolatingComponents(all_comp_map);
        if (components.size() > 0) {
            try {
                if (isEmpty(fixedFileContents)) {
                    System.out.println("Skipping.. Error while processing file: " +
                                       file.getPath());
                    return;
                }

                applyFixedFile(file, fixedFileContents, bDoADE);
            } catch (Exception ex) {
                System.out.println("Exception while trying to fix file: " +
                                   file.getPath());
                ex.printStackTrace();
            }
            resultComponents.put(file.getPath(), components);
        }
        m_totalViolationsFixed += noComponentIdProcessor.getViolationsFixed();
        m_violationsNotFixed_formatting +=
                noComponentIdProcessor.getViolationsNotFixed_formatting();

        ArrayList<String> list_linesNotfixed =
            noComponentIdProcessor.getLinesNotFixedDueToFormatting();
        if (list_linesNotfixed != null && list_linesNotfixed.size() > 0) {
            map_linesNotFixed_formattig.put(file.getAbsolutePath(),
                                            list_linesNotfixed);
        }


    }

    public void applyFixedFile(File file, String newFileContents,
                               boolean bDoAde) throws Exception {
        if (bDoAde) {
            checkoutAndDelete(file);

            FileWriter fw = new FileWriter(file.getAbsolutePath());
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write(newFileContents);
            bw.close();

            checkinAndDiff(file);
        }

    }

    private void checkinAndDiff(File f) throws Exception {

        Process ciProcess =
            Runtime.getRuntime().exec("ade ci " + f.getAbsolutePath());
        checkProcess(ciProcess, f, "checkin", false);

        String diffCmd = "ade diff -label " + f.getAbsolutePath();
        Process diffProcess = Runtime.getRuntime().exec(diffCmd);
        checkProcess(diffProcess, f, "diff file with previous in label", true);
    }

    private void checkoutAndDelete(File f) throws Exception {

        Process coProcess =
            Runtime.getRuntime().exec("ade co -nc " + f.getAbsolutePath());
        checkProcess(coProcess, f, "checkout", false);

        Process rmProcess =
            Runtime.getRuntime().exec("rm -f " + f.getAbsolutePath());
        checkProcess(rmProcess, f, "remove old file", false);


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
    private void crawlDirectory(String path, Boolean bDoADE) {
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
                    processUiFile(listOfFiles[i], bDoADE);
                }
            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                if (isValidDirectory(dir)) {
                    crawlDirectory(dir, bDoADE);
                }
            } else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }

    /**
     * Valid directories are those that do not contain
     * - model and dbschema and publicModel and test and classes
     * @param absDirPath
     * @return
     */
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

    /**
     * Valid ui files are those that
     * - end with jspx or jsff
     * - and are not in a directory containing test or modeltest or classes or model or publicmodel
     * @param absFilePath
     * @return
     */
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

    /**
     *checks whether the given string is empty
     * @param str
     * @return
     */
    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }

    /**
     *Starts the diff log
     * Removes any existing diff file and creates a new diff file
     * @throws Exception
     */
    private void startDiffLog() throws Exception {
        // Remove any existing diff.txt
        Process p = Runtime.getRuntime().exec("rm -f " + DIFF_FILE_NAME);
        checkProcess(p, null, "remove any existing diff log", false);

        diff = new BufferedWriter(new FileWriter(DIFF_FILE_NAME));
    }

    /**
     *Checks whether the process exited correctly
     * @param p
     * @param f
     * @param description
     * @param isDiff
     * @throws Exception
     */
    private void checkProcess(Process p, File f, String description,
                              boolean isDiff) throws Exception {
        int exitCode = p.waitFor();
        if (!isDiff && exitCode != 0) {
            if (f != null)
                System.out.println("There was an error when trying to " +
                                   description + " for file : " +
                                   f.getAbsolutePath());
            else
                System.out.println("There was an error when trying to " +
                                   description);
        }

        dumpInputStream(p.getInputStream(), isDiff);
        dumpInputStream(p.getErrorStream(), isDiff);
    }

    /**
     *Error while trying to interact with ade
     * @param in
     * @param isDiff
     * @throws Exception
     */
    private void dumpInputStream(InputStream in,
                                 boolean isDiff) throws Exception {
        int n = in.available();
        byte b[] = new byte[n];
        in.read(b, 0, n);
        String str = new String(b);
        if (str.contains("ade ERROR")) {
            System.out.println(str);
            System.out.println("There was an error interacting with ADE, exiting script.");
            System.exit(-1);
        }

        if (isDiff == false)
            System.out.println(str.trim());
        else
            diff.write(str);
    }

    /**
     *Closes the diff log
     * @throws Exception
     */
    private void closeDiffLog() throws Exception {
        diff.close();
    }

    private void writeOutputXml(HashMap<String, ArrayList<ComponentDetails>> results,
                                String xmlFileName) {
        String ROOT_NODE = "componentsWithoutIds";
        String FILE_NODE = "file";
        String TOTAL_VIOLATIONS_NODE = "NumberOfViolations";
        String PER_FILE_VIOLATIONS = "Violations";
        String FILE_PATH_ATTR = "filePath";
        String NUMBER_OF_COMP = "numberOfComponents";
        String COMPONENTS = "components";
        String COMPONENT = "component";
        String COMPONENT_NAME_ATTR = "name";
        String FILE_PRODUCT_ATTR = "product";
        String FILE_NAME_ATTR = "fileName";
        String LINE_NUM_ATTR = "lineNumber";

        Document doc = createXmlDocument();
        Element rootNode = doc.createElement(ROOT_NODE);
        Element totalViolationsNode = doc.createElement(TOTAL_VIOLATIONS_NODE);
        Integer totalViolations = getTotalNumberOfViolations(resultComponents);
        totalViolationsNode.setNodeValue(totalViolations.toString());
        rootNode.appendChild(totalViolationsNode);
        Set<String> keySet = results.keySet();
        Iterator<String> keyIter = keySet.iterator();
        while (keyIter.hasNext()) {

            String fileName = keyIter.next();
            ArrayList<ComponentDetails> components = results.get(fileName);
            Integer numOfViol = components.size();
            Element fileNode = doc.createElement(FILE_NODE);
            fileNode.setAttribute(FILE_PATH_ATTR, fileName);
            fileNode.setAttribute(PER_FILE_VIOLATIONS, numOfViol.toString());

            Iterator<ComponentDetails> componentIter = components.iterator();
            Element componentsNode = doc.createElement(COMPONENTS);
            Integer numberofComp = components.size();
            componentsNode.setAttribute(NUMBER_OF_COMP,
                                        numberofComp.toString());
            while (componentIter.hasNext()) {

                ComponentDetails component = componentIter.next();


                Element componentNode = doc.createElement(COMPONENT);
                componentNode.setAttribute(COMPONENT_NAME_ATTR,
                                           component.getComponentName());
                componentNode.setAttribute(FILE_PRODUCT_ATTR,
                                           component.getProduct());
                componentNode.setAttribute(FILE_NAME_ATTR,
                                           component.getFileName());
                componentNode.setAttribute(LINE_NUM_ATTR,
                                           component.getSourceLineNumber().toString());
                componentsNode.appendChild(componentNode);
            }
            fileNode.appendChild(componentsNode);
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


    public HashMap<String, ArrayList<ComponentDetails>> getResultComponents() {
        return resultComponents;
    }

    public int getTotalViolationsFixed() {
        return m_totalViolationsFixed;
    }

    public int getViolationsNotFixed_formatting() {
        return m_violationsNotFixed_formatting;
    }
}
