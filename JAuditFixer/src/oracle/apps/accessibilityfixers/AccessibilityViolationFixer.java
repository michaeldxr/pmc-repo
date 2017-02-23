package oracle.apps.accessibilityfixers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import oracle.apps.helpers.Mail;
import oracle.apps.utility.JoesBaseClass;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLParseException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;


public class AccessibilityViolationFixer extends JoesBaseClass {


    public enum Key {
        FILENAME,
        PATH,
        COMPONENT_TYPE,
        COMPONENT_ID,
        PROPERTY_NAME,
        PROPERTY_VALUE,
        PROPERTY_VALUE_B,
        JAUDIT_DESCRIPTION;
    }

    private static final Pattern DROPDOWN_REGEX = Pattern.compile("^(.*((Gen)|(Attr))Bundle): \\['(.+)'\\] \\- (.+)$");
    private static final Pattern BUNDLE_DEF_REGEX = Pattern.compile("^#\\{adfBundle\\['([\\w\\.]+)'\\]\\}$");
    private static final String C_NAMESPACE = "xmlns:c=\"http://java.sun.com/jsp/jstl/core\"";

    private UIBundleFinder uiBundleFinder;
    private HashMap<String, HashMap<String, String>> jprMappings;

    private static final int ROW_SIZE = 8;
    private boolean TWO_PROPERTY_VALUES = false;

    private int numRowsUnresolved = 0;
    private int numRowsProcessed = 0;
    private int numRowsMalformed = 0;
    private int numRowsIgnoredBlank = 0;
    private int numAlreadyChanged = 0;

    private boolean MODIFIED = false;


    private DOMParser parser = new DOMParser();

    private class InvalidPathException extends Exception {
    }

    public AccessibilityViolationFixer() {
        // Crawl Type not used in this class.
        super(CRAWL_TYPE.JSPX_AND_JSFF);
    }

    protected void processFile(File fName, boolean bDoADE) {
        // Not used in this class.
    }

    protected String getSummaryReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("===========================================================================\n");
        sb.append("Number of CSV rows processed successfully:                           " + numRowsProcessed + "\n");
        if (numAlreadyChanged > 0)
            sb.append("Number of CSV rows skipped because the change has already been made: " + numAlreadyChanged + "\n");
        if (numRowsUnresolved > 0)
            sb.append("Number of CSV rows skipped because the string was unresolvable:      " + numRowsUnresolved + "\n");
        if (numRowsMalformed > 0)
            sb.append("Number of CSV rows skipped because they were malformed:              " + numRowsMalformed + "\n");
        if (numRowsIgnoredBlank > 0)
            sb.append("Number of CSV rows ignored, as they have blank property values:      " + numRowsIgnoredBlank + "\n");
        sb.append("===========================================================================\n");

