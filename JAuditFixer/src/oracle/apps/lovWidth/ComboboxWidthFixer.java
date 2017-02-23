package oracle.apps.lovWidth;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileOutputStream;
import java.io.FileReader;

import java.io.FileWriter;

import java.sql.Connection;

import java.sql.DriverManager;

import java.sql.PreparedStatement;

import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;
import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.XMLParserHelper;

import oracle.apps.helpers.XmlFixer;

import org.w3c.dom.Node;

public class ComboboxWidthFixer {
    static boolean m_debug = true;
    static boolean m_abortOnValidationError = true;

    // Later enhancements
    static String restrictFamily = "no";
    static String restrictProduct = "no";
    HashMap<String, VOFixDetails> voFixMap;
    static String currentDir = "";
    static AdeDiffHelper diffHelper = new AdeDiffHelper("diff.txt");
    static boolean stripeByRunDir = true;

    public ComboboxWidthFixer() {
        super();
    }

    public static void main(String[] args) {
        ComboboxWidthFixer comboboxWidthFixer = new ComboboxWidthFixer();

        Date startDate = new Date();

        if (args.length != 3 || isEmpty(args[0]) || isEmpty(args[0]) || isEmpty(args[1])) {
            System.out.println("Usage:  setVoTuning.sh <csv file name path> <doADE>");
            System.exit(1);
        }
        currentDir = args[0];
        String csvFilePath = args[1];
        Boolean bDoADE = new Boolean(args[2]);
        String runARgs = setSystemProperties();
        System.out.println("Running with System properties: " + runARgs);
        System.out.println(String.format("----------Starting Combobox Width Fixer at %s --------------", startDate));
        System.out.println("Will Parse Csv file location " + csvFilePath);
        System.out.println("Will perform ADE Operations = " + bDoADE);
        System.out.println("------------------------------------------");

        File csvFile = new File(csvFilePath);
        if (csvFile == null || !csvFile.exists()) {
            System.out.println(" [ERROR] Csv File does not exist: " + csvFilePath);
            System.exit(1);
        }

        try {
            if (bDoADE) {
                System.out.println("removing any existing diff.txt");
                diffHelper.startDiffLog();
            }
        } catch (Exception ex) {
            System.out.println("Error while starting ade dff");
            ex.printStackTrace();
            return;
        }

        boolean parsingStatus = false;
        ArrayList<String> validationErrors = null;
        try {
            System.out.println("====> Starting to Parse CSV File: " + csvFilePath);
            validationErrors = comboboxWidthFixer.parseCsvFile(csvFile);
            if (validationErrors != null && validationErrors.size() > 0) {
                parsingStatus = false;
            } else {
                parsingStatus = true;
            }
        } catch (Exception ex) {
            System.out.println("CSV File Parsing Failed (Could be due to validation errors)");
            System.out.println("Reason: " + ex.getMessage());
            parsingStatus = false;
        }

        if (parsingStatus == false) {

            comboboxWidthFixer.printValidationErrors(validationErrors);
            if (m_abortOnValidationError) {
                System.out.println("Aborting due to validation failures.. ");
                System.exit(1);
            }
        }

        System.out.println("Finished Parsing CSV File.. Number of VO Attributes to fix:" + comboboxWidthFixer.voFixMap.size());
        System.out.println("=====> Making Fixes");
        ArrayList<VOFixDetails> resultsAfterFix = null;
        try {
            resultsAfterFix = comboboxWidthFixer.makeFixes(bDoADE);
            if (bDoADE) {
                diffHelper.closeDiffLog();
            }
        } catch (Exception ex) {
            System.out.println("Exception occurred while making fix");
            ex.printStackTrace();

        }
        comboboxWidthFixer.writeFinalResults(resultsAfterFix);


    }

