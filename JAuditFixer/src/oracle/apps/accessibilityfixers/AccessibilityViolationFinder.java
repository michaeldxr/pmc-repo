package oracle.apps.accessibilityfixers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;
import oracle.apps.helpers.Mail;
import oracle.apps.utility.JoesBaseClass;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLParseException;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;


public class AccessibilityViolationFinder extends JoesBaseClass {

    private Workbook workbook;
    private Worksheet worksheet;
    private Worksheet validationSheet;
    private int currentRow = 2;

    List<JAuditSearch> jAuditSearches;

    private HashMap<String, HashMap<String, String>> jprMappings;
    private DOMParser parser = new DOMParser();

    private HashMap<String, Integer> violationsCount = new HashMap<String, Integer>(10);

    public AccessibilityViolationFinder(List<JAuditSearch> jAuditSearches) {
        super(CRAWL_TYPE.JSPX_AND_JSFF);

        this.jAuditSearches = jAuditSearches;

        workbook = new Workbook("Accessibility Finder");
        worksheet = new Worksheet("Main");
        validationSheet = new Worksheet("Validation");

        // filename
        worksheet.setColumnWidth(1, 30);
        // path
        worksheet.setColumnWidth(2, 5);
        // Component Type
        worksheet.setColumnWidth(3, 24);
        // tag id
        worksheet.setColumnWidth(4, 10);
        // attribute
        worksheet.setColumnWidth(5, 15);
        // attribute value(s)
        worksheet.setColumnWidth(6, 60);
        worksheet.setColumnWidth(7, 36);
        // JAudit description
        worksheet.setColumnWidth(8, 37);

        worksheet.addCell(new Cell(new CellLocation("A1"),
                                   new CellValue("File Name")));
        worksheet.addCell(new Cell(new CellLocation("B1"),
                                   new CellValue("Path")));
        worksheet.addCell(new Cell(new CellLocation("C1"),
                                   new CellValue("Comp. Type")));
        worksheet.addCell(new Cell(new CellLocation("D1"),
                                   new CellValue("Comp. ID")));
        worksheet.addCell(new Cell(new CellLocation("E1"),
                                   new CellValue("Comp. Property")));

        worksheet.addCell(new Cell(new CellLocation("F1"),
                                   new CellValue("Comp. Property Value")));
        worksheet.addCell(new Cell(new CellLocation("G1"),
                                   new CellValue("Comp. Property Value Part B (optional)")));
        worksheet.addCell(new Cell(new CellLocation("H1"),
                                   new CellValue("JAudit Description")));

        validationSheet.addCell(new Cell(new CellLocation("A1"),
                                         new CellValue("This spreadsheet contains data validations for the Main spreadhseet.")));

        workbook.addWorksheet(worksheet);
        workbook.addWorksheet(validationSheet);
    }