        return sb.toString();
    }

    protected String getSummaryReportSubject() {
        return "SCRIPT: Accessibility Violation Fixer";
    }

    public void start(String viewRoot, String csvPath, Boolean doADE) {
        // Build the xlf mappings first.
        uiBundleFinder = new UIBundleFinder();
        jprMappings = uiBundleFinder.getMappings();
        System.out.println();
        System.out.println("================================================");
        System.out.println("Processing CSV and making changes to UI files...");
        System.out.println("================================================");

        // Parse the csv and process each row.
        try {
            Collection<String> csvRows = parse(csvPath);
            Iterator<String> csvRowsIterator = csvRows.iterator();
            while (csvRowsIterator.hasNext()) {
                String csvRow = csvRowsIterator.next();
                try {
                    System.out.println("INFO: Processing CSV row: " + csvRow);
                    HashMap<Key, String> row = parseRow(csvRow);
                    if (row.get(Key.PROPERTY_VALUE).isEmpty()) {
                        numRowsIgnoredBlank++;
                        continue;
                    }
                    String path = replaceADERootWithViewRoot(row.get(Key.PATH),
                                                             viewRoot);
                    File file = new File(path);
                    String propertyValueA = row.get(Key.PROPERTY_VALUE);
                    String propertyValueB = row.get(Key.PROPERTY_VALUE_B);
                    String[] resolvedPropertySetA, resolvedPropertySetB;
                    if (isFromValidationList(propertyValueA)) {
                        resolvedPropertySetA = resolveAttrFromList(file,
                                                                   propertyValueA);
                    }
                    else {
                        resolvedPropertySetA = resolveStringManuallyTyped(file,
                                                                          propertyValueA,
                                                                          row.get(Key.JAUDIT_DESCRIPTION));
                    }

                    if (resolvedPropertySetA == null) {
                        System.err.println("ERROR: String value \"" + propertyValueA + "\" unresolvable. Either you are using an invalid string type for this violation or the corresponding JPR needs a new resource bundle alias: " + identifyJPR(file)
                                           .getName());
                        numRowsUnresolved++;
                        continue;
                    }

                    if (TWO_PROPERTY_VALUES) {
                        if (isFromValidationList(propertyValueB)) {
                            resolvedPropertySetB = resolveAttrFromList(file,
                                                                       propertyValueB);
                        }
                        else {
                            resolvedPropertySetB = resolveStringManuallyTyped(file,
                                                                              propertyValueB,
                                                                              row.get(Key.JAUDIT_DESCRIPTION));
                        }

                        if (resolvedPropertySetB == null) {
                            System.err.println("ERROR: String value \"" + propertyValueB + "\" unresolvable. Either you are using an invalid string type for this violation or the corresponding JPR needs a new resource bundle alias: " + identifyJPR(file)
                                               .getName());
                            numRowsUnresolved++;
                            continue;
                        }

                        addAttribute(file, row.get(Key.COMPONENT_ID),
                                     row.get(Key.PROPERTY_NAME),
                                     resolvedPropertySetA,
                                     resolvedPropertySetB, doADE);
                    }
                    else {
                        addAttribute(file, row.get(Key.COMPONENT_ID),
                                     row.get(Key.PROPERTY_NAME),
                                     resolvedPropertySetA, null, doADE);
                    }

                    numRowsProcessed++;
                }
                catch (Exception e) {
                    System.err.println("ERROR: Error processing CSV row: " + csvRow);
                    e.printStackTrace();
                    numRowsMalformed++;
                    this.logException("Error processing CSV row: " + csvRow,
                                      e);
                }
            }
        }
        catch (FileNotFoundException e) {
            System.err.println("FATAL ERROR: Could not find CSV file: " + csvPath);
            this.logException("ERROR: Count not find CSV file: ", e);
        }
        catch (IOException e) {
            System.err.println("FATAL ERROR: Could not read CSV file: " + csvPath);
            this.logException("ERROR: Could not read CSV file", e);
        }
    }

    private String pullFQNFromElExpression(String expression) {
        Matcher matcher = BUNDLE_DEF_REGEX.matcher(expression);
        if (!matcher.matches())
            throw new RuntimeException("EL Expression looks nothing like what I expected: " + expression);
        return matcher.group(1);
    }

    private String getNameFromFQN(String fqn) {
        Matcher m = Pattern.compile(".+\\.(.+)").matcher(fqn);
        if (m.matches()) {
            return m.group(1);
        }
        else
            throw new RuntimeException("getNameFromFQN was not fed an FQN");
    }

    private boolean isFromValidationList(String string) {
        return DROPDOWN_REGEX.matcher(string).matches();
    }

    private String replaceADERootWithViewRoot(String adePath, String viewRoot) throws InvalidPathException {
        if (isEmpty(adePath)) {
            throw new InvalidPathException();
        }
        if (adePath.contains("/fusionapps/")) {
            int i = adePath.indexOf("/fusionapps/");
            String relativePath = adePath.substring(i);
            return viewRoot + relativePath;
        }
        else {
            throw new InvalidPathException();
        }
    }

    private Collection<String> parse(String csvPath) throws FileNotFoundException,
                                                            IOException {
        File csvFile = new File(csvPath);
        FileReader reader = new FileReader(csvFile);
        BufferedReader br = new BufferedReader(reader);

        String line;
        Collection<String> csv = new ArrayList<String>();
        // Skip the first line, as it contains the useless headers.
        line = br.readLine();
        line = br.readLine();
        while (line != null) {
            csv.add(line);
            line = br.readLine();
        }

        return csv;
    }

    private HashMap<Key, String> parseRow(String csvRow) {
        String[] elements = csvRow.split(",");
        int length = elements.length;
        if (length != ROW_SIZE)
            throw new RuntimeException("This CSV does not contain the correct number of rows.");

        for (int i = 0; i < length; i++) {
            elements[i] = elements[i].trim();
        }

        if (elements[6].isEmpty())
            TWO_PROPERTY_VALUES = false;
        else
            TWO_PROPERTY_VALUES = true;

        HashMap<Key, String> row = new HashMap<Key, String>();
        row.put(Key.FILENAME, elements[0]);
        row.put(Key.PATH, elements[1]);
        row.put(Key.COMPONENT_TYPE, elements[2]);
        row.put(Key.COMPONENT_ID, elements[3]);
        row.put(Key.PROPERTY_NAME, elements[4]);
        row.put(Key.PROPERTY_VALUE, elements[5]);
        row.put(Key.PROPERTY_VALUE_B, elements[6]);
        row.put(Key.JAUDIT_DESCRIPTION, elements[7]);

        return row;
    }

    private String[] resolveAttrFromList(File uiFile, String compPropertyValue) throws IOException,
                                                                                       XMLParseException,
                                                                                       SAXException,
                                                                                       Exception {
        Matcher matcher = DROPDOWN_REGEX.matcher(compPropertyValue);
        if (matcher.matches()) {
            String bundle = matcher.group(1);
            String key = matcher.group(5);
            String value = matcher.group(6);

            HashMap<String, String> xlfMappings = jprMappings.get(identifyJPR(uiFile)
                                                                  .getName());
            if (xlfMappings == null) {
                uiBundleFinder.addJPR(identifyJPR(uiFile));
                xlfMappings = jprMappings.get(identifyJPR(uiFile).getName());
                if (xlfMappings == null) {
                    throw new RuntimeException("Something went wrong with finding the xlf mappings for the file: " + uiFile.getAbsolutePath());
                }
            }

            parser.reset();
            parser.setErrorStream(System.out);
            parser.setValidationMode(DOMParser.NONVALIDATING);
            parser.showWarnings(true);
            parser.parse("file:" + uiFile.getCanonicalPath());

            XMLDocument doc = parser.getDocument();

            String resolution;
            String[] match = matchingBundle(doc, bundle);
            if (match != null) {
                String path = xlfMappings.get(match[1]);
                if (path != null) {
                    JarFile jarFile = new JarFile(path);
                    ZipEntry xlfEntry = jarFile.getEntry(match[1]
                                                         .replace('.', '/') + ".xlf");
                    resolution = findKeyInXLF(jarFile.getInputStream(xlfEntry),
                                              value, key);
                    if (resolution != null) {
                        String[] returnSet = { "#{" + match[0] + "['" + resolution + "']}",
                                               null, null };
                        return returnSet;
                    }
                }
            }

            // If code reaches this point we need to continue searching for the
            // string in the project's referenced bundles and define the bundle in
            // the UI file.
            Set<String> xlfFQNs = xlfMappings.keySet();
            Iterator<String> xlfFQNiterator = xlfFQNs.iterator();
            while (xlfFQNiterator.hasNext()) {
                String xlfFQN = xlfFQNiterator.next();
                String bundleName = getNameFromFQN(xlfFQN);
                if (bundleName.equals(bundle)) {
                    String xlfPath = xlfMappings.get(xlfFQN);
                    JarFile jarFile = new JarFile(xlfPath);
                    ZipEntry xlfEntry = jarFile.getEntry(xlfFQN.replace('.',
                                                                        '/') + ".xlf");
                    resolution = findKeyInXLF(jarFile.getInputStream(xlfEntry),
                                              value, key);
                    if (resolution != null) {
                        String[] returnSet = { "#{" + bundleName + "['" + resolution + "']}",
                                               bundleName, xlfFQN };
                        return returnSet;
                    }
                }
            }
        }

        return null;
    }

    private String[] resolveStringManuallyTyped(File uiFile, String compPropertyValue, String violation) throws IOException,
                                                                                                                XMLParseException,
                                                                                                                SAXException {
        parser.reset();
        parser.setErrorStream(System.out);
        parser.setValidationMode(DOMParser.NONVALIDATING);
        parser.showWarnings(true);
        parser.parse("file:" + uiFile.getCanonicalPath());

        XMLDocument doc = parser.getDocument();

        if (compPropertyValue.equals(JAuditSearch.IMAGE_EXTRA_OPTION2)) {
            String[] returnSet = { "", null, null };
            return returnSet;
        }
        String resolution;

        String stringType;
        if (violation.equals(JAuditSearch.JAudit.COLUMN_HEADERTEXT.toString()))
            stringType = "Header";
        else if (violation.equals(JAuditSearch.JAudit.COMP_LABELED.toString()))
            stringType = "OLabel";
        else if (violation.equals(JAuditSearch.JAudit.COMP_TEXT.toString()))
            stringType = "Header";
        else if (violation.equals(JAuditSearch.JAudit.COMP_TITLE.toString()))
            stringType = "Header";
        else if (violation.equals(JAuditSearch.JAudit.IMAGE_SHORTDESC.toString()))
            stringType = "AltTxt";
        else if (violation.equals(JAuditSearch.JAudit.TABLE_SUMMARIES.toString()))
            stringType = "Header";
        else
            throw new RuntimeException("ERROR: Row with file doesn't specify an Accessibility Violation");

        HashMap<String, String> xlfMappings = jprMappings.get(identifyJPR(uiFile)
                                                              .getName());
        if (xlfMappings == null) {
            uiBundleFinder.addJPR(identifyJPR(uiFile));
            xlfMappings = jprMappings.get(identifyJPR(uiFile).getName());
            if (xlfMappings == null) {
                throw new RuntimeException("Something went wrong with finding the xlf mappings for the file: " + uiFile.getAbsolutePath());
            }
        }

        Iterator<String> xlfFQNs = xlfMappings.keySet().iterator();

        Pattern xlfKeyRegex = Pattern.compile(stringType + "\\..+");
        Matcher xlfKeyMatcher = xlfKeyRegex.matcher(compPropertyValue);
        if (xlfKeyMatcher.matches()) {
            while (xlfFQNs.hasNext()) {
                String xlfFQN = xlfFQNs.next();
                String bundleName = getNameFromFQN(xlfFQN);

                String xlfPath = xlfMappings.get(xlfFQN);
                JarFile jarFile = new JarFile(xlfPath);
                ZipEntry xlfEntry = jarFile.getEntry(xlfFQN.replace('.',
                                                                    '/') + ".xlf");
                resolution = findKeyInXLF(jarFile.getInputStream(xlfEntry),
                                          null, compPropertyValue);
                if (resolution != null) {
                    String[] match = matchingBundle(doc, bundleName);

                    if (match == null) {
                        String[] returnSet = { "#{" + bundleName + "['" + resolution + "']}",
                                               bundleName, xlfFQN };
                        return returnSet;
                    }
                    else {
                        String[] returnSet = { "#{" + bundleName + "['" + resolution + "']}",
                                               null, null };
                        return returnSet;
                    }
                }
            }
        }
        else {
            while (xlfFQNs.hasNext()) {
                String xlfFQN = xlfFQNs.next();
                String bundleName = getNameFromFQN(xlfFQN);

                String xlfPath = xlfMappings.get(xlfFQN);
                JarFile jarFile = new JarFile(xlfPath);
                ZipEntry xlfEntry = jarFile.getEntry(xlfFQN.replace('.',
                                                                    '/') + ".xlf");
                resolution = findKeyInXLF(jarFile.getInputStream(xlfEntry),
                                          compPropertyValue, null);
                if (resolution != null) {
                    String[] match = matchingBundle(doc, bundleName);

                    if (match == null) {
                        String[] returnSet = { "#{" + bundleName + "['" + resolution + "']}",
                                               bundleName, xlfFQN };
                        return returnSet;
                    }
                    else {
                        String[] returnSet = { "#{" + bundleName + "['" + resolution + "']}",
                                               null, null };
                        return returnSet;
                    }
                }
            }
        }

        return null;
    }

    /**
     *
     * @param doc
     * @param bundleName
     * @return If matched, returns { NAME, FQN }, else null
     */
    private String[] matchingBundle(XMLDocument doc, String bundleName) {
        NodeList setNodes = doc.getElementsByTagName("set");

        for (int i = 0; i < setNodes.getLength(); i++) {
            Node setNode = setNodes.item(i);
            String var = setNode.getAttributes().getNamedItem("var")
                .getNodeValue();
            String value = setNode.getAttributes().getNamedItem("value")
                .getNodeValue();
            String fqn = pullFQNFromElExpression(value);
            String myBundleName = getNameFromFQN(fqn);
            if (myBundleName.equals(bundleName)) {
                String[] returnSet = { var, fqn };
                return returnSet;
            }
        }

        return null;
    }


    private File identifyJPR(File path) {
        File parent = path.getParentFile();
        File[] listOfFiles = parent.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            String fileName = listOfFiles[i].getAbsolutePath();
            if (fileName.endsWith(".jpr"))
                return listOfFiles[i];
        }
        return identifyJPR(parent);
    }

    private String findKeyInXLF(InputStream xlf, String attribute, String key) throws IOException,
                                                                                      XMLParseException,
                                                                                      SAXException {
        parser.reset();
        parser.setErrorStream(System.out);
        parser.setValidationMode(DOMParser.NONVALIDATING);
        parser.showWarnings(true);
        parser.parse(xlf);

        XMLDocument doc = parser.getDocument();

        NodeList transUnits = doc.getElementsByTagName("trans-unit");

        // Search through all of the <trans-unit> tags
        for (int i = 0; i < transUnits.getLength(); i++) {
            Node transUnit = transUnits.item(i);
            String transUnitID = transUnit.getAttributes().getNamedItem("id")
                .getNodeValue();
            if (key != null && !transUnitID.equals(key))
                continue;
            NodeList children = transUnit.getChildNodes();
            // Find the <source> tag in each trans unit and try to find a match.
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (child.getNodeName().equals("source")) {
                    if (attribute == null || (child.getTextContent().trim()
                                              .equals(attribute.trim())))
                        return transUnitID;
                }
            }
        }

        return null;
    }

    private void addAttribute(File file, String componentID, String componentProperty, String[] setA, String[] setB, Boolean doADE) throws FileNotFoundException,
                                                                                                                                           IOException,
                                                                                                                                           Exception {
        MODIFIED = false;
        String newValue = (setB == null) ? setA[0] : setA[0] + ":" + setB[0];
        File file_copy = new File(file.getAbsolutePath());
        FileReader reader = new FileReader(file_copy);
        BufferedReader input = new BufferedReader(reader);

        FileReader reader2 = new FileReader(file_copy);
        BufferedReader input2 = new BufferedReader(reader2);

        String line;
        StringBuffer out = new StringBuffer();
        StringBuffer in = new StringBuffer();

        input.mark(1024);

        line = input.readLine();
        while (line != null) {
            input2.skip(line.length());
            input2.mark(1024);
            int c1 = input2.read();
            int c2 = input2.read();
            input2.reset();

            String newline = "\n";
            if (c1 == -1) {
                newline = "";
            }
            else if (c1 == '\n') {
                input2.read();
            }
            else if (c1 == '\r' && c2 != '\n') {
                newline = "\r";
                input2.read();
            }
            else if (c1 == '\r' && c2 == '\n') {
                newline = "\r\n";
                input2.read();
                input2.read();
            }
            in.append(line);
            in.append(newline);

            if (line.contains("<")) {
                line = consumeAllOfTag(input, line, newline);
                line = replaceOrAddAttributeInString(line, componentProperty,
                                                     newValue, componentID);
                if (line.contains("<jsp:root")) {
                    line = ensureXMLHasCNameSpace(line, newline);
                    out.append(line);
                    if (setA[1] != null) {
                        MODIFIED = true;
                        out.append("  <c:set var=\"" + setA[1] + "\"");
                        out.append(newline);
                        out.append("         value=\"#{adfBundle['" + setA[2] + "']}\"/>");
                        out.append(newline);
                    }
                    if (setB != null && setB[1] != null && !setB[1]
                        .equals(setA[1])) {
                        MODIFIED = true;
                        out.append("  <c:set var=\"" + setB[1] + "\"");
                        out.append(newline);
                        out.append("         value=\"#{adfBundle['" + setB[2] + "']}\"/>");
                        out.append(newline);
                    }
                }
                else {
                    out.append(line);
                }
            }
            else {
                out.append(line);
                out.append(newline);
            }

            line = input.readLine();
        }

        // Write out the file.
        input.close();
        input2.close();

        if (doADE) {
            if (MODIFIED) {
                checkoutAndDelete(file);
                FileWriter fw = new FileWriter(file.getAbsolutePath());
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(out.toString());
                bw.close();
                checkinAndDiff(file);
            }
            else {
                numRowsProcessed--;
                numAlreadyChanged++;
            }
        }
        else {
            if (MODIFIED) {
                System.out.println(out.toString());
            }
            else {
                numRowsProcessed--;
                numAlreadyChanged++;
            }
        }
    }

    /**
     * Replaces or adds the specified newAttrValue of type ATTR_TYPE if the tag
     * contains id="tagID"
     * @param tag A string containing the full start tag of an xml element
     * @param newAttrValue the value of the new attribute
     * @param tagID The tagID that this method tries to match. If this tag
     * doesn't contain tagID, then the tag is returned untouched.
     * @return the modified tag with the new or modified attribute
     */
    private String replaceOrAddAttributeInString(String tag, String attrType, String newAttrValue, String tagID) {
        String newString;
        if (!Pattern.compile("id=\"" + tagID + "\"").matcher(tag).find())
            return tag;
        Pattern attribute_regex = Pattern.compile("(" + attrType + "=\")(.*?)(\")");
        Matcher matcher = attribute_regex.matcher(tag);
        if (matcher.find()) {
            if (attrType.equals("label"))
                newString = replaceOrAddAttributeInString(matcher.replaceFirst("$1" + newAttrValue + "$3"),
                                                          "simple", "true",
                                                          tagID);
            else
                newString = matcher.replaceFirst("$1" + newAttrValue + "$3");
        }
        else {
            if (attrType.equals("label")) {
                newString = replaceOrAddAttributeInString(Pattern.compile("(<.*?id=\"" + tagID + "\".*?)(/?>)")
                                                          .matcher(tag)
                                                          .replaceFirst("$1 " + attrType + "=\"" + newAttrValue + "\"$2"),
                                                          "simple", "true",
                                                          tagID);
            }
            else {
                newString = Pattern.compile("(<.*?id=\"" + tagID + "\".*?)(/?>)").matcher(tag)
                        .replaceFirst("$1 " + attrType + "=\"" + newAttrValue + "\"$2");
            }
        }

        if (!newString.equals(tag))
            MODIFIED = true;

        return newString;
    }


    private String ensureXMLHasCNameSpace(String line, String newline) {
        String newString;
        if (line.contains(C_NAMESPACE))
            newString = line;
        else
            newString = Pattern.compile(".*xmlns.*").matcher(line)
                    .replaceFirst("$0" + newline + "          " + C_NAMESPACE);
        if (!newString.equals(line))
            MODIFIED = true;


        return newString;
    }

    private String consumeAllOfTag(BufferedReader input, String firstLine, String newline) throws IOException {
        StringBuilder fullTag = new StringBuilder();
        String currentLine = firstLine;
        while (currentLine != null) {
            fullTag.append(currentLine);
            fullTag.append(newline);
            if (currentLine.contains(">"))
                break;
            currentLine = input.readLine();
        }

        return fullTag.toString();
    }


    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }

    protected void logUsageReport(String csvPath, String viewRoot) {
        String s = "";

        s += "user.dir set to  --->  " + System.getProperty("user.dir") + "\n";
        s += "CSV path set to  --->  " + csvPath + "\n";
        s += "View root set to --->  " + viewRoot + "\n";

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
       // Mail.sendMail("angel.irizarry@oracle.com", s,  getSummaryReportSubject());
    }

    public static void main(String[] args) {
        if (args.length != 3 || args[0] == null || args[1] == null || args[2] == null || args[0]
            .equals("") || args[1].equals("") || args[2].equals("")) {
            System.out.println("Usage: findAccessibilityViolations.sh <path to your .csv file>");
            System.exit(1);
        }
        String viewRoot = args[0];
        Boolean doADE = new Boolean(args[1]);
        String csvPath = args[2];

        if (!csvPath.endsWith(".csv")) {
            System.err.println("Usage: findAccessibilityViolations.sh <path to your .csv file>");
            System.err.println(csvPath + " may not be a valid CSV file.");
            System.exit(1);
        }

        AccessibilityViolationFixer x = new AccessibilityViolationFixer();

        if (doADE.booleanValue()) {
            try {
                x.startDiffLog();
            }
            catch (Exception e) {
                x.logException("diff log exception", e);
            }
        }

        x.start(viewRoot, csvPath, doADE);

        x.logUsageReport(csvPath, viewRoot);
    }
}
