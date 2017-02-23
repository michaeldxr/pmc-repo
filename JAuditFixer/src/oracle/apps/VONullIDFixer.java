package oracle.apps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import oracle.apps.helpers.Mail;
import oracle.apps.utility.JoesBaseClass;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Identifies and fixes issues with the NullValueID field of the LOVs on all
 * VOs. When searching for NullValueIDs, it identifies and classifies them into
 * six possible cases, listed below.
 *
 * 1. NullValueID is set to a string that ends in LOVUIHints_NullValueId, and
 * when the script resolves that ID in the corresponding XLF, the value of
 * that ID is an empty string or a positive number of spaces (' ').
 *
 * ACTION TAKEN: The script replaces the value of NullValueID in the VO.xml with
 * the global internal resource bundle's null value:
 * ${adfBundle['oracle.javatools.resourcebundle.SystemBundle']['NULL_VALUE_RESID']}
 *
 * -----------------------------------------------------------------------------
 *
 * 2. NullValueID is set to a string that ends in LOVUIHints_NullValueId, and
 * when the script resolves that ID in the corresponding XLF, it finds that
 * the XLF does not contain that ID.
 *
 * ACTION TAKEN: Since the default UI behavior for an error of this kind results
 * in the ID being resolved to an empty string, the script will replace the
 * value of the NullValueID in the VO.xml with the global internal resource
 * bundle's null value.
 *
 * -----------------------------------------------------------------------------
 *
 * 3. NullValueID is set to a string that ends in LOVUIHints_NullValueId, and
 * when the script resolves that ID in the corresponding XLF, it cannot find
 * the XLF file in the file system.
 *
 * ACTION TAKEN: Since the default UI behavior for an error of this kind results
 * in the ID being resolved to an empty string, the script will replace the
 * value of the NullValueID in the VO.xml with the global internal resource
 * bundle's null value.
 *
 * -----------------------------------------------------------------------------
 *
 * 4. NullValueID is set to a string that ends in LOVUIHints_NullValueId, and
 * when the script attempts to resolve the ID with the corresponding XLF, it
 * finds that no corresponding XLF is defined in the VO.xml.
 *
 * ACTION TAKEN: Since the default UI behavior for an error of this kind results
 * in the ID being resolved to an empty string, the script will replace the
 * value of the NullValueID in the VO.xml with the global internal resource
 * bundle's null value.
 *
 * -----------------------------------------------------------------------------
 *
 * 5. NullValueID is set to a string that ends in LOVUIHints_NullValueId, and
 * when the script resolves that ID in the corresponding XLF, the value of
 * that ID is a string that includes non-space characters.
 *
 * ACTION TAKEN: The script considers the NullValueID valid and does not perform
 * any actions on it.
 *
 * -----------------------------------------------------------------------------
 *
 * 6. NullValueID is set and does NOT end in LOVUIHints_NullValueId or it is not
 * set.
 *
 * ACTION TAKEN: The script considers this valid and does not perform
 * any actions.
 *
 */