    public void writeFinalResults(ArrayList<VOFixDetails> fixResults) {

        Workbook myBook = new Workbook("output");
        Worksheet resultSheet = new Worksheet("VOFixes");
        resultSheet.setColumnWidth(1, 30);
        resultSheet.addCell(new Cell(new CellLocation("A1"), new CellValue("VO Package")));
        resultSheet.setColumnWidth(1, 30);
        resultSheet.addCell(new Cell(new CellLocation("B1"), new CellValue("VO Attribute")));
        resultSheet.setColumnWidth(1, 20);
        resultSheet.addCell(new Cell(new CellLocation("C1"), new CellValue("VO Attribute Original Width")));
        resultSheet.setColumnWidth(1, 20);
        resultSheet.addCell(new Cell(new CellLocation("D1"), new CellValue("VO Attribute Modified Width")));
        resultSheet.setColumnWidth(1, 20);
        resultSheet.addCell(new Cell(new CellLocation("E1"), new CellValue("Status")));
        myBook.addWorksheet(resultSheet);

        Iterator<VOFixDetails> resultIter = fixResults.iterator();
        int row = 2;
        while (resultIter.hasNext()) {
            VOFixDetails votoWrite = resultIter.next();
            resultSheet.addCell(new Cell(new CellLocation("A" + row), new CellValue(votoWrite.getVoPackage())));
            resultSheet.addCell(new Cell(new CellLocation("B" + row), new CellValue(votoWrite.getAttributeName())));
            resultSheet.addCell(new Cell(new CellLocation("C" + row), new CellValue(votoWrite.getOrigAttributeWidth())));
            resultSheet.addCell(new Cell(new CellLocation("D" + row), new CellValue(votoWrite.getNewAttributeWidth())));
            resultSheet.addCell(new Cell(new CellLocation("E" + row), new CellValue(votoWrite.getStatus())));
            row++;
        }
        try {
            writeSpreadsheet(myBook, "output.xlsx");
        } catch (Exception ex) {
            System.out.println("Exception while writing results to spreadsheet");

            ex.printStackTrace();
        }

    }