    public final void start(String crawlDirectory, String outputFilename) {
        // Build the xlf mappings first, if this is the first run.
        UIBundleFinder uiBundleFinder = new UIBundleFinder();
        System.out.println();
        uiBundleFinder.start(crawlDirectory);
        jprMappings = uiBundleFinder.getMappings();
        if (jprMappings.size() == 0) {
            System.err.println("FATAL ERROR: No JPR files found in any subdirectory. Please run this at a project level or higher.");
            System.exit(0);
        }

        System.out.println();
        uiBundleFinder.printMappings();
        System.out.println();
        System.out.println("===================================");
        System.out.println("Searching directory for UI files...");
        System.out.println("===================================");

        super.crawlDirectory(crawlDirectory, false);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(crawlDirectory + "/" + outputFilename);
            XLSXCreator.create(workbook, out);
        }
        catch (Exception e) {
            System.err.println("ERROR: Unable to create xlsx. Is this a read-only file-system?");
            this.logException("error in creating xlsx.", e);
        }
    }

    protected void processFile(File f, boolean bDoADE) {
        try {
            System.out.println("INFO: Reviewing file:  " + f.getAbsolutePath());
            parser.reset();
            try {
                parser.setErrorStream(System.out);
                parser.setValidationMode(DOMParser.NONVALIDATING);
                parser.showWarnings(true);

                parser.parse("file:" + f.getAbsolutePath());
            }
            catch (Exception e) {
                System.err.println("ERROR:  XML Parse error while parsing file:  " + f.getAbsolutePath());
                System.err.println("Skipping...");
                this.logException("Error parsing file: " + f.getAbsolutePath(),
                                  e);
                return;
            }

            XMLDocument doc = parser.getDocument();
            List<String> xlfKeys = new ArrayList<String>();
            String jprName = null;
            try {
                jprName = identifyJPR(f).getName();
            }
            catch (NullPointerException e) {
                System.err.println("ERROR: File " + f.getAbsolutePath() + " has no JPR. Skipping...");
                return;
            }

            HashMap<String, String> xlfMappings = jprMappings.get(jprName);
            if (xlfMappings != null) {
                Iterator<String> xlfFQNs = xlfMappings.keySet().iterator();
                try {
                    while (xlfFQNs.hasNext()) {
                        String xlfFQN = xlfFQNs.next();
                        String jarPath = xlfMappings.get(xlfFQN);
                        JarFile jarFile = new JarFile(jarPath);
                        ZipEntry xlfEntry = jarFile.getEntry(xlfFQN.replace('.',
                                                                            '/') + ".xlf");
                        xlfKeys.addAll(findKeys(getNameFromFQN(xlfFQN),
                                                jarFile.getInputStream(xlfEntry)));
                    }
                }
                catch (Exception e) {
                    this.logException("Error populating strings dropdown for file: " + f.getAbsolutePath(),
                                      e);
                    System.err.println("ERROR: Error with parsing resource bundles for file: " + f.getAbsolutePath());
                    System.err.println("Skipping...");
                    return;
                }
            }
            else {
                System.err.println("WARNING: File " + f.getAbsolutePath() + " has no resource bundles associated with it.");
            }

            for (JAuditSearch jAuditSearch : jAuditSearches) {
                List<String> filteredXLFKeys = new ArrayList<String>(xlfKeys.size());
                for (String xlfKey : xlfKeys) {
                    if (jAuditSearch.getXlfKeyRegex().matcher(xlfKey)
                        .matches())
                        filteredXLFKeys.add(xlfKey);
                }
                searchForJAuditViolation(f, doc, filteredXLFKeys,
                                         jAuditSearch);
            }
        }
        catch (Exception e) {
            System.err.println("ERROR: Unexpected error in file: " + f.getAbsoluteFile());
            System.err.println("Skipping...");
            this.logException("Unhandled exception for file : " + f.getAbsolutePath(),
                              e);
        }
    }

    private void searchForJAuditViolation(File f, XMLDocument doc, List<String> xlfKeys, JAuditSearch jAuditSearch) {
        int numTags = 0;
        NodeList tags;
        for (JAuditSearch.Component component : jAuditSearch.getComponents()) {
            tags = doc.getElementsByTagName(component.name());
            for (int i = 0; i < tags.getLength(); i++) {

                String id = null;
                String attribute = null;
                Node parentNode, attributeNode;
                // FILTER!!
                switch (jAuditSearch.getJAudit()) {
                case TABLE_SUMMARIES:
                    attribute = "summary";
                    attributeNode = tags.item(i).getAttributes()
                            .getNamedItem(attribute);
                    if (attributeNode != null && !attributeNode.getNodeValue()
                        .trim().isEmpty())
                        continue;
                    break;
                case COMP_LABELED:
                    // THE MOST COMPLICATED JAUDIT CHECK EVER!
                    attribute = "label";
                    boolean failsLabelTest = false;
                    Element element = (Element)tags.item(i);

                    String label = element.getAttributeNS(null, "label");
                    String shortDesc = element.getAttributeNS(null,
                                                              "shortDesc");
                    String labelAndAccessKey = element.getAttributeNS(null,
                                                                      "labelAndAccessKey");
                    if ((label == null || label.trim()
                         .isEmpty()) && (shortDesc == null || shortDesc.trim()
                                         .isEmpty()) && (labelAndAccessKey == null || labelAndAccessKey.trim()
                                                         .isEmpty())) {
                        if (component.equals(JAuditSearch.Component.selectItem)) {
                            break;
                        }
                        else {
                            failsLabelTest = true;
                            if (component.equals(JAuditSearch.Component.selectBooleanCheckbox) || component.equals(JAuditSearch.Component.selectBooleanRadio)) {
                                String text = element.getAttributeNS(null,
                                                                     "text");
                                String textAndAccessKey = element.getAttributeNS(null,
                                                                                 "textAndAccessKey");
                                if ((text != null && !text.trim()
                                     .isEmpty()) || (textAndAccessKey != null && !textAndAccessKey.trim()
                                                     .isEmpty())) {
                                    failsLabelTest = false;
                                }
                            }
                        }
                    }

                    if (failsLabelTest) {
                        NodeList panelLabelAndMessages = doc.getElementsByTagName(JAuditSearch.Component.panelLabelAndMessage.name());
                        NodeList outputLabels = doc.getElementsByTagName(JAuditSearch.Component.outputLabel.name());
                        Attr forAttr;
                        for (int j = 0; j < panelLabelAndMessages.getLength();
                             j++) {
                            forAttr = ((Element)panelLabelAndMessages.item(j))
                                    .getAttributeNodeNS(null, "for");
                            if (forAttr != null && forAttr.getValue()
                                .equals(element.getAttributeNS(null, "id"))) {
                                failsLabelTest = false;
                                break;
                                }
                        }
                        for (int j = 0; j < outputLabels.getLength(); j++) {
                            forAttr = ((Element)outputLabels.item(j))
                                    .getAttributeNodeNS(null, "for");
                            if (forAttr != null && forAttr.getValue()
                                .equals(element.getAttributeNS(null, "id")))
                                failsLabelTest = false;
                                break;
                        }
                        if (!failsLabelTest)
                            continue;
                    }
                    else {
                        continue;
                    }
                    break;
                case COMP_TITLE:
                    attribute = "title";
                    attributeNode = tags.item(i).getAttributes()
                            .getNamedItem("title");
                    if (attributeNode != null && !attributeNode.getNodeValue()
                        .trim().isEmpty())
                        continue;
                    break;
                case COLUMN_HEADERTEXT:
                    parentNode = tags.item(i).getParentNode();
                    if (!parentNode.getNodeName()
                        .equals("af:table") && !parentNode.getNodeName()
                        .equals("af:treeTable"))
                        continue;
                    attribute = "headerText";
                    attributeNode = tags.item(i).getAttributes()
                            .getNamedItem(attribute);
                    if (attributeNode != null && !attributeNode.getNodeValue()
                        .trim().isEmpty())
                        continue;
                    Node rowHeaderAttr = tags.item(i).getAttributes()
                        .getNamedItem("rowHeader");
                    // if rowHeader="true" and width="5", a headerText is not required.
                    if (rowHeaderAttr != null && rowHeaderAttr.getNodeValue()
                        .equals("true")) {
                        Node child = tags.item(i).getFirstChild();
                        if (child == null)
                            continue;
                        else if (child.getNextSibling() == null && child.getNodeName().equals("af:outputText") &&
                                 (child.getAttributes().getNamedItem("value") == null || child.getAttributes().getNamedItem("value").getNodeValue().isEmpty()))
                            continue;
                    }
                    break;
                case IMAGE_SHORTDESC:
                    attribute = "shortDesc";
                    attributeNode = tags.item(i).getAttributes()
                            .getNamedItem(attribute);
                    if (attributeNode != null)
                        continue;
                    break;
                case COMP_TEXT:
                    attribute = "text";
                    attributeNode = tags.item(i).getAttributes()
                            .getNamedItem(attribute);
                    if (attributeNode != null && !attributeNode.getNodeValue()
                        .trim().isEmpty())
                        continue;
                }
                
                Node idNode = tags.item(i).getAttributes().getNamedItem("id");
                if (idNode != null)
                    id = idNode.getNodeValue();
                else {
                    System.err.println("ERROR: A component of type " + component.toString() + " has no 'id' property in file " + f.getAbsolutePath());
                    System.err.println("Skipping component...");
                    continue;
                }

                numTags++;

                Integer count = violationsCount.get(jAuditSearch.getJAudit()
                                                    .toString());
                if (count == null) {
                    violationsCount.put(jAuditSearch.getJAudit().toString(),
                                        new Integer(1));
                }
                else {
                    violationsCount.put(jAuditSearch.getJAudit().toString(),
                                        count + 1);
                }

                // File name
                CellLocation l1 = new CellLocation("A" + currentRow);
                CellValue v1 = new CellValue(f.getName());
                worksheet.addCell(new Cell(l1, v1));

                // File path
                CellLocation l2 = new CellLocation("B" + currentRow);
                CellValue v2 = new CellValue(f.getAbsolutePath());
                worksheet.addCell(new Cell(l2, v2));

                // Component type
                CellLocation l3 = new CellLocation("C" + currentRow);
                CellValue v3 = new CellValue(component.toString());
                worksheet.addCell(new Cell(l3, v3));

                // Component ID
                CellLocation l4 = new CellLocation("D" + currentRow);
                CellValue v4 = new CellValue(id);
                worksheet.addCell(new Cell(l4, v4));
                StringWriter sw = printNodeToString(tags.item(i), "",
                                                    new StringWriter(), false);
                String nodeSource = sw.getBuffer().toString();
                worksheet.createComment(nodeSource, "D" + currentRow, 3,
                                        20);

                // Atrribute name
                CellLocation l5 = new CellLocation("E" + currentRow);
                CellValue v5 = new CellValue(attribute);
                worksheet.addCell(new Cell(l5, v5));

                // JAuditDescription
                CellLocation l8 = new CellLocation("H" + currentRow);
                CellValue v8 = new CellValue(jAuditSearch.getJAudit()
                                             .toString());
                worksheet.addCell(new Cell(l8, v8));

                currentRow++;
            }

            if (jAuditSearch.getJAudit()
                .equals(JAuditSearch.JAudit.IMAGE_SHORTDESC))
                xlfKeys.add(0, JAuditSearch.IMAGE_EXTRA_OPTION);

            if (numTags != 0 && xlfKeys.size() > 0) {
                // Add validation: Step 2) Build the validation table.
                int validationRow = currentRow - 1;
                int currentColumn = 0;
                CellLocation l;
                CellValue v;
                Cell c;
                for (; currentColumn < xlfKeys.size(); currentColumn++) {
                    l = new CellLocation(validationRow, currentColumn);
                    v = new CellValue(xlfKeys.get(currentColumn));
                    c = new Cell(l, v);
                    validationSheet.addCell(c);
                }
                currentColumn--;
                String colListName = "col_list" + validationRow;
                String start = getExactCellLocation(0, validationRow);
                String end = getExactCellLocation(currentColumn,
                                                  validationRow);
                workbook.defineName(colListName, "Validation", start, end);
                worksheet.createDataValidation(colListName,
                                               "F" + (validationRow - numTags + 1),
                                               "F" + validationRow, false);
                if (jAuditSearch.getJAudit()
                    .equals(JAuditSearch.JAudit.TABLE_SUMMARIES))
                    worksheet.createDataValidation(colListName,
                                                   "G" + (validationRow - numTags + 1),
                                                   "G" + validationRow, false);
            }
        }
    }

    /**
     * Finds all string key-value pairs in the xlf and returns a list of keys
     * in the proper Excel dropdown format: ['&lt;StringType&gt;.&lt;Key&gt;'] - &lt;Value&gt;
     * @param xlf an input stream of the .xlf file to search
     * @return a list of key-value pairs of resource strings in the excel
     * dropdown format.
     * @throws IOException
     * @throws XMLParseException
     * @throws SAXException
     */
    private List<String> findKeys(String bundleName, InputStream xlf) throws IOException,
                                                                             XMLParseException,
                                                                             SAXException {
        parser.reset();
        parser.setErrorStream(System.out);
        parser.setValidationMode(DOMParser.NONVALIDATING);
        parser.showWarnings(true);
        parser.parse(xlf);
        XMLDocument doc = parser.getDocument();

        NodeList transUnits = doc.getElementsByTagName("trans-unit");
        List<String> keys = new ArrayList<String>();
        // Search through all of the <trans-unit> tags
        StringBuilder sb;
        for (int i = 0; i < transUnits.getLength(); i++) {
            sb = new StringBuilder();
            Node transUnit = transUnits.item(i);
            sb.append(bundleName + ": ['" + transUnit.getAttributes()
                      .getNamedItem("id").getNodeValue() + "']");
            NodeList children = transUnit.getChildNodes();
            // Find the <source> tag in each trans unit and try to find a match.
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (child.getNodeName().equals("source")) {
                    sb.append(" - " + child.getTextContent().trim());
                    break;
                }
            }
            keys.add(sb.toString());
        }

        return keys;
    }

    private String getNameFromFQN(String fqn) {
        Matcher m = Pattern.compile(".+\\.(.+)").matcher(fqn);
        if (m.matches()) {
            return m.group(1);
        }
        else
            throw new RuntimeException("getNameFromFQN was not fed an FQN");
    }

    private File identifyJPR(File path) throws NullPointerException {
        File parent = path.getParentFile();
        File[] listOfFiles = parent.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            String fileName = listOfFiles[i].getAbsolutePath();
            if (fileName.endsWith(".jpr"))
                return listOfFiles[i];
        }
        return identifyJPR(parent);
    }

    private static StringWriter printNodeToString(Node node, String indent, StringWriter sw, boolean recurseChildren) {

        switch (node.getNodeType()) {

        case Node.DOCUMENT_NODE:
            String str = "<xml version=\"1.0\">\n";
            sw.append(str);

            // recurse on each child

            NodeList nodes = node.getChildNodes();
            if (recurseChildren && nodes != null) {
                for (int i = 0; i < nodes.getLength(); i++) {
                    printNodeToString(nodes.item(i), "", sw, recurseChildren);
                }
            }
            break;

        case Node.ELEMENT_NODE:
            String name = node.getNodeName();
            sw.append(indent + "<" + name);
            NamedNodeMap attributes = node.getAttributes();

            for (int i = 0; i < attributes.getLength(); i++) {
                Node current = attributes.item(i);
                sw.append(" " + current.getNodeName() + "=\"" + current.getNodeValue() + "\"");
            }
            sw.append(">");

            // recurse on each child
            NodeList children = node.getChildNodes();
            if (recurseChildren && children != null) {
                for (int i = 0; i < children.getLength(); i++) {
                    printNodeToString(children.item(i), indent + "  ", sw,
                                      recurseChildren);
                }
            }

            sw.append("</" + name + ">");
            break;

        case Node.TEXT_NODE:
            sw.append(node.getNodeValue());
            break;
        }

        return sw;

    }

    protected String getSummaryReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n============================================================\n");
        Iterator<String> jAuditDescriptions = violationsCount.keySet()
            .iterator();
        if (!jAuditDescriptions.hasNext()) {
            sb.append("No accessibility violations found!");
        }
        while (jAuditDescriptions.hasNext()) {
            String description = jAuditDescriptions.next();
            sb.append("\n" +
                    description + ": " + violationsCount.get(description) + " violations\n");
        }
        sb.append("\n============================================================\n");

        return sb.toString();
    }

    protected String getSummaryReportSubject() {
        return "SCRIPT: Accessibility Violations Search";
    }

    @Override
    protected void logUsageReport(String sCrawlDir) {
        String s = "";

        s += "user.dir set to  --->  " + System.getProperty("user.dir") + "\n";
        s += "Crawl Dir set to --->  " + sCrawlDir + "\n";

        s += getSummaryReport();

        System.out.println(s);

        s += "\n\n**EXCEPTION REPORT ************************************************\n";
        if (m_numExceptions == 0) {
            s += "No Exceptions to report.\n";
        }
        else {
            s += "A total of " + m_numExceptions + " exception(s) were collected.\n\n";
            s += sExceptions;
        }
        //Mail.sendMail("angel.irizarry@oracle.com", s, getSummaryReportSubject());
    }

    public static void main(String[] args) {
        if (args.length != 2 || args[0] == null || args[1] == null || args[0]
            .equals("") || args[1].equals("")) {
            System.out.println("findTableAndTreeSummaries.sh does not accept any arguments.");
            System.exit(1);
        }

        String crawlDirectory = args[0];
        String outputFilename = args[1];


        List<JAuditSearch> jAuditSearches = new ArrayList<JAuditSearch>(5);
        List<JAuditSearch.Component> components;

        /* *************************** VIOLATIONS *************************** */

        /* ************* Verify that tables/trees has summaries. ************ */
        components = new ArrayList<JAuditSearch.Component>(3);

        components.add(JAuditSearch.Component.table);
        components.add(JAuditSearch.Component.treeTable);
        components.add(JAuditSearch.Component.tree);
        jAuditSearches.add(new JAuditSearch(JAuditSearch.JAudit.TABLE_SUMMARIES,
                                            components, "Header"));


        /* ************** Verify that the component is labeled. ************* */
        components = new ArrayList<JAuditSearch.Component>(21);
        components.add(JAuditSearch.Component.inputColor);
        components.add(JAuditSearch.Component.inputComboboxListOfValues);
        components.add(JAuditSearch.Component.inputDate); // VERIFIED!
        components.add(JAuditSearch.Component.inputFile);
        components.add(JAuditSearch.Component.inputListOfValues);
        components.add(JAuditSearch.Component.inputNumberSlider);
        components.add(JAuditSearch.Component.inputNumberSpinbox);
        components.add(JAuditSearch.Component.inputRangeSlider);
        components.add(JAuditSearch.Component.inputText);
        components.add(JAuditSearch.Component.richTextEditor);
        components.add(JAuditSearch.Component.selectBooleanCheckbox);
        components.add(JAuditSearch.Component.selectBooleanRadio);
        components.add(JAuditSearch.Component.selectItem);
        components.add(JAuditSearch.Component.selectManyCheckbox);
        components.add(JAuditSearch.Component.selectManyChoice);
        components.add(JAuditSearch.Component.selectManyListbox);
        components.add(JAuditSearch.Component.selectManyShuttle);
        components.add(JAuditSearch.Component.selectOneChoice);
        components.add(JAuditSearch.Component.selectOneListbox);
        components.add(JAuditSearch.Component.selectOneRadio);
        components.add(JAuditSearch.Component.selectOrderShuttle);
        jAuditSearches.add(new JAuditSearch(JAuditSearch.JAudit.COMP_LABELED,
                                            components, "OLabel"));


        /* ************* Verify that the component has a title. ************* */
        components = new ArrayList<JAuditSearch.Component>(3);
        components.add(JAuditSearch.Component.dialog);
        components.add(JAuditSearch.Component.document);
        components.add(JAuditSearch.Component.panelWindow);
        jAuditSearches.add(new JAuditSearch(JAuditSearch.JAudit.COMP_TITLE,
                                            components, "Header"));


        /* ************* Verify that table columns have headers. ************ */
        components = new ArrayList<JAuditSearch.Component>(1);
        components.add(JAuditSearch.Component.column);
        jAuditSearches.add(new JAuditSearch(JAuditSearch.JAudit.COLUMN_HEADERTEXT,
                                            components, "Header"));


        /* ************* Verify that images have a description. ************* */
        components = new ArrayList<JAuditSearch.Component>(2);
        components.add(JAuditSearch.Component.image);
        jAuditSearches.add(new JAuditSearch(JAuditSearch.JAudit.IMAGE_SHORTDESC,
                                            components, "AltTxt"));


        /* *************** Verify that the component has text. ************** */
        components = new ArrayList<JAuditSearch.Component>(2);
        components.add(JAuditSearch.Component.panelBox);
        components.add(JAuditSearch.Component.panelHeader);
        components.add(JAuditSearch.Component.showDetailHeader);
        jAuditSearches.add(new JAuditSearch(JAuditSearch.JAudit.COMP_TEXT,
                                            components, "Header"));

        /* ****************************************************************** */

        AccessibilityViolationFinder x = new AccessibilityViolationFinder(jAuditSearches);
        x.start(crawlDirectory, outputFilename);

        x.logUsageReport(crawlDirectory);


    }

}