public class VONullIDFixer
    extends JoesBaseClass {

    public class InvalidNullValueIDException
        extends Exception {
        public InvalidNullValueIDException(String message) {
            super(message);
        }
    }

    public class NoXLFReferenceException
        extends Exception {
        public NoXLFReferenceException(String message) {
            super(message);
        }
    }

    public class InvalidXLFReferenceException
        extends Exception {
        public InvalidXLFReferenceException(String message) {
            super(message);
        }
    }

    private static final String GLOBAL_NULL_VALUE_ID =
        "${adfBundle['oracle.javatools.resourcebundle.SystemBundle']['NULL_VALUE_RESID']}";

    private static final String UI_HINTS_SUFFIX = "LOVUIHints_NullValueId";
    private static final String UI_HINTS_PREFIX = "NullValueId=\"";

    private static final int UI_HINTS_SUFFIX_LENGTH = 22;
    private static final int UI_HINTS_PREFIX_LENGTH = 13;

    /* Statistics */
    int numVOFilesSeen = 0;
    int numListBindingSeen = 0;
    int numVOFilesSkipped = 0;
    int numVOFilesErrored = 0;
    int numCase1 = 0;
    int numCase2 = 0;
    int numCase3 = 0;
    int numCase4 = 0;
    int numCase5 = 0;
    int numCase6 = 0;

    int numMultipleXLFEntries = 0;

    DOMParser parser;

    BufferedWriter csv;
    StringBuilder csvSB;

    public VONullIDFixer(JoesBaseClass.CRAWL_TYPE crawL_TYPE) {
        super(crawL_TYPE);

        parser = new DOMParser();
        parser.reset();
    }

    public void startCSV() {
        try {
            Process p =
                Runtime.getRuntime().exec("rm -f out.csv");
            p.waitFor();

            csv = new BufferedWriter(new FileWriter("out.csv"));
            csvSB = new StringBuilder();
            csvSB.append("FILE PATH, NULL VALUE ID, NULL VALUE (IF AVAILABLE), CASE NUMBER, ACTION TAKEN\n");
        }
        catch (Exception e) {
            this.logException("out.csv exception", e);
        }
    }

    public void closeCSV() {
        try {
            csv.write(csvSB.toString());
            csv.close();
        }
        catch (IOException e) {
            this.logException("out.csv exception", e);
        }
    }

    protected void processFile(File file, boolean doADE) {
        List<String> nullValueIDsToReplace = new ArrayList<String>();
        boolean needsFix = false;
        try {
            numVOFilesSeen++;
            System.out.println("INFO: Processing file: " +
                               file.getAbsolutePath());
            parser.reset();
            parser.setDebugMode(true);
            parser.setErrorStream(System.out);
            parser.setEntityResolver(new NullEntityResolver());
            parser.showWarnings(true);
            parser.setValidationMode(DOMParser.NONVALIDATING);
            parser.parse("file:" + file.getAbsolutePath());

            XMLDocument doc = parser.getDocument();
            NodeList voTag = doc.getElementsByTagName("ViewObject");

            if (voTag == null || voTag.getLength() == 0) {
                numVOFilesSkipped++;
                System.out.println("INFO: File contained no <ViewObject> tags. Skipping...");
            }
            else if (voTag.getLength() > 1) {
                throw new RuntimeException("Corrupt VO.xml. Multiple ViewObject tags.");
            }
            else {
                // Extract the <ListBinding> tags
                NodeList listBindings =
                    doc.getElementsByTagName("ListBinding");
                for (int i = 0; i < listBindings.getLength(); i++) {
                    Node listBinding = listBindings.item(i);
                    numListBindingSeen++;

                    // Extract name
                    NamedNodeMap attrs = listBinding.getAttributes();

                    Node nullValueIDNode = attrs.getNamedItem("NullValueId");

                    String nullValueID, nullValue = null;
                    if (nullValueIDNode == null) {
                        numCase6++;
                    }
                    else {
                        nullValueID = nullValueIDNode.getNodeValue();
                        // Open XLF file and resolve NullValueId
                        if (nullValueID.endsWith(UI_HINTS_SUFFIX)) {
                            try {
                                nullValue =
                                        getNullValueFromIDInDoc(nullValueID,
                                                                doc,
                                                                file.getAbsolutePath());
                                if (nullValue.isEmpty()) {
                                    numCase1++;
                                    csvSB.append(file.getAbsolutePath() +
                                                 ", " + nullValueID +
                                                 ", , CASE 1, NullValueID replaced with global internal resource bundle null value." +
                                                 "\n");
                                    nullValueIDsToReplace.add(nullValueID);
                                    needsFix = true;
                                }
                                else {
                                    numCase5++;
                                    csvSB.append(file.getAbsolutePath() +
                                                 ", " + nullValueID + ", \"" +
                                                 nullValue +
                                                 "\", CASE 5, No change made." +
                                                 "\n");
                                }
                            }
                            catch (InvalidNullValueIDException e) {
                                numCase2++;
                                csvSB.append(file.getAbsolutePath() + ", " +
                                             nullValueID +
                                             ", , CASE 2, NullValueID replaced with global internal resource bundle null value." +
                                             "\n");
                                nullValueIDsToReplace.add(nullValueID);
                                needsFix = true;
                            }
                            catch (InvalidXLFReferenceException e) {
                                numCase3++;
                                csvSB.append(file.getAbsolutePath() + ", " +
                                             nullValueID +
                                             ", , CASE 3, NullValueID replaced with global internal resource bundle null value." +
                                             "\n");
                                nullValueIDsToReplace.add(nullValueID);
                                needsFix = true;
                            }
                            catch (NoXLFReferenceException e) {
                                numCase4++;
                                csvSB.append(file.getAbsolutePath() + ", " +
                                             nullValueID +
                                             ", , CASE 4, NullValueID replaced with global internal resource bundle null value." +
                                             "\n");
                                nullValueIDsToReplace.add(nullValueID);
                                needsFix = true;
                            }
                        }
                        else {
                            numCase6++;
                            csvSB.append(file.getAbsolutePath() + ", " +
                                         nullValueID +
                                         ", , CASE 6, No change made." + "\n");
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            numVOFilesErrored++;
            System.out.println("ERROR: Couldn't parse VO.xml. Possible stale link or corrupt file. Skipping...");
            this.logException("Exception on VO file", e);
            return;
        }

        if (needsFix) {
            try {
                applyFix(file, nullValueIDsToReplace, doADE);
            }
            catch (Exception e) {
                numVOFilesErrored++;
                System.out.println("ERROR: Couldn't modify VO.xml: " + file.getAbsolutePath());
                this.logException("Error fixing file: " + file.getAbsolutePath(), e);
                return;
            }
        }
    }

    private String getNullValueFromIDInDoc(String nullValueID,
                                           XMLDocument voDoc,
                                           String voFilePath)
        throws InvalidNullValueIDException, NoXLFReferenceException,
               InvalidXLFReferenceException {
        NodeList xliffBundleTags = voDoc.getElementsByTagName("XliffBundle");

        if (xliffBundleTags == null || xliffBundleTags.getLength() == 0) {
            throw new NoXLFReferenceException("No XLF Reference found in VO file: " +
                                              voFilePath);
        }
        else if (xliffBundleTags.getLength() > 1) {
            numMultipleXLFEntries++;
            System.out.println("WARNING: More than one <XliffBundle> tags found in " +
                               voFilePath);
            System.out.println("Ignoring all but first entry...");
        }

        String xlfBundleFQN =
            xliffBundleTags.item(0).getAttributes().getNamedItem("id").getNodeValue();
        String xlfBundlePath = getXLFPathFromFQN(xlfBundleFQN, voFilePath);
        File xlf = new File(xlfBundlePath);
        String nullValue = null;


        parser.reset();
        parser.setDebugMode(true);
        try {
            parser.setErrorStream(System.out);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        parser.setEntityResolver(new NullEntityResolver());
        parser.showWarnings(true);
        parser.setValidationMode(DOMParser.NONVALIDATING);

        try {
            parser.parse("file:" + xlf.getAbsolutePath());
        }
        catch (Exception e) {
            throw new InvalidXLFReferenceException("The reference: " +
                                                   xlf.getAbsolutePath() +
                                                   " in file: " + voFilePath +
                                                   " does not point to a valid XLF.");
        }

        XMLDocument xlfDoc = parser.getDocument();
        NodeList transUnits = xlfDoc.getElementsByTagName("trans-unit");
        Node transUnit = null;
        for (int i = 0; i < transUnits.getLength(); i++) {
            if (transUnits.item(i).getAttributes().getNamedItem("id").getNodeValue().equals(nullValueID)) {
                transUnit = transUnits.item(i);
                break;
            }
        }
        if (transUnit == null) {
            throw new InvalidNullValueIDException("No trans-unit with ID: " +
                                                  nullValueID +
                                                  " found in XLF: " +
                                                  xlf.getAbsolutePath());
        }

        NodeList childNodes = transUnit.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeName().equals("source")) {
                nullValue = childNode.getTextContent().trim();
                break;
            }
        }
        if (nullValue == null) {
            throw new RuntimeException("ERROR: Corrupt XLF! No 'source' found for trans-unit " +
                                       nullValueID + " in " +
                                       xlf.getAbsolutePath());
        }
        else {
            return nullValue;
        }
    }

    private String getXLFPathFromFQN(String xlfBundleFQN, String voFilePath) {
        String relativePath = xlfBundleFQN.replace('.', '/');
        relativePath += ".xlf";

        int i = voFilePath.indexOf("/src/");
        // chop everything off after "/src/" from voFilePath
        return voFilePath.substring(0, i + 5) + relativePath;
    }

    private void applyFix(File file, List<String> nullValueIDsToReplace,
                          Boolean doADE)
        throws Exception {
        File file_copy = new File(file.getAbsolutePath());
        FileReader reader = new FileReader(file_copy);
        BufferedReader input = new BufferedReader(reader);

        FileReader newline_reader = new FileReader(file_copy);
        BufferedReader newline_input = new BufferedReader(newline_reader);

        String line;

        StringBuffer sb = new StringBuffer();

        input.mark(1024);

        boolean insideOfListBinding = false;

        while ((line = input.readLine()) != null) {
            newline_input.skip(line.length());
            newline_input.mark(1024);
            int c1 = newline_input.read();
            int c2 = newline_input.read();
            newline_input.reset();

            String newline = "\n";
            if (c1 == -1) {
                newline = "";
            }
            else if (c1 == '\n') {
                newline_input.read();
            }
            else if (c1 == '\r' && c2 != '\n') {
                newline = "\r";
                newline_input.read();
            }
            else if (c1 == '\r' && c2 == '\n') {
                newline = "\r\n";
                newline_input.read();
                newline_input.read();
            }

            if (line.indexOf("<ListBinding") > -1)
                insideOfListBinding = true;
            else if (line.indexOf("</ListBinding>") > -1) {
                insideOfListBinding = false;
            }

            int i = line.indexOf(UI_HINTS_PREFIX);
            int j = line.indexOf(UI_HINTS_SUFFIX);
            if (i > -1 && j > -1 && insideOfListBinding) {
                String nullValueID =
                    line.substring(i + UI_HINTS_PREFIX_LENGTH, j +
                                   UI_HINTS_SUFFIX_LENGTH);
                if (nullValueIDsToReplace.contains(nullValueID)) {
                    sb.append(line.substring(0, i + UI_HINTS_PREFIX_LENGTH));
                    sb.append(GLOBAL_NULL_VALUE_ID);
                    sb.append(line.substring(j + UI_HINTS_SUFFIX_LENGTH));
                    sb.append(newline);
                }
                else {
                    sb.append(line);
                    sb.append(newline);
                }
            }
            else {
                sb.append(line);
                sb.append(newline);
            }
        }

        reader.close();

        if (doADE) {
            checkoutAndDelete(file);

            FileWriter fw = new FileWriter(file.getAbsolutePath());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(sb.toString());
            bw.close();

            checkinAndDiff(file);
        }
        else {
            System.out.println(sb.toString());
        }
    }

    protected String getSummaryReport() {
        int numTotalFixed = numCase1 + numCase2 + numCase3 + numCase4;
        int numTotalSkipped = numCase5 + numCase6;

        StringBuilder sb = new StringBuilder();

        sb.append("------------ Summary Report ------------" + "\n");
        sb.append("Number of VO.xml files seen (including those skipped):                     " +
                  numVOFilesSeen + "\n");
        sb.append("Number of VO.xml files skipped (No <ViewObject> tag found):                " +
                  numVOFilesSkipped + "\n" +
                "\n");
        if (numVOFilesErrored > 0)
            sb.append("Number of VO.xml files that caused errors and were not processed (PLEASE REVIEW out.txt): " +
                      numVOFilesErrored + "\n\n");

        sb.append("Number of ListBindings with NullValueIDs that this script fixed:           " +
                  numTotalFixed + "\n");
        sb.append("Number of ListBindings that had no NullValueID issues and were left alone: " +
                  numTotalSkipped + "\n");
        sb.append("---------------------------------------------------------------------------------" +
                  "\n");
        sb.append("Total number of <ListBinding> tags seen:                                   " +
                  numListBindingSeen + "\n" +
                "\n");

        sb.append("  FIXED: Case 1: NullValueID resolves to string containing only spaces:               " +
                  numCase1 + "\n");
        sb.append("  FIXED: Case 2: NullValueID not found in CASE 2:                                     " +
                  numCase2 + "\n");
        sb.append("  FIXED: Case 3: XLF file not found in file system:                                   " +
                  numCase3 + "\n");
        sb.append("  FIXED: Case 4: No XLF defined in VO.xml:                                            " +
                  numCase4 + "\n");
        sb.append("SKIPPED: Case 5: Non-empty null value:                                                " +
                  numCase5 + "\n");
        sb.append("SKIPPED: Case 6: NullValueID either not set or doesn't end in LOVUIHints_NullValueId: " +
                  numCase6 + "\n");

        return sb.toString();
    }

    protected String getSummaryReportSubject() {
        return "SCRIPT RUN: Fix Null ID Values in VO XMLs";
    }

    @Override
    protected void logUsageReport(String sCrawlDir) {
        String s = "";

        s += "user.dir set to  --->  " + System.getProperty("user.dir") + "\n";
        s += "Crawl Dir set to --->  " + sCrawlDir + "\n";

        s += getSummaryReport();

        System.out.println(s);

        s +=
  "\n\n**EXCEPTION REPORT ************************************************\n";
        if (m_numExceptions == 0) {
            s += "No Exceptions to report.\n";
        }
        else {
            s +=
  "A total of " + m_numExceptions + " exception(s) were collected.\n\n";
            s += sExceptions;
        }
        //Mail.sendMail("angel.irizarry@oracle.com", s, getSummaryReportSubject());
    }

    public static void main(String[] args) {
        if (args.length != 2 || args[0] == null || args[1] == null ||
            args[0].equals("") || args[1].equals("")) {
            System.out.println("Usage:  fixVONullIDs.sh <path to your code in /ade_autofs> <doADE>");
            System.exit(1);
        }

        String crawlDirectory = args[0];
        Boolean doADE = new Boolean(args[1]);

        VONullIDFixer x = new VONullIDFixer(CRAWL_TYPE.VO_XML);

        x.startCSV();
        if (doADE.booleanValue()) {
            try {
                x.startDiffLog();
            }
            catch (Exception e) {
                x.logException("diff log exception", e);
            }
        }

        x.crawlDirectory(crawlDirectory, doADE);
        x.closeCSV();
        if (doADE.booleanValue()) {
            try {
                x.closeDiffLog();
            }
            catch (Exception e) {
                x.logException("diff log exception", e);
            }
        }

        System.out.println(x.getSummaryReport());
        x.logUsageReport(crawlDirectory);
    }
}