    /**
     *Write spreadsheet to disk
     * @param outputFileName
     * Output file name to write to
     * @throws Exception
     * Exception if operation is unsuccessful
     */
    public static void writeSpreadsheet(Workbook book, String outputFileName) throws Exception {

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outputFileName);
            XLSXCreator.create(book, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            out.close();
        }
    }

    public boolean isVOInRunPath(String voPath) {
        if (isEmpty(voPath))
            return false;
        if (voPath.contains(currentDir))
            return true;
        return false;
    }

    public ArrayList<VOFixDetails> makeFixes(boolean bDoADE) throws Exception {
        if (voFixMap == null || voFixMap.size() == 0) {
            System.out.println("Nothing to fix.. ");
            return null;
        }
        ArrayList<VOFixDetails> finalResults = new ArrayList<VOFixDetails>();
        Connection connection = null;
        PreparedStatement stmt = null;

        connection = getConnection();
        stmt = getStmtForVOPath(connection);


        Collection<VOFixDetails> vosToFix = voFixMap.values();
        Iterator<VOFixDetails> vosToFixIter = vosToFix.iterator();
        while (vosToFixIter.hasNext()) {
            VOFixDetails voToFix = vosToFixIter.next();
            if (needToFix(voToFix)) {
                String voPkg = voToFix.getVoPackage();

                String voPath = getVOPath(stmt, voPkg);
                voToFix.setVoFullPath(voPath);
                if (isEmpty(voPath)) {
                    voToFix.setStatus("VOPath  not obtained");
                    finalResults.add(voToFix);
                    continue;
                }

                makeAndApplyFix(voToFix, bDoADE);
                finalResults.add(voToFix);
            } else {
                voToFix.setStatus("NoFixRequired");
                finalResults.add(voToFix);
            }
        }
        return finalResults;
    }

    public String getAdeViewRoot() {
        if (currentDir.contains("/fusionapps/")) {
            return currentDir.substring(0, currentDir.indexOf("/fusionapps/"));
        } else {
            return System.getenv("ADE_VIEW_ROOT");
        }
    }

    public boolean makeAndApplyFix(VOFixDetails voToFix, boolean bDoADE) {
        // get the absolute path of the vo file and verify it exists
        String viewRoot = getAdeViewRoot();
        if (isEmpty(viewRoot)) {
            System.out.println("Could not obtain view root");
            voToFix.setStatus("ViewRoot not obtained");
            return false;
        }
        String absPath = viewRoot + "/" + voToFix.getVoFullPath();
        if (stripeByRunDir) {
            if (!absPath.contains(currentDir)) {
                if (m_debug) {
                    System.out.println(String.format("VO not in run directory.. skipping %s", absPath));
                }
                voToFix.setStatus("VO Not in Run directory.. Skipping");
                return false;
            }
        }
        File voFile = new File(absPath);
        if (voFile == null || !voFile.exists()) {
            System.out.println("Could not obtain VO abs path: " + absPath);
            voToFix.setStatus("Could not obtain VO abs path");
            return false;
        }
        ViewObjectHelper voHelper = new ViewObjectHelper(absPath);
        XmlFixer voFileFixer = null;
        try {
            voFileFixer = new XmlFixer(absPath);
            voFileFixer.setBDebug(m_debug);
        } catch (Exception ex) {
            voToFix.setStatus("failed");
            ex.printStackTrace();
            return false;
        }
        Node attributeNode = ViewObjectHelper.getAttributeByName(voHelper.getViewObjectNode(), voToFix.getAttributeName());
        if (attributeNode == null) {
            voToFix.setStatus("Failed");
            return false;
        }

        Node displayWidthNode = ViewObjectHelper.getDisplayWidthNodeForAttribute(attributeNode);
        if (displayWidthNode == null) {
            // voToFix.setStatus("DisplayWidthNodeNotPresent TODO: Handle Case to add the display width");
            String displayNodeString = String.format("<DISPLAYWIDTH Value=\"%s\"/>", voToFix.getNewAttributeWidth());

            // Check if the node has a properties node
            Node propertiesNode = XMLParserHelper.getChildNodeWithName(attributeNode, "Properties");

            Node nodeToAddTo = null;
            String strRepOfNodeToAdd = "";

            String newLine = "\n";
            String childIndentation = "  ";
            String indentationFromParent = childIndentation;
            if (propertiesNode == null) {
                String attributeNodeIndentation = voFileFixer.getIndentation(attributeNode);


                StringBuffer sb = new StringBuffer();
                sb.append("<Properties>");
                sb.append(newLine + attributeNodeIndentation + childIndentation + childIndentation);
                sb.append("<SchemaBasedProperties>");
                sb.append(newLine + attributeNodeIndentation + childIndentation + childIndentation + childIndentation);
                sb.append(displayNodeString);
                sb.append(newLine + attributeNodeIndentation + childIndentation + childIndentation + "</SchemaBasedProperties>");
                sb.append(newLine + attributeNodeIndentation + childIndentation + "</Properties>");

                nodeToAddTo = attributeNode;
                strRepOfNodeToAdd = sb.toString();

            } else {
                Node schemaNode = XMLParserHelper.getChildNodeWithName(propertiesNode, "SchemaBasedProperties");
                if (schemaNode == null) {
                    StringBuffer sb = new StringBuffer();
                    String propertiesNodeindentiation = voFileFixer.getIndentation(propertiesNode);
                    sb.append("<SchemaBasedProperties>");
                    sb.append(newLine + propertiesNodeindentiation + childIndentation + childIndentation);
                    sb.append(displayNodeString);
                    sb.append(newLine + propertiesNodeindentiation + childIndentation + "</SchemaBasedProperties>");
                    nodeToAddTo = propertiesNode;
                    strRepOfNodeToAdd = sb.toString();
                } else {
                    nodeToAddTo = schemaNode;
                    strRepOfNodeToAdd = displayNodeString;
                }
            }
            if (nodeToAddTo != null) {
                try {
                    voFileFixer.addAsLastChildToNode(nodeToAddTo, strRepOfNodeToAdd, indentationFromParent);
                    voToFix.setStatus("Success");
                    if (bDoADE) {
                        applyFix(voFileFixer, absPath, true, bDoADE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    voToFix.setStatus(ex.getMessage());
                    return false;
                }
            }

            return false;
        }
        String attrValueInFile = XMLParserHelper.getAttributeValue(displayWidthNode, "Value");
        String attrOrigVal = voToFix.getOrigAttributeWidth();
        if (!attrOrigVal.equals(attrValueInFile)) {
            System.out.println(String.format("AttrValueMismatch in file.. File has Value (%s), and excel has %s",
                                             attrValueInFile, attrOrigVal));
            voToFix.setStatus(String.format("AttrValueMismatch in file.. File has Value (%s), and excel has %s", attrValueInFile,
                                            attrOrigVal));
            return false;
        } else {
            // Make the fix
            try {


                voFileFixer.modifyAttribute(displayWidthNode, "Value", attrOrigVal, voToFix.getNewAttributeWidth(), false);

                if (bDoADE) {
                    applyFix(voFileFixer, absPath, true, bDoADE);
                }
                voToFix.setStatus("Success");
                return true;
            } catch (Exception ex) {
                ex.printStackTrace();
                voToFix.setStatus(ex.getMessage());
                return false;
            }


        }

    }

    public void applyFix(XmlFixer fixer, String absPath, boolean makeFix, boolean bDoADE) throws Exception {
        if (makeFix && bDoADE) {
            try {
                String configNewContents = fixer.getFileContentsAsString(false);
                applyFix(absPath, configNewContents);
            } catch (Exception fixex) {
                System.out.println("[ERROR.. skipping file] Excpetion while making fix for file: " + absPath);
                fixex.printStackTrace();
                throw fixex;
            }

        }
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

    public PreparedStatement getStmtForVOPath(Connection connection) throws Exception {
        return connection.prepareStatement("SELECT BC_FILEPATH FROM ZEESHA_MODELMAPPER WHERE BC_PACKAGE=?");
    }

    public Connection getConnection() throws Exception {
        String jdbc = "jdbc:oracle:thin:@//rws65979fwks:1522/rws65979";
        String username = "fusion";
        String password = "fusion";

        Connection connection = DriverManager.getConnection(jdbc, username, password);
        return connection;

    }

    public String getVOPath(PreparedStatement stmt, String bcPackage) {
        String voPath = "";
        try {
            stmt.clearParameters();
            stmt.setString(1, bcPackage);
            ResultSet results = stmt.executeQuery();
            if (results != null && results.next()) {
                voPath = results.getString(1);
            }
            if (results != null)
                results.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return voPath;

    }

    public boolean needToFix(VOFixDetails vo) {
        if (!isEmpty(vo.getNewAttributeWidth()) && !vo.getNewAttributeWidth().equals(vo.getOrigAttributeWidth()))
            return true;
        return false;
    }

    public void printValidationErrors(ArrayList<String> validationErrors) {
        if (validationErrors == null)
            return;
        System.out.println("There were one or more CSV Validation Errors.. Please correct the errors before proceeding.. ");
        Iterator<String> validateerrorIter = validationErrors.iterator();
        int num = 0;
        while (validateerrorIter.hasNext()) {
            num++;
            String error = validateerrorIter.next();
            System.out.println(String.format("%d . %s", num, error));
        }
    }

    public static String setSystemProperties() {
        StringBuffer sb = new StringBuffer();
        String debugOpt = System.getProperty("debug");
        if (!isEmpty(debugOpt) && "true".equals(debugOpt.trim()))
            m_debug = true;
        else if (!isEmpty(debugOpt) && "false".equals(debugOpt.trim()))
            m_debug = false;

        sb.append(" -Ddebug=" + m_debug);

        String abortOnValidationOpt = System.getProperty("abortOnValidationError");
        if (!isEmpty(abortOnValidationOpt) && "true".equals(abortOnValidationOpt.trim()))
            m_abortOnValidationError = true;
        else if (!isEmpty(abortOnValidationOpt) && "false".equals(abortOnValidationOpt.trim()))
            m_abortOnValidationError = false;
        sb.append(" -DabortOnValidationError=" + m_abortOnValidationError);

        String stripeOnRunDir = System.getProperty("stripeOnRunDir");
        if (!isEmpty(stripeOnRunDir) && "true".equals(stripeOnRunDir.trim()))
            stripeByRunDir = true;
        else if (!isEmpty(stripeOnRunDir) && "false".equals(stripeOnRunDir.trim()))
            stripeByRunDir = false;
        sb.append(" -DstripeOnRunDir=" + stripeByRunDir);
        return sb.toString();
    }

    public ArrayList<String> parseCsvFile(File csvFile) throws Exception {

        voFixMap = new HashMap<String, VOFixDetails>(100);
        ArrayList<String> validationErrors = new ArrayList<String>();
        int lineNum = 0;
        FileReader reader = new FileReader(csvFile);
        BufferedReader input = new BufferedReader(reader);
        String firstLine = input.readLine();
        lineNum++;

        if (firstLine == null || !validateCsvHeaderFormat(firstLine)) {
            if (firstLine == null)
                firstLine = "";
            throw new Exception("CSV Header Invalid Format: " + firstLine);
        }
        String line;
        while ((line = input.readLine()) != null) {
            lineNum++;
            if (isEmpty(line)) {
                continue;
            }
            try {
                String voPackage = getColumnValue(line, "M");
                String voAttributeName = getColumnValue(line, "G");
                String voAtributeWidth = getColumnValue(line, "H");
                String voAtributeNewWidth = getColumnValue(line, "I");

                // Validate that only  numbers present in the widths columns
                if (!isEmpty(voAtributeWidth)) {
                    Integer.parseInt(voAtributeWidth);
                }
                if (!isEmpty(voAtributeNewWidth)) {
                    Integer.parseInt(voAtributeNewWidth);
                }

                if (isEmpty(voPackage) || isEmpty(voAttributeName)) {
                    validationErrors.add(String.format("LineNumber: %d , Exception VoPackage or attribute name was null",
                                                       lineNum));
                    continue;
                }

                String key = voPackage + "#" + voAttributeName;
                VOFixDetails fixDetailVO = new VOFixDetails();
                fixDetailVO.setVoPackage(voPackage);
                fixDetailVO.setAttributeName(voAttributeName);
                fixDetailVO.setOrigAttributeWidth(voAtributeWidth);
                fixDetailVO.setNewAttributeWidth(voAtributeNewWidth);

                if (voFixMap.containsKey(key)) {
                    VOFixDetails existingDetailVO = voFixMap.get(key);
                    String existingNewAttrWidth = existingDetailVO.getNewAttributeWidth();
                    String attrWidthToSet = fixDetailVO.getNewAttributeWidth();
                    if (!existingNewAttrWidth.equals(attrWidthToSet)) {
                        System.out.println("Validation error.. ");
                        validationErrors.add(String.format("LineNum: %d : ValidationError:(new attribute width mismatch) (Key: %s) (Values in conflict %s (on line num %d) and %s ",
                                                           lineNum, key, existingNewAttrWidth, lineNum, attrWidthToSet));
                        continue;
                    }
                } else {
                    voFixMap.put(key, fixDetailVO);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                validationErrors.add(String.format("LineNumber: %d , Exception;", lineNum, ex.getMessage()));
            }

        }
        System.out.println("Total number of lines read: " + lineNum);
        return validationErrors;
    }

    public boolean validateCsvHeaderFormat(String headerLine) {
        try {
            String voNameCol = getColumnValue(headerLine, "F");
            String voAttrCol = getColumnValue(headerLine, "G");
            String voAttrWidth = getColumnValue(headerLine, "H");
            String voAttrNewWid = getColumnValue(headerLine, "I");
            String voPackage = getColumnValue(headerLine, "M");

            if (isEmpty(voNameCol) || isEmpty(voAttrCol) || isEmpty(voAttrWidth) || isEmpty(voAttrNewWid) ||
                isEmpty(voPackage)) {
                return false;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }

        return true;
    }

    public String getColumnValue(String line, String columnName) throws Exception {

        if (isEmpty(line) || isEmpty(columnName))
            return "";

        String[] parts = line.split(",");
        // Invalid line format
        if (parts == null || parts.length < 13) {
            throw new Exception("Invalid line format.. does not have atleast 13 columns");
        }

        // COL = F
        int voNameColIndex = 5;
        // COL = G
        int voAttributeColIndex = 6;
        // COL = H
        int voAttributeWidthColInd = 7;
        // COL = I
        int voAttributeNewWidthColInd = 8;
        // COL = M
        int voPackageColInd = 12;


        if ("F".equals(columnName.trim()) || "VOName".equalsIgnoreCase(columnName.trim())) {
            return parts[voNameColIndex];
        } else if ("G".equals(columnName.trim()) || "VOAttribute".equalsIgnoreCase(columnName.trim())) {
            return parts[voAttributeColIndex];
        } else if ("H".equals(columnName.trim()) || "VOAttributeWidth".equalsIgnoreCase(columnName.trim())) {
            return parts[voAttributeWidthColInd];
        } else if ("I".equals(columnName.trim()) || "VOAttributeNewWidth".equalsIgnoreCase(columnName.trim())) {
            return parts[voAttributeNewWidthColInd];
        } else if ("M".equals(columnName.trim()) || "VOPackage".equalsIgnoreCase(columnName.trim())) {
            return parts[voPackageColInd];
        } else {
            throw new Exception("Invalid column Name");
        }

    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }

}
