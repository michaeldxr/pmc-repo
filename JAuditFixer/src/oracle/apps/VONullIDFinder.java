package oracle.apps;

import java.io.File;

import java.io.IOException;

import java.sql.Connection;

import java.sql.DriverManager;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.ArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.JoesBaseClass;


import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;

import oracle.xml.parser.v2.XMLParseException;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;


public class VONullIDFinder
    extends JoesBaseClass {

    private class ViolationDetails{
        private String series;
        private String fileName;
        private String LOVName;
        private String NUllValueID;
        private String label;
        private String family;
        private String module;
        private String product;
        private String issueType;
        private String subIssue = "Null Value ID Key should not have a local bundle reference";
        private String description;
        
        
        public ViolationDetails(String path,String LOVName,String NUllValueID){
            try {
                String[] info = FamilyModuleHelper.getFileNameInfo1(path).split(",");
                family = info[0];
                module = info[1];
                product = info[2];
                this.fileName = info[3];
                this.series = info[4];
                this.label = info[5];
            } catch (IOException e) {
                System.out.println("File Not Found in the label");
                return;
            }
            
            this.LOVName = LOVName;
            this.NUllValueID = NUllValueID;
            this.issueType = "i18n VONullValueID";
            this.description = LOVName + " has a bad string reference: " + NUllValueID;
            
        }

        public String getFileName() {
            return fileName;
        }

        public String getLabel() {
            return label;
        }

        public String getFamily() {
            return family;
        }

        public String getModule() {
            return module;
        }

        public String getProduct() {
            return product;
        }

        public String getIssueType() {
            return issueType;
        }

        public String getSubIssue() {
            return subIssue;
        }

        public String getDescription() {
            return description;
        }

        public void setSubIssue(String sub_issue) {
            this.subIssue = sub_issue;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getSeries() {
            return series;
        }
    }

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


    private static final String UI_HINTS_SUFFIX = "LOVUIHints_NullValueId";


    private static final String FIX_WILL_FIX =
        "...replace the key with the internal resource bundle.";

    private static final String FIX_WONT_FIX =
        "...leave this NullValueID alone. Looks good.";

    private static final String FIX_NO_NULL_VALUE =
        "...leave this NullValueID alone, but needs follow-up: Null value key was not found in the referenced XLF file!";

    private static final String FIX_NO_XLF_TAG =
        "...leave this NullValueID alone, but needs follow-up: No XliffBundle defined in VO.xml";

    private static final String FIX_NO_XLF =
        "...leave this NullValueID alone, but needs follow-up: XliffBundle defined in VO.xml, but not found.";

    private static final String FIX_HARDCODED =
        "...leave this NullValueID alone. It points to a hard-coded, non-empty value.";



    /* Statistics */
    int numVOFilesSeen = 0;
    int numListBindingSeen = 0;
    int numVOFilesSkipped = 0;
    int numNullValueIDToFix = 0;
    int numNullValueIDToIgnore = 0;
    int numNoNullValueID = 0;
    int numBadNullValueID = 0;
    int numNoXLFTag = 0;
    int numInvalidXLFReference = 0;
    int numMultipleXLFEntries = 0;

    DOMParser parser;
    ArrayList<ViolationDetails> list;

    public VONullIDFinder(JoesBaseClass.CRAWL_TYPE crawL_TYPE) {
        super(crawL_TYPE);
        list = new ArrayList<ViolationDetails>();
        parser = new DOMParser();
        parser.reset();
    }

    protected void processFile(File file, boolean bDoADE) {
        try {            
            numVOFilesSeen++;
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
                    Node lovNameNode = attrs.getNamedItem("Name");
                    String lovName = lovNameNode.getNodeValue();

                    // Extract NullValueID and resolve null value if needed. Fix issues.
                    String resolution = null;

                    Node nullValueIDNode = attrs.getNamedItem("NullValueId");
                    String nullValueID, nullValue = null;
                    if (nullValueIDNode == null) {
                        numNoNullValueID++;
                        resolution = FIX_WONT_FIX;
                        nullValueID = "";
                        nullValue = "";
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
                                    numNullValueIDToFix++;
                                    resolution = FIX_WILL_FIX;
                                    nullValue = "";
                                    
                                    // Add a row to the list
                                    list.add(new ViolationDetails(file.getCanonicalPath(),lovName,nullValueID));
                                }
                                else {
                                    numNullValueIDToIgnore++;
                                    resolution = FIX_HARDCODED;
                                    ViolationDetails detail = new ViolationDetails(file.getCanonicalPath(),lovName,nullValueID);
                                    detail.setSubIssue("Null Value ID should not be hardcoded");
                                    detail.setDescription(lovName + ": "+ nullValue + " must be added to SR");
                                    list.add(detail);
                                    
                                }
                            }
                            catch (InvalidNullValueIDException e) {
                                numBadNullValueID++;
                                resolution = FIX_NO_NULL_VALUE;
                                nullValue = "";
                                ViolationDetails detail = new ViolationDetails(file.getCanonicalPath(),lovName,nullValueID);
                                detail.setDescription(lovName + " has a null value id  "+ nullValue + "  that was missing in xlf");
                                list.add(detail);
                            }
                            catch (InvalidXLFReferenceException e) {
                                numInvalidXLFReference++;
                                resolution = FIX_NO_XLF;
                                nullValue = "";
                                ViolationDetails detail = new ViolationDetails(file.getCanonicalPath(),lovName,nullValueID);
                                detail.setDescription(lovName + " had an XLF reference that couldn't be found");
                                list.add(detail);
                            }
                            catch (NoXLFReferenceException e) {
                                numNoXLFTag++;
                                resolution = FIX_NO_XLF_TAG;
                                nullValue = "";
                                ViolationDetails detail = new ViolationDetails(file.getCanonicalPath(),lovName,nullValueID);
                                detail.setDescription("VO should've had an XLF reference but didn't");
                                list.add(detail);
                            }
                        }
                        else {
                            numNullValueIDToIgnore++;
                            resolution = FIX_WONT_FIX;
                            nullValue = "";
                        }
                    }
                }
            }
        }
        catch (IOException e) {
            numVOFilesSkipped++;
            System.out.println("ERROR: Couldn't parse VO.xml. Skipping...");
        }
        catch (XMLParseException e) {
            numVOFilesSkipped++;
            System.out.println("ERROR: Couldn't parse VO.xml. Skipping...");
        }
        catch (SAXException e) {
            numVOFilesSkipped++;
            System.out.println("ERROR: Couldn't parse VO.xml. Skipping...");
        }
        catch (Exception e) {
            numVOFilesSkipped++;
            System.out.println("ERROR: Couldn't parse VO.xml. Skipping...");
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

    protected String getSummaryReport() {
        return "Success!";
    }

    protected String getSummaryReportSubject() {
        return "Find Null ID Values in VO XMLs";
    }
    
    public void pushToAB() throws SQLException{
        Connection con = null;
        String sqlDelete = "";
        con = DriverManager.getConnection("jdbc:oracle:thin:codescan/codescan@angrybirds.oracle.com:1521:codescan");
        PreparedStatement delete = con.prepareStatement("DELETE FROM codescan_results where issuetype = 'i18n VONullValueID' and series = ? ");
        delete.setString(1, series);
        delete.execute();
        PreparedStatement insert = con.prepareStatement("INSERT INTO codescan_results (LABEL,FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION,SERIES) VALUES (?,?,?,?,?,?,?,?,?)");
        for(int i=0; i< list.size(); i++){
            if(isQOC(list.get(i).getFileName()))
                continue;
            
            insert.setString(1, list.get(i).getLabel());
            insert.setString(2, list.get(i).getFamily());
            insert.setString(3, list.get(i).getModule());
            insert.setString(4, list.get(i).getProduct());
            insert.setString(5, list.get(i).getFileName());
            insert.setString(6, list.get(i).getIssueType());
            insert.setString(7, list.get(i).getSubIssue());
            insert.setString(8, list.get(i).getDescription());
            insert.setString(9, list.get(i).getSeries());
            insert.execute();
        }
        PreparedStatement deleteRedsidue = con.prepareStatement("DELETE FROM codescan_results where issuetype = 'i18n VONullValueID' and series = ? and family is null");
        deleteRedsidue.setString(1, series);
        deleteRedsidue.execute();
        con.commit();
        con.close();
    }

    static String series;
    public static void main(String[] args) {
        if (args.length != 1 || args[0] == null || args[0].equals("")) {
            System.out.println("Usage:  fixVONullIDs.sh <series>");
            System.exit(1);
        }

        series = args[0];
        Boolean doADE = false;
            
        try {
            String label = LRGUtil.getLatestLabel(series);
            String crawlDirectory = LRGUtil.getCrawlDir(series, "" , label, "i18n VONullValueID");
            
            VONullIDFinder x = new VONullIDFinder(CRAWL_TYPE.VO_XML);

            if (doADE.booleanValue())
                x.startDiffLog();

            x.crawlDirectory(crawlDirectory, doADE);

           if (doADE.booleanValue())
                x.closeDiffLog();

            System.out.println("------------ Summary Report ------------");
            System.out.println("Number of VO.xml files seen (including those skipped):                           " +
                               x.numVOFilesSeen);
            System.out.println("Number of VO.xml files skipped (No <ViewObject> tag found):                      " +
                               x.numVOFilesSkipped);

            System.out.println();
            System.out.println("Number of ListBindings with NullValueIDs that this script will fix:              " +
                               x.numNullValueIDToFix);
            System.out.println("Number of ListBindings with valid NullValueIDs that will be left alone:          " +
                               x.numNullValueIDToIgnore);
            System.out.println("Number of ListBindings with no NullValueId attr. set:                            " +
                               x.numNoNullValueID);
            System.out.println("Number of ListBindings with NullValueId that was missing in XLF:                 " +
                               x.numBadNullValueID);
            System.out.println("Number of ListBindings whose VO.xml should've had an XLF reference but didn't:   " +
                               x.numNoXLFTag);
            System.out.println("Number of ListBindings whose VO.xml had an XLF reference that couldn't be found: " +
                               x.numInvalidXLFReference);
            System.out.println("--------------------------------------------------------------------------------------");
            System.out.println("Total number of <ListBinding> tags seen:                                         " +
                               x.numListBindingSeen);
            
            x.pushToAB();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    private boolean isQOC(String filename) {
        return filename.contains("fusionapps/crm/components/orderCapture/qoc/") || filename.contains("fusionapps/crm/qoc/components/");
    }
}
